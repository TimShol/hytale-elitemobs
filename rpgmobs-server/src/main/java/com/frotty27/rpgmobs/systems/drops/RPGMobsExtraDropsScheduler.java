package com.frotty27.rpgmobs.systems.drops;

import com.frotty27.rpgmobs.utils.TickClock;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.PriorityQueue;

public final class RPGMobsExtraDropsScheduler {

    public record DropJob(long dueTick, Vector3d position, Vector3f rotation, ObjectArrayList<ItemStack> itemDrops,
                          @Nullable String particleId) {
    }

    private final TickClock tickClock;

    private final PriorityQueue<DropJob> scheduledDropJobs = new PriorityQueue<>(Comparator.comparingLong(DropJob::dueTick));

    public RPGMobsExtraDropsScheduler(TickClock tickClock) {
        this.tickClock = tickClock;
    }

    public void enqueueDrops(long delayTicks, Vector3d position, Vector3f rotation,
                             ObjectArrayList<ItemStack> itemDrops, @Nullable String particleId) {
        if (itemDrops == null || itemDrops.isEmpty()) return;

        long dueTick = tickClock.getTick() + Math.max(0L, delayTicks);

        String normalizedParticleId = (particleId == null || particleId.isBlank()) ? null : particleId;

        synchronized (scheduledDropJobs) {
            scheduledDropJobs.add(new DropJob(dueTick,
                                              position.clone(),
                                              rotation.clone(),
                                              new ObjectArrayList<>(itemDrops),
                                              normalizedParticleId
            ));
        }
    }

    public void flushDue(Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        long currentTick = tickClock.getTick();

        while (true) {
            DropJob nextJob;

            synchronized (scheduledDropJobs) {
                nextJob = scheduledDropJobs.peek();
                if (nextJob == null || nextJob.dueTick() > currentTick) return;
                scheduledDropJobs.poll();
            }

            var itemEntities = ItemComponent.generateItemDrops(entityStore,
                                                               nextJob.itemDrops,
                                                               nextJob.position,
                                                               nextJob.rotation
            );

            commandBuffer.addEntities(itemEntities, AddReason.SPAWN);
        }
    }
}
