package ru.geroldina.ftauctionbot.client.infrastructure.minecraft;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyExecutor;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyLoopController;
import ru.geroldina.ftauctionbot.client.application.balance.BalanceService;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyAttemptResult;
import ru.geroldina.ftauctionbot.client.domain.balance.MoneySnapshot;
import ru.geroldina.ftauctionbot.client.infrastructure.logging.RelevantPacketLogger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class FtAuctionBotClientCommands {
    private static final DateTimeFormatter BALANCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private FtAuctionBotClientCommands() {
    }

    public static void register(
        AutobuyConfigManager configManager,
        BalanceService balanceService,
        RelevantPacketLogger packetLogger,
        AutobuyExecutor autobuyExecutor,
        AutobuyLoopController autobuyLoopController,
        Runnable openAutobuyGuiAction
    ) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommandManager.literal("ftab")
                .then(ClientCommandManager.literal("gui")
                    .executes(context -> {
                        openAutobuyGuiAction.run();
                        context.getSource().sendFeedback(Text.literal("Opened autobuy config UI."));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("reload")
                    .executes(context -> {
                        int count = configManager.reload().buyRules().size();
                        context.getSource().sendFeedback(Text.literal("Reloaded autobuy config. Rules: " + count));
                        return count;
                    }))
                .then(ClientCommandManager.literal("balance")
                    .executes(context -> {
                        boolean started = balanceService.requestRefresh();
                        context.getSource().sendFeedback(Text.literal(started
                            ? "Requested balance refresh via /money."
                            : "Balance refresh is already in progress or client is not ready."));
                        return started ? 1 : 0;
                    })
                    .then(ClientCommandManager.literal("show")
                        .executes(context -> {
                            sendBalanceStatus(context.getSource(), balanceService.getLastKnownBalance(), balanceService.isAwaitingRefresh());
                            return 1;
                        })))
                .then(ClientCommandManager.literal("trace")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("Packet trace is " + (packetLogger.isEnabled() ? "enabled" : "disabled") + "."));
                        return packetLogger.isEnabled() ? 1 : 0;
                    })
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            packetLogger.setEnabled(true);
                            context.getSource().sendFeedback(Text.literal("Packet trace enabled."));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            packetLogger.setEnabled(false);
                            context.getSource().sendFeedback(Text.literal("Packet trace disabled."));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("status")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("Packet trace is " + (packetLogger.isEnabled() ? "enabled" : "disabled") + "."));
                            return packetLogger.isEnabled() ? 1 : 0;
                        })))
                .then(ClientCommandManager.literal("buy")
                    .then(ClientCommandManager.literal("slot")
                        .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                BuyAttemptResult result = autobuyExecutor.attemptBuySlot(IntegerArgumentType.getInteger(context, "slot"));
                                sendBuyResult(context.getSource(), result);
                                return result.successful() || result.pending() ? 1 : 0;
                            }))))
                .then(ClientCommandManager.literal("autobuy")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal("Autobuy loop is " + (autobuyLoopController.isEnabled() ? "running" : "stopped") + "."));
                        return autobuyLoopController.isEnabled() ? 1 : 0;
                    })
                    .then(ClientCommandManager.literal("start")
                        .executes(context -> {
                            autobuyLoopController.start();
                            context.getSource().sendFeedback(Text.literal("Autobuy loop started."));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("stop")
                        .executes(context -> {
                            autobuyLoopController.stop();
                            context.getSource().sendFeedback(Text.literal("Autobuy loop stopped."));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("status")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("Autobuy loop is " + (autobuyLoopController.isEnabled() ? "running" : "stopped") + "."));
                            return autobuyLoopController.isEnabled() ? 1 : 0;
                        })))
        ));
    }

    private static void sendBalanceStatus(FabricClientCommandSource source, Optional<MoneySnapshot> snapshot, boolean awaitingRefresh) {
        if (snapshot.isEmpty()) {
            source.sendFeedback(Text.literal("Last known balance: <unknown>" + (awaitingRefresh ? " (refresh in progress)" : "")));
            return;
        }

        MoneySnapshot moneySnapshot = snapshot.get();
        source.sendFeedback(Text.literal(
            "Last known balance: $" + moneySnapshot.amount()
                + " at " + BALANCE_TIME_FORMAT.format(moneySnapshot.observedAt())
                + (awaitingRefresh ? " (refresh in progress)" : "")
        ));
    }

    private static void sendBuyResult(FabricClientCommandSource source, BuyAttemptResult result) {
        if (result.pending() || result.successful()) {
            source.sendFeedback(Text.literal(result.message()));
            return;
        }

        source.sendError(Text.literal(result.message()));
    }
}
