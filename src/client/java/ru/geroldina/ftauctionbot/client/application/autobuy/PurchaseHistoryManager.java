package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PurchaseHistoryManager {
    private final PurchaseHistoryRepository repository;
    private final ScanLogger logger;
    private final Clock clock;

    private List<PurchaseHistoryEntry> cachedEntries = List.of();

    public PurchaseHistoryManager(PurchaseHistoryRepository repository, ScanLogger logger) {
        this(repository, logger, Clock.systemDefaultZone());
    }

    PurchaseHistoryManager(PurchaseHistoryRepository repository, ScanLogger logger, Clock clock) {
        this.repository = repository;
        this.logger = logger;
        this.clock = clock;
    }

    public List<PurchaseHistoryEntry> load() {
        cachedEntries = sortDescending(repository.load());
        return cachedEntries;
    }

    public void recordPurchase(AuctionLot lot) {
        List<PurchaseHistoryEntry> updatedEntries = new ArrayList<>(cachedEntries.isEmpty() ? repository.load() : cachedEntries);
        PurchaseHistoryEntry entry = new PurchaseHistoryEntry(
            lot.minecraftId(),
            lot.displayName(),
            lot.count(),
            lot.totalPrice(),
            clock.millis()
        );
        updatedEntries.add(entry);
        updatedEntries = sortDescending(updatedEntries);
        repository.save(updatedEntries);
        cachedEntries = updatedEntries;
        logger.info(
            "PURCHASE_HISTORY",
            "Recorded purchase history entry: minecraftId=" + lot.minecraftId()
                + ", displayName=" + lot.displayName()
                + ", count=" + lot.count()
                + ", totalPrice=" + lot.totalPrice()
        );
    }

    public List<PurchaseHistoryEntry> getCachedEntries() {
        return cachedEntries;
    }

    private static List<PurchaseHistoryEntry> sortDescending(List<PurchaseHistoryEntry> entries) {
        return entries.stream()
            .sorted(Comparator.comparingLong(PurchaseHistoryEntry::purchasedAtEpochMillis).reversed())
            .toList();
    }
}
