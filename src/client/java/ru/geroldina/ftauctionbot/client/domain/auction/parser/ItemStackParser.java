package ru.geroldina.ftauctionbot.client.domain.auction.parser;

import net.minecraft.item.ItemStack;

public interface ItemStackParser<T> {
    T parse(ItemStack stack);
}
