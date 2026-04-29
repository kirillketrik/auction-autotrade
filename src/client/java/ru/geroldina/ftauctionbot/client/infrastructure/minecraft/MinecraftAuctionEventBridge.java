package ru.geroldina.ftauctionbot.client.infrastructure.minecraft;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftAuctionEventBridge {
    private static final List<MinecraftClientEventListener> listeners = new ArrayList<>();

    private MinecraftAuctionEventBridge() {
    }

    public static void register(MinecraftClientEventListener listener) {
        listeners.add(listener);
    }

    public static void onOpenScreen(int syncId, String title, ScreenHandler currentHandler) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onOpenScreen(syncId, title, currentHandler);
        }
    }

    public static void onInventory(int syncId, int revision, List<ItemStack> contents, ItemStack cursorStack, ScreenHandler currentHandler) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onInventory(syncId, revision, contents, cursorStack, currentHandler);
        }
    }

    public static void onSlotUpdate(int syncId, int revision, int slot, ItemStack stack, ScreenHandler currentHandler) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onSlotUpdate(syncId, revision, slot, stack, currentHandler);
        }
    }

    public static void onCloseScreen(int syncId) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onCloseScreen(syncId);
        }
    }

    public static void onGameMessage(String message, boolean overlay) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onGameMessage(message, overlay);
        }
    }

    public static void onProfilelessChatMessage(String message) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onProfilelessChatMessage(message);
        }
    }

    public static void onChatMessage(String message) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onChatMessage(message);
        }
    }

    public static void onOutgoingPacket(Packet<?> packet) {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onOutgoingPacket(packet);
        }
    }

    public static void onClientTick() {
        for (MinecraftClientEventListener listener : listeners) {
            listener.onClientTick();
        }
    }

    public static boolean shouldSuppressScreen(Screen screen) {
        for (MinecraftClientEventListener listener : listeners) {
            if (listener.shouldSuppressScreen(screen)) {
                return true;
            }
        }

        return false;
    }
}
