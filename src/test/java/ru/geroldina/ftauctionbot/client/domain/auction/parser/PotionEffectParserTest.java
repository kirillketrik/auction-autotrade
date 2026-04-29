package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PotionEffectParserTest {
    @Test
    void convertsPotionDurationsFromTicksToSeconds() {
        assertEquals(45, PotionEffectParser.toDurationSeconds(900));
        assertEquals(600, PotionEffectParser.toDurationSeconds(12_000));
        assertEquals(0, PotionEffectParser.toDurationSeconds(0));
    }

    @Test
    void convertsAmplifierToDisplayedLevel() {
        assertEquals(1, PotionEffectParser.toDisplayedLevel(0));
        assertEquals(2, PotionEffectParser.toDisplayedLevel(1));
        assertEquals(3, PotionEffectParser.toDisplayedLevel(2));
    }
}
