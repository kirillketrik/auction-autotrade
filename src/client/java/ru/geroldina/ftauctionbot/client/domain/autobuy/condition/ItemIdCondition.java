package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public record ItemIdCondition(String minecraftId) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        if (minecraftId == null || minecraftId.isBlank()) {
            return ConditionMatchResult.failure("minecraftId is required");
        }

        return ConditionSupport.normalizeMinecraftId(minecraftId).equals(ConditionSupport.normalizeMinecraftId(lot.minecraftId()))
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("minecraftId mismatch");
    }
}
