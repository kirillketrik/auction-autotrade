package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import java.util.List;

record AutobuyValidationResult(
    boolean valid,
    List<String> errors
) {
    static AutobuyValidationResult of(List<String> errors) {
        return new AutobuyValidationResult(errors.isEmpty(), List.copyOf(errors));
    }
}
