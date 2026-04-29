package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public record MaxCountCondition(Integer value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return value != null && lot.count() > value
            ? ConditionMatchResult.failure("count above maxCount")
            : ConditionMatchResult.success();
    }
}
