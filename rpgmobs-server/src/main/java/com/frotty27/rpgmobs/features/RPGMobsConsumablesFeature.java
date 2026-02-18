package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsConsumablesFeature implements IRPGMobsFeature {

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
    public Object getConfig(RPGMobsConfig config) {
        return config.consumablesConfig.defaultConsumables;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
    }
}
