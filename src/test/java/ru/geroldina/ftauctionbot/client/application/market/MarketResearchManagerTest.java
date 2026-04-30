package ru.geroldina.ftauctionbot.client.application.market;

import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanController;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanLifecycleObserver;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanPageObserver;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionPageDecision;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanResultSummary;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.auction.model.EnchantmentData;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredEnchantmentsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketResearchManagerTest {
    @Test
    void startsSearchByRuleNameAndAggregatesUnitPrices() {
        FakeScanController scanController = new FakeScanController();
        InMemoryRepository repository = new InMemoryRepository();
        MarketResearchManager manager = new MarketResearchManager(
            scanController,
            repository,
            new NoopLogger(),
            Clock.fixed(Instant.parse("2026-04-30T10:15:30Z"), ZoneOffset.UTC)
        );

        MarketResearchStartResult startResult = manager.startRuleResearch(
            BuyRule.of("totem", "Totem Rule", true, new ItemIdCondition("minecraft:totem_of_undying")),
            sampleConfig(),
            350
        );

        assertTrue(startResult.started());
        assertEquals("ah search Totem Rule", scanController.lastCommand);
        assertEquals(350, scanController.lastDelayMs);

        AuctionPageDecision firstDecision = scanController.pageObserver.onPageScanned(1, 1, 2, List.of(
            new AuctionLot(1, 1, "minecraft:totem_of_undying", "Totem", 1, 120L, 120L, null, List.of(), List.of()),
            new AuctionLot(1, 2, "minecraft:stone", "Stone", 64, 640L, 10L, null, List.of(), List.of())
        ));
        AuctionPageDecision secondDecision = scanController.pageObserver.onPageScanned(2, 2, 2, List.of(
            new AuctionLot(2, 1, "minecraft:totem_of_undying", "Totem", 2, 300L, 150L, null, List.of(), List.of())
        ));
        scanController.lifecycleObserver.onScanFinished(new AuctionScanResultSummary("ah search Totem Rule", true, null, 2, 3));

        assertEquals(AuctionPageDecision.CONTINUE, firstDecision);
        assertEquals(AuctionPageDecision.CONTINUE, secondDecision);
        assertEquals(1, repository.results.size());

        MarketResearchResult result = repository.results.getFirst();
        assertEquals("minecraft:totem_of_undying", result.targetMinecraftId());
        assertEquals("Totem", result.itemDisplayName());
        assertEquals(2, result.scannedPages());
        assertEquals(3, result.totalScannedLots());
        assertEquals(2, result.matchedLots());
        assertEquals(120L, result.minUnitPrice());
        assertEquals(135L, result.avgUnitPrice());
        assertEquals(150L, result.maxUnitPrice());
        assertEquals(128L, result.p25UnitPrice());
        assertEquals(135L, result.p50UnitPrice());
        assertEquals(143L, result.p75UnitPrice());
        assertEquals(4, result.recommendations().size());
        assertEquals("ABSTAINED", result.recommendations().get(0).status());
        assertEquals(108L, result.recommendations().get(1).maxBuyPrice());
        assertEquals(135L, result.recommendations().get(1).recommendedSellPrice());
        assertNotNull(result.recommendations().get(3).status());
    }

    @Test
    void rejectsRuleWithoutMinecraftId() {
        FakeScanController scanController = new FakeScanController();
        MarketResearchManager manager = new MarketResearchManager(
            scanController,
            new InMemoryRepository(),
            new NoopLogger(),
            Clock.systemUTC()
        );

        MarketResearchStartResult result = manager.startRuleResearch(BuyRule.of("totem", "Totem Rule", true), sampleConfig(), 200);

        assertTrue(!result.started());
        assertNull(scanController.lastCommand);
    }

    @Test
    void filtersResearchLotsByAllRuleConditions() {
        FakeScanController scanController = new FakeScanController();
        InMemoryRepository repository = new InMemoryRepository();
        MarketResearchManager manager = new MarketResearchManager(
            scanController,
            repository,
            new NoopLogger(),
            Clock.fixed(Instant.parse("2026-04-30T10:15:30Z"), ZoneOffset.UTC)
        );

        BuyRule rule = BuyRule.of(
            "sharp_sword",
            "Sharp Sword",
            false,
            new ItemIdCondition("minecraft:diamond_sword"),
            new RequiredEnchantmentsCondition(List.of(new RequiredEnchantment("minecraft:sharpness", 5)))
        );

        MarketResearchStartResult startResult = manager.startRuleResearch(rule, sampleConfig(), 350);

        assertTrue(startResult.started());

        scanController.pageObserver.onPageScanned(1, 1, 1, List.of(
            new AuctionLot(1, 1, "minecraft:diamond_sword", "Sharp Sword", 1, 1_500L, 1_500L, null, List.of(
                new EnchantmentData("minecraft:sharpness", 5)
            ), List.of()),
            new AuctionLot(1, 2, "minecraft:diamond_sword", "Sharp Sword", 1, 900L, 900L, null, List.of(
                new EnchantmentData("minecraft:sharpness", 4)
            ), List.of()),
            new AuctionLot(1, 3, "minecraft:stone", "Stone", 64, 640L, 10L, null, List.of(), List.of())
        ));
        scanController.lifecycleObserver.onScanFinished(new AuctionScanResultSummary("ah search Sharp Sword", true, null, 1, 3));

        MarketResearchResult result = repository.results.getFirst();
        assertEquals(3, result.totalScannedLots());
        assertEquals(1, result.matchedLots());
        assertEquals(1_500L, result.minUnitPrice());
        assertEquals(1_500L, result.avgUnitPrice());
        assertEquals(1_500L, result.maxUnitPrice());
    }

    private static AutobuyConfig sampleConfig() {
        return new AutobuyConfig(30, 10, 200, AutobuyScanLogMode.MATCHED_ONLY, 15, 5, List.of());
    }

    private static final class FakeScanController implements AuctionScanController {
        private AuctionScanPageObserver pageObserver;
        private AuctionScanLifecycleObserver lifecycleObserver;
        private String lastCommand;
        private int lastDelayMs;

        @Override
        public void startScan(int maxPages, int pageSwitchDelayMs) {
        }

        @Override
        public void startScanCommand(String command, int maxPages, int pageSwitchDelayMs) {
            this.lastCommand = command;
            this.lastDelayMs = pageSwitchDelayMs;
        }

        @Override
        public void startScanAllPagesCommand(String command, int pageSwitchDelayMs) {
            this.lastCommand = command;
            this.lastDelayMs = pageSwitchDelayMs;
        }

        @Override
        public boolean isIdle() {
            return true;
        }

        @Override
        public void addPageObserver(AuctionScanPageObserver observer) {
            this.pageObserver = observer;
        }

        @Override
        public void addLifecycleObserver(AuctionScanLifecycleObserver observer) {
            this.lifecycleObserver = observer;
        }
    }

    private static final class InMemoryRepository implements MarketResearchRepository {
        private List<MarketResearchResult> results = List.of();

        @Override
        public List<MarketResearchResult> load() {
            return results;
        }

        @Override
        public void save(List<MarketResearchResult> results) {
            this.results = results;
        }
    }

    private static final class NoopLogger implements ScanLogger {
        @Override
        public void info(String category, String message) {
        }

        @Override
        public void block(String category, List<String> lines) {
        }
    }
}
