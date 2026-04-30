package ru.geroldina.ftauctionbot.client.application.scan;

public record AuctionScanResultSummary(
    String command,
    boolean completed,
    String abortReason,
    int scannedPages,
    int totalItems
) {
}
