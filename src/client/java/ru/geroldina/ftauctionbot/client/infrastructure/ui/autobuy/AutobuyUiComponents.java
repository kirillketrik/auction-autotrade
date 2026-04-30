package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

final class AutobuyUiComponents {
    static final int SCREEN_WIDTH_PERCENT = 92;
    static final int SCREEN_HEIGHT_PERCENT = 88;
    static final int RULE_LIST_WIDTH = 248;
    static final int APP_BACKGROUND = 0xE40B0E13;
    static final int PANEL_BACKGROUND = 0xCC10151C;
    static final int CARD_BACKGROUND = 0xCC171E27;
    static final int SUBTLE_CARD_BACKGROUND = 0xB8141A22;
    static final int PANEL_OUTLINE = 0xFF232D39;
    static final int ACCENT_OUTLINE = 0xFF39495C;
    static final int SELECTED_CARD = 0xFF202C38;
    static final int SELECTED_OUTLINE = 0xFF657A94;
    static final int TEXT_PRIMARY = 0xFFF3F5F7;
    static final int TEXT_SECONDARY = 0xFF9AA8B8;
    static final int TEXT_MUTED = 0xFF6C7A89;
    static final int TEXT_WARNING = 0xFFF0B486;
    static final int TEXT_DANGER = 0xFFFFB4A2;
    static final int BUTTON_SECONDARY = 0xFF151E27;
    static final int BUTTON_SECONDARY_HOVER = 0xFF223040;
    static final int BUTTON_SUCCESS = 0xFF234B42;
    static final int BUTTON_SUCCESS_HOVER = 0xFF337163;
    static final int BUTTON_SELECTED = 0xFF30455A;
    static final int BUTTON_SELECTED_HOVER = 0xFF42627E;
    static final int SCROLLBAR_COLOR = 0x90A7B9CC;

    private AutobuyUiComponents() {
    }

