package ru.geroldina.ftauctionbot.client.domain.autobuy;

import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.auction.model.EnchantmentData;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PotionEffectData;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.DisplayNameCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxTotalPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredEnchantmentsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredPotionEffectsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.SellerAllowListCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAutobuyRuleMatcherTest {
    private final DefaultAutobuyRuleMatcher matcher = new DefaultAutobuyRuleMatcher();

    @Test
    void approvesMatchingRule() {
        BuyRule rule = BuyRule.of(
            "totem",
            "Totem",
            true,
            new ItemIdCondition("minecraft:totem_of_undying"),
            new DisplayNameCondition("Талисман"),
            new MaxTotalPriceCondition(7_000_000L)
        );
        AuctionLot lot = new AuctionLot(1, 5, "minecraft:totem_of_undying", "[★] Талисман Вихря", 1, 6_800_000L, 6_800_000L, "DoctorInsane", List.of(), List.of());

        BuyDecision decision = matcher.match(lot, List.of(rule));

        assertTrue(decision.approved());
    }

    @Test
    void rejectsWhenPriceExceedsLimit() {
        BuyRule rule = BuyRule.of("totem", "Totem", true, new ItemIdCondition("minecraft:totem_of_undying"), new MaxTotalPriceCondition(6_000_000L));
        AuctionLot lot = new AuctionLot(1, 5, "minecraft:totem_of_undying", "[★] Талисман Вихря", 1, 6_800_000L, 6_800_000L, "DoctorInsane", List.of(), List.of());

        BuyDecision decision = matcher.match(lot, List.of(rule));

        assertFalse(decision.approved());
    }

    @Test
    void treatsEnabledAsTrueWhenOmitted() {
        BuyRule rule = BuyRule.of("totem", null, null, new ItemIdCondition("minecraft:totem_of_undying"));
        AuctionLot lot = new AuctionLot(1, 5, "minecraft:totem_of_undying", "Totem", 1, 100L, 100L, null, List.of(), List.of());

        BuyDecision decision = matcher.match(lot, List.of(rule));

        assertTrue(decision.approved());
    }

    @Test
    void matchesRequiredEnchantmentsAndPotionEffects() {
        BuyRule rule = new BuyRule(
            "potion",
            "Potion rule",
            true,
            List.of(
                new ItemIdCondition("minecraft:splash_potion"),
                new RequiredEnchantmentsCondition(List.of(new RequiredEnchantment("minecraft:channeling", 1))),
                new RequiredPotionEffectsCondition(List.of(new RequiredPotionEffect("minecraft:strength", 3, 1200)))
            )
        );
        AuctionLot lot = new AuctionLot(
            1,
            4,
            "minecraft:splash_potion",
            "[★] Зелье Ассасина",
            1,
            1_300_000L,
            1_300_000L,
            null,
            List.of(new EnchantmentData("minecraft:channeling", 1)),
            List.of(new PotionEffectData("minecraft:strength", 3, 1200))
        );

        BuyDecision decision = matcher.match(lot, List.of(rule));

        assertTrue(decision.approved());
    }

    @Test
    void ignoresDisplayNameConditionDuringBuyMatching() {
        BuyRule rule = new BuyRule(
            "totem",
            "Totem",
            true,
            List.of(
                new ItemIdCondition("TOTEM_OF_UNDYING"),
                new DisplayNameCondition("Совсем другое имя"),
                new MaxTotalPriceCondition(7_000_000L),
                new SellerAllowListCondition(List.of("doctorinsane"))
            )
        );
        AuctionLot lot = new AuctionLot(
            1,
            12,
            "minecraft:totem_of_undying",
            "Неважное название",
            1,
            6_800_000L,
            6_800_000L,
            "DoctorInsane",
            List.of(),
            List.of()
        );

        BuyDecision decision = matcher.match(lot, List.of(rule));

        assertTrue(decision.approved());
    }
}
