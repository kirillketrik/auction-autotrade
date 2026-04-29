package ru.geroldina.ftauctionbot.client.application.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;

public interface AutobuyRuleRepository {
    AutobuyConfig load();
}
