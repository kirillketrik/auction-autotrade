package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
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
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

final class AutobuyRuleListView {
    ParentComponent build(AutobuyScreenViewHost host) {
        AutobuyConfigSession session = host.session();

        FlowLayout outer = Containers.verticalFlow(Sizing.fixed(AutobuyUiComponents.RULE_LIST_WIDTH), Sizing.fill());
        outer.surface(Surface.flat(AutobuyUiComponents.PANEL_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        outer.padding(Insets.of(8));
        outer.gap(8);

        FlowLayout header = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        header.child(AutobuyUiComponents.primaryLabel("Конфигурации"));
        header.child(AutobuyUiComponents.horizontalSpacer());
        header.child(AutobuyUiComponents.actionButton("Создать", button -> host.presenter().createRule(), true));
        header.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        outer.child(header);

        FlowLayout ruleList = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        ruleList.gap(5);
        if (session.draft().buyRules.isEmpty()) {
            ruleList.child(AutobuyUiComponents.emptyState("Пока пусто", "Создайте первое правило, чтобы начать настройку."));
        } else {
            for (int i = 0; i < session.draft().buyRules.size(); i++) {
                ruleList.child(buildRuleListEntry(host, i, session.draft().buyRules.get(i)));
            }
        }

        UiScrollContainer<FlowLayout> scroll = AutobuyUiComponents.styledVerticalScroll(
            Sizing.fill(),
            Sizing.expand(),
            ruleList,
            session.ruleListScrollProgress()
        );
        host.setRuleListScroll(scroll);
        outer.child(scroll);

        if (session.selectedRuleIndex() >= 0 && session.selectedRuleIndex() < session.draft().buyRules.size()) {
            FlowLayout actions = Containers.verticalFlow(Sizing.fill(), Sizing.content());
            actions.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
            actions.padding(Insets.of(7));
            actions.gap(6);
            actions.child(AutobuyUiComponents.mutedLabel("Действия для выбранного правила"));

            FlowLayout rowOne = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            rowOne.gap(4);
            rowOne.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_MOVE_UP, "Поднять правило выше", button -> host.presenter().moveSelectedRule(-1)));
            rowOne.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_MOVE_DOWN, "Опустить правило ниже", button -> host.presenter().moveSelectedRule(1)));
            actions.child(rowOne);

            FlowLayout rowTwo = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            rowTwo.gap(4);
            rowTwo.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_DUPLICATE, "Дублировать правило", button -> host.presenter().duplicateSelectedRule()));
            rowTwo.child(AutobuyUiComponents.iconSmallAction(AutobuyUiTextSupport.ICON_DELETE, "Удалить правило", button -> host.presenter().deleteSelectedRule()));
            actions.child(rowTwo);
            outer.child(actions);
        }

        return outer;
    }

    private ParentComponent buildRuleListEntry(AutobuyScreenViewHost host, int ruleIndex, AutobuyConfigDraft.BuyRuleDraft rule) {
        boolean selected = ruleIndex == host.session().selectedRuleIndex();
        FlowLayout card = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        card.surface(Surface.flat(selected ? AutobuyUiComponents.SELECTED_CARD : AutobuyUiComponents.CARD_BACKGROUND)
            .and(Surface.outline(selected ? AutobuyUiComponents.SELECTED_OUTLINE : AutobuyUiComponents.PANEL_OUTLINE)));
        card.padding(Insets.of(7));
        card.gap(7);
        card.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        card.cursorStyle(CursorStyle.HAND);
        card.mouseDown().subscribe((mouseX, mouseY, button) -> {
            host.presenter().openRuleEditor(ruleIndex);
            return true;
        });

        FlowLayout content = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        content.gap(7);
        content.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        content.child(buildRuleAvatar(ruleIndex, rule));
        content.child(Components.label(AutobuyUiTextSupport.uiText(AutobuyUiTextSupport.displayRuleTitle(rule, ruleIndex))).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
            label.maxWidth(1000);
        }));
        card.child(content);
        card.child(AutobuyUiComponents.horizontalSpacer());
        card.child(AutobuyUiComponents.smallAction(rule.enabled ? "Вкл" : "Выкл", button -> host.presenter().toggleRuleEnabled(rule), rule.enabled));
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
            avatar.child(Components.label(AutobuyUiTextSupport.uiText(ruleInitials(rule))).<LabelComponent>configure(label -> label.color(Color.ofRgb(0xFFFFFFFF))));
        }

        return avatar;
    }

    private ItemStack previewStackForRule(AutobuyConfigDraft.BuyRuleDraft rule) {
        for (AutobuyConfigDraft.ConditionDraft condition : rule.conditions) {
            if (condition.type != AutobuyConfigDraft.ConditionType.MINECRAFT_ID || AutobuyUiTextSupport.isBlank(condition.stringValue)) {
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
        String source = !AutobuyUiTextSupport.isBlank(rule.name) ? rule.name : (!AutobuyUiTextSupport.isBlank(rule.id) ? rule.id : "rule");
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
        String base = AutobuyUiTextSupport.displayRuleTitle(rule, ruleIndex);
        int hash = Math.abs(base.hashCode());
        int red = 60 + hash % 70;
        int green = 90 + (hash / 7) % 80;
        int blue = 110 + (hash / 13) % 90;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
