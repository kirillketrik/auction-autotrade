package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public record MaxUnitPriceCondition(Long value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return value != null && lot.unitPrice() > value
            ? ConditionMatchResult.failure("price exceeds maxUnitPrice")
            : ConditionMatchResult.success();
    }
}
