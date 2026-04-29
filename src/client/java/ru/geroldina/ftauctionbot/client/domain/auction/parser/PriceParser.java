package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceParser implements ItemStackParser<OptionalLong> {
    private static final Pattern PRICE_GROUP_PATTERN = Pattern.compile("([0-9][0-9\\s._,]*)");

    @Override
    public OptionalLong parse(ItemStack stack) {
        return parseLines(LoreParserSupport.loreLines(stack));
    }

    public OptionalLong parseLines(List<String> lines) {
        OptionalLong keywordPrice = OptionalLong.empty();
        OptionalLong fallbackPrice = OptionalLong.empty();

        for (String rawLine : lines) {
            String normalized = LoreParserSupport.normalize(rawLine);
            if (normalized.isBlank()) {
                continue;
            }

            OptionalLong candidate = extractFirstLong(normalized);
            if (candidate.isEmpty()) {
                continue;
            }

            if (containsPriceHint(normalized)) {
                keywordPrice = candidate;
                break;
            }

            if (fallbackPrice.isEmpty()) {
                fallbackPrice = candidate;
            }
        }

        return keywordPrice.isPresent() ? keywordPrice : fallbackPrice;
    }

    private static OptionalLong extractFirstLong(String line) {
        Matcher matcher = PRICE_GROUP_PATTERN.matcher(line);
        while (matcher.find()) {
            String digitsOnly = matcher.group(1).replaceAll("\\D", "");
            if (digitsOnly.isEmpty()) {
                continue;
            }

            try {
                return OptionalLong.of(Long.parseLong(digitsOnly));
            } catch (NumberFormatException ignored) {
                // Keep scanning if one candidate overflows or is malformed.
            }
        }

        return OptionalLong.empty();
    }

    private static boolean containsPriceHint(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("цена") || lower.contains("цена") || lower.contains("стоимость") || lower.contains("price") || lower.contains("$");
    }
}
