package com.frotty27.rpgmobs.api;

import com.frotty27.rpgmobs.api.events.*;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
        RPGMobsLogger.debug(LOGGER,
                            "[EventBus] fire(RPGMobsSpawnedEvent) tier=%d listeners=%d",
                            RPGMobsLogLevel.INFO,
                            event.getTier(),
                            listeners.size()
        );
        for (var listener : listeners) {
            try {
                listener.onRPGMobSpawned(event);
            } catch (Throwable t) {
                logError("onRPGMobSpawned", t);
            }
        }
    }

    public void fire(RPGMobsDeathEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobDeath(event);
            } catch (Throwable t) {
                logError("onRPGMobDeath", t);
            }
        }
    }

    public void fire(RPGMobsDropsEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobDrops(event);
            } catch (Throwable t) {
                logError("onRPGMobDrops", t);
            }
        }
    }

    public void fire(RPGMobsDamageDealtEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobDamageDealt(event);
            } catch (Throwable t) {
                logError("onRPGMobDamageDealt", t);
            }
        }
    }

    public void fire(RPGMobsDamageReceivedEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobDamageReceived(event);
            } catch (Throwable t) {
                logError("onRPGMobDamageReceived", t);
            }
        }
    }

    public void fire(RPGMobsReconcileEvent event) {
        for (var listener : listeners) {
            try {
                listener.onReconcile(event);
            } catch (Throwable t) {
                logError("onReconcile", t);
            }
        }
    }

    public void fire(RPGMobsAggroEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobAggro(event);
            } catch (Throwable t) {
                logError("onRPGMobAggro", t);
            }
        }
    }

    public void fire(RPGMobsDeaggroEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobDeaggro(event);
            } catch (Throwable t) {
                logError("onRPGMobDeaggro", t);
            }
        }
    }

    public void fire(RPGMobsAbilityStartedEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobAbilityStarted(event);
            } catch (Throwable t) {
                logError("onRPGMobAbilityStarted", t);
            }
        }
    }

    public void fire(RPGMobsAbilityCompletedEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobAbilityCompleted(event);
            } catch (Throwable t) {
                logError("onRPGMobAbilityCompleted", t);
            }
        }
    }

    public void fire(RPGMobsAbilityInterruptedEvent event) {
        for (var listener : listeners) {
            try {
                listener.onRPGMobAbilityInterrupted(event);
            } catch (Throwable t) {
                logError("onRPGMobAbilityInterrupted", t);
            }
        }
    }

    public void fire(RPGMobsScalingAppliedEvent event) {
        for (var listener : listeners) {
            try {
                listener.onScalingApplied(event);
            } catch (Throwable t) {
                logError("onScalingApplied", t);
            }
        }
    }

    private static void logError(String eventName, Throwable t) {
        RPGMobsLogger.debug(LOGGER, "Listener threw in %s: %s", RPGMobsLogLevel.WARNING, eventName, t.toString());
    }
}
