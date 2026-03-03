package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConfigOverlay {

    public @Nullable Boolean enabled = null;

    public @Nullable String progressionStyle = null;

    public double @Nullable [] spawnChancePerTier = null;

    public @Nullable Map<String, double[]> environmentTierRules = null;

    public @Nullable Double distancePerTier = null;
    public @Nullable Double distanceBonusInterval = null;
    public @Nullable Float distanceHealthBonusPerInterval = null;
    public @Nullable Float distanceDamageBonusPerInterval = null;
    public @Nullable Float distanceHealthBonusCap = null;
    public @Nullable Float distanceDamageBonusCap = null;

    public @Nullable Boolean enableHealthScaling = null;

    public float @Nullable [] healthMultiplierPerTier = null;

    public @Nullable Boolean enableDamageScaling = null;

    public float @Nullable [] damageMultiplierPerTier = null;

    public @Nullable Map<String, AbilityOverlay> abilityOverlays = null;

    public int @Nullable [] vanillaDroplistExtraRollsPerTier = null;
    public @Nullable Double dropWeaponChance = null;
    public @Nullable Double dropArmorPieceChance = null;
    public @Nullable Double dropOffhandItemChance = null;
    public @Nullable Double droppedGearDurabilityMin = null;
    public @Nullable Double droppedGearDurabilityMax = null;

    public @Nullable String defaultLootTemplate = null;

    public @Nullable Float healthRandomVariance = null;

    public @Nullable Float damageRandomVariance = null;

    public @Nullable Boolean eliteFriendlyFireDisabled = null;
    public @Nullable Boolean eliteFallDamageDisabled = null;

    public @Nullable Boolean eliteNoAggroOnElite = null;

    public @Nullable Boolean enableNameplates = null;

    public @Nullable String nameplateMode = null;

    public boolean @Nullable [] nameplateTierEnabled = null;

    public @Nullable String[] nameplatePrefixPerTier = null;

    public @Nullable Map<String, List<String>> tierPrefixesByFamily = null;

    public @Nullable Boolean enableModelScaling = null;

    public float @Nullable [] modelScalePerTier = null;

    public @Nullable Float modelScaleVariance = null;

    public @Nullable Boolean rpgLevelingEnabled = null;

    public float @Nullable [] xpMultiplierPerTier = null;

    public @Nullable Double xpBonusPerAbility = null;

    public @Nullable Double minionXPMultiplier = null;

    public @Nullable Map<String, TierOverride> tierOverrides = null;

    public @Nullable Map<String, RPGMobsConfig.MobRule> mobRules = null;

    public RPGMobsConfig.@Nullable MobRuleCategory mobRuleCategoryTree = null;

    public @Nullable Map<String, RPGMobsConfig.LootTemplate> lootTemplates = null;

    public RPGMobsConfig.@Nullable LootTemplateCategory lootTemplateCategoryTree = null;

    public @Nullable ConfigOverlay customPreset = null;

    public static final class AbilityLinkedEntry {
        public String key;
        public boolean[] enabledPerTier = {true, true, true, true, true};

        public AbilityLinkedEntry() {
            this.key = "";
        }

        public AbilityLinkedEntry(String key, boolean[] enabledPerTier) {
            this.key = key;
            this.enabledPerTier = enabledPerTier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbilityLinkedEntry that)) return false;
            return Objects.equals(key, that.key) && Arrays.equals(enabledPerTier, that.enabledPerTier);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(key) + Arrays.hashCode(enabledPerTier);
        }
    }

    public static final class AbilityOverlay {
        public @Nullable Boolean enabled = null;
        public @Nullable List<AbilityLinkedEntry> linkedEntries = null;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbilityOverlay that)) return false;
            return Objects.equals(enabled, that.enabled)
                    && Objects.equals(linkedEntries, that.linkedEntries);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(enabled);
            result = 31 * result + Objects.hashCode(linkedEntries);
            return result;
        }
    }

    public static final class TierOverride {
        public boolean[] allowedTiers = {true, true, true, true, true};

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TierOverride that)) return false;
            return Arrays.equals(allowedTiers, that.allowedTiers);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(allowedTiers);
        }
    }

    public static boolean effectivelyEquals(ConfigOverlay a, ConfigOverlay b, ResolvedConfig base) {

        if (!Objects.equals(eff(a.enabled, base.enabled), eff(b.enabled, base.enabled))) return false;

        if (!Objects.equals(eff(a.progressionStyle, base.progressionStyle.name()), eff(b.progressionStyle, base.progressionStyle.name()))) return false;
        if (!Arrays.equals(eff(a.spawnChancePerTier, base.spawnChancePerTier), eff(b.spawnChancePerTier, base.spawnChancePerTier))) return false;
        if (!envRuleMapsEqual(a.environmentTierRules, b.environmentTierRules)) return false;

        if (!Objects.equals(eff(a.distancePerTier, base.distancePerTier), eff(b.distancePerTier, base.distancePerTier))) return false;
        if (!Objects.equals(eff(a.distanceBonusInterval, base.distanceBonusInterval), eff(b.distanceBonusInterval, base.distanceBonusInterval))) return false;
        if (!Objects.equals(eff(a.distanceHealthBonusPerInterval, base.distanceHealthBonusPerInterval), eff(b.distanceHealthBonusPerInterval, base.distanceHealthBonusPerInterval))) return false;
        if (!Objects.equals(eff(a.distanceDamageBonusPerInterval, base.distanceDamageBonusPerInterval), eff(b.distanceDamageBonusPerInterval, base.distanceDamageBonusPerInterval))) return false;
        if (!Objects.equals(eff(a.distanceHealthBonusCap, base.distanceHealthBonusCap), eff(b.distanceHealthBonusCap, base.distanceHealthBonusCap))) return false;
        if (!Objects.equals(eff(a.distanceDamageBonusCap, base.distanceDamageBonusCap), eff(b.distanceDamageBonusCap, base.distanceDamageBonusCap))) return false;

        if (!Objects.equals(eff(a.enableHealthScaling, base.enableHealthScaling), eff(b.enableHealthScaling, base.enableHealthScaling))) return false;
        if (!Arrays.equals(eff(a.healthMultiplierPerTier, base.healthMultiplierPerTier), eff(b.healthMultiplierPerTier, base.healthMultiplierPerTier))) return false;
        if (!Objects.equals(eff(a.enableDamageScaling, base.enableDamageScaling), eff(b.enableDamageScaling, base.enableDamageScaling))) return false;
        if (!Arrays.equals(eff(a.damageMultiplierPerTier, base.damageMultiplierPerTier), eff(b.damageMultiplierPerTier, base.damageMultiplierPerTier))) return false;
        if (!Objects.equals(eff(a.healthRandomVariance, base.healthRandomVariance), eff(b.healthRandomVariance, base.healthRandomVariance))) return false;
        if (!Objects.equals(eff(a.damageRandomVariance, base.damageRandomVariance), eff(b.damageRandomVariance, base.damageRandomVariance))) return false;

        if (!Objects.equals(a.abilityOverlays, b.abilityOverlays)) return false;

        if (!Arrays.equals(eff(a.vanillaDroplistExtraRollsPerTier, base.vanillaDroplistExtraRollsPerTier), eff(b.vanillaDroplistExtraRollsPerTier, base.vanillaDroplistExtraRollsPerTier))) return false;
        if (!Objects.equals(eff(a.dropWeaponChance, base.dropWeaponChance), eff(b.dropWeaponChance, base.dropWeaponChance))) return false;
        if (!Objects.equals(eff(a.dropArmorPieceChance, base.dropArmorPieceChance), eff(b.dropArmorPieceChance, base.dropArmorPieceChance))) return false;
        if (!Objects.equals(eff(a.dropOffhandItemChance, base.dropOffhandItemChance), eff(b.dropOffhandItemChance, base.dropOffhandItemChance))) return false;
        if (!Objects.equals(eff(a.droppedGearDurabilityMin, base.droppedGearDurabilityMin), eff(b.droppedGearDurabilityMin, base.droppedGearDurabilityMin))) return false;
        if (!Objects.equals(eff(a.droppedGearDurabilityMax, base.droppedGearDurabilityMax), eff(b.droppedGearDurabilityMax, base.droppedGearDurabilityMax))) return false;
        if (!Objects.equals(eff(a.defaultLootTemplate, base.defaultLootTemplate), eff(b.defaultLootTemplate, base.defaultLootTemplate))) return false;

        if (!Objects.equals(eff(a.eliteFriendlyFireDisabled, base.eliteFriendlyFireDisabled), eff(b.eliteFriendlyFireDisabled, base.eliteFriendlyFireDisabled))) return false;
        if (!Objects.equals(eff(a.eliteFallDamageDisabled, base.eliteFallDamageDisabled), eff(b.eliteFallDamageDisabled, base.eliteFallDamageDisabled))) return false;
        if (!Objects.equals(eff(a.eliteNoAggroOnElite, base.eliteNoAggroOnElite), eff(b.eliteNoAggroOnElite, base.eliteNoAggroOnElite))) return false;

        if (!Objects.equals(eff(a.enableNameplates, base.enableNameplates), eff(b.enableNameplates, base.enableNameplates))) return false;
        if (!Objects.equals(eff(a.nameplateMode, base.nameplateMode), eff(b.nameplateMode, base.nameplateMode))) return false;
        if (!Arrays.equals(eff(a.nameplateTierEnabled, base.nameplateTierEnabled), eff(b.nameplateTierEnabled, base.nameplateTierEnabled))) return false;
        if (!Arrays.equals(eff(a.nameplatePrefixPerTier, base.nameplatePrefixPerTier), eff(b.nameplatePrefixPerTier, base.nameplatePrefixPerTier))) return false;
        if (!Objects.equals(a.tierPrefixesByFamily, b.tierPrefixesByFamily)) return false;

        if (!Objects.equals(eff(a.enableModelScaling, base.enableModelScaling), eff(b.enableModelScaling, base.enableModelScaling))) return false;
        if (!Arrays.equals(eff(a.modelScalePerTier, base.modelScalePerTier), eff(b.modelScalePerTier, base.modelScalePerTier))) return false;
        if (!Objects.equals(eff(a.modelScaleVariance, base.modelScaleVariance), eff(b.modelScaleVariance, base.modelScaleVariance))) return false;

        if (!Objects.equals(eff(a.rpgLevelingEnabled, base.rpgLevelingEnabled), eff(b.rpgLevelingEnabled, base.rpgLevelingEnabled))) return false;
        if (!Arrays.equals(eff(a.xpMultiplierPerTier, base.xpMultiplierPerTier), eff(b.xpMultiplierPerTier, base.xpMultiplierPerTier))) return false;
        if (!Objects.equals(eff(a.xpBonusPerAbility, base.xpBonusPerAbility), eff(b.xpBonusPerAbility, base.xpBonusPerAbility))) return false;
        if (!Objects.equals(eff(a.minionXPMultiplier, base.minionXPMultiplier), eff(b.minionXPMultiplier, base.minionXPMultiplier))) return false;

        if (!Objects.equals(a.tierOverrides, b.tierOverrides)) return false;

        if (!Objects.equals(a.mobRules, b.mobRules)) return false;
        if (!Objects.equals(a.mobRuleCategoryTree, b.mobRuleCategoryTree)) return false;

        if (!Objects.equals(a.lootTemplates, b.lootTemplates)) return false;
        return Objects.equals(a.lootTemplateCategoryTree, b.lootTemplateCategoryTree);
    }

    private static <T> T eff(@Nullable T overlay, T base) { return overlay != null ? overlay : base; }
    private static double[] eff(double @Nullable [] overlay, double[] base) { return overlay != null ? overlay : base; }
    private static float[] eff(float @Nullable [] overlay, float[] base) { return overlay != null ? overlay : base; }
    private static boolean[] eff(boolean @Nullable [] overlay, boolean[] base) { return overlay != null ? overlay : base; }
    private static int[] eff(int @Nullable [] overlay, int[] base) { return overlay != null ? overlay : base; }
    private static String[] eff(@Nullable String[] overlay, String[] base) { return overlay != null ? overlay : base; }

    private static boolean envRuleMapsEqual(@Nullable Map<String, double[]> a, @Nullable Map<String, double[]> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (Map.Entry<String, double[]> entry : a.entrySet()) {
            double[] bVal = b.get(entry.getKey());
            if (!Arrays.equals(entry.getValue(), bVal)) return false;
        }
        return true;
    }

}
