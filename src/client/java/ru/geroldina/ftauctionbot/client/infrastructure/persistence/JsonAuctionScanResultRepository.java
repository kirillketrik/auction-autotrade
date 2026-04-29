package ru.geroldina.ftauctionbot.client.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.FtAuctionBotMod;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionScanResultRepository;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class JsonAuctionScanResultRepository implements AuctionScanResultRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SCAN_DIR = FabricLoader.getInstance().getGameDir().resolve("auction-scans");

    @Override
    public Path save(String fileName, AuctionScanResult result) {
        try {
            Files.createDirectories(SCAN_DIR);
            Path file = SCAN_DIR.resolve(fileName);
            Files.writeString(
                file,
                GSON.toJson(result),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return file;
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to save auction scan result", e);
            throw new IllegalStateException("Failed to save auction scan result", e);
        }
    }
}
