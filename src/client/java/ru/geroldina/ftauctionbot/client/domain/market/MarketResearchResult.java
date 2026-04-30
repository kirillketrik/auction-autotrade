package ru.geroldina.ftauctionbot.client.domain.market;

import java.util.List;

public record MarketResearchResult(
    String ruleId,
    String ruleName,
    String searchCommand,
    String targetMinecraftId,
    String itemDisplayName,
    String startedAt,
    String finishedAt,
    boolean completed,
    String abortReason,
    int scannedPages,
    int totalScannedLots,
    int matchedLots,
    Long p10UnitPrice,
    Long p25UnitPrice,
    Long p50UnitPrice,
    Long p75UnitPrice,
    Long p90UnitPrice,
    Long largestUnitPriceGap,
    Integer lowerClusterSize,
    Integer mainClusterSize,
    Long minUnitPrice,
    Long avgUnitPrice,
    Long maxUnitPrice,
    List<MarketPriceRecommendation> recommendations
) {
    public MarketResearchResult {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
