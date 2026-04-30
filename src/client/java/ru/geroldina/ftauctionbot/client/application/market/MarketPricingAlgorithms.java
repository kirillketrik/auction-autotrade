package ru.geroldina.ftauctionbot.client.application.market;

import ru.geroldina.ftauctionbot.client.domain.market.MarketPriceRecommendation;

import java.util.ArrayList;
import java.util.List;

final class MarketPricingAlgorithms {
    private MarketPricingAlgorithms() {
    }

    static RecommendationBundle buildRecommendations(List<Long> sortedUnitPrices, int targetMarginPercent, int riskBufferPercent) {
        PriceStats stats = PriceStats.from(sortedUnitPrices);
        List<MarketPriceRecommendation> recommendations = List.of(
            quantileFlip(stats, targetMarginPercent, riskBufferPercent),
            discountFromMedian(stats, targetMarginPercent, riskBufferPercent),
            lowerClusterGap(stats, targetMarginPercent, riskBufferPercent),
            liquidityFloor(stats, targetMarginPercent, riskBufferPercent)
        );
        return new RecommendationBundle(stats, recommendations);
    }

    private static MarketPriceRecommendation quantileFlip(PriceStats stats, int targetMarginPercent, int riskBufferPercent) {
        if (stats.count() < 3 || stats.p75() == null) {
            return abstained("quantile_flip", "Quantile Flip", "Продажа по верхнему квартилю рынка.", "Недостаточно лотов для квантильной оценки.");
        }

        long sellPrice = stats.p75();
        return active("quantile_flip", "Quantile Flip", "Продажа по p75 текущего рынка.", "BASE", sellPrice, profitableBuyThreshold(sellPrice, targetMarginPercent, riskBufferPercent));
    }

    private static MarketPriceRecommendation discountFromMedian(PriceStats stats, int targetMarginPercent, int riskBufferPercent) {
        if (stats.count() < 2 || stats.p50() == null) {
            return abstained("discount_from_median", "Median Discount", "Покупка с дисконтом к медиане рынка.", "Недостаточно лотов для оценки медианы.");
        }

        long sellPrice = stats.p50();
        return active("discount_from_median", "Median Discount", "Продажа по медиане текущего рынка.", "SAFE", sellPrice, profitableBuyThreshold(sellPrice, targetMarginPercent, riskBufferPercent));
    }

    private static MarketPriceRecommendation lowerClusterGap(PriceStats stats, int targetMarginPercent, int riskBufferPercent) {
        if (!stats.hasMeaningfulClusterSplit()) {
            return abstained("lower_cluster_gap", "Cluster Gap", "Разделение дешёвого кластера и основной рыночной массы.", "Не найден значимый ценовой разрыв.");
        }

        List<Long> upperCluster = stats.values().subList(stats.lowerClusterSize(), stats.values().size());
        long sellPrice = quantile(upperCluster, 0.5);
        long profitableThreshold = profitableBuyThreshold(sellPrice, targetMarginPercent, riskBufferPercent);
        long lowerClusterMax = stats.values().get(stats.lowerClusterSize() - 1);
        long maxBuyPrice = Math.min(profitableThreshold, lowerClusterMax);
        return active("lower_cluster_gap", "Cluster Gap", "Продажа по центру основной массы после ценового разрыва.", "OPPORTUNISTIC", sellPrice, maxBuyPrice);
    }

    private static MarketPriceRecommendation liquidityFloor(PriceStats stats, int targetMarginPercent, int riskBufferPercent) {
        if (stats.count() < 3 || stats.p50() == null) {
            return abstained("liquidity_floor", "Liquidity Floor", "Опора на самую плотную ценовую зону рынка.", "Недостаточно лотов для оценки ликвидности.");
        }

        DensestBand band = findDensestBand(stats.values(), stats.p50());
        if (band.count < 2) {
            return abstained("liquidity_floor", "Liquidity Floor", "Опора на самую плотную ценовую зону рынка.", "Не удалось выделить плотную зону ликвидности.");
        }

        long sellPrice = quantile(band.values, 0.5);
        return active("liquidity_floor", "Liquidity Floor", "Продажа по центру наиболее плотной ценовой зоны.", "LIQUID", sellPrice, profitableBuyThreshold(sellPrice, targetMarginPercent, riskBufferPercent));
    }

