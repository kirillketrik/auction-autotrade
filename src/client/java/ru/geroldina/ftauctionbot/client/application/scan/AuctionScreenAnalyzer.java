package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;

import java.util.List;
import java.util.Optional;

public interface AuctionScreenAnalyzer {
    boolean isAuctionScreenTitle(String title);

    Optional<PageInfo> parsePageInfo(String title);

    int resolveTopSlotCount(ScreenHandler currentHandler, int fallbackTopSlotCount);

    int findNextPageSlot(List<ItemStack> topContents);
}
