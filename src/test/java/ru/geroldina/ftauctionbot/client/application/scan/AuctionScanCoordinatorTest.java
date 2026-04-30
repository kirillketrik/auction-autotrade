package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionScanResult;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionScanCoordinatorTest {
    @Test
    void savesSnapshotWhenPageLimitIsReached() {
        FakeGateway gateway = new FakeGateway();
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            gateway,
            new FakeScreenAnalyzer(45, new PageInfo(1, 99), -1),
            new FakeLotExtractor(),
            repository,
            new FakeLogger()
        );

        coordinator.startScan(1);
        coordinator.onAuctionScreenOpened(7, "1/99 Аукцион", null);
        coordinator.onAuctionInventory(7, filledInventory(45), null);

        assertNotNull(repository.savedResult);
        assertTrue(repository.savedResult.completed());
        assertEquals(45, repository.savedResult.totalItems());
    }

    @Test
    void abortsWhenOpenTimeoutExpires() {
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            new FakeGateway(),
            new FakeScreenAnalyzer(45, new PageInfo(1, 99), -1),
            new FakeLotExtractor(),
            repository,
            new FakeLogger()
        );

        coordinator.startScan(2);
        for (int index = 0; index < 100; index++) {
            coordinator.onClientTick();
        }

        assertNotNull(repository.savedResult);
        assertFalse(repository.savedResult.completed());
        assertEquals("auction-screen-open-timeout", repository.savedResult.abortReason());
    }

    @Test
    void retriesNextPageBeforeTimingOut() {
        FakeGateway gateway = new FakeGateway();
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            gateway,
            new FakeScreenAnalyzer(45, new PageInfo(1, 99), 44),
            new FakeLotExtractor(),
            repository,
            new FakeLogger()
        );

        coordinator.startScan(5);
        coordinator.onAuctionScreenOpened(7, "1/99 Аукцион", null);
        coordinator.onAuctionInventory(7, filledInventory(45), null);

        for (int index = 0; index < 20; index++) {
            coordinator.onClientTick();
        }

        assertEquals(1, gateway.clickedSlots.size());
    }

    @Test
    void usesConfiguredPageSwitchDelayBeforeClickingNextPage() {
        FakeGateway gateway = new FakeGateway();
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            gateway,
            new FakeScreenAnalyzer(45, new PageInfo(1, 99), 44),
            new FakeLotExtractor(),
            repository,
            new FakeLogger()
        );

        coordinator.startScan(5, 500);
        coordinator.onAuctionScreenOpened(7, "1/99 Аукцион", null);
        coordinator.onAuctionInventory(7, filledInventory(45), null);

        for (int index = 0; index < 10; index++) {
            coordinator.onClientTick();
        }
        assertEquals(0, gateway.clickedSlots.size());

        coordinator.onClientTick();
        assertEquals(1, gateway.clickedSlots.size());
    }

    @Test
    void scansUntilLastPageWhenRequestedForAllPages() {
        FakeGateway gateway = new FakeGateway();
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            gateway,
            new PagingScreenAnalyzer(45, 44),
            new FakeLotExtractor(),
            repository,
            new FakeLogger()
        );

        coordinator.startScanAllPagesCommand("ah search Stone", 200);
        coordinator.onAuctionScreenOpened(7, "1/2 Аукцион", null);
        coordinator.onAuctionInventory(7, filledInventory(45), null);
        for (int index = 0; index < 4; index++) {
            coordinator.onClientTick();
        }
        coordinator.onAuctionScreenOpened(8, "2/2 Аукцион", null);
        coordinator.onAuctionInventory(8, filledInventory(45), null);

        assertNotNull(repository.savedResult);
        assertTrue(repository.savedResult.completed());
        assertEquals(2, repository.savedResult.scannedPages());
        assertEquals(90, repository.savedResult.totalItems());
    }

    @Test
    void appliesJitterToConfiguredPageSwitchDelay() {
        FakeGateway gateway = new FakeGateway();
        FakeRepository repository = new FakeRepository();
        AuctionScanCoordinator coordinator = new AuctionScanCoordinator(
            gateway,
            new FakeScreenAnalyzer(45, new PageInfo(1, 99), 44),
            new FakeLotExtractor(),
            repository,
            new FakeLogger(),
            new FixedIntRandom(200)
        );

        coordinator.startScan(5, 500, 100);
        coordinator.onAuctionScreenOpened(7, "1/99 Аукцион", null);
        coordinator.onAuctionInventory(7, filledInventory(45), null);

        for (int index = 0; index < 12; index++) {
            coordinator.onClientTick();
        }
        assertEquals(0, gateway.clickedSlots.size());

        coordinator.onClientTick();
        assertEquals(1, gateway.clickedSlots.size());
    }

    private static List<ItemStack> filledInventory(int size) {
        List<ItemStack> contents = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            contents.add(null);
        }
        return contents;
    }

    private record FakeScreenAnalyzer(int topSlotCount, PageInfo pageInfo, int nextPageSlot) implements AuctionScreenAnalyzer {
        @Override
        public boolean isAuctionScreenTitle(String title) {
            return title.contains("Аукцион");
        }

        @Override
        public Optional<PageInfo> parsePageInfo(String title) {
            return Optional.of(pageInfo);
        }

        @Override
        public int resolveTopSlotCount(ScreenHandler currentHandler, int fallbackTopSlotCount) {
            return topSlotCount;
        }

        @Override
        public int findNextPageSlot(List<ItemStack> topContents) {
            return nextPageSlot;
        }
    }

    private record PagingScreenAnalyzer(int topSlotCount, int nextPageSlot) implements AuctionScreenAnalyzer {
        @Override
        public boolean isAuctionScreenTitle(String title) {
            return title.contains("Аукцион");
        }

        @Override
        public Optional<PageInfo> parsePageInfo(String title) {
            String[] fragments = title.split(" ");
            String[] pages = fragments[0].split("/");
            return Optional.of(new PageInfo(Integer.parseInt(pages[0]), Integer.parseInt(pages[1])));
        }

        @Override
        public int resolveTopSlotCount(ScreenHandler currentHandler, int fallbackTopSlotCount) {
            return topSlotCount;
        }

        @Override
        public int findNextPageSlot(List<ItemStack> topContents) {
            return nextPageSlot;
        }
    }

    private static final class FakeLotExtractor implements AuctionLotExtractor {
        @Override
        public boolean looksLikeAuctionLot(ItemStack stack) {
            return true;
        }

        @Override
        public AuctionLot extract(ItemStack stack, int page, int slotIndex) {
            return new AuctionLot(page, slotIndex, "minecraft:stone", "Stone", 1, 100L, 100L, null, List.of(), List.of());
        }
    }

    private static final class FakeGateway implements AuctionClientGateway {
        private final List<Integer> clickedSlots = new ArrayList<>();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void sendChatCommand(String command) {
        }

        @Override
        public void sendOpenAuctionCommand() {
        }

        @Override
        public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType) {
            clickedSlots.add(slotId);
        }

        @Override
        public boolean closeActiveHandledScreen() {
            return false;
        }
    }

    private static final class FakeRepository implements AuctionScanResultRepository {
        private AuctionScanResult savedResult;

        @Override
        public Path save(String fileName, AuctionScanResult result) {
            this.savedResult = result;
            return Path.of("/tmp", fileName);
        }
    }

    private static final class FakeLogger implements ScanLogger {
        @Override
        public void info(String category, String message) {
        }

        @Override
        public void block(String category, List<String> lines) {
        }
    }

    private static final class FixedIntRandom extends Random {
        private final int value;

        private FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
        }
    }
}
