package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.Constants;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class RPGMobsAbilityCombatReevaluationSystem extends EntityTickingSystem<EntityStore> {

    private static final long REEVALUATION_INTERVAL_TICKS = Constants.TICKS_PER_SECOND;

    private final RPGMobsPlugin plugin;
    private final RPGMobsAbilityTriggerListener triggerListener;

    public RPGMobsAbilityCombatReevaluationSystem(RPGMobsPlugin plugin, RPGMobsAbilityTriggerListener triggerListener) {
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
                                                                   plugin.getCombatTrackingComponentType()
        );
        if (combat == null || !combat.isInCombat()) return;

        RPGMobsAbilityLockComponent lock = chunk.getComponent(entityIndex, plugin.getAbilityLockComponentType());
        if (lock != null && (lock.isLocked() || lock.isChainStartPending())) return;

        long currentTick = plugin.getTickClock().getTick();
        long phase = combat.stateChangedTick % REEVALUATION_INTERVAL_TICKS;
        if ((currentTick % REEVALUATION_INTERVAL_TICKS) != phase) return;

        ChargeLeapAbilityComponent chargeLeap = store.getComponent(chunk.getReferenceTo(entityIndex),
                                                                   plugin.getChargeLeapAbilityComponentType()
        );
        SummonUndeadAbilityComponent summon = store.getComponent(chunk.getReferenceTo(entityIndex),
                                                                 plugin.getSummonUndeadAbilityComponentType()
        );

        boolean hasChargeLeap = chargeLeap != null && chargeLeap.abilityEnabled;
        boolean hasSummon = summon != null && summon.abilityEnabled;

        if (!hasChargeLeap && !hasSummon) return;

        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
        triggerListener.reevaluateAbilitiesForCombatEntity(entityRef);
    }
}
