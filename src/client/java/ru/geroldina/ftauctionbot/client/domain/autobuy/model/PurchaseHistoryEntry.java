package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

public record PurchaseHistoryEntry(
    String minecraftId,
    String displayName,
    int count,
    long totalPrice,
    long purchasedAtEpochMillis
) {
}
