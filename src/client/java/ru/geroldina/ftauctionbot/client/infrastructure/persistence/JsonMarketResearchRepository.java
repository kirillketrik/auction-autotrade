package ru.geroldina.ftauctionbot.client.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.FtAuctionBotMod;
import ru.geroldina.ftauctionbot.client.application.market.MarketResearchRepository;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class JsonMarketResearchRepository implements MarketResearchRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RESULT_LIST_TYPE = new TypeToken<List<MarketResearchResult>>() { }.getType();
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("market-research-results.json");

    @Override
    public List<MarketResearchResult> load() {
        if (!Files.exists(FILE)) {
            return List.of();
        }

        try {
            String rawJson = Files.readString(FILE);
            List<MarketResearchResult> results = GSON.fromJson(rawJson, RESULT_LIST_TYPE);
            return results == null ? List.of() : List.copyOf(results);
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to read market research results", e);
            throw new IllegalStateException("Failed to read market research results", e);
        }
    }

    @Override
    public void save(List<MarketResearchResult> results) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(
                FILE,
                GSON.toJson(results == null ? List.of() : results, RESULT_LIST_TYPE),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to write market research results", e);
            throw new IllegalStateException("Failed to write market research results", e);
        }
    }
}
