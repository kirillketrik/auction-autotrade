package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SellerParser implements ItemStackParser<Optional<String>> {
    @Override
    public Optional<String> parse(ItemStack stack) {
        return parseLines(LoreParserSupport.loreLines(stack));
    }

    public Optional<String> parseLines(List<String> lines) {
        for (String rawLine : lines) {
            String normalized = LoreParserSupport.normalize(rawLine);
            if (normalized.isBlank()) {
                continue;
            }

            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!lower.contains("продавец") && !lower.contains("seller")) {
                continue;
            }

            int separator = normalized.indexOf(':');
            if (separator < 0 || separator + 1 >= normalized.length()) {
                continue;
            }

            String seller = LoreParserSupport.sanitizeLabelPrefix(normalized.substring(separator + 1)).trim();
            if (!seller.isBlank()) {
                return Optional.of(seller);
            }
        }

        return Optional.empty();
    }
}
