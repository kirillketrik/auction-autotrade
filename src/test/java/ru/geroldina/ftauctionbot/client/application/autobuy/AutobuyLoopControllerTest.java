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
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxUnitPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutobuyLoopControllerTest {
    @Test
    void startsScanAfterBalanceRefresh() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 2, 7, 350, 90, AutobuyScanLogMode.MATCHED_ONLY, true, 7, 20, 15, 5, List.of(
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
            new PurchaseHistoryManager(new InMemoryPurchaseHistoryRepository(), new FakeLogger()),
            new FakeLogger(),
            new Random(0)
        );

        controller.start();
        controller.onClientTick();
        assertTrue(balanceService.refreshRequested);

        controller.onBalanceUpdated(new MoneySnapshot(10_000_000L, Instant.now()));
        assertEquals(7, scanController.lastStartedMaxPages);
        assertEquals("ah", scanController.lastCommand);
        assertEquals(350, scanController.lastPageSwitchDelayMs);
        assertEquals(90, scanController.lastPageSwitchDelayJitterMs);
    }

    @Test
    void buysMatchingLotAndContinuesPageProcessing() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 2, 7, 200, 50, AutobuyScanLogMode.MATCHED_ONLY, true, 7, 20, 15, 5, List.of(
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
            new PurchaseHistoryManager(new InMemoryPurchaseHistoryRepository(), new FakeLogger()),
            new FakeLogger(),
            new Random(0)
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
    void usesSingleGenericAuctionScanForAutobuyRules() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 2, 7, 200, 50, AutobuyScanLogMode.MATCHED_ONLY, true, 7, 20, 15, 5, List.of(
                BuyRule.of("stone", "Булыжник", true, new ItemIdCondition("minecraft:stone")),
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
            new PurchaseHistoryManager(new InMemoryPurchaseHistoryRepository(), new FakeLogger()),
            new FakeLogger(),
            new Random(0)
        );

        controller.start();
        controller.onClientTick();
        controller.onBalanceUpdated(new MoneySnapshot(10_000_000L, Instant.now()));

        assertEquals("ah", scanController.lastCommand);
        assertEquals(7, scanController.lastStartedMaxPages);
    }

    @Test
    void performsAntiAfkMovementWhileWaitingForNextCycle() {
        FakeGateway gateway = new FakeGateway();
        FakeScanController scanController = new FakeScanController();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(
            () -> new AutobuyConfig(5, 2, 7, 200, 50, AutobuyScanLogMode.MATCHED_ONLY, true, 1, 0, 15, 5, List.of(
                BuyRule.of("stone", "Stone", true, new ItemIdCondition("minecraft:stone"))
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
            new PurchaseHistoryManager(new InMemoryPurchaseHistoryRepository(), new FakeLogger()),
            new FakeLogger(),
            new FixedIntRandom(0)
        );

        controller.start();
        controller.onClientTick();
        controller.onBalanceUpdated(new MoneySnapshot(10_000_000L, Instant.now()));
        controller.onClientTick();
        controller.onClientTick();

        for (int index = 0; index < 21; index++) {
            controller.onClientTick();
        }

        assertTrue(gateway.movements.contains(AntiAfkMoveDirection.FORWARD));
    }

    private static final class FakeGateway implements AuctionClientGateway {
        private int syncId = -1;
        private int slot = -1;
        private SlotActionType actionType;
        private boolean closeRequested;
        private final List<AntiAfkMoveDirection> movements = new java.util.ArrayList<>();
        private int stopMovementCalls;
        private int jumpCalls;

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

        @Override
        public boolean canPerformAntiAfkActions() {
            return true;
        }

        @Override
        public void applyAntiAfkMovement(AntiAfkMoveDirection direction) {
            movements.add(direction);
        }

        @Override
        public void stopAntiAfkMovement() {
            stopMovementCalls++;
        }

        @Override
        public void jump() {
            jumpCalls++;
        }
    }

    private static final class FakeScanController implements AuctionScanController {
        private int lastStartedMaxPages = -1;
        private int lastPageSwitchDelayMs = -1;
        private int lastPageSwitchDelayJitterMs = -1;
        private String lastCommand;

        @Override
        public void startScan(int maxPages) {
            lastStartedMaxPages = maxPages;
            lastCommand = "ah";
        }

        @Override
        public void startScan(int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs) {
            lastStartedMaxPages = maxPages;
            lastCommand = "ah";
            lastPageSwitchDelayMs = pageSwitchDelayMs;
            lastPageSwitchDelayJitterMs = pageSwitchDelayJitterMs;
        }

        @Override
        public void startScanCommand(String command, int maxPages) {
            lastCommand = command;
            lastStartedMaxPages = maxPages;
        }

        @Override
        public void startScanCommand(String command, int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs) {
            lastCommand = command;
            lastStartedMaxPages = maxPages;
            lastPageSwitchDelayMs = pageSwitchDelayMs;
            lastPageSwitchDelayJitterMs = pageSwitchDelayJitterMs;
        }

        @Override
        public boolean isIdle() {
            return true;
        }

        @Override
        public void addPageObserver(AuctionScanPageObserver observer) {
        }

        @Override
        public void addLifecycleObserver(ru.geroldina.ftauctionbot.client.application.scan.AuctionScanLifecycleObserver observer) {
        }
    }

    private static final class FixedIntRandom extends Random {
        private final int value;

        private FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
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

    private static final class InMemoryPurchaseHistoryRepository implements PurchaseHistoryRepository {
        @Override
        public List<ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry> load() {
            return List.of();
        }

        @Override
        public void save(List<ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry> entries) {
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
