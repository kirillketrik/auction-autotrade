package ru.geroldina.ftauctionbot.client.application.balance;

import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneyParser;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class BalanceTracker implements BalanceService, MinecraftClientEventListener {
    private static final int BALANCE_TIMEOUT_TICKS = 100;

    private final AuctionClientGateway gateway;
    private final MoneyParser parser;
    private final ScanLogger logger;
    private final List<BalanceObserver> observers = new ArrayList<>();

    private MoneySnapshot lastKnownBalance;
    private boolean awaitingRefresh;
    private int timeoutTicks;

    public BalanceTracker(AuctionClientGateway gateway, MoneyParser parser, ScanLogger logger) {
        this.gateway = gateway;
        this.parser = parser;
        this.logger = logger;
    }

    @Override
    public Optional<MoneySnapshot> getLastKnownBalance() {
        return Optional.ofNullable(lastKnownBalance);
    }

    @Override
    public boolean isAwaitingRefresh() {
        return awaitingRefresh;
    }

    @Override
    public boolean requestRefresh() {
        if (!gateway.isReady()) {
            logger.info("BALANCE", "Balance refresh ignored because client is not ready.");
            return false;
        }

        if (awaitingRefresh) {
            return false;
        }

        awaitingRefresh = true;
        timeoutTicks = BALANCE_TIMEOUT_TICKS;
        gateway.sendChatCommand("money");
        logger.info("BALANCE", "Requested balance refresh via /money.");
        return true;
    }

    @Override
    public void addObserver(BalanceObserver observer) {
        observers.add(observer);
    }

    @Override
    public void onClientTick() {
        if (!awaitingRefresh) {
            return;
        }

        timeoutTicks--;
        if (timeoutTicks > 0) {
            return;
        }

        awaitingRefresh = false;
        logger.info("BALANCE", "Balance refresh timed out.");
        for (BalanceObserver observer : observers) {
            observer.onBalanceRefreshFailed("timeout");
        }
    }

    @Override
    public void onGameMessage(String message, boolean overlay) {
        handlePotentialBalanceMessage(message);
    }

    @Override
    public void onProfilelessChatMessage(String message) {
        handlePotentialBalanceMessage(message);
    }

    @Override
    public void onChatMessage(String message) {
        handlePotentialBalanceMessage(message);
    }

    private void handlePotentialBalanceMessage(String message) {
        OptionalLong parsedBalance = parser.parseBalance(message);
        if (parsedBalance.isEmpty()) {
            return;
        }

        lastKnownBalance = new MoneySnapshot(parsedBalance.getAsLong(), Instant.now());
        awaitingRefresh = false;
        timeoutTicks = 0;
        logger.info("BALANCE", "Updated balance to $" + parsedBalance.getAsLong() + ".");

        for (BalanceObserver observer : observers) {
            observer.onBalanceUpdated(lastKnownBalance);
        }
    }
}
