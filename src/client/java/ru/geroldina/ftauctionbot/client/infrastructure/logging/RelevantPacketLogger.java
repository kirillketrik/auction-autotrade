package ru.geroldina.ftauctionbot.client.infrastructure.logging;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import ru.geroldina.ftauctionbot.client.application.scan.ScanLogger;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;

import java.util.List;

public final class RelevantPacketLogger implements MinecraftClientEventListener {
    private final ScanLogger logger;
    private boolean enabled = true;

    public RelevantPacketLogger(ScanLogger logger) {
        this.logger = logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("PACKET_TRACE", "Packet trace " + (enabled ? "enabled" : "disabled") + ".");
    }

    @Override
    public void onOutgoingPacket(Packet<?> packet) {
        if (!enabled || !isRelevantOutgoing(packet)) {
            return;
        }

        logger.info("PACKET_OUT", describeOutgoing(packet));
    }

    @Override
    public void onOpenScreen(int syncId, String title, ScreenHandler currentHandler) {
        if (enabled) {
            logger.info("PACKET_IN", "OpenScreenS2CPacket{syncId=" + syncId + ", title=\"" + title + "\"}");
        }
    }

    @Override
    public void onInventory(int syncId, int revision, List<ItemStack> contents, ItemStack cursorStack, ScreenHandler currentHandler) {
        if (enabled) {
            logger.info(
                "PACKET_IN",
                "InventoryS2CPacket{syncId=" + syncId + ", revision=" + revision + ", slots=" + contents.size() + ", cursorEmpty=" + cursorStack.isEmpty() + "}"
            );
        }
    }

    @Override
    public void onSlotUpdate(int syncId, int revision, int slot, ItemStack stack, ScreenHandler currentHandler) {
        if (enabled) {
            logger.info(
                "PACKET_IN",
                "ScreenHandlerSlotUpdateS2CPacket{syncId=" + syncId + ", revision=" + revision + ", slot=" + slot + ", item=" + describeStack(stack) + "}"
            );
        }
    }

    @Override
    public void onCloseScreen(int syncId) {
        if (enabled) {
            logger.info("PACKET_IN", "CloseScreenS2CPacket{syncId=" + syncId + "}");
        }
    }

    @Override
    public void onGameMessage(String message, boolean overlay) {
        if (enabled) {
            logger.info("PACKET_IN", "GameMessageS2CPacket{overlay=" + overlay + ", message=\"" + message + "\"}");
        }
    }

    @Override
    public void onProfilelessChatMessage(String message) {
        if (enabled) {
            logger.info("PACKET_IN", "ProfilelessChatMessageS2CPacket{message=\"" + message + "\"}");
        }
    }

    @Override
    public void onChatMessage(String message) {
        if (enabled) {
            logger.info("PACKET_IN", "ChatMessageS2CPacket{message=\"" + message + "\"}");
        }
    }

    private boolean isRelevantOutgoing(Packet<?> packet) {
        return packet instanceof ClickSlotC2SPacket
            || packet instanceof ButtonClickC2SPacket
            || packet instanceof CloseHandledScreenC2SPacket
            || packet instanceof ChatCommandSignedC2SPacket
            || packet instanceof ChatMessageC2SPacket;
    }

    private String describeOutgoing(Packet<?> packet) {
        if (packet instanceof ClickSlotC2SPacket clickSlotPacket) {
            return "ClickSlotC2SPacket{syncId=" + clickSlotPacket.getSyncId()
                + ", revision=" + clickSlotPacket.getRevision()
                + ", slot=" + clickSlotPacket.getSlot()
                + ", button=" + clickSlotPacket.getButton()
                + ", actionType=" + clickSlotPacket.getActionType()
                + ", stack=" + describeStack(clickSlotPacket.getStack())
                + "}";
        }

        if (packet instanceof ButtonClickC2SPacket) {
            return packet.toString();
        }

        if (packet instanceof CloseHandledScreenC2SPacket) {
            return packet.toString();
        }

        if (packet instanceof ChatCommandSignedC2SPacket chatCommandPacket) {
            return "ChatCommandSignedC2SPacket{command=\"" + chatCommandPacket.command() + "\"}";
        }

        if (packet instanceof ChatMessageC2SPacket chatMessagePacket) {
            return "ChatMessageC2SPacket{message=\"" + chatMessagePacket.chatMessage() + "\"}";
        }

        return packet.toString();
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }

        return stack.getCount() + "x " + stack.getName().getString();
    }
}
