package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.PurchaseHistoryManager;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;

final class AutobuyConfigPresenter {
    private final AutobuyConfigManager configManager;
    private final AutobuyLoopControl autobuyLoopControl;
    private final PurchaseHistoryManager purchaseHistoryManager;
    private final AutobuyConfigSession session;
    private final AutobuyPickerCatalog pickerCatalog;
    private final Runnable rebuildUi;
    private final Runnable closeScreen;

    AutobuyConfigPresenter(
        AutobuyConfigManager configManager,
        AutobuyLoopControl autobuyLoopControl,
        PurchaseHistoryManager purchaseHistoryManager,
        AutobuyConfigSession session,
        AutobuyPickerCatalog pickerCatalog,
        Runnable rebuildUi,
        Runnable closeScreen
    ) {
        this.configManager = configManager;
        this.autobuyLoopControl = autobuyLoopControl;
        this.purchaseHistoryManager = purchaseHistoryManager;
        this.session = session;
        this.pickerCatalog = pickerCatalog;
        this.rebuildUi = rebuildUi;
        this.closeScreen = closeScreen;
    }

    void initialize() {
        session.reset(configManager.getCurrentConfig(), "Загружена активная конфигурация.");
        session.purchaseHistoryEntries(purchaseHistoryManager.load());
    }

    void requestClose() {
        if (session.dirty() && !session.confirmClosePending()) {
            session.confirmClosePending(true);
            session.statusMessage("Есть несохранённые изменения. Сохраните или отмените их перед выходом.");
            rebuildUi.run();
            return;
        }
        closeScreen.run();
    }

    void save() {
        session.revalidate();
        if (!session.isValid()) {
            session.statusMessage("Сначала исправьте ошибки валидации.");
            rebuildUi.run();
            return;
        }

        try {
            configManager.saveAndReload(session.draft().toDomain());
            session.reset(configManager.getCurrentConfig(), "Конфигурация сохранена.");
        } catch (RuntimeException exception) {
            session.statusMessage("Ошибка сохранения: " + exception.getMessage());
        }
        rebuildUi.run();
    }

    void saveAndClose() {
        save();
        if (!session.dirty()) {
            closeScreen.run();
        }
    }

    void reloadFromFile() {
        try {
            AutobuyConfig config = configManager.reload();
            session.reset(config, "Конфигурация заново загружена из файла.");
        } catch (RuntimeException exception) {
            session.statusMessage("Ошибка загрузки: " + exception.getMessage());
        }
        rebuildUi.run();
    }

    void discardDraft() {
        session.reset(configManager.getCurrentConfig(), "Несохранённые изменения отменены.");
        rebuildUi.run();
    }

    void discardAndClose() {
        session.dirty(false);
        session.confirmClosePending(false);
        closeScreen.run();
    }

    void cancelCloseConfirmation() {
        session.confirmClosePending(false);
        session.statusMessage("Закрытие отменено.");
        rebuildUi.run();
    }

    void toggleAutobuy() {
        if (autobuyLoopControl.isEnabled()) {
            autobuyLoopControl.stop();
            session.statusMessage("Цикл автобая остановлен.");
        } else {
            autobuyLoopControl.start();
            session.statusMessage("Цикл автобая запущен.");
        }
        rebuildUi.run();
    }

    boolean isAutobuyEnabled() {
        return autobuyLoopControl.isEnabled();
    }

    void selectTab(AutobuyScreenTab tab) {
        session.activeTab(tab);
        if (tab == AutobuyScreenTab.PURCHASE_HISTORY) {
            session.purchaseHistoryEntries(purchaseHistoryManager.load());
        }
        rebuildUi.run();
    }

    void reloadPurchaseHistory() {
        session.purchaseHistoryEntries(purchaseHistoryManager.load());
        session.statusMessage("История покупок обновлена.");
        rebuildUi.run();
    }

    void openRuleEditor(int ruleIndex) {
        session.selectedRuleIndex(ruleIndex);
        session.editorScrollProgress(0);
        session.confirmClosePending(false);
        rebuildUi.run();
    }

    void createRule() {
        AutobuyConfigDraft.BuyRuleDraft rule = new AutobuyConfigDraft.BuyRuleDraft();
        int nextIndex = session.draft().buyRules.size() + 1;
        rule.id = "rule_" + nextIndex;
        rule.name = "Правило " + nextIndex;
        session.draft().buyRules.add(rule);
        session.selectedRuleIndex(session.draft().buyRules.size() - 1);
        session.markDirty("Создано правило " + rule.id + ".");
        rebuildUi.run();
    }

    void moveSelectedRule(int delta) {
        int selectedRuleIndex = session.selectedRuleIndex();
        int targetIndex = selectedRuleIndex + delta;
        if (selectedRuleIndex < 0 || selectedRuleIndex >= session.draft().buyRules.size() || targetIndex < 0 || targetIndex >= session.draft().buyRules.size()) {
            return;
        }

        AutobuyConfigDraft.BuyRuleDraft rule = session.draft().buyRules.remove(selectedRuleIndex);
        session.draft().buyRules.add(targetIndex, rule);
        session.selectedRuleIndex(targetIndex);
        session.markDirty("Порядок правил изменён.");
        rebuildUi.run();
    }

