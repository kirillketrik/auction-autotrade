package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.List;

public record SellerAllowListCondition(List<String> value) implements BuyRuleCondition {
    public SellerAllowListCondition {
        value = value == null ? List.of() : List.copyOf(value);
    }

    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return value.isEmpty() || ConditionSupport.containsNormalized(value, lot.seller())
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("seller not allowed");
    }
}
