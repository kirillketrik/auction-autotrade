package ru.geroldina.ftauctionbot.client.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.FtAuctionBotMod;
import ru.geroldina.ftauctionbot.client.application.autobuy.PurchaseHistoryRepository;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class JsonPurchaseHistoryRepository implements PurchaseHistoryRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<PurchaseHistoryEntry>>() {}.getType();
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("ftauctionbot-purchase-history.json");

    @Override
    public List<PurchaseHistoryEntry> load() {
        if (!Files.exists(FILE)) {
            return List.of();
        }

        try {
            String content = Files.readString(FILE);
            List<PurchaseHistoryEntry> entries = GSON.fromJson(content, ENTRY_LIST_TYPE);
            return entries == null ? List.of() : List.copyOf(entries);
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to load purchase history", e);
            throw new IllegalStateException("Failed to load purchase history", e);
        }
    }

    @Override
    public void save(List<PurchaseHistoryEntry> entries) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(
                FILE,
                GSON.toJson(entries, ENTRY_LIST_TYPE),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to save purchase history", e);
            throw new IllegalStateException("Failed to save purchase history", e);
        }
    }
}
