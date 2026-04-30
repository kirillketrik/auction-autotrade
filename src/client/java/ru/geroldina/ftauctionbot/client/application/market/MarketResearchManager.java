package ru.geroldina.ftauctionbot.client.application.market;

import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanController;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanLifecycleObserver;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanPageObserver;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionPageDecision;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanResultSummary;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MarketResearchManager implements AuctionScanPageObserver, AuctionScanLifecycleObserver {
    private static final DateTimeFormatter JSON_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AuctionScanController scanController;
    private final MarketResearchRepository repository;
    private final ScanLogger logger;
    private final Clock clock;

    private List<MarketResearchResult> cachedResults = List.of();
    private ActiveSession activeSession;

    public MarketResearchManager(AuctionScanController scanController, MarketResearchRepository repository, ScanLogger logger) {
        this(scanController, repository, logger, Clock.systemDefaultZone());
    }

    MarketResearchManager(AuctionScanController scanController, MarketResearchRepository repository, ScanLogger logger, Clock clock) {
        this.scanController = scanController;
        this.repository = repository;
        this.logger = logger;
        this.clock = clock;
        this.scanController.addPageObserver(this);
        this.scanController.addLifecycleObserver(this);
    }

    public List<MarketResearchResult> load() {
        cachedResults = sortDescending(repository.load());
        return cachedResults;
    }

    public List<MarketResearchResult> getCachedResults() {
        return cachedResults;
    }

    public boolean isRunning() {
        return activeSession != null;
    }

    public MarketResearchStartResult startRuleResearch(BuyRule rule, AutobuyConfig config) {
        return startRuleResearch(rule, config, config.pageSwitchDelayMs());
    }

    public MarketResearchStartResult startRuleResearch(BuyRule rule, AutobuyConfig config, int pageSwitchDelayMs) {
        if (activeSession != null || !scanController.isIdle()) {
            return MarketResearchStartResult.rejected("Исследование рынка уже выполняется.");
        }
        if (rule == null) {
            return MarketResearchStartResult.rejected("Не выбрано правило для исследования рынка.");
        }
        if (config == null) {
            return MarketResearchStartResult.rejected("Не найдена конфигурация market research.");
        }

        String ruleName = rule.name() == null ? "" : rule.name().trim();
        if (ruleName.isEmpty()) {
            return MarketResearchStartResult.rejected("Для исследования рынка у правила должно быть заполнено название.");
        }

        String targetMinecraftId = resolveMinecraftId(rule);
        if (targetMinecraftId == null) {
            return MarketResearchStartResult.rejected("Для исследования рынка у правила должно быть условие minecraft ID.");
        }

        String command = "ah search " + ruleName;
        String startedAt = JSON_TIMESTAMP_FORMAT.format(LocalDateTime.now(clock));
        activeSession = new ActiveSession(
            rule,
            rule.id(),
            ruleName,
            command,
            targetMinecraftId,
            startedAt,
            config.marketResearchTargetMarginPercent(),
            config.marketResearchRiskBufferPercent()
        );
        scanController.startScanAllPagesCommand(command, pageSwitchDelayMs);
        logger.info("MARKET_RESEARCH", "Started market research for rule=" + rule.id() + ", command=/" + command + ", minecraftId=" + targetMinecraftId);
        return MarketResearchStartResult.started("Запущено исследование рынка для правила " + ruleName + ".");
    }

    @Override
    public AuctionPageDecision onPageScanned(int syncId, int currentPage, int totalPages, List<AuctionLot> pageLots) {
        if (activeSession == null) {
            return AuctionPageDecision.CONTINUE;
        }

        activeSession.scannedPages = Math.max(activeSession.scannedPages, currentPage);
        activeSession.totalScannedLots += pageLots.size();
        for (AuctionLot lot : pageLots) {
            if (!matchesRuleConditions(lot, activeSession.rule)) {
                continue;
            }

            activeSession.matchedLots++;
            activeSession.matchedUnitPrices.add(lot.unitPrice());
            activeSession.minUnitPrice = activeSession.minUnitPrice == null ? lot.unitPrice() : Math.min(activeSession.minUnitPrice, lot.unitPrice());
            activeSession.maxUnitPrice = activeSession.maxUnitPrice == null ? lot.unitPrice() : Math.max(activeSession.maxUnitPrice, lot.unitPrice());
            if (activeSession.itemDisplayName == null || activeSession.itemDisplayName.isBlank()) {
                activeSession.itemDisplayName = lot.displayName();
            }
        }

        return AuctionPageDecision.CONTINUE;
    }

    @Override
    public void onScanFinished(AuctionScanResultSummary summary) {
        if (activeSession == null) {
            return;
        }

        ActiveSession session = activeSession;
        activeSession = null;

        session.matchedUnitPrices.sort(Long::compareTo);
        MarketPricingAlgorithms.RecommendationBundle bundle = MarketPricingAlgorithms.buildRecommendations(
            session.matchedUnitPrices,
            session.targetMarginPercent,
            session.riskBufferPercent
        );
        MarketPricingAlgorithms.PriceStats stats = bundle.stats();
        MarketResearchResult result = new MarketResearchResult(
            session.ruleId,
            session.ruleName,
            session.command,
            session.targetMinecraftId,
            session.itemDisplayName == null || session.itemDisplayName.isBlank() ? session.ruleName : session.itemDisplayName,
            session.startedAt,
            JSON_TIMESTAMP_FORMAT.format(LocalDateTime.now(clock)),
            summary.completed(),
            summary.abortReason(),
            summary.scannedPages(),
            session.totalScannedLots,
            session.matchedLots,
            stats.p10(),
            stats.p25(),
            stats.p50(),
            stats.p75(),
            stats.p90(),
            stats.largestGap(),
            stats.lowerClusterSize(),
            stats.mainClusterSize(),
            session.minUnitPrice,
            stats.avg(),
            session.maxUnitPrice,
            bundle.recommendations()
        );

        List<MarketResearchResult> updatedResults = new ArrayList<>(cachedResults.isEmpty() ? repository.load() : cachedResults);
        updatedResults.add(result);
        updatedResults = sortDescending(updatedResults);
        repository.save(updatedResults);
        cachedResults = updatedResults;
        logger.info(
            "MARKET_RESEARCH",
            "Finished market research for rule=" + session.ruleId
                + ", completed=" + summary.completed()
                + ", matchedLots=" + session.matchedLots
                + ", minUnitPrice=" + result.minUnitPrice()
                + ", avgUnitPrice=" + result.avgUnitPrice()
                + ", maxUnitPrice=" + result.maxUnitPrice()
        );
    }

    private static String resolveMinecraftId(BuyRule rule) {
        for (BuyRuleCondition condition : rule.conditions()) {
            if (condition instanceof ItemIdCondition itemIdCondition && itemIdCondition.minecraftId() != null && !itemIdCondition.minecraftId().isBlank()) {
                return itemIdCondition.minecraftId().trim();
            }
        }
        return null;
    }

    private static List<MarketResearchResult> sortDescending(List<MarketResearchResult> results) {
        return results.stream()
            .sorted(Comparator.comparing(MarketResearchResult::finishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    private static boolean matchesRuleConditions(AuctionLot lot, BuyRule rule) {
        for (BuyRuleCondition condition : rule.conditions()) {
            if (!condition.matches(lot).matched()) {
                return false;
            }
        }
        return true;
    }

    private static final class ActiveSession {
        private final BuyRule rule;
        private final String ruleId;
        private final String ruleName;
        private final String command;
        private final String targetMinecraftId;
        private final String startedAt;
        private final int targetMarginPercent;
        private final int riskBufferPercent;

        private int scannedPages;
        private int totalScannedLots;
        private int matchedLots;
        private final List<Long> matchedUnitPrices = new ArrayList<>();
        private Long minUnitPrice;
        private Long maxUnitPrice;
        private String itemDisplayName;

        private ActiveSession(
            BuyRule rule,
            String ruleId,
            String ruleName,
            String command,
            String targetMinecraftId,
            String startedAt,
            int targetMarginPercent,
            int riskBufferPercent
        ) {
            this.rule = rule;
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.command = command;
            this.targetMinecraftId = targetMinecraftId;
            this.startedAt = startedAt;
            this.targetMarginPercent = targetMarginPercent;
            this.riskBufferPercent = riskBufferPercent;
        }
    }
}
