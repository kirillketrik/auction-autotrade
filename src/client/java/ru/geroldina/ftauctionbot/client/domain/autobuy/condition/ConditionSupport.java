package ru.geroldina.ftauctionbot.client.domain.autobuy.condition;

import ru.geroldina.ftauctionbot.client.domain.auction.model.EnchantmentData;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PotionEffectData;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class ConditionSupport {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private ConditionSupport() {
    }

    static String normalizeMinecraftId(String value) {
        String normalized = normalizeFreeText(value);
        if (normalized.isEmpty()) {
            return normalized;
        }

        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    static String normalizeFreeText(String value) {
        if (value == null) {
            return "";
        }

        return WHITESPACE_PATTERN.matcher(value.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    static boolean containsNormalized(List<String> values, String candidate) {
        if (candidate == null) {
            return false;
        }

        String normalizedCandidate = normalizeFreeText(candidate);
        for (String value : values) {
            if (normalizedCandidate.equals(normalizeFreeText(value))) {
                return true;
            }
        }

        return false;
    }

    static boolean matchesRequiredEnchantments(List<EnchantmentData> lotEnchantments, List<RequiredEnchantment> requiredEnchantments) {
        for (RequiredEnchantment required : requiredEnchantments) {
            boolean matched = false;
            for (EnchantmentData actual : lotEnchantments) {
                if (!normalizeMinecraftId(actual.id()).equals(normalizeMinecraftId(required.id()))) {
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

    static boolean matchesRequiredPotionEffects(List<PotionEffectData> lotPotionEffects, List<RequiredPotionEffect> requiredPotionEffects) {
        for (RequiredPotionEffect required : requiredPotionEffects) {
            boolean matched = false;
            for (PotionEffectData actual : lotPotionEffects) {
                if (!normalizeMinecraftId(actual.id()).equals(normalizeMinecraftId(required.id()))) {
                    continue;
                }

                if (required.level() != null && actual.level() < required.level()) {
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
