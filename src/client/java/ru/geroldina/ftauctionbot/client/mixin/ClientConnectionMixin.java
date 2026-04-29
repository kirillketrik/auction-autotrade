package ru.geroldina.ftauctionbot.client.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftAuctionEventBridge;

@Mixin(ClientConnection.class)
abstract class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void ftauctionbot$onOutgoingPacket(Packet<?> packet, CallbackInfo ci) {
        MinecraftAuctionEventBridge.onOutgoingPacket(packet);
    }
}
