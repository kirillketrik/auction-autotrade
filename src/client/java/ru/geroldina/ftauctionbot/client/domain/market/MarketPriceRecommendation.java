package ru.geroldina.ftauctionbot.client.domain.market;

public record MarketPriceRecommendation(
    String algorithmId,
    String title,
    String summary,
    Long maxBuyPrice,
    Long recommendedSellPrice,
    Integer expectedMarginPercent,
    String riskLabel,
    String status,
    String reason
) {
}
