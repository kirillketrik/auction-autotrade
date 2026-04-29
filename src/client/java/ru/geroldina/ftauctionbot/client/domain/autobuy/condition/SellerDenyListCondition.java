package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.List;

public record SellerDenyListCondition(List<String> value) implements BuyRuleCondition {
    public SellerDenyListCondition {
        value = value == null ? List.of() : List.copyOf(value);
    }

    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionSupport.containsNormalized(value, lot.seller())
            ? ConditionMatchResult.failure("seller denied")
            : ConditionMatchResult.success();
    }
}
