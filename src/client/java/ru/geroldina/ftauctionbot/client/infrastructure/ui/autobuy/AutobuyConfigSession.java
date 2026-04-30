package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.util.List;

final class AutobuyConfigSession {
    private final AutobuyConfigValidator validator;

    private AutobuyConfigDraft draft = AutobuyConfigDraft.fromDomain(AutobuyConfig.empty());
    private int selectedRuleIndex = -1;
    private boolean dirty;
    private boolean confirmClosePending;
    private String statusMessage = "";
    private List<String> validationErrors = List.of();
    private AutobuyScreenTab activeTab = AutobuyScreenTab.CONFIG;
    private List<PurchaseHistoryEntry> purchaseHistoryEntries = List.of();
    private List<MarketResearchResult> marketResearchResults = List.of();
    private SearchPickerState activePicker;
    private double ruleListScrollProgress;
    private double editorScrollProgress;
    private double historyScrollProgress;
    private double marketResearchScrollProgress;
    private double pickerResultsScrollProgress;

    AutobuyConfigSession(AutobuyConfigValidator validator) {
        this.validator = validator;
    }

    void reset(AutobuyConfig config, String message) {
        this.draft = AutobuyConfigDraft.fromDomain(config);
        if (draft.buyRules.isEmpty()) {
            selectedRuleIndex = -1;
        } else if (selectedRuleIndex < 0 || selectedRuleIndex >= draft.buyRules.size()) {
            selectedRuleIndex = 0;
        }
        dirty = false;
        confirmClosePending = false;
        statusMessage = message;
        activePicker = null;
        revalidate();
    }

    void markDirty(String message) {
        dirty = true;
        confirmClosePending = false;
        statusMessage = message;
        revalidate();
    }

    void revalidate() {
        validationErrors = validator.validate(draft).errors();
    }

    AutobuyConfigDraft draft() {
        return draft;
    }

    int selectedRuleIndex() {
        return selectedRuleIndex;
    }

    void selectedRuleIndex(int selectedRuleIndex) {
        this.selectedRuleIndex = selectedRuleIndex;
    }

    boolean dirty() {
        return dirty;
    }

    void dirty(boolean dirty) {
        this.dirty = dirty;
    }

    boolean confirmClosePending() {
        return confirmClosePending;
    }

    void confirmClosePending(boolean confirmClosePending) {
        this.confirmClosePending = confirmClosePending;
    }

    String statusMessage() {
        return statusMessage;
    }

    void statusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    List<String> validationErrors() {
        return validationErrors;
    }

    boolean isValid() {
        return validationErrors.isEmpty();
    }

    SearchPickerState activePicker() {
        return activePicker;
    }

    void activePicker(SearchPickerState activePicker) {
        this.activePicker = activePicker;
    }

    AutobuyScreenTab activeTab() {
        return activeTab;
    }

    void activeTab(AutobuyScreenTab activeTab) {
        this.activeTab = activeTab;
    }

    List<PurchaseHistoryEntry> purchaseHistoryEntries() {
        return purchaseHistoryEntries;
    }

    void purchaseHistoryEntries(List<PurchaseHistoryEntry> purchaseHistoryEntries) {
        this.purchaseHistoryEntries = purchaseHistoryEntries;
    }

    List<MarketResearchResult> marketResearchResults() {
        return marketResearchResults;
    }

    void marketResearchResults(List<MarketResearchResult> marketResearchResults) {
        this.marketResearchResults = marketResearchResults;
    }

    double ruleListScrollProgress() {
        return ruleListScrollProgress;
    }

    void ruleListScrollProgress(double ruleListScrollProgress) {
        this.ruleListScrollProgress = ruleListScrollProgress;
    }

    double editorScrollProgress() {
        return editorScrollProgress;
    }

    void editorScrollProgress(double editorScrollProgress) {
        this.editorScrollProgress = editorScrollProgress;
    }

    double historyScrollProgress() {
        return historyScrollProgress;
    }

    void historyScrollProgress(double historyScrollProgress) {
        this.historyScrollProgress = historyScrollProgress;
    }

    double marketResearchScrollProgress() {
        return marketResearchScrollProgress;
    }

    void marketResearchScrollProgress(double marketResearchScrollProgress) {
        this.marketResearchScrollProgress = marketResearchScrollProgress;
    }

    double pickerResultsScrollProgress() {
        return pickerResultsScrollProgress;
    }

    void pickerResultsScrollProgress(double pickerResultsScrollProgress) {
        this.pickerResultsScrollProgress = pickerResultsScrollProgress;
    }
}
