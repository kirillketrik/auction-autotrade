package ru.geroldina.ftauctionbot.client.domain.balance;

import java.time.Instant;

public record MoneySnapshot(
    long amount,
    Instant observedAt
) {
}
