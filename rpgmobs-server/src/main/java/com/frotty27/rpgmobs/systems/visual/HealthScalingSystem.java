package com.frotty27.rpgmobs.systems.visual;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.RPGMobsScalingAppliedEvent;
import com.frotty27.rpgmobs.api.events.RPGMobsSpawnedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.RPGMobsHealthScalingFeature;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.StoreHelpers;
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

public class HealthScalingSystem extends EntityTickingSystem<EntityStore> implements IRPGMobsEventListener {

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
        if (healthComp == null || !config.healthConfig.enableMobHealthScaling || !healthComp.healthApplied) return;

        if (!healthComp.healthFinalized && healthComp.shouldRetryHealthFinalization()) {
            verifyHealthScalingFreshSpawn(npcRef, store, commandBuffer, healthComp);
            return;
        }

        if (!plugin.shouldReconcileThisTick()) return;

        if (healthComp.healthFinalized && !healthComp.resyncDone) {
            resyncAfterRestart(npcRef, store, commandBuffer, healthComp);
            return;
        }

        if (healthComp.healthFinalized) {
            reconcileConfigChange(npcRef, store, commandBuffer, healthComp, config);
        }
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
        float configHealthMult = 1.0f;
        if (config.healthConfig.mobHealthMultiplierPerTier != null && config.healthConfig.mobHealthMultiplierPerTier.length > tierIndex) {
            configHealthMult = config.healthConfig.mobHealthMultiplierPerTier[tierIndex];
        }

        boolean healthMultChanged = Math.abs(configHealthMult - healthComp.appliedHealthMult) > 0.001f;

