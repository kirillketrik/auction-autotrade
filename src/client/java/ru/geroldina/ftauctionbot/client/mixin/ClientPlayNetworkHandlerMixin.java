package ru.geroldina.ftauctionbot.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftAuctionEventBridge;

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onOpenScreen", at = @At("TAIL"))
    private void ftauctionbot$onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        MinecraftAuctionEventBridge.onOpenScreen(
            packet.getSyncId(),
            packet.getName().getString(),
            client.player == null ? null : client.player.currentScreenHandler
        );
    }

    @Inject(method = "onInventory", at = @At("TAIL"))
    private void ftauctionbot$onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        MinecraftAuctionEventBridge.onInventory(
            packet.getSyncId(),
            packet.getRevision(),
            packet.getContents(),
            packet.getCursorStack(),
            client.player == null ? null : client.player.currentScreenHandler
        );
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void ftauctionbot$onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        MinecraftAuctionEventBridge.onSlotUpdate(
            packet.getSyncId(),
            packet.getRevision(),
            packet.getSlot(),
            packet.getStack(),
            client.player == null ? null : client.player.currentScreenHandler
        );
    }

    @Inject(method = "onCloseScreen", at = @At("TAIL"))
    private void ftauctionbot$onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
        MinecraftAuctionEventBridge.onCloseScreen(packet.getSyncId());
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void ftauctionbot$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        MinecraftAuctionEventBridge.onGameMessage(packet.content().getString(), packet.overlay());
    }

    @Inject(method = "onProfilelessChatMessage", at = @At("TAIL"))
    private void ftauctionbot$onProfilelessChatMessage(ProfilelessChatMessageS2CPacket packet, CallbackInfo ci) {
        MinecraftAuctionEventBridge.onProfilelessChatMessage(packet.message().getString());
    }

    @Inject(method = "onChatMessage", at = @At("TAIL"))
    private void ftauctionbot$onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        MinecraftAuctionEventBridge.onChatMessage(packet.unsignedContent() == null ? "" : packet.unsignedContent().getString());
    }
}
