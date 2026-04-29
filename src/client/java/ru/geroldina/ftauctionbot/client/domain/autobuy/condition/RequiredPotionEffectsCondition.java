package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.util.List;

public record RequiredPotionEffectsCondition(List<RequiredPotionEffect> value) implements BuyRuleCondition {
    public RequiredPotionEffectsCondition {
        value = value == null ? List.of() : List.copyOf(value);
    }

    @Override
    public ConditionMatchResult matches(AuctionLot lot) {
        return ConditionSupport.matchesRequiredPotionEffects(lot.potionEffects(), value)
            ? ConditionMatchResult.success()
            : ConditionMatchResult.failure("required potion effects mismatch");
    }
}
