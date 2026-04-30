package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;

final class AutobuyUiTextSupport {
    static final Identifier UI_FONT = Identifier.of("minecraft", "uniform");
    static final String ICON_DUPLICATE = "⧉";
    static final String ICON_MOVE_UP = "↑";
    static final String ICON_MOVE_DOWN = "↓";
    static final String ICON_DELETE = "🗑";

    private AutobuyUiTextSupport() {
    }

    static String displayRuleTitle(AutobuyConfigDraft.BuyRuleDraft rule, int index) {
        if (rule.name != null && !rule.name.isBlank()) {
            return rule.name;
        }
        if (rule.id != null && !rule.id.isBlank()) {
            return rule.id;
        }
        return "Правило " + (index + 1);
    }

    static String shortTypeName(AutobuyConfigDraft.ConditionType type) {
        return switch (type) {
            case MINECRAFT_ID -> "Minecraft ID";
            case DISPLAY_NAME -> "Название предмета";
            case MAX_TOTAL_PRICE -> "Макс. цена";
            case MAX_UNIT_PRICE -> "Макс. цена за шт.";
            case MIN_COUNT -> "Мин. кол-во";
            case MAX_COUNT -> "Макс. кол-во";
            case REQUIRED_ENCHANTMENTS -> "Чары";
            case REQUIRED_POTION_EFFECTS -> "Эффекты зелий";
            case SELLER_ALLOW_LIST -> "Продавцы+";
            case SELLER_DENY_LIST -> "Продавцы-";
        };
    }

    static String localizeConditionType(AutobuyConfigDraft.ConditionType type) {
        return switch (type) {
            case MINECRAFT_ID -> "minecraft ID";
            case DISPLAY_NAME -> "название предмета";
            case MAX_TOTAL_PRICE -> "макс. общая цена";
            case MAX_UNIT_PRICE -> "макс. цена за штуку";
            case MIN_COUNT -> "мин. количество";
            case MAX_COUNT -> "макс. количество";
            case REQUIRED_ENCHANTMENTS -> "зачарования";
            case REQUIRED_POTION_EFFECTS -> "эффекты зелий";
            case SELLER_ALLOW_LIST -> "белый список продавцов";
            case SELLER_DENY_LIST -> "чёрный список продавцов";
        };
    }

    static String localizeLogMode(AutobuyScanLogMode mode) {
        return switch (mode) {
            case ALL -> "Все";
            case MATCHED_ONLY -> "Только совпадения";
        };
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static Text uiText(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withFont(UI_FONT));
    }
}
