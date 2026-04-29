package ru.geroldina.ftauctionbot.client.application.scan;

import java.util.List;

public interface ScanLogger {
    void info(String category, String message);

    void block(String category, List<String> lines);
}
