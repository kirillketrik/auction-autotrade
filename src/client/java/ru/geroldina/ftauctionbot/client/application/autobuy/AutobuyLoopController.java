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
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy.AutobuyConfigScreen;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.random.RandomGenerator;

public final class AutobuyLoopController implements MinecraftClientEventListener, BalanceObserver, AuctionScanPageObserver {
    private static final int TICKS_PER_SECOND = 20;
    private static final int PURCHASE_SUPPRESSION_TICKS = 40;
    private static final int NEXT_TASK_START_DELAY_TICKS = 4;
    private static final int ANTI_AFK_MOVEMENT_TICKS = 3;

    private final AuctionClientGateway gateway;
    private final AuctionScanController scanController;
    private final BalanceService balanceService;
    private final AutobuyConfigManager configManager;
    private final AutobuyRuleMatcher ruleMatcher;
    private final PurchaseHistoryManager purchaseHistoryManager;
    private final ScanLogger logger;
    private final RandomGenerator random;

    private boolean enabled;
    private boolean waitingForBalance;
    private int ticksUntilNextScan;
    private int ticksUntilNextAntiAfkAction;
    private boolean scanCycleActive;
    private int purchaseSuppressionTicks;
    private int suppressedSyncId = -1;
    private final Queue<ScanTask> pendingScanTasks = new ArrayDeque<>();
    private ScanTask delayedScanTask;
    private int delayedScanTaskTicks;
    private AntiAfkMoveDirection activeAntiAfkMovement;
    private int activeAntiAfkMovementTicks;

    public AutobuyLoopController(
        AuctionClientGateway gateway,
        AuctionScanController scanController,
        BalanceService balanceService,
        AutobuyConfigManager configManager,
        AutobuyRuleMatcher ruleMatcher,
        PurchaseHistoryManager purchaseHistoryManager,
        ScanLogger logger
    ) {
        this(gateway, scanController, balanceService, configManager, ruleMatcher, purchaseHistoryManager, logger, RandomGenerator.getDefault());
    }

    AutobuyLoopController(
        AuctionClientGateway gateway,
        AuctionScanController scanController,
        BalanceService balanceService,
        AutobuyConfigManager configManager,
        AutobuyRuleMatcher ruleMatcher,
        PurchaseHistoryManager purchaseHistoryManager,
        ScanLogger logger,
        RandomGenerator random
    ) {
        this.gateway = gateway;
        this.scanController = scanController;
        this.balanceService = balanceService;
        this.configManager = configManager;
        this.ruleMatcher = ruleMatcher;
        this.purchaseHistoryManager = purchaseHistoryManager;
        this.logger = logger;
        this.random = random;
        this.balanceService.addObserver(this);
        this.scanController.addPageObserver(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        enabled = true;
        ticksUntilNextScan = 0;
        ticksUntilNextAntiAfkAction = 0;
        waitingForBalance = false;
        scanCycleActive = false;
        purchaseSuppressionTicks = 0;
        suppressedSyncId = -1;
        pendingScanTasks.clear();
        delayedScanTask = null;
        delayedScanTaskTicks = 0;
        clearAntiAfkState();
        logger.info("AUTOBUY_LOOP", "Autobuy loop started.");
    }

    public void stop() {
        enabled = false;
        ticksUntilNextScan = 0;
        ticksUntilNextAntiAfkAction = 0;
        waitingForBalance = false;
        scanCycleActive = false;
        purchaseSuppressionTicks = 0;
        suppressedSyncId = -1;
        pendingScanTasks.clear();
        delayedScanTask = null;
        delayedScanTaskTicks = 0;
        clearAntiAfkState();
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
            clearAntiAfkState();
            return;
        }

        if (waitingForBalance || balanceService.isAwaitingRefresh()) {
            clearAntiAfkState();
            return;
        }

        if (delayedScanTask != null) {
            clearAntiAfkState();
            if (delayedScanTaskTicks > 0) {
                return;
            }

            ScanTask task = delayedScanTask;
            delayedScanTask = null;
            startScanTask(task);
            return;
        }

        if (scanController.isIdle() && !pendingScanTasks.isEmpty()) {
            clearAntiAfkState();
            startNextQueuedScan();
            return;
        }

        if (scanController.isIdle() && scanCycleActive && pendingScanTasks.isEmpty()) {
            scanCycleActive = false;
            scheduleNextScan();
            logger.info("AUTOBUY_LOOP", "Completed autobuy scan cycle. Scheduled next cycle in " + ticksUntilNextScan + " ticks.");
            return;
        }

        if (!scanController.isIdle()) {
            clearAntiAfkState();
            return;
        }

        if (ticksUntilNextScan > 0) {
            tickAntiAfkDuringPause();
            ticksUntilNextScan--;
            return;
        }

        clearAntiAfkState();
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
                + "s, intervalJitter=" + config.scanIntervalJitterSeconds()
                + "s, pageLimit=" + config.scanPageLimit()
                + ", pageSwitchDelayMs=" + config.pageSwitchDelayMs()
                + ", pageSwitchDelayJitterMs=" + config.pageSwitchDelayJitterMs()
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
            purchaseHistoryManager.recordPurchase(lot);
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
        AutobuyConfig config = configManager.getCurrentConfig();
        int randomizedSeconds = randomizeAroundBase(config.scanIntervalSeconds(), config.scanIntervalJitterSeconds(), 1);
        ticksUntilNextScan = randomizedSeconds * TICKS_PER_SECOND;
        ticksUntilNextAntiAfkAction = Math.min(ticksUntilNextScan, config.antiAfkActionIntervalSeconds() * TICKS_PER_SECOND);
        clearAntiAfkState();
        logger.info(
            "AUTOBUY_LOOP",
            "Next autobuy cycle scheduled in " + randomizedSeconds
                + "s (base=" + config.scanIntervalSeconds()
                + "s, jitter=" + config.scanIntervalJitterSeconds() + "s)."
        );
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
        clearAntiAfkState();
        AutobuyConfig config = configManager.getCurrentConfig();
        logger.info("AUTOBUY_LOOP", "Starting headless scan task: " + task.description + " via /" + task.command);
        scanController.startScanCommand(task.command, task.maxPages, config.pageSwitchDelayMs(), config.pageSwitchDelayJitterMs());
    }

