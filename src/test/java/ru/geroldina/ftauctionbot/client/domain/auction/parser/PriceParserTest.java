package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceParserTest {
    private final PriceParser parser = new PriceParser();

    @Test
    void parsesPriceFromRussianLoreLine() {
        OptionalLong price = parser.parseLines(List.of(
            "➥ Нажмите, чтобы купить",
            "$ Ценa: 7,300,000"
        ));

        assertEquals(OptionalLong.of(7_300_000L), price);
    }

    @Test
    void fallsBackToFirstNumberWhenNoPriceLabelExists() {
        OptionalLong price = parser.parseLines(List.of(
            "Лимит: 300",
            "Доступно 1200"
        ));

        assertEquals(OptionalLong.of(300L), price);
    }
}
