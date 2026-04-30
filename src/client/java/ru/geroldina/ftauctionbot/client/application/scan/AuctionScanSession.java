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
    final int pageSwitchDelayTicks;
    final List<AuctionLot> scannedItems = new ArrayList<>();
    final Set<Integer> scannedPages = new HashSet<>();

    int timeoutTicks;
    int lastScannedPage;
    int activeSyncId = -1;
    int activeTopSlotCount = -1;
    int pendingNextPageSlot = -1;
    int pageClickDelayTicks;
    int pageWaitTicks;
    int nextPageAttempts;
    String activeTitle = "";

    AuctionScanSession(String startedAt, String scanFileName, String command, int maxPagesToScan, int timeoutTicks, int pageSwitchDelayTicks) {
        this.startedAt = startedAt;
        this.scanFileName = scanFileName;
        this.command = command;
        this.maxPagesToScan = maxPagesToScan;
        this.timeoutTicks = timeoutTicks;
        this.pageSwitchDelayTicks = pageSwitchDelayTicks;
    }
}
