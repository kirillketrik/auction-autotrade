package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;

public record BuyCandidate(
    int syncId,
    int revision,
    int slot,
    AuctionLot auctionLot
) {
}
