package ru.geroldina.ftauctionbot.client.domain.auction.model;

import java.util.List;

public record AuctionScanResult(
    String startedAt,
    String finishedAt,
    boolean completed,
    String abortReason,
    int scannedPages,
    int totalItems,
    List<AuctionLot> items
) {
}
