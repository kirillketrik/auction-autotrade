package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.Optional;

public record DisplayNameContainsCondition(String value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionSupport.normalizeFreeText(lot.displayName()).contains(ConditionSupport.normalizeFreeText(value))
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("displayNameContains mismatch");
    }

    @Override
    public Optional<String> searchQuery() {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }
}
