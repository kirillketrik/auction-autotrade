package ru.geroldina.ftauctionbot.client.application.scan;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AuctionScanSession {
    final String startedAt;
    final String scanFileName;
    final String command;
    final int maxPagesToScan;
    final boolean scanAllPages;
    final int pageSwitchDelayTicks;
    final int pageSwitchDelayMs;
    final int pageSwitchDelayJitterMs;
    final List<AuctionLot> scannedItems = new ArrayList<>();
    final Set<Integer> scannedPages = new HashSet<>();

    int timeoutTicks;
    int lastScannedPage;
    int activeSyncId = -1;
    int activeTopSlotCount = -1;
    int pendingNextPageSlot = -1;
    int pageClickDelayTicks;
    int randomizedPageClickDelayMs;
    int pageWaitTicks;
    int nextPageAttempts;
    String activeTitle = "";

    AuctionScanSession(
        String startedAt,
        String scanFileName,
        String command,
        int maxPagesToScan,
        boolean scanAllPages,
        int timeoutTicks,
        int pageSwitchDelayMs,
        int pageSwitchDelayJitterMs,
        int pageSwitchDelayTicks
    ) {
        this.startedAt = startedAt;
        this.scanFileName = scanFileName;
        this.command = command;
        this.maxPagesToScan = maxPagesToScan;
        this.scanAllPages = scanAllPages;
        this.timeoutTicks = timeoutTicks;
        this.pageSwitchDelayMs = pageSwitchDelayMs;
        this.pageSwitchDelayJitterMs = pageSwitchDelayJitterMs;
        this.pageSwitchDelayTicks = pageSwitchDelayTicks;
    }
}
