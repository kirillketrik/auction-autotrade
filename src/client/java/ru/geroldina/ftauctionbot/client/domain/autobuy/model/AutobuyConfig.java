package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import java.util.List;

public record AutobuyConfig(
    Integer scanIntervalSeconds,
    Integer scanPageLimit,
    AutobuyScanLogMode scanLogMode,
    List<BuyRule> buyRules
) {
    public AutobuyConfig {
        scanIntervalSeconds = scanIntervalSeconds == null || scanIntervalSeconds <= 0 ? 30 : scanIntervalSeconds;
        scanPageLimit = scanPageLimit == null || scanPageLimit <= 0 ? 10 : scanPageLimit;
        scanLogMode = scanLogMode == null ? AutobuyScanLogMode.MATCHED_ONLY : scanLogMode;
        buyRules = buyRules == null ? List.of() : List.copyOf(buyRules);
    }

    public static AutobuyConfig empty() {
        return new AutobuyConfig(30, 10, AutobuyScanLogMode.MATCHED_ONLY, List.of());
    }
}
