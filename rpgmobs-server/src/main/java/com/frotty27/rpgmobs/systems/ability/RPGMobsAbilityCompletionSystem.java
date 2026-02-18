package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.api.events.RPGMobsAbilityCompletedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public final class RPGMobsAbilityCompletionSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final InteractionType ABILITY_INTERACTION_TYPE = InteractionType.Ability2;

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
        if (lock == null || !lock.isLocked()) return;

        long currentTick = plugin.getTickClock().getTick();

        if (lock.isChainStartPending()) return;

        if (lock.isWithinStartGracePeriod(currentTick)) {
            RPGMobsLogger.debug(LOGGER,
                                "[AbilityCompletion] GRACE skip: ability=%s startedAt=%d currentTick=%d",
                                RPGMobsLogLevel.INFO,
                                lock.activeAbilityId,
                                lock.chainStartedAtTick,
                                currentTick
            );
            return;
        }

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        boolean chainRunning = AbilityHelpers.isInteractionTypeRunning(store, entityRef, ABILITY_INTERACTION_TYPE);
        if (chainRunning) return;

        String completedAbilityId = lock.activeAbilityId;

        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierIndex = (tier != null) ? clampTierIndex(tier.tierIndex) : 0;

        RPGMobsLogger.debug(LOGGER,
                            "[AbilityCompletion] DETECTED: ability=%s tier=%d pending=%b startedAt=%d currentTick=%d chainRunning=%b",
                            RPGMobsLogLevel.INFO,
                            completedAbilityId,
                            tierIndex,
                            lock.chainStartPending,
                            lock.chainStartedAtTick,
                            currentTick,
                            chainRunning
        );

        RPGMobsAbilityCompletedEvent completedEvent = new RPGMobsAbilityCompletedEvent(entityRef,
                                                                                       completedAbilityId,
                                                                                       tierIndex
        );
        plugin.getEventBus().fire(completedEvent);

        RPGMobsLogger.debug(LOGGER,
                            "[AbilityCompletion] %s completed tier=%d",
                            RPGMobsLogLevel.INFO,
                            completedAbilityId,
                            tierIndex
        );
    }
}
