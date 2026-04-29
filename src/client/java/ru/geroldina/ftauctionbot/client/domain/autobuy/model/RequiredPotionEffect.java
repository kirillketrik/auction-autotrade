package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

public record RequiredPotionEffect(
    String id,
    Integer minAmplifier,
    Integer minDurationSeconds
) {
}
