package ru.geroldina.ftauctionbot.client.infrastructure.logging;

import net.fabricmc.loader.api.FabricLoader;
import ru.geroldina.ftauctionbot.FtAuctionBotMod;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class FileScanLogger implements ScanLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path LOG_FILE = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("ah-actions.log");

    public FileScanLogger() {
        initialize();
    }

    @Override
    public void info(String category, String message) {
        String line = "[" + TIMESTAMP_FORMAT.format(LocalDateTime.now()) + "] [" + category + "] " + message;
        FtAuctionBotMod.LOGGER.info(line);

        try {
            Files.writeString(
                LOG_FILE,
                line + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to append to AH action log {}", LOG_FILE, e);
        }
    }

    @Override
    public void block(String category, List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String prefix = index == 0 ? "BEGIN" : index == lines.size() - 1 ? "END" : "LINE";
            info(category, prefix + " " + lines.get(index));
        }
    }

    private void initialize() {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            if (Files.notExists(LOG_FILE)) {
                Files.createFile(LOG_FILE);
            }
        } catch (IOException e) {
            FtAuctionBotMod.LOGGER.error("Failed to initialize AH action log file {}", LOG_FILE, e);
            return;
        }

        info("LOGGER", "AH action logger initialized at " + LOG_FILE.toAbsolutePath());
    }
}
