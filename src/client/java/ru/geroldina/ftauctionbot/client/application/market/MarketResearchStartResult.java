package ru.geroldina.ftauctionbot.client.application.market;

public record MarketResearchStartResult(
    boolean started,
    String message
) {
    public static MarketResearchStartResult started(String message) {
        return new MarketResearchStartResult(true, message);
    }

    public static MarketResearchStartResult rejected(String message) {
        return new MarketResearchStartResult(false, message);
    }
}
