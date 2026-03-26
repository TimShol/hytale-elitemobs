package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class RPGMobsAbilityCombatTickSystem extends EntityTickingSystem<EntityStore> {

    private static final long COMBAT_TICK_INTERVAL = 20;

    private final RPGMobsPlugin plugin;
    private final RPGMobsAbilityTriggerListener triggerListener;

    public RPGMobsAbilityCombatTickSystem(RPGMobsPlugin plugin, RPGMobsAbilityTriggerListener triggerListener) {
        this.plugin = plugin;
        this.triggerListener = triggerListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getRPGMobsComponentType(),
                plugin.getCombatTrackingComponentType(),
                plugin.getAbilityLockComponentType()
        );
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        RPGMobsCombatTrackingComponent combat = chunk.getComponent(entityIndex,
                plugin.getCombatTrackingComponentType());
        if (combat == null || !combat.isInCombat()) return;

        RPGMobsAbilityLockComponent lock = chunk.getComponent(entityIndex, plugin.getAbilityLockComponentType());
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) return;

        long currentTick = plugin.getTickClock().getTick();
        long phase = combat.stateChangedTick % COMBAT_TICK_INTERVAL;
        if ((currentTick % COMBAT_TICK_INTERVAL) != phase) return;

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
        triggerListener.evaluateAbilitiesForEntity(entityRef, AbilityTriggerSource.COMBAT_TICK);
    }
}
