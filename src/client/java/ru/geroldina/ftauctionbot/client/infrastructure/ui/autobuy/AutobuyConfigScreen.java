package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyLoopController;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class AutobuyConfigScreen extends BaseOwoScreen<StackLayout> {
    private static final int SCREEN_WIDTH_PERCENT = 92;
    private static final int SCREEN_HEIGHT_PERCENT = 88;
    private static final int RULE_LIST_WIDTH = 248;
    private static final int INFO_PANEL_WIDTH = 238;
    private static final int PICKER_RESULT_LIMIT = 120;
    private static final Identifier UI_FONT = Identifier.of("minecraft", "uniform");
    private static final int APP_BACKGROUND = 0xE40B0E13;
    private static final int PANEL_BACKGROUND = 0xCC10151C;
    private static final int CARD_BACKGROUND = 0xCC171E27;
    private static final int SUBTLE_CARD_BACKGROUND = 0xB8141A22;
    private static final int PANEL_OUTLINE = 0xFF232D39;
    private static final int ACCENT_OUTLINE = 0xFF39495C;
    private static final int SELECTED_CARD = 0xFF202C38;
    private static final int SELECTED_OUTLINE = 0xFF657A94;
    private static final int TEXT_PRIMARY = 0xFFF3F5F7;
    private static final int TEXT_SECONDARY = 0xFF9AA8B8;
    private static final int TEXT_MUTED = 0xFF6C7A89;
    private static final int TEXT_SUCCESS = 0xFF85C79B;
    private static final int TEXT_WARNING = 0xFFF0B486;
    private static final int TEXT_DANGER = 0xFFFFB4A2;
    private static final int BUTTON_SECONDARY = 0xFF151E27;
    private static final int BUTTON_SECONDARY_HOVER = 0xFF223040;
    private static final int BUTTON_SUCCESS = 0xFF234B42;
    private static final int BUTTON_SUCCESS_HOVER = 0xFF337163;
    private static final int BUTTON_SELECTED = 0xFF30455A;
    private static final int BUTTON_SELECTED_HOVER = 0xFF42627E;
    private static final int SCROLLBAR_COLOR = 0x90A7B9CC;

    private final AutobuyConfigManager configManager;
    private final AutobuyLoopController autobuyLoopController;

    private AutobuyConfigDraft draft;
    private int selectedRuleIndex = -1;
    private boolean dirty;
    private boolean confirmClosePending;
    private String statusMessage = "";
    private List<String> validationErrors = List.of();
    private ButtonComponent saveButton;
    private SearchPickerState activePicker;
    private UiScrollContainer<FlowLayout> ruleListScroll;
    private UiScrollContainer<FlowLayout> editorScroll;
    private UiScrollContainer<FlowLayout> pickerResultsScroll;
    private double ruleListScrollProgress;
    private double editorScrollProgress;
    private double pickerResultsScrollProgress;

    public AutobuyConfigScreen(AutobuyConfigManager configManager, AutobuyLoopController autobuyLoopController) {
        super(uiText("Конфигурация автобая"));
        this.configManager = configManager;
        this.autobuyLoopController = autobuyLoopController;
        resetDraft(configManager.getCurrentConfig(), false, "Загружена активная конфигурация.");
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::stack);
    }

    @Override
    protected void build(StackLayout rootComponent) {
        rebuildUi();
    }

    @Override
    public void close() {
        if (dirty && !confirmClosePending) {
            confirmClosePending = true;
            statusMessage = "Есть несохранённые изменения. Сохраните или отмените их перед выходом.";
            rebuildUi();
            return;
        }

        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void rebuildUi() {
        validationErrors = validateDraft();
        captureScrollState();

        StackLayout root = this.uiAdapter.rootComponent;
        root.clearChildren();
        root.surface(Surface.flat(0xD107090C));
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        FlowLayout frame = Containers.verticalFlow(Sizing.fill(SCREEN_WIDTH_PERCENT), Sizing.fill(SCREEN_HEIGHT_PERCENT));
        frame.surface(Surface.flat(APP_BACKGROUND).and(Surface.outline(ACCENT_OUTLINE)));
        frame.padding(Insets.of(10));
        frame.gap(8);
        root.child(frame);

        frame.child(buildHeader());
        frame.child(buildBody());

        updateSaveButtonState();
        if (activePicker != null) {
            root.child(buildPickerOverlay());
        }
    }

    private ParentComponent buildHeader() {
        FlowLayout header = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        header.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        header.padding(Insets.of(8));
        header.gap(8);

        FlowLayout metaRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        metaRow.gap(6);
        metaRow.child(sectionTag("АВТОБАЙ"));
        metaRow.child(sectionTag(dirty ? "ЧЕРНОВИК" : "СИНХР."));
        metaRow.child(sectionTag(autobuyLoopController.isEnabled() ? "ЗАПУЩЕН" : "ОСТАНОВЛЕН"));
        metaRow.child(horizontalSpacer());
        metaRow.child(mutedLabel("F7 или /ftab gui"));
        header.child(metaRow);

        FlowLayout titleRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        titleRow.child(primaryLabel("Доступные конфигурации"));
        titleRow.child(horizontalSpacer());
        saveButton = actionButton("Сохранить", button -> saveDraft(), true);
        titleRow.child(saveButton);
        titleRow.child(actionButton("Загрузить", button -> reloadFromFile(), false));
        titleRow.child(actionButton("Сбросить", button -> discardDraft(), false));
        titleRow.child(actionButton(autobuyLoopController.isEnabled() ? "Стоп" : "Старт", button -> toggleAutobuy(), false));
        titleRow.child(actionButton("Закрыть", button -> close(), false));
        header.child(titleRow);

        FlowLayout summaryRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        summaryRow.gap(6);
        summaryRow.child(statPill("Правила", String.valueOf(draft.buyRules.size())));
        summaryRow.child(statPill("Задержка", draft.pageSwitchDelayMs + " мс"));
        summaryRow.child(statPill("Логи", localizeLogMode(draft.scanLogMode)));
        summaryRow.child(horizontalSpacer());
        summaryRow.child(Components.label(uiText(buildRuntimeStatus()))
            .<LabelComponent>configure(label -> label.color(Color.ofRgb(validationErrors.isEmpty() ? TEXT_SUCCESS : TEXT_WARNING))));
        header.child(summaryRow);

        if (!statusMessage.isBlank()) {
            header.child(infoStripe(statusMessage, validationErrors.isEmpty() ? TEXT_WARNING : TEXT_DANGER));
        }

        if (confirmClosePending) {
            FlowLayout confirmRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            confirmRow.gap(6);
            confirmRow.child(Components.label(uiText("Закрыть окно с несохранёнными изменениями?"))
                .<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_DANGER))));
            confirmRow.child(actionButton("Сохранить и закрыть", button -> saveDraftAndClose(), true));
            confirmRow.child(actionButton("Без сохранения", button -> discardAndClose(), false));
            confirmRow.child(actionButton("Отмена", button -> {
                confirmClosePending = false;
                statusMessage = "Закрытие отменено.";
                rebuildUi();
            }, false));
            header.child(confirmRow);
        }

        if (!validationErrors.isEmpty()) {
            for (String error : validationErrors) {
                header.child(infoStripe(error, TEXT_DANGER));
            }
        }

        return header;
    }

    private ParentComponent buildBody() {
        FlowLayout body = Containers.horizontalFlow(Sizing.fill(), Sizing.expand());
        body.gap(8);
        body.child(buildRuleListPanel());
        body.child(buildEditorPanel());
        body.child(buildInfoPanel());
        return body;
    }

    private ParentComponent buildRuleListPanel() {
        FlowLayout outer = Containers.verticalFlow(Sizing.fixed(RULE_LIST_WIDTH), Sizing.fill());
        outer.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        outer.padding(Insets.of(8));
        outer.gap(8);

        outer.child(primaryLabel("Конфигурации"));
        outer.child(mutedLabel("Плотный список правил с иконками и статусом"));
        outer.child(actionButton("Создать", button -> createRule(), true));

        FlowLayout ruleList = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        ruleList.gap(5);
        if (draft.buyRules.isEmpty()) {
            ruleList.child(emptyState("Пока пусто", "Создайте первое правило, чтобы начать настройку."));
        } else {
            for (int i = 0; i < draft.buyRules.size(); i++) {
                ruleList.child(buildRuleListEntry(i, draft.buyRules.get(i)));
            }
        }
        ruleListScroll = styledVerticalScroll(Sizing.fill(), Sizing.expand(), ruleList, ruleListScrollProgress);
        outer.child(ruleListScroll);

        if (selectedRuleIndex >= 0 && selectedRuleIndex < draft.buyRules.size()) {
            FlowLayout actions = Containers.verticalFlow(Sizing.fill(), Sizing.content());
            actions.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
            actions.padding(Insets.of(7));
            actions.gap(6);
            actions.child(mutedLabel("Действия для выбранного правила"));

            FlowLayout rowOne = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            rowOne.gap(4);
            rowOne.child(smallAction("Выше", button -> moveSelectedRule(-1)));
            rowOne.child(smallAction("Ниже", button -> moveSelectedRule(1)));
            actions.child(rowOne);

            FlowLayout rowTwo = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            rowTwo.gap(4);
            rowTwo.child(smallAction("Дубль", button -> duplicateSelectedRule()));
            rowTwo.child(smallAction("Удалить", button -> deleteSelectedRule()));
            actions.child(rowTwo);
            outer.child(actions);
        }

        return outer;
    }

    private ParentComponent buildEditorPanel() {
        FlowLayout editorContent = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        editorContent.gap(8);
        editorContent.child(buildGlobalSettingsCard());
        if (selectedRuleIndex >= 0 && selectedRuleIndex < draft.buyRules.size()) {
            editorContent.child(buildSelectedRuleCard(draft.buyRules.get(selectedRuleIndex)));
        } else {
            editorContent.child(emptyCard("Рабочая область", "Выберите правило слева или создайте новое."));
        }
        editorContent.child(Components.box(Sizing.fill(), Sizing.fixed(28)));

        FlowLayout panel = Containers.verticalFlow(Sizing.expand(), Sizing.fill());
        panel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        panel.padding(Insets.of(8));
        panel.gap(6);
        panel.child(primaryLabel("Редактор"));
        panel.child(mutedLabel("Изменение параметров сканирования и выбранного правила"));
        editorScroll = styledVerticalScroll(Sizing.fill(), Sizing.fill(), editorContent, editorScrollProgress);
        panel.child(editorScroll);
        return panel;
    }

    private ParentComponent buildInfoPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fixed(INFO_PANEL_WIDTH), Sizing.fill());
        panel.surface(Surface.flat(PANEL_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        panel.padding(Insets.of(8));
        panel.gap(8);

        panel.child(primaryLabel("Информация"));
        panel.child(mutedLabel("Метаданные и быстрые действия"));
        panel.child(buildInfoCard());
        panel.child(buildInfoActions());
        if (!validationErrors.isEmpty()) {
            panel.child(buildValidationPreview());
        }
        return panel;
    }

    private ParentComponent buildGlobalSettingsCard() {
        FlowLayout card = card("Параметры конфигурации", "Общие настройки для всего набора правил.");

        FlowLayout stats = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        stats.gap(6);
        stats.child(statPill("Интервал", draft.scanIntervalSeconds + " с"));
        stats.child(statPill("Страниц", String.valueOf(draft.scanPageLimit)));
        stats.child(statPill("Смена", draft.pageSwitchDelayMs + " мс"));
        card.child(stats);

        FlowLayout topRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        topRow.gap(6);
        topRow.child(halfWidth(integerField("Интервал сканирования, сек", draft.scanIntervalSeconds, value -> draft.scanIntervalSeconds = clampPositive(value, 30))));
        topRow.child(halfWidth(integerField("Лимит страниц", draft.scanPageLimit, value -> draft.scanPageLimit = clampPositive(value, 10))));
        card.child(topRow);

        FlowLayout bottomRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        bottomRow.gap(6);
        bottomRow.child(halfWidth(integerField("Задержка смены страниц, мс", draft.pageSwitchDelayMs, value -> draft.pageSwitchDelayMs = clampPositive(value, 200))));
        bottomRow.child(halfWidth(cycleField("Режим логов", localizeLogMode(draft.scanLogMode), () -> {
            draft.scanLogMode = draft.scanLogMode == AutobuyScanLogMode.ALL
                ? AutobuyScanLogMode.MATCHED_ONLY
                : AutobuyScanLogMode.ALL;
            markDirty("Изменён режим логирования.");
            rebuildUi();
        })));
        card.child(bottomRow);
        return card;
    }

    private ParentComponent buildSelectedRuleCard(AutobuyConfigDraft.BuyRuleDraft rule) {
        FlowLayout card = card("Редактор правила", "Центральная область настройки выбранного правила.");

        FlowLayout hero = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        hero.gap(6);
        hero.child(statPill("Правило", displayRuleTitle(rule, selectedRuleIndex)));
        hero.child(statPill("Условий", String.valueOf(rule.conditions.size())));
        hero.child(statPill("Статус", rule.enabled ? "Активно" : "Выключено"));
        card.child(hero);

        FlowLayout fieldRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        fieldRow.gap(6);
        fieldRow.child(halfWidth(textField("Идентификатор", rule.id, value -> rule.id = value)));
        fieldRow.child(halfWidth(textField("Название", rule.name, value -> rule.name = value)));
        card.child(fieldRow);

        CheckboxComponent enabledCheckbox = Components.checkbox(uiText("Включено"));
        enabledCheckbox.checked(rule.enabled);
        enabledCheckbox.onChanged(value -> {
            rule.enabled = value;
            markDirty("Изменён флаг активности правила.");
        });
        card.child(enabledCheckbox);

        card.child(primaryLabel("Условия"));
        if (rule.conditions.isEmpty()) {
            card.child(emptyState("Нет условий", "Добавьте фильтры по предмету, цене, чарам, эффектам или продавцу."));
        } else {
            for (int i = 0; i < rule.conditions.size(); i++) {
                card.child(buildConditionCard(rule, i, rule.conditions.get(i)));
            }
        }

        card.child(buildConditionPalette(rule));
        return card;
    }

    private ParentComponent buildConditionCard(AutobuyConfigDraft.BuyRuleDraft rule, int index, AutobuyConfigDraft.ConditionDraft condition) {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        card.padding(Insets.of(7));
        card.gap(6);

        FlowLayout header = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        header.child(sectionTag(localizeConditionType(condition.type).toUpperCase(Locale.ROOT)));
        header.child(horizontalSpacer());
        header.child(smallAction("Выше", button -> moveCondition(rule, index, -1)));
        header.child(smallAction("Ниже", button -> moveCondition(rule, index, 1)));
        header.child(smallAction("Удалить", button -> deleteCondition(rule, index)));
        card.child(header);

        switch (condition.type) {
            case MINECRAFT_ID -> card.child(itemPickerField("Предмет Minecraft", condition.stringValue, value -> condition.stringValue = value));
            case DISPLAY_NAME -> card.child(textField("Поиск по названию", condition.stringValue, value -> condition.stringValue = value));
            case MAX_TOTAL_PRICE -> card.child(longField("Макс. общая цена", condition.longValue, value -> condition.longValue = value));
            case MAX_UNIT_PRICE -> card.child(longField("Макс. цена за штуку", condition.longValue, value -> condition.longValue = value));
            case MIN_COUNT -> card.child(integerFieldNullable("Мин. количество", condition.intValue, value -> condition.intValue = value));
            case MAX_COUNT -> card.child(integerFieldNullable("Макс. количество", condition.intValue, value -> condition.intValue = value));
            case REQUIRED_ENCHANTMENTS -> buildRequiredEnchantmentsEditor(card, condition);
            case REQUIRED_POTION_EFFECTS -> buildRequiredPotionEffectsEditor(card, condition);
            case SELLER_ALLOW_LIST, SELLER_DENY_LIST -> buildStringListEditor(card, condition);
        }

        return card;
    }

    private ParentComponent buildConditionPalette(AutobuyConfigDraft.BuyRuleDraft rule) {
        FlowLayout palette = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        palette.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        palette.padding(Insets.of(7));
        palette.gap(6);
        palette.child(primaryLabel("Добавить условие"));
        palette.child(mutedLabel("Соберите правило из фильтров цены, предмета, эффектов и продавцов"));

        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(4);
        int index = 0;
        for (AutobuyConfigDraft.ConditionType type : AutobuyConfigDraft.ConditionType.values()) {
            if (index == 4) {
                palette.child(row);
                row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
            }
            row.child(smallAction(shortTypeName(type), button -> {
                rule.conditions.add(AutobuyConfigDraft.ConditionDraft.create(type));
                markDirty("Добавлено условие: " + localizeConditionType(type) + ".");
                rebuildUi();
            }, true));
            index++;
        }
        palette.child(row);
        return palette;
    }

    private void buildRequiredEnchantmentsEditor(FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.requiredEnchantments.isEmpty()) {
            parent.child(mutedLabel("Нет обязательных зачарований"));
        } else {
            for (AutobuyConfigDraft.RequiredEnchantmentDraft enchantment : condition.requiredEnchantments) {
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(enchantmentPickerCompact(enchantment.id, value -> enchantment.id = value));
                row.child(integerFieldCompact("уровень", enchantment.level, value -> enchantment.level = value));
                row.child(smallAction("Убрать", button -> {
                    condition.requiredEnchantments.remove(enchantment);
                    markDirty("Удалена строка зачарования.");
                    rebuildUi();
                }));
                parent.child(row);
            }
        }
        parent.child(smallAction("Добавить чар", button -> {
            condition.requiredEnchantments.add(new AutobuyConfigDraft.RequiredEnchantmentDraft("", null));
            markDirty("Добавлена строка зачарования.");
            rebuildUi();
        }, true));
    }

    private void buildRequiredPotionEffectsEditor(FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.requiredPotionEffects.isEmpty()) {
            parent.child(mutedLabel("Нет обязательных эффектов"));
        } else {
            for (AutobuyConfigDraft.RequiredPotionEffectDraft effect : condition.requiredPotionEffects) {
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(potionEffectPickerCompact(effect.id, value -> effect.id = value));
                row.child(integerFieldCompact("уровень", effect.level, value -> effect.level = value));
                row.child(integerFieldCompact("длительность", effect.durationSeconds, value -> effect.durationSeconds = value));
                row.child(smallAction("Убрать", button -> {
                    condition.requiredPotionEffects.remove(effect);
                    markDirty("Удалена строка эффекта.");
                    rebuildUi();
                }));
                parent.child(row);
            }
        }
        parent.child(smallAction("Добавить эффект", button -> {
            condition.requiredPotionEffects.add(new AutobuyConfigDraft.RequiredPotionEffectDraft("", null, null));
            markDirty("Добавлена строка эффекта.");
            rebuildUi();
        }, true));
    }

    private void buildStringListEditor(FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.stringList.isEmpty()) {
            parent.child(mutedLabel("Нет записей"));
        } else {
            for (int i = 0; i < condition.stringList.size(); i++) {
                int index = i;
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(textFieldCompact("значение", condition.stringList.get(i), value -> condition.stringList.set(index, value)));
                row.child(smallAction("Убрать", button -> {
                    condition.stringList.remove(index);
                    markDirty("Удалена строка списка.");
                    rebuildUi();
                }));
                parent.child(row);
            }
        }
        parent.child(smallAction("Добавить запись", button -> {
            condition.stringList.add("");
            markDirty("Добавлена строка списка.");
            rebuildUi();
        }, true));
    }

    private FlowLayout card(String title, String subtitle) {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(CARD_BACKGROUND).and(Surface.outline(ACCENT_OUTLINE)));
        card.padding(Insets.of(8));
        card.gap(6);
        card.child(primaryLabel(title));
        if (subtitle != null && !subtitle.isBlank()) {
            card.child(mutedLabel(subtitle));
        }
        return card;
    }

    private ParentComponent emptyCard(String title, String text) {
        FlowLayout card = card(title, null);
        card.child(emptyState(title, text));
        return card;
    }

    private ParentComponent textField(String label, String initialValue, Consumer<String> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : initialValue);
        textBox.onChanged().subscribe(value -> {
            onChange.accept(value);
            markDirty("Изменено поле: " + label + ".");
        });
        field.child(textBox);
        return field;
    }

    private ParentComponent itemPickerField(String label, String selectedId, Consumer<String> onPick) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        field.child(pickerSelectionButton(describeItem(selectedId), button -> openItemPicker(onPick)));
        return field;
    }

    private Component enchantmentPickerCompact(String selectedId, Consumer<String> onPick) {
        return compactPickerButton(describeEnchantment(selectedId), button -> openEnchantmentPicker(onPick));
    }

    private Component potionEffectPickerCompact(String selectedId, Consumer<String> onPick) {
        return compactPickerButton(describePotionEffect(selectedId), button -> openPotionEffectPicker(onPick));
    }

    private Component textFieldCompact(String placeholder, String initialValue, Consumer<String> onChange) {
        TextBoxComponent textBox = Components.textBox(Sizing.expand(), initialValue == null ? "" : initialValue);
        textBox.tooltip(uiText(placeholder));
        textBox.onChanged().subscribe(value -> {
            onChange.accept(value);
            markDirty("Изменено поле: " + placeholder + ".");
        });
        return textBox;
    }

    private ParentComponent integerField(String label, Integer initialValue, Consumer<Integer> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), String.valueOf(initialValue == null ? 0 : initialValue));
        textBox.onChanged().subscribe(value -> {
            onChange.accept(parseInteger(value));
            markDirty("Изменено поле: " + label + ".");
        });
        field.child(textBox);
        return field;
    }

    private ParentComponent integerFieldNullable(String label, Integer initialValue, Consumer<Integer> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.onChanged().subscribe(value -> {
            onChange.accept(parseInteger(value));
            markDirty("Изменено поле: " + label + ".");
        });
        field.child(textBox);
        return field;
    }

    private Component integerFieldCompact(String placeholder, Integer initialValue, Consumer<Integer> onChange) {
        TextBoxComponent textBox = Components.textBox(Sizing.fixed(90), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.tooltip(uiText(placeholder));
        textBox.onChanged().subscribe(value -> {
            onChange.accept(parseInteger(value));
            markDirty("Изменено поле: " + placeholder + ".");
        });
        return textBox;
    }

    private ParentComponent longField(String label, Long initialValue, Consumer<Long> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.onChanged().subscribe(value -> {
            onChange.accept(parseLong(value));
            markDirty("Изменено поле: " + label + ".");
        });
        field.child(textBox);
        return field;
    }

    private ParentComponent cycleField(String label, String value, Runnable onCycle) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(secondaryLabel(label));
        field.child(actionButton(value, button -> onCycle.run(), false));
        return field;
    }

    private ButtonComponent actionButton(String label, Consumer<ButtonComponent> onPress, boolean emphasized) {
        ButtonComponent button = Components.button(uiText(label), onPress);
        button.horizontalSizing(Sizing.content(8));
        button.renderer(ButtonComponent.Renderer.flat(
            emphasized ? BUTTON_SUCCESS : BUTTON_SECONDARY,
            emphasized ? BUTTON_SUCCESS_HOVER : BUTTON_SECONDARY_HOVER,
            0xFF10151B
        ));
        return button;
    }

    private ButtonComponent smallAction(String label, Consumer<ButtonComponent> onPress) {
        return smallAction(label, onPress, false);
    }

    private ButtonComponent smallAction(String label, Consumer<ButtonComponent> onPress, boolean emphasized) {
        ButtonComponent button = Components.button(uiText(label), onPress);
        button.horizontalSizing(Sizing.content(6));
        button.renderer(ButtonComponent.Renderer.flat(
            emphasized ? BUTTON_SELECTED : BUTTON_SECONDARY,
            emphasized ? BUTTON_SELECTED_HOVER : BUTTON_SECONDARY_HOVER,
            0xFF10151B
        ));
        return button;
    }

    private ButtonComponent pickerSelectionButton(String label, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = actionButton(label, onPress, false);
        button.horizontalSizing(Sizing.fill());
        return button;
    }

    private ButtonComponent compactPickerButton(String label, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = actionButton(label, onPress, false);
        button.horizontalSizing(Sizing.expand());
        return button;
    }

    private Component horizontalSpacer() {
        return Components.box(Sizing.expand(), Sizing.fixed(0));
    }

    private ParentComponent buildPickerOverlay() {
        FlowLayout modal = Containers.verticalFlow(Sizing.fill(72), Sizing.fill(72));
        modal.surface(Surface.flat(0xF1151C23).and(Surface.outline(SELECTED_OUTLINE)));
        modal.padding(Insets.of(10));
        modal.gap(8);
        modal.zIndex(300);

        FlowLayout titleRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        titleRow.child(primaryLabel(activePicker.title));
        titleRow.child(horizontalSpacer());
        titleRow.child(actionButton("Закрыть", button -> closePicker(), false));
        modal.child(titleRow);

        TextBoxComponent searchBox = Components.textBox(Sizing.fill(), activePicker.query);
        searchBox.tooltip(uiText("Поиск по названию или id"));
        modal.child(searchBox);

        FlowLayout results = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        results.gap(4);
        populatePickerResults(results, activePicker.query);
        searchBox.onChanged().subscribe(value -> {
            activePicker.query = value == null ? "" : value;
            populatePickerResults(results, activePicker.query);
        });

        pickerResultsScroll = styledVerticalScroll(Sizing.fill(), Sizing.expand(), results, pickerResultsScrollProgress);
        modal.child(pickerResultsScroll);

        FlowLayout overlay = Containers.verticalFlow(Sizing.fill(), Sizing.fill());
        overlay.surface(Surface.flat(0xA0000000));
        overlay.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        overlay.positioning(Positioning.absolute(0, 0));
        overlay.zIndex(250);
        overlay.child(modal);
        return overlay;
    }

    private void populatePickerResults(FlowLayout results, String query) {
        results.clearChildren();

        String normalizedQuery = normalizeSearch(query);
        int added = 0;
        for (SearchPickerEntry entry : activePicker.entries) {
            if (!normalizedQuery.isBlank() && !entry.searchText.contains(normalizedQuery)) {
                continue;
            }

            results.child(buildPickerResultRow(entry));
            added++;
            if (added >= PICKER_RESULT_LIMIT) {
                break;
            }
        }

        if (added == 0) {
            results.child(mutedLabel("Ничего не найдено"));
        }
    }

    private Component buildPickerResultRow(SearchPickerEntry entry) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        row.padding(Insets.of(6));
        row.gap(6);

        row.child(buildPickerIcon(entry));

        FlowLayout texts = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        texts.child(Components.label(entry.name.copy().setStyle(entry.name.getStyle().withFont(UI_FONT)))
            .<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_PRIMARY))));
        texts.child(Components.label(uiText(entry.id)).<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_SECONDARY))));
        row.child(texts);

        ButtonComponent selectButton = smallAction("Выбрать", button -> selectPickerEntry(entry));
        selectButton.horizontalSizing(Sizing.content(8));
        row.child(selectButton);
        return row;
    }

    private Component buildPickerIcon(SearchPickerEntry entry) {
        FlowLayout icon = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        icon.gap(3);

        if (entry.itemStack != null) {
            icon.child(Components.item(entry.itemStack).margins(Insets.right(2)));
        }

        if (entry.statusEffect != null) {
            icon.child(Components.sprite(MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(entry.statusEffect))
                .sizing(Sizing.fixed(18), Sizing.fixed(18)));
        }

        if (entry.badgeText != null && !entry.badgeText.isBlank()) {
            FlowLayout badge = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18));
            badge.surface(Surface.flat(entry.badgeColor).and(Surface.outline(0xFF101418)));
            badge.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            badge.child(Components.label(uiText(entry.badgeText)).<LabelComponent>configure(label -> label.color(Color.ofRgb(0xFFFFFFFF))));
            icon.child(badge);
        }

        return icon;
    }

    private void openItemPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = Registries.ITEM.stream()
            .map(this::toItemPickerEntry)
            .sorted(Comparator.comparing(entry -> entry.id))
            .toList();
        activePicker = new SearchPickerState("Выбор предмета Minecraft", entries, onPick);
        rebuildUi();
    }

    private void openPotionEffectPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = Registries.STATUS_EFFECT.streamEntries()
            .map(this::toPotionEffectPickerEntry)
            .sorted(Comparator.comparing(entry -> entry.id))
            .toList();
        activePicker = new SearchPickerState("Выбор эффекта", entries, onPick);
        rebuildUi();
    }

    private void openEnchantmentPicker(Consumer<String> onPick) {
        List<SearchPickerEntry> entries = enchantmentRegistry().streamEntries()
            .map(this::toEnchantmentPickerEntry)
            .sorted(Comparator.comparing(SearchPickerEntry::id))
            .toList();
        activePicker = new SearchPickerState("Выбор зачарования", entries, onPick);
        rebuildUi();
    }

    private SearchPickerEntry toItemPickerEntry(Item item) {
        ItemStack stack = new ItemStack(item);
        String id = Registries.ITEM.getId(item).toString();
        Text name = stack.getName();
        return new SearchPickerEntry(id, name, normalizeSearch(name.getString() + " " + id), stack, null, null, 0);
    }

    private SearchPickerEntry toPotionEffectPickerEntry(RegistryEntry<StatusEffect> entry) {
        StatusEffect effect = entry.value();
        String id = Registries.STATUS_EFFECT.getId(effect).toString();
        Text name = effect.getName();
        return new SearchPickerEntry(id, name, normalizeSearch(name.getString() + " " + id), null, entry, null, 0);
    }

    private SearchPickerEntry toEnchantmentPickerEntry(RegistryEntry<Enchantment> entry) {
        Enchantment enchantment = entry.value();
        String id = enchantmentRegistry().getId(enchantment).toString();
        Text name = enchantment.description();
        return new SearchPickerEntry(
            id,
            name,
            normalizeSearch(name.getString() + " " + id),
            enchantmentBaseStack(id),
            null,
            enchantmentBadge(id, name.getString()),
            enchantmentBadgeColor(id)
        );
    }

    private void selectPickerEntry(SearchPickerEntry entry) {
        SearchPickerState picker = activePicker;
        if (picker == null) {
            return;
        }

        picker.onPick.accept(entry.id);
        activePicker = null;
        markDirty("Выбрано значение: " + entry.id + ".");
        rebuildUi();
    }

    private void closePicker() {
        activePicker = null;
        rebuildUi();
    }

    private String describeItem(String id) {
        if (isBlank(id)) {
            return "Выберите предмет...";
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return id;
        }

        Item item = Registries.ITEM.get(identifier);
        return new ItemStack(item).getName().getString() + " | " + id;
    }

    private String describePotionEffect(String id) {
        if (isBlank(id)) {
            return "Выберите эффект...";
        }

        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.STATUS_EFFECT.containsId(identifier)) {
            return id;
        }

        return Registries.STATUS_EFFECT.get(identifier).getName().getString() + " | " + id;
    }

    private String describeEnchantment(String id) {
        if (isBlank(id)) {
            return "Выберите зачарование...";
        }

        Identifier identifier = Identifier.tryParse(id);
        Registry<Enchantment> registry = enchantmentRegistry();
        if (identifier == null || !registry.containsId(identifier)) {
            return id;
        }

        return registry.get(identifier).description().getString() + " | " + id;
    }

    private ItemStack enchantmentBaseStack(String enchantmentId) {
        String path = safePath(enchantmentId);
        ItemStack stack = new ItemStack(switch (path) {
            case "power", "punch", "flame", "infinity" -> Items.BOW;
            case "multishot", "quick_charge", "piercing" -> Items.CROSSBOW;
            case "loyalty", "riptide", "channeling", "impaling" -> Items.TRIDENT;
            case "luck_of_the_sea", "lure" -> Items.FISHING_ROD;
            case "protection", "blast_protection", "fire_protection", "projectile_protection", "thorns", "unbreaking", "mending" -> Items.DIAMOND_CHESTPLATE;
            case "feather_falling", "depth_strider", "frost_walker", "soul_speed" -> Items.DIAMOND_BOOTS;
            case "respiration", "aqua_affinity" -> Items.DIAMOND_HELMET;
            case "swift_sneak" -> Items.DIAMOND_LEGGINGS;
            case "efficiency", "fortune", "silk_touch" -> Items.DIAMOND_PICKAXE;
            case "sharpness", "smite", "bane_of_arthropods", "fire_aspect", "looting", "knockback", "sweeping_edge" -> Items.DIAMOND_SWORD;
            default -> Items.ENCHANTED_BOOK;
        });

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private String enchantmentBadge(String enchantmentId, String displayName) {
        String path = safePath(enchantmentId);
        String[] tokens = path.split("_");
        if (tokens.length >= 2) {
            return (tokens[0].substring(0, 1) + tokens[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        String normalized = displayName.replaceAll("[^\\p{L}\\p{Nd}]+", "");
        if (normalized.length() >= 2) {
            return normalized.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return "EN";
    }

    private int enchantmentBadgeColor(String enchantmentId) {
        int hash = Math.abs(enchantmentId.hashCode());
        int red = 80 + hash % 120;
        int green = 80 + (hash / 7) % 120;
        int blue = 80 + (hash / 17) % 120;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static String safePath(String identifier) {
        Identifier parsed = Identifier.tryParse(identifier);
        return parsed == null ? identifier : parsed.getPath();
    }

    private Registry<Enchantment> enchantmentRegistry() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            throw new IllegalStateException("Client world is not available for enchantment registry access");
        }
        return client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String buildRuntimeStatus() {
        return "Правил: " + draft.buyRules.size()
            + " | Автобай: " + (autobuyLoopController.isEnabled() ? "работает" : "остановлен")
            + " | Смена страниц: " + draft.pageSwitchDelayMs + " мс"
            + " | Логи: " + localizeLogMode(draft.scanLogMode)
            + " | " + (validationErrors.isEmpty() ? "готово к сохранению" : ("ошибок: " + validationErrors.size()));
    }

    private ParentComponent buildInfoCard() {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(CARD_BACKGROUND).and(Surface.outline(ACCENT_OUTLINE)));
        card.padding(Insets.of(8));
        card.gap(6);

        String selectedRuleName = selectedRuleIndex >= 0 && selectedRuleIndex < draft.buyRules.size()
            ? displayRuleTitle(draft.buyRules.get(selectedRuleIndex), selectedRuleIndex)
            : "Правило не выбрано";
        String selectedRuleId = selectedRuleIndex >= 0 && selectedRuleIndex < draft.buyRules.size()
            ? draft.buyRules.get(selectedRuleIndex).id
            : "нет";

        card.child(primaryLabel(selectedRuleName));
        card.child(detailRow("ID выбранного правила", isBlank(selectedRuleId) ? "нет" : selectedRuleId));
        card.child(detailRow("Всего правил", String.valueOf(draft.buyRules.size())));
        card.child(detailRow("Интервал сканирования", draft.scanIntervalSeconds + " сек"));
        card.child(detailRow("Лимит страниц", String.valueOf(draft.scanPageLimit)));
        card.child(detailRow("Код профиля", String.valueOf(Math.abs(draft.toDomain().hashCode()))));
        return card;
    }

    private ParentComponent buildInfoActions() {
        FlowLayout actions = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        actions.surface(Surface.flat(CARD_BACKGROUND).and(Surface.outline(ACCENT_OUTLINE)));
        actions.padding(Insets.of(8));
        actions.gap(6);
        actions.child(primaryLabel("Действия"));
        actions.child(actionButton("Сохранить в файл", button -> saveDraft(), true));
        actions.child(actionButton("Перезагрузить файл", button -> reloadFromFile(), false));
        actions.child(actionButton("Дублировать правило", button -> duplicateSelectedRule(), false));
        actions.child(actionButton("Удалить правило", button -> deleteSelectedRule(), false));
        return actions;
    }

    private ParentComponent buildValidationPreview() {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(CARD_BACKGROUND).and(Surface.outline(ACCENT_OUTLINE)));
        card.padding(Insets.of(8));
        card.gap(6);
        card.child(primaryLabel("Предупреждения"));
        int previewCount = Math.min(validationErrors.size(), 4);
        for (int i = 0; i < previewCount; i++) {
            card.child(infoStripe(validationErrors.get(i), TEXT_DANGER));
        }
        if (validationErrors.size() > previewCount) {
            card.child(mutedLabel("Ещё: " + (validationErrors.size() - previewCount)));
        }
        return card;
    }

    private ParentComponent buildRuleListEntry(int ruleIndex, AutobuyConfigDraft.BuyRuleDraft rule) {
        boolean selected = ruleIndex == selectedRuleIndex;
        FlowLayout card = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(selected ? SELECTED_CARD : CARD_BACKGROUND).and(Surface.outline(selected ? SELECTED_OUTLINE : PANEL_OUTLINE)));
        card.padding(Insets.of(7));
        card.gap(7);
        card.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

        card.child(buildRuleAvatar(ruleIndex, rule));
        card.child(Components.label(uiText(displayRuleTitle(rule, ruleIndex))).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(TEXT_PRIMARY));
            label.maxWidth(1000);
        }));
        card.child(horizontalSpacer());
        card.child(smallAction(rule.enabled ? "Вкл" : "Выкл", button -> {
            rule.enabled = !rule.enabled;
            markDirty(rule.enabled ? "Правило включено." : "Правило выключено.");
            rebuildUi();
        }, rule.enabled));
        card.child(smallAction("Открыть", button -> {
            selectedRuleIndex = ruleIndex;
            editorScrollProgress = 0;
            confirmClosePending = false;
            rebuildUi();
        }, selected));
        return card;
    }

    private ParentComponent buildRuleAvatar(int ruleIndex, AutobuyConfigDraft.BuyRuleDraft rule) {
        FlowLayout avatar = Containers.verticalFlow(Sizing.fixed(34), Sizing.fixed(34));
        avatar.surface(Surface.flat(ruleAccentColor(ruleIndex, rule)).and(Surface.outline(0xFF0F141A)));
        avatar.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        avatar.gap(2);

        ItemStack previewStack = previewStackForRule(rule);
        if (previewStack != null) {
            avatar.child(Components.item(previewStack));
        } else {
            avatar.child(Components.label(uiText(ruleInitials(rule))).<LabelComponent>configure(label -> label.color(Color.ofRgb(0xFFFFFFFF))));
        }

        return avatar;
    }

    private ItemStack previewStackForRule(AutobuyConfigDraft.BuyRuleDraft rule) {
        for (AutobuyConfigDraft.ConditionDraft condition : rule.conditions) {
            if (condition.type != AutobuyConfigDraft.ConditionType.MINECRAFT_ID || isBlank(condition.stringValue)) {
                continue;
            }
            Identifier identifier = Identifier.tryParse(condition.stringValue);
            if (identifier != null && Registries.ITEM.containsId(identifier)) {
                return new ItemStack(Registries.ITEM.get(identifier));
            }
        }
        return null;
    }

    private String ruleInitials(AutobuyConfigDraft.BuyRuleDraft rule) {
        String source = !isBlank(rule.name) ? rule.name : (!isBlank(rule.id) ? rule.id : "rule");
        String normalized = source.replaceAll("[^\\p{L}\\p{Nd}]+", " ").trim();
        if (normalized.isBlank()) {
            return "R";
        }
        String[] parts = normalized.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        return normalized.substring(0, Math.min(2, normalized.length())).toUpperCase(Locale.ROOT);
    }

    private int ruleAccentColor(int ruleIndex, AutobuyConfigDraft.BuyRuleDraft rule) {
        String base = displayRuleTitle(rule, ruleIndex);
        int hash = Math.abs(base.hashCode());
        int red = 60 + hash % 70;
        int green = 90 + (hash / 7) % 80;
        int blue = 110 + (hash / 13) % 90;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private ParentComponent statPill(String label, String value) {
        FlowLayout pill = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        pill.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        pill.padding(Insets.of(4, 8, 4, 8));
        pill.gap(6);
        pill.child(Components.label(uiText(label)).<LabelComponent>configure(component -> component.color(Color.ofRgb(TEXT_MUTED))));
        pill.child(Components.label(uiText(value)).<LabelComponent>configure(component -> component.color(Color.ofRgb(TEXT_PRIMARY))));
        return pill;
    }

    private ParentComponent sectionTag(String text) {
        FlowLayout tag = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        tag.surface(Surface.flat(0xCC101722).and(Surface.outline(ACCENT_OUTLINE)));
        tag.padding(Insets.of(3, 7, 3, 7));
        tag.child(Components.label(uiText(text)).<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_SECONDARY))));
        return tag;
    }

    private ParentComponent infoStripe(String text, int color) {
        FlowLayout stripe = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        stripe.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        stripe.padding(Insets.of(6, 8, 6, 8));
        stripe.child(Components.label(uiText(text)).<LabelComponent>configure(label -> label.color(Color.ofRgb(color))));
        return stripe;
    }

    private ParentComponent emptyState(String title, String text) {
        FlowLayout state = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        state.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        state.padding(Insets.of(10));
        state.gap(4);
        state.child(Components.label(uiText(title)).<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_PRIMARY))));
        state.child(mutedLabel(text));
        return state;
    }

    private ParentComponent detailRow(String label, String value) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.child(mutedLabel(label));
        row.child(horizontalSpacer());
        row.child(Components.label(uiText(value)).<LabelComponent>configure(component -> component.color(Color.ofRgb(TEXT_PRIMARY))));
        return row;
    }

    private ParentComponent halfWidth(ParentComponent child) {
        FlowLayout wrapper = Containers.verticalFlow(Sizing.fill(50), Sizing.content());
        wrapper.child(child);
        return wrapper;
    }

    private LabelComponent primaryLabel(String text) {
        LabelComponent label = Components.label(uiText(text));
        label.color(Color.ofRgb(TEXT_PRIMARY));
        label.shadow(false);
        return label;
    }

    private LabelComponent secondaryLabel(String text) {
        LabelComponent label = Components.label(uiText(text));
        label.color(Color.ofRgb(TEXT_SECONDARY));
        label.shadow(false);
        return label;
    }

    private LabelComponent mutedLabel(String text) {
        LabelComponent label = Components.label(uiText(text));
        label.color(Color.ofRgb(TEXT_MUTED));
        label.shadow(false);
        return label;
    }

    private UiScrollContainer<FlowLayout> styledVerticalScroll(Sizing horizontalSizing, Sizing verticalSizing, FlowLayout child, double progress) {
        UiScrollContainer<FlowLayout> scroll = new UiScrollContainer<>(horizontalSizing, verticalSizing, child);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(SCROLLBAR_COLOR)));
        scroll.scrollbarThiccness(4);
        scroll.restoreProgress(progress);
        return scroll;
    }

    private void captureScrollState() {
        if (ruleListScroll != null) {
            ruleListScrollProgress = ruleListScroll.progress();
        }
        if (editorScroll != null) {
            editorScrollProgress = editorScroll.progress();
        }
        if (pickerResultsScroll != null) {
            pickerResultsScrollProgress = pickerResultsScroll.progress();
        }
    }

    private void createRule() {
        AutobuyConfigDraft.BuyRuleDraft rule = new AutobuyConfigDraft.BuyRuleDraft();
        int nextIndex = draft.buyRules.size() + 1;
        rule.id = "rule_" + nextIndex;
        rule.name = "Правило " + nextIndex;
        draft.buyRules.add(rule);
        selectedRuleIndex = draft.buyRules.size() - 1;
        markDirty("Создано правило " + rule.id + ".");
        rebuildUi();
    }

    private void moveSelectedRule(int delta) {
        int targetIndex = selectedRuleIndex + delta;
        if (selectedRuleIndex < 0 || selectedRuleIndex >= draft.buyRules.size() || targetIndex < 0 || targetIndex >= draft.buyRules.size()) {
            return;
        }

        AutobuyConfigDraft.BuyRuleDraft rule = draft.buyRules.remove(selectedRuleIndex);
        draft.buyRules.add(targetIndex, rule);
        selectedRuleIndex = targetIndex;
        markDirty("Порядок правил изменён.");
        rebuildUi();
    }

    private void duplicateSelectedRule() {
        if (selectedRuleIndex < 0 || selectedRuleIndex >= draft.buyRules.size()) {
            return;
        }

        AutobuyConfigDraft.BuyRuleDraft copy = AutobuyConfigDraft.BuyRuleDraft.fromDomain(draft.buyRules.get(selectedRuleIndex).toDomain());
        copy.id = isBlank(copy.id) ? "rule_copy" : copy.id + "_copy";
        copy.name = isBlank(copy.name) ? "Копия правила" : copy.name + " Копия";
        draft.buyRules.add(selectedRuleIndex + 1, copy);
        selectedRuleIndex++;
        markDirty("Правило дублировано.");
        rebuildUi();
    }

    private void deleteSelectedRule() {
        if (selectedRuleIndex < 0 || selectedRuleIndex >= draft.buyRules.size()) {
            return;
        }

        draft.buyRules.remove(selectedRuleIndex);
        if (draft.buyRules.isEmpty()) {
            selectedRuleIndex = -1;
        } else if (selectedRuleIndex >= draft.buyRules.size()) {
            selectedRuleIndex = draft.buyRules.size() - 1;
        }
        markDirty("Правило удалено.");
        rebuildUi();
    }

    private void moveCondition(AutobuyConfigDraft.BuyRuleDraft rule, int index, int delta) {
        int targetIndex = index + delta;
        if (targetIndex < 0 || targetIndex >= rule.conditions.size()) {
            return;
        }

        AutobuyConfigDraft.ConditionDraft condition = rule.conditions.remove(index);
        rule.conditions.add(targetIndex, condition);
        markDirty("Порядок условий изменён.");
        rebuildUi();
    }

    private void deleteCondition(AutobuyConfigDraft.BuyRuleDraft rule, int index) {
        if (index < 0 || index >= rule.conditions.size()) {
            return;
        }

        rule.conditions.remove(index);
        markDirty("Условие удалено.");
        rebuildUi();
    }

    private void saveDraft() {
        validationErrors = validateDraft();
        if (!validationErrors.isEmpty()) {
            statusMessage = "Сначала исправьте ошибки валидации.";
            updateSaveButtonState();
            rebuildUi();
            return;
        }

        try {
            configManager.saveAndReload(draft.toDomain());
            resetDraft(configManager.getCurrentConfig(), true, "Конфигурация сохранена.");
        } catch (RuntimeException exception) {
            statusMessage = "Ошибка сохранения: " + exception.getMessage();
            rebuildUi();
        }
    }

    private void saveDraftAndClose() {
        saveDraft();
        if (!dirty) {
            close();
        }
    }

    private void reloadFromFile() {
        try {
            AutobuyConfig config = configManager.reload();
            resetDraft(config, true, "Конфигурация заново загружена из файла.");
        } catch (RuntimeException exception) {
            statusMessage = "Ошибка загрузки: " + exception.getMessage();
            rebuildUi();
        }
    }

    private void discardDraft() {
        resetDraft(configManager.getCurrentConfig(), true, "Несохранённые изменения отменены.");
    }

    private void discardAndClose() {
        dirty = false;
        confirmClosePending = false;
        close();
    }

    private void toggleAutobuy() {
        if (autobuyLoopController.isEnabled()) {
            autobuyLoopController.stop();
            statusMessage = "Цикл автобая остановлен.";
        } else {
            autobuyLoopController.start();
            statusMessage = "Цикл автобая запущен.";
        }
        rebuildUi();
    }

    private void resetDraft(AutobuyConfig config, boolean preserveStatus, String message) {
        this.draft = AutobuyConfigDraft.fromDomain(config);
        if (draft.buyRules.isEmpty()) {
            selectedRuleIndex = -1;
        } else if (selectedRuleIndex < 0 || selectedRuleIndex >= draft.buyRules.size()) {
            selectedRuleIndex = 0;
        }
        dirty = false;
        confirmClosePending = false;
        validationErrors = validateDraft();
        statusMessage = preserveStatus ? message : message;
        if (uiAdapter != null) {
            rebuildUi();
        }
    }

    private void markDirty(String message) {
        dirty = true;
        statusMessage = message;
        validationErrors = validateDraft();
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        if (saveButton != null) {
            saveButton.active(validationErrors.isEmpty());
        }
    }

    private List<String> validateDraft() {
        List<String> errors = new ArrayList<>();
        if (draft.scanIntervalSeconds <= 0) {
            errors.add("Интервал сканирования должен быть больше 0.");
        }
        if (draft.scanPageLimit <= 0) {
            errors.add("Лимит страниц должен быть больше 0.");
        }
        if (draft.pageSwitchDelayMs <= 0) {
            errors.add("Задержка смены страниц должна быть больше 0.");
        }

        for (int ruleIndex = 0; ruleIndex < draft.buyRules.size(); ruleIndex++) {
            AutobuyConfigDraft.BuyRuleDraft rule = draft.buyRules.get(ruleIndex);
            String label = "Правило " + (ruleIndex + 1);
            if (isBlank(rule.id)) {
                errors.add(label + ": требуется id.");
            }
            if (isBlank(rule.name)) {
                errors.add(label + ": требуется название.");
            }

            for (int conditionIndex = 0; conditionIndex < rule.conditions.size(); conditionIndex++) {
                AutobuyConfigDraft.ConditionDraft condition = rule.conditions.get(conditionIndex);
                String conditionLabel = label + ", условие " + (conditionIndex + 1) + " (" + localizeConditionType(condition.type) + ")";
                switch (condition.type) {
                    case MINECRAFT_ID, DISPLAY_NAME -> {
                        if (isBlank(condition.stringValue)) {
                            errors.add(conditionLabel + ": требуется непустое значение.");
                        }
                    }
                    case MAX_TOTAL_PRICE, MAX_UNIT_PRICE -> {
                        if (condition.longValue == null || condition.longValue < 0) {
                            errors.add(conditionLabel + ": требуется неотрицательное число.");
                        }
                    }
                    case MIN_COUNT, MAX_COUNT -> {
                        if (condition.intValue == null || condition.intValue < 0) {
                            errors.add(conditionLabel + ": требуется неотрицательное число.");
                        }
                    }
                    case REQUIRED_ENCHANTMENTS -> {
                        for (AutobuyConfigDraft.RequiredEnchantmentDraft enchantment : condition.requiredEnchantments) {
                            if (isBlank(enchantment.id)) {
                                errors.add(conditionLabel + ": найдено зачарование без id.");
                            }
                            if (enchantment.level != null && enchantment.level < 0) {
                                errors.add(conditionLabel + ": уровень зачарования не может быть отрицательным.");
                            }
                        }
                    }
                    case REQUIRED_POTION_EFFECTS -> {
                        for (AutobuyConfigDraft.RequiredPotionEffectDraft effect : condition.requiredPotionEffects) {
                            if (isBlank(effect.id)) {
                                errors.add(conditionLabel + ": найден эффект без id.");
                            }
                            if (effect.level != null && effect.level < 0) {
                                errors.add(conditionLabel + ": уровень эффекта не может быть отрицательным.");
                            }
                            if (effect.durationSeconds != null && effect.durationSeconds < 0) {
                                errors.add(conditionLabel + ": длительность не может быть отрицательной.");
                            }
                        }
                    }
                    case SELLER_ALLOW_LIST, SELLER_DENY_LIST -> {
                        for (String entry : condition.stringList) {
                            if (isBlank(entry)) {
                                errors.add(conditionLabel + ": найден пустой продавец.");
                            }
                        }
                    }
                }
            }
        }
        return errors;
    }

    private static String displayRuleTitle(AutobuyConfigDraft.BuyRuleDraft rule, int index) {
        if (rule.name != null && !rule.name.isBlank()) {
            return rule.name;
        }
        if (rule.id != null && !rule.id.isBlank()) {
            return rule.id;
        }
        return "Правило " + (index + 1);
    }

    private static String shortTypeName(AutobuyConfigDraft.ConditionType type) {
        return switch (type) {
            case MINECRAFT_ID -> "Предмет";
            case DISPLAY_NAME -> "Имя";
            case MAX_TOTAL_PRICE -> "Общая";
            case MAX_UNIT_PRICE -> "За шт.";
            case MIN_COUNT -> "Мин";
            case MAX_COUNT -> "Макс";
            case REQUIRED_ENCHANTMENTS -> "Чары";
            case REQUIRED_POTION_EFFECTS -> "Эффекты";
            case SELLER_ALLOW_LIST -> "Продавцы+";
            case SELLER_DENY_LIST -> "Продавцы-";
        };
    }

    private static String localizeConditionType(AutobuyConfigDraft.ConditionType type) {
        return switch (type) {
            case MINECRAFT_ID -> "предмет";
            case DISPLAY_NAME -> "название";
            case MAX_TOTAL_PRICE -> "макс. общая цена";
            case MAX_UNIT_PRICE -> "макс. цена за штуку";
            case MIN_COUNT -> "мин. количество";
            case MAX_COUNT -> "макс. количество";
            case REQUIRED_ENCHANTMENTS -> "зачарования";
            case REQUIRED_POTION_EFFECTS -> "эффекты";
            case SELLER_ALLOW_LIST -> "белый список продавцов";
            case SELLER_DENY_LIST -> "чёрный список продавцов";
        };
    }

    private static String localizeLogMode(AutobuyScanLogMode mode) {
        return switch (mode) {
            case ALL -> "Все";
            case MATCHED_ONLY -> "Только совпадения";
        };
    }

    private static Integer parseInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long parseLong(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int clampPositive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Text uiText(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(UI_FONT));
    }

    private static final class UiScrollContainer<C extends Component> extends ScrollContainer<C> {
        private Double pendingProgress = null;

        private UiScrollContainer(Sizing horizontalSizing, Sizing verticalSizing, C child) {
            super(ScrollDirection.VERTICAL, horizontalSizing, verticalSizing, child);
        }

        private double progress() {
            return this.maxScroll <= 0 ? 0 : Math.max(0, Math.min(1, this.scrollOffset / this.maxScroll));
        }

        private void restoreProgress(double progress) {
            this.pendingProgress = progress;
        }

        @Override
        public void layout(Size space) {
            super.layout(space);
            if (this.pendingProgress != null) {
                this.scrollTo(this.pendingProgress);
                this.currentScrollPosition = this.scrollOffset;
                this.pendingProgress = null;
            }
        }
    }

    private static final class SearchPickerState {
        private final String title;
        private final List<SearchPickerEntry> entries;
        private final Consumer<String> onPick;
        private String query = "";

        private SearchPickerState(String title, List<SearchPickerEntry> entries, Consumer<String> onPick) {
            this.title = title;
            this.entries = entries;
            this.onPick = onPick;
        }
    }

    private record SearchPickerEntry(
        String id,
        Text name,
        String searchText,
        ItemStack itemStack,
        RegistryEntry<StatusEffect> statusEffect,
        String badgeText,
        int badgeColor
    ) {
    }
}
