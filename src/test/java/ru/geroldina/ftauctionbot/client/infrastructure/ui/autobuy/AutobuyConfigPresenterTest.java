package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.PurchaseHistoryManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.PurchaseHistoryRepository;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleRepository;
import ru.geroldina.ftauctionbot.client.application.market.MarketResearchManager;
import ru.geroldina.ftauctionbot.client.application.market.MarketResearchRepository;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutobuyConfigPresenterTest {
    @Test
    void requestCloseWithDirtyDraftShowsConfirmation() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();

        context.presenter.updateRuleName(context.session.draft().buyRules.getFirst(), "Updated");
        context.presenter.requestClose();

        assertTrue(context.session.confirmClosePending());
        assertEquals(0, context.closeCalls.get());
    }

    @Test
    void saveInvalidDraftKeepsSessionDirtyAndDoesNotPersist() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();
        AutobuyConfigDraft.BuyRuleDraft rule = context.session.draft().buyRules.getFirst();

        context.presenter.updateRuleId(rule, "");
        context.presenter.save();

        assertTrue(context.session.dirty());
        assertTrue(context.session.statusMessage().contains("Сначала исправьте ошибки"));
        assertEquals("totem_rule", context.configManager.getCurrentConfig().buyRules().getFirst().id());
    }

    @Test
    void saveValidDraftPersistsAndResetsDirtyState() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();
        AutobuyConfigDraft.BuyRuleDraft rule = context.session.draft().buyRules.getFirst();

        context.presenter.updateRuleName(rule, "Updated Rule");
        context.presenter.save();

        assertFalse(context.session.dirty());
        assertEquals("Updated Rule", context.configManager.getCurrentConfig().buyRules().getFirst().name());
    }

    @Test
    void createDuplicateDeleteAndMoveRuleMaintainSelection() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();

        context.presenter.createRule();
        assertEquals(1, context.session.selectedRuleIndex());

        context.presenter.moveSelectedRule(-1);
        assertEquals(0, context.session.selectedRuleIndex());

        context.presenter.duplicateSelectedRule();
        assertEquals(1, context.session.selectedRuleIndex());

        context.presenter.deleteSelectedRule();
        assertEquals(1, context.session.selectedRuleIndex());
    }

    @Test
    void pickerSelectionUpdatesDraftAndClosesOverlay() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();
        AutobuyConfigDraft.ConditionDraft condition = context.session.draft().buyRules.getFirst().conditions.getFirst();
        SearchPickerEntry entry = new SearchPickerEntry(
            "minecraft:stone",
            AutobuyUiTextSupport.uiText("Stone"),
            "stone minecraft:stone",
            null,
            null,
            null,
            null,
            0
        );
        context.session.activePicker(new SearchPickerState("Test", List.of(entry), value -> condition.stringValue = value));

        context.presenter.selectPickerEntry(entry);

        assertEquals("minecraft:stone", condition.stringValue);
        assertNull(context.session.activePicker());
    }

    @Test
    void toggleAutobuyDelegatesToLoopControl() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();

        context.presenter.toggleAutobuy();
        assertTrue(context.loopControl.enabled);
        assertTrue(context.session.statusMessage().contains("запущен"));

        context.presenter.toggleAutobuy();
        assertFalse(context.loopControl.enabled);
        assertTrue(context.session.statusMessage().contains("остановлен"));
    }

    @Test
    void selectHistoryTabLoadsPurchaseHistory() {
        TestContext context = new TestContext(sampleConfig());
        context.presenter.initialize();

        context.historyRepository.entries = List.of(new PurchaseHistoryEntry("minecraft:stone", "Stone", 3, 1200, 123456789L));
        context.presenter.selectTab(AutobuyScreenTab.PURCHASE_HISTORY);

        assertEquals(AutobuyScreenTab.PURCHASE_HISTORY, context.session.activeTab());
        assertEquals(1, context.session.purchaseHistoryEntries().size());
    }

    private static AutobuyConfig sampleConfig() {
        return new AutobuyConfig(
            30,
            10,
            200,
            AutobuyScanLogMode.MATCHED_ONLY,
            15,
            5,
            List.of(BuyRule.of("totem_rule", "Totem Rule", true, new ItemIdCondition("minecraft:totem_of_undying")))
        );
    }

    private static final class TestContext {
        private final AtomicInteger rebuildCalls = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();
        private final TestLoopControl loopControl = new TestLoopControl();
        private final AutobuyConfigManager configManager;
        private final InMemoryPurchaseHistoryRepository historyRepository = new InMemoryPurchaseHistoryRepository();
        private final InMemoryMarketResearchRepository marketResearchRepository = new InMemoryMarketResearchRepository();
        private final AutobuyConfigSession session = new AutobuyConfigSession(new AutobuyConfigValidator());
        private final AutobuyConfigPresenter presenter;

        private TestContext(AutobuyConfig initialConfig) {
            InMemoryRepository repository = new InMemoryRepository(initialConfig);
            this.configManager = new AutobuyConfigManager(repository, new NoopLogger());
            this.configManager.loadStartup();
            this.presenter = new AutobuyConfigPresenter(
                configManager,
                loopControl,
                new PurchaseHistoryManager(historyRepository, new NoopLogger()),
                new MarketResearchManager(new NoopScanController(), marketResearchRepository, new NoopLogger()),
                session,
                new AutobuyPickerCatalog(),
                rebuildCalls::incrementAndGet,
                closeCalls::incrementAndGet
            );
        }
    }

    private static final class TestLoopControl implements AutobuyLoopControl {
        private boolean enabled;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void start() {
            enabled = true;
        }

        @Override
        public void stop() {
            enabled = false;
        }
    }

    private static final class InMemoryRepository implements AutobuyRuleRepository {
        private AutobuyConfig stored;

        private InMemoryRepository(AutobuyConfig stored) {
            this.stored = stored;
        }

        @Override
        public AutobuyConfig load() {
            return stored;
        }

        @Override
        public void save(AutobuyConfig config) {
            stored = config;
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

    private static final class InMemoryPurchaseHistoryRepository implements PurchaseHistoryRepository {
        private List<PurchaseHistoryEntry> entries = List.of();

        @Override
        public List<PurchaseHistoryEntry> load() {
            return entries;
        }

        @Override
        public void save(List<PurchaseHistoryEntry> entries) {
            this.entries = entries;
        }
    }

    private static final class InMemoryMarketResearchRepository implements MarketResearchRepository {
        private List<MarketResearchResult> entries = List.of();

        @Override
        public List<MarketResearchResult> load() {
            return entries;
        }

        @Override
        public void save(List<MarketResearchResult> results) {
            this.entries = results;
        }
    }

    private static final class NoopScanController implements ru.geroldina.ftauctionbot.client.application.scan.AuctionScanController {
        @Override
        public void startScan(int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs) {
        }

        @Override
        public void startScanCommand(String command, int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs) {
        }

        @Override
        public boolean isIdle() {
            return true;
        }

        @Override
        public void addPageObserver(ru.geroldina.ftauctionbot.client.application.scan.AuctionScanPageObserver observer) {
        }

        @Override
        public void addLifecycleObserver(ru.geroldina.ftauctionbot.client.application.scan.AuctionScanLifecycleObserver observer) {
        }
    }
}
