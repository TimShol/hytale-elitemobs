package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public interface IRPGMobsAbilityFeature extends IRPGMobsFeature {
    String id();

    @Override
    default String getFeatureKey() {
        return snakeToPascal(id());
    }

    private static String snakeToPascal(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        for (String part : snakeCase.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    default String getAssetId() {
        return id();
    }

    @Override
    default Object getConfig(RPGMobsConfig config) {
        return config.abilitiesConfig.defaultAbilities.get(id());
    }

    @Override
    default void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                       Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                       RPGMobsTierComponent tierComponent, @Nullable String roleName) {

    }
}
