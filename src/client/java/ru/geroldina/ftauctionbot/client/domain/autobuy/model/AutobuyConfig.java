package ru.geroldina.ftauctionbot.client.domain.autobuy.model;

import java.util.List;

public record AutobuyConfig(
    Integer scanIntervalSeconds,
    Integer scanIntervalJitterSeconds,
    Integer scanPageLimit,
    Integer pageSwitchDelayMs,
    Integer pageSwitchDelayJitterMs,
    AutobuyScanLogMode scanLogMode,
    Boolean antiAfkEnabled,
    Integer antiAfkActionIntervalSeconds,
    Integer antiAfkJumpChancePercent,
    Integer marketResearchTargetMarginPercent,
    Integer marketResearchRiskBufferPercent,
    List<BuyRule> buyRules
) {
    public AutobuyConfig {
        scanIntervalSeconds = scanIntervalSeconds == null || scanIntervalSeconds <= 0 ? 30 : scanIntervalSeconds;
        scanIntervalJitterSeconds = scanIntervalJitterSeconds == null || scanIntervalJitterSeconds < 0
            ? 2
            : Math.min(scanIntervalJitterSeconds, scanIntervalSeconds - 1);
        scanPageLimit = scanPageLimit == null || scanPageLimit <= 0 ? 10 : scanPageLimit;
        pageSwitchDelayMs = pageSwitchDelayMs == null || pageSwitchDelayMs <= 0 ? 200 : pageSwitchDelayMs;
        pageSwitchDelayJitterMs = pageSwitchDelayJitterMs == null || pageSwitchDelayJitterMs < 0 ? 150 : pageSwitchDelayJitterMs;
        scanLogMode = scanLogMode == null ? AutobuyScanLogMode.MATCHED_ONLY : scanLogMode;
        antiAfkEnabled = antiAfkEnabled == null ? Boolean.TRUE : antiAfkEnabled;
        antiAfkActionIntervalSeconds = antiAfkActionIntervalSeconds == null || antiAfkActionIntervalSeconds <= 0 ? 7 : antiAfkActionIntervalSeconds;
        antiAfkJumpChancePercent = antiAfkJumpChancePercent == null ? 20 : Math.max(0, Math.min(100, antiAfkJumpChancePercent));
        marketResearchTargetMarginPercent = marketResearchTargetMarginPercent == null || marketResearchTargetMarginPercent < 0 ? 15 : marketResearchTargetMarginPercent;
        marketResearchRiskBufferPercent = marketResearchRiskBufferPercent == null || marketResearchRiskBufferPercent < 0 ? 5 : marketResearchRiskBufferPercent;
        buyRules = buyRules == null ? List.of() : List.copyOf(buyRules);
    }

    public AutobuyConfig(
        Integer scanIntervalSeconds,
        Integer scanPageLimit,
        Integer pageSwitchDelayMs,
        AutobuyScanLogMode scanLogMode,
        Integer marketResearchTargetMarginPercent,
        Integer marketResearchRiskBufferPercent,
        List<BuyRule> buyRules
    ) {
        this(
            scanIntervalSeconds,
            2,
            scanPageLimit,
            pageSwitchDelayMs,
            150,
            scanLogMode,
            true,
            7,
            20,
            marketResearchTargetMarginPercent,
            marketResearchRiskBufferPercent,
            buyRules
        );
    }

    public static AutobuyConfig empty() {
        return new AutobuyConfig(30, 2, 10, 200, 150, AutobuyScanLogMode.MATCHED_ONLY, true, 7, 20, 15, 5, List.of());
    }
}
