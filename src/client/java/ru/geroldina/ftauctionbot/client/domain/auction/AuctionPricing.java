package ru.geroldina.ftauctionbot.client.domain.auction;

public final class AuctionPricing {
    private AuctionPricing() {
    }

    public static long calculateUnitPrice(long totalPrice, int count) {
        if (count <= 0) {
            return totalPrice;
        }

        return totalPrice / count;
    }
}