    static FlowLayout card(String title, String subtitle) {
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

    static ParentComponent emptyCard(String title, String text) {
        FlowLayout card = card(title, null);
        card.child(emptyState(title, text));
        return card;
    }

    static ParentComponent sectionTag(String text) {
        FlowLayout tag = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        tag.surface(Surface.flat(0xCC101722).and(Surface.outline(ACCENT_OUTLINE)));
        tag.padding(Insets.of(5, 5, 5, 5));
        tag.child(Components.label(AutobuyUiTextSupport.uiText(text)).<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_SECONDARY))));
        tag.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        return tag;
    }

    static ParentComponent infoStripe(String text, int color) {
        FlowLayout stripe = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        stripe.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        stripe.padding(Insets.of(6, 8, 6, 8));
        stripe.child(Components.label(AutobuyUiTextSupport.uiText(text)).<LabelComponent>configure(label -> label.color(Color.ofRgb(color))));
        return stripe;
    }

    static ParentComponent emptyState(String title, String text) {
        FlowLayout state = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        state.surface(Surface.flat(SUBTLE_CARD_BACKGROUND).and(Surface.outline(PANEL_OUTLINE)));
        state.padding(Insets.of(10));
        state.gap(4);
        state.child(Components.label(AutobuyUiTextSupport.uiText(title)).<LabelComponent>configure(label -> label.color(Color.ofRgb(TEXT_PRIMARY))));
        state.child(mutedLabel(text));
        return state;
    }

    static ParentComponent detailRow(String label, String value) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.child(mutedLabel(label));
        row.child(Components.label(AutobuyUiTextSupport.uiText(value)).<LabelComponent>configure(component -> component.color(Color.ofRgb(TEXT_PRIMARY))));
        return row;
    }

    static LabelComponent primaryLabel(String text) {
        LabelComponent label = Components.label(AutobuyUiTextSupport.uiText(text));
        label.color(Color.ofRgb(TEXT_PRIMARY));
        label.shadow(false);
        return label;
    }

    static LabelComponent secondaryLabel(String text) {
        LabelComponent label = Components.label(AutobuyUiTextSupport.uiText(text));
        label.color(Color.ofRgb(TEXT_SECONDARY));
        label.shadow(false);
        return label;
    }

    static LabelComponent mutedLabel(String text) {
        LabelComponent label = Components.label(AutobuyUiTextSupport.uiText(text));
        label.color(Color.ofRgb(TEXT_MUTED));
        label.shadow(false);
        return label;
    }

    static ButtonComponent actionButton(String label, Consumer<ButtonComponent> onPress, boolean emphasized) {
        ButtonComponent button = Components.button(AutobuyUiTextSupport.uiText(label), onPress);
        button.horizontalSizing(Sizing.content(8));
        button.renderer(ButtonComponent.Renderer.flat(
            emphasized ? BUTTON_SUCCESS : BUTTON_SECONDARY,
            emphasized ? BUTTON_SUCCESS_HOVER : BUTTON_SECONDARY_HOVER,
            0xFF10151B
        ));
        return button;
    }

    static ButtonComponent smallAction(String label, Consumer<ButtonComponent> onPress) {
        return smallAction(label, onPress, false);
    }

    static ButtonComponent smallAction(String label, Consumer<ButtonComponent> onPress, boolean emphasized) {
        ButtonComponent button = Components.button(AutobuyUiTextSupport.uiText(label), onPress);
        button.horizontalSizing(Sizing.content(6));
        button.renderer(ButtonComponent.Renderer.flat(
            emphasized ? BUTTON_SELECTED : BUTTON_SECONDARY,
            emphasized ? BUTTON_SELECTED_HOVER : BUTTON_SECONDARY_HOVER,
            0xFF10151B
        ));
        return button;
    }

    static ButtonComponent iconSmallAction(String icon, String tooltip, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = smallAction(icon, onPress, false);
        button.tooltip(AutobuyUiTextSupport.uiText(tooltip));
        return button;
    }

    static ButtonComponent pickerSelectionButton(String label, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = actionButton(label, onPress, false);
        button.horizontalSizing(Sizing.fill());
        return button;
    }

    static ButtonComponent compactPickerButton(String label, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = actionButton(label, onPress, false);
        button.horizontalSizing(Sizing.expand());
        return button;
    }

    static Component horizontalSpacer() {
        return new BaseComponent() {
            {
                this.sizing(Sizing.expand(), Sizing.fixed(0));
            }

            @Override
            public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            }
        };
    }

    static UiScrollContainer<FlowLayout> styledVerticalScroll(Sizing horizontalSizing, Sizing verticalSizing, FlowLayout child, double progress) {
        UiScrollContainer<FlowLayout> scroll = new UiScrollContainer<>(horizontalSizing, verticalSizing, child);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(SCROLLBAR_COLOR)));
        scroll.scrollbarThiccness(4);
        scroll.restoreProgress(progress);
        return scroll;
    }

    static Component pickerPreviewIcon(SearchPickerEntry previewEntry) {
        FlowLayout icon = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        icon.gap(3);
        icon.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        if (previewEntry != null && previewEntry.itemStack() != null) {
            icon.child(Components.item(previewEntry.itemStack()).margins(Insets.right(2)));
            return icon;
        }

        if (previewEntry != null && previewEntry.statusEffect() != null) {
            icon.child(Components.sprite(MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(previewEntry.statusEffect()))
                .sizing(Sizing.fixed(18), Sizing.fixed(18)));
            return icon;
        }

        FlowLayout badge = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18));
        int badgeColor = previewEntry != null && previewEntry.badgeText() != null && !previewEntry.badgeText().isBlank()
            ? previewEntry.badgeColor()
            : BUTTON_SELECTED;
        String badgeText = previewEntry != null && previewEntry.badgeText() != null && !previewEntry.badgeText().isBlank()
            ? previewEntry.badgeText()
            : "?";
        badge.surface(Surface.flat(badgeColor).and(Surface.outline(0xFF101418)));
        badge.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        badge.child(Components.label(AutobuyUiTextSupport.uiText(badgeText)).<LabelComponent>configure(label -> label.color(Color.ofRgb(0xFFFFFFFF))));
        icon.child(badge);
        return icon;
    }
}
