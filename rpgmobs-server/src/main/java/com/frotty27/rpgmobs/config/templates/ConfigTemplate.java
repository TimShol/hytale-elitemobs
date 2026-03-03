package com.frotty27.rpgmobs.config.templates;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ConfigOverlay;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigTemplate {

    private final String name;
    private final ConfigOverlay overlay;

    private ConfigTemplate(String name, ConfigOverlay overlay) {
        this.name = name;
        this.overlay = overlay;
    }

    public String getName() { return name; }

    public ConfigOverlay getOverlay() { return overlay; }

    private static final Map<String, ConfigTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("full", createDefault());
        TEMPLATES.put("empty", createEmpty());
    }

    public static Map<String, ConfigTemplate> getAll() {
        return TEMPLATES;
    }

    public static ConfigTemplate get(String key) {
        return TEMPLATES.get(key);
    }

    private static ConfigTemplate createDefault() {
        ConfigOverlay o = new ConfigOverlay();
        o.enabled = true;
        return new ConfigTemplate("Default", o);
    }

    private static ConfigTemplate createEmpty() {
        ConfigOverlay o = new ConfigOverlay();
        o.enabled = true;

        o.progressionStyle = "NONE";
        o.spawnChancePerTier = new double[]{0, 0, 0, 0, 0};

        o.enableHealthScaling = false;
        o.healthMultiplierPerTier = new float[]{0f, 0f, 0f, 0f, 0f};
        o.enableDamageScaling = false;
        o.damageMultiplierPerTier = new float[]{0f, 0f, 0f, 0f, 0f};
        o.healthRandomVariance = 0f;
        o.damageRandomVariance = 0f;

        o.abilityOverlays = new LinkedHashMap<>();
        for (String abilId : new String[]{"charge_leap", "heal_leap", "undead_summon"}) {
            ConfigOverlay.AbilityOverlay ao = new ConfigOverlay.AbilityOverlay();
            ao.enabled = false;
            o.abilityOverlays.put(abilId, ao);
        }

        o.vanillaDroplistExtraRollsPerTier = new int[]{0, 0, 0, 0, 0};
        o.dropWeaponChance = 0.0;
        o.dropArmorPieceChance = 0.0;
        o.dropOffhandItemChance = 0.0;
        o.droppedGearDurabilityMin = 0.0;
        o.droppedGearDurabilityMax = 0.0;

        o.mobRules = new LinkedHashMap<>();
        o.mobRuleCategoryTree = new RPGMobsConfig.MobRuleCategory("All", List.of());

        o.lootTemplates = new LinkedHashMap<>();
        o.lootTemplateCategoryTree = new RPGMobsConfig.LootTemplateCategory("All", List.of());

        o.enableNameplates = false;
        o.nameplateTierEnabled = new boolean[]{false, false, false, false, false};
        o.enableModelScaling = false;
        o.modelScalePerTier = new float[]{1f, 1f, 1f, 1f, 1f};
        o.modelScaleVariance = 0f;

        o.eliteFriendlyFireDisabled = false;
        o.eliteFallDamageDisabled = false;
        o.eliteNoAggroOnElite = false;

        o.distancePerTier = 0.0;
        o.distanceBonusInterval = 0.0;
        o.distanceHealthBonusPerInterval = 0f;
        o.distanceDamageBonusPerInterval = 0f;
        o.distanceHealthBonusCap = 0f;
        o.distanceDamageBonusCap = 0f;

        o.rpgLevelingEnabled = false;
        o.xpMultiplierPerTier = new float[]{0f, 0f, 0f, 0f, 0f};
        o.xpBonusPerAbility = 0.0;
        o.minionXPMultiplier = 0.0;

        return new ConfigTemplate("Empty", o);
    }
}
