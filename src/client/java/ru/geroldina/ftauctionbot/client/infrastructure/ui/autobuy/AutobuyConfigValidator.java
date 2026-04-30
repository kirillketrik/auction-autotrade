package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import java.util.ArrayList;
import java.util.List;

final class AutobuyConfigValidator {
    AutobuyValidationResult validate(AutobuyConfigDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft.scanIntervalSeconds <= 0) {
            errors.add("Интервал сканирования должен быть больше 0.");
        }
        if (draft.scanIntervalJitterSeconds < 0) {
            errors.add("Разброс интервала сканирования не может быть отрицательным.");
        }
        if (draft.scanIntervalJitterSeconds >= draft.scanIntervalSeconds) {
            errors.add("Разброс интервала сканирования должен быть меньше самого интервала.");
        }
        if (draft.scanPageLimit <= 0) {
            errors.add("Лимит страниц должен быть больше 0.");
        }
        if (draft.pageSwitchDelayMs <= 0) {
            errors.add("Задержка смены страниц должна быть больше 0.");
        }
        if (draft.pageSwitchDelayJitterMs < 0) {
            errors.add("Разброс задержки смены страниц не может быть отрицательным.");
        }
        if (draft.antiAfkActionIntervalSeconds <= 0) {
            errors.add("Интервал анти-AFK должен быть больше 0.");
        }
        if (draft.antiAfkJumpChancePercent < 0 || draft.antiAfkJumpChancePercent > 100) {
            errors.add("Шанс прыжка анти-AFK должен быть в диапазоне от 0 до 100.");
        }
        if (draft.marketResearchTargetMarginPercent < 0) {
            errors.add("Целевая маржа market research не может быть отрицательной.");
        }
        if (draft.marketResearchRiskBufferPercent < 0) {
            errors.add("Буфер риска market research не может быть отрицательным.");
        }

        for (int ruleIndex = 0; ruleIndex < draft.buyRules.size(); ruleIndex++) {
            AutobuyConfigDraft.BuyRuleDraft rule = draft.buyRules.get(ruleIndex);
            String label = "Правило " + (ruleIndex + 1);
            if (AutobuyUiTextSupport.isBlank(rule.id)) {
                errors.add(label + ": требуется id.");
            }
            if (AutobuyUiTextSupport.isBlank(rule.name)) {
                errors.add(label + ": требуется название.");
            }

            for (int conditionIndex = 0; conditionIndex < rule.conditions.size(); conditionIndex++) {
                AutobuyConfigDraft.ConditionDraft condition = rule.conditions.get(conditionIndex);
                String conditionLabel = label + ", условие " + (conditionIndex + 1) + " (" + AutobuyUiTextSupport.localizeConditionType(condition.type) + ")";
                switch (condition.type) {
                    case MINECRAFT_ID -> {
                        if (AutobuyUiTextSupport.isBlank(condition.stringValue)) {
                            errors.add(conditionLabel + ": требуется непустое значение.");
                        }
                    }
                    case MAX_TOTAL_PRICE, MAX_UNIT_PRICE -> {
                        if (condition.longValue == null || condition.longValue < 0) {
                            errors.add(conditionLabel + ": требуется неотрицательное число.");
                        }
                    }
                    case MIN_COUNT, MAX_COUNT -> {
                        if (condition.intValue == null || condition.intValue < 0) {
                            errors.add(conditionLabel + ": требуется неотрицательное число.");
                        }
                    }
                    case REQUIRED_ENCHANTMENTS -> {
                        for (AutobuyConfigDraft.RequiredEnchantmentDraft enchantment : condition.requiredEnchantments) {
                            if (AutobuyUiTextSupport.isBlank(enchantment.id)) {
                                errors.add(conditionLabel + ": найдено зачарование без id.");
                            }
                            if (enchantment.level != null && enchantment.level < 0) {
                                errors.add(conditionLabel + ": уровень зачарования не может быть отрицательным.");
                            }
                        }
                    }
                    case REQUIRED_POTION_EFFECTS -> {
                        for (AutobuyConfigDraft.RequiredPotionEffectDraft effect : condition.requiredPotionEffects) {
                            if (AutobuyUiTextSupport.isBlank(effect.id)) {
                                errors.add(conditionLabel + ": найден эффект без id.");
                            }
                            if (effect.level != null && effect.level < 0) {
                                errors.add(conditionLabel + ": уровень эффекта не может быть отрицательным.");
                            }
                            if (effect.durationSeconds != null && effect.durationSeconds < 0) {
                                errors.add(conditionLabel + ": длительность не может быть отрицательной.");
                            }
                        }
                    }
                    case SELLER_ALLOW_LIST, SELLER_DENY_LIST -> {
                        for (String entry : condition.stringList) {
                            if (AutobuyUiTextSupport.isBlank(entry)) {
                                errors.add(conditionLabel + ": найден пустой продавец.");
                            }
                        }
                    }
                }
            }
        }

        return AutobuyValidationResult.of(errors);
    }
}
