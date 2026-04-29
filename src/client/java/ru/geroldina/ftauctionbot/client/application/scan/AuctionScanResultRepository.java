package ru.geroldina.ftauctionbot.client.application.scan;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionScanResult;

import java.nio.file.Path;

public interface AuctionScanResultRepository {
    Path save(String fileName, AuctionScanResult result);
}
