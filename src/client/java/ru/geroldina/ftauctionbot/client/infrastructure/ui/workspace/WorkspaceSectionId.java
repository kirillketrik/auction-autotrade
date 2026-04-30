package ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace;

import java.util.Objects;

public record WorkspaceSectionId(String value) {
    public WorkspaceSectionId {
        value = Objects.requireNonNull(value, "value");
    }
}
