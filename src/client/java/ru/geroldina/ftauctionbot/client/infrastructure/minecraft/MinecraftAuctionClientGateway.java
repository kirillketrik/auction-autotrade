package ru.geroldina.ftauctionbot.client.infrastructure.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.autobuy.AntiAfkMoveDirection;
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

    @Override
    public boolean canPerformAntiAfkActions() {
        ClientPlayerEntity player = client.player;
        return player != null
            && client.currentScreen == null
            && player.currentScreenHandler == player.playerScreenHandler;
    }

    @Override
    public void applyAntiAfkMovement(AntiAfkMoveDirection direction) {
        if (!canPerformAntiAfkActions()) {
            return;
        }

        stopAntiAfkMovement();
        switch (direction) {
            case FORWARD -> client.options.forwardKey.setPressed(true);
            case BACKWARD -> client.options.backKey.setPressed(true);
            case LEFT -> client.options.leftKey.setPressed(true);
            case RIGHT -> client.options.rightKey.setPressed(true);
        }
    }

    @Override
    public void stopAntiAfkMovement() {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }

    @Override
    public void jump() {
        ClientPlayerEntity player = client.player;
        if (!canPerformAntiAfkActions() || player == null || !player.isOnGround()) {
            return;
        }

        player.jump();
    }
}
