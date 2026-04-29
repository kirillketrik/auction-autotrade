package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;

import java.util.List;

public record RequiredEnchantmentsCondition(List<RequiredEnchantment> value) implements BuyRuleCondition {
    public RequiredEnchantmentsCondition {
        value = value == null ? List.of() : List.copyOf(value);
    }

    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionSupport.matchesRequiredEnchantments(lot.enchantments(), value)
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("required enchantments mismatch");
    }
}
