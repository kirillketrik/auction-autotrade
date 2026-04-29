package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellerParserTest {
    private final SellerParser parser = new SellerParser();

    @Test
    void extractsSellerFromRussianLoreLine() {
        Optional<String> seller = parser.parseLines(List.of(
            "$ Ценa: 400,000",
            "? Продавец: kapyctik333444"
        ));

        assertEquals(Optional.of("kapyctik333444"), seller);
    }

    @Test
    void returnsEmptyWhenSellerLineIsAbsent() {
        Optional<String> seller = parser.parseLines(List.of(
            "$ Ценa: 400,000",
            "⟲ Истeкaeт: 23 ч. 59 мин."
        ));

        assertEquals(Optional.empty(), seller);
    }
}
