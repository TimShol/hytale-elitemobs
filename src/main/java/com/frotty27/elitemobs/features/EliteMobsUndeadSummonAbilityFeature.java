package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.EliteMobsUndeadSummonAbilitySystem;

public final class EliteMobsUndeadSummonAbilityFeature implements EliteMobsAbilityFeature {

    public static final String ABILITY_UNDEAD_SUMMON = "undead_summon";

    @Override
    public String getFeatureKey() {
        return "UndeadSummon";
    }

    @Override
    public String id() {
        return ABILITY_UNDEAD_SUMMON;
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new EliteMobsUndeadSummonAbilitySystem(plugin));
    }
}