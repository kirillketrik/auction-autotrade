package ru.geroldina.ftauctionbot.client.infrastructure.ui.autobuy;

import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.BuyRuleCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.ItemIdCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxCountCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxTotalPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MaxUnitPriceCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.MinCountCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredEnchantmentsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.RequiredPotionEffectsCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.SellerAllowListCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.condition.SellerDenyListCondition;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyConfig;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.AutobuyScanLogMode;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.BuyRule;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredEnchantment;
import ru.geroldina.ftauctionbot.client.domain.autobuy.model.RequiredPotionEffect;

import java.util.ArrayList;
import java.util.List;

final class AutobuyConfigDraft {
    int scanIntervalSeconds;
    int scanIntervalJitterSeconds;
    int scanPageLimit;
    int pageSwitchDelayMs;
    int pageSwitchDelayJitterMs;
    AutobuyScanLogMode scanLogMode;
    boolean antiAfkEnabled;
    int antiAfkActionIntervalSeconds;
    int antiAfkJumpChancePercent;
    int marketResearchTargetMarginPercent;
    int marketResearchRiskBufferPercent;
    final List<BuyRuleDraft> buyRules = new ArrayList<>();

    static AutobuyConfigDraft fromDomain(AutobuyConfig config) {
        AutobuyConfigDraft draft = new AutobuyConfigDraft();
        draft.scanIntervalSeconds = config.scanIntervalSeconds();
        draft.scanIntervalJitterSeconds = config.scanIntervalJitterSeconds();
        draft.scanPageLimit = config.scanPageLimit();
        draft.pageSwitchDelayMs = config.pageSwitchDelayMs();
        draft.pageSwitchDelayJitterMs = config.pageSwitchDelayJitterMs();
        draft.scanLogMode = config.scanLogMode();
        draft.antiAfkEnabled = config.antiAfkEnabled();
        draft.antiAfkActionIntervalSeconds = config.antiAfkActionIntervalSeconds();
        draft.antiAfkJumpChancePercent = config.antiAfkJumpChancePercent();
        draft.marketResearchTargetMarginPercent = config.marketResearchTargetMarginPercent();
        draft.marketResearchRiskBufferPercent = config.marketResearchRiskBufferPercent();
        for (BuyRule rule : config.buyRules()) {
            draft.buyRules.add(BuyRuleDraft.fromDomain(rule));
        }
        return draft;
    }

    AutobuyConfig toDomain() {
        List<BuyRule> rules = new ArrayList<>();
        for (BuyRuleDraft rule : buyRules) {
            rules.add(rule.toDomain());
        }
        return new AutobuyConfig(
            scanIntervalSeconds,
            scanIntervalJitterSeconds,
            scanPageLimit,
            pageSwitchDelayMs,
            pageSwitchDelayJitterMs,
            scanLogMode,
            antiAfkEnabled,
            antiAfkActionIntervalSeconds,
            antiAfkJumpChancePercent,
            marketResearchTargetMarginPercent,
            marketResearchRiskBufferPercent,
            rules
        );
    }

    static final class BuyRuleDraft {
        String id;
        String name;
        boolean enabled = true;
        final List<ConditionDraft> conditions = new ArrayList<>();

        static BuyRuleDraft fromDomain(BuyRule rule) {
            BuyRuleDraft draft = new BuyRuleDraft();
            draft.id = rule.id();
            draft.name = rule.name();
            draft.enabled = rule.enabled();
            for (BuyRuleCondition condition : rule.conditions()) {
                draft.conditions.add(ConditionDraft.fromDomain(condition));
            }
            return draft;
        }

        BuyRule toDomain() {
            List<BuyRuleCondition> mappedConditions = new ArrayList<>();
            for (ConditionDraft condition : conditions) {
                mappedConditions.add(condition.toDomain());
            }
            return new BuyRule(id, name, enabled, mappedConditions);
        }
    }

    enum ConditionType {
        MINECRAFT_ID("minecraft_id"),
        MAX_TOTAL_PRICE("max_total_price"),
        MAX_UNIT_PRICE("max_unit_price"),
        MIN_COUNT("min_count"),
        MAX_COUNT("max_count"),
        REQUIRED_ENCHANTMENTS("required_enchantments"),
        REQUIRED_POTION_EFFECTS("required_potion_effects"),
        SELLER_ALLOW_LIST("seller_allow_list"),
        SELLER_DENY_LIST("seller_deny_list");

        final String jsonType;

        ConditionType(String jsonType) {
            this.jsonType = jsonType;
        }
    }

