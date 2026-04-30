package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;

final class AutobuyPickerOverlayView {
    private static final int PICKER_RESULT_LIMIT = 120;

    ParentComponent build(AutobuyScreenViewHost host) {
        SearchPickerState activePicker = host.session().activePicker();
        if (activePicker == null) {
            throw new IllegalStateException("Picker overlay requested without active picker");
        }

        FlowLayout modal = Containers.verticalFlow(Sizing.fill(72), Sizing.fill(72));
        modal.surface(Surface.flat(0xF1151C23).and(Surface.outline(AutobuyUiComponents.SELECTED_OUTLINE)));
        modal.padding(Insets.of(10));
        modal.gap(8);
        modal.zIndex(300);

        FlowLayout titleRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        titleRow.child(AutobuyUiComponents.primaryLabel(activePicker.title));
        titleRow.child(AutobuyUiComponents.actionButton("Закрыть", button -> host.presenter().closePicker(), false));
        modal.child(titleRow);

        TextBoxComponent searchBox = Components.textBox(Sizing.fill(), activePicker.query);
        searchBox.tooltip(AutobuyUiTextSupport.uiText("Поиск по названию или id"));
        modal.child(searchBox);

        FlowLayout results = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        results.gap(4);
        populatePickerResults(host, results, activePicker.query);
        searchBox.onChanged().subscribe(value -> {
            host.presenter().updatePickerQuery(value);
            populatePickerResults(host, results, value);
        });

        UiScrollContainer<FlowLayout> scroll = AutobuyUiComponents.styledVerticalScroll(
            Sizing.fill(),
            Sizing.expand(),
            results,
            host.session().pickerResultsScrollProgress()
        );
        host.setPickerResultsScroll(scroll);
        modal.child(scroll);

        FlowLayout overlay = Containers.verticalFlow(Sizing.fill(), Sizing.fill());
        overlay.surface(Surface.flat(0xA0000000));
        overlay.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        overlay.positioning(Positioning.absolute(0, 0));
        overlay.zIndex(250);
        overlay.child(modal);
        return overlay;
    }

    private void populatePickerResults(AutobuyScreenViewHost host, FlowLayout results, String query) {
        results.clearChildren();

        SearchPickerState picker = host.session().activePicker();
        if (picker == null) {
            return;
        }

        String normalizedQuery = host.pickerCatalog().normalizeSearch(query);
        int added = 0;
        for (SearchPickerEntry entry : picker.entries) {
            if (!normalizedQuery.isBlank() && !entry.searchText().contains(normalizedQuery)) {
                continue;
            }

            results.child(buildPickerResultRow(host, entry));
            added++;
            if (added >= PICKER_RESULT_LIMIT) {
                break;
            }
        }

        if (added == 0) {
            results.child(AutobuyUiComponents.mutedLabel("Ничего не найдено"));
        }
    }

    private Component buildPickerResultRow(AutobuyScreenViewHost host, SearchPickerEntry entry) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        row.padding(Insets.of(6));
        row.gap(6);

        row.child(buildPickerIcon(entry));

        FlowLayout texts = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        texts.child(Components.label(entry.name().copy().setStyle(entry.name().getStyle().withFont(AutobuyUiTextSupport.UI_FONT)))
            .<LabelComponent>configure(label -> label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY))));
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.id())).<LabelComponent>configure(label -> label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY))));
        row.child(texts);

        ButtonComponent selectButton = AutobuyUiComponents.smallAction("Выбрать", button -> host.presenter().selectPickerEntry(entry));
        selectButton.horizontalSizing(Sizing.content(8));
        row.child(selectButton);
        return row;
    }

    private Component buildPickerIcon(SearchPickerEntry entry) {
        FlowLayout icon = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        icon.gap(3);

        if (entry.itemStack() != null) {
            icon.child(Components.item(entry.itemStack()).margins(Insets.right(2)));
        }

        if (entry.statusEffect() != null) {
            icon.child(Components.sprite(MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(entry.statusEffect()))
                .sizing(Sizing.fixed(18), Sizing.fixed(18)));
        }

        if (entry.badgeText() != null && !entry.badgeText().isBlank()) {
            FlowLayout badge = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18));
            badge.surface(Surface.flat(entry.badgeColor()).and(Surface.outline(0xFF101418)));
            badge.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            badge.child(Components.label(AutobuyUiTextSupport.uiText(entry.badgeText())).<LabelComponent>configure(label -> label.color(Color.ofRgb(0xFFFFFFFF))));
            icon.child(badge);
        }

        return icon;
    }
}
