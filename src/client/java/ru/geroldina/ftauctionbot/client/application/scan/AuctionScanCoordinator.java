package ru.geroldina.ftauctionbot.client.application.scan;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionScanResult;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;
import ru.geroldina.ftauctionbot.client.infrastructure.minecraft.MinecraftClientEventListener;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AuctionScanCoordinator implements MinecraftClientEventListener, AuctionScanController {
    static final int DEFAULT_NEXT_PAGE_CLICK_DELAY_MS = 200;
    private static final int TICKS_PER_SECOND = 20;
    private static final int OPEN_TIMEOUT_TICKS = 100;
    private static final int PAGE_TIMEOUT_TICKS = 60;
    private static final int NEXT_PAGE_RETRY_TICKS = 20;
    private static final int MAX_NEXT_PAGE_ATTEMPTS = 3;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter JSON_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AuctionClientGateway clientGateway;
    private final AuctionScreenAnalyzer screenAnalyzer;
    private final AuctionLotExtractor lotExtractor;
    private final AuctionScanResultRepository resultRepository;
    private final ScanLogger logger;
    private final List<AuctionScanPageObserver> pageObservers = new ArrayList<>();
    private final List<AuctionScanLifecycleObserver> lifecycleObservers = new ArrayList<>();

    private AuctionScanState state = AuctionScanState.IDLE;
    private AuctionScanSession session;

    public AuctionScanCoordinator(
        AuctionClientGateway clientGateway,
        AuctionScreenAnalyzer screenAnalyzer,
        AuctionLotExtractor lotExtractor,
        AuctionScanResultRepository resultRepository,
        ScanLogger logger
    ) {
        this.clientGateway = clientGateway;
        this.screenAnalyzer = screenAnalyzer;
        this.lotExtractor = lotExtractor;
        this.resultRepository = resultRepository;
        this.logger = logger;
    }

    @Override
    public void startScan(int maxPages) {
        startScan(maxPages, DEFAULT_NEXT_PAGE_CLICK_DELAY_MS);
    }

    @Override
    public void startScan(int maxPages, int pageSwitchDelayMs) {
        startScanCommand("ah", maxPages, pageSwitchDelayMs);
    }

    @Override
    public void startScanCommand(String command, int maxPages) {
        startScanCommand(command, maxPages, DEFAULT_NEXT_PAGE_CLICK_DELAY_MS);
    }

    @Override
    public void startScanCommand(String command, int maxPages, int pageSwitchDelayMs) {
        if (state != AuctionScanState.IDLE) {
            logger.info("SCANNER", "Scan request ignored because another scan is already running.");
            return;
        }

        if (!clientGateway.isReady()) {
            logger.info("SCANNER", "Scan request ignored because client is not ready.");
            return;
        }

        boolean scanAllPages = maxPages <= 0;
        if (!scanAllPages && maxPages <= 0) {
            logger.info("SCANNER", "Scan request ignored because maxPages must be > 0.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        session = new AuctionScanSession(
            JSON_TIMESTAMP_FORMAT.format(now),
            "auction-scan-" + FILE_TIMESTAMP_FORMAT.format(now) + ".json",
            command,
            maxPages,
            scanAllPages,
            OPEN_TIMEOUT_TICKS,
            toDelayTicks(pageSwitchDelayMs)
        );
        state = AuctionScanState.OPENING_AUCTION;

        clientGateway.sendChatCommand(command);
        logger.info(
            "SCANNER",
            "Started auction scan. Waiting for /" + command + " to open. maxPages=" + (scanAllPages ? "ALL" : maxPages)
                + ", pageSwitchDelayMs=" + normalizeDelayMs(pageSwitchDelayMs)
        );
    }

    @Override
    public void onClientTick() {
        if (state == AuctionScanState.IDLE || session == null) {
            return;
        }

        if (!clientGateway.isReady()) {
            abortScan("player-or-network-unavailable");
            return;
        }

        if (state == AuctionScanState.WAITING_FOR_NEXT_PAGE) {
            tickWaitForNextPage();
            return;
        }

        session.timeoutTicks--;
        if (session.timeoutTicks <= 0) {
            abortScan("auction-screen-open-timeout");
        }
    }

    @Override
    public void onOpenScreen(int syncId, String title, ScreenHandler currentHandler) {
        if (state == AuctionScanState.IDLE || !screenAnalyzer.isAuctionScreenTitle(title) || session == null) {
            return;
        }

        session.activeSyncId = syncId;
        session.activeTitle = title;
        session.activeTopSlotCount = screenAnalyzer.resolveTopSlotCount(currentHandler, session.activeTopSlotCount);
        session.pageWaitTicks = 0;

        logger.info(
            "SCANNER",
            "Received auction screen packet. syncId=" + syncId + ", title=\"" + title + "\""
        );
    }

    @Override
    public void onInventory(int syncId, int revision, List<ItemStack> contents, ItemStack cursorStack, ScreenHandler currentHandler) {
        if (state == AuctionScanState.IDLE || session == null || syncId != session.activeSyncId) {
            return;
        }

        int topSlotCount = screenAnalyzer.resolveTopSlotCount(currentHandler, session.activeTopSlotCount);
        if (topSlotCount <= 0 || contents.size() < topSlotCount) {
            abortScan("invalid-auction-inventory-layout");
            return;
        }

        processAuctionPage(syncId, session.activeTitle, contents.subList(0, topSlotCount));
    }

    @Override
    public void onCloseScreen(int syncId) {
        if (state == AuctionScanState.IDLE || session == null || syncId != session.activeSyncId) {
            return;
        }

        session.activeSyncId = -1;
        session.activeTopSlotCount = -1;
        session.activeTitle = "";

        if (state == AuctionScanState.WAITING_FOR_NEXT_PAGE) {
            logger.info("SCANNER", "Auction screen closed while waiting for next page. Continuing to wait for reopen.");
            return;
        }

        abortScan("auction-screen-closed");
    }

    @Override
    public boolean shouldSuppressScreen(Screen screen) {
        return state != AuctionScanState.IDLE
            && screen != null
            && screenAnalyzer.isAuctionScreenTitle(screen.getTitle().getString());
    }

    @Override
    public boolean isIdle() {
        return state == AuctionScanState.IDLE;
    }

    @Override
    public void addPageObserver(AuctionScanPageObserver observer) {
        pageObservers.add(observer);
    }

    @Override
    public void addLifecycleObserver(AuctionScanLifecycleObserver observer) {
        lifecycleObservers.add(observer);
    }

    public void onAuctionScreenOpened(int syncId, String title, ScreenHandler currentHandler) {
        onOpenScreen(syncId, title, currentHandler);
    }

    public void onAuctionInventory(int syncId, List<ItemStack> contents, ScreenHandler currentHandler) {
        onInventory(syncId, 0, contents, null, currentHandler);
    }

    public void onAuctionScreenClosed(int syncId) {
        onCloseScreen(syncId);
    }

    private void processAuctionPage(int syncId, String title, List<ItemStack> topContents) {
        AuctionScanSession currentSession = requireSession();
        Optional<PageInfo> pageInfoOptional = screenAnalyzer.parsePageInfo(title);
        if (pageInfoOptional.isEmpty()) {
            abortScan("failed-to-parse-page-info");
            return;
        }

        PageInfo pageInfo = pageInfoOptional.get();
        if (state == AuctionScanState.WAITING_FOR_NEXT_PAGE && pageInfo.currentPage() <= currentSession.lastScannedPage) {
            return;
        }

        if (currentSession.scannedPages.contains(pageInfo.currentPage())) {
            return;
        }

        int addedOnPage = 0;
        List<ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot> pageLots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < topContents.size(); slotIndex++) {
            ItemStack stack = topContents.get(slotIndex);
            if (!lotExtractor.looksLikeAuctionLot(stack)) {
                continue;
            }

            var lot = lotExtractor.extract(stack, pageInfo.currentPage(), slotIndex);
            logRawLotComponents(stack, lot);
            currentSession.scannedItems.add(lot);
            pageLots.add(lot);
            addedOnPage++;
        }

        currentSession.scannedPages.add(pageInfo.currentPage());
        currentSession.lastScannedPage = pageInfo.currentPage();

        logger.info(
            "SCANNER",
            "Scanned page " + pageInfo.currentPage() + "/" + pageInfo.totalPages() + ", collected " + addedOnPage + " auction items."
        );

        for (AuctionScanPageObserver observer : pageObservers) {
            if (observer.onPageScanned(syncId, pageInfo.currentPage(), pageInfo.totalPages(), List.copyOf(pageLots)) == AuctionPageDecision.STOP) {
                finishScan(false, "stopped-by-observer");
                return;
            }
        }

        if ((!currentSession.scanAllPages && currentSession.scannedPages.size() >= currentSession.maxPagesToScan)
            || pageInfo.currentPage() >= pageInfo.totalPages()) {
            finishScan(true, null);
            return;
        }

        int nextPageSlot = screenAnalyzer.findNextPageSlot(topContents);
        if (nextPageSlot < 0) {
            finishScan(true, null);
            return;
        }

        state = AuctionScanState.WAITING_FOR_NEXT_PAGE;
        currentSession.pendingNextPageSlot = nextPageSlot;
        currentSession.activeSyncId = syncId;
        currentSession.pageClickDelayTicks = currentSession.pageSwitchDelayTicks;
        currentSession.pageWaitTicks = 0;
        currentSession.nextPageAttempts = 0;
    }

    private void tickWaitForNextPage() {
        AuctionScanSession currentSession = requireSession();
        if (currentSession.pageClickDelayTicks > 0) {
            currentSession.pageClickDelayTicks--;
            return;
        }

        if (currentSession.nextPageAttempts == 0 && currentSession.activeSyncId >= 0) {
            currentSession.pageWaitTicks = 0;
            clickNextPage();
            return;
        }

        currentSession.pageWaitTicks++;

        if (currentSession.pageWaitTicks >= NEXT_PAGE_RETRY_TICKS
            && currentSession.nextPageAttempts < MAX_NEXT_PAGE_ATTEMPTS
            && currentSession.activeSyncId >= 0) {
            currentSession.pageWaitTicks = 0;
            clickNextPage();
            return;
        }

        if (currentSession.pageWaitTicks >= PAGE_TIMEOUT_TICKS) {
            abortScan("next-page-open-timeout");
        }
    }

    private void clickNextPage() {
        AuctionScanSession currentSession = requireSession();
        if (currentSession.activeSyncId < 0 || currentSession.pendingNextPageSlot < 0) {
            abortScan("missing-next-page-context");
            return;
        }

        currentSession.nextPageAttempts++;
        clientGateway.clickSlot(currentSession.activeSyncId, currentSession.pendingNextPageSlot, 0, SlotActionType.PICKUP);
        logger.info(
            "SCANNER",
            "Requested next page click. syncId=" + currentSession.activeSyncId
                + ", slot=" + currentSession.pendingNextPageSlot
                + ", attempt=" + currentSession.nextPageAttempts
        );
    }

    private void abortScan(String reason) {
        finishScan(false, reason);
    }

    private static int normalizeDelayMs(int pageSwitchDelayMs) {
        return pageSwitchDelayMs <= 0 ? DEFAULT_NEXT_PAGE_CLICK_DELAY_MS : pageSwitchDelayMs;
    }

    private static int toDelayTicks(int pageSwitchDelayMs) {
        return Math.max(1, (int) Math.ceil(normalizeDelayMs(pageSwitchDelayMs) / (1000.0 / TICKS_PER_SECOND)));
    }

    private void finishScan(boolean completed, String abortReason) {
        AuctionScanSession currentSession = requireSession();
        state = AuctionScanState.IDLE;

        AuctionScanResult result = new AuctionScanResult(
            currentSession.startedAt,
            JSON_TIMESTAMP_FORMAT.format(LocalDateTime.now()),
            completed,
            abortReason,
            currentSession.scannedPages.size(),
            currentSession.scannedItems.size(),
            List.copyOf(currentSession.scannedItems)
        );

        Path file = resultRepository.save(currentSession.scanFileName, result);
        logger.info(
            "SCANNER",
            (completed ? "Completed" : "Aborted") + " auction scan. Saved " + currentSession.scannedItems.size() + " items to "
                + file.toAbsolutePath()
                + (abortReason == null ? "" : " (reason=" + abortReason + ")")
        );

        AuctionScanResultSummary summary = new AuctionScanResultSummary(
            currentSession.command,
            completed,
            abortReason,
            currentSession.scannedPages.size(),
            currentSession.scannedItems.size()
        );
        for (AuctionScanLifecycleObserver observer : lifecycleObservers) {
            observer.onScanFinished(summary);
        }

        session = null;
    }

    private AuctionScanSession requireSession() {
        return Objects.requireNonNull(session, "Auction scan session is not initialized");
    }

    private void logRawLotComponents(ItemStack stack, ru.geroldina.ftauctionbot.client.domain.auction.model.AuctionLot lot) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        ItemEnchantmentsComponent storedEnchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);

        boolean hasPotionContents = potionContents != null && potionContents.hasEffects();
        boolean hasEnchantments = enchantments != null && !enchantments.isEmpty();
        boolean hasStoredEnchantments = storedEnchantments != null && !storedEnchantments.isEmpty();
        if (!hasPotionContents && !hasEnchantments && !hasStoredEnchantments) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("raw component dump for lot page=" + lot.page()
            + ", slot=" + lot.slotIndex()
            + ", item=" + lot.minecraftId()
            + ", name=\"" + lot.displayName() + "\"");
        lines.add("parsed potionEffects=" + lot.potionEffects());
        lines.add("parsed enchantments=" + lot.enchantments());
        lines.add("raw potionContents=" + potionContents);
        lines.add("raw enchantments=" + enchantments);
        lines.add("raw storedEnchantments=" + storedEnchantments);
        lines.add("raw components=" + stack.getComponents());

        if (hasPotionContents) {
            int effectIndex = 0;
            for (StatusEffectInstance effect : potionContents.getEffects()) {
                RegistryEntry<?> effectType = effect.getEffectType();
                String effectId = effectType.getKey()
                    .map(key -> key.getValue().toString())
                    .orElse(effectType.getIdAsString());
                lines.add("raw potionEffect[" + effectIndex + "]"
                    + " id=" + effectId
                    + ", amplifier=" + effect.getAmplifier()
                    + ", displayedLevel=" + (effect.getAmplifier() + 1)
                    + ", durationTicks=" + effect.getDuration()
                    + ", parsedDurationSeconds=" + (effect.getDuration() / 20)
                    + ", rawObject=" + effect);
                effectIndex++;
            }
        }

        logger.block("SCANNER_DEBUG", lines);
    }
}
