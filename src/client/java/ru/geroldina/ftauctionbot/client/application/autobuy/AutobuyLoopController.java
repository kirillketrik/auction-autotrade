package ru.geroldina.ftauctionbot.client.application.autobuy;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceObserver;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceService;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionPageDecision;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanController;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanPageObserver;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy.AutobuyConfigScreen;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class AutobuyLoopController implements MinecraftClientEventListener, BalanceObserver, AuctionScanPageObserver {
    private static final int TICKS_PER_SECOND = 20;
    private static final int PURCHASE_SUPPRESSION_TICKS = 40;
    private static final int NEXT_TASK_START_DELAY_TICKS = 4;

    private final AuctionClientGateway gateway;
    private final AuctionScanController scanController;
    private final BalanceService balanceService;
    private final AutobuyConfigManager configManager;
    private final AutobuyRuleMatcher ruleMatcher;
    private final ScanLogger logger;

    private boolean enabled;
    private boolean waitingForBalance;
    private int ticksUntilNextScan;
    private boolean scanCycleActive;
    private int purchaseSuppressionTicks;
    private int suppressedSyncId = -1;
    private final Queue<ScanTask> pendingScanTasks = new ArrayDeque<>();
    private ScanTask delayedScanTask;
    private int delayedScanTaskTicks;

    public AutobuyLoopController(
        AuctionClientGateway gateway,
        AuctionScanController scanController,
        BalanceService balanceService,
        AutobuyConfigManager configManager,
        AutobuyRuleMatcher ruleMatcher,
        ScanLogger logger
    ) {
        this.gateway = gateway;
        this.scanController = scanController;
        this.balanceService = balanceService;
        this.configManager = configManager;
        this.ruleMatcher = ruleMatcher;
        this.logger = logger;
        this.balanceService.addObserver(this);
        this.scanController.addPageObserver(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        enabled = true;
        ticksUntilNextScan = 0;
        waitingForBalance = false;
        scanCycleActive = false;
        purchaseSuppressionTicks = 0;
        suppressedSyncId = -1;
        pendingScanTasks.clear();
        delayedScanTask = null;
        delayedScanTaskTicks = 0;
        logger.info("AUTOBUY_LOOP", "Autobuy loop started.");
    }

    public void stop() {
        enabled = false;
        ticksUntilNextScan = 0;
        waitingForBalance = false;
        scanCycleActive = false;
        purchaseSuppressionTicks = 0;
        suppressedSyncId = -1;
        pendingScanTasks.clear();
        delayedScanTask = null;
        delayedScanTaskTicks = 0;
        logger.info("AUTOBUY_LOOP", "Autobuy loop stopped.");
    }

    @Override
    public void onClientTick() {
        if (purchaseSuppressionTicks > 0) {
            purchaseSuppressionTicks--;
            if (purchaseSuppressionTicks == 0) {
                suppressedSyncId = -1;
            }
        }

        if (delayedScanTaskTicks > 0) {
            delayedScanTaskTicks--;
        }

        if (!enabled || !gateway.isReady()) {
            return;
        }

        if (waitingForBalance || balanceService.isAwaitingRefresh()) {
            return;
        }

        if (delayedScanTask != null) {
            if (delayedScanTaskTicks > 0) {
                return;
            }

            ScanTask task = delayedScanTask;
            delayedScanTask = null;
            startScanTask(task);
            return;
        }

        if (scanController.isIdle() && !pendingScanTasks.isEmpty()) {
            startNextQueuedScan();
            return;
        }

        if (scanController.isIdle() && scanCycleActive && pendingScanTasks.isEmpty()) {
            scanCycleActive = false;
            scheduleNextScan();
            logger.info("AUTOBUY_LOOP", "Completed autobuy scan cycle. Next cycle in " + configManager.getCurrentConfig().scanIntervalSeconds() + "s.");
            return;
        }

        if (!scanController.isIdle()) {
            return;
        }

        if (ticksUntilNextScan > 0) {
            ticksUntilNextScan--;
            return;
        }

        waitingForBalance = balanceService.requestRefresh();
        if (!waitingForBalance) {
            logger.info("AUTOBUY_LOOP", "Failed to request balance refresh before scan.");
        }
    }

    @Override
    public void onBalanceUpdated(MoneySnapshot snapshot) {
        if (!enabled || !waitingForBalance) {
            return;
        }

        waitingForBalance = false;
        AutobuyConfig config = configManager.getCurrentConfig();
        pendingScanTasks.clear();
        delayedScanTask = null;
        delayedScanTaskTicks = 0;
        pendingScanTasks.addAll(buildScanTasks(config.buyRules(), config.scanPageLimit()));
        scanCycleActive = !pendingScanTasks.isEmpty();
        logger.info(
            "AUTOBUY_LOOP",
            "Prepared autobuy scan cycle. interval=" + config.scanIntervalSeconds()
                + "s, pageLimit=" + config.scanPageLimit()
                + ", pageSwitchDelayMs=" + config.pageSwitchDelayMs()
                + ", balance=$" + snapshot.amount()
                + ", tasks=" + pendingScanTasks.size()
        );

        if (!scanCycleActive) {
            scheduleNextScan();
            logger.info("AUTOBUY_LOOP", "Skipped scan cycle because there are no buy rules.");
            return;
        }

        startNextQueuedScan();
    }

    @Override
    public void onBalanceRefreshFailed(String reason) {
        if (!enabled || !waitingForBalance) {
            return;
        }

        waitingForBalance = false;
        scheduleNextScan();
        logger.info("AUTOBUY_LOOP", "Balance refresh failed before scan: " + reason + ".");
    }

    @Override
    public AuctionPageDecision onPageScanned(int syncId, int currentPage, int totalPages, List<AuctionLot> pageLots) {
        if (!enabled) {
            return AuctionPageDecision.CONTINUE;
        }

        MoneySnapshot balance = balanceService.getLastKnownBalance().orElse(null);
        if (balance == null) {
            return AuctionPageDecision.CONTINUE;
        }

        int evaluatedLots = 0;
        int matchedLots = 0;
        for (AuctionLot lot : pageLots) {
            evaluatedLots++;
            BuyDecision decision = ruleMatcher.match(lot, configManager.getCurrentConfig().buyRules());
            logLotScanResult(lot, decision, balance);
            if (!decision.approved()) {
                continue;
            }
            matchedLots++;

            if (balance.amount() < lot.totalPrice()) {
                logger.info(
                    "AUTOBUY_LOOP",
                    "Matched lot at slot " + lot.slotIndex() + " but skipped because balance $" + balance.amount()
                        + " is below total price $" + lot.totalPrice() + "."
                );
                continue;
            }

            logPageMatchSummary(currentPage, totalPages, evaluatedLots, matchedLots, pageLots.size());
            gateway.clickSlot(syncId, lot.slotIndex(), 0, SlotActionType.QUICK_MOVE);
            purchaseSuppressionTicks = PURCHASE_SUPPRESSION_TICKS;
            suppressedSyncId = syncId;
            logger.info(
                "AUTOBUY_LOOP",
                "Bought matched lot on page " + currentPage + "/" + totalPages
                    + ", slot=" + lot.slotIndex()
                    + ", rule=" + decision.matchedRule().id()
                    + ", totalPrice=$" + lot.totalPrice()
                    + ", unitPrice=$" + lot.unitPrice()
                    + ", continuing_scan=true"
            );
            return AuctionPageDecision.CONTINUE;
        }

        logPageMatchSummary(currentPage, totalPages, evaluatedLots, matchedLots, pageLots.size());
        return AuctionPageDecision.CONTINUE;
    }

    @Override
    public void onCloseScreen(int syncId) {
        // Keep suppression alive through close/reopen transitions in the purchase flow.
    }

    @Override
    public boolean shouldSuppressScreen(Screen screen) {
        return purchaseSuppressionTicks > 0
            && screen != null
            && !(screen instanceof AutobuyConfigScreen);
    }

    private void scheduleNextScan() {
        ticksUntilNextScan = configManager.getCurrentConfig().scanIntervalSeconds() * TICKS_PER_SECOND;
    }

    private void startNextQueuedScan() {
        ScanTask nextTask = pendingScanTasks.poll();
        if (nextTask == null) {
            return;
        }

        if (gateway.closeActiveHandledScreen()) {
            delayedScanTask = nextTask;
            delayedScanTaskTicks = NEXT_TASK_START_DELAY_TICKS;
            logger.info(
                "AUTOBUY_LOOP",
                "Closed active handled screen before next scan task. Delaying " + nextTask.description
                    + " for " + NEXT_TASK_START_DELAY_TICKS + " ticks."
            );
            return;
        }

        startScanTask(nextTask);
    }

    private void startScanTask(ScanTask task) {
        logger.info("AUTOBUY_LOOP", "Starting headless scan task: " + task.description + " via /" + task.command);
        scanController.startScanCommand(task.command, task.maxPages, configManager.getCurrentConfig().pageSwitchDelayMs());
    }

    private void logLotScanResult(AuctionLot lot, BuyDecision decision, MoneySnapshot balance) {
        AutobuyScanLogMode logMode = configManager.getCurrentConfig().scanLogMode();
        if (logMode == AutobuyScanLogMode.MATCHED_ONLY && !decision.approved()) {
            return;
        }

        StringBuilder message = new StringBuilder()
            .append("minecraft_id=").append(lot.minecraftId())
            .append(", count=").append(lot.count())
            .append(", price=").append(lot.totalPrice())
            .append(", unit_price=").append(lot.unitPrice())
            .append(", matched=").append(decision.approved());

        if (decision.approved()) {
            message.append(", rule=").append(decision.matchedRule().id());
            if (balance.amount() < lot.totalPrice()) {
                message.append(", reason=insufficient_balance");
            } else {
                message.append(", reason=matched");
            }
        } else {
            message.append(", reason=").append(decision.reason());
        }

        logger.info("AUTOBUY_SCAN_RESULT", message.toString());
    }

    private void logPageMatchSummary(int currentPage, int totalPages, int evaluatedLots, int matchedLots, int totalLotsOnPage) {
        logger.info(
            "AUTOBUY_LOOP",
            "Evaluated autobuy rules for page " + currentPage + "/" + totalPages
                + ": parsedLots=" + totalLotsOnPage
                + ", evaluatedLots=" + evaluatedLots
                + ", matchedLots=" + matchedLots
        );
    }

    private List<ScanTask> buildScanTasks(List<BuyRule> rules, int pageLimit) {
        Set<String> uniqueQueries = new LinkedHashSet<>();
        boolean requiresGenericScan = false;

        for (BuyRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }

            String searchQuery = resolveSearchQuery(rule);
            if (searchQuery != null) {
                uniqueQueries.add(searchQuery);
            } else {
                requiresGenericScan = true;
            }
        }

        Queue<ScanTask> tasks = new ArrayDeque<>();
        for (String query : uniqueQueries) {
            tasks.add(new ScanTask("ah search " + query, "search \"" + query + "\"", pageLimit));
        }

        if (requiresGenericScan) {
            tasks.add(new ScanTask("ah", "generic auction scan", pageLimit));
        }

        return List.copyOf(tasks);
    }

    private String resolveSearchQuery(BuyRule rule) {
        for (BuyRuleCondition condition : rule.conditions()) {
            String query = condition.searchQuery().orElse(null);
            if (query != null && !query.isBlank()) {
                return query.trim();
            }
        }

        return null;
    }

    private record ScanTask(String command, String description, int maxPages) {
    }
}
