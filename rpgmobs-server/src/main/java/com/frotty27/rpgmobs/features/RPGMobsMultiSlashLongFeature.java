package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.MultiSlashLongComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Random;

public final class RPGMobsMultiSlashLongFeature
        extends AbstractMultiSlashFeature<MultiSlashLongComponent> {

    @Override
    public String id() {
        return AbilityIds.MULTI_SLASH_LONG;
    }

    @Override
    public String displayName() {
        return "Multi Slash: Long";
    }

    @Override
    public String description() {
        return "Full 4-6 hit weapon combos with finisher";
    }

    @Override
    protected int variationCount() {
        return 1;
    }

    @Override
    protected String rootKeyFor(String variant, int variationIdx) {
        return "root" + capitalize(variant);
    }

    @Override
    protected String getWeaponVariant(MultiSlashLongComponent component) {
        return component.weaponVariant;
    }

    @Override
    protected void setWeaponVariant(MultiSlashLongComponent component, String variant) {
        component.weaponVariant = variant;
    }

    @Override
    protected float getTriggerChance(MultiSlashLongComponent component) {
        return component.slashTriggerChance;
    }

    @Override
    protected void setTriggerChance(MultiSlashLongComponent component, float chance) {
        component.slashTriggerChance = chance;
    }

    @Override
    protected ComponentType<EntityStore, MultiSlashLongComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getMultiSlashLongComponentType();
    }

    @Override
    protected MultiSlashLongComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                    RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getMultiSlashLongComponentType());
    }

    @Override
    protected MultiSlashLongComponent createComponent(RPGMobsConfig.MultiSlashAbilityConfig abilityConfig,
                                                       int tierIndex, boolean enabled, Random random) {
        float triggerChance = tierIndex < abilityConfig.slashTriggerChancePerTier.length
                ? abilityConfig.slashTriggerChancePerTier[tierIndex] : 0f;

        var component = new MultiSlashLongComponent();
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
