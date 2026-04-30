package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.Optional;

public record DisplayNameCondition(String value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionMatchResult.success();
    }

    @Override
    public Optional<String> searchQuery() {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }
}
