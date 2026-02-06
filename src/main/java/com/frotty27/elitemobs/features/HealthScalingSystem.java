package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

public class HealthScalingSystem extends EntityTickingSystem<EntityStore> {

    private static final String HEALTH_MODIFIER_KEY = "EliteMobs.HealthMult";
    private static final float HEALTH_MAX_EPSILON = 0.05f;
    private static final int HEALTH_FINALIZE_MAX_TRIES = 5;
    private static final boolean FORCE_MAX_HEALTH_ON_SPAWN = true;

    private final EliteMobsPlugin plugin;

    public HealthScalingSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), plugin.getEliteMobsComponent());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null) return;

        EliteMobsTierComponent tierComponent = chunk.getComponent(entityIndex, plugin.getEliteMobsComponent());
        if (tierComponent == null) return;

        if (config.health.enableHealthScaling && tierComponent.healthApplied && tierComponent.appliedHealthMult > 0.0001f) {
            repairHealthScalingIfNeeded(plugin, config, chunk.getReferenceTo(entityIndex), store, commandBuffer, tierComponent);
        }
    }

    private void repairHealthScalingIfNeeded(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent
    ) {
        EntityStatMap entityStats = entityStore.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        int maxFinalizeTries = Math.max(1, HEALTH_FINALIZE_MAX_TRIES);

        boolean allowTopOff =
                FORCE_MAX_HEALTH_ON_SPAWN
                        && !tierComponent.needsHealthResync
                        && !tierComponent.healthFinalized
                        && tierComponent.healthFinalizeTries < maxFinalizeTries;

        float expectedMax =
                tierComponent.expectedHealthMax > 0.0001f
                        ? tierComponent.expectedHealthMax
                        : (tierComponent.baseHealthMax > 0.0001f ? (tierComponent.baseHealthMax * tierComponent.appliedHealthMult) : -1f);

        float actualMax = healthStatValue.getMax();
        boolean changed = false;

        boolean maxMismatch =
                (expectedMax > 0.0001f) && (Math.abs(actualMax - expectedMax) > HEALTH_MAX_EPSILON);

        if (expectedMax <= 0.0001f || maxMismatch) {
            applyOrRepairHealthScaling(
                    npcRef,
                    commandBuffer,
                    entityStats,
                    healthStatId,
                    tierComponent.appliedHealthMult,
                    allowTopOff
            );

            var refreshedHealthStatValue = entityStats.get(healthStatId);
            if (refreshedHealthStatValue != null) tierComponent.expectedHealthMax = refreshedHealthStatValue.getMax();

            changed = true;
        }

        if (allowTopOff) {
            var refreshedHealthStatValue = entityStats.get(healthStatId);
            if (refreshedHealthStatValue != null) {
                float currentHealth = refreshedHealthStatValue.get();
                float maxHealth = refreshedHealthStatValue.getMax();

                if (maxHealth > 0.0001f && currentHealth < maxHealth - HEALTH_MAX_EPSILON) {
                    entityStats.maximizeStatValue(healthStatId);
                    entityStats.update();
                    commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
                    changed = true;
                }
            }
        }

        if (!tierComponent.healthFinalized || tierComponent.needsHealthResync) {
            tierComponent.healthFinalizeTries++;

            if (tierComponent.healthFinalizeTries >= maxFinalizeTries) {
                tierComponent.healthFinalized = true;
                tierComponent.needsHealthResync = false;
            }

            changed = true;
        }

        if (changed) {
            commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
        }
    }

    private void applyOrRepairHealthScaling(
            Ref<EntityStore> npcRef,
            CommandBuffer<EntityStore> commandBuffer,
            EntityStatMap entityStats,
            int healthStatId,
            float healthMultiplier,
            boolean allowTopOff
    ) {
        var before = entityStats.get(healthStatId);
        if (before == null) return;

        float currentHealth = before.get();
        float maxHealth = before.getMax();
        float healthRatio = (maxHealth > 0.0001f) ? (currentHealth / maxHealth) : 1.0f;

        entityStats.putModifier(
                healthStatId,
                HEALTH_MODIFIER_KEY,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        Math.max(0.01f, healthMultiplier)
                )
        );

        entityStats.update();

        if (allowTopOff) {
            entityStats.maximizeStatValue(healthStatId);
            entityStats.update();
        } else {
            var after = entityStats.get(healthStatId);
            if (after != null) {
                float newMaxHealth = after.getMax();
                float newCurrentHealth = Math.max(0f, Math.min(newMaxHealth, healthRatio * newMaxHealth));
                entityStats.setStatValue(healthStatId, newCurrentHealth);
                entityStats.update();
            }
        }

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
    }
}
