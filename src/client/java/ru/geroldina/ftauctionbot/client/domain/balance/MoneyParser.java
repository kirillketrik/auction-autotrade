package ru.geroldina.ftauctionbot.client.domain.balance;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MoneyParser {
    private static final Pattern BALANCE_PATTERN = Pattern.compile("ваш баланс\\s*:\\s*\\$?([0-9][0-9,._\\s]*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public OptionalLong parseBalance(String message) {
        Matcher matcher = BALANCE_PATTERN.matcher(message);
        if (!matcher.find()) {
            return OptionalLong.empty();
        }

        String digitsOnly = matcher.group(1).replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.parseLong(digitsOnly));
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }
}
