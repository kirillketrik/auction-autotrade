package ru.geroldina.ftauctionbot.client.application.autobuy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceObserver;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceService;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionPageDecision;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanController;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanPageObserver;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.DefaultAutobuyRuleMatcher;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.DisplayNameCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxUnitPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutobuyLoopControllerTest {
    @Test
    void startsScanAfterBalanceRefresh() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 7, 350, AutobuyScanLogMode.MATCHED_ONLY, List.of(
                BuyRule.of("generic", "Generic", true, new ItemIdCondition("minecraft:stone"))
            )),
            new FakeLogger()
        );
        configManager.loadStartup();

        AutobuyLoopController controller = new AutobuyLoopController(
            gateway,
            scanController,
            balanceService,
            configManager,
            new DefaultAutobuyRuleMatcher(),
            new FakeLogger()
        );

        controller.start();
        controller.onClientTick();
        assertTrue(balanceService.refreshRequested);

        controller.onBalanceUpdated(new MoneySnapshot(10_000_000L, Instant.now()));
        assertEquals(7, scanController.lastStartedMaxPages);
        assertEquals("ah", scanController.lastCommand);
        assertEquals(350, scanController.lastPageSwitchDelayMs);
    }

    @Test
    void buysMatchingLotAndContinuesPageProcessing() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 7, 200, AutobuyScanLogMode.MATCHED_ONLY, List.of(
                BuyRule.of("totem", "Totem", true, new ItemIdCondition("minecraft:totem_of_undying"), new MaxUnitPriceCondition(7_000_000L))
            )),
            new FakeLogger()
        );
        configManager.loadStartup();

        AutobuyLoopController controller = new AutobuyLoopController(
            gateway,
            scanController,
            balanceService,
            configManager,
            new DefaultAutobuyRuleMatcher(),
            new FakeLogger()
        );
        controller.start();
        balanceService.balance = Optional.of(new MoneySnapshot(10_000_000L, Instant.now()));

        AuctionPageDecision decision = controller.onPageScanned(
            18,
            1,
            100,
            List.of(new AuctionLot(1, 12, "minecraft:totem_of_undying", "Totem", 1, 6_800_000L, 6_800_000L, "DoctorInsane", List.of(), List.of()))
        );

        assertEquals(AuctionPageDecision.CONTINUE, decision);
        assertEquals(18, gateway.syncId);
        assertEquals(12, gateway.slot);
        assertEquals(SlotActionType.QUICK_MOVE, gateway.actionType);
        assertTrue(controller.shouldSuppressScreen(new FakeScreen("Аукцион")));
        controller.onCloseScreen(18);
    }

    @Test
    void prefersSearchTasksForRulesWithDisplayName() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 7, 200, AutobuyScanLogMode.MATCHED_ONLY, List.of(
                BuyRule.of("stone", "Stone", true, new ItemIdCondition("minecraft:stone"), new DisplayNameCondition("Булыжник")),
                BuyRule.of("generic", "Generic", true, new ItemIdCondition("minecraft:dirt"))
            )),
            new FakeLogger()
        );
        configManager.loadStartup();

        AutobuyLoopController controller = new AutobuyLoopController(
            gateway,
            scanController,
            balanceService,
            configManager,
            new DefaultAutobuyRuleMatcher(),
            new FakeLogger()
        );

        controller.start();
        controller.onClientTick();
        controller.onBalanceUpdated(new MoneySnapshot(10_000_000L, Instant.now()));

        assertEquals("ah search Булыжник", scanController.lastCommand);
    }

    private static final class FakeGateway implements AuctionClientGateway {
        private int syncId = -1;
        private int slot = -1;
        private SlotActionType actionType;
        private boolean closeRequested;

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void sendChatCommand(String command) {
        }

        @Override
        public void sendOpenAuctionCommand() {
        }

        @Override
        public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType) {
            this.syncId = syncId;
            this.slot = slotId;
            this.actionType = actionType;
        }

        @Override
        public boolean closeActiveHandledScreen() {
            closeRequested = true;
            return false;
        }
    }

    private static final class FakeScanController implements AuctionScanController {
        private int lastStartedMaxPages = -1;
        private int lastPageSwitchDelayMs = -1;
        private String lastCommand;

        @Override
        public void startScan(int maxPages) {
            lastStartedMaxPages = maxPages;
            lastCommand = "ah";
        }

        @Override
        public void startScan(int maxPages, int pageSwitchDelayMs) {
            lastStartedMaxPages = maxPages;
            lastCommand = "ah";
            lastPageSwitchDelayMs = pageSwitchDelayMs;
        }

        @Override
        public void startScanCommand(String command, int maxPages) {
            lastCommand = command;
            lastStartedMaxPages = maxPages;
        }

        @Override
        public void startScanCommand(String command, int maxPages, int pageSwitchDelayMs) {
            lastCommand = command;
            lastStartedMaxPages = maxPages;
            lastPageSwitchDelayMs = pageSwitchDelayMs;
        }

        @Override
        public boolean isIdle() {
            return true;
        }

        @Override
        public void addPageObserver(AuctionScanPageObserver observer) {
        }
    }

    private static final class FakeBalanceService implements BalanceService {
        private boolean refreshRequested;
        private Optional<MoneySnapshot> balance = Optional.empty();
        private BalanceObserver observer;

        @Override
        public Optional<MoneySnapshot> getLastKnownBalance() {
            return balance;
        }

        @Override
        public boolean isAwaitingRefresh() {
            return false;
        }

        @Override
        public boolean requestRefresh() {
            refreshRequested = true;
            return true;
        }

        @Override
        public void addObserver(BalanceObserver observer) {
            this.observer = observer;
        }
    }

    private static final class FakeLogger implements ScanLogger {
        @Override
        public void info(String category, String message) {
        }

        @Override
        public void block(String category, List<String> lines) {
        }
    }

    private static final class FakeScreen extends Screen {
        FakeScreen(String title) {
            super(Text.literal(title));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        }
    }
}
