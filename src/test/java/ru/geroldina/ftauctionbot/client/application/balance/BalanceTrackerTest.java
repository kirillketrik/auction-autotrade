package ru.geroldina.ftauctionbot.client.application.balance;

import org.junit.jupiter.api.Test;
import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneyParser;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceTrackerTest {
    @Test
    void requestsMoneyCommandAndStoresParsedBalance() {
        FakeGateway gateway = new FakeGateway();
        BalanceTracker tracker = new BalanceTracker(gateway, new MoneyParser(), new FakeLogger());

        assertTrue(tracker.requestRefresh());
        assertEquals(List.of("money"), gateway.commands);

        tracker.onGameMessage("[$] Ваш баланс: $3,881,093", false);

        Optional<MoneySnapshot> snapshot = tracker.getLastKnownBalance();
        assertTrue(snapshot.isPresent());
        assertEquals(3_881_093L, snapshot.get().amount());
        assertFalse(tracker.isAwaitingRefresh());
    }

    @Test
    void timesOutAwaitingBalanceResponse() {
        BalanceTracker tracker = new BalanceTracker(new FakeGateway(), new MoneyParser(), new FakeLogger());

        tracker.requestRefresh();
        for (int index = 0; index < 100; index++) {
            tracker.onClientTick();
        }

        assertFalse(tracker.isAwaitingRefresh());
    }

    private static final class FakeGateway implements AuctionClientGateway {
        private final List<String> commands = new ArrayList<>();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void sendChatCommand(String command) {
            commands.add(command);
        }

        @Override
        public void sendOpenAuctionCommand() {
        }

        @Override
        public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType) {
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
