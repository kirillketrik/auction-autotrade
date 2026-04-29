package ru.geroldina.ftauctionbot.client.infrastructure.minecraft;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.ScreenHandler;

import java.util.List;

public interface MinecraftClientEventListener {
    default void onClientTick() {
    }

    default void onOpenScreen(int syncId, String title, ScreenHandler currentHandler) {
    }

    default void onInventory(int syncId, int revision, List<ItemStack> contents, ItemStack cursorStack, ScreenHandler currentHandler) {
    }

    default void onSlotUpdate(int syncId, int revision, int slot, ItemStack stack, ScreenHandler currentHandler) {
    }

    default void onCloseScreen(int syncId) {
    }

    default void onGameMessage(String message, boolean overlay) {
    }

    default void onProfilelessChatMessage(String message) {
    }

    default void onChatMessage(String message) {
    }

    default void onOutgoingPacket(Packet<?> packet) {
    }

    default boolean shouldSuppressScreen(Screen screen) {
        return false;
    }
}
