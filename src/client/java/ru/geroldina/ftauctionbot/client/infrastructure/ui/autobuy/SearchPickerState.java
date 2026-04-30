package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import java.util.List;
import java.util.function.Consumer;

final class SearchPickerState {
    final String title;
    final List<SearchPickerEntry> entries;
    final Consumer<String> onPick;
    String query = "";

    SearchPickerState(String title, List<SearchPickerEntry> entries, Consumer<String> onPick) {
        this.title = title;
        this.entries = entries;
        this.onPick = onPick;
    }
}
