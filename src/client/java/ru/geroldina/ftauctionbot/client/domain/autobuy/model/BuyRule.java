package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;

import java.util.List;

public record BuyRule(
    String id,
    String name,
    Boolean enabled,
    List<BuyRuleCondition> conditions
) {
    public BuyRule {
        enabled = enabled == null || enabled;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public static BuyRule of(String id, String name, Boolean enabled, BuyRuleCondition... conditions) {
        return new BuyRule(id, name, enabled, List.of(conditions));
    }
}
