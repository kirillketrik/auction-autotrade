package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.Optional;

public interface BuyRuleCondition {
    ConditionMatchResult matches(AuctionLot lot);

    default Optional<String> searchQuery() {
        return Optional.empty();
    }
}
