package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;

import java.util.Locale;

final class AutobuyRuleEditorView {
    ParentComponent build(AutobuyScreenViewHost host) {
        AutobuyConfigSession session = host.session();

        FlowLayout editorContent = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        editorContent.gap(8);
        editorContent.child(buildGlobalSettingsCard(host));
        if (session.selectedRuleIndex() >= 0 && session.selectedRuleIndex() < session.draft().buyRules.size()) {
            editorContent.child(buildSelectedRuleCard(host, session.draft().buyRules.get(session.selectedRuleIndex())));
        } else {
            editorContent.child(AutobuyUiComponents.emptyCard("Рабочая область", "Выберите правило слева или создайте новое."));
        }
        editorContent.child(Components.box(Sizing.fill(), Sizing.fixed(28)));

        FlowLayout panel = Containers.verticalFlow(Sizing.expand(), Sizing.fill());
        panel.surface(Surface.flat(AutobuyUiComponents.PANEL_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        panel.padding(Insets.of(8));
        panel.gap(6);
        panel.child(AutobuyUiComponents.primaryLabel("Редактор"));
        UiScrollContainer<FlowLayout> scroll = AutobuyUiComponents.styledVerticalScroll(
            Sizing.fill(),
            Sizing.fill(),
            editorContent,
            session.editorScrollProgress()
        );
        host.setEditorScroll(scroll);
        panel.child(scroll);
        return panel;
    }

    private ParentComponent buildGlobalSettingsCard(AutobuyScreenViewHost host) {
        AutobuyConfigDraft draft = host.session().draft();
        FlowLayout card = AutobuyUiComponents.card("Параметры конфигурации", "Общие настройки для всего набора правил.");
        card.gap(6);
        card.child(integerField("Интервал сканирования, сек", draft.scanIntervalSeconds, host.presenter()::updateScanInterval));
        card.child(integerField("Разброс интервала, сек", draft.scanIntervalJitterSeconds, host.presenter()::updateScanIntervalJitter));
        card.child(integerField("Лимит страниц", draft.scanPageLimit, host.presenter()::updateScanPageLimit));
        card.child(integerField("Задержка смены страниц, мс", draft.pageSwitchDelayMs, host.presenter()::updatePageSwitchDelay));
        card.child(integerField("Разброс задержки страниц, мс", draft.pageSwitchDelayJitterMs, host.presenter()::updatePageSwitchDelayJitter));
        card.child(checkboxField("Анти-AFK включён", draft.antiAfkEnabled, host.presenter()::updateAntiAfkEnabled));
        card.child(integerField("Интервал анти-AFK, сек", draft.antiAfkActionIntervalSeconds, host.presenter()::updateAntiAfkActionInterval));
        card.child(integerField("Шанс прыжка анти-AFK, %", draft.antiAfkJumpChancePercent, host.presenter()::updateAntiAfkJumpChance));
        card.child(integerField("Маржа market research, %", draft.marketResearchTargetMarginPercent, host.presenter()::updateMarketResearchTargetMargin));
        card.child(integerField("Буфер риска market research, %", draft.marketResearchRiskBufferPercent, host.presenter()::updateMarketResearchRiskBuffer));
        card.child(cycleField("Режим логов", AutobuyUiTextSupport.localizeLogMode(draft.scanLogMode), host.presenter()::cycleLogMode));
        return card;
    }

    private ParentComponent buildSelectedRuleCard(AutobuyScreenViewHost host, AutobuyConfigDraft.BuyRuleDraft rule) {
        FlowLayout card = AutobuyUiComponents.card("Редактор правила", "Центральная область настройки выбранного правила.");
        card.gap(6);
        card.child(textField("Идентификатор", rule.id, value -> host.presenter().updateRuleId(rule, value)));
        card.child(textField("Название", rule.name, value -> host.presenter().updateRuleName(rule, value)));

        CheckboxComponent enabledCheckbox = Components.checkbox(AutobuyUiTextSupport.uiText("Включено"));
        enabledCheckbox.checked(rule.enabled);
        enabledCheckbox.onChanged(value -> host.presenter().updateRuleEnabled(rule, value));
        card.child(enabledCheckbox);
        card.child(AutobuyUiComponents.actionButton("Исследовать рынок", button -> host.presenter().startMarketResearch(), true));

        card.child(AutobuyUiComponents.primaryLabel("Условия"));
        if (rule.conditions.isEmpty()) {
            card.child(AutobuyUiComponents.emptyState("Нет условий", "Добавьте фильтры по предмету, цене, чарам, эффектам или продавцу."));
        } else {
            for (int i = 0; i < rule.conditions.size(); i++) {
                card.child(buildConditionCard(host, rule, i, rule.conditions.get(i)));
            }
        }

        card.child(buildConditionPalette(host, rule));
        return card;
    }

    private ParentComponent buildConditionCard(AutobuyScreenViewHost host, AutobuyConfigDraft.BuyRuleDraft rule, int index, AutobuyConfigDraft.ConditionDraft condition) {
        FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        card.padding(Insets.of(7));
        card.gap(6);

        FlowLayout header = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        header.child(AutobuyUiComponents.sectionTag(AutobuyUiTextSupport.localizeConditionType(condition.type).toUpperCase(Locale.ROOT)));
        header.child(AutobuyUiComponents.horizontalSpacer());
        header.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_MOVE_UP, "Поднять условие выше", button -> host.presenter().moveCondition(rule, index, -1)));
        header.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_MOVE_DOWN, "Опустить условие ниже", button -> host.presenter().moveCondition(rule, index, 1)));
        header.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_DELETE, "Удалить условие", button -> host.presenter().deleteCondition(rule, index)));
        card.child(header);

        switch (condition.type) {
            case MINECRAFT_ID -> card.child(itemPickerField(host, "Предмет Minecraft", condition.stringValue, () -> host.presenter().openItemPicker(condition)));
            case MAX_TOTAL_PRICE -> card.child(longField("Макс. общая цена", condition.longValue, value -> host.presenter().updateConditionLong(condition, "Макс. общая цена", value)));
            case MAX_UNIT_PRICE -> card.child(longField("Макс. цена за штуку", condition.longValue, value -> host.presenter().updateConditionLong(condition, "Макс. цена за штуку", value)));
            case MIN_COUNT -> card.child(integerFieldNullable("Мин. количество", condition.intValue, value -> host.presenter().updateConditionInteger(condition, "Мин. количество", value)));
            case MAX_COUNT -> card.child(integerFieldNullable("Макс. количество", condition.intValue, value -> host.presenter().updateConditionInteger(condition, "Макс. количество", value)));
            case REQUIRED_ENCHANTMENTS -> buildRequiredEnchantmentsEditor(host, card, condition);
            case REQUIRED_POTION_EFFECTS -> buildRequiredPotionEffectsEditor(host, card, condition);
            case SELLER_ALLOW_LIST, SELLER_DENY_LIST -> buildStringListEditor(host, card, condition);
        }

        return card;
    }

    private ParentComponent buildConditionPalette(AutobuyScreenViewHost host, AutobuyConfigDraft.BuyRuleDraft rule) {
        FlowLayout palette = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        palette.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        palette.padding(Insets.of(7));
        palette.gap(6);
        palette.child(AutobuyUiComponents.primaryLabel("Добавить условие"));
        palette.child(AutobuyUiComponents.mutedLabel("Соберите правило из фильтров цены, предмета, эффектов и продавцов"));

        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(4);
        int index = 0;
        for (AutobuyConfigDraft.ConditionType type : AutobuyConfigDraft.ConditionType.values()) {
            if (index == 4) {
                palette.child(row);
                row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
            }
            row.child(AutobuyUiComponents.smallAction(AutobuyUiTextSupport.shortTypeName(type), button -> host.presenter().addCondition(rule, type), true));
            index++;
        }
        palette.child(row);
        return palette;
    }

    private void buildRequiredEnchantmentsEditor(AutobuyScreenViewHost host, FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.requiredEnchantments.isEmpty()) {
            parent.child(AutobuyUiComponents.mutedLabel("Нет обязательных зачарований"));
        } else {
            for (AutobuyConfigDraft.RequiredEnchantmentDraft enchantment : condition.requiredEnchantments) {
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(compactEnchantmentPicker(host, enchantment.id, () -> host.presenter().openEnchantmentPicker(enchantment)));
                row.child(integerFieldCompact("уровень", enchantment.level, value -> host.presenter().updateEnchantmentLevel(enchantment, value)));
                row.child(AutobuyUiComponents.smallAction("Убрать", button -> host.presenter().removeEnchantment(condition, enchantment)));
                parent.child(row);
            }
        }
        parent.child(AutobuyUiComponents.smallAction("Добавить чар", button -> host.presenter().addEnchantment(condition), true));
    }

    private void buildRequiredPotionEffectsEditor(AutobuyScreenViewHost host, FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.requiredPotionEffects.isEmpty()) {
            parent.child(AutobuyUiComponents.mutedLabel("Нет обязательных эффектов"));
        } else {
            for (AutobuyConfigDraft.RequiredPotionEffectDraft effect : condition.requiredPotionEffects) {
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(compactPotionEffectPicker(host, effect.id, () -> host.presenter().openPotionEffectPicker(effect)));
                row.child(integerFieldCompact("уровень", effect.level, value -> host.presenter().updatePotionEffectLevel(effect, value)));
                row.child(integerFieldCompact("длительность", effect.durationSeconds, value -> host.presenter().updatePotionEffectDuration(effect, value)));
                row.child(AutobuyUiComponents.smallAction("Убрать", button -> host.presenter().removePotionEffect(condition, effect)));
                parent.child(row);
            }
        }
        parent.child(AutobuyUiComponents.smallAction("Добавить эффект", button -> host.presenter().addPotionEffect(condition), true));
    }

    private void buildStringListEditor(AutobuyScreenViewHost host, FlowLayout parent, AutobuyConfigDraft.ConditionDraft condition) {
        if (condition.stringList.isEmpty()) {
            parent.child(AutobuyUiComponents.mutedLabel("Нет записей"));
        } else {
            for (int i = 0; i < condition.stringList.size(); i++) {
                int index = i;
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
                row.gap(4);
                row.child(textFieldCompact("значение", condition.stringList.get(i), value -> host.presenter().updateStringListEntry(condition, index, value)));
                row.child(AutobuyUiComponents.smallAction("Убрать", button -> host.presenter().removeStringListEntry(condition, index)));
                parent.child(row);
            }
        }
        parent.child(AutobuyUiComponents.smallAction("Добавить запись", button -> host.presenter().addStringListEntry(condition), true));
    }

    private ParentComponent textField(String label, String initialValue, java.util.function.Consumer<String> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : initialValue);
        textBox.onChanged().subscribe(onChange::accept);
        field.child(textBox);
        return field;
    }

    private ParentComponent itemPickerField(AutobuyScreenViewHost host, String label, String selectedId, Runnable onPick) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        field.child(selectionPickerButton(
            host.pickerCatalog().resolveItemSelection(selectedId),
            host.pickerCatalog().describeItem(selectedId),
            "Выберите предмет...",
            onPick,
            false
        ));
        return field;
    }

    private Component compactEnchantmentPicker(AutobuyScreenViewHost host, String selectedId, Runnable onPick) {
        return selectionPickerButton(
            host.pickerCatalog().resolveEnchantmentSelection(selectedId),
            host.pickerCatalog().describeEnchantment(selectedId),
            "Выберите зачарование...",
            onPick,
            true
        );
    }

    private Component compactPotionEffectPicker(AutobuyScreenViewHost host, String selectedId, Runnable onPick) {
        return selectionPickerButton(
            host.pickerCatalog().resolvePotionEffectSelection(selectedId),
            host.pickerCatalog().describePotionEffect(selectedId),
            "Выберите эффект...",
            onPick,
            true
        );
    }

    private Component textFieldCompact(String placeholder, String initialValue, java.util.function.Consumer<String> onChange) {
        TextBoxComponent textBox = Components.textBox(Sizing.expand(), initialValue == null ? "" : initialValue);
        textBox.tooltip(AutobuyUiTextSupport.uiText(placeholder));
        textBox.onChanged().subscribe(onChange::accept);
        return textBox;
    }

    private ParentComponent integerField(String label, Integer initialValue, java.util.function.Consumer<String> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), String.valueOf(initialValue == null ? 0 : initialValue));
        textBox.onChanged().subscribe(onChange::accept);
        field.child(textBox);
        return field;
    }

    private ParentComponent integerFieldNullable(String label, Integer initialValue, java.util.function.Consumer<String> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.onChanged().subscribe(onChange::accept);
        field.child(textBox);
        return field;
    }

    private ParentComponent checkboxField(String label, boolean initialValue, java.util.function.Consumer<Boolean> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        CheckboxComponent checkbox = Components.checkbox(AutobuyUiTextSupport.uiText(label));
        checkbox.checked(initialValue);
        checkbox.onChanged(onChange::accept);
        field.child(checkbox);
        return field;
    }

    private Component integerFieldCompact(String placeholder, Integer initialValue, java.util.function.Consumer<String> onChange) {
        TextBoxComponent textBox = Components.textBox(Sizing.fixed(90), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.tooltip(AutobuyUiTextSupport.uiText(placeholder));
        textBox.onChanged().subscribe(onChange::accept);
        return textBox;
    }

    private ParentComponent longField(String label, Long initialValue, java.util.function.Consumer<String> onChange) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        TextBoxComponent textBox = Components.textBox(Sizing.fill(), initialValue == null ? "" : String.valueOf(initialValue));
        textBox.onChanged().subscribe(onChange::accept);
        field.child(textBox);
        return field;
    }

    private ParentComponent cycleField(String label, String value, Runnable onCycle) {
        FlowLayout field = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        field.gap(4);
        field.child(AutobuyUiComponents.secondaryLabel(label));
        field.child(AutobuyUiComponents.actionButton(value, button -> onCycle.run(), false));
        field.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        return field;
    }

    private Component selectionPickerButton(
        SearchPickerEntry previewEntry,
        String fallbackLabel,
        String emptyLabel,
        Runnable onPick,
        boolean compact
    ) {
        FlowLayout button = Containers.horizontalFlow(compact ? Sizing.expand() : Sizing.fill(), Sizing.content());
        button.surface(Surface.flat(AutobuyUiComponents.BUTTON_SECONDARY).and(Surface.outline(0xFF10151B)));
        button.padding(Insets.of(6));
        button.gap(6);
        button.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        button.cursorStyle(CursorStyle.HAND);
        button.mouseDown().subscribe((mouseX, mouseY, mouseButton) -> {
            onPick.run();
            return true;
        });

        button.child(AutobuyUiComponents.pickerPreviewIcon(previewEntry));

        FlowLayout texts = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        String title = previewEntry != null ? previewEntry.name().getString() : (AutobuyUiTextSupport.isBlank(fallbackLabel) ? emptyLabel : fallbackLabel);
        texts.child(Components.label(AutobuyUiTextSupport.uiText(title)).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
            if (compact) {
                label.maxWidth(180);
            }
        }));
        if (previewEntry != null) {
            texts.child(Components.label(AutobuyUiTextSupport.uiText(previewEntry.id())).<LabelComponent>configure(label -> {
                label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
                if (compact) {
                    label.maxWidth(180);
                }
            }));
        } else if (!AutobuyUiTextSupport.isBlank(fallbackLabel) && !fallbackLabel.equals(emptyLabel)) {
            texts.child(Components.label(AutobuyUiTextSupport.uiText(fallbackLabel)).<LabelComponent>configure(label -> {
                label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
                if (compact) {
                    label.maxWidth(180);
                }
            }));
        }
        if (!compact) {
            button.child(texts);
            button.child(AutobuyUiComponents.smallAction("Выбрать", component -> onPick.run()));
            return button;
        }

        button.child(texts);
        return button;
    }

}
