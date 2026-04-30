package ru.geroldina.ftauctionbot.client.application.scan;

public interface AuctionScanController {
    default void startScan(int maxPages) {
        startScan(maxPages, AuctionScanCoordinator.DEFAULT_NEXT_PAGE_CLICK_DELAY_MS);
    }

    void startScan(int maxPages, int pageSwitchDelayMs);

    default void startScanCommand(String command, int maxPages) {
        startScanCommand(command, maxPages, AuctionScanCoordinator.DEFAULT_NEXT_PAGE_CLICK_DELAY_MS);
    }

    void startScanCommand(String command, int maxPages, int pageSwitchDelayMs);

    boolean isIdle();

    void addPageObserver(AuctionScanPageObserver observer);
}
