package ru.geroldina.ftauctionbot.client.application.scan;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.List;

public interface AuctionScanPageObserver {
    AuctionPageDecision onPageScanned(int syncId, int currentPage, int totalPages, List<AuctionLot> pageLots);
}
