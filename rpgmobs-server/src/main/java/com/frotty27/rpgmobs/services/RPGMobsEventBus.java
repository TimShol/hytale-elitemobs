package com.frotty27.rpgmobs.services;

import com.frotty27.rpgmobs.api.IRPGMobsEventBus;
import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.*;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class RPGMobsEventBus implements IRPGMobsEventBus {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final List<IRPGMobsEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void registerListener(IRPGMobsEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(IRPGMobsEventListener listener) {
        listeners.remove(listener);
    }

    public void fire(RPGMobsSpawnedEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobSpawned, "onRPGMobSpawned",
                 "fire(RPGMobsSpawnedEvent) tier=%d role=%s listeners=%d",
                 event.getTier(), event.getRoleName(), listeners.size());
    }

    public void fire(RPGMobsDeathEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobDeath, "onRPGMobDeath",
                 "fire(RPGMobsDeathEvent) tier=%d role=%s isMinion=%s listeners=%d",
                 event.getTier(), event.getRoleName(), event.isMinion(), listeners.size());
    }

    public void fire(RPGMobsDropsEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobDrops, "onRPGMobDrops",
                 "fire(RPGMobsDropsEvent) tier=%d role=%s dropCount=%d listeners=%d",
                 event.getTier(), event.getRoleName(), event.getDrops().size(), listeners.size());
    }

    public void fire(RPGMobsDamageDealtEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobDamageDealt, "onRPGMobDamageDealt",
                 "fire(RPGMobsDamageDealtEvent) tier=%d role=%s baseDamage=%.2f multiplier=%.2f listeners=%d",
                 event.getTier(), event.getRoleName(), event.getBaseDamage(), event.getMultiplier(), listeners.size());
    }

    public void fire(RPGMobsDamageReceivedEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobDamageReceived, "onRPGMobDamageReceived",
                 "fire(RPGMobsDamageReceivedEvent) tier=%d role=%s damageAmount=%.2f listeners=%d",
                 event.getTier(), event.getRoleName(), event.getDamageAmount(), listeners.size());
    }

    public void fire(RPGMobsReconcileEvent event) {
        dispatch(event, IRPGMobsEventListener::onReconcile, "onReconcile",
                 "fire(RPGMobsReconcileEvent) listeners=%d",
                 listeners.size());
    }

    public void fire(RPGMobsAggroEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobAggro, "onRPGMobAggro",
                 "fire(RPGMobsAggroEvent) tier=%d role=%s listeners=%d",
                 event.getTier(), event.getRoleName(), listeners.size());
    }

    public void fire(RPGMobsDeaggroEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobDeaggro, "onRPGMobDeaggro",
                 "fire(RPGMobsDeaggroEvent) tier=%d role=%s listeners=%d",
                 event.getTier(), event.getRoleName(), listeners.size());
    }

    public void fire(RPGMobsAbilityStartedEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobAbilityStarted, "onRPGMobAbilityStarted",
                 "fire(RPGMobsAbilityStartedEvent) abilityId=%s tierIndex=%d listeners=%d",
                 event.getAbilityId(), event.getTierIndex(), listeners.size());
    }

    public void fire(RPGMobsAbilityCompletedEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobAbilityCompleted, "onRPGMobAbilityCompleted",
                 "fire(RPGMobsAbilityCompletedEvent) abilityId=%s tierIndex=%d listeners=%d",
                 event.getAbilityId(), event.getTierIndex(), listeners.size());
    }

    public void fire(RPGMobsAbilityInterruptedEvent event) {
        dispatch(event, IRPGMobsEventListener::onRPGMobAbilityInterrupted, "onRPGMobAbilityInterrupted",
                 "fire(RPGMobsAbilityInterruptedEvent) abilityId=%s tierIndex=%d reason=%s listeners=%d",
                 event.getAbilityId(), event.getTierIndex(), event.getReason(), listeners.size());
    }

    public void fire(RPGMobsScalingAppliedEvent event) {
        dispatch(event, IRPGMobsEventListener::onScalingApplied, "onScalingApplied",
                 "fire(RPGMobsScalingAppliedEvent) tier=%d healthMult=%.2f damageMult=%.2f modelScale=%.2f baseHealth=%.1f finalHealth=%.1f listeners=%d",
                 event.tierIndex(), event.healthMultiplier(), event.damageMultiplier(),
                 event.modelScale(), event.baseHealth(), event.finalHealth(), listeners.size());
    }

    private <E> void dispatch(E event,
                                                    BiConsumer<IRPGMobsEventListener, E> handler,
                                                    String handlerName,
                                                    String debugFormat,
                                                    Object... debugArgs) {
        RPGMobsLogger.debug(LOGGER, debugFormat, RPGMobsLogLevel.INFO, debugArgs);
        for (var listener : listeners) {
            try {
                handler.accept(listener, event);
            } catch (Throwable t) {
                logError(handlerName, t);
            }
        }
    }

    private static void logError(String eventName, Throwable t) {
        RPGMobsLogger.debug(LOGGER, "Listener threw in %s: %s", RPGMobsLogLevel.WARNING, eventName, t.toString());
    }
}
