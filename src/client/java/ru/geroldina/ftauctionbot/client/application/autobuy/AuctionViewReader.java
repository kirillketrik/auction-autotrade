package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyCandidate;

import java.util.Optional;

public interface AuctionViewReader {
    Optional<BuyCandidate> getCandidateAtSlot(int slot);

    boolean isCurrentScreen(int syncId);
}
