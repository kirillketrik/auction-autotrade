package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import java.util.List;

public record BuyRule(
    String id,
    String name,
    Boolean enabled,
    String minecraftId,
    String displayNameContains,
    String displayNameEquals,
    Long maxTotalPrice,
    Long maxUnitPrice,
    Integer minCount,
    Integer maxCount,
    java.util.List<RequiredEnchantment> requiredEnchantments,
    java.util.List<RequiredPotionEffect> requiredPotionEffects,
    List<String> sellerAllowList,
    List<String> sellerDenyList
) {
    public BuyRule {
        enabled = enabled == null || enabled;
        requiredEnchantments = requiredEnchantments == null ? List.of() : List.copyOf(requiredEnchantments);
        requiredPotionEffects = requiredPotionEffects == null ? List.of() : List.copyOf(requiredPotionEffects);
        sellerAllowList = sellerAllowList == null ? List.of() : List.copyOf(sellerAllowList);
        sellerDenyList = sellerDenyList == null ? List.of() : List.copyOf(sellerDenyList);
    }
}
