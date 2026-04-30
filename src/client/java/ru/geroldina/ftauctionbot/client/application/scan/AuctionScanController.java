package ru.geroldina.ftauctionbot.client.application.scan;

public interface AuctionScanController {
    default void startScan(int maxPages) {
        startScan(maxPages, AuctionScanCoordinator.DEFAULT_NEXT_PAGE_CLICK_DELAY_MS, 0);
    }

    default void startScan(int maxPages, int pageSwitchDelayMs) {
        startScan(maxPages, pageSwitchDelayMs, 0);
    }

    void startScan(int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs);

    default void startScanCommand(String command, int maxPages) {
        startScanCommand(command, maxPages, AuctionScanCoordinator.DEFAULT_NEXT_PAGE_CLICK_DELAY_MS, 0);
    }

    default void startScanCommand(String command, int maxPages, int pageSwitchDelayMs) {
        startScanCommand(command, maxPages, pageSwitchDelayMs, 0);
    }

    void startScanCommand(String command, int maxPages, int pageSwitchDelayMs, int pageSwitchDelayJitterMs);

    default void startScanAllPagesCommand(String command, int pageSwitchDelayMs) {
        startScanCommand(command, 0, pageSwitchDelayMs, 0);
    }

    default void startScanAllPagesCommand(String command, int pageSwitchDelayMs, int pageSwitchDelayJitterMs) {
        startScanCommand(command, 0, pageSwitchDelayMs, pageSwitchDelayJitterMs);
    }

    boolean isIdle();

    void addPageObserver(AuctionScanPageObserver observer);

    void addLifecycleObserver(AuctionScanLifecycleObserver observer);
}
