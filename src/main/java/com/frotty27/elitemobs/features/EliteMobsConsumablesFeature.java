package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsConsumablesFeature implements EliteMobsFeature {

    public static final String CONSUMABLE_FOOD_TIER1 = "food_tier1";
    public static final String CONSUMABLE_FOOD_TIER2 = "food_tier2";
    public static final String CONSUMABLE_FOOD_TIER3 = "food_tier3";
    public static final String CONSUMABLE_SMALL_POTIONS = "small_potions";
    public static final String CONSUMABLE_BIG_POTIONS = "big_potions";

    @Override
    public String getFeatureKey() {
        return "Consumables";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.consumablesConfig.defaultConsumables;
    }

    @Override
    public void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {}
}
