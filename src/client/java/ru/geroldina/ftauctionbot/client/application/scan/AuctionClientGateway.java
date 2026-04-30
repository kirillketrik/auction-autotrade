package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.application.autobuy.AntiAfkMoveDirection;

public interface AuctionClientGateway {
    boolean isReady();

    void sendChatCommand(String command);

    void sendOpenAuctionCommand();

    void clickSlot(int syncId, int slotId, int button, SlotActionType actionType);

    default boolean closeActiveHandledScreen() {
        return false;
    }

    default boolean canPerformAntiAfkActions() {
        return false;
    }

    default void applyAntiAfkMovement(AntiAfkMoveDirection direction) {
    }

    default void stopAntiAfkMovement() {
    }

    default void jump() {
    }
}
