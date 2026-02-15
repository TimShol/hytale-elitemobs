package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.Constants;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class EliteMobsAbilityCombatReevaluationSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final long REEVALUATION_INTERVAL_TICKS = Constants.TICKS_PER_SECOND;

    private final EliteMobsPlugin plugin;
    private final EliteMobsAbilityTriggerListener triggerListener;

    public EliteMobsAbilityCombatReevaluationSystem(
            EliteMobsPlugin plugin,
            EliteMobsAbilityTriggerListener triggerListener
    ) {
        this.plugin = plugin;
        this.triggerListener = triggerListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                plugin.getEliteMobsComponent(),
                plugin.getCombatTrackingComponent(),
                plugin.getAbilityLockComponent()
        );
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        EliteMobsCombatTrackingComponent combat = chunk.getComponent(entityIndex, plugin.getCombatTrackingComponent());
        if (combat == null || !combat.isInCombat()) return;

        EliteMobsAbilityLockComponent lock = chunk.getComponent(entityIndex, plugin.getAbilityLockComponent());
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) return;

        long currentTick = plugin.getTickClock().getTick();
        long phase = combat.stateChangedTick % REEVALUATION_INTERVAL_TICKS;
        if ((currentTick % REEVALUATION_INTERVAL_TICKS) != phase) return;

        ChargeLeapAbilityComponent chargeLeap = store.getComponent(
                chunk.getReferenceTo(entityIndex), plugin.getChargeLeapAbilityComponent());
        SummonUndeadAbilityComponent summon = store.getComponent(
                chunk.getReferenceTo(entityIndex), plugin.getSummonUndeadAbilityComponent());

        boolean hasChargeLeap = chargeLeap != null && chargeLeap.abilityEnabled;
        boolean hasSummon = summon != null && summon.abilityEnabled;

        if (!hasChargeLeap && !hasSummon) return;

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
        triggerListener.reevaluateAbilitiesForCombatEntity(entityRef);
    }
}
