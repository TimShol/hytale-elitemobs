package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.MultiSlashMediumComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Random;

public final class RPGMobsMultiSlashMediumFeature
        extends AbstractMultiSlashFeature<MultiSlashMediumComponent> {

    @Override
    public String id() {
        return AbilityIds.MULTI_SLASH_MEDIUM;
    }

    @Override
    public String displayName() {
        return "Multi Slash: Medium";
    }

    @Override
    public String description() {
        return "Standard 2-4 hit weapon combos";
    }

    @Override
    protected int variationCount() {
        return 2;
    }

    @Override
    protected String rootKeyFor(String variant, int variationIdx) {
        return "root" + capitalize(variant) + "V" + (variationIdx + 1);
    }

    @Override
    protected String getWeaponVariant(MultiSlashMediumComponent component) {
        return component.weaponVariant;
    }

    @Override
    protected void setWeaponVariant(MultiSlashMediumComponent component, String variant) {
        component.weaponVariant = variant;
    }

    @Override
    protected float getTriggerChance(MultiSlashMediumComponent component) {
        return component.slashTriggerChance;
    }

    @Override
    protected void setTriggerChance(MultiSlashMediumComponent component, float chance) {
        component.slashTriggerChance = chance;
    }

    @Override
    protected ComponentType<EntityStore, MultiSlashMediumComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getMultiSlashMediumComponentType();
    }

    @Override
    protected MultiSlashMediumComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                      RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getMultiSlashMediumComponentType());
    }

    @Override
    protected MultiSlashMediumComponent createComponent(RPGMobsConfig.MultiSlashAbilityConfig abilityConfig,
                                                         int tierIndex, boolean enabled, Random random) {
        float triggerChance = tierIndex < abilityConfig.slashTriggerChancePerTier.length
                ? abilityConfig.slashTriggerChancePerTier[tierIndex] : 0f;

        var component = new MultiSlashMediumComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.slashTriggerChance = triggerChance;
        return component;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
