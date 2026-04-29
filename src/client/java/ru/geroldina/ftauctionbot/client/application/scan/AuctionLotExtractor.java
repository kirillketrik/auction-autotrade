package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.item.ItemStack;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public interface AuctionLotExtractor {
    boolean looksLikeAuctionLot(ItemStack stack);

    AuctionLot extract(ItemStack stack, int page, int slotIndex);
}
