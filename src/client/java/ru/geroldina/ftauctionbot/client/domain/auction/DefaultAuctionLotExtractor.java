package ru.geroldina.ftauctionbot.client.domain.auction;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionLotExtractor;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.EnchantmentParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.PriceParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.PotionEffectParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.SellerParser;

import java.util.Locale;

public final class DefaultAuctionLotExtractor implements AuctionLotExtractor {
    private final PriceParser priceParser;
    private final SellerParser sellerParser;
    private final EnchantmentParser enchantmentParser;
    private final PotionEffectParser potionEffectParser;

    public DefaultAuctionLotExtractor(
        PriceParser priceParser,
        SellerParser sellerParser,
        EnchantmentParser enchantmentParser,
        PotionEffectParser potionEffectParser
    ) {
        this.priceParser = priceParser;
        this.sellerParser = sellerParser;
        this.enchantmentParser = enchantmentParser;
        this.potionEffectParser = potionEffectParser;
    }

    @Override
    public boolean looksLikeAuctionLot(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("следующая страница") || name.contains("предыдущая страница")
            || name.contains("next page") || name.contains("previous page")) {
            return false;
        }

        return priceParser.parse(stack).isPresent();
    }

    @Override
    public AuctionLot extract(ItemStack stack, int page, int slotIndex) {
        long totalPrice = priceParser.parse(stack).orElse(0L);

        return new AuctionLot(
            page,
            slotIndex,
            Registries.ITEM.getId(stack.getItem()).toString(),
            stack.getName().getString(),
            stack.getCount(),
            totalPrice,
            AuctionPricing.calculateUnitPrice(totalPrice, stack.getCount()),
            sellerParser.parse(stack).orElse(null),
            enchantmentParser.parse(stack),
            potionEffectParser.parse(stack)
        );
    }
}
