package ru.geroldina.ftauctionbot.client.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.DisplayNameCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxTotalPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredEnchantmentsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredPotionEffectsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

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
        assertEquals(200, config.pageSwitchDelayMs());
        assertEquals(AutobuyScanLogMode.MATCHED_ONLY, config.scanLogMode());
    }

    @Test
    void loadsConfiguredRules() throws Exception {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        Files.writeString(configPath, """
            {
              "scanIntervalSeconds": 15,
              "scanPageLimit": 6,
              "pageSwitchDelayMs": 450,
              "scanLogMode": "ALL",
              "buyRules": [
                {
                  "id": "totem",
                  "name": "Totem",
                  "enabled": true,
                  "conditions": [
                    {
                      "type": "minecraft_id",
                      "minecraftId": "minecraft:totem_of_undying"
                    },
                    {
                      "type": "max_total_price",
                      "value": 6800000
                    }
                  ]
                }
              ]
            }
            """);

        JsonAutobuyRuleRepository repository = new JsonAutobuyRuleRepository(configPath);
        AutobuyConfig config = repository.load();

        assertEquals(1, config.buyRules().size());
        assertEquals(15, config.scanIntervalSeconds());
        assertEquals(6, config.scanPageLimit());
        assertEquals(450, config.pageSwitchDelayMs());
        assertEquals(AutobuyScanLogMode.ALL, config.scanLogMode());
        BuyRule rule = config.buyRules().getFirst();
        assertEquals("totem", rule.id());
        assertEquals(2, rule.conditions().size());
        assertEquals(new ItemIdCondition("minecraft:totem_of_undying"), rule.conditions().get(0));
        assertEquals(new MaxTotalPriceCondition(6_800_000L), rule.conditions().get(1));
    }

    @Test
    void savesAndLoadsCurrentJsonShape() {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        JsonAutobuyRuleRepository repository = new JsonAutobuyRuleRepository(configPath);

        AutobuyConfig original = new AutobuyConfig(
            12,
            5,
            275,
            AutobuyScanLogMode.ALL,
            List.of(new BuyRule(
                "holy_water",
                "Holy Water",
                true,
                List.of(
                    new ItemIdCondition("minecraft:splash_potion"),
                    new RequiredEnchantmentsCondition(List.of(
                        new RequiredEnchantment("minecraft:sharpness", 5)
                    )),
                    new RequiredPotionEffectsCondition(List.of(
                        new RequiredPotionEffect("minecraft:regeneration", 2, 45)
                    ))
                )
            ))
        );

        repository.save(original);
        AutobuyConfig loaded = repository.load();

        assertEquals(original, loaded);
        String rawJson = readRaw(configPath);
        assertTrue(rawJson.contains("\"required_enchantments\""));
        assertTrue(rawJson.contains("\"level\": 5"));
        assertTrue(rawJson.contains("\"required_potion_effects\""));
        assertTrue(rawJson.contains("\"durationSeconds\": 45"));
    }

    @Test
    void loadsLegacyDisplayNameConditionAliases() throws Exception {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        Files.writeString(configPath, """
            {
              "buyRules": [
                {
                  "id": "exp",
                  "name": "Exp",
                  "enabled": true,
                  "conditions": [
                    {
                      "type": "display_name_contains",
                      "value": "Bottle o' Enchanting"
                    }
                  ]
                }
              ]
            }
            """);

        JsonAutobuyRuleRepository repository = new JsonAutobuyRuleRepository(configPath);
        AutobuyConfig config = repository.load();

        assertEquals(1, config.buyRules().size());
        assertEquals(List.of(new DisplayNameCondition("Bottle o' Enchanting")), config.buyRules().getFirst().conditions());
    }

    private static String readRaw(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