    static final class ConditionDraft {
        ConditionType type;
        String stringValue = "";
        Long longValue;
        Integer intValue;
        final List<String> stringList = new ArrayList<>();
        final List<RequiredEnchantmentDraft> requiredEnchantments = new ArrayList<>();
        final List<RequiredPotionEffectDraft> requiredPotionEffects = new ArrayList<>();

        static ConditionDraft create(ConditionType type) {
            ConditionDraft draft = new ConditionDraft();
            draft.type = type;
            return draft;
        }

        static ConditionDraft fromDomain(BuyRuleCondition condition) {
            ConditionDraft draft = new ConditionDraft();
            switch (condition) {
                case ItemIdCondition value -> {
                    draft.type = ConditionType.MINECRAFT_ID;
                    draft.stringValue = defaultString(value.minecraftId());
                }
                case MaxTotalPriceCondition value -> {
                    draft.type = ConditionType.MAX_TOTAL_PRICE;
                    draft.longValue = value.value();
                }
                case MaxUnitPriceCondition value -> {
                    draft.type = ConditionType.MAX_UNIT_PRICE;
                    draft.longValue = value.value();
                }
                case MinCountCondition value -> {
                    draft.type = ConditionType.MIN_COUNT;
                    draft.intValue = value.value();
                }
                case MaxCountCondition value -> {
                    draft.type = ConditionType.MAX_COUNT;
                    draft.intValue = value.value();
                }
                case RequiredEnchantmentsCondition value -> {
                    draft.type = ConditionType.REQUIRED_ENCHANTMENTS;
                    for (RequiredEnchantment enchantment : value.value()) {
                        draft.requiredEnchantments.add(new RequiredEnchantmentDraft(
                            defaultString(enchantment.id()),
                            enchantment.minLevel()
                        ));
                    }
                }
                case RequiredPotionEffectsCondition value -> {
                    draft.type = ConditionType.REQUIRED_POTION_EFFECTS;
                    for (RequiredPotionEffect effect : value.value()) {
                        draft.requiredPotionEffects.add(new RequiredPotionEffectDraft(
                            defaultString(effect.id()),
                            effect.level(),
                            effect.minDurationSeconds()
                        ));
                    }
                }
                case SellerAllowListCondition value -> {
                    draft.type = ConditionType.SELLER_ALLOW_LIST;
                    draft.stringList.addAll(value.value());
                }
                case SellerDenyListCondition value -> {
                    draft.type = ConditionType.SELLER_DENY_LIST;
                    draft.stringList.addAll(value.value());
                }
                default -> throw new IllegalArgumentException("Unsupported condition type: " + condition.getClass().getName());
            }
            return draft;
        }

        BuyRuleCondition toDomain() {
            return switch (type) {
                case MINECRAFT_ID -> new ItemIdCondition(blankToNull(stringValue));
                case MAX_TOTAL_PRICE -> new MaxTotalPriceCondition(longValue);
                case MAX_UNIT_PRICE -> new MaxUnitPriceCondition(longValue);
                case MIN_COUNT -> new MinCountCondition(intValue);
                case MAX_COUNT -> new MaxCountCondition(intValue);
                case REQUIRED_ENCHANTMENTS -> new RequiredEnchantmentsCondition(mapEnchantments());
                case REQUIRED_POTION_EFFECTS -> new RequiredPotionEffectsCondition(mapPotionEffects());
                case SELLER_ALLOW_LIST -> new SellerAllowListCondition(new ArrayList<>(stringList));
                case SELLER_DENY_LIST -> new SellerDenyListCondition(new ArrayList<>(stringList));
            };
        }

        private List<RequiredEnchantment> mapEnchantments() {
            List<RequiredEnchantment> result = new ArrayList<>();
            for (RequiredEnchantmentDraft enchantment : requiredEnchantments) {
                result.add(new RequiredEnchantment(blankToNull(enchantment.id), enchantment.level));
            }
            return result;
        }

        private List<RequiredPotionEffect> mapPotionEffects() {
            List<RequiredPotionEffect> result = new ArrayList<>();
            for (RequiredPotionEffectDraft effect : requiredPotionEffects) {
                result.add(new RequiredPotionEffect(blankToNull(effect.id), effect.level, effect.durationSeconds));
            }
            return result;
        }
    }

    static final class RequiredEnchantmentDraft {
        String id;
        Integer level;

        RequiredEnchantmentDraft(String id, Integer level) {
            this.id = id;
            this.level = level;
        }
    }

    static final class RequiredPotionEffectDraft {
        String id;
        Integer level;
        Integer durationSeconds;

        RequiredPotionEffectDraft(String id, Integer level, Integer durationSeconds) {
            this.id = id;
            this.level = level;
            this.durationSeconds = durationSeconds;
        }
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
