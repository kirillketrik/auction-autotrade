package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import ru.geroldina.ftauctionbot.client.application.autobuy.AutobuyLoopController;

interface AutobuyLoopControl {
    boolean isEnabled();

    void start();

    void stop();

    static AutobuyLoopControl from(AutobuyLoopController controller) {
        return new AutobuyLoopControl() {
            @Override
            public boolean isEnabled() {
                return controller.isEnabled();
            }

            @Override
            public void start() {
                controller.start();
            }

            @Override
            public void stop() {
                controller.stop();
            }
        };
    }
}
