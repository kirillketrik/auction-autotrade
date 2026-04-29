package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

public record ConditionMatchResult(
    boolean matched,
    String reason
) {
    public static ConditionMatchResult success() {
        return new ConditionMatchResult(true, "matched");
    }

    public static ConditionMatchResult failure(String reason) {
        return new ConditionMatchResult(false, reason);
    }
}