        if (!healthMultChanged) {
            return;
        }

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Config reconcile: tier=%d oldMult=%.2f newMult=%.2f",
                            RPGMobsLogLevel.INFO,
                            tierIndex,
                            healthComp.appliedHealthMult,
                            configHealthMult
        );

        EntityStatMap entityStats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (entityStats == null) return;

        int healthStatId = DefaultEntityStatTypes.getHealth();

        float distanceHealthBonus = 0f;
        RPGMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            distanceHealthBonus = prog.distanceHealthBonus();
        }

        float totalMultiplier = configHealthMult + distanceHealthBonus;

        entityStats.putModifier(healthStatId,
                                feature.getFeatureKey(),
                                new StaticModifier(Modifier.ModifierTarget.MAX,
                                                   StaticModifier.CalculationType.MULTIPLICATIVE,
                                                   Math.max(0.01f, totalMultiplier)
                                )
        );

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

        healthComp.appliedHealthMult = configHealthMult;
        healthComp.healthFinalized = false;
        healthComp.healthFinalizeTries = 0;
        commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Config reconcile: registered new modifier (mult=%.2f), entering VERIFY",
                            RPGMobsLogLevel.INFO,
                            totalMultiplier
        );
    }


    private void applyHealthModifier(Ref<EntityStore> npcRef, CommandBuffer<EntityStore> commandBuffer,
                                     EntityStatMap entityStats, int healthStatId, float totalMultiplier) {
        var before = entityStats.get(healthStatId);
        if (before == null) return;

        float maxHealthBefore = before.getMax();

        entityStats.putModifier(healthStatId,
                                feature.getFeatureKey(),
                                new StaticModifier(Modifier.ModifierTarget.MAX,
                                                   StaticModifier.CalculationType.MULTIPLICATIVE,
                                                   Math.max(0.01f, totalMultiplier)
                                )
        );

        RPGMobsLogger.debug(LOGGER,
                            "[HealthScaling] Registered modifier: mult=%.2f beforeMax=%.1f",
                            RPGMobsLogLevel.INFO,
                            totalMultiplier,
                            maxHealthBefore
        );

        commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
    }


    public void applyHealthScalingOnSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                          CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null || !config.healthConfig.enableMobHealthScaling) return;

        RPGMobsTierComponent tierComponent = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        RPGMobsHealthScalingComponent healthScalingComponent = store.getComponent(npcRef,
                                                                                  plugin.getHealthScalingComponentType()
        );

        if (tierComponent == null) return;

        if (healthScalingComponent != null && healthScalingComponent.healthApplied) return;


        int tierIndex = tierComponent.tierIndex;
        float tierHealthMult = 1.0f;
        if (config.healthConfig.mobHealthMultiplierPerTier != null && config.healthConfig.mobHealthMultiplierPerTier.length > tierIndex) {
            tierHealthMult = config.healthConfig.mobHealthMultiplierPerTier[tierIndex];
        }


        float distanceHealthBonus = 0f;
        RPGMobsProgressionComponent progressionComponent = store.getComponent(npcRef,
                                                                              plugin.getProgressionComponentType()
        );
        if (progressionComponent != null) {
            distanceHealthBonus = progressionComponent.distanceHealthBonus();
        }

        float healthRandomVariance = config.healthConfig.mobHealthRandomVariance;
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
                            "[HealthScaling] BEFORE spawn scaling: baseMax=%.1f tierMult=%.2f distBonus=%.2f totalMult=%.2f tier=%d",
                            RPGMobsLogLevel.INFO,
                            baseHealthMax,
                            tierHealthMult,
                            distanceHealthBonus,
                            totalMultiplier,
                            tierIndex
        );

        applyHealthModifier(npcRef, commandBuffer, entityStats, healthStatId, totalMultiplier);


        if (healthScalingComponent != null) {
            healthScalingComponent.healthApplied = true;
            healthScalingComponent.appliedHealthMult = tierHealthMult;
            healthScalingComponent.baseHealthMax = baseHealthMax;
            healthScalingComponent.healthFinalized = false;
            healthScalingComponent.healthFinalizeTries = 0;
            healthScalingComponent.resyncDone = true;
            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthScalingComponent);

            RPGMobsLogger.debug(LOGGER,
                                "[HealthScaling] Component stored: healthApplied=true baseMax=%.1f appliedMult=%.2f",
                                RPGMobsLogLevel.INFO,
                                healthScalingComponent.baseHealthMax,
                                healthScalingComponent.appliedHealthMult
            );
        }

        float modelScale = 1.0f;
        RPGMobsModelScalingComponent modelComp = store.getComponent(npcRef, plugin.getModelScalingComponentType());
        if (modelComp != null && modelComp.scaledApplied) {
            modelScale = modelComp.appliedScale;
        }

        float damageMultiplier = 1.0f + (tierIndex * 0.5f);
        RPGMobsProgressionComponent prog = store.getComponent(npcRef, plugin.getProgressionComponentType());
        if (prog != null) {
            damageMultiplier += prog.distanceDamageBonus();
        }

        var finalHealthStat = store.getComponent(npcRef, EntityStatMap.getComponentType());
        float finalHealth = baseHealthMax * totalMultiplier;
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


    private void verifyHealthScalingFreshSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store,
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

        boolean modifierApplied = Math.abs(actualMax - baseMax) > 1.0f;

        if (modifierApplied) {
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

            healthComp.healthFinalized = true;
            healthComp.healthFinalizeTries = HEALTH_FINALIZE_MAX_TRIES;
            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);

            RPGMobsLogger.debug(LOGGER,
                                "[HealthScaling] VERIFY finalized: actualMax=%.1f (base was %.1f)",
                                RPGMobsLogLevel.INFO,
                                actualMax,
                                baseMax
            );
        } else {
            healthComp.incrementFinalizeTries();

            if (healthComp.healthFinalizeTries >= HEALTH_FINALIZE_MAX_TRIES) {
                if (actualCurrent < (actualMax - HEALTH_MAX_EPSILON)) {
                    entityStats.maximizeStatValue(healthStatId);
                    entityStats.update();
                    commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);

                    RPGMobsLogger.debug(LOGGER,
                                        "[HealthScaling] VERIFY exhausted top-off: current was %.1f, maximized to max=%.1f",
                                        RPGMobsLogLevel.INFO,
                                        actualCurrent,
                                        actualMax
                    );
                }

                healthComp.healthFinalized = true;

                RPGMobsLogger.debug(LOGGER,
                                    "[HealthScaling] VERIFY exhausted: accepting actualMax=%.1f as final (base was %.1f). Modifier may not have applied.",
                                    RPGMobsLogLevel.WARNING,
                                    actualMax,
                                    baseMax
                );
            }

            commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
        }
    }

    @Override
    public void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
        if (event.isCancelled()) return;

        Ref<EntityStore> npcRef = event.getEntityRef();
        Store<EntityStore> store = npcRef.getStore();

        NPCEntity npcEntity = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null || npcEntity.getWorld() == null) return;

        npcEntity.getWorld().execute(() -> {
            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> entityStore = entityStoreProvider.getStore();

            StoreHelpers.withEntity(entityStore,
                                    npcRef,
                                    (_, commandBuffer, _) -> applyHealthScalingOnSpawn(npcRef,
                                                                                       entityStore,
                                                                                       commandBuffer
                                    )
            );
        });
    }
}
