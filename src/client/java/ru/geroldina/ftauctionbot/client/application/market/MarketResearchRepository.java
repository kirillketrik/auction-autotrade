package ru.geroldina.ftauctionbot.client.application.market;

import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.util.List;

public interface MarketResearchRepository {
    List<MarketResearchResult> load();

    void save(List<MarketResearchResult> results);
}
