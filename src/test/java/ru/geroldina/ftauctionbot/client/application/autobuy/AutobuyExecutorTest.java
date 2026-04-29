package ru.geroldina.ftauctionbot.client.application.autobuy;

import net.minecraft.screen.slot.SlotActionType;
import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceObserver;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceService;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyAttemptResult;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyCandidate;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutobuyExecutorTest {
    @Test
    void sendsQuickMoveForApprovedBuyAttempt() {
        FakeGateway gateway = new FakeGateway();
        FakeBalanceService balanceService = new FakeBalanceService();
        AutobuyConfigManager configManager = new AutobuyConfigManager(() -> new AutobuyConfig(30, 10, AutobuyScanLogMode.MATCHED_ONLY, List.of(
            new BuyRule("totem", "Totem", true, "minecraft:totem_of_undying", null, null, 7_000_000L, null, null, null, List.of(), List.of(), List.of(), List.of())
        )), new FakeLogger());
        configManager.loadStartup();
        AutobuyExecutor executor = new AutobuyExecutor(
            gateway,
            new FakeAuctionViewTracker(),
            balanceService,
            configManager,
            (lot, rules) -> BuyDecision.approved(rules.getFirst()),
            new FakeLogger()
        );

        BuyAttemptResult result = executor.attemptBuySlot(12);
        assertTrue(result.pending());

        balanceService.pushBalance(10_000_000L);

        assertEquals(18, gateway.syncId);
        assertEquals(12, gateway.slotId);
        assertEquals(0, gateway.button);
        assertEquals(SlotActionType.QUICK_MOVE, gateway.actionType);
    }

    private static final class FakeAuctionViewTracker implements AuctionViewReader {
        @Override
        public Optional<BuyCandidate> getCandidateAtSlot(int slot) {
            return Optional.of(new BuyCandidate(
                18,
                1,
                slot,
                new AuctionLot(1, slot, "minecraft:totem_of_undying", "Totem", 1, 6_800_000L, 6_800_000L, "DoctorInsane", List.of(), List.of())
            ));
        }

        @Override
        public boolean isCurrentScreen(int syncId) {
            return syncId == 18;
        }
    }

    private static final class FakeGateway implements AuctionClientGateway {
        private int syncId = -1;
        private int slotId = -1;
        private int button = -1;
        private SlotActionType actionType;

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
            this.slotId = slotId;
            this.button = button;
            this.actionType = actionType;
        }
    }

    private static final class FakeBalanceService implements BalanceService {
        private BalanceObserver observer;

        @Override
        public Optional<MoneySnapshot> getLastKnownBalance() {
            return Optional.empty();
        }

        @Override
        public boolean isAwaitingRefresh() {
            return false;
        }

        @Override
        public boolean requestRefresh() {
            return true;
        }

        @Override
        public void addObserver(BalanceObserver observer) {
            this.observer = observer;
        }

        void pushBalance(long amount) {
            observer.onBalanceUpdated(new MoneySnapshot(amount, Instant.now()));
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
}
