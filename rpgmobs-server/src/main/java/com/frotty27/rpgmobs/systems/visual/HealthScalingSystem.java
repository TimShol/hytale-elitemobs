package com.frotty27.rpgmobs.systems.visual;

import com.frotty27.rpgmobs.api.events.RPGMobsScalingAppliedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.features.RPGMobsHealthScalingFeature;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Random;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public class HealthScalingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float HEALTH_MAX_EPSILON = 0.05f;
    private static final int HEALTH_FINALIZE_MAX_TRIES = 5;

    private final RPGMobsPlugin plugin;
    private final RPGMobsHealthScalingFeature feature;
    private final Random random = new Random();

    public HealthScalingSystem(RPGMobsPlugin plugin, RPGMobsHealthScalingFeature feature) {
        this.plugin = plugin;
        this.feature = feature;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPC_COMPONENT_TYPE, plugin.getRPGMobsComponentType());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = chunk.getReferenceTo(entityIndex);

        RPGMobsHealthScalingComponent healthComp = store.getComponent(npcRef, plugin.getHealthScalingComponentType());
        if (healthComp == null || !resolveHealthScalingEnabled(config, npcRef, store)) return;

        if (!healthComp.healthApplied) {
            applyInitialHealthScaling(npcRef, store, commandBuffer, healthComp, config);
            return;
        }

        if (!healthComp.healthFinalized && healthComp.shouldRetryHealthFinalization()) {
            verifyAndTopOffHealth(npcRef, store, commandBuffer, healthComp);
            return;
        }

        RPGMobsTierComponent tierComp = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        boolean needsReconcile = plugin.shouldReconcileThisTick()
                || (tierComp != null && tierComp.lastReconciledAt < plugin.getConfigReloadCount());
        if (!needsReconcile) return;

        if (healthComp.healthFinalized && !healthComp.resyncDone) {
            resyncAfterRestart(npcRef, store, commandBuffer, healthComp);
            return;
        }

        if (healthComp.healthFinalized) {
            reconcileConfigChange(npcRef, store, commandBuffer, healthComp, config);
        }
    }

    private void applyInitialHealthScaling(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                            CommandBuffer<EntityStore> commandBuffer,
                                            RPGMobsHealthScalingComponent healthComp, RPGMobsConfig config) {
        RPGMobsTierComponent tierComponent = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        if (tierComponent == null) return;

        int tierIndex = tierComponent.tierIndex;
        float tierHealthMult = resolveHealthMultiplier(config, npcRef, store, tierIndex);

        float distanceHealthBonus = 0f;
        RPGMobsProgressionComponent progressionComponent = store.getComponent(npcRef,
                                                                              plugin.getProgressionComponentType()
        );
        if (progressionComponent != null) {
            distanceHealthBonus = progressionComponent.distanceHealthBonus();
        }

        float healthRandomVariance = resolveHealthVariance(config, npcRef, store);
        if (healthRandomVariance > 0f) {
            tierHealthMult += (random.nextFloat() * 2f - 1f) * healthRandomVariance;
        }

        float totalMultiplier = tierHealthMult + distanceHealthBonus;

        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float baseHealthMax = healthStatValue.getMax();

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Applying: baseMax=%.1f tierMult=%.2f distBonus=%.2f totalMult=%.2f tier=%d",
                            RPGMobsLogLevel.INFO,
                            baseHealthMax,
                            tierHealthMult,
                            distanceHealthBonus,
                            totalMultiplier,
                            tierIndex
        );

        entityStats.putModifier(healthStatId,
                                feature.getFeatureKey(),
                                new StaticModifier(Modifier.ModifierTarget.MAX,
                                                   StaticModifier.CalculationType.MULTIPLICATIVE,
                                                   Math.max(0.01f, totalMultiplier)
                                )
        );
        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

        healthComp.healthApplied = true;
        healthComp.appliedHealthMult = tierHealthMult;
        healthComp.baseHealthMax = baseHealthMax;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        healthComp.resyncDone = true;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Applied: healthApplied=true baseMax=%.1f appliedMult=%.2f",
                            RPGMobsLogLevel.INFO,
                            healthComp.baseHealthMax,
                            healthComp.appliedHealthMult
        );

        float modelScale = 1.0f;
        RPGMobsModelScalingComponent modelComp = store.getComponent(npcRef, plugin.getModelScalingComponentType());
        if (modelComp != null && modelComp.scaleApplied) {
            modelScale = modelComp.appliedScale;
        }

        float damageMultiplier = 1.0f + (tierIndex * 0.5f);
        RPGMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            damageMultiplier += prog.distanceDamageBonus();
        }

        float finalHealth = baseHealthMax * totalMultiplier;
        var finalHealthStat = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (finalHealthStat != null) {
            var hv = finalHealthStat.get(healthStatId);
            if (hv != null) finalHealth = hv.getMax();
        }

        plugin.getEventBus().fire(new RPGMobsScalingAppliedEvent(npcRef,
                                                                 tierIndex,
                                                                 tierHealthMult,
                                                                 damageMultiplier,
                                                                 modelScale,
                                                                 baseHealthMax,
                                                                 finalHealth,
                                                                 false
        ));
    }

    private void verifyAndTopOffHealth(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                       CommandBuffer<EntityStore> commandBuffer,
                                       RPGMobsHealthScalingComponent healthComp) {
        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float actualMax = healthStatValue.getMax();
        float actualCurrent = healthStatValue.get();
        float baseMax = healthComp.baseHealthMax;

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] VERIFY tick: actualCurrent=%.1f actualMax=%.1f baseMax=%.1f tries=%d",
                            RPGMobsLogLevel.INFO,
                            actualCurrent,
                            actualMax,
                            baseMax,
                            healthComp.healthFinalizeTries
        );

        if (actualCurrent < (actualMax - HEALTH_MAX_EPSILON)) {
            entityStats.maximizeStatValue(healthStatId);
            entityStats.update();
            commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

            RPGMobsLogger.debug(LOGGER,
                                "[HealthScaling] VERIFY top-off: current was %.1f, maximized to max=%.1f",
                                RPGMobsLogLevel.INFO,
                                actualCurrent,
                                actualMax
            );
        }

        boolean modifierApplied = Math.abs(actualMax - baseMax) > 1.0f;
        if (modifierApplied || healthComp.healthFinalizeTries >= HEALTH_FINALIZE_MAX_TRIES) {
            healthComp.healthFinalized = true;
            healthComp.healthFinalizeTries = HEALTH_FINALIZE_MAX_TRIES;

            RPGMobsLogger.debug(LOGGER,
                                "[HealthScaling] VERIFY finalized: actualMax=%.1f (base was %.1f)%s",
                                RPGMobsLogLevel.INFO,
                                actualMax,
                                baseMax,
                                modifierApplied ? "" : " (exhausted tries)"
            );
        } else {
            healthComp.incrementFinalizeTries();
        }

        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
    }

    private void resyncAfterRestart(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                    CommandBuffer<EntityStore> commandBuffer,
                                    RPGMobsHealthScalingComponent healthComp) {
        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return;

        float distanceHealthBonus = 0f;
        RPGMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            distanceHealthBonus = prog.distanceHealthBonus();
        }

        float totalMultiplier = healthComp.appliedHealthMult + distanceHealthBonus;
        float actualMax = healthStatValue.getMax();

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Post-restart resync: actualMax=%.1f baseMax=%.1f totalMult=%.2f",
                            RPGMobsLogLevel.INFO,
                            actualMax,
                            healthComp.baseHealthMax,
                            totalMultiplier
        );

        if (actualMax <= healthComp.baseHealthMax + HEALTH_MAX_EPSILON) {
            entityStats.putModifier(healthStatId,
                                    feature.getFeatureKey(),
                                    new StaticModifier(Modifier.ModifierTarget.MAX,
                                                       StaticModifier.CalculationType.MULTIPLICATIVE,
                                                       Math.max(0.01f, totalMultiplier)
                                    )
            );

            RPGMobsLogger.debug(LOGGER,
                                "[HealthScaling] Resync: registered modifier (mult=%.2f), engine will apply on flush",
                                RPGMobsLogLevel.INFO,
                                totalMultiplier
            );

            commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
        }

        healthComp.resyncDone = true;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
    }

    private void reconcileConfigChange(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                       CommandBuffer<EntityStore> commandBuffer,
                                       RPGMobsHealthScalingComponent healthComp, RPGMobsConfig config) {
        RPGMobsTierComponent tierComp = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        if (tierComp == null) return;

        int tierIndex = tierComp.tierIndex;
        float configHealthMult = resolveHealthMultiplier(config, npcRef, store, tierIndex);
        float healthVariance = resolveHealthVariance(config, npcRef, store);
        float currentMult = healthComp.appliedHealthMult;

        float minValid = configHealthMult - healthVariance;
        float maxValid = configHealthMult + healthVariance;

        if (currentMult >= minValid - 0.001f && currentMult <= maxValid + 0.001f) {
            return;
        }

        float newMult = currentMult < minValid ? minValid : maxValid;

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Config reconcile: tier=%d oldMult=%.2f newMult=%.2f (base=%.2f var=%.2f)",
                            RPGMobsLogLevel.INFO,
                            tierIndex,
                            currentMult,
                            newMult,
                            configHealthMult,
                            healthVariance
        );

        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();

        float distanceHealthBonus = 0f;
        RPGMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            distanceHealthBonus = prog.distanceHealthBonus();
        }

        float totalMultiplier = newMult + distanceHealthBonus;

        entityStats.putModifier(healthStatId,
                                feature.getFeatureKey(),
                                new StaticModifier(Modifier.ModifierTarget.MAX,
                                                   StaticModifier.CalculationType.MULTIPLICATIVE,
                                                   Math.max(0.01f, totalMultiplier)
                                )
        );

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

        healthComp.appliedHealthMult = newMult;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Config reconcile: registered new modifier (mult=%.2f), entering VERIFY",
                            RPGMobsLogLevel.INFO,
                            totalMultiplier
        );
    }

    private boolean resolveHealthScalingEnabled(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npc != null && npc.getWorld() != null) {
            ResolvedConfig resolved = plugin.getResolvedConfig(npc.getWorld().getName());
            return resolved.enableHealthScaling;
        }
        return config.healthConfig.enableMobHealthScaling;
    }

    private float resolveHealthVariance(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npc != null && npc.getWorld() != null) {
            ResolvedConfig resolved = plugin.getResolvedConfig(npc.getWorld().getName());
            return resolved.healthRandomVariance;
        }
        return config.healthConfig.mobHealthRandomVariance;
    }

    private float resolveHealthMultiplier(RPGMobsConfig config, Ref<EntityStore> npcRef,
                                           Store<EntityStore> store, int tierIndex) {
        NPCEntity npc = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npc != null && npc.getWorld() != null) {
            String worldName = npc.getWorld().getName();
            ResolvedConfig resolved = plugin.getResolvedConfig(worldName);
            if (resolved.healthMultiplierPerTier != null && resolved.healthMultiplierPerTier.length > tierIndex) {
                return resolved.healthMultiplierPerTier[tierIndex];
            }
        }
        if (config.healthConfig.mobHealthMultiplierPerTier != null && config.healthConfig.mobHealthMultiplierPerTier.length > tierIndex) {
            return config.healthConfig.mobHealthMultiplierPerTier[tierIndex];
        }
        return 1.0f;
    }
}
