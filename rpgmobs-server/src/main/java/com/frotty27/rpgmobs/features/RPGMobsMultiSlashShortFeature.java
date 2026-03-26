package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.MultiSlashShortComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Random;

public final class RPGMobsMultiSlashShortFeature
        extends AbstractMultiSlashFeature<MultiSlashShortComponent> {

    @Override
    public String id() {
        return AbilityIds.MULTI_SLASH_SHORT;
    }

    @Override
    public String displayName() {
        return "Multi Slash: Short";
    }

    @Override
    public String description() {
        return "Quick 1-2 hit weapon strikes";
    }

    @Override
    protected int variationCount() {
        return 3;
    }

    @Override
    protected String rootKeyFor(String variant, int variationIdx) {
        return "root" + capitalize(variant) + "V" + (variationIdx + 1);
    }

    @Override
    protected String getWeaponVariant(MultiSlashShortComponent component) {
        return component.weaponVariant;
    }

    @Override
    protected void setWeaponVariant(MultiSlashShortComponent component, String variant) {
        component.weaponVariant = variant;
    }

    @Override
    protected float getTriggerChance(MultiSlashShortComponent component) {
        return component.slashTriggerChance;
    }

    @Override
    protected void setTriggerChance(MultiSlashShortComponent component, float chance) {
        component.slashTriggerChance = chance;
    }

    @Override
    protected ComponentType<EntityStore, MultiSlashShortComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getMultiSlashShortComponentType();
    }

    @Override
    protected MultiSlashShortComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                     RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getMultiSlashShortComponentType());
    }

    @Override
    protected MultiSlashShortComponent createComponent(RPGMobsConfig.MultiSlashAbilityConfig abilityConfig,
                                                        int tierIndex, boolean enabled, Random random) {
        float triggerChance = tierIndex < abilityConfig.slashTriggerChancePerTier.length
                ? abilityConfig.slashTriggerChancePerTier[tierIndex] : 0f;

        var component = new MultiSlashShortComponent();
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
