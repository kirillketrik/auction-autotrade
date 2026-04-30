package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;

public interface AutobuyRuleRepository {
    AutobuyConfig load();

    default void save(AutobuyConfig config) {
        throw new UnsupportedOperationException("Saving autobuy config is not supported by this repository");
    }
}
