package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.*;
import com.frotty27.rpgmobs.assets.TemplateNameGenerator;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.*;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.IRPGMobsAbilityFeature;
import com.frotty27.rpgmobs.features.RPGMobsAbilityFeatureHelpers;
import com.frotty27.rpgmobs.features.RPGMobsFeatureRegistry;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.frotty27.rpgmobs.utils.Constants;
import com.frotty27.rpgmobs.utils.StoreHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsAbilityTriggerListener implements IRPGMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long SUMMON_SPAWN_DELAY_TICKS = 66;

    private final RPGMobsPlugin plugin;
    private final Map<String, ComponentType<EntityStore, ?>> abilityComponentTypes;

    public RPGMobsAbilityTriggerListener(RPGMobsPlugin plugin) {
        this.plugin = plugin;
        this.abilityComponentTypes = Map.ofEntries(
                Map.entry(AbilityIds.CHARGE_LEAP, plugin.getChargeLeapAbilityComponentType()),
                Map.entry(AbilityIds.HEAL_LEAP, plugin.getHealLeapAbilityComponentType()),
                Map.entry(AbilityIds.SUMMON_UNDEAD, plugin.getSummonUndeadAbilityComponentType()),
                Map.entry(AbilityIds.DODGE_ROLL, plugin.getDodgeRollAbilityComponentType()),
                Map.entry(AbilityIds.MULTI_SLASH_SHORT, plugin.getMultiSlashShortComponentType()),
                Map.entry(AbilityIds.MULTI_SLASH_MEDIUM, plugin.getMultiSlashMediumComponentType()),
                Map.entry(AbilityIds.MULTI_SLASH_LONG, plugin.getMultiSlashLongComponentType()),
                Map.entry(AbilityIds.ENRAGE, plugin.getEnrageAbilityComponentType()),
                Map.entry(AbilityIds.VOLLEY, plugin.getVolleyAbilityComponentType())
        );
    }

    public void reevaluateAbilitiesForCombatEntity(Ref<EntityStore> entityRef) {
        evaluateAbilitiesForEntity(entityRef, AbilityTriggerSource.AGGRO);
    }

    @Override
    public void onRPGMobAggro(RPGMobsAggroEvent event) {
        RPGMobsLogger.debug(LOGGER, "Mob aggro'd tier=%d", RPGMobsLogLevel.INFO, event.getTier());
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

    public void evaluateAbilitiesForEntity(Ref<EntityStore> entityRef, AbilityTriggerSource source) {
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        DeathComponent death = store.getComponent(entityRef, DeathComponent.getComponentType());
        if (death != null) return;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        boolean interruptingAbility = false;
        boolean isReactiveTrigger = source == AbilityTriggerSource.PLAYER_ATTACK_NEARBY
                || source == AbilityTriggerSource.PLAYER_CHARGED_ATTACK_NEARBY
                || source == AbilityTriggerSource.DAMAGE_RECEIVED;
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) {
            boolean hasActiveAbility = lock.activeAbilityId != null;
            boolean onlyGlobalCooldown = !hasActiveAbility && lock.isOnGlobalCooldown();
            if (isReactiveTrigger && onlyGlobalCooldown) {
                // Reactive abilities bypass global cooldown - they're responses to player actions
            } else if (isReactiveTrigger && hasActiveAbility
                    && AbilityIds.MULTI_SLASH_SHORT.equals(lock.activeAbilityId)) {
                interruptingAbility = true;
            } else {
                return;
            }
        }

        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        int tierIndex = clampTierIndex(tier.tierIndex);
        Ref<EntityStore> targetRef = getAggroTarget(entityRef, store);

        TriggerContext context = new TriggerContext(
                plugin, entityRef, store, config, null, tierIndex, source, targetRef
        );

        RPGMobsFeatureRegistry registry = RPGMobsFeatureRegistry.getInstance();
        if (registry == null) return;

        for (IRPGMobsAbilityFeature feature : registry.getAbilityFeatures()) {
            if (!feature.triggerSources().contains(source)) continue;
            if (!feature.canTrigger(context)) continue;
            if (tryTriggerAbility(context, feature, interruptingAbility)) return;
        }
    }

    private boolean tryTriggerAbility(TriggerContext context, IRPGMobsAbilityFeature feature,
                                      boolean interruptingAbility) {
        String abilityId = feature.id();

        RPGMobsAbilityStartedEvent startedEvent = new RPGMobsAbilityStartedEvent(
                context.entityRef(), abilityId, context.tierIndex(), context.targetRef()
        );
        plugin.getEventBus().fire(startedEvent);
        if (startedEvent.isCancelled()) return false;

        boolean started = startAbilityChain(context, feature, interruptingAbility);
        if (!started) {
            RPGMobsLogger.debug(LOGGER,
                    "%s chain failed to start for tier %d",
                    RPGMobsLogLevel.WARNING, abilityId, context.tierIndex());
            return false;
        }

        RPGMobsLogger.debug(LOGGER,
                "%s TRIGGERED tier=%d",
                RPGMobsLogLevel.INFO, abilityId, context.tierIndex());

        return true;
    }

    private boolean startAbilityChain(TriggerContext context, IRPGMobsAbilityFeature feature,
                                      boolean interruptingAbility) {
        String abilityId = feature.id();
        RPGMobsConfig config = context.config();
        int tierIndex = context.tierIndex();

        RPGMobsConfig.AbilityConfig abilityConfig = getAbilityConfig(config, abilityId);
        if (abilityConfig == null) {
            RPGMobsLogger.debug(LOGGER,
                    "No AbilityConfig found for abilityId=%s",
                    RPGMobsLogLevel.WARNING, abilityId);
            return false;
        }

        String templateKey = feature.resolveRootTemplateKey(context);
        String rootTemplatePath = abilityConfig.templates.getTemplate(templateKey);
        if (rootTemplatePath == null || rootTemplatePath.isBlank()) {
            rootTemplatePath = abilityConfig.templates.getTemplate(
                    RPGMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION);
        }
        if (rootTemplatePath == null || rootTemplatePath.isBlank()) {
            RPGMobsLogger.debug(LOGGER,
                    "No rootInteraction template for abilityId=%s key=%s",
                    RPGMobsLogLevel.WARNING, abilityId, templateKey);
            return false;
        }

        String rootInteractionId = TemplateNameGenerator.getTemplateNameWithTierFromPath(
                rootTemplatePath, config, tierIndex);
        if (rootInteractionId == null || rootInteractionId.isBlank()) {
            RPGMobsLogger.debug(LOGGER,
                    "Failed to resolve root interaction id for abilityId=%s tier=%d",
                    RPGMobsLogLevel.WARNING, abilityId, tierIndex);
            return false;
        }

        NPCEntity npcEntity = context.store().getComponent(context.entityRef(), NPC_COMPONENT_TYPE);
        if (npcEntity == null || npcEntity.getWorld() == null) {
            RPGMobsLogger.debug(LOGGER,
                    "NPCEntity or World is null for abilityId=%s",
                    RPGMobsLogLevel.WARNING, abilityId);
            return false;
        }

        RPGMobsAbilityLockComponent evalLock = context.store().getComponent(
                context.entityRef(), plugin.getAbilityLockComponentType());
        if (evalLock != null) {
            evalLock.lock(abilityId);
        }

        final String resolvedRootId = rootInteractionId;
        npcEntity.getWorld().execute(() -> {
            if (!context.entityRef().isValid()) return;

            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> worldStore = entityStoreProvider.getStore();

            TriggerContext deferredCtx = new TriggerContext(
                    context.plugin(), context.entityRef(), worldStore, context.config(),
                    context.resolved(), context.tierIndex(), context.source(),
                    context.targetRef()
            );

            StoreHelpers.withEntity(worldStore, context.entityRef(), (_, commandBuffer, _) -> {
                RPGMobsAbilityLockComponent lock = worldStore.getComponent(
                        context.entityRef(), plugin.getAbilityLockComponentType());

                if (!interruptingAbility && lock != null
                        && lock.activeAbilityId != null && !lock.isChainStartPending()) {
                    RPGMobsLogger.debug(LOGGER,
                            "Deferred %s aborted - chain already running for %s",
                            RPGMobsLogLevel.INFO, abilityId, lock.activeAbilityId);
                    return;
                }

                if (interruptingAbility && lock != null && lock.activeAbilityId != null) {
                    RPGMobsLogger.debug(LOGGER,
                            "Interrupting %s with %s",
                            RPGMobsLogLevel.INFO, lock.activeAbilityId, abilityId);
                    AbilityHelpers.cancelInteractionType(worldStore, commandBuffer,
                            context.entityRef(), InteractionType.Ability2);
                    plugin.getEventBus().fire(new RPGMobsAbilityInterruptedEvent(
                            context.entityRef(), lock.activeAbilityId, context.tierIndex(),
                            "dodge_interrupt"));
                    lock.unlock();
                }

                feature.onPreChainStart(deferredCtx, npcEntity);

                boolean chainStarted;
                try {
                    chainStarted = RPGMobsAbilityFeatureHelpers.tryStartInteraction(
                            context.entityRef(), worldStore, commandBuffer,
                            InteractionType.Ability2, resolvedRootId);
                } catch (Exception e) {
                    LOGGER.atWarning().log(
                            "Chain start threw exception for rootId=%s: %s",
                            resolvedRootId, e.getMessage());
                    chainStarted = false;
                }

                if (!chainStarted) {
                    feature.onChainStartFailed(deferredCtx, npcEntity);

                    if (lock != null && lock.isLocked()) {
                        lock.unlock();
                        commandBuffer.replaceComponent(context.entityRef(),
                                plugin.getAbilityLockComponentType(), lock);
                    }

                    RPGMobsLogger.debug(LOGGER,
                            "Deferred chain start failed for rootId=%s",
                            RPGMobsLogLevel.WARNING, resolvedRootId);
                } else {
                    applyPostTriggerEffects(deferredCtx, feature);
                    commitAbilityComponent(abilityId, worldStore, context.entityRef(),
                            commandBuffer);

                    if (lock != null) {
                        long tick = plugin.getTickClock().getTick();
                        lock.lockWithTimestamp(abilityId, tick);
                        lock.markChainStarted(tick);

                        commandBuffer.replaceComponent(context.entityRef(),
                                plugin.getAbilityLockComponentType(), lock);
                    }

                    activateFleeIfStandingHeal(abilityId, templateKey, npcEntity);
                    updateDebugNameplateIfEnabled(context.entityRef(), worldStore, commandBuffer);

                    RPGMobsLogger.debug(LOGGER,
                            "Deferred chain started for rootId=%s tick=%d",
                            RPGMobsLogLevel.INFO, resolvedRootId,
                            plugin.getTickClock().getTick());
                }
            });
        });

        return true;
    }

    private void updateDebugNameplateIfEnabled(Ref<EntityStore> entityRef, Store<EntityStore> store,
                                                CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.debugConfig.isDebugModeEnabled) {
            plugin.getNameplateService().updateDebugSegment(plugin, entityRef, store, commandBuffer, true);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void commitAbilityComponent(String abilityId, Store<EntityStore> store,
                                        Ref<EntityStore> entityRef,
                                        CommandBuffer<EntityStore> commandBuffer) {
        ComponentType componentType = abilityComponentTypes.get(abilityId);
        if (componentType == null) return;

        Object component = store.getComponent(entityRef, componentType);
        if (component != null) {
            ((CommandBuffer) commandBuffer).replaceComponent(entityRef, componentType,
                    (com.hypixel.hytale.component.Component) component);
        }
    }

    private void applyPostTriggerEffects(TriggerContext context, IRPGMobsAbilityFeature feature) {
        String abilityId = feature.id();
        RPGMobsConfig config = context.config();
        int tierIndex = context.tierIndex();

        if (AbilityIds.HEAL_LEAP.equals(abilityId)) {
            HealLeapAbilityComponent healLeap = context.store().getComponent(
                    context.entityRef(), plugin.getHealLeapAbilityComponentType());
            if (healLeap != null) {
                RPGMobsConfig.AbilityConfig rawCfg = getAbilityConfig(config, abilityId);
                if (rawCfg instanceof RPGMobsConfig.HealLeapAbilityConfig healCfg) {
                    healLeap.cooldownTicksRemaining = getCooldownTicks(
                            healCfg.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.CHARGE_LEAP.equals(abilityId)) {
            ChargeLeapAbilityComponent chargeLeap = context.store().getComponent(
                    context.entityRef(), plugin.getChargeLeapAbilityComponentType());
            if (chargeLeap != null) {
                RPGMobsConfig.AbilityConfig rawCfg = getAbilityConfig(config, abilityId);
                if (rawCfg instanceof RPGMobsConfig.ChargeLeapAbilityConfig leapCfg) {
                    chargeLeap.cooldownTicksRemaining = getCooldownTicks(
                            leapCfg.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.SUMMON_UNDEAD.equals(abilityId)) {
            SummonUndeadAbilityComponent summon = context.store().getComponent(
                    context.entityRef(), plugin.getSummonUndeadAbilityComponentType());
            if (summon != null) {
                NPCEntity npc = context.store().getComponent(context.entityRef(), NPC_COMPONENT_TYPE);
                String roleName = (npc != null) ? npc.getRoleName() : null;
                RPGMobsConfig.AbilityConfig rawCfg = getAbilityConfig(config, abilityId);
                String roleIdentifier = RPGMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(
                        rawCfg, roleName);
                summon.pendingSummonRole = roleIdentifier;
                summon.pendingSummonTicksRemaining = SUMMON_SPAWN_DELAY_TICKS;

                if (rawCfg instanceof RPGMobsConfig.SummonAbilityConfig summonCfg) {
                    summon.cooldownTicksRemaining = getCooldownTicks(
                            summonCfg.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.DODGE_ROLL.equals(abilityId)) {
            DodgeRollAbilityComponent dodgeRoll = context.store().getComponent(
                    context.entityRef(), plugin.getDodgeRollAbilityComponentType());
            if (dodgeRoll != null) {
                RPGMobsConfig.AbilityConfig rawCfg = getAbilityConfig(config, abilityId);
                if (rawCfg instanceof RPGMobsConfig.DodgeRollAbilityConfig dodgeCfg) {
                    dodgeRoll.cooldownTicksRemaining = getCooldownTicks(
                            dodgeCfg.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.MULTI_SLASH_SHORT.equals(abilityId)) {
            MultiSlashShortComponent msShort = context.store().getComponent(
                    context.entityRef(), plugin.getMultiSlashShortComponentType());
            if (msShort != null) {
                var ac = config.abilitiesConfig.defaultAbilities.get(AbilityIds.MULTI_SLASH_SHORT);
                if (ac instanceof RPGMobsConfig.MultiSlashAbilityConfig msCfg) {
                    var vc = msCfg.getVariantOrDefault(msShort.weaponVariant);
                    msShort.cooldownTicksRemaining = getCooldownTicks(
                            vc.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.MULTI_SLASH_MEDIUM.equals(abilityId)) {
            MultiSlashMediumComponent msMedium = context.store().getComponent(
                    context.entityRef(), plugin.getMultiSlashMediumComponentType());
            if (msMedium != null) {
                var ac = config.abilitiesConfig.defaultAbilities.get(AbilityIds.MULTI_SLASH_MEDIUM);
                if (ac instanceof RPGMobsConfig.MultiSlashAbilityConfig msCfg) {
                    var vc = msCfg.getVariantOrDefault(msMedium.weaponVariant);
                    msMedium.cooldownTicksRemaining = getCooldownTicks(
                            vc.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.MULTI_SLASH_LONG.equals(abilityId)) {
            MultiSlashLongComponent msLong = context.store().getComponent(
                    context.entityRef(), plugin.getMultiSlashLongComponentType());
            if (msLong != null) {
                var ac = config.abilitiesConfig.defaultAbilities.get(AbilityIds.MULTI_SLASH_LONG);
                if (ac instanceof RPGMobsConfig.MultiSlashAbilityConfig msCfg) {
                    var vc = msCfg.getVariantOrDefault(msLong.weaponVariant);
                    msLong.cooldownTicksRemaining = getCooldownTicks(
                            vc.cooldownSecondsPerTier, tierIndex);
                }
            }
        } else if (AbilityIds.ENRAGE.equals(abilityId)) {
            EnrageAbilityComponent enrage = context.store().getComponent(
                    context.entityRef(), plugin.getEnrageAbilityComponentType());
            if (enrage != null) {
                enrage.enraged = true;
                enrage.cooldownTicksRemaining = getCooldownTicks(
                        config.abilitiesConfig.defaultAbilities.get(abilityId).cooldownSecondsPerTier, tierIndex);
            }
        } else if (AbilityIds.VOLLEY.equals(abilityId)) {
            VolleyAbilityComponent volley = context.store().getComponent(
                    context.entityRef(), plugin.getVolleyAbilityComponentType());
            if (volley != null) {
                RPGMobsConfig.AbilityConfig rawCfg = getAbilityConfig(config, abilityId);
                if (rawCfg instanceof RPGMobsConfig.VolleyAbilityConfig volleyCfg) {
                    volley.cooldownTicksRemaining = getCooldownTicks(
                            volleyCfg.cooldownSecondsPerTier, tierIndex);
                }
            }
        }
    }

    private void handleAbilityCompletionRetrigger(RPGMobsAbilityCompletedEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        String abilityId = event.getAbilityId();

        if (AbilityIds.CHARGE_LEAP.equals(abilityId)) {
            ChargeLeapAbilityComponent chargeLeap = store.getComponent(entityRef,
                    plugin.getChargeLeapAbilityComponentType());
            if (chargeLeap != null) {
                RPGMobsLogger.debug(LOGGER,
                        "[ChargeLeap] COMPLETED: cooldownRemaining=%d ticks (%.1f sec)",
                        RPGMobsLogLevel.INFO,
                        chargeLeap.cooldownTicksRemaining,
                        chargeLeap.cooldownTicksRemaining / (float) Constants.TICKS_PER_SECOND);
            }
        }

        RPGMobsCombatTrackingComponent combat = store.getComponent(entityRef,
                plugin.getCombatTrackingComponentType());
        if (combat != null && combat.isInCombat()) {
            evaluateAbilitiesForEntity(entityRef, AbilityTriggerSource.ABILITY_COMPLETED);
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
                activeAbilityId, tierIndex, "death");
        plugin.getEventBus().fire(interruptedEvent);

        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity != null && npcEntity.getWorld() != null) {
            npcEntity.getWorld().execute(() -> {
                if (!entityRef.isValid()) return;
                var entityStoreProvider = npcEntity.getWorld().getEntityStore();
                if (entityStoreProvider == null) return;
                Store<EntityStore> worldStore = entityStoreProvider.getStore();
                StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                    RPGMobsAbilityLockComponent worldLock = worldStore.getComponent(
                            entityRef, plugin.getAbilityLockComponentType());
                    if (worldLock != null && worldLock.isLocked()) {
                        worldLock.unlock();
                        worldLock.globalCooldownTicksRemaining = 0;
                        commandBuffer.replaceComponent(entityRef,
                                plugin.getAbilityLockComponentType(), worldLock);
                    }
                    restoreSwappedWeaponIfNeeded(activeAbilityId, npcEntity, worldStore, entityRef, commandBuffer);
                    deactivateFleeIfHealLeap(activeAbilityId, npcEntity);
                });
            });
        }

        RPGMobsLogger.debug(LOGGER,
                "%s interrupted by death tier=%d",
                RPGMobsLogLevel.INFO, activeAbilityId, tierIndex);
    }

    private void handleDeaggroInterrupt(RPGMobsDeaggroEvent event) {
        Ref<EntityStore> entityRef = event.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) return;

        RPGMobsLogger.debug(LOGGER, "Mob deaggro'd tier=%d", RPGMobsLogLevel.INFO, event.getTier());

        RPGMobsAbilityLockComponent lock = store.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock == null || !lock.isLocked()) return;

        String activeAbilityId = lock.activeAbilityId;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsAbilityInterruptedEvent interruptedEvent = new RPGMobsAbilityInterruptedEvent(entityRef,
                activeAbilityId, tierIndex, "deaggro");
        plugin.getEventBus().fire(interruptedEvent);

        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity != null && npcEntity.getWorld() != null) {
            npcEntity.getWorld().execute(() -> {
                if (!entityRef.isValid()) return;
                var entityStoreProvider = npcEntity.getWorld().getEntityStore();
                if (entityStoreProvider == null) return;
                Store<EntityStore> worldStore = entityStoreProvider.getStore();
                StoreHelpers.withEntity(worldStore, entityRef, (_, commandBuffer, _) -> {
                    RPGMobsAbilityLockComponent worldLock = worldStore.getComponent(
                            entityRef, plugin.getAbilityLockComponentType());
                    if (worldLock != null && worldLock.isLocked()) {
                        worldLock.unlock();
                        worldLock.globalCooldownTicksRemaining = 0;
                        commandBuffer.replaceComponent(entityRef,
                                plugin.getAbilityLockComponentType(), worldLock);
                    }
                    restoreSwappedWeaponIfNeeded(activeAbilityId, npcEntity, worldStore, entityRef, commandBuffer);
                    deactivateFleeIfHealLeap(activeAbilityId, npcEntity);

                    // Enrage exhaustion death: an enraged mob is too far gone to stop.
                    // Even if the target dies/disappears, the mob dies from exhaustion.
                    if (AbilityIds.ENRAGE.equals(activeAbilityId)) {
                        EnrageAbilityComponent enrageComponent = worldStore.getComponent(
                                entityRef, plugin.getEnrageAbilityComponentType());
                        if (enrageComponent != null && enrageComponent.enraged) {
                            Damage exhaustionDamage = new Damage(Damage.NULL_SOURCE,
                                    DamageCause.ENVIRONMENT, Float.MAX_VALUE);
                            DeathComponent.tryAddComponent(commandBuffer, entityRef, exhaustionDamage);
                            RPGMobsLogger.debug(LOGGER,
                                    "Enrage exhaustion death on deaggro interrupt",
                                    RPGMobsLogLevel.INFO);
                        }
                    }
                });
            });
        }

        RPGMobsLogger.debug(LOGGER,
                "%s interrupted by deaggro tier=%d",
                RPGMobsLogLevel.INFO, activeAbilityId, tierIndex);
    }

    private void restoreSwappedWeaponIfNeeded(String activeAbilityId, NPCEntity npcEntity,
                                               Store<EntityStore> worldStore, Ref<EntityStore> entityRef,
                                               CommandBuffer<EntityStore> commandBuffer) {
        if (AbilityIds.HEAL_LEAP.equals(activeAbilityId)) {
            restoreIfSwapped(npcEntity, worldStore, entityRef, commandBuffer,
                    plugin.getHealLeapAbilityComponentType());
        } else if (AbilityIds.SUMMON_UNDEAD.equals(activeAbilityId)) {
            restoreIfSwapped(npcEntity, worldStore, entityRef, commandBuffer,
                    plugin.getSummonUndeadAbilityComponentType());
        } else if (AbilityIds.ENRAGE.equals(activeAbilityId)) {
            EnrageAbilityComponent enrage = worldStore.getComponent(entityRef,
                    plugin.getEnrageAbilityComponentType());
            if (enrage != null && (enrage.isSwapActive() || enrage.utilitySwapActive)) {
                AbilityHelpers.restoreWeaponIfNeeded(npcEntity, enrage);
                AbilityHelpers.restoreEnrageUtilityIfNeeded(npcEntity, enrage);
                commandBuffer.replaceComponent(entityRef,
                        plugin.getEnrageAbilityComponentType(), enrage);
            }
        } else if (AbilityIds.VOLLEY.equals(activeAbilityId)) {
            restoreIfSwapped(npcEntity, worldStore, entityRef, commandBuffer,
                    plugin.getVolleyAbilityComponentType());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void restoreIfSwapped(NPCEntity npcEntity, Store<EntityStore> store, Ref<EntityStore> entityRef,
                                  CommandBuffer<EntityStore> commandBuffer,
                                  ComponentType componentType) {
        Object component = store.getComponent(entityRef, componentType);
        if (component instanceof WeaponSwappable swappable && swappable.isSwapActive()) {
            AbilityHelpers.restoreWeaponIfNeeded(npcEntity, swappable);
            ((CommandBuffer) commandBuffer).replaceComponent(entityRef, componentType,
                    (com.hypixel.hytale.component.Component) component);
        }
    }

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

        RPGMobsConfig config = plugin.getConfig();
        RPGMobsConfig.HealLeapAbilityConfig hlConfig = config != null
                ? (RPGMobsConfig.HealLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(AbilityIds.HEAL_LEAP)
                : null;
        int interruptHits = hlConfig != null ? hlConfig.interruptHitCount : 3;

        healLeap.hitsTaken++;
        RPGMobsLogger.debug(LOGGER,
                "[HealLeap] Hit taken during ability: hitsTaken=%d/%d",
                RPGMobsLogLevel.INFO, healLeap.hitsTaken, interruptHits);

        if (healLeap.hitsTaken < interruptHits) {
            return true;
        }

        RPGMobsLogger.debug(LOGGER,
                "[HealLeap] INTERRUPTED by %d hits, cancelling chain",
                RPGMobsLogLevel.INFO, healLeap.hitsTaken);

        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity != null) {
            AbilityHelpers.restoreWeaponIfNeeded(npcEntity, healLeap);
        }

        healLeap.hitsTaken = 0;

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
                                worldStore, commandBuffer, InteractionType.Ability2, cancelRootId);
                    } else {
                        AbilityHelpers.cancelInteractionType(worldStore, commandBuffer,
                                entityRef, InteractionType.Ability2);
                    }

                    RPGMobsAbilityLockComponent worldLock = worldStore.getComponent(
                            entityRef, plugin.getAbilityLockComponentType());
                    if (worldLock != null && worldLock.isLocked()) {
                        worldLock.unlock();
                        worldLock.globalCooldownTicksRemaining = 0;
                        commandBuffer.replaceComponent(entityRef,
                                plugin.getAbilityLockComponentType(), worldLock);
                    }
                });
            });
        }

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsAbilityInterruptedEvent interruptedEvent = new RPGMobsAbilityInterruptedEvent(entityRef,
                AbilityIds.HEAL_LEAP, tierIndex, "hit_cancel");
        plugin.getEventBus().fire(interruptedEvent);

        return true;
    }

    private @Nullable Ref<EntityStore> getAggroTarget(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        RPGMobsCombatTrackingComponent combat = store.getComponent(entityRef, plugin.getCombatTrackingComponentType());
        if (combat == null) return null;
        return combat.getBestTarget();
    }

    private long getCooldownTicks(float[] cooldownSecondsPerTier, int tierIndex) {
        if (cooldownSecondsPerTier == null || tierIndex < 0 || tierIndex >= cooldownSecondsPerTier.length) {
            return 0L;
        }
        float seconds = cooldownSecondsPerTier[tierIndex];
        if (seconds <= 0f) return 0L;
        return (long) (seconds * Constants.TICKS_PER_SECOND);
    }

    private RPGMobsConfig.@Nullable AbilityConfig getAbilityConfig(RPGMobsConfig config, String abilityId) {
        if (config.abilitiesConfig == null || config.abilitiesConfig.defaultAbilities == null) return null;
        return config.abilitiesConfig.defaultAbilities.get(abilityId);
    }

    private @Nullable String resolveCancelRootInteractionId(RPGMobsConfig config, int tierIndex) {
        if (config == null) return null;
        RPGMobsConfig.AbilityConfig raw = getAbilityConfig(config, AbilityIds.HEAL_LEAP);
        if (!(raw instanceof RPGMobsConfig.HealLeapAbilityConfig healConfig)) return null;
        String cancelTemplatePath = healConfig.templates.getTemplate(
                RPGMobsConfig.HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL);
        if (cancelTemplatePath == null || cancelTemplatePath.isBlank()) return null;
        return TemplateNameGenerator.getTemplateNameWithTierFromPath(cancelTemplatePath, config, tierIndex);
    }

    private void activateFleeIfStandingHeal(String abilityId, String templateKey, NPCEntity npcEntity) {
        if (!AbilityIds.HEAL_LEAP.equals(abilityId)) return;
        if (!RPGMobsConfig.HealLeapAbilityConfig.TEMPLATE_STANDING_HEAL_ROOT.equals(templateKey)) return;

        Role role = npcEntity.getRole();
        if (role != null) {
            role.setBackingAway(true);
            RPGMobsLogger.debug(LOGGER, "Activated flee for standing heal",
                                RPGMobsLogLevel.INFO);
        }
    }

    private void deactivateFleeIfHealLeap(String activeAbilityId, NPCEntity npcEntity) {
        if (!AbilityIds.HEAL_LEAP.equals(activeAbilityId)) return;

        Role role = npcEntity.getRole();
        if (role != null) {
            role.setBackingAway(false);
        }
    }
}
