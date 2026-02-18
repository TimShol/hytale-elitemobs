package com.frotty27.rpgmobs.systems.drops;

import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class RPGMobsExtraDropsSchedulerSystem extends EntityTickingSystem<EntityStore> {

    private final RPGMobsPlugin RPGMobsPlugin;

    private long lastProcessedTick = Long.MIN_VALUE;

    private boolean advancedThisEngineTick = false;
    private long lastAdvanceNanos = 0L;

    private static final long NEW_ENGINE_TICK_THRESHOLD_NS = 1_000_000L;

    public RPGMobsExtraDropsSchedulerSystem(RPGMobsPlugin RPGMobsPlugin) {
        this.RPGMobsPlugin = RPGMobsPlugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return TransformComponent.getComponentType();
    }

    @Override
    public void tick(float deltaTimeSeconds, int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        long nowNanos = System.nanoTime();
        if (advancedThisEngineTick && (nowNanos - lastAdvanceNanos) < NEW_ENGINE_TICK_THRESHOLD_NS) {
            long currentTick = RPGMobsPlugin.getTickClock().getTick();
            if (currentTick == lastProcessedTick) return;
            lastProcessedTick = currentTick;
            RPGMobsPlugin.onWorldTick();
            RPGMobsPlugin.getExtraDropsScheduler().flushDue(entityStore, commandBuffer);
            return;
        }

        RPGMobsPlugin.getTickClock().advance(deltaTimeSeconds);
        advancedThisEngineTick = true;
        lastAdvanceNanos = nowNanos;

        long currentTick = RPGMobsPlugin.getTickClock().getTick();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        RPGMobsPlugin.onWorldTick();
        RPGMobsPlugin.getExtraDropsScheduler().flushDue(entityStore, commandBuffer);
    }
}
