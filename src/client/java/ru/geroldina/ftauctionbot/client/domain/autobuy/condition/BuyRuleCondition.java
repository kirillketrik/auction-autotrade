package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public interface BuyRuleCondition {
    ConditionMatchResult matches(AuctionLot lot);
}
