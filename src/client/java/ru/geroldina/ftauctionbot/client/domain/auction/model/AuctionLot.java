package ru.geroldina.ftauctionbot.client.domain.auction.model;

import java.util.List;

public record AuctionLot(
    int page,
    int slotIndex,
    String minecraftId,
    String displayName,
    int count,
    long totalPrice,
    long unitPrice,
    String seller,
    List<EnchantmentData> enchantments,
    List<PotionEffectData> potionEffects
) {
}
