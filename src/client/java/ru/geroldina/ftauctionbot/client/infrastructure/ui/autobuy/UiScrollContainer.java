package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;

final class UiScrollContainer<C extends Component> extends ScrollContainer<C> {
    private Double pendingProgress;

    UiScrollContainer(Sizing horizontalSizing, Sizing verticalSizing, C child) {
        super(ScrollDirection.VERTICAL, horizontalSizing, verticalSizing, child);
    }

    double progress() {
        return this.maxScroll <= 0 ? 0 : Math.clamp(this.scrollOffset / this.maxScroll, 0, 1);
    }

    void restoreProgress(double progress) {
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
