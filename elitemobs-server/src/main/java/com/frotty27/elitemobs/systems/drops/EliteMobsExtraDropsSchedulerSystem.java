package com.frotty27.elitemobs.systems.drops;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class EliteMobsExtraDropsSchedulerSystem extends EntityTickingSystem<EntityStore> {

    private final EliteMobsPlugin eliteMobsPlugin;

    private long lastProcessedTick = Long.MIN_VALUE;

    private boolean advancedThisEngineTick = false;
    private long lastAdvanceNanos = 0L;

    private static final long NEW_ENGINE_TICK_THRESHOLD_NS = 1_000_000L;

    public EliteMobsExtraDropsSchedulerSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return TransformComponent.getComponentType();
    }

    @Override
    public void tick(
            float deltaTimeSeconds,
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        long nowNanos = System.nanoTime();
        if (advancedThisEngineTick && (nowNanos - lastAdvanceNanos) < NEW_ENGINE_TICK_THRESHOLD_NS) {
            long currentTick = eliteMobsPlugin.getTickClock().getTick();
            if (currentTick == lastProcessedTick) return;
            lastProcessedTick = currentTick;
            eliteMobsPlugin.onWorldTick();
            eliteMobsPlugin.getExtraDropsScheduler().flushDue(entityStore, commandBuffer);
            return;
        }

        eliteMobsPlugin.getTickClock().advance(deltaTimeSeconds);
        advancedThisEngineTick = true;
        lastAdvanceNanos = nowNanos;

        long currentTick = eliteMobsPlugin.getTickClock().getTick();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        eliteMobsPlugin.onWorldTick();
        eliteMobsPlugin.getExtraDropsScheduler().flushDue(entityStore, commandBuffer);
    }
}
