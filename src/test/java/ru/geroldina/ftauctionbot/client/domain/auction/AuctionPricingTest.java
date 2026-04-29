package ru.geroldina.ftauctionbot.client.domain.auction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionPricingTest {
    @Test
    void calculatesUnitPriceFromStackCount() {
        assertEquals(133_333L, AuctionPricing.calculateUnitPrice(400_000L, 3));
    }
}
