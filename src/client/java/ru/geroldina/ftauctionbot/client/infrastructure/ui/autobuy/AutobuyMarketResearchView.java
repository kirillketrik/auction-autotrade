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
import ru.geroldina.ftauctionbot.client.domain.market.MarketPriceRecommendation;
import ru.geroldina.ftauctionbot.client.domain.market.MarketResearchResult;

import java.util.List;

final class AutobuyMarketResearchView {
    ParentComponent build(AutobuyScreenViewHost host) {
        FlowLayout content = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        content.gap(8);

        FlowLayout actions = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        actions.gap(6);
        actions.child(AutobuyUiComponents.primaryLabel("Исследование рынка"));
        actions.child(AutobuyUiComponents.horizontalSpacer());
        actions.child(AutobuyUiComponents.actionButton("Обновить", button -> host.presenter().reloadMarketResearchResults(), false));
        content.child(actions);

        List<MarketResearchResult> entries = host.session().marketResearchResults();
        if (entries.isEmpty()) {
            content.child(AutobuyUiComponents.emptyState("Нет результатов", "Запустите исследование рынка из редактора выбранного правила."));
        } else {
            content.child(buildHeaderRow());
            for (MarketResearchResult entry : entries) {
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
            host.session().marketResearchScrollProgress()
        );
        host.setMarketResearchScroll(scroll);
        panel.child(scroll);
        return panel;
    }

    private ParentComponent buildHeaderRow() {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        row.padding(Insets.of(6));
        row.child(columnLabel("Предмет", 36));
        row.child(columnLabel("Статистика", 24));
        row.child(columnLabel("Рекомендации", 40));
        return row;
    }

    private ParentComponent buildEntryRow(MarketResearchResult entry) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.surface(Surface.flat(AutobuyUiComponents.CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.ACCENT_OUTLINE)));
        row.padding(Insets.of(6));
        row.gap(4);
        row.child(itemCell(entry));
        row.child(statsCell(entry));
        row.child(recommendationsCell(entry.recommendations()));
        return row;
    }

    private ParentComponent itemCell(MarketResearchResult entry) {
        FlowLayout cell = Containers.horizontalFlow(Sizing.fill(36), Sizing.content());
        cell.gap(6);
        SearchPickerEntry preview = hostlessPreview(entry.targetMinecraftId());
        cell.child(AutobuyUiComponents.pickerPreviewIcon(preview));

        FlowLayout texts = Containers.verticalFlow(Sizing.expand(), Sizing.content());
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.itemDisplayName())).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
            label.maxWidth(220);
        }));
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.ruleName())).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
            label.maxWidth(220);
        }));
        texts.child(Components.label(AutobuyUiTextSupport.uiText(entry.targetMinecraftId())).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
            label.maxWidth(220);
        }));
        cell.child(texts);
        return cell;
    }

    private ParentComponent statsCell(MarketResearchResult entry) {
        FlowLayout cell = Containers.verticalFlow(Sizing.fill(24), Sizing.content());
        cell.gap(2);
        cell.child(statLine("Min", formatPrice(entry.minUnitPrice())));
        cell.child(statLine("Avg", formatPrice(entry.avgUnitPrice())));
        cell.child(statLine("Max", formatPrice(entry.maxUnitPrice())));
        cell.child(statLine("p25/p50/p75", formatPrice(entry.p25UnitPrice()) + " / " + formatPrice(entry.p50UnitPrice()) + " / " + formatPrice(entry.p75UnitPrice())));
        cell.child(statLine("Gap", formatPrice(entry.largestUnitPriceGap())));
        cell.child(statLine("Страницы/лоты", entry.scannedPages() + " / " + entry.matchedLots()));
        cell.child(statLine("Кластеры", formatCluster(entry)));
        return cell;
    }

    private ParentComponent recommendationsCell(List<MarketPriceRecommendation> recommendations) {
        FlowLayout cell = Containers.verticalFlow(Sizing.fill(40), Sizing.content());
        cell.gap(4);
        if (recommendations.isEmpty()) {
            cell.child(AutobuyUiComponents.mutedLabel("Нет рекомендаций"));
            return cell;
        }

        for (MarketPriceRecommendation recommendation : recommendations) {
            FlowLayout card = Containers.verticalFlow(Sizing.fill(), Sizing.content());
            card.surface(Surface.flat(AutobuyUiComponents.SUBTLE_CARD_BACKGROUND).and(Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
            card.padding(Insets.of(5));
            card.gap(2);
            card.child(Components.label(AutobuyUiTextSupport.uiText(recommendation.title() + " [" + recommendation.riskLabel() + "]")).<LabelComponent>configure(label -> {
                label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
                label.maxWidth(320);
            }));
            card.child(Components.label(AutobuyUiTextSupport.uiText(recommendation.summary())).<LabelComponent>configure(label -> {
                label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
                label.maxWidth(320);
            }));
            if ("ACTIVE".equals(recommendation.status())) {
                card.child(statLine("Buy<= ", formatPrice(recommendation.maxBuyPrice())));
                card.child(statLine("Sell", formatPrice(recommendation.recommendedSellPrice())));
                card.child(statLine("Маржа", recommendation.expectedMarginPercent() + "%"));
            } else {
                card.child(statLine("Статус", recommendation.reason() == null ? recommendation.status() : recommendation.reason()));
            }
            cell.child(card);
        }
        return cell;
    }

    private LabelComponent columnLabel(String text, int widthPercent) {
        return Components.label(AutobuyUiTextSupport.uiText(text)).configure(label -> {
            label.horizontalSizing(Sizing.fill(widthPercent));
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY));
        });
    }

    private ParentComponent statLine(String title, String value) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(4);
        row.child(Components.label(AutobuyUiTextSupport.uiText(title + ":")).<LabelComponent>configure(label -> label.color(Color.ofRgb(AutobuyUiComponents.TEXT_SECONDARY))));
        row.child(Components.label(AutobuyUiTextSupport.uiText(value)).<LabelComponent>configure(label -> {
            label.color(Color.ofRgb(AutobuyUiComponents.TEXT_PRIMARY));
            label.maxWidth(320);
        }));
        return row;
    }

    private String formatPrice(Long price) {
        return price == null ? "n/a" : "$" + price;
    }

    private String formatCluster(MarketResearchResult entry) {
        if (entry.lowerClusterSize() == null || entry.mainClusterSize() == null) {
            return "n/a";
        }
        return entry.lowerClusterSize() + " / " + entry.mainClusterSize();
    }

    private SearchPickerEntry hostlessPreview(String minecraftId) {
        if (AutobuyUiTextSupport.isBlank(minecraftId)) {
            return null;
        }
        return new AutobuyPickerCatalog().resolveItemSelection(minecraftId);
    }
}
