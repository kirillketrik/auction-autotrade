package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;

import java.util.List;

public interface AutobuyRuleMatcher {
    BuyDecision match(AuctionLot lot, List<BuyRule> rules);
}
