package ru.geroldina.ftauctionbot.client.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleRepository;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.DisplayNameContainsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.DisplayNameEqualsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxCountCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxTotalPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxUnitPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MinCountCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredEnchantmentsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredPotionEffectsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.SellerAllowListCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.SellerDenyListCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class JsonAutobuyRuleRepository implements AutobuyRuleRepository {
    private static final Type REQUIRED_ENCHANTMENTS_TYPE = new TypeToken<List<RequiredEnchantment>>() { }.getType();
    private static final Type REQUIRED_POTION_EFFECTS_TYPE = new TypeToken<List<RequiredPotionEffect>>() { }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() { }.getType();
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(BuyRule.class, new BuyRuleJsonAdapter())
        .registerTypeAdapter(BuyRuleCondition.class, new BuyRuleConditionJsonAdapter())
        .setPrettyPrinting()
        .create();
    private final Path configPath;

    public JsonAutobuyRuleRepository() {
        this(FabricLoader.getInstance().getGameDir().resolve("config").resolve("ftauctionbot-buy-rules.json"));
    }

    public JsonAutobuyRuleRepository(Path configPath) {
        this.configPath = configPath;
    }

    @Override
    public AutobuyConfig load() {
        ensureConfigExists();

        try {
            String rawJson = Files.readString(configPath);
            AutobuyConfig config = GSON.fromJson(rawJson, AutobuyConfig.class);
            return config == null ? AutobuyConfig.empty() : config;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read autobuy config from " + configPath, e);
        }
    }

    private void ensureConfigExists() {
        if (Files.exists(configPath)) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(
                configPath,
                GSON.toJson(AutobuyConfig.empty()),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create default autobuy config at " + configPath, e);
        }
    }

    private static final class BuyRuleJsonAdapter implements JsonDeserializer<BuyRule> {
        @Override
        public BuyRule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            String id = readString(object, "id");
            String name = readString(object, "name");
            Boolean enabled = object.has("enabled") ? object.get("enabled").getAsBoolean() : null;
            List<BuyRuleCondition> conditions = readConditions(object, context);
            return new BuyRule(id, name, enabled, conditions);
        }

        private List<BuyRuleCondition> readConditions(JsonObject object, JsonDeserializationContext context) {
            if (!object.has("conditions") || object.get("conditions").isJsonNull()) {
                return List.of();
            }

            JsonArray rawConditions = object.getAsJsonArray("conditions");
            if (rawConditions == null) {
                return List.of();
            }

            List<BuyRuleCondition> conditions = new java.util.ArrayList<>();
            for (JsonElement condition : rawConditions) {
                conditions.add(context.deserialize(condition, BuyRuleCondition.class));
            }
            return conditions;
        }
    }

    private static final class BuyRuleConditionJsonAdapter implements JsonDeserializer<BuyRuleCondition> {
        @Override
        public BuyRuleCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            String type = readString(object, "type");
            if (type == null || type.isBlank()) {
                throw new JsonParseException("Buy rule condition type is required");
            }

            return switch (type) {
                case "minecraft_id" -> new ItemIdCondition(readString(object, "minecraftId"));
                case "display_name_contains" -> new DisplayNameContainsCondition(readString(object, "value"));
                case "display_name_equals" -> new DisplayNameEqualsCondition(readString(object, "value"));
                case "max_total_price" -> new MaxTotalPriceCondition(readLong(object, "value"));
                case "max_unit_price" -> new MaxUnitPriceCondition(readLong(object, "value"));
                case "min_count" -> new MinCountCondition(readInteger(object, "value"));
                case "max_count" -> new MaxCountCondition(readInteger(object, "value"));
                case "required_enchantments" -> new RequiredEnchantmentsCondition(context.deserialize(object.get("value"), REQUIRED_ENCHANTMENTS_TYPE));
                case "required_potion_effects" -> new RequiredPotionEffectsCondition(context.deserialize(object.get("value"), REQUIRED_POTION_EFFECTS_TYPE));
                case "seller_allow_list" -> new SellerAllowListCondition(context.deserialize(object.get("value"), STRING_LIST_TYPE));
                case "seller_deny_list" -> new SellerDenyListCondition(context.deserialize(object.get("value"), STRING_LIST_TYPE));
                default -> throw new JsonParseException("Unknown buy rule condition type: " + type);
            };
        }
    }

    private static String readString(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        return object.get(field).getAsString();
    }

    private static Long readLong(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        return object.get(field).getAsLong();
    }

    private static Integer readInteger(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        return object.get(field).getAsInt();
    }
}
