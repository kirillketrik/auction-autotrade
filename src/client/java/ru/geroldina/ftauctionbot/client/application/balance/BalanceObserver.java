package ru.geroldina.ftauctionbot.client.application.balance;

import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

public interface BalanceObserver {
    default void onBalanceUpdated(MoneySnapshot snapshot) {
    }

    default void onBalanceRefreshFailed(String reason) {
    }
}
