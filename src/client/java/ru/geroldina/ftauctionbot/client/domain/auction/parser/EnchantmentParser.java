package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import ru.geroldina.ftauctionbot.client.domain.auction.model.EnchantmentData;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentParser implements ItemStackParser<List<EnchantmentData>> {
    @Override
    public List<EnchantmentData> parse(ItemStack stack) {
        List<EnchantmentData> result = new ArrayList<>();
        appendEnchantments(result, stack.get(DataComponentTypes.ENCHANTMENTS));
        appendEnchantments(result, stack.get(DataComponentTypes.STORED_ENCHANTMENTS));
        return result;
    }

    private static void appendEnchantments(List<EnchantmentData> target, ItemEnchantmentsComponent component) {
        if (component == null || component.isEmpty()) {
            return;
        }

        for (var entry : component.getEnchantmentEntries()) {
            RegistryEntry<net.minecraft.enchantment.Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            String id = enchantment.getKey()
                .map(key -> key.getValue().toString())
                .orElse(enchantment.getIdAsString());

            target.add(new EnchantmentData(id, level));
        }
    }
}
