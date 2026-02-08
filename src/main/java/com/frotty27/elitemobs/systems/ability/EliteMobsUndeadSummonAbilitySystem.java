package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.AbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonAbilityConfig;
import com.frotty27.elitemobs.features.EliteMobsAbilityFeatureHelpers;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Random;
import java.util.Set;

import static com.frotty27.elitemobs.features.EliteMobsUndeadSummonAbilityFeature.ABILITY_UNDEAD_SUMMON;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;
import static com.frotty27.elitemobs.utils.Constants.TICKS_PER_SECOND;
import static com.frotty27.elitemobs.utils.StringHelpers.safeRoleName;

public class EliteMobsUndeadSummonAbilitySystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType SUMMON_INTERACTION_TYPE = InteractionType.Ability3;
    public static final float SUMMON_CHAIN_DELAY_SECONDS = 2.2f;
    public static final double SUMMON_MARKER_RANGE = 30.0;


    private final EliteMobsPlugin plugin;
    private final Random random = new Random();

    public EliteMobsUndeadSummonAbilitySystem(EliteMobsPlugin plugin) {
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

        Ref<EntityStore> npcRef = chunk.getReferenceTo(entityIndex);
        NPCEntity npcEntity = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
        if (npcEntity == null) return;

        EliteMobsTierComponent tierComponent = chunk.getComponent(entityIndex, plugin.getEliteMobsComponent());
        if (tierComponent == null || tierComponent.tierIndex < 0) return;

        int tierIndex = tierComponent.tierIndex;
        if (tierIndex < 0) tierIndex = 0;

        long currentTick = plugin.getTickClock().getTick();

        if (tierComponent.healInProgress || tierComponent.pendingSummonTick >= 0) return;

        AbilityConfig summonConfig = getSummonConfig(config);
        if (summonConfig == null) return;
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(summonConfig, tierIndex)) return;
        if (!AbilityGateEvaluator.isAllowed(summonConfig, safeRoleName(npcEntity), "", tierIndex)) return;

        if (!tierComponent.summonAbilityRollInitialized) {
            float chance = AssetConfigHelpers.getFloatForTier(summonConfig.chancePerTier, tierIndex, 1f);
            tierComponent.summonAbilityEnabledRoll = random.nextFloat() < clamp01(chance);
            tierComponent.summonAbilityRollInitialized = true;
            commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
        }

        if (!tierComponent.summonAbilityEnabledRoll) return;
        if (!isHealthBelowThreshold(store, npcRef, tierComponent)) return;
        if (tierComponent.nextSummonAllowedTick > currentTick) return;
        int maxAlive = EliteMobsConfig.DEFAULT_SUMMON_MAX_ALIVE;
        if (summonConfig instanceof SummonAbilityConfig summonAbilityConfig) {
            maxAlive = clampSummonMaxAlive(summonAbilityConfig.maxAlive);
        }
        if (maxAlive == 0) return;
        if (tierComponent.summonedAliveCount >= maxAlive) return;
        if (AbilityHelpers.isInteractionTypeRunning(store, npcRef, SUMMON_INTERACTION_TYPE)) return;

        String roleIdentifier = EliteMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(summonConfig, safeRoleName(npcEntity));
        String normalizedIdentifier = EliteMobsConfig.normalizeRoleIdentifier(roleIdentifier);
        String rootInteractionId = EliteMobsAbilityFeatureHelpers.buildRoleSpecificSummonRootId(config, normalizedIdentifier, tierIndex);
        if (rootInteractionId == null || AbilityHelpers.getRootInteraction(rootInteractionId) == null) {
            rootInteractionId = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(
                    config,
                    summonConfig,
                    AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                    tierIndex
            );
        }
        if (rootInteractionId == null || rootInteractionId.isBlank()) return;

        boolean started = EliteMobsAbilityFeatureHelpers.tryStartInteraction(
                npcRef,
                store,
                commandBuffer,
                SUMMON_INTERACTION_TYPE,
                rootInteractionId
        );
        if (!started) return;

        if (summonConfig instanceof SummonAbilityConfig summonAbilityConfig) {
            TransformComponent transform = store.getComponent(npcRef, TransformComponent.getComponentType());
            if (transform != null) {
                Set<String> roleNames = EliteMobsAbilityFeatureHelpers.getSummonRoleNames(
                        summonAbilityConfig,
                        normalizedIdentifier
                );
                plugin.getSummonRiseTracker().recordSummon(transform.getPosition(), SUMMON_MARKER_RANGE, roleNames);
            }
        }

        long cooldownTicks = secondsToTicks(AssetConfigHelpers.getFloatForTier(summonConfig.cooldownSecondsPerTier, tierIndex, 0f));
        tierComponent.nextSummonAllowedTick = currentTick + cooldownTicks;
        tierComponent.pendingSummonRoleIdentifier = normalizedIdentifier;
        tierComponent.pendingSummonTick = currentTick + secondsToTicks(SUMMON_CHAIN_DELAY_SECONDS);
        commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);

        if (config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Summon started: role=%s tier=%d root=%s identifier=%s",
                    EliteMobsLogLevel.INFO,
                    safeRoleName(npcEntity),
                    tierIndex,
                    rootInteractionId,
                    normalizedIdentifier
            );
        }
    }

    private static AbilityConfig getSummonConfig(EliteMobsConfig config) {
        return (AbilityConfig) AssetConfigHelpers.getAssetConfig(
                config,
                AssetType.ABILITIES, ABILITY_UNDEAD_SUMMON
        );
    }

    private static long secondsToTicks(float seconds) {
        if (seconds <= 0f) return 0L;
        return Math.max(1L, (long) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    private static boolean isHealthBelowThreshold(
            Store<EntityStore> entityStore,
            Ref<EntityStore> npcRef,
            EliteMobsTierComponent tierComponent
    ) {
        if (entityStore == null || npcRef == null) return false;
        var stats = entityStore.getComponent(npcRef, EntityStatMap.getComponentType());
        if (stats == null) return false;
        var healthStat = stats.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) return false;
        float maxHealth = (float) healthStat.getMax();
        if (maxHealth <= 0.0001f) return false;
        float currentPercent = (float) (healthStat.get() / maxHealth);
        float threshold = tierComponent.healThresholdInitialized ? tierComponent.healTriggerPercent : 0.5f;
        return currentPercent <= threshold;
    }

    private static int clampSummonMaxAlive(int value) {
        if (value < EliteMobsConfig.SUMMON_MAX_ALIVE_MIN) return EliteMobsConfig.SUMMON_MAX_ALIVE_MIN;
        return Math.min(value, EliteMobsConfig.SUMMON_MAX_ALIVE_MAX);
    }
}
