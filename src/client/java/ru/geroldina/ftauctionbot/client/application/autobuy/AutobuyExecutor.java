package ru.geroldina.ftauctionbot.client.application.autobuy;

import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceObserver;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceService;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyAttemptResult;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyCandidate;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyDecision;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;

public final class AutobuyExecutor implements BalanceObserver {
    private final AuctionClientGateway gateway;
    private final AuctionViewReader auctionViewTracker;
    private final BalanceService balanceService;
    private final AutobuyConfigManager configManager;
    private final AutobuyRuleMatcher ruleMatcher;
    private final ScanLogger logger;

    private PendingBuy pendingBuy;

    public AutobuyExecutor(
        AuctionClientGateway gateway,
        AuctionViewReader auctionViewTracker,
        BalanceService balanceService,
        AutobuyConfigManager configManager,
        AutobuyRuleMatcher ruleMatcher,
        ScanLogger logger
    ) {
        this.gateway = gateway;
        this.auctionViewTracker = auctionViewTracker;
        this.balanceService = balanceService;
        this.configManager = configManager;
        this.ruleMatcher = ruleMatcher;
        this.logger = logger;
        this.balanceService.addObserver(this);
    }

    public BuyAttemptResult attemptBuySlot(int slot) {
        if (pendingBuy != null) {
            return BuyAttemptResult.failure("Another buy attempt is already waiting for completion.");
        }

        if (!gateway.isReady()) {
            return BuyAttemptResult.failure("Client is not ready.");
        }

        BuyCandidate candidate = auctionViewTracker.getCandidateAtSlot(slot)
            .orElse(null);
        if (candidate == null) {
            return BuyAttemptResult.failure("No auction lot found at slot " + slot + ".");
        }

        BuyDecision decision = ruleMatcher.match(candidate.auctionLot(), configManager.getCurrentConfig().buyRules());
        if (!decision.approved()) {
            return BuyAttemptResult.failure("Buy rule rejected slot " + slot + ": " + decision.reason());
        }

        pendingBuy = new PendingBuy(candidate, decision);
        balanceService.requestRefresh();
        logger.info("AUTOBUY", "Queued buy attempt for slot " + slot + " and requested fresh balance.");
        return BuyAttemptResult.pending("Refreshing balance before buy attempt.");
    }

    @Override
    public void onBalanceUpdated(MoneySnapshot snapshot) {
        if (pendingBuy == null) {
            return;
        }

        BuyCandidate candidate = pendingBuy.candidate;
        if (!auctionViewTracker.isCurrentScreen(candidate.syncId())) {
            logger.info("AUTOBUY", "Cancelled pending buy because the active screen changed.");
            pendingBuy = null;
            return;
        }

        long price = candidate.auctionLot().totalPrice();
        if (snapshot.amount() < price) {
            logger.info("AUTOBUY", "Rejected buy attempt because balance $" + snapshot.amount() + " is below price $" + price + ".");
            pendingBuy = null;
            return;
        }

        gateway.clickSlot(candidate.syncId(), candidate.slot(), 0, SlotActionType.QUICK_MOVE);
        logger.info(
            "AUTOBUY",
            "Triggered buy attempt for slot " + candidate.slot()
                + ", revision=" + candidate.revision()
                + ", actionType=" + SlotActionType.QUICK_MOVE
                + ", rule=" + pendingBuy.decision.matchedRule().id()
                + ", price=$" + price
                + ", balance=$" + snapshot.amount()
        );
        pendingBuy = null;
    }

    @Override
    public void onBalanceRefreshFailed(String reason) {
        if (pendingBuy == null) {
            return;
        }

        logger.info("AUTOBUY", "Cancelled pending buy because balance refresh failed: " + reason + ".");
        pendingBuy = null;
    }

    private record PendingBuy(BuyCandidate candidate, BuyDecision decision) {
    }
}
