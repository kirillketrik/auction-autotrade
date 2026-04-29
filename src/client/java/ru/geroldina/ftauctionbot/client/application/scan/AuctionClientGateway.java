package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.screen.slot.SlotActionType;

public interface AuctionClientGateway {
    boolean isReady();

    void sendChatCommand(String command);

    void sendOpenAuctionCommand();

    void clickSlot(int syncId, int slotId, int button, SlotActionType actionType);
}
