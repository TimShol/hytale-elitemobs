package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;

import java.util.*;

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
    public @Nullable Float healthRandomVariance = null;
    public @Nullable Boolean enableDamageScaling = null;
    public float @Nullable [] damageMultiplierPerTier = null;
    public @Nullable Float damageRandomVariance = null;

    public @Nullable Map<String, AbilityOverlay> abilityOverlays = null;
    public @Nullable Float globalCooldownMinSeconds = null;
    public @Nullable Float globalCooldownMaxSeconds = null;

    public int @Nullable [] vanillaDroplistExtraRollsPerTier = null;
    public @Nullable Double dropWeaponChance = null;
    public @Nullable Double dropArmorPieceChance = null;
    public @Nullable Double dropOffhandItemChance = null;
    public @Nullable Double droppedGearDurabilityMin = null;
    public @Nullable Double droppedGearDurabilityMax = null;
    public @Nullable String defaultLootTemplate = null;
    public @Nullable Map<String, RPGMobsConfig.LootTemplate> lootTemplates = null;
    public RPGMobsConfig.@Nullable LootTemplateCategory lootTemplateCategoryTree = null;

    public @Nullable Boolean eliteFallDamageDisabled = null;

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
    public @Nullable Set<String> disabledMobRuleKeys = null;

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

        if (!OverlayFieldRegistry.allEffectivelyEqual(a, b, base)) return false;

        if (!Objects.equals(effectiveValue(a.progressionStyle, base.progressionStyle.name()), effectiveValue(b.progressionStyle, base.progressionStyle.name()))) return false;
        if (!Arrays.equals(effectiveValue(a.spawnChancePerTier, base.spawnChancePerTier), effectiveValue(b.spawnChancePerTier, base.spawnChancePerTier))) return false;
        if (!envRuleMapsEqual(a.environmentTierRules, b.environmentTierRules)) return false;

        if (!Objects.equals(a.abilityOverlays, b.abilityOverlays)) return false;

        if (!Objects.equals(a.tierPrefixesByFamily, b.tierPrefixesByFamily)) return false;

        if (!Objects.equals(a.tierOverrides, b.tierOverrides)) return false;

        if (!Objects.equals(a.disabledMobRuleKeys, b.disabledMobRuleKeys)) return false;

        if (!Objects.equals(a.lootTemplates, b.lootTemplates)) return false;
        return Objects.equals(a.lootTemplateCategoryTree, b.lootTemplateCategoryTree);
    }

    private static <T> T effectiveValue(@Nullable T overlay, T base) { return overlay != null ? overlay : base; }
    private static double[] effectiveValue(double @Nullable [] overlay, double[] base) { return overlay != null ? overlay : base; }
    private static float[] effectiveValue(float @Nullable [] overlay, float[] base) { return overlay != null ? overlay : base; }
    private static boolean[] effectiveValue(boolean @Nullable [] overlay, boolean[] base) { return overlay != null ? overlay : base; }
    private static int[] effectiveValue(int @Nullable [] overlay, int[] base) { return overlay != null ? overlay : base; }
    private static String[] effectiveValue(@Nullable String[] overlay, String[] base) { return overlay != null ? overlay : base; }

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
