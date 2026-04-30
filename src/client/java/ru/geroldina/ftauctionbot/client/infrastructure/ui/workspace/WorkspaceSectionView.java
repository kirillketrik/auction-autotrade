package ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace;

import io.wispforest.owo.ui.core.ParentComponent;

public interface WorkspaceSectionView {
    ParentComponent build();

    default void onOpened() {
    }

    default void onClosed() {
    }
}
