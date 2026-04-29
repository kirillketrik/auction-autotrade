package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public record MaxTotalPriceCondition(Long value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return value != null && lot.totalPrice() > value
            ? ConditionMatchResult.failure("price exceeds maxTotalPrice")
            : ConditionMatchResult.success();
    }
}
