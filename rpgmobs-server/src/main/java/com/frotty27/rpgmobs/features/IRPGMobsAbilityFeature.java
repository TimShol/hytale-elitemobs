package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface IRPGMobsAbilityFeature extends IRPGMobsFeature {
    String id();

    default String displayName() {
        return id();
    }

    default RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.AbilityConfig();
    }

    default List<AbilityConfigField> describeConfigFields() {
        return List.of();
    }

    @Override
    default String getFeatureKey() {
        return snakeToPascal(id());
    }

    private static String snakeToPascal(String snakeCase) {
        var builder = new StringBuilder();
        for (String part : snakeCase.split("_")) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)));
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
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
    default void apply(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                       Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                       CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                       @Nullable String roleName) {
    }
}
