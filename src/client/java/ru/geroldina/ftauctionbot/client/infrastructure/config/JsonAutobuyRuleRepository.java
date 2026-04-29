package ru.geroldina.ftauctionbot.client.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyRuleRepository;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class JsonAutobuyRuleRepository implements AutobuyRuleRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
}
