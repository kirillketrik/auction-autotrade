package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import net.minecraft.text.Text;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace.WorkspaceSection;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace.WorkspaceSectionId;
import ru.geroldina.ftauctionbot.client.infrastructure.ui.workspace.WorkspaceSectionView;

public final class AutobuyWorkspaceSection implements WorkspaceSection {
    private static final WorkspaceSectionId ID = new WorkspaceSectionId("autobuy");

    @Override
    public WorkspaceSectionId id() {
        return ID;
    }

    @Override
    public Text title() {
        return AutobuyUiTextSupport.uiText("Автобай");
    }

    @Override
    public WorkspaceSectionView createView() {
        return () -> {
            throw new UnsupportedOperationException("Autobuy workspace section is not wired to a shell screen yet");
        };
    }
}