    private void tickAntiAfkDuringPause() {
        if (activeAntiAfkMovementTicks > 0 && activeAntiAfkMovement != null) {
            if (!gateway.canPerformAntiAfkActions()) {
                clearAntiAfkState();
                return;
            }

            gateway.applyAntiAfkMovement(activeAntiAfkMovement);
            activeAntiAfkMovementTicks--;
            if (activeAntiAfkMovementTicks == 0) {
                gateway.stopAntiAfkMovement();
                activeAntiAfkMovement = null;
            }
        }

        AutobuyConfig config = configManager.getCurrentConfig();
        if (!config.antiAfkEnabled() || !gateway.canPerformAntiAfkActions()) {
            clearAntiAfkState();
            return;
        }

        if (ticksUntilNextAntiAfkAction > 0) {
            ticksUntilNextAntiAfkAction--;
            return;
        }

        performAntiAfkAction(config);
        ticksUntilNextAntiAfkAction = Math.min(ticksUntilNextScan, config.antiAfkActionIntervalSeconds() * TICKS_PER_SECOND);
    }

    private void performAntiAfkAction(AutobuyConfig config) {
        int jumpRoll = random.nextInt(100);
        if (jumpRoll < config.antiAfkJumpChancePercent()) {
            gateway.jump();
            logger.info("AUTOBUY_LOOP", "Performed anti-AFK jump while waiting for the next autobuy cycle.");
            return;
        }

        AntiAfkMoveDirection direction = switch (random.nextInt(4)) {
            case 0 -> AntiAfkMoveDirection.FORWARD;
            case 1 -> AntiAfkMoveDirection.BACKWARD;
            case 2 -> AntiAfkMoveDirection.LEFT;
            default -> AntiAfkMoveDirection.RIGHT;
        };
        activeAntiAfkMovement = direction;
        activeAntiAfkMovementTicks = ANTI_AFK_MOVEMENT_TICKS;
        gateway.applyAntiAfkMovement(direction);
        logger.info("AUTOBUY_LOOP", "Performed anti-AFK movement: " + direction + ".");
    }

    private int randomizeAroundBase(int base, int jitter, int minValue) {
        int normalizedJitter = Math.max(0, jitter);
        if (normalizedJitter == 0) {
            return Math.max(minValue, base);
        }

        int delta = random.nextInt(normalizedJitter * 2 + 1) - normalizedJitter;
        return Math.max(minValue, base + delta);
    }

    private void clearAntiAfkState() {
        if (activeAntiAfkMovement != null || activeAntiAfkMovementTicks > 0) {
            gateway.stopAntiAfkMovement();
        }
        activeAntiAfkMovement = null;
        activeAntiAfkMovementTicks = 0;
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
        for (BuyRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }
            return List.of(new ScanTask("ah", "generic auction scan", pageLimit));
        }

        return List.of();
    }

    private record ScanTask(String command, String description, int maxPages) {
    }
}
