package ru.geroldina.ftauctionbot.client.application.scan;

public interface AuctionScanController {
    void startScan(int maxPages);

    void startScanCommand(String command, int maxPages);

    boolean isIdle();

    void addPageObserver(AuctionScanPageObserver observer);
}
