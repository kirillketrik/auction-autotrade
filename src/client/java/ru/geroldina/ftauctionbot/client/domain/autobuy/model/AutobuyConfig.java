package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import java.util.List;

public record AutobuyConfig(
    Integer scanIntervalSeconds,
    Integer scanPageLimit,
    Integer pageSwitchDelayMs,
    AutobuyScanLogMode scanLogMode,
    List<BuyRule> buyRules
) {
    public AutobuyConfig {
        scanIntervalSeconds = scanIntervalSeconds == null || scanIntervalSeconds <= 0 ? 30 : scanIntervalSeconds;
        scanPageLimit = scanPageLimit == null || scanPageLimit <= 0 ? 10 : scanPageLimit;
        pageSwitchDelayMs = pageSwitchDelayMs == null || pageSwitchDelayMs <= 0 ? 200 : pageSwitchDelayMs;
        scanLogMode = scanLogMode == null ? AutobuyScanLogMode.MATCHED_ONLY : scanLogMode;
        buyRules = buyRules == null ? List.of() : List.copyOf(buyRules);
    }

    public static AutobuyConfig empty() {
        return new AutobuyConfig(30, 10, 200, AutobuyScanLogMode.MATCHED_ONLY, List.of());
    }
}
