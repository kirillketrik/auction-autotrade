package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;

final class AutobuyConfigHeaderView {
    ParentComponent build(AutobuyScreenViewHost host) {
        AutobuyConfigSession session = host.session();

        FlowLayout header = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        header.surface(io.wispforest.owo.ui.core.Surface.flat(AutobuyUiComponents.PANEL_BACKGROUND).and(io.wispforest.owo.ui.core.Surface.outline(AutobuyUiComponents.PANEL_OUTLINE)));
        header.padding(Insets.of(8));
        header.gap(8);

        FlowLayout titleRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        ButtonComponent saveButton = AutobuyUiComponents.actionButton("Сохранить", button -> host.presenter().save(), true);
        saveButton.active(session.isValid());
        host.setSaveButton(saveButton);
        titleRow.child(saveButton);
        titleRow.child(AutobuyUiComponents.actionButton("Загрузить", button -> host.presenter().reloadFromFile(), false));
        titleRow.child(AutobuyUiComponents.actionButton("Сбросить", button -> host.presenter().discardDraft(), false));
        titleRow.child(AutobuyUiComponents.actionButton(host.presenter().isAutobuyEnabled() ? "Стоп" : "Старт", button -> host.presenter().toggleAutobuy(), false));
        titleRow.child(AutobuyUiComponents.actionButton("Закрыть", button -> host.presenter().requestClose(), false));
        header.child(titleRow);

        FlowLayout tabRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        tabRow.gap(6);
        tabRow.child(AutobuyUiComponents.smallAction(
            "Правила",
            button -> host.presenter().selectTab(AutobuyScreenTab.CONFIG),
            host.session().activeTab() == AutobuyScreenTab.CONFIG
        ));
        tabRow.child(AutobuyUiComponents.smallAction(
            "История покупок",
            button -> host.presenter().selectTab(AutobuyScreenTab.PURCHASE_HISTORY),
            host.session().activeTab() == AutobuyScreenTab.PURCHASE_HISTORY
        ));
        header.child(tabRow);

        if (!session.statusMessage().isBlank()) {
            header.child(AutobuyUiComponents.infoStripe(
                session.statusMessage(),
                session.validationErrors().isEmpty() ? AutobuyUiComponents.TEXT_WARNING : AutobuyUiComponents.TEXT_DANGER
            ));
        }

        if (session.confirmClosePending()) {
            FlowLayout confirmRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
            confirmRow.gap(6);
            confirmRow.child(Components.label(AutobuyUiTextSupport.uiText("Закрыть окно с несохранёнными изменениями?"))
                .<LabelComponent>configure(label -> label.color(Color.ofRgb(AutobuyUiComponents.TEXT_DANGER))));
            confirmRow.child(AutobuyUiComponents.actionButton("Сохранить и закрыть", button -> host.presenter().saveAndClose(), true));
            confirmRow.child(AutobuyUiComponents.actionButton("Без сохранения", button -> host.presenter().discardAndClose(), false));
            confirmRow.child(AutobuyUiComponents.actionButton("Отмена", button -> host.presenter().cancelCloseConfirmation(), false));
            header.child(confirmRow);
        }

        if (!session.validationErrors().isEmpty()) {
            for (String error : session.validationErrors()) {
                header.child(AutobuyUiComponents.infoStripe(error, AutobuyUiComponents.TEXT_DANGER));
            }
        }

        header.alignment(HorizontalAlignment.LEFT, io.wispforest.owo.ui.core.VerticalAlignment.CENTER);
        return header;
    }
}
