package ru.geroldina.ftauctionbot.client.application.scan;

public interface AuctionScanLifecycleObserver {
    void onScanFinished(AuctionScanResultSummary summary);
}
