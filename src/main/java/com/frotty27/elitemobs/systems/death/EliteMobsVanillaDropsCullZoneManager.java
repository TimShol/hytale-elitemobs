package com.frotty27.elitemobs.systems.death;

import com.frotty27.elitemobs.utils.TickClock;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.concurrent.CopyOnWriteArrayList;

public final class EliteMobsVanillaDropsCullZoneManager {

    private record CullZone(
            Vector3d centerPosition,
            double radiusSquared,
            long expiresAtTick
    ) {}

    private final TickClock tickClock;
    private final CopyOnWriteArrayList<CullZone> activeCullZones = new CopyOnWriteArrayList<>();

    public EliteMobsVanillaDropsCullZoneManager(TickClock tickClock) {
        this.tickClock = tickClock;
    }

    public void addCullZone(Vector3d centerPosition, double radius, long lifetimeTicks) {
        long expirationTick = tickClock.getTick() + Math.max(0L, lifetimeTicks);
        double radiusSquared = radius * radius;

        activeCullZones.add(
                new CullZone(centerPosition.clone(), radiusSquared, expirationTick)
        );
    }

    public boolean shouldCull(Vector3d itemPosition) {
        long currentTick = tickClock.getTick();

        for (CullZone cullZone : activeCullZones) {
            if (currentTick >= cullZone.expiresAtTick()) {
                activeCullZones.remove(cullZone);
                continue;
            }

            double deltaX = itemPosition.x - cullZone.centerPosition().x;
            double deltaY = itemPosition.y - cullZone.centerPosition().y;
            double deltaZ = itemPosition.z - cullZone.centerPosition().z;

            double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            if (distanceSquared <= cullZone.radiusSquared()) {
                return true;
            }
        }

        return false;
    }
}
