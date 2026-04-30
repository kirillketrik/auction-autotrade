package ru.geroldina.ftauctionbot.client.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
        assertEquals(2, config.scanIntervalJitterSeconds());
        assertEquals(10, config.scanPageLimit());
        assertEquals(200, config.pageSwitchDelayMs());
        assertEquals(150, config.pageSwitchDelayJitterMs());
        assertEquals(AutobuyScanLogMode.MATCHED_ONLY, config.scanLogMode());
        assertTrue(config.antiAfkEnabled());
        assertEquals(7, config.antiAfkActionIntervalSeconds());
        assertEquals(20, config.antiAfkJumpChancePercent());
        assertEquals(15, config.marketResearchTargetMarginPercent());
        assertEquals(5, config.marketResearchRiskBufferPercent());
    }

    @Test
    void loadsConfiguredRules() throws Exception {
        Path configPath = tempDir.resolve("ftauctionbot-buy-rules.json");
        Files.writeString(configPath, """
            {
              "scanIntervalSeconds": 15,
              "scanIntervalJitterSeconds": 4,
              "scanPageLimit": 6,
              "pageSwitchDelayMs": 450,
              "pageSwitchDelayJitterMs": 90,
              "scanLogMode": "ALL",
              "antiAfkEnabled": false,
              "antiAfkActionIntervalSeconds": 11,
              "antiAfkJumpChancePercent": 35,
              "marketResearchTargetMarginPercent": 18,
              "marketResearchRiskBufferPercent": 7,
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
        assertEquals(4, config.scanIntervalJitterSeconds());
        assertEquals(6, config.scanPageLimit());
        assertEquals(450, config.pageSwitchDelayMs());
        assertEquals(90, config.pageSwitchDelayJitterMs());
        assertEquals(AutobuyScanLogMode.ALL, config.scanLogMode());
        assertEquals(false, config.antiAfkEnabled());
        assertEquals(11, config.antiAfkActionIntervalSeconds());
        assertEquals(35, config.antiAfkJumpChancePercent());
        assertEquals(18, config.marketResearchTargetMarginPercent());
        assertEquals(7, config.marketResearchRiskBufferPercent());
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
            3,
            5,
            275,
            80,
            AutobuyScanLogMode.ALL,
            true,
            9,
            25,
            18,
            6,
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
        assertTrue(rawJson.contains("\"scanIntervalJitterSeconds\": 3"));
        assertTrue(rawJson.contains("\"pageSwitchDelayJitterMs\": 80"));
        assertTrue(rawJson.contains("\"antiAfkEnabled\": true"));
        assertTrue(rawJson.contains("\"antiAfkActionIntervalSeconds\": 9"));
        assertTrue(rawJson.contains("\"antiAfkJumpChancePercent\": 25"));
        assertTrue(rawJson.contains("\"marketResearchTargetMarginPercent\": 18"));
        assertTrue(rawJson.contains("\"marketResearchRiskBufferPercent\": 6"));
    }

    private static String readRaw(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
