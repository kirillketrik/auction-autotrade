package ru.geroldina.ftauctionbot.client.domain.balance;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyParserTest {
    private final MoneyParser parser = new MoneyParser();

    @Test
    void parsesBalanceFromSystemMessage() {
        assertEquals(OptionalLong.of(3_881_093L), parser.parseBalance("[$] Ваш баланс: $3,881,093"));
    }

    @Test
    void ignoresIrrelevantMessages() {
        assertEquals(OptionalLong.empty(), parser.parseBalance("Покупка успешно совершена"));
    }
}
