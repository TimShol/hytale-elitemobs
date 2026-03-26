package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public record TriggerContext(
        RPGMobsPlugin plugin,
        Ref<EntityStore> entityRef,
        Store<EntityStore> store,
        RPGMobsConfig config,
        @Nullable ResolvedConfig resolved,
        int tierIndex,
        AbilityTriggerSource source,
        @Nullable Ref<EntityStore> targetRef
) {}
