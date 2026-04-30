package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutobuyConfigValidatorTest {
    @Test
    void rejectsRuleWithoutId() {
        AutobuyConfigDraft draft = AutobuyConfigDraft.fromDomain(new AutobuyConfig(
            30,
            10,
            200,
            AutobuyScanLogMode.MATCHED_ONLY,
            15,
            5,
            List.of(new BuyRule(null, "Rule", true, List.of()))
        ));

        AutobuyValidationResult result = new AutobuyConfigValidator().validate(draft);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("требуется id")));
    }

    @Test
    void acceptsDefaultConfigWithoutRules() {
        AutobuyConfigDraft draft = AutobuyConfigDraft.fromDomain(AutobuyConfig.empty());

        AutobuyValidationResult result = new AutobuyConfigValidator().validate(draft);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }
}
