package ru.geroldina.ftauctionbot.client.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleRepository;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
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
import java.util.ArrayList;
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

    @Override
    public void save(AutobuyConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(
                configPath,
                GSON.toJson(config == null ? AutobuyConfig.empty() : config),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write autobuy config to " + configPath, e);
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

    private static final class BuyRuleJsonAdapter implements JsonDeserializer<BuyRule>, JsonSerializer<BuyRule> {
        @Override
        public BuyRule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            String id = readString(object, "id");
            String name = readString(object, "name");
            Boolean enabled = object.has("enabled") ? object.get("enabled").getAsBoolean() : null;
            List<BuyRuleCondition> conditions = readConditions(object, context);
            return new BuyRule(id, name, enabled, conditions);
        }

        @Override
        public JsonElement serialize(BuyRule src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            if (src.id() != null) {
                object.addProperty("id", src.id());
            }
            if (src.name() != null) {
                object.addProperty("name", src.name());
            }
            object.addProperty("enabled", src.enabled());

            JsonArray conditions = new JsonArray();
            for (BuyRuleCondition condition : src.conditions()) {
                conditions.add(context.serialize(condition, BuyRuleCondition.class));
            }
            object.add("conditions", conditions);
            return object;
        }

        private List<BuyRuleCondition> readConditions(JsonObject object, JsonDeserializationContext context) {
            if (!object.has("conditions") || object.get("conditions").isJsonNull()) {
                return List.of();
            }

            JsonArray rawConditions = object.getAsJsonArray("conditions");
            if (rawConditions == null) {
                return List.of();
            }

            List<BuyRuleCondition> conditions = new ArrayList<>();
            for (JsonElement condition : rawConditions) {
                conditions.add(context.deserialize(condition, BuyRuleCondition.class));
            }
            return conditions;
        }
    }

    private static final class BuyRuleConditionJsonAdapter implements JsonDeserializer<BuyRuleCondition>, JsonSerializer<BuyRuleCondition> {
        @Override
        public BuyRuleCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            String type = readString(object, "type");
            if (type == null || type.isBlank()) {
                throw new JsonParseException("Buy rule condition type is required");
            }

            return switch (type) {
                case "minecraft_id" -> new ItemIdCondition(readString(object, "minecraftId"));
                case "max_total_price" -> new MaxTotalPriceCondition(readLong(object, "value"));
                case "max_unit_price" -> new MaxUnitPriceCondition(readLong(object, "value"));
                case "min_count" -> new MinCountCondition(readInteger(object, "value"));
                case "max_count" -> new MaxCountCondition(readInteger(object, "value"));
                case "required_enchantments" -> new RequiredEnchantmentsCondition(readRequiredEnchantments(object));
                case "required_potion_effects" -> new RequiredPotionEffectsCondition(readRequiredPotionEffects(object));
                case "seller_allow_list" -> new SellerAllowListCondition(context.deserialize(object.get("value"), STRING_LIST_TYPE));
                case "seller_deny_list" -> new SellerDenyListCondition(context.deserialize(object.get("value"), STRING_LIST_TYPE));
                default -> throw new JsonParseException("Unknown buy rule condition type: " + type);
            };
        }

        @Override
        public JsonElement serialize(BuyRuleCondition src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            switch (src) {
                case ItemIdCondition condition -> {
                    object.addProperty("type", "minecraft_id");
                    object.addProperty("minecraftId", condition.minecraftId());
                }
                case MaxTotalPriceCondition condition -> {
                    object.addProperty("type", "max_total_price");
                    addLongProperty(object, "value", condition.value());
                }
                case MaxUnitPriceCondition condition -> {
                    object.addProperty("type", "max_unit_price");
                    addLongProperty(object, "value", condition.value());
                }
                case MinCountCondition condition -> {
                    object.addProperty("type", "min_count");
                    addIntegerProperty(object, "value", condition.value());
                }
                case MaxCountCondition condition -> {
                    object.addProperty("type", "max_count");
                    addIntegerProperty(object, "value", condition.value());
                }
                case RequiredEnchantmentsCondition condition -> {
                    object.addProperty("type", "required_enchantments");
                    object.add("value", writeRequiredEnchantments(condition.value()));
                }
                case RequiredPotionEffectsCondition condition -> {
                    object.addProperty("type", "required_potion_effects");
                    object.add("value", writeRequiredPotionEffects(condition.value()));
                }
                case SellerAllowListCondition condition -> {
                    object.addProperty("type", "seller_allow_list");
                    object.add("value", context.serialize(condition.value(), STRING_LIST_TYPE));
                }
                case SellerDenyListCondition condition -> {
                    object.addProperty("type", "seller_deny_list");
                    object.add("value", context.serialize(condition.value(), STRING_LIST_TYPE));
                }
                default -> throw new JsonParseException("Unsupported buy rule condition type for serialization: " + src.getClass().getName());
            }

            return object;
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

    private static List<RequiredEnchantment> readRequiredEnchantments(JsonObject object) {
        if (!object.has("value") || object.get("value").isJsonNull()) {
            return List.of();
        }

        JsonArray values = object.getAsJsonArray("value");
        if (values == null) {
            return List.of();
        }

        List<RequiredEnchantment> result = new ArrayList<>();
        for (JsonElement element : values) {
            JsonObject value = element.getAsJsonObject();
            result.add(new RequiredEnchantment(
                readString(value, "id"),
                readInteger(value, "level")
            ));
        }
        return result;
    }

    private static List<RequiredPotionEffect> readRequiredPotionEffects(JsonObject object) {
        if (!object.has("value") || object.get("value").isJsonNull()) {
            return List.of();
        }

        JsonArray values = object.getAsJsonArray("value");
        if (values == null) {
            return List.of();
        }

        List<RequiredPotionEffect> result = new ArrayList<>();
        for (JsonElement element : values) {
            JsonObject value = element.getAsJsonObject();
            result.add(new RequiredPotionEffect(
                readString(value, "id"),
                readInteger(value, "level"),
                readInteger(value, "durationSeconds")
            ));
        }
        return result;
    }

    private static JsonArray writeRequiredEnchantments(List<RequiredEnchantment> enchantments) {
        JsonArray values = new JsonArray();
        for (RequiredEnchantment enchantment : enchantments) {
            JsonObject value = new JsonObject();
            if (enchantment.id() != null) {
                value.addProperty("id", enchantment.id());
            }
            addIntegerProperty(value, "level", enchantment.minLevel());
            values.add(value);
        }
        return values;
    }

    private static JsonArray writeRequiredPotionEffects(List<RequiredPotionEffect> effects) {
        JsonArray values = new JsonArray();
        for (RequiredPotionEffect effect : effects) {
            JsonObject value = new JsonObject();
            if (effect.id() != null) {
                value.addProperty("id", effect.id());
            }
            addIntegerProperty(value, "level", effect.level());
            addIntegerProperty(value, "durationSeconds", effect.minDurationSeconds());
            values.add(value);
        }
        return values;
    }

    private static void addIntegerProperty(JsonObject object, String field, Integer value) {
        if (value != null) {
            object.addProperty(field, value);
        }
    }

    private static void addLongProperty(JsonObject object, String field, Long value) {
        if (value != null) {
            object.addProperty(field, value);
        }
    }
}
