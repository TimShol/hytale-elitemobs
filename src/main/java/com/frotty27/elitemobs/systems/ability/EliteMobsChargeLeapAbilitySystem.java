package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsLeapAbilityStateComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.equipment.EliteMobsEquipmentService;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.frotty27.elitemobs.features.EliteMobsChargeLeapAbilityFeature.ABILITY_CHARGE_LEAP;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.elitemobs.utils.StringHelpers.safeRoleName;
import static com.frotty27.elitemobs.utils.StringHelpers.toLowerOrEmpty;

public final class EliteMobsChargeLeapAbilitySystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType ABILITY_INTERACTION_TYPE = InteractionType.Ability1;
    private static final float ABILITY_CHECKS_PER_SECOND = 4.0f;
    private static final int ABILITY_CHECK_STEP_DIVISOR = 4;
    private static final float DEBUG_SAMPLE_SECONDS = 2.0f;

    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE =
            TransformComponent.getComponentType();
    private static final ComponentType<EntityStore, DeathComponent> DEATH_COMPONENT_TYPE =
            DeathComponent.getComponentType();
    private static final ComponentType<EntityStore, NetworkId> NETWORK_ID_COMPONENT_TYPE =
            NetworkId.getComponentType();
    private static final ComponentType<EntityStore, Velocity> VELOCITY_COMPONENT_TYPE =
            Velocity.getComponentType();
    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_COMPONENT_TYPE =
            PlayerRef.getComponentType();

    private final EliteMobsPlugin plugin;

    public EliteMobsChargeLeapAbilitySystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                NPCEntity.getComponentType(),
                plugin.getEliteMobsComponent(),
                plugin.getLeapAbilityComponent()
        );
    }

    @Override
    public void tick(
            float deltaTimeSeconds,
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(entityIndex);
        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex, NPCEntity.getComponentType());
        if (npcEntity == null) return;

        EliteMobsTierComponent tierComponent =
                archetypeChunk.getComponent(entityIndex, plugin.getEliteMobsComponent());
        if (tierComponent == null || tierComponent.tierIndex < 0) return;

        EliteMobsLeapAbilityStateComponent abilityState =
                archetypeChunk.getComponent(entityIndex, plugin.getLeapAbilityComponent());
        if (abilityState == null) return;

        if (getComponentSafe(entityStore, npcRef, DEATH_COMPONENT_TYPE) != null) return;

        int tierIndex = clampTierIndex(tierComponent.tierIndex);

        EliteMobsConfig.ChargeLeapAbilityConfig leapAbilityConfig =
                getChargeLeapConfig(config);
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(leapAbilityConfig, tierIndex)) {
            return;
        }

        boolean wroteStateThisTick = false;

        try {
            tickDebugSample(deltaTimeSeconds, config, abilityState, entityStore, npcRef);

            if (abilityState.cooldownSeconds > 0f) {
                if (tickCooldown(deltaTimeSeconds, abilityState)) {
                    commandBuffer.replaceComponent(npcRef, plugin.getLeapAbilityComponent(), abilityState);
                    wroteStateThisTick = true;
                }
                return;
            }

            if (!advanceAbilityCheckTimer(deltaTimeSeconds, abilityState)) return;
            if (!shouldEvaluateAbilityThisStep(entityStore, npcRef, abilityState)) return;

            Ref<EntityStore> targetEntityRef = AbilityHelpers.getLockedTargetRef(npcEntity);
            if (targetEntityRef == null || !targetEntityRef.isValid()) {
                Ref<EntityStore> lastAggro = tierComponent.lastAggroRef;
                if (lastAggro != null && lastAggro.isValid()) {
                    targetEntityRef = lastAggro;
                }
            }

            if (targetEntityRef == null
                    || !targetEntityRef.isValid()
                    || getComponentSafe(entityStore, targetEntityRef, DEATH_COMPONENT_TYPE) != null
                    || (getComponentSafe(entityStore, targetEntityRef, PLAYER_REF_COMPONENT_TYPE) == null
                        && getComponentSafe(entityStore, targetEntityRef, NPCEntity.getComponentType()) == null)) {
                if (shouldLogGate(config, abilityState)) {
                    EliteMobsLogger.debug(
                            LOGGER,
                            "Leap no target: tier=%d role=%s",
                            EliteMobsLogLevel.INFO,
                            tierIndex, safeRoleName(npcEntity)
                    );
                }
                return;
            }

            TransformComponent mobTransform = getComponentSafe(entityStore, npcRef, TRANSFORM_COMPONENT_TYPE);
            TransformComponent targetTransform = getComponentSafe(entityStore, targetEntityRef, TRANSFORM_COMPONENT_TYPE);
            if (mobTransform == null || targetTransform == null) {
                return;
            }

            // Calculate distance and yaw using Vector3f
            Vector3f mobPos = mobTransform.getPosition().toVector3f();
            Vector3f targetPos = targetTransform.getPosition().toVector3f();
            float deltaX = targetPos.getX() - mobPos.getX();
            float deltaZ = targetPos.getZ() - mobPos.getZ();
            double horizontalDistanceSquared = (double) deltaX * deltaX + (double) deltaZ * deltaZ;

            if (!isWithinRange(horizontalDistanceSquared, leapAbilityConfig.minRange, leapAbilityConfig.maxRange)) {
                if (shouldLogGate(config, abilityState)) {
                    EliteMobsLogger.debug(
                            LOGGER,
                            "Leap out of range: dist=%.2f min=%.2f max=%.2f role=%s",
                            EliteMobsLogLevel.INFO,
                            Math.sqrt(horizontalDistanceSquared),
                            leapAbilityConfig.minRange,
                            leapAbilityConfig.maxRange,
                            safeRoleName(npcEntity)
                    );
                }
                return;
            }

            String roleNameLowercase = toLowerOrEmpty(npcEntity.getRoleName());
            String weaponIdLowercase = toLowerOrEmpty(EliteMobsEquipmentService.getWeaponIdInHand(npcEntity));

            if (!AbilityGateEvaluator.isAllowed(
                    leapAbilityConfig,
                    roleNameLowercase,
                    weaponIdLowercase,
                    tierIndex
            )) {
                return;
            }

            String rootInteractionId =
                    AssetConfigHelpers.getTieredAssetIdFromTemplateKey(
                            config,
                            leapAbilityConfig,
                            EliteMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                            tierIndex
                    );

            var rootInteraction = AbilityHelpers.getRootInteraction(rootInteractionId);
            if (rootInteraction == null) return;

            if (leapAbilityConfig.faceTarget) {
                float yawDegrees = (float) Math.toDegrees(Math.atan2(deltaX, -deltaZ));
                Vector3f rotation = mobTransform.getRotation();
                rotation.setYaw(yawDegrees);
                mobTransform.setRotation(rotation);
                commandBuffer.replaceComponent(npcRef, TRANSFORM_COMPONENT_TYPE, mobTransform);
            }

            ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                    InteractionModule.get().getInteractionManagerComponent();
            InteractionManager interactionManager =
                    getComponentSafe(entityStore, npcRef, interactionManagerComponentType);
            if (interactionManager == null) return;

            InteractionContext interactionContext =
                    InteractionContext.forInteraction(
                            interactionManager,
                            npcRef,
                            ABILITY_INTERACTION_TYPE,
                            entityStore
                    );

            boolean started = interactionManager.tryStartChain(
                    npcRef,
                    commandBuffer,
                    ABILITY_INTERACTION_TYPE,
                    interactionContext,
                    rootInteraction
            );

            if (started) {
                abilityState.debugSampleTimeLeft = DEBUG_SAMPLE_SECONDS;
                abilityState.cooldownSeconds =
                        Math.max(
                                0f,
                                AssetConfigHelpers.getFloatForTier(
                                        leapAbilityConfig.cooldownSecondsPerTier,
                                        tierIndex,
                                        0f
                                )
                        );

                commandBuffer.replaceComponent(npcRef, interactionManagerComponentType, interactionManager);

                if (config.debugConfig.isDebugModeEnabled) {
                    EliteMobsLogger.debug(
                            LOGGER,
                            "started ability=%s root=%s dist=%.2f yaw=%.1f role=%s weapon=%s tier=%d",
                            EliteMobsLogLevel.INFO,
                            ABILITY_CHARGE_LEAP,
                            rootInteractionId,
                            Math.sqrt(horizontalDistanceSquared),
                            (double) mobTransform.getRotation().getYaw(),
                            roleNameLowercase,
                            weaponIdLowercase,
                            tierIndex
                    );
                }
            }
        } finally {
            if (!wroteStateThisTick) {
                commandBuffer.replaceComponent(npcRef, plugin.getLeapAbilityComponent(), abilityState);
            }
        }
    }

    private static EliteMobsConfig.ChargeLeapAbilityConfig getChargeLeapConfig(EliteMobsConfig config) {
        var cfg = AssetConfigHelpers.getAssetConfig(config, AssetType.ABILITIES, ABILITY_CHARGE_LEAP);
        return (cfg instanceof EliteMobsConfig.ChargeLeapAbilityConfig c) ? c : null;
    }

    private static boolean advanceAbilityCheckTimer(
            float deltaTimeSeconds,
            EliteMobsLeapAbilityStateComponent abilityState
    ) {
        float checkIntervalSeconds = 1.0f / ABILITY_CHECKS_PER_SECOND;

        abilityState.timeSinceLastAbilityCheckSeconds += deltaTimeSeconds;
        if (abilityState.timeSinceLastAbilityCheckSeconds < checkIntervalSeconds) return false;

        abilityState.timeSinceLastAbilityCheckSeconds =
                abilityState.timeSinceLastAbilityCheckSeconds % checkIntervalSeconds;
        return true;
    }

    private static boolean shouldEvaluateAbilityThisStep(
            Store<EntityStore> entityStore,
            Ref<EntityStore> mobEntityRef,
            EliteMobsLeapAbilityStateComponent abilityState
    ) {
        abilityState.abilityCheckStepCounter++;

        NetworkId networkId = getComponentSafe(entityStore, mobEntityRef, NETWORK_ID_COMPONENT_TYPE);
        int shardIdentifier = (networkId != null) ? networkId.getId() : 0;

        int phaseIdentifier = abilityState.abilityCheckStepCounter + shardIdentifier;
        return (phaseIdentifier % ABILITY_CHECK_STEP_DIVISOR) == 0;
    }

    private static <T extends com.hypixel.hytale.component.Component<EntityStore>> @Nullable T getComponentSafe(
            Store<EntityStore> entityStore,
            Ref<EntityStore> entityRef,
            @Nullable ComponentType<EntityStore, T> componentType
    ) {
        if (componentType == null) return null;
        return entityStore.getComponent(entityRef, componentType);
    }

    private static boolean isWithinRange(
            double distanceSquared,
            float minRange,
            float maxRange
    ) {
        if (maxRange < minRange) return false;

        double minRangeSquared = (double) minRange * (double) minRange;
        double maxRangeSquared = (double) maxRange * (double) maxRange;

        return distanceSquared >= minRangeSquared && distanceSquared <= maxRangeSquared;
    }

    private static boolean shouldLogGate(EliteMobsConfig config, EliteMobsLeapAbilityStateComponent abilityState) {
        if (config == null || abilityState == null) return false;
        if (!config.debugConfig.isDebugModeEnabled) return false;
        return (abilityState.abilityCheckStepCounter % 20) == 0;
    }

    private static boolean tickCooldown(
            float deltaTimeSeconds,
            EliteMobsLeapAbilityStateComponent abilityState
    ) {
        if (abilityState.cooldownSeconds <= 0f) return false;

        float nextCooldownSeconds =
                Math.max(0f, abilityState.cooldownSeconds - deltaTimeSeconds);
        if (nextCooldownSeconds == abilityState.cooldownSeconds) return false;

        abilityState.cooldownSeconds = nextCooldownSeconds;
        return true;
    }

    private static void tickDebugSample(
            float deltaTimeSeconds,
            EliteMobsConfig config,
            EliteMobsLeapAbilityStateComponent abilityState,
            Store<EntityStore> entityStore,
            Ref<EntityStore> mobEntityRef
    ) {
        if (abilityState.debugSampleTimeLeft <= 0f) return;

        float nextSampleTime =
                Math.max(0f, abilityState.debugSampleTimeLeft - deltaTimeSeconds);
        if (nextSampleTime == abilityState.debugSampleTimeLeft) return;

        abilityState.debugSampleTimeLeft = nextSampleTime;

        if (!config.debugConfig.isDebugModeEnabled) return;

        Velocity velocity = getComponentSafe(entityStore, mobEntityRef, VELOCITY_COMPONENT_TYPE);
        TransformComponent transform = getComponentSafe(entityStore, mobEntityRef, TRANSFORM_COMPONENT_TYPE);
        if (velocity == null || transform == null) return;

        EliteMobsLogger.debug(
                LOGGER,
                "SAMPLE t=%.2f vel=(%.3f, %.3f, %.3f) yaw=%.1f pos=(%.2f, %.2f, %.2f)",
                EliteMobsLogLevel.INFO,
                abilityState.debugSampleTimeLeft,
                velocity.getX(),
                velocity.getY(),
                velocity.getZ(),
                (double) transform.getRotation().getYaw(),
                transform.getPosition().getX(),
                transform.getPosition().getY(),
                transform.getPosition().getZ()
        );
    }
}
