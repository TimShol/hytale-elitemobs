package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResolvedConfig {

    public boolean enabled = true;

    public RPGMobsConfig.ProgressionStyle progressionStyle = RPGMobsConfig.ProgressionStyle.ENVIRONMENT;
    public double[] spawnChancePerTier = {0.46, 0.28, 0.16, 0.08, 0.04};

    public boolean hasCustomSpawnChances = false;

    public Map<String, double[]> environmentTierRules = new LinkedHashMap<>();

    public double distancePerTier = 1000.0;
    public double distanceBonusInterval = 100.0;
    public float distanceHealthBonusPerInterval = 0.01f;
    public float distanceDamageBonusPerInterval = 0.005f;
    public float distanceHealthBonusCap = 0.5f;
    public float distanceDamageBonusCap = 0.5f;

    public boolean enableHealthScaling = true;
    public float[] healthMultiplierPerTier = {0.3f, 0.6f, 1.2f, 1.8f, 2.6f};
    public boolean enableDamageScaling = true;
    public float[] damageMultiplierPerTier = {0.6f, 1.1f, 1.6f, 2.1f, 2.6f};

    public float healthRandomVariance = 0.05f;
    public float damageRandomVariance = 0.05f;

    public Map<String, ResolvedAbilityConfig> resolvedAbilities = new LinkedHashMap<>();

    public static final class ResolvedAbilityConfig {
        public boolean enabled = true;
        public Map<String, boolean[]> linkedMobEntries = new LinkedHashMap<>();
    }

    public int[] vanillaDroplistExtraRollsPerTier = {0, 0, 2, 4, 6};
    public double dropWeaponChance = 0.05;
    public double dropArmorPieceChance = 0.05;
    public double dropOffhandItemChance = 0.05;
    public double droppedGearDurabilityMin = 0.3;
    public double droppedGearDurabilityMax = 0.8;
    public String defaultLootTemplate = "";

    public boolean eliteFriendlyFireDisabled = false;
    public boolean eliteFallDamageDisabled = false;

    public boolean eliteNoAggroOnElite = false;

    public boolean enableNameplates = true;
    public String nameplateMode = "RANKED_ROLE";
    public boolean[] nameplateTierEnabled = {true, true, true, true, true};
    public String[] nameplatePrefixPerTier = {"[•]", "[• •]", "[• • •]", "[• • • •]", "[• • • • •]"};
    public Map<String, List<String>> tierPrefixesByFamily = new LinkedHashMap<>();

    public boolean enableModelScaling = true;
    public float[] modelScalePerTier = {0.74f, 0.85f, 0.96f, 1.07f, 1.18f};
    public float modelScaleVariance = 0.04f;

    public boolean rpgLevelingEnabled = true;
    public float[] xpMultiplierPerTier = {1.0f, 1.5f, 2.0f, 3.0f, 5.0f};
    public double xpBonusPerAbility = 1000.0;
    public double minionXPMultiplier = 0.05;

    public Map<String, ConfigOverlay.TierOverride> tierOverrides = new LinkedHashMap<>();

    public Map<String, RPGMobsConfig.MobRule> mobRules = new LinkedHashMap<>();
    public RPGMobsConfig.MobRuleCategory mobRuleCategoryTree = new RPGMobsConfig.MobRuleCategory();

    public RPGMobsConfig.GearCategory weaponCategoryTree = new RPGMobsConfig.GearCategory();
    public RPGMobsConfig.GearCategory armorCategoryTree = new RPGMobsConfig.GearCategory();

    public Map<String, RPGMobsConfig.LootTemplate> lootTemplates = new LinkedHashMap<>();
    public RPGMobsConfig.LootTemplateCategory lootTemplateCategoryTree = new RPGMobsConfig.LootTemplateCategory();
}