    void duplicateSelectedRule() {
        int selectedRuleIndex = session.selectedRuleIndex();
        if (selectedRuleIndex < 0 || selectedRuleIndex >= session.draft().buyRules.size()) {
            return;
        }

        AutobuyConfigDraft.BuyRuleDraft copy = AutobuyConfigDraft.BuyRuleDraft.fromDomain(session.draft().buyRules.get(selectedRuleIndex).toDomain());
        copy.id = AutobuyUiTextSupport.isBlank(copy.id) ? "rule_copy" : copy.id + "_copy";
        copy.name = AutobuyUiTextSupport.isBlank(copy.name) ? "Копия правила" : copy.name + " Копия";
        session.draft().buyRules.add(selectedRuleIndex + 1, copy);
        session.selectedRuleIndex(selectedRuleIndex + 1);
        session.markDirty("Правило дублировано.");
        rebuildUi.run();
    }

    void deleteSelectedRule() {
        int selectedRuleIndex = session.selectedRuleIndex();
        if (selectedRuleIndex < 0 || selectedRuleIndex >= session.draft().buyRules.size()) {
            return;
        }

        session.draft().buyRules.remove(selectedRuleIndex);
        if (session.draft().buyRules.isEmpty()) {
            session.selectedRuleIndex(-1);
        } else if (selectedRuleIndex >= session.draft().buyRules.size()) {
            session.selectedRuleIndex(session.draft().buyRules.size() - 1);
        }
        session.markDirty("Правило удалено.");
        rebuildUi.run();
    }

    void toggleRuleEnabled(AutobuyConfigDraft.BuyRuleDraft rule) {
        rule.enabled = !rule.enabled;
        session.markDirty(rule.enabled ? "Правило включено." : "Правило выключено.");
        rebuildUi.run();
    }

    void moveCondition(AutobuyConfigDraft.BuyRuleDraft rule, int index, int delta) {
        int targetIndex = index + delta;
        if (targetIndex < 0 || targetIndex >= rule.conditions.size()) {
            return;
        }

        AutobuyConfigDraft.ConditionDraft condition = rule.conditions.remove(index);
        rule.conditions.add(targetIndex, condition);
        session.markDirty("Порядок условий изменён.");
        rebuildUi.run();
    }

    void deleteCondition(AutobuyConfigDraft.BuyRuleDraft rule, int index) {
        if (index < 0 || index >= rule.conditions.size()) {
            return;
        }

        rule.conditions.remove(index);
        session.markDirty("Условие удалено.");
        rebuildUi.run();
    }

    void addCondition(AutobuyConfigDraft.BuyRuleDraft rule, AutobuyConfigDraft.ConditionType type) {
        rule.conditions.add(AutobuyConfigDraft.ConditionDraft.create(type));
        session.markDirty("Добавлено условие: " + AutobuyUiTextSupport.localizeConditionType(type) + ".");
        rebuildUi.run();
    }

    void addEnchantment(AutobuyConfigDraft.ConditionDraft condition) {
        condition.requiredEnchantments.add(new AutobuyConfigDraft.RequiredEnchantmentDraft("", null));
        session.markDirty("Добавлена строка зачарования.");
        rebuildUi.run();
    }

    void removeEnchantment(AutobuyConfigDraft.ConditionDraft condition, AutobuyConfigDraft.RequiredEnchantmentDraft enchantment) {
        condition.requiredEnchantments.remove(enchantment);
        session.markDirty("Удалена строка зачарования.");
        rebuildUi.run();
    }

    void addPotionEffect(AutobuyConfigDraft.ConditionDraft condition) {
        condition.requiredPotionEffects.add(new AutobuyConfigDraft.RequiredPotionEffectDraft("", null, null));
        session.markDirty("Добавлена строка эффекта.");
        rebuildUi.run();
    }

    void removePotionEffect(AutobuyConfigDraft.ConditionDraft condition, AutobuyConfigDraft.RequiredPotionEffectDraft effect) {
        condition.requiredPotionEffects.remove(effect);
        session.markDirty("Удалена строка эффекта.");
        rebuildUi.run();
    }

    void addStringListEntry(AutobuyConfigDraft.ConditionDraft condition) {
        condition.stringList.add("");
        session.markDirty("Добавлена строка списка.");
        rebuildUi.run();
    }

    void removeStringListEntry(AutobuyConfigDraft.ConditionDraft condition, int index) {
        condition.stringList.remove(index);
        session.markDirty("Удалена строка списка.");
        rebuildUi.run();
    }

