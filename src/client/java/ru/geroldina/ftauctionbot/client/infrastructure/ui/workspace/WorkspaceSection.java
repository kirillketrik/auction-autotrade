package ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace;

import net.minecraft.text.Text;

public interface WorkspaceSection {
    WorkspaceSectionId id();

    Text title();

    WorkspaceSectionView createView();
}
