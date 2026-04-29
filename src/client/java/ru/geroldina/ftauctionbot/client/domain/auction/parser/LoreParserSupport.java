package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class LoreParserSupport {
    private LoreParserSupport() {
    }

    public static List<String> loreLines(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(lore.lines().size());
        for (Text line : lore.lines()) {
            lines.add(normalize(line.getString()));
        }
        return lines;
    }

    public static String normalize(String value) {
        return value.replace('\u00A0', ' ').trim();
    }

    public static String sanitizeLabelPrefix(String value) {
        return value
            .replace("?", "")
            .replace("$", "")
            .replace("➥", "")
            .trim();
    }
}
