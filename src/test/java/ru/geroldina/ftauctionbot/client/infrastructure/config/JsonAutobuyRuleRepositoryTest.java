package ru.geroldina.ftauctionbot.client.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAutobuyRuleRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDefaultConfigWhenFileIsMissing() {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        JsonAutobuyRuleRepository repository = new JsonAutobuyRuleRepository(configPath);

        AutobuyConfig config = repository.load();

        assertTrue(Files.exists(configPath));
        assertTrue(config.buyRules().isEmpty());
        assertEquals(30, config.scanIntervalSeconds());
        assertEquals(10, config.scanPageLimit());
        assertEquals(AutobuyScanLogMode.MATCHED_ONLY, config.scanLogMode());
    }

    @Test
    void loadsConfiguredRules() throws Exception {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        Files.writeString(configPath, """
            {
              "scanIntervalSeconds": 15,
              "scanPageLimit": 6,
              "scanLogMode": "ALL",
              "buyRules": [
                {
                  "id": "totem",
                  "name": "Totem",
                  "enabled": true,
                  "minecraftId": "minecraft:totem_of_undying",
                  "maxTotalPrice": 6800000
                }
              ]
            }
            """);

        JsonAutobuyRuleRepository repository = new JsonAutobuyRuleRepository(configPath);
        AutobuyConfig config = repository.load();

        assertEquals(1, config.buyRules().size());
        assertEquals(15, config.scanIntervalSeconds());
        assertEquals(6, config.scanPageLimit());
        assertEquals(AutobuyScanLogMode.ALL, config.scanLogMode());
        BuyRule rule = config.buyRules().getFirst();
        assertEquals("totem", rule.id());
        assertEquals("minecraft:totem_of_undying", rule.minecraftId());
    }
}
