package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;

interface AutobuyScreenViewHost {
    AutobuyConfigSession session();

    AutobuyConfigPresenter presenter();

    AutobuyPickerCatalog pickerCatalog();

    void setSaveButton(ButtonComponent buttonComponent);

    void setRuleListScroll(UiScrollContainer<FlowLayout> scroll);

    void setEditorScroll(UiScrollContainer<FlowLayout> scroll);

    void setHistoryScroll(UiScrollContainer<FlowLayout> scroll);

    void setMarketResearchScroll(UiScrollContainer<FlowLayout> scroll);

    void setPickerResultsScroll(UiScrollContainer<FlowLayout> scroll);
}
