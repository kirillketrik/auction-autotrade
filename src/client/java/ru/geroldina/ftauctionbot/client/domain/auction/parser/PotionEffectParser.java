package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PotionEffectData;

import java.util.ArrayList;
import java.util.List;

public final class PotionEffectParser implements ItemStackParser<List<PotionEffectData>> {
    private static final int TICKS_PER_SECOND = 20;

    static int toDurationSeconds(int durationTicks) {
        return durationTicks / TICKS_PER_SECOND;
    }

    static int toDisplayedLevel(int amplifier) {
        return amplifier + 1;
    }

    @Override
    public List<PotionEffectData> parse(ItemStack stack) {
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);

        if (potionContents == null || !potionContents.hasEffects()) {
            return List.of();
        }

        List<PotionEffectData> effects = new ArrayList<>();

        for (StatusEffectInstance effect : potionContents.getEffects()) {
            RegistryEntry<?> effectType = effect.getEffectType();
            String id = effectType.getKey()
                .map(key -> key.getValue().toString())
                .orElse(effectType.getIdAsString());

            effects.add(new PotionEffectData(
                id,
                toDisplayedLevel(effect.getAmplifier()),
                toDurationSeconds(effect.getDuration())
            ));
        }

        return effects;
    }
}
