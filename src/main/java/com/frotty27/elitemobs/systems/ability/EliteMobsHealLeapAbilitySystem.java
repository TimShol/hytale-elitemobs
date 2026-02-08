package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.features.EliteMobsAbilityFeatureHelpers;
import com.frotty27.elitemobs.features.EliteMobsHealLeapAbilityFeature;
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
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;
import static com.frotty27.elitemobs.utils.Constants.TICKS_PER_SECOND;
import static com.frotty27.elitemobs.utils.StringHelpers.safeRoleName;

public class EliteMobsHealLeapAbilitySystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType HEAL_INTERACTION_TYPE = InteractionType.Ability2;

    public static final int HEAL_INTERRUPT_HIT_COUNT = 3;


    private final EliteMobsPlugin plugin;
    private final Random random = new Random();

    public EliteMobsHealLeapAbilitySystem(EliteMobsPlugin plugin) {
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
            tierComponent.healTriggerPercent = rollHealTriggerPercent(random, healConfig);
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
        
        if (config.debugConfig.isDebugModeEnabled) {
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
                EliteMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) return;
        if (AbilityHelpers.getRootInteraction(rootInteractionId) == null) {
            if (config.debugConfig.isDebugModeEnabled) {
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

        boolean started = EliteMobsAbilityFeatureHelpers.tryStartInteraction(
                npcRef,
                store,
                commandBuffer,
                HEAL_INTERACTION_TYPE,
                rootInteractionId
        );
        if (config.debugConfig.isDebugModeEnabled) {
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

        swapToPotionInHandIfPossible(npcEntity, config, tierComponent);

        long cooldownTicks = secondsToTicks(AssetConfigHelpers.getFloatForTier(healConfig.cooldownSecondsPerTier, tierIndex, 0f));
        tierComponent.nextHealAllowedTick = currentTick + cooldownTicks;
        tierComponent.healInProgress = true;
        tierComponent.healHitsTaken = 0;
        tierComponent.healSwapRestoreTick = currentTick + secondsToTicks(resolveHealChainDurationSeconds(healConfig));
        tierComponent.debugSampleTimeLeft = 2.0f;
        commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
    }

    public void onDamage(EliteMobsPlugin plugin, EliteMobsConfig config, Ref<EntityStore> victimRef,
                         Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                         EliteMobsTierComponent tierComponent, @Nullable NPCEntity npcEntity, int tierIndex,
                         long currentTick, Damage damage) {
        if (npcEntity == null) return;

        boolean healChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore,
                                                                          victimRef,
                                                                          HEAL_INTERACTION_TYPE
        );
        boolean changed = false;

        if (tierComponent.healInProgress && !healChainActive) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
            changed = true;
        } else if (!tierComponent.healInProgress && healChainActive) {
            tierComponent.healInProgress = true;
            tierComponent.healHitsTaken = 0;
            changed = true;
        }

        if (!tierComponent.healInProgress || !healChainActive) {
            if (changed) {
                commandBuffer.replaceComponent(victimRef, plugin.getEliteMobsComponent(), tierComponent);
            }
            return;
        }

        // Only interrupt if we are actually in the DRINKING phase (after leap)
        EliteMobsConfig.AbilityConfig healConfig = getHealLeapConfig(config);
        float drinkDuration = resolveHealDrinkDurationSeconds(healConfig);
        long drinkStartTick = tierComponent.healSwapRestoreTick - secondsToTicks(drinkDuration);

        if (currentTick < drinkStartTick) {
            return; // Too early to interrupt
        }

        tierComponent.healHitsTaken++;

        if (tierComponent.healHitsTaken >= HEAL_INTERRUPT_HIT_COUNT) {
            AbilityHelpers.cancelInteractionType(entityStore, commandBuffer, victimRef, HEAL_INTERACTION_TYPE);
            playHealBreakSound(config, npcEntity, victimRef, entityStore, commandBuffer, tierIndex);
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);

            long cooldownTicks = 0L;
            if (healConfig != null) {
                cooldownTicks = secondsToTicks(AssetConfigHelpers.getFloatForTier(healConfig.cooldownSecondsPerTier,
                                                                                  tierIndex,
                                                                                  0f
                ));
            }
            tierComponent.nextHealAllowedTick = currentTick + cooldownTicks;
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
        }

        commandBuffer.replaceComponent(victimRef, plugin.getEliteMobsComponent(), tierComponent);
    }

    private void playHealBreakSound(EliteMobsConfig config, NPCEntity npcEntity, Ref<EntityStore> victimRef,
                                    Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                    int tierIndex) {
        EliteMobsConfig.AbilityConfig healConfig = getHealLeapConfig(config);
        if (healConfig == null) return;
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(healConfig, tierIndex)) return;
        if (!AbilityGateEvaluator.isAllowed(healConfig, safeRoleName(npcEntity), "", tierIndex)) return;

        String rootInteractionId = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(config,
                                                                                      healConfig,
                                                                                      EliteMobsConfig.HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL,
                                                                                      tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) return;

        EliteMobsAbilityFeatureHelpers.tryStartInteraction(victimRef,
                                                              entityStore,
                                                              commandBuffer,
                                                              HEAL_INTERACTION_TYPE,
                                                              rootInteractionId
        );
    }

    private static EliteMobsConfig.AbilityConfig getHealLeapConfig(EliteMobsConfig config) {
        return (EliteMobsConfig.AbilityConfig) AssetConfigHelpers.getAssetConfig(config,
                                                                                 AssetType.ABILITIES,
                                                                                 EliteMobsHealLeapAbilityFeature.ABILITY_HEAL_LEAP
        );
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

        if (!config.debugConfig.isDebugModeEnabled) return;

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
                EliteMobsHealLeapAbilityFeature.ABILITY_HEAL_LEAP
        );
    }


    private static long secondsToTicks(float seconds) {
        if (seconds <= 0f) return 0L;
        return Math.max(1L, (long) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    private static float resolveHealChainDurationSeconds(EliteMobsConfig.AbilityConfig healConfig) {
        return resolveHealDrinkDurationSeconds(healConfig);
    }

    static void swapToPotionInHandIfPossible(NPCEntity npcEntity, EliteMobsConfig config,
                                             EliteMobsTierComponent tierComponent) {
        if (npcEntity == null || config == null || tierComponent == null) return;
        if (tierComponent.healSwapActive) return;

        EliteMobsConfig.AbilityConfig healConfig = getHealLeapConfig(config);
        if (!(healConfig instanceof EliteMobsConfig.HealLeapAbilityConfig healLeapAbilityConfig)) return;

        String potionItemId = healLeapAbilityConfig.npcDrinkItemId;
        if (potionItemId == null || potionItemId.isBlank()) return;
        if (Item.getAssetMap().getAsset(potionItemId) == null) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = inventory.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        ItemStack current;
        try {
            current = inventory.getHotbar().getItemStack((short) slot);
        } catch (Throwable ignored) {
            return;
        }
        tierComponent.healSwapPreviousItem = current;
        tierComponent.healSwapSlot = slot;
        tierComponent.healSwapActive = true;

        inventory.getHotbar().setItemStackForSlot(slot, new ItemStack(potionItemId, 1));
        inventory.markChanged();
    }

    private static float rollHealTriggerPercent(Random random, EliteMobsConfig.AbilityConfig healConfig) {
        float min = 0.1f;
        float max = 0.4f;
        if (healConfig instanceof EliteMobsConfig.HealLeapAbilityConfig healLeapAbilityConfig) {
            min = healLeapAbilityConfig.minHealthTriggerPercent;
            max = healLeapAbilityConfig.maxHealthTriggerPercent;
        }
        return AbilityHelpers.rollPercentInRange(random, min, max, 0.5f);
    }

    private static float resolveHealDrinkDurationSeconds(EliteMobsConfig.AbilityConfig healConfig) {
        if (healConfig instanceof EliteMobsConfig.HealLeapAbilityConfig healLeapAbilityConfig) {
            return healLeapAbilityConfig.npcDrinkDurationSeconds;
        }
        return 0f;
    }
}
