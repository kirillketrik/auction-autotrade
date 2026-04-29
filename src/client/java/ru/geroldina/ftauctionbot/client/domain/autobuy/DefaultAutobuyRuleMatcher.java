package ru.geroldina.ftauctionbot.client.domain.autobuy;

import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleMatcher;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ConditionMatchResult;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;

import java.util.List;

public final class DefaultAutobuyRuleMatcher implements AutobuyRuleMatcher {
    @Override
    public BuyDecision match(AuctionLot lot, List<BuyRule> rules) {
        for (BuyRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }

            BuyDecision decision = matchRule(lot, rule);
            if (decision.approved()) {
                return decision;
            }
        }

        return BuyDecision.rejected("no matching rule");
    }

    private BuyDecision matchRule(AuctionLot lot, BuyRule rule) {
        if (rule.conditions().isEmpty()) {
            return BuyDecision.rejected("rule has no conditions");
        }

        for (BuyRuleCondition condition : rule.conditions()) {
            ConditionMatchResult result = condition.matches(lot);
            if (!result.matched()) {
                return BuyDecision.rejected(result.reason());
            }
        }

        return BuyDecision.approved(rule);
    }
}
