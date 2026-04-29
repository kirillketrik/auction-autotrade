package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

public record BuyDecision(
    boolean approved,
    BuyRule matchedRule,
    String reason
) {
    public static BuyDecision approved(BuyRule matchedRule) {
        return new BuyDecision(true, matchedRule, "matched");
    }

    public static BuyDecision rejected(String reason) {
        return new BuyDecision(false, null, reason);
    }
}
