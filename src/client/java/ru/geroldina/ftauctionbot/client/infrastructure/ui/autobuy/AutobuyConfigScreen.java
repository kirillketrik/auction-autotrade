package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyConfigManager;
import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyLoopController;

public final class AutobuyConfigScreen extends BaseOwoScreen<StackLayout> implements AutobuyScreenViewHost {
    private final AutobuyConfigSession session;
    private final AutobuyConfigPresenter presenter;
    private final AutobuyPickerCatalog pickerCatalog;
    private final AutobuyConfigHeaderView headerView = new AutobuyConfigHeaderView();
    private final AutobuyRuleListView ruleListView = new AutobuyRuleListView();
    private final AutobuyRuleEditorView ruleEditorView = new AutobuyRuleEditorView();
    private final AutobuyPickerOverlayView pickerOverlayView = new AutobuyPickerOverlayView();

    private io.wispforest.owo.ui.component.ButtonComponent saveButton;
    private UiScrollContainer<FlowLayout> ruleListScroll;
    private UiScrollContainer<FlowLayout> editorScroll;
    private UiScrollContainer<FlowLayout> pickerResultsScroll;

    public AutobuyConfigScreen(AutobuyConfigManager configManager, AutobuyLoopController autobuyLoopController) {
        super(AutobuyUiTextSupport.uiText("Конфигурация автобая"));
        AutobuyConfigValidator validator = new AutobuyConfigValidator();
        this.session = new AutobuyConfigSession(validator);
        this.pickerCatalog = new AutobuyPickerCatalog();
        this.presenter = new AutobuyConfigPresenter(
            configManager,
            AutobuyLoopControl.from(autobuyLoopController),
            session,
            pickerCatalog,
            this::rebuildUi,
            this::doClose
        );
        this.presenter.initialize();
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
        presenter.requestClose();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public AutobuyConfigSession session() {
        return session;
    }

    @Override
    public AutobuyConfigPresenter presenter() {
        return presenter;
    }

    @Override
    public AutobuyPickerCatalog pickerCatalog() {
        return pickerCatalog;
    }

    @Override
    public void setSaveButton(io.wispforest.owo.ui.component.ButtonComponent buttonComponent) {
        this.saveButton = buttonComponent;
    }

    @Override
    public void setRuleListScroll(UiScrollContainer<FlowLayout> scroll) {
        this.ruleListScroll = scroll;
    }

    @Override
    public void setEditorScroll(UiScrollContainer<FlowLayout> scroll) {
        this.editorScroll = scroll;
    }

    @Override
    public void setPickerResultsScroll(UiScrollContainer<FlowLayout> scroll) {
        this.pickerResultsScroll = scroll;
    }

    private void rebuildUi() {
        if (uiAdapter == null) {
            return;
        }

        captureScrollState();

        StackLayout root = this.uiAdapter.rootComponent;
        root.clearChildren();
        root.surface(Surface.flat(0xD107090C));
        root.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        FlowLayout frame = Containers.verticalFlow(
            Sizing.fill(AutobuyUiComponents.SCREEN_WIDTH_PERCENT),
            Sizing.fill(AutobuyUiComponents.SCREEN_HEIGHT_PERCENT)
        );
        frame.surface(Surface.flat(AutobuyUiComponents.APP_BACKGROUND).and(Surface.outline(AutobuyUiComponents.ACCENT_OUTLINE)));
        frame.padding(Insets.of(10));
        frame.gap(8);
        root.child(frame);

        frame.child(headerView.build(this));
        frame.child(buildBody());

        if (saveButton != null) {
            saveButton.active(session.isValid());
        }
        if (session.activePicker() != null) {
            root.child(pickerOverlayView.build(this));
        }
    }

    private FlowLayout buildBody() {
        FlowLayout body = Containers.horizontalFlow(Sizing.fill(), Sizing.expand());
        body.gap(8);
        body.child(ruleListView.build(this));
        body.child(ruleEditorView.build(this));
        return body;
    }

    private void captureScrollState() {
        if (ruleListScroll != null) {
            session.ruleListScrollProgress(ruleListScroll.progress());
        }
        if (editorScroll != null) {
            session.editorScrollProgress(editorScroll.progress());
        }
        if (pickerResultsScroll != null) {
            session.pickerResultsScrollProgress(pickerResultsScroll.progress());
        }
    }

    private void doClose() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
