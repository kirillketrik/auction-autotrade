package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;

public final class AutobuyConfigManager {
    private final AutobuyRuleRepository repository;
    private final ScanLogger logger;

    private AutobuyConfig currentConfig = AutobuyConfig.empty();

    public AutobuyConfigManager(AutobuyRuleRepository repository, ScanLogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public AutobuyConfig loadStartup() {
        currentConfig = repository.load();
        logger.info("AUTOBUY_CONFIG", "Loaded " + currentConfig.buyRules().size() + " buy rules.");
        return currentConfig;
    }

    public AutobuyConfig reload() {
        currentConfig = repository.load();
        logger.info("AUTOBUY_CONFIG", "Reloaded " + currentConfig.buyRules().size() + " buy rules.");
        return currentConfig;
    }

    public AutobuyConfig saveAndReload(AutobuyConfig config) {
        repository.save(config);
        currentConfig = repository.load();
        logger.info("AUTOBUY_CONFIG", "Saved and reloaded autobuy config. Rules: " + currentConfig.buyRules().size() + ".");
        return currentConfig;
    }

    public AutobuyConfig getCurrentConfig() {
        return currentConfig;
    }
}
