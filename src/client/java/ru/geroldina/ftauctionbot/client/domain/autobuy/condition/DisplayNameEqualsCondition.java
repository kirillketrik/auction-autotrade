package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.Optional;

public record DisplayNameEqualsCondition(String value) implements BuyRuleCondition {
    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionSupport.normalizeFreeText(value).equals(ConditionSupport.normalizeFreeText(lot.displayName()))
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("displayNameEquals mismatch");
    }

    @Override
    public Optional<String> searchQuery() {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }
}