    void updateScanInterval(String value) {
        session.draft().scanIntervalSeconds = AutobuyDraftParsing.clampPositive(AutobuyDraftParsing.parseInteger(value), 30);
        markFieldChanged("Интервал сканирования, сек");
    }

    void updateScanPageLimit(String value) {
        session.draft().scanPageLimit = AutobuyDraftParsing.clampPositive(AutobuyDraftParsing.parseInteger(value), 10);
        markFieldChanged("Лимит страниц");
    }

    void updatePageSwitchDelay(String value) {
        session.draft().pageSwitchDelayMs = AutobuyDraftParsing.clampPositive(AutobuyDraftParsing.parseInteger(value), 200);
        markFieldChanged("Задержка смены страниц, мс");
    }

    void cycleLogMode() {
        session.draft().scanLogMode = session.draft().scanLogMode == AutobuyScanLogMode.ALL
            ? AutobuyScanLogMode.MATCHED_ONLY
            : AutobuyScanLogMode.ALL;
        session.markDirty("Изменён режим логирования.");
        rebuildUi.run();
    }

    void updateRuleId(AutobuyConfigDraft.BuyRuleDraft rule, String value) {
        rule.id = value;
        markFieldChanged("Идентификатор");
    }

    void updateRuleName(AutobuyConfigDraft.BuyRuleDraft rule, String value) {
        rule.name = value;
        markFieldChanged("Название");
    }

    void updateRuleEnabled(AutobuyConfigDraft.BuyRuleDraft rule, boolean value) {
        rule.enabled = value;
        session.markDirty("Изменён флаг активности правила.");
    }

    void updateConditionString(AutobuyConfigDraft.ConditionDraft condition, String label, String value) {
        condition.stringValue = value;
        markFieldChanged(label);
    }

    void updateConditionInteger(AutobuyConfigDraft.ConditionDraft condition, String label, String value) {
        condition.intValue = AutobuyDraftParsing.parseInteger(value);
        markFieldChanged(label);
    }

    void updateConditionLong(AutobuyConfigDraft.ConditionDraft condition, String label, String value) {
        condition.longValue = AutobuyDraftParsing.parseLong(value);
        markFieldChanged(label);
    }

    void updateEnchantmentId(AutobuyConfigDraft.RequiredEnchantmentDraft enchantment, String value) {
        enchantment.id = value;
        markFieldChanged("зачарование");
    }

    void updateEnchantmentLevel(AutobuyConfigDraft.RequiredEnchantmentDraft enchantment, String value) {
        enchantment.level = AutobuyDraftParsing.parseInteger(value);
        markFieldChanged("уровень");
    }

    void updatePotionEffectId(AutobuyConfigDraft.RequiredPotionEffectDraft effect, String value) {
        effect.id = value;
        markFieldChanged("эффект");
    }

    void updatePotionEffectLevel(AutobuyConfigDraft.RequiredPotionEffectDraft effect, String value) {
        effect.level = AutobuyDraftParsing.parseInteger(value);
        markFieldChanged("уровень");
    }

    void updatePotionEffectDuration(AutobuyConfigDraft.RequiredPotionEffectDraft effect, String value) {
        effect.durationSeconds = AutobuyDraftParsing.parseInteger(value);
        markFieldChanged("длительность");
    }

    void updateStringListEntry(AutobuyConfigDraft.ConditionDraft condition, int index, String value) {
        condition.stringList.set(index, value);
        markFieldChanged("значение");
    }

    void openItemPicker(AutobuyConfigDraft.ConditionDraft condition) {
        session.activePicker(pickerCatalog.openItemPicker(value -> {
            condition.stringValue = value;
            session.markDirty("Выбрано значение: " + value + ".");
        }));
        rebuildUi.run();
    }

    void openEnchantmentPicker(AutobuyConfigDraft.RequiredEnchantmentDraft enchantment) {
        session.activePicker(pickerCatalog.openEnchantmentPicker(value -> {
            enchantment.id = value;
            session.markDirty("Выбрано значение: " + value + ".");
        }));
        rebuildUi.run();
    }

    void openPotionEffectPicker(AutobuyConfigDraft.RequiredPotionEffectDraft effect) {
        session.activePicker(pickerCatalog.openPotionEffectPicker(value -> {
            effect.id = value;
            session.markDirty("Выбрано значение: " + value + ".");
        }));
        rebuildUi.run();
    }

    void updatePickerQuery(String value) {
        SearchPickerState picker = session.activePicker();
        if (picker == null) {
            return;
        }
        picker.query = value == null ? "" : value;
    }

    void selectPickerEntry(SearchPickerEntry entry) {
        SearchPickerState picker = session.activePicker();
        if (picker == null) {
            return;
        }

        picker.onPick.accept(entry.id());
        session.activePicker(null);
        rebuildUi.run();
    }

    void closePicker() {
        session.activePicker(null);
        rebuildUi.run();
    }

    private void markFieldChanged(String label) {
        session.markDirty("Изменено поле: " + label + ".");
    }
}
