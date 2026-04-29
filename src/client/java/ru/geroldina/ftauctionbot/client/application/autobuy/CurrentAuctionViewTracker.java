package ru.geroldina.ftauctionbot.client.application.autobuy;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionLotExtractor;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScreenAnalyzer;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyCandidate;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CurrentAuctionViewTracker implements MinecraftClientEventListener, AuctionViewReader {
    private final AuctionScreenAnalyzer screenAnalyzer;
    private final AuctionLotExtractor lotExtractor;

    private int activeSyncId = -1;
    private int revision;
    private int topSlotCount = -1;
    private String title = "";
    private List<ItemStack> topContents = List.of();

    public CurrentAuctionViewTracker(AuctionScreenAnalyzer screenAnalyzer, AuctionLotExtractor lotExtractor) {
        this.screenAnalyzer = screenAnalyzer;
        this.lotExtractor = lotExtractor;
    }

    @Override
    public void onOpenScreen(int syncId, String title, ScreenHandler currentHandler) {
        if (!screenAnalyzer.isAuctionScreenTitle(title)) {
            return;
        }

        activeSyncId = syncId;
        this.title = title;
        topSlotCount = screenAnalyzer.resolveTopSlotCount(currentHandler, topSlotCount);
        topContents = List.of();
    }

    @Override
    public void onInventory(int syncId, int revision, List<ItemStack> contents, ItemStack cursorStack, ScreenHandler currentHandler) {
        if (syncId != activeSyncId || activeSyncId < 0) {
            return;
        }

        topSlotCount = screenAnalyzer.resolveTopSlotCount(currentHandler, topSlotCount);
        if (topSlotCount <= 0 || contents.size() < topSlotCount) {
            return;
        }

        this.revision = revision;
        topContents = List.copyOf(contents.subList(0, topSlotCount));
    }

    @Override
    public void onSlotUpdate(int syncId, int revision, int slot, ItemStack stack, ScreenHandler currentHandler) {
        if (syncId != activeSyncId || slot < 0 || slot >= topContents.size()) {
            return;
        }

        List<ItemStack> updatedContents = new ArrayList<>(topContents);
        updatedContents.set(slot, stack);
        topContents = List.copyOf(updatedContents);
        this.revision = revision;
    }

    @Override
    public void onCloseScreen(int syncId) {
        if (syncId != activeSyncId) {
            return;
        }

        activeSyncId = -1;
        revision = 0;
        topSlotCount = -1;
        title = "";
        topContents = List.of();
    }

    @Override
    public Optional<BuyCandidate> getCandidateAtSlot(int slot) {
        if (activeSyncId < 0 || slot < 0 || slot >= topContents.size()) {
            return Optional.empty();
        }

        ItemStack stack = topContents.get(slot);
        if (stack == null || !lotExtractor.looksLikeAuctionLot(stack)) {
            return Optional.empty();
        }

        int page = screenAnalyzer.parsePageInfo(title)
            .map(PageInfo::currentPage)
            .orElse(0);

        return Optional.of(new BuyCandidate(
            activeSyncId,
            revision,
            slot,
            lotExtractor.extract(stack, page, slot)
        ));
    }

    @Override
    public boolean isCurrentScreen(int syncId) {
        return activeSyncId == syncId;
    }
}
