package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

final class AutobuyDraftParsing {
    private AutobuyDraftParsing() {
    }

    static Integer parseInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static Long parseLong(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static int clampPositive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    static int clampNonNegative(Integer value, int fallback) {
        return value == null || value < 0 ? fallback : value;
    }
}
