package ru.geroldina.ftauctionbot.client.domain.auction.model;

public record PotionEffectData(
    String id,
    int amplifier,
    int durationSeconds
) {
}
