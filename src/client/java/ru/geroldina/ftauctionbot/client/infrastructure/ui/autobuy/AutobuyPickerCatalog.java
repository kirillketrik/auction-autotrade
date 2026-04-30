package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

final class AutobuyPickerCatalog {
    SearchPickerEntry resolveItemSelection(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return null;
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return null;
        }

        return toItemPickerEntry(Registries.ITEM.get(identifier));
    }

    SearchPickerEntry resolvePotionEffectSelection(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return null;
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.STATUS_EFFECT.containsId(identifier)) {
            return null;
        }

        StatusEffect effect = Registries.STATUS_EFFECT.get(identifier);
        return effect == null ? null : toPotionEffectPickerEntry(Registries.STATUS_EFFECT.getEntry(effect));
    }

    SearchPickerEntry resolveEnchantmentSelection(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return null;
        }

        Identifier identifier = Identifier.tryParse(id);
        Registry<Enchantment> registry = enchantmentRegistry();
        if (identifier == null || !registry.containsId(identifier)) {
            return null;
        }

        Enchantment enchantment = registry.get(identifier);
        return enchantment == null ? null : toEnchantmentPickerEntry(registry.getEntry(enchantment));
    }

    SearchPickerState openItemPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = Registries.ITEM.stream()
            .map(this::toItemPickerEntry)
            .sorted(Comparator.comparing(SearchPickerEntry::id))
            .toList();
        return new SearchPickerState("Выбор предмета Minecraft", entries, onPick);
    }

    SearchPickerState openPotionEffectPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = Registries.STATUS_EFFECT.streamEntries()
            .map(this::toPotionEffectPickerEntry)
            .sorted(Comparator.comparing(SearchPickerEntry::id))
            .toList();
        return new SearchPickerState("Выбор эффекта", entries, onPick);
    }

    SearchPickerState openEnchantmentPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = enchantmentRegistry().streamEntries()
            .map(this::toEnchantmentPickerEntry)
            .sorted(Comparator.comparing(SearchPickerEntry::id))
            .toList();
        return new SearchPickerState("Выбор зачарования", entries, onPick);
    }

    String describeItem(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return "Выберите предмет...";
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return id;
        }

        Item item = Registries.ITEM.get(identifier);
        return new ItemStack(item).getName().getString() + " | " + id;
    }

    String describePotionEffect(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return "Выберите эффект...";
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.STATUS_EFFECT.containsId(identifier)) {
            return id;
        }

        return Objects.requireNonNull(Registries.STATUS_EFFECT.get(identifier)).getName().getString();
    }

    String describeEnchantment(String id) {
        if (AutobuyUiTextSupport.isBlank(id)) {
            return "Выберите зачарование...";
        }

        Identifier identifier = Identifier.tryParse(id);
        Registry<Enchantment> registry = enchantmentRegistry();
        if (identifier == null || !registry.containsId(identifier)) {
            return id;
        }

        return Objects.requireNonNull(registry.get(identifier)).description().getString();
    }

    String normalizeSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private SearchPickerEntry toItemPickerEntry(Item item) {
        ItemStack stack = new ItemStack(item);
        String id = Registries.ITEM.getId(item).toString();
        Text name = stack.getName();
        return new SearchPickerEntry(id, name, normalizeSearch(name.getString() + " " + id), stack, null, null, null, 0);
    }

    private SearchPickerEntry toPotionEffectPickerEntry(RegistryEntry<StatusEffect> entry) {
        StatusEffect effect = entry.value();
        String id = Registries.STATUS_EFFECT.getId(effect).toString();
        Text name = effect.getName();
        return new SearchPickerEntry(id, name, normalizeSearch(name.getString() + " " + id), null, entry, null, null, 0);
    }

    private SearchPickerEntry toEnchantmentPickerEntry(RegistryEntry<Enchantment> entry) {
        Enchantment enchantment = entry.value();
        String id = enchantmentRegistry().getId(enchantment).toString();
        Text name = enchantment.description();
        return new SearchPickerEntry(
            id,
            name,
            normalizeSearch(name.getString() + " " + id),
            enchantmentBaseStack(id),
            null,
            entry,
            enchantmentBadge(id, name.getString()),
            enchantmentBadgeColor(id)
        );
    }

    private ItemStack enchantmentBaseStack(String enchantmentId) {
        String path = safePath(enchantmentId);
        ItemStack stack = new ItemStack(switch (path) {
            case "power", "punch", "flame", "infinity" -> Items.BOW;
            case "multishot", "quick_charge", "piercing" -> Items.CROSSBOW;
            case "loyalty", "riptide", "channeling", "impaling" -> Items.TRIDENT;
            case "luck_of_the_sea", "lure" -> Items.FISHING_ROD;
            case "protection", "blast_protection", "fire_protection", "projectile_protection", "thorns", "unbreaking", "mending" -> Items.DIAMOND_CHESTPLATE;
            case "feather_falling", "depth_strider", "frost_walker", "soul_speed" -> Items.DIAMOND_BOOTS;
            case "respiration", "aqua_affinity" -> Items.DIAMOND_HELMET;
            case "swift_sneak" -> Items.DIAMOND_LEGGINGS;
            case "efficiency", "fortune", "silk_touch" -> Items.DIAMOND_PICKAXE;
            case "sharpness", "smite", "bane_of_arthropods", "fire_aspect", "looting", "knockback", "sweeping_edge" -> Items.DIAMOND_SWORD;
            default -> Items.ENCHANTED_BOOK;
        });
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private String enchantmentBadge(String enchantmentId, String displayName) {
        String path = safePath(enchantmentId);
        String[] tokens = path.split("_");
        if (tokens.length >= 2) {
            return (tokens[0].substring(0, 1) + tokens[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        String normalized = displayName.replaceAll("[^\\p{L}\\p{Nd}]+", "");
        if (normalized.length() >= 2) {
            return normalized.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return "EN";
    }

    private int enchantmentBadgeColor(String enchantmentId) {
        int hash = Math.abs(enchantmentId.hashCode());
        int red = 80 + hash % 120;
        int green = 80 + (hash / 7) % 120;
        int blue = 80 + (hash / 17) % 120;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static String safePath(String identifier) {
        Identifier parsed = Identifier.tryParse(identifier);
        return parsed == null ? identifier : parsed.getPath();
    }

    private Registry<Enchantment> enchantmentRegistry() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            throw new IllegalStateException("Client world is not available for enchantment registry access");
        }
        return client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
    }
}
