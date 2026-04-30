package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

record SearchPickerEntry(
    String id,
    Text name,
    String searchText,
    ItemStack itemStack,
    RegistryEntry<StatusEffect> statusEffect,
    RegistryEntry<Enchantment> enchantment,
    String badgeText,
    int badgeColor
) {
}
