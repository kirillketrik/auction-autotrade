package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

public record BuyAttemptResult(
    boolean successful,
    boolean pending,
    String message
) {
    public static BuyAttemptResult success(String message) {
        return new BuyAttemptResult(true, false, message);
    }

    public static BuyAttemptResult failure(String message) {
        return new BuyAttemptResult(false, false, message);
    }

    public static BuyAttemptResult pending(String message) {
        return new BuyAttemptResult(false, true, message);
    }
}
