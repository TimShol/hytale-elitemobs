package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.EliteMobsHealLeapAbilitySystem;

public final class EliteMobsHealLeapAbilityFeature implements EliteMobsAbilityFeature {

    public static final String ABILITY_HEAL_LEAP = "heal_leap";

    @Override
    public String getFeatureKey() {
        return "HealLeap";
    }

    @Override
    public String id() {
        return ABILITY_HEAL_LEAP;
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new EliteMobsHealLeapAbilitySystem(plugin));
    }
}