    private static DensestBand findDensestBand(List<Long> values, long anchorPrice) {
        long radius = Math.max(5L, Math.round(anchorPrice * 0.05d));
        DensestBand bestBand = new DensestBand(List.of(), 0);
        for (int index = 0; index < values.size(); index++) {
            long center = values.get(index);
            List<Long> band = new ArrayList<>();
            for (long value : values) {
                if (Math.abs(value - center) <= radius) {
                    band.add(value);
                }
            }
            if (band.size() > bestBand.count) {
                bestBand = new DensestBand(List.copyOf(band), band.size());
            }
        }
        return bestBand;
    }

    private static MarketPriceRecommendation active(String algorithmId, String title, String summary, String riskLabel, long sellPrice, long maxBuyPrice) {
        if (sellPrice <= 0 || maxBuyPrice <= 0 || maxBuyPrice >= sellPrice) {
            return abstained(algorithmId, title, summary, "Параметры маржи и буфера не дают прибыльный порог покупки.");
        }

        int expectedMarginPercent = (int) Math.round(((double) (sellPrice - maxBuyPrice) / sellPrice) * 100.0d);
        return new MarketPriceRecommendation(
            algorithmId,
            title,
            summary,
            maxBuyPrice,
            sellPrice,
            expectedMarginPercent,
            riskLabel,
            "ACTIVE",
            null
        );
    }

    private static MarketPriceRecommendation abstained(String algorithmId, String title, String summary, String reason) {
        return new MarketPriceRecommendation(
            algorithmId,
            title,
            summary,
            null,
            null,
            null,
            "N/A",
            "ABSTAINED",
            reason
        );
    }

    private static long profitableBuyThreshold(long sellPrice, int targetMarginPercent, int riskBufferPercent) {
        double factor = 1.0d - ((targetMarginPercent + riskBufferPercent) / 100.0d);
        return (long) Math.floor(sellPrice * factor);
    }

    private static long quantile(List<Long> sortedValues, double quantile) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.getFirst();
        }

        double position = (sortedValues.size() - 1) * quantile;
        int lowerIndex = (int) Math.floor(position);
        int upperIndex = (int) Math.ceil(position);
        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        double weight = position - lowerIndex;
        double interpolated = sortedValues.get(lowerIndex) + ((sortedValues.get(upperIndex) - sortedValues.get(lowerIndex)) * weight);
        return Math.round(interpolated);
    }

    record RecommendationBundle(PriceStats stats, List<MarketPriceRecommendation> recommendations) {
    }

    record PriceStats(
        List<Long> values,
        int count,
        Long min,
        Long avg,
        Long max,
        Long p10,
        Long p25,
        Long p50,
        Long p75,
        Long p90,
        Long largestGap,
        Integer lowerClusterSize,
        Integer mainClusterSize
    ) {
        static PriceStats from(List<Long> sortedValues) {
            if (sortedValues == null || sortedValues.isEmpty()) {
                return new PriceStats(List.of(), 0, null, null, null, null, null, null, null, null, null, null, null);
            }

            long sum = 0L;
            Long largestGap = 0L;
            int bestGapIndex = -1;
            for (int index = 0; index < sortedValues.size(); index++) {
                sum += sortedValues.get(index);
                if (index == 0) {
                    continue;
                }
                long gap = sortedValues.get(index) - sortedValues.get(index - 1);
                if (gap > largestGap) {
                    largestGap = gap;
                    bestGapIndex = index - 1;
                }
            }

            Integer lowerClusterSize = null;
            Integer mainClusterSize = null;
            if (bestGapIndex >= 0) {
                long lowerPrice = sortedValues.get(bestGapIndex);
                long threshold = Math.max(5L, Math.round(lowerPrice * 0.08d));
                if (largestGap >= threshold && bestGapIndex + 1 < sortedValues.size() - 1) {
                    lowerClusterSize = bestGapIndex + 1;
                    mainClusterSize = sortedValues.size() - lowerClusterSize;
                }
            }

            return new PriceStats(
                List.copyOf(sortedValues),
                sortedValues.size(),
                sortedValues.getFirst(),
                Math.round((double) sum / sortedValues.size()),
                sortedValues.getLast(),
                quantile(sortedValues, 0.10d),
                quantile(sortedValues, 0.25d),
                quantile(sortedValues, 0.50d),
                quantile(sortedValues, 0.75d),
                quantile(sortedValues, 0.90d),
                largestGap,
                lowerClusterSize,
                mainClusterSize
            );
        }

        boolean hasMeaningfulClusterSplit() {
            return lowerClusterSize != null && mainClusterSize != null && lowerClusterSize > 0 && mainClusterSize > 1;
        }
    }

    private record DensestBand(List<Long> values, int count) {
    }
}
