package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.*;
import com.frotty27.rpgmobs.assets.TemplateNameGenerator;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.RPGMobsAbilityFeatureHelpers;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.frotty27.rpgmobs.utils.Constants;
import com.frotty27.rpgmobs.utils.StoreHelpers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsAbilityTriggerListener implements IRPGMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final RPGMobsPlugin plugin;

    public RPGMobsAbilityTriggerListener(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reevaluateAbilitiesForCombatEntity(Ref<EntityStore> entityRef) {
        evaluateAbilitiesForEntity(entityRef, AbilityTriggerSource.AGGRO);
    }

    @Override
    public void onRPGMobAggro(RPGMobsAggroEvent event) {
        RPGMobsLogger.debug(LOGGER, "[Aggro] Mob aggro'd tier=%d", RPGMobsLogLevel.INFO, event.getTier());
        evaluateAbilitiesForEntity(event.getEntityRef(), AbilityTriggerSource.AGGRO);
    }

    @Override
    public void onRPGMobDamageReceived(RPGMobsDamageReceivedEvent event) {
        if (checkHealLeapInterrupt(event)) return;

        evaluateAbilitiesForEntity(event.getEntityRef(), AbilityTriggerSource.DAMAGE_RECEIVED);
    }

    @Override
    public void onRPGMobAbilityCompleted(RPGMobsAbilityCompletedEvent event) {
        handleAbilityCompletionRetrigger(event);
    }

    @Override
    public void onRPGMobDeath(RPGMobsDeathEvent event) {
        handleDeathInterrupt(event);
    }

    @Override
    public void onRPGMobDeaggro(RPGMobsDeaggroEvent event) {
        handleDeaggroInterrupt(event);
    }

    private void evaluateAbilitiesForEntity(Ref<EntityStore> entityRef, AbilityTriggerSource source) {
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        // Dead mobs cannot use abilities
        DeathComponent death = store.getComponent(entityRef, DeathComponent.getComponentType());
        if (death != null) return;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityEval] SKIP: lock active (locked=%b pending=%b ability=%s) source=%s",
                                RPGMobsLogLevel.INFO,
                                lock.isLocked(),
                                lock.isChainStartPending(),
                                lock.activeAbilityId,
                                source.name()
            );
            return;
        }

        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        int tierIndex = clampTierIndex(tier.tierIndex);

        if (source == AbilityTriggerSource.DAMAGE_RECEIVED) {
            if (tryTriggerHealLeap(entityRef, store, config, tierIndex)) return;
        }

        if (source == AbilityTriggerSource.AGGRO) {
            if (tryTriggerChargeLeap(entityRef, store, config, tierIndex)) return;
            tryTriggerSummonUndead(entityRef, store, config, tierIndex);
        }
    }

    private boolean tryTriggerHealLeap(Ref<EntityStore> entityRef, Store<EntityStore> store, RPGMobsConfig config,
                                       int tierIndex) {
        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null || !healLeap.abilityEnabled) return false;

        if (healLeap.cooldownTicksRemaining > 0) return false;

        float healthPercent = calculateHealthPercent(entityRef, store);
        if (healthPercent >= healLeap.triggerHealthPercent) return false;

        Ref<EntityStore> targetRef = getAggroTarget(entityRef, store);
        RPGMobsAbilityStartedEvent startedEvent = new RPGMobsAbilityStartedEvent(entityRef,
                                                                                 AbilityIds.HEAL_LEAP,
                                                                                 tierIndex,
                                                                                 targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return false;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.HEAL_LEAP, tierIndex, config);
        if (!started) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] heal_leap chain failed to start for tier %d",
                                RPGMobsLogLevel.WARNING,
                                tierIndex
            );
            return false;
        }

        RPGMobsConfig.HealLeapAbilityConfig abilityConfig = getHealLeapConfig(config);
        if (abilityConfig != null) {
            healLeap.cooldownTicksRemaining = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        }

        lockAbility(entityRef, store, AbilityIds.HEAL_LEAP);

        RPGMobsLogger.debug(LOGGER,
                            "[AbilityTrigger] heal_leap triggered at %.0f%% health (threshold=%.0f%%) tier=%d",
                            RPGMobsLogLevel.INFO,
                            healthPercent * 100f,
                            healLeap.triggerHealthPercent * 100f,
                            tierIndex
        );

        return true;
    }

    private boolean tryTriggerChargeLeap(Ref<EntityStore> entityRef, Store<EntityStore> store, RPGMobsConfig config,
                                         int tierIndex) {
        ChargeLeapAbilityComponent chargeLeap = store.getComponent(entityRef,
                                                                   plugin.getChargeLeapAbilityComponentType()
        );
        if (chargeLeap == null || !chargeLeap.abilityEnabled) return false;

        if (chargeLeap.cooldownTicksRemaining > 0) {
            RPGMobsLogger.debug(LOGGER,
                                "[ChargeLeap] BLOCKED by cooldown: remaining=%d ticks (%.1f sec)",
                                RPGMobsLogLevel.INFO,
                                chargeLeap.cooldownTicksRemaining,
                                chargeLeap.cooldownTicksRemaining / (float) Constants.TICKS_PER_SECOND
            );
            return false;
        }

        RPGMobsCombatTrackingComponent combat = store.getComponent(entityRef, plugin.getCombatTrackingComponentType());
        if (combat == null || !combat.isInCombat()) {
            RPGMobsLogger.debug(LOGGER,
                                "[ChargeLeap] BLOCKED: mob not in combat (state=%s)",
                                RPGMobsLogLevel.INFO,
                                combat != null ? combat.state.name() : "null"
            );
            return false;
        }

        Ref<EntityStore> targetRef = combat.getBestTarget();
        if (targetRef == null || !targetRef.isValid()) {
            RPGMobsLogger.debug(LOGGER, "[ChargeLeap] BLOCKED: no valid target", RPGMobsLogLevel.INFO);
            return false;
        }

        if (combat.aiTarget == null || !combat.aiTarget.isValid()) {
            RPGMobsLogger.debug(LOGGER,
                                "[ChargeLeap] BLOCKED: no AI target (mob may be retreating, damage target still set)",
                                RPGMobsLogLevel.INFO
            );
            return false;
        }

        RPGMobsConfig.ChargeLeapAbilityConfig abilityConfig = getChargeLeapConfig(config);
        if (abilityConfig == null) return false;

        float distance = calculateDistance(entityRef, targetRef, store);
        if (distance < abilityConfig.minRange || distance > abilityConfig.maxRange) {
            RPGMobsLogger.debug(LOGGER,
                                "[ChargeLeap] BLOCKED by distance: dist=%.1f (range=%.1f-%.1f)",
                                RPGMobsLogLevel.INFO,
                                distance,
                                abilityConfig.minRange,
                                abilityConfig.maxRange
            );
            return false;
        }

        RPGMobsAbilityStartedEvent startedEvent = new RPGMobsAbilityStartedEvent(entityRef,
                                                                                 AbilityIds.CHARGE_LEAP,
                                                                                 tierIndex,
                                                                                 targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return false;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.CHARGE_LEAP, tierIndex, config);
        if (!started) {
            RPGMobsLogger.debug(LOGGER,
                                "[ChargeLeap] chain failed to start for tier %d",
                                RPGMobsLogLevel.INFO,
                                tierIndex
            );
            return false;
        }

        long cooldownTicks = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        chargeLeap.cooldownTicksRemaining = cooldownTicks;

        lockAbility(entityRef, store, AbilityIds.CHARGE_LEAP);

        RPGMobsLogger.debug(LOGGER,
                            "[ChargeLeap] TRIGGERED: dist=%.1f (range=%.1f-%.1f) tier=%d cooldown=%d ticks (%.1f sec)",
                            RPGMobsLogLevel.INFO,
                            distance,
                            abilityConfig.minRange,
                            abilityConfig.maxRange,
                            tierIndex,
                            cooldownTicks,
                            cooldownTicks / (float) Constants.TICKS_PER_SECOND
        );

        return true;
    }

    private static final long SUMMON_SPAWN_DELAY_TICKS = 66;

    private void tryTriggerSummonUndead(Ref<EntityStore> entityRef, Store<EntityStore> store, RPGMobsConfig config,
                                        int tierIndex) {
        SummonUndeadAbilityComponent summon = store.getComponent(entityRef,
                                                                 plugin.getSummonUndeadAbilityComponentType()
        );
        if (summon == null || !summon.abilityEnabled) return;

        if (summon.cooldownTicksRemaining > 0) return;

        RPGMobsConfig.SummonAbilityConfig abilityConfig = getSummonConfig(config);
        if (abilityConfig == null) return;

        RPGMobsSummonMinionTrackingComponent tracking = store.getComponent(entityRef,
                                                                           plugin.getSummonMinionTrackingComponentType()
        );
        if (tracking != null) {
            int maxAlive = Math.max(0, Math.min(50, abilityConfig.maxAlive));
            if (!tracking.canSummonMore(maxAlive)) {
                RPGMobsLogger.debug(LOGGER,
                                    "[SummonUndead] BLOCKED: cap reached alive=%d max=%d",
                                    RPGMobsLogLevel.INFO,
                                    tracking.summonedAliveCount,
                                    maxAlive
                );
                return;
            }
        }

        String roleIdentifier = resolveSummonRole(entityRef, store, config);

        Ref<EntityStore> targetRef = getAggroTarget(entityRef, store);
        RPGMobsAbilityStartedEvent startedEvent = new RPGMobsAbilityStartedEvent(entityRef,
                                                                                 AbilityIds.SUMMON_UNDEAD,
                                                                                 tierIndex,
                                                                                 targetRef
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return;

        boolean started = startAbilityChain(entityRef, store, AbilityIds.SUMMON_UNDEAD, tierIndex, config);
        if (!started) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] summon_undead chain failed to start for tier %d",
                                RPGMobsLogLevel.WARNING,
                                tierIndex
            );
            return;
        }

        summon.pendingSummonRole = roleIdentifier;
        summon.pendingSummonTicksRemaining = SUMMON_SPAWN_DELAY_TICKS;

        long cooldownTicks = getCooldownTicks(abilityConfig.cooldownSecondsPerTier, tierIndex);
        summon.cooldownTicksRemaining = cooldownTicks;

        lockAbility(entityRef, store, AbilityIds.SUMMON_UNDEAD);

        RPGMobsLogger.debug(LOGGER,
                            "[SummonUndead] TRIGGERED: tier=%d role=%s spawnDelay=%d cooldown=%d ticks",
                            RPGMobsLogLevel.INFO,
                            tierIndex,
                            roleIdentifier,
                            SUMMON_SPAWN_DELAY_TICKS,
                            cooldownTicks
        );

    }

    private void handleAbilityCompletionRetrigger(RPGMobsAbilityCompletedEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        String abilityId = event.getAbilityId();

        if (AbilityIds.CHARGE_LEAP.equals(abilityId)) {
            ChargeLeapAbilityComponent chargeLeap = store.getComponent(entityRef,
                                                                       plugin.getChargeLeapAbilityComponentType()
            );
            if (chargeLeap != null) {
                RPGMobsLogger.debug(LOGGER,
                                    "[ChargeLeap] COMPLETED: cooldownRemaining=%d ticks (%.1f sec)",
                                    RPGMobsLogLevel.INFO,
                                    chargeLeap.cooldownTicksRemaining,
                                    chargeLeap.cooldownTicksRemaining / (float) Constants.TICKS_PER_SECOND
                );
            }
        }

        unlockAbility(entityRef, store);

        if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
            RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
            if (tier != null && tier.tierIndex >= 0) {
                RPGMobsCombatTrackingComponent combat = store.getComponent(entityRef,
                                                                           plugin.getCombatTrackingComponentType()
                );
                if (combat != null && combat.isInCombat()) {
                    RPGMobsConfig config = plugin.getConfig();
                    if (config != null) {
                        int tierIndex = clampTierIndex(tier.tierIndex);
                        tryTriggerSummonUndead(entityRef, store, config, tierIndex);
                    }
                }
            }
        }
    }

    private void handleDeathInterrupt(RPGMobsDeathEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        String activeAbilityId = lock.activeAbilityId;
        int tierIndex = clampTierIndex(event.getTier());

        RPGMobsAbilityInterruptedEvent interruptedEvent = new RPGMobsAbilityInterruptedEvent(entityRef,
                                                                                             activeAbilityId,
                                                                                             tierIndex,
                                                                                             "death"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        RPGMobsLogger.debug(LOGGER,
                            "[AbilityTrigger] %s interrupted by death tier=%d",
                            RPGMobsLogLevel.INFO,
                            activeAbilityId,
                            tierIndex
        );
    }

    private void handleDeaggroInterrupt(RPGMobsDeaggroEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        RPGMobsLogger.debug(LOGGER, "[Deaggro] Mob deaggro'd tier=%d", RPGMobsLogLevel.INFO, event.getTier());

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        String activeAbilityId = lock.activeAbilityId;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsAbilityInterruptedEvent interruptedEvent = new RPGMobsAbilityInterruptedEvent(entityRef,
                                                                                             activeAbilityId,
                                                                                             tierIndex,
                                                                                             "deaggro"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        RPGMobsLogger.debug(LOGGER,
                            "[AbilityTrigger] %s interrupted by deaggro tier=%d",
                            RPGMobsLogLevel.INFO,
                            activeAbilityId,
                            tierIndex
        );
    }

    private static final int HEAL_LEAP_INTERRUPT_HITS = 3;

    private boolean checkHealLeapInterrupt(RPGMobsDamageReceivedEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return false;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return false;

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return false;
        if (!AbilityIds.HEAL_LEAP.equals(lock.activeAbilityId)) return false;

        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null) return false;

        healLeap.hitsTaken++;
        RPGMobsLogger.debug(LOGGER,
                            "[HealLeap] Hit taken during ability: hitsTaken=%d/%d",
                            RPGMobsLogLevel.INFO,
                            healLeap.hitsTaken,
                            HEAL_LEAP_INTERRUPT_HITS
        );

        if (healLeap.hitsTaken < HEAL_LEAP_INTERRUPT_HITS) {
            return true;
        }

        RPGMobsLogger.debug(LOGGER,
                            "[HealLeap] INTERRUPTED by %d hits, cancelling chain",
                            RPGMobsLogLevel.INFO,
                            healLeap.hitsTaken
        );

        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity != null) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, healLeap);
        }

        healLeap.hitsTaken = 0;

        RPGMobsConfig config = plugin.getConfig();
        RPGMobsTierComponent tier2 = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int cancelTierIndex = (tier2 != null) ? clampTierIndex(tier2.tierIndex) : 0;

        if (npcEntity != null && npcEntity.getWorld() != null) {
            npcEntity.getWorld().execute(() -> {
                if (!entityRef.isValid()) return;
                var entityStoreProvider = npcEntity.getWorld().getEntityStore();
                if (entityStoreProvider == null) return;
                Store<EntityStore> worldStore = entityStoreProvider.getStore();
                StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                                            String cancelRootId = resolveCancelRootInteractionId(config, cancelTierIndex);
                                            if (cancelRootId != null) {
                                                RPGMobsAbilityFeatureHelpers.tryStartInteraction(entityRef,
                                                                                                 worldStore,
                                                                                                 commandBuffer,
                                                                                                 InteractionType.Ability2,
                                                                                                 cancelRootId
                                                );
                                            } else {
                                                AbilityHelpers.cancelInteractionType(worldStore,
                                                                                     commandBuffer,
                                                                                     entityRef,
                                                                                     InteractionType.Ability2
                                                );
                                            }
                                        }
                );
            });
        }

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsAbilityInterruptedEvent interruptedEvent = new RPGMobsAbilityInterruptedEvent(entityRef,
                                                                                             AbilityIds.HEAL_LEAP,
                                                                                             tierIndex,
                                                                                             "hit_cancel"
        );
        plugin.getEventBus().fire(interruptedEvent);

        unlockAbility(entityRef, store);

        return true;
    }

    private boolean startAbilityChain(Ref<EntityStore> entityRef, Store<EntityStore> store, String abilityId,
                                      int tierIndex, RPGMobsConfig config) {
        RPGMobsConfig.AbilityConfig abilityConfig = getAbilityConfig(config, abilityId);
        if (abilityConfig == null) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] No AbilityConfig found for abilityId=%s",
                                RPGMobsLogLevel.WARNING,
                                abilityId
            );
            return false;
        }

        String rootInteractionTemplatePath = abilityConfig.templates.getTemplate(RPGMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION);
        if (rootInteractionTemplatePath == null || rootInteractionTemplatePath.isBlank()) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] No rootInteraction template for abilityId=%s",
                                RPGMobsLogLevel.WARNING,
                                abilityId
            );
            return false;
        }

        String rootInteractionId = TemplateNameGenerator.getTemplateNameWithTierFromPath(rootInteractionTemplatePath,
                                                                                         config,
                                                                                         tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] Failed to resolve root interaction id for abilityId=%s tier=%d",
                                RPGMobsLogLevel.WARNING,
                                abilityId,
                                tierIndex
            );
            return false;
        }

        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null || npcEntity.getWorld() == null) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityTrigger] NPCEntity or World is null for abilityId=%s",
                                RPGMobsLogLevel.WARNING,
                                abilityId
            );
            return false;
        }

        final String resolvedRootId = rootInteractionId;
        npcEntity.getWorld().execute(() -> {
            if (!entityRef.isValid()) return;

            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> worldStore = entityStoreProvider.getStore();

            StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                                        RPGMobsAbilityLockComponent lock = worldStore.getComponent(entityRef,
                                                                                                   plugin.getAbilityLockComponentType()
                                        );

                                        if (AbilityIds.HEAL_LEAP.equals(abilityId)) {
                                            performHealLeapWeaponSwap(entityRef, worldStore, npcEntity);
                                        }
                                        if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
                                            performSummonSpellbookSwap(entityRef, worldStore, npcEntity);
                                        }

                                        boolean started;
                                        try {
                                            started = RPGMobsAbilityFeatureHelpers.tryStartInteraction(entityRef,
                                                                                                       worldStore,
                                                                                                       commandBuffer,
                                                                                                       InteractionType.Ability2,
                                                                                                       resolvedRootId
                                            );
                                        } catch (Exception e) {
                                            LOGGER.atWarning().log("[AbilityTrigger] Chain start threw exception for rootId=%s: %s",
                                                                   resolvedRootId,
                                                                   e.getMessage()
                                            );
                                            started = false;
                                        }

                                        if (!started) {
                                            if (lock != null && lock.isLocked()) {
                                                lock.unlock();
                                                commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
                                            }
                                            if (AbilityIds.HEAL_LEAP.equals(abilityId)) {
                                                AbilityHelpers.restorePreviousItemIfNeeded(npcEntity,
                                                                                           worldStore.getComponent(entityRef,
                                                                                                                   plugin.getHealLeapAbilityComponentType()
                                                                                           )
                                                );
                                            }
                                            if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
                                                AbilityHelpers.restoreSummonWeaponIfNeeded(npcEntity,
                                                                                           worldStore.getComponent(entityRef,
                                                                                                                   plugin.getSummonUndeadAbilityComponentType()
                                                                                           )
                                                );
                                            }
                                            RPGMobsLogger.debug(LOGGER,
                                                                "[AbilityTrigger] Deferred chain start failed for rootId=%s",
                                                                RPGMobsLogLevel.WARNING,
                                                                resolvedRootId
                                            );
                                        } else {
                                            if (lock != null) {
                                                lock.markChainStarted(plugin.getTickClock().getTick());
                                                commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
                                            }

                                            RPGMobsLogger.debug(LOGGER,
                                                                "[AbilityTrigger] Deferred chain started for rootId=%s tick=%d",
                                                                RPGMobsLogLevel.INFO,
                                                                resolvedRootId,
                                                                plugin.getTickClock().getTick()
                                            );
                                        }
                                    }
            );
        });

        return true;
    }

    private float calculateHealthPercent(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStats == null) return 1.0f;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return 1.0f;

        float current = healthStatValue.get();
        float max = healthStatValue.getMax();
        if (max <= 0) return 1.0f;

        return current / max;
    }

    private float calculateDistance(Ref<EntityStore> entityRef, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        TransformComponent mobTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());

        if (mobTransform == null || targetTransform == null) return Float.MAX_VALUE;

        Vector3d mobPos = mobTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();

        double dx = targetPos.getX() - mobPos.getX();
        double dy = targetPos.getY() - mobPos.getY();
        double dz = targetPos.getZ() - mobPos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private @Nullable Ref<EntityStore> getAggroTarget(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        RPGMobsCombatTrackingComponent combat = store.getComponent(entityRef, plugin.getCombatTrackingComponentType());
        if (combat == null) return null;
        return combat.getBestTarget();
    }

    private void lockAbility(Ref<EntityStore> entityRef, Store<EntityStore> store, String abilityId) {
        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null) {
            lock.lock(abilityId);
        }
    }

    private void unlockAbility(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null) {
            lock.unlock();
        }
    }

    private long getCooldownTicks(float[] cooldownSecondsPerTier, int tierIndex) {
        if (cooldownSecondsPerTier == null || tierIndex < 0 || tierIndex >= cooldownSecondsPerTier.length) {
            return 0L;
        }
        float seconds = cooldownSecondsPerTier[tierIndex];
        if (seconds <= 0f) return 0L;
        return (long) (seconds * Constants.TICKS_PER_SECOND);
    }

    private void performHealLeapWeaponSwap(Ref<EntityStore> entityRef, Store<EntityStore> store, NPCEntity npcEntity) {
        HealLeapAbilityComponent healLeap = store.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (healLeap == null) return;

        RPGMobsConfig config = plugin.getConfig();
        RPGMobsConfig.HealLeapAbilityConfig healConfig = getHealLeapConfig(config);
        if (healConfig == null) return;

        String potionItemId = healConfig.npcDrinkItemId;
        if (potionItemId == null || potionItemId.isBlank()) {
            potionItemId = "Potion_Health_Greater";
        }

        boolean swapped = AbilityHelpers.swapToPotionInHand(npcEntity, healLeap, potionItemId);
        if (swapped) {
            RPGMobsLogger.debug(LOGGER, "[HealLeap] Weapon swapped to potion '%s'", RPGMobsLogLevel.INFO, potionItemId);
        } else {
            RPGMobsLogger.debug(LOGGER,
                                "[HealLeap] Weapon swap failed (itemId=%s)",
                                RPGMobsLogLevel.INFO,
                                potionItemId
            );
        }
    }

    private void performSummonSpellbookSwap(Ref<EntityStore> entityRef, Store<EntityStore> store, NPCEntity npcEntity) {
        SummonUndeadAbilityComponent summon = store.getComponent(entityRef,
                                                                 plugin.getSummonUndeadAbilityComponentType()
        );
        if (summon == null) return;

        String staffItemId = "Weapon_Staff_Bone";

        boolean swapped = AbilityHelpers.swapToSpellbookInHand(npcEntity, summon, staffItemId);
        if (swapped) {
            RPGMobsLogger.debug(LOGGER,
                                "[SummonUndead] Weapon swapped to staff '%s'",
                                RPGMobsLogLevel.INFO,
                                staffItemId
            );
        } else {
            RPGMobsLogger.debug(LOGGER,
                                "[SummonUndead] Weapon swap failed (itemId=%s)",
                                RPGMobsLogLevel.INFO,
                                staffItemId
            );
        }
    }

    private RPGMobsConfig.@Nullable AbilityConfig getAbilityConfig(RPGMobsConfig config, String abilityId) {
        if (config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return null;
        return config.abilitiesConfig.defaultAbilities.get(abilityId);
    }

    private RPGMobsConfig.@Nullable ChargeLeapAbilityConfig getChargeLeapConfig(RPGMobsConfig config) {
        RPGMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.CHARGE_LEAP);
        return (raw instanceof RPGMobsConfig.ChargeLeapAbilityConfig c) ? c : null;
    }

    private RPGMobsConfig.@Nullable HealLeapAbilityConfig getHealLeapConfig(RPGMobsConfig config) {
        RPGMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.HEAL_LEAP);
        return (raw instanceof RPGMobsConfig.HealLeapAbilityConfig c) ? c : null;
    }

    private RPGMobsConfig.@Nullable SummonAbilityConfig getSummonConfig(RPGMobsConfig config) {
        RPGMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.SUMMON_UNDEAD);
        return (raw instanceof RPGMobsConfig.SummonAbilityConfig c) ? c : null;
    }

    private @NonNull String resolveSummonRole(Ref<EntityStore> entityRef, Store<EntityStore> store,
                                              RPGMobsConfig config) {
        NPCEntity npc = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npc == null) return "default";

        String roleName = npc.getRoleName();
        if (roleName == null || roleName.isBlank()) return "default";

        RPGMobsConfig.SummonAbilityConfig summonConfig = getSummonConfig(config);
        if (summonConfig == null || summonConfig.roleIdentifiers == null) return "default";

        String roleNameLower = roleName.toLowerCase(Locale.ROOT);
        for (String identifier : summonConfig.roleIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            if (roleNameLower.contains(identifier.toLowerCase(Locale.ROOT))) {
                return identifier;
            }
        }
        return "default";
    }

    private @Nullable String resolveCancelRootInteractionId(RPGMobsConfig config, int tierIndex) {
        if (config == null) return null;
        RPGMobsConfig.HealLeapAbilityConfig healConfig = getHealLeapConfig(config);
        if (healConfig == null) return null;
        String cancelTemplatePath = healConfig.templates.getTemplate(RPGMobsConfig.HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL);
        if (cancelTemplatePath == null || cancelTemplatePath.isBlank()) return null;
        return TemplateNameGenerator.getTemplateNameWithTierFromPath(cancelTemplatePath, config, tierIndex);
    }
}

enum AbilityTriggerSource {
    AGGRO, DAMAGE_RECEIVED
}
