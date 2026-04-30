package ru.geroldina.ftauctionbot.client.infrastructure.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.scan.AuctionClientGateway;

public final class MinecraftAuctionClientGateway implements AuctionClientGateway {
    private final MinecraftClient client;

    public MinecraftAuctionClientGateway(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public boolean isReady() {
        return client.player != null && client.getNetworkHandler() != null && client.interactionManager != null;
    }

    @Override
    public void sendChatCommand(String command) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    @Override
    public void sendOpenAuctionCommand() {
        sendChatCommand("ah");
    }

    @Override
    public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            throw new IllegalStateException("Cannot click auction slot while client is not ready");
        }

        client.interactionManager.clickSlot(syncId, slotId, button, actionType, player);
    }

    @Override
    public boolean closeActiveHandledScreen() {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return false;
        }

        if (client.currentScreen == null || player.currentScreenHandler == player.playerScreenHandler) {
            return false;
        }

        player.closeHandledScreen();
        return true;
    }
}
