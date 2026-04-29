package ru.geroldina.ftauctionbot.client.domain.autobuy;

import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleMatcher;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.auction.model.EnchantmentData;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PotionEffectData;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.util.List;
import java.util.Locale;

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
        if (rule.minecraftId() == null || rule.minecraftId().isBlank()) {
            return BuyDecision.rejected("minecraftId is required");
        }

        if (!rule.minecraftId().equals(lot.minecraftId())) {
            return BuyDecision.rejected("minecraftId mismatch");
        }

        if (rule.displayNameEquals() != null && !rule.displayNameEquals().equals(lot.displayName())) {
            return BuyDecision.rejected("displayNameEquals mismatch");
        }

        if (rule.displayNameContains() != null
            && !lot.displayName().toLowerCase(Locale.ROOT).contains(rule.displayNameContains().toLowerCase(Locale.ROOT))) {
            return BuyDecision.rejected("displayNameContains mismatch");
        }

        if (rule.maxTotalPrice() != null && lot.totalPrice() > rule.maxTotalPrice()) {
            return BuyDecision.rejected("price exceeds maxTotalPrice");
        }

        if (rule.maxUnitPrice() != null && lot.unitPrice() > rule.maxUnitPrice()) {
            return BuyDecision.rejected("price exceeds maxUnitPrice");
        }

        if (rule.minCount() != null && lot.count() < rule.minCount()) {
            return BuyDecision.rejected("count below minCount");
        }

        if (rule.maxCount() != null && lot.count() > rule.maxCount()) {
            return BuyDecision.rejected("count above maxCount");
        }

        if (!matchesRequiredEnchantments(lot.enchantments(), rule.requiredEnchantments())) {
            return BuyDecision.rejected("required enchantments mismatch");
        }

        if (!matchesRequiredPotionEffects(lot.potionEffects(), rule.requiredPotionEffects())) {
            return BuyDecision.rejected("required potion effects mismatch");
        }

        if (!rule.sellerAllowList().isEmpty() && (lot.seller() == null || !rule.sellerAllowList().contains(lot.seller()))) {
            return BuyDecision.rejected("seller not allowed");
        }

        if (!rule.sellerDenyList().isEmpty() && lot.seller() != null && rule.sellerDenyList().contains(lot.seller())) {
            return BuyDecision.rejected("seller denied");
        }

        return BuyDecision.approved(rule);
    }

    private boolean matchesRequiredEnchantments(List<EnchantmentData> lotEnchantments, List<RequiredEnchantment> requiredEnchantments) {
        for (RequiredEnchantment required : requiredEnchantments) {
            boolean matched = false;
            for (EnchantmentData actual : lotEnchantments) {
                if (!actual.id().equals(required.id())) {
                    continue;
                }

                if (required.minLevel() != null && actual.level() < required.minLevel()) {
                    continue;
                }

                matched = true;
                break;
            }

            if (!matched) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesRequiredPotionEffects(List<PotionEffectData> lotPotionEffects, List<RequiredPotionEffect> requiredPotionEffects) {
        for (RequiredPotionEffect required : requiredPotionEffects) {
            boolean matched = false;
            for (PotionEffectData actual : lotPotionEffects) {
                if (!actual.id().equals(required.id())) {
                    continue;
                }

                if (required.minAmplifier() != null && actual.amplifier() < required.minAmplifier()) {
                    continue;
                }

                if (required.minDurationSeconds() != null && actual.durationSeconds() < required.minDurationSeconds()) {
                    continue;
                }

                matched = true;
                break;
            }

            if (!matched) {
                return false;
            }
        }

        return true;
    }
}
