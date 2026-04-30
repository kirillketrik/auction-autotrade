package ru.geroldina.ftauctionbot.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyExecutor;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyLoopController;
import ru.geroldina.ftauctionbot.client.application.autobuy.CurrentAuctionViewTracker;
import ru.geroldina.ftauctionbot.client.application.autobuy.PurchaseHistoryManager;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceTracker;
import ru.geroldina.ftauctionbot.client.application.market.MarketResearchManager;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanCoordinator;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.autobuy.DefaultAutobuyRuleMatcher;
import ru.geroldina.ftauctionbot.client.domain.auction.DefaultAuctionLotExtractor;
import ru.geroldina.ftauctionbot.client.domain.auction.DefaultAuctionScreenAnalyzer;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneyParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.EnchantmentParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.PotionEffectParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.PriceParser;
import ru.geroldina.ftauctionbot.client.domain.auction.parser.SellerParser;
import ru.geroldina.ftauctionbot.client.infrastructure.logging.FileScanLogger;
import ru.geroldina.ftauctionbot.client.infrastructure.logging.RelevantPacketLogger;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.FtAuctionBotClientCommands;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftAuctionClientGateway;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftAuctionEventBridge;
import ru.geroldina.ftauctionbot.client.infrastructure.config.JsonAutobuyRuleRepository;
import ru.geroldina.ftauctionbot.client.infrastructure.persistence.JsonAuctionScanResultRepository;
import ru.geroldina.ftauctionbot.client.infrastructure.persistence.JsonMarketResearchRepository;
import ru.geroldina.ftauctionbot.client.infrastructure.persistence.JsonPurchaseHistoryRepository;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy.AutobuyConfigScreen;

public final class FtAuctionBotClient implements ClientModInitializer {
    private static final int DEFAULT_SCAN_PAGE_LIMIT = 10;

    private KeyBinding startScanKey;
    private KeyBinding openAutobuyConfigKey;
    private AuctionScanCoordinator coordinator;
    private AutobuyConfigManager configManager;
    private AutobuyLoopController autobuyLoopController;
    private PurchaseHistoryManager purchaseHistoryManager;
    private MarketResearchManager marketResearchManager;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        ScanLogger logger = new FileScanLogger();
        MinecraftAuctionClientGateway gateway = new MinecraftAuctionClientGateway(client);
        DefaultAuctionScreenAnalyzer screenAnalyzer = new DefaultAuctionScreenAnalyzer();
        DefaultAuctionLotExtractor lotExtractor = new DefaultAuctionLotExtractor(
            new PriceParser(),
            new SellerParser(),
            new EnchantmentParser(),
            new PotionEffectParser()
        );

        coordinator = new AuctionScanCoordinator(
            gateway,
            screenAnalyzer,
            lotExtractor,
            new JsonAuctionScanResultRepository(),
            logger
        );
        BalanceTracker balanceTracker = new BalanceTracker(gateway, new MoneyParser(), logger);
        CurrentAuctionViewTracker auctionViewTracker = new CurrentAuctionViewTracker(screenAnalyzer, lotExtractor);
        configManager = new AutobuyConfigManager(new JsonAutobuyRuleRepository(), logger);
        configManager.loadStartup();
        purchaseHistoryManager = new PurchaseHistoryManager(new JsonPurchaseHistoryRepository(), logger);
        purchaseHistoryManager.load();
        marketResearchManager = new MarketResearchManager(coordinator, new JsonMarketResearchRepository(), logger);
        marketResearchManager.load();
        RelevantPacketLogger packetLogger = new RelevantPacketLogger(logger);
        AutobuyExecutor autobuyExecutor = new AutobuyExecutor(
            gateway,
            auctionViewTracker,
            balanceTracker,
            configManager,
            new DefaultAutobuyRuleMatcher(),
            purchaseHistoryManager,
            logger
        );
        autobuyLoopController = new AutobuyLoopController(
            gateway,
            coordinator,
            balanceTracker,
            configManager,
            new DefaultAutobuyRuleMatcher(),
            purchaseHistoryManager,
            logger
        );

        MinecraftAuctionEventBridge.register(coordinator);
        MinecraftAuctionEventBridge.register(balanceTracker);
        MinecraftAuctionEventBridge.register(auctionViewTracker);
        MinecraftAuctionEventBridge.register(packetLogger);
        MinecraftAuctionEventBridge.register(autobuyLoopController);
        FtAuctionBotClientCommands.register(
            configManager,
            balanceTracker,
            packetLogger,
            autobuyExecutor,
            autobuyLoopController,
            () -> client.execute(() -> client.setScreen(new AutobuyConfigScreen(configManager, autobuyLoopController, purchaseHistoryManager, marketResearchManager)))
        );

        startScanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ftauctionbot.scan_auction",
            GLFW.GLFW_KEY_F6,
            "category.ftauctionbot"
        ));
        openAutobuyConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ftauctionbot.open_autobuy_config",
            GLFW.GLFW_KEY_F7,
            "category.ftauctionbot"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
    }

    private void onEndClientTick(MinecraftClient client) {
        while (startScanKey.wasPressed()) {
            coordinator.startScan(DEFAULT_SCAN_PAGE_LIMIT);
        }
        while (openAutobuyConfigKey.wasPressed()) {
            purchaseHistoryManager.load();
            marketResearchManager.load();
            client.execute(() -> client.setScreen(new AutobuyConfigScreen(configManager, autobuyLoopController, purchaseHistoryManager, marketResearchManager)));
        }

        MinecraftAuctionEventBridge.onClientTick();
    }
}
