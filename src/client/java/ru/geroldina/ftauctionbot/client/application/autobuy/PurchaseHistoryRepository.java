package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;

import java.util.List;

public interface PurchaseHistoryRepository {
    List<PurchaseHistoryEntry> load();

    void save(List<PurchaseHistoryEntry> entries);
}
