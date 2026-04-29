package ru.geroldina.ftauctionbot.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftAuctionEventBridge;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void ftauctionbot$suppressAuctionGui(Screen screen, CallbackInfo ci) {
        if (MinecraftAuctionEventBridge.shouldSuppressScreen(screen)) {
            ci.cancel();
        }
    }
}
