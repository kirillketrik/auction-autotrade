package ru.geroldina.ftauctionbot.client.application.balance;

import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

import java.util.Optional;

public interface BalanceService {
    Optional<MoneySnapshot> getLastKnownBalance();

    boolean isAwaitingRefresh();

    boolean requestRefresh();

    void addObserver(BalanceObserver observer);
}
