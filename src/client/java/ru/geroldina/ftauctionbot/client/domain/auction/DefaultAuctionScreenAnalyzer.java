package ru.geroldina.ftauctionbot.client.domain.auction;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScreenAnalyzer;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.LoreParserSupport;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultAuctionScreenAnalyzer implements AuctionScreenAnalyzer {
    private static final Pattern PAGE_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    @Override
    public boolean isAuctionScreenTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("аук")
            || normalized.contains("auction")
            || parsePageInfo(title).isPresent();
    }

    @Override
    public Optional<PageInfo> parsePageInfo(String title) {
        Matcher matcher = PAGE_PATTERN.matcher(extractCounterSegment(title));
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new PageInfo(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
            ));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public int resolveTopSlotCount(ScreenHandler currentHandler, int fallbackTopSlotCount) {
        if (currentHandler instanceof GenericContainerScreenHandler genericHandler) {
            return genericHandler.getRows() * 9;
        }

        return fallbackTopSlotCount;
    }

    @Override
    public int findNextPageSlot(List<ItemStack> topContents) {
        for (int slotIndex = 0; slotIndex < topContents.size(); slotIndex++) {
            ItemStack stack = topContents.get(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }

            String lowerName = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (lowerName.contains("следующая страница") || lowerName.contains("next page")) {
                return slotIndex;
            }

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) {
                continue;
            }

            for (Text line : lore.lines()) {
                String lowerLine = LoreParserSupport.normalize(line.getString()).toLowerCase(Locale.ROOT);
                if (lowerLine.contains("следующая страница") || lowerLine.contains("next page")) {
                    return slotIndex;
                }
            }
        }

        return -1;
    }

    private static String extractCounterSegment(String title) {
        int auctionIndex = title.indexOf("Аукцион");
        String prefix = auctionIndex >= 0 ? title.substring(0, auctionIndex) : title;
        return prefix.replace('\u00A0', ' ').trim();
    }
}
