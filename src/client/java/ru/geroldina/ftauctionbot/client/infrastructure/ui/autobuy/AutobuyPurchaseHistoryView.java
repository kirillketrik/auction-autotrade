package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.PurchaseHistoryEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class AutobuyPurchaseHistoryView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    ParentComponent build(AutobuyScreenViewHost host) {
        FlowLayout content = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        content.gap(8);

        FlowLayout actions = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        actions.gap(6);
        actions.child(AutobuyUiComponents.primaryLabel("История покупок"));
        actions.child(AutobuyUiComponents.horizontalSpacer());
        actions.child(AutobuyUiComponents.actionButton("Обновить", button -> host.presenter().reloadPurchaseHistory(), false));
        content.child(actions);

        List<PurchaseHistoryEntry> entries = host.session().purchaseHistoryEntries();
        if (entries.isEmpty()) {
            content.child(AutobuyUiComponents.emptyState("Покупок пока нет", "Когда бот купит предмет, запись появится здесь."));
        } else {
            content.child(buildHeaderRow());
            for (PurchaseHistoryEntry entry : entries) {
                content.child(buildEntryRow(entry));
            }
        }

        FlowLayout panel = Containers.verticalFlow(Sizing.fill(), Sizing.fill());
        panel.surface(Surface.flat(AutobuyUiComponents.PANEL_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        panel.padding(Insets.of(8));
        panel.gap(8);
        UiScrollContainer<FlowLayout> scroll = AutobuyUiComponents.styledVerticalScroll(
            Sizing.fill(),
            Sizing.fill(),
            content,
            host.session().historyScrollProgress()
        );
        host.setHistoryScroll(scroll);
        panel.child(scroll);
        return panel;
    }

    private ParentComponent buildHeaderRow() {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        row.padding(Insets.of(6));
        row.child(columnLabel("Предмет", 38));
        row.child(columnLabel("Кол-во", 12));
        row.child(columnLabel("Цена", 18));
        row.child(columnLabel("Дата и время", 32));
        return row;
    }

    private ParentComponent buildEntryRow(PurchaseHistoryEntry entry) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(AutobuyUiComponents.CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.ACCENT_OUTLINE)));
        row.padding(Insets.of(6));
        row.gap(4);
        row.child(itemCell(entry));
        row.child(cell(String.valueOf(entry.count()), 12));
        row.child(cell(formatPrice(entry.totalPrice()), 18));
        row.child(cell(formatDateTime(entry.purchasedAtEpochMillis()), 32));
        return row;
    }

    private ParentComponent itemCell(PurchaseHistoryEntry entry) {
        FlowLayout cell = Containers.horizontalFlow(Sizing.fill(38), Sizing.content());
        cell.gap(6);
        SearchPickerEntry preview = hostlessPreview(entry);
        cell.child(AutobuyUiComponents.pickerPreviewIcon(preview));
        FlowLayout texts = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.displayName())).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
            label.maxWidth(220);
        }));
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.minecraftId())).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
            label.maxWidth(220);
        }));
        cell.child(texts);
        return cell;
    }

    private ParentComponent cell(String text, int widthPercent) {
        FlowLayout cell = Containers.verticalFlow(Sizing.fill(widthPercent), Sizing.content());
        cell.child(Components.label(AutobuyUiTextSupport.uiText(text)).<LabelComponent>configure(label -> label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY))));
        return cell;
    }

    private LabelComponent columnLabel(String text, int widthPercent) {
        return Components.label(AutobuyUiTextSupport.uiText(text)).configure(label -> {
            label.horizontalSizing(Sizing.fill(widthPercent));
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
        });
    }

    private String formatPrice(long totalPrice) {
        return "$" + totalPrice;
    }

    private String formatDateTime(long epochMillis) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private SearchPickerEntry hostlessPreview(PurchaseHistoryEntry entry) {
        String id = entry.minecraftId();
        if (AutobuyUiTextSupport.isBlank(id)) {
            return null;
        }
        return new AutobuyPickerCatalog().resolveItemSelection(id);
    }
}
