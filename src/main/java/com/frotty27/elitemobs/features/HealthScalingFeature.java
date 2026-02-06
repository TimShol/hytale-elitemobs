package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

public final class HealthScalingFeature implements EliteMobsFeature {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HEALTH_MODIFIER_KEY = "EliteMobs.HealthMult";
    private static final float HEALTH_MAX_EPSILON = 0.05f;

    @Override
    public String id() {
        return "health_scaling";
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
    ) {
        if (!config.health.enableHealthScaling) return;
        if (config.health.healthMultiplierPerTier == null || config.health.healthMultiplierPerTier.length <= tierComponent.tierIndex) return;

        float healthMultiplier = Math.max(0.01f, config.health.healthMultiplierPerTier[tierComponent.tierIndex]);

        EntityStatMap entityStats = entityStore.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();

        var baseHealthStatValue = entityStats.get(healthStatId);
        if (baseHealthStatValue != null) tierComponent.baseHealthMax = Math.max(0.0f, baseHealthStatValue.getMax());

        if (config.debug.isDebugModeEnabled) {
            var before = entityStats.get(healthStatId);
            EliteMobsLogger.debug(
                    LOGGER,
                    "HP BEFORE tier=%d role=%s cur=%.2f max=%.2f mult=%.3f",
                    EliteMobsLogLevel.INFO,
                    tierComponent.tierIndex,
                    roleName,
                    before == null ? -1f : before.get(),
                    before == null ? -1f : before.getMax(),
                    healthMultiplier
            );
        }

        entityStats.putModifier(
                healthStatId,
                HEALTH_MODIFIER_KEY,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        healthMultiplier
                )
        );

        entityStats.update();
        entityStats.maximizeStatValue(healthStatId);
        entityStats.update();

        var boostedHealthStatValue = entityStats.get(healthStatId);
        if (boostedHealthStatValue != null) tierComponent.expectedHealthMax = Math.max(0.0f, boostedHealthStatValue.getMax());

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

        if (config.debug.isDebugModeEnabled) {
            var after = entityStats.get(healthStatId);
            EliteMobsLogger.debug(
                    LOGGER,
                    "HP AFTER  tier=%d role=%s cur=%.2f max=%.2f",
                    EliteMobsLogLevel.INFO,
                    tierComponent.tierIndex,
                    roleName,
                    after == null ? -1f : after.get(),
                    after == null ? -1f : after.getMax()
            );
        }

        tierComponent.healthApplied = true;
        tierComponent.appliedHealthMult = healthMultiplier;
        tierComponent.healthFinalized = false;
        tierComponent.healthFinalizeTries = 0;
        tierComponent.needsHealthResync = false;
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new HealthScalingSystem(plugin));
    }
}
