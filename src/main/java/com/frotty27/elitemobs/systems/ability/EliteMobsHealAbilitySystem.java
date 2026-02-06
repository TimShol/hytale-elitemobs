package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_HEAL_POTION_KEY;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;
import static com.frotty27.elitemobs.utils.Constants.TICKS_PER_SECOND;

public class EliteMobsHealAbilitySystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType HEAL_INTERACTION_TYPE = InteractionType.Ability2;

    private final EliteMobsPlugin plugin;
    private final Random random = new Random();

    public EliteMobsHealAbilitySystem(EliteMobsPlugin plugin) {
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

        int tierIndex = tierComponent.tierIndex; // Already clamped? Should clamp.
        if (tierIndex < 0) tierIndex = 0; // simple clamp

        long currentTick = plugin.getTickClock().getTick();

        tickDebugSample(deltaTime, config, tierComponent, store, npcRef);

        EliteMobsConfig.AbilityConfig healConfig = getHealConfig(config);
        if (healConfig == null) return;
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(healConfig, tierIndex)) return;
        if (!AbilityGateEvaluator.isAllowed(healConfig, safeRoleName(npcEntity), "", tierIndex)) return;

        if (!tierComponent.healAbilityRollInitialized) {
            float chance = AssetConfigHelpers.getFloatForTier(healConfig.chancePerTier, tierIndex, 1f);
            tierComponent.healAbilityEnabledRoll = random.nextFloat() < clamp01(chance);
            tierComponent.healAbilityRollInitialized = true;
            commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
        }

        if (!tierComponent.healAbilityEnabledRoll) return;
        if (tierComponent.nextHealAllowedTick > currentTick) return;

        if (!tierComponent.healThresholdInitialized) {
            tierComponent.healTriggerPercent = EliteMobsAbilityInteractionHelper.rollHealTriggerPercent(random, healConfig);
            tierComponent.healThresholdInitialized = true;
            commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
        }

        if (AbilityHelpers.isInteractionTypeRunning(store, npcRef, HEAL_INTERACTION_TYPE)) return;

        EntityStatMap stats = store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        var healthStat = stats.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) return;

        float maxHealth = (float) healthStat.getMax();
        if (maxHealth <= 0.0001f) return;
        float currentPercent = (float) (healthStat.get() / maxHealth);
        if (currentPercent > tierComponent.healTriggerPercent) return;
        
        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Heal trigger: hp=%.3f threshold=%.3f tier=%d role=%s",
                    EliteMobsLogLevel.INFO,
                    currentPercent,
                    tierComponent.healTriggerPercent,
                    tierIndex,
                    safeRoleName(npcEntity)
            );
        }

        String rootInteractionId = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(
                config,
                healConfig,
                EliteMobsConfig.HealAbilityConfig.TEMPLATE_ROOT_INTERACTION_INSTANT,
                tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) return;
        if (AbilityHelpers.getRootInteraction(rootInteractionId) == null) {
            if (config.debug.isDebugModeEnabled) {
                EliteMobsLogger.debug(
                        LOGGER,
                        "Heal root interaction missing: %s",
                        EliteMobsLogLevel.INFO,
                        rootInteractionId
                );
            }
            return;
        }

        if (faceTowardTarget(npcEntity, npcRef, store, commandBuffer, tierComponent)) {
            // rotation updated
        }

        boolean started = EliteMobsAbilityInteractionHelper.tryStartInteraction(
                npcRef,
                store,
                commandBuffer,
                HEAL_INTERACTION_TYPE,
                rootInteractionId
        );
        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Heal interaction started=%s root=%s tier=%d role=%s",
                    EliteMobsLogLevel.INFO,
                    String.valueOf(started),
                    rootInteractionId,
                    tierIndex,
                    safeRoleName(npcEntity)
            );
        }
        if (!started) {
            // Backoff to prevent log spam and hammer-retrying
            tierComponent.nextHealAllowedTick = currentTick + secondsToTicks(1.0f);
            commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
            return;
        }

        EliteMobsAbilityInteractionHelper.swapToPotionInHandIfPossible(npcEntity, config, tierComponent);

        long cooldownTicks = secondsToTicks(AssetConfigHelpers.getFloatForTier(healConfig.cooldownSecondsPerTier, tierIndex, 0f));
        tierComponent.nextHealAllowedTick = currentTick + cooldownTicks;
        tierComponent.healInProgress = true;
        tierComponent.healHitsTaken = 0;
        tierComponent.healSwapRestoreTick = currentTick + secondsToTicks(resolveHealChainDurationSeconds(healConfig));
        tierComponent.debugSampleTimeLeft = 2.0f;
        commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
    }

    private void tickDebugSample(
            float deltaTimeSeconds,
            EliteMobsConfig config,
            EliteMobsTierComponent tierComponent,
            Store<EntityStore> entityStore,
            Ref<EntityStore> npcRef
    ) {
        if (tierComponent.debugSampleTimeLeft <= 0f) return;

        float nextSampleTime = Math.max(0f, tierComponent.debugSampleTimeLeft - deltaTimeSeconds);
        tierComponent.debugSampleTimeLeft = nextSampleTime;

        if (!config.debug.isDebugModeEnabled) return;

        Velocity velocity = entityStore.getComponent(npcRef, Velocity.getComponentType());
        TransformComponent transform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        if (velocity == null || transform == null) return;

        EliteMobsLogger.debug(
                LOGGER,
                "HEAL SAMPLE t=%.2f vel=(%.3f, %.3f, %.3f) yaw=%.1f pos=(%.2f, %.2f, %.2f)",
                EliteMobsLogLevel.INFO,
                tierComponent.debugSampleTimeLeft,
                velocity.getX(),
                velocity.getY(),
                velocity.getZ(),
                (double) transform.getRotation().getYaw(),
                transform.getPosition().getX(),
                transform.getPosition().getY(),
                transform.getPosition().getZ()
        );
    }

    private boolean faceTowardTarget(
            NPCEntity npcEntity,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent
    ) {
        Ref<EntityStore> targetRef = AbilityHelpers.getLockedTargetRef(npcEntity);
        if (targetRef == null || !targetRef.isValid()) {
            if (tierComponent != null && tierComponent.lastAggroRef != null && tierComponent.lastAggroRef.isValid()) {
                targetRef = tierComponent.lastAggroRef;
            }
        }
        
        if (targetRef == null || !targetRef.isValid()) {
            targetRef = AbilityHelpers.findNearestVisiblePlayer(npcEntity, 30.0f);
        }

        if (targetRef == null || !targetRef.isValid()) return false;
        if (entityStore.getComponent(targetRef, PlayerRef.getComponentType()) == null 
            && entityStore.getComponent(targetRef, NPCEntity.getComponentType()) == null) return false;

        TransformComponent npcTransform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = entityStore.getComponent(targetRef, TransformComponent.getComponentType());
        if (npcTransform == null || targetTransform == null) return false;

        double deltaX = targetTransform.getPosition().getX() - npcTransform.getPosition().getX();
        double deltaZ = targetTransform.getPosition().getZ() - npcTransform.getPosition().getZ();

        float yawToTarget = (float) Math.toDegrees(Math.atan2(deltaX, -deltaZ));

        var rotation = npcTransform.getRotation();
        rotation.setYaw(yawToTarget);
        npcTransform.setRotation(rotation);

        commandBuffer.replaceComponent(npcRef, TransformComponent.getComponentType(), npcTransform);
        return true;
    }

    private static EliteMobsConfig.AbilityConfig getHealConfig(EliteMobsConfig config) {
        return (EliteMobsConfig.AbilityConfig) AssetConfigHelpers.getAssetConfig(
                config,
                AssetType.ABILITIES,
                ABILITY_HEAL_POTION_KEY
        );
    }


    private static long secondsToTicks(float seconds) {
        if (seconds <= 0f) return 0L;
        return Math.max(1L, (long) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    private static float resolveHealChainDurationSeconds(EliteMobsConfig.AbilityConfig healConfig) {
        float drinkDuration = EliteMobsAbilityInteractionHelper.resolveHealDrinkDurationSeconds(healConfig);
        return AbilityConstants.HEAL_PRE_DRINK_SECONDS + drinkDuration;
    }

    private static String safeRoleName(NPCEntity npcEntity) {
        if (npcEntity == null) return "";
        String roleName = npcEntity.getRoleName();
        return roleName == null ? "" : roleName;
    }
}
