package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.api.events.RPGMobsAbilityCompletedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.EnrageAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.ability.VolleyAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import org.jspecify.annotations.NonNull;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsAbilityCompletionSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType ABILITY_INTERACTION_TYPE = InteractionType.Ability2;
    private static final long STALE_LOCK_TIMEOUT_TICKS = 300;

    private final RPGMobsPlugin plugin;

    public RPGMobsAbilityCompletionSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getRPGMobsComponentType(), plugin.getAbilityLockComponentType());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        RPGMobsAbilityLockComponent lock = chunk.getComponent(entityIndex, plugin.getAbilityLockComponentType());
        if (lock == null || lock.activeAbilityId == null) return;

        long currentTick = plugin.getTickClock().getTick();

        if (lock.isChainStartPending()) {
            if (lock.isLockedBeyondTimeout(currentTick, STALE_LOCK_TIMEOUT_TICKS)) {
                Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
                RPGMobsLogger.debug(LOGGER,
                        "STALE PENDING: ability=%s lockedAt=%d currentTick=%d  - force unlocking",
                        RPGMobsLogLevel.WARNING,
                        lock.activeAbilityId, lock.lockedAtTick, currentTick);
                lock.unlock();
                commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
            }
            return;
        }

        if (lock.isWithinStartGracePeriod(currentTick)) {
            RPGMobsLogger.debug(LOGGER,
                                "GRACE skip: ability=%s startedAt=%d currentTick=%d",
                                RPGMobsLogLevel.INFO,
                                lock.activeAbilityId,
                                lock.chainStartedAtTick,
                                currentTick
            );
            return;
        }

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        boolean chainRunning = AbilityHelpers.isInteractionTypeRunning(store, entityRef, ABILITY_INTERACTION_TYPE);
        if (chainRunning) {
            if (lock.isLockedBeyondTimeout(currentTick, STALE_LOCK_TIMEOUT_TICKS)) {
                RPGMobsLogger.debug(LOGGER,
                        "STALE RUNNING: ability=%s lockedAt=%d currentTick=%d  - force unlocking",
                        RPGMobsLogLevel.WARNING,
                        lock.activeAbilityId, lock.lockedAtTick, currentTick);
                lock.unlock();
                commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);
            }
            return;
        }

        String completedAbilityId = lock.activeAbilityId;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsLogger.debug(LOGGER,
                            "DETECTED: ability=%s tier=%d pending=%b startedAt=%d currentTick=%d chainRunning=%b",
                            RPGMobsLogLevel.INFO,
                            completedAbilityId,
                            tierIndex,
                            lock.chainStartPending,
                            lock.chainStartedAtTick,
                            currentTick,
                            chainRunning
        );

        float gcMin = 1.0f;
        float gcMax = 3.0f;
        NPCEntity npcEntity = store.getComponent(entityRef, NPC_COMPONENT_TYPE);
        if (npcEntity != null && npcEntity.getWorld() != null) {
            var resolved = plugin.getConfigResolver().getResolvedConfig(npcEntity.getWorld().getName());
            gcMin = resolved.globalCooldownMinSeconds;
            gcMax = resolved.globalCooldownMaxSeconds;
        }
        lock.unlockWithGlobalCooldown(gcMin, gcMax);
        commandBuffer.replaceComponent(entityRef, plugin.getAbilityLockComponentType(), lock);

        if (AbilityIds.ENRAGE.equals(completedAbilityId)) {
            EnrageAbilityComponent enrage = store.getComponent(entityRef, plugin.getEnrageAbilityComponentType());
            if (enrage != null && npcEntity != null) {
                AbilityHelpers.restoreWeaponIfNeeded(npcEntity, enrage);
                AbilityHelpers.restoreEnrageUtilityIfNeeded(npcEntity, enrage);
                commandBuffer.replaceComponent(entityRef, plugin.getEnrageAbilityComponentType(), enrage);
            }

            Damage damage = new Damage(Damage.NULL_SOURCE, DamageCause.ENVIRONMENT, Float.MAX_VALUE);
            DeathComponent.tryAddComponent(commandBuffer, entityRef, damage);
            RPGMobsLogger.debug(LOGGER,
                                "ENRAGE exhaustion death: tier=%d",
                                RPGMobsLogLevel.INFO, tierIndex);
        } else if (AbilityIds.VOLLEY.equals(completedAbilityId)) {
            VolleyAbilityComponent volley = store.getComponent(entityRef, plugin.getVolleyAbilityComponentType());
            if (volley != null && npcEntity != null && volley.swapActive) {
                AbilityHelpers.restoreWeaponIfNeeded(npcEntity, volley);
                commandBuffer.replaceComponent(entityRef, plugin.getVolleyAbilityComponentType(), volley);
            }
        }

        if (AbilityIds.HEAL_LEAP.equals(completedAbilityId) && npcEntity != null) {
            Role role = npcEntity.getRole();
            if (role != null) {
                role.setBackingAway(false);
            }
        }

        RPGMobsAbilityCompletedEvent completedEvent = new RPGMobsAbilityCompletedEvent(entityRef,
                                                                                       completedAbilityId,
                                                                                       tierIndex
        );
        plugin.getEventBus().fire(completedEvent);

        RPGMobsConfig config = plugin.getConfig();
        if (config != null && config.debugConfig.isDebugModeEnabled) {
            plugin.getNameplateService().updateDebugSegment(plugin, entityRef, store, commandBuffer, true);
        }

        RPGMobsLogger.debug(LOGGER,
                            "%s completed tier=%d globalCooldown=%d",
                            RPGMobsLogLevel.INFO,
                            completedAbilityId,
                            tierIndex,
                            lock.globalCooldownTicksRemaining
        );
    }
}
