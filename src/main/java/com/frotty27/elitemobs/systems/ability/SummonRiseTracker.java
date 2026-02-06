package com.frotty27.elitemobs.systems.ability;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class SummonRiseTracker {

    private static final long DEFAULT_WINDOW_MS = 3500L;

    private final ArrayList<SummonWindow> windows = new ArrayList<>();

    public void recordSummon(Vector3d center, double radius, Set<String> roleNames) {
        if (center == null || roleNames == null || roleNames.isEmpty()) return;
        cleanupExpired();
        long expiresAt = System.currentTimeMillis() + DEFAULT_WINDOW_MS;
        windows.add(new SummonWindow(center, radius, new HashSet<>(roleNames), expiresAt));
    }

    public boolean shouldApply(NPCEntity npcEntity, TransformComponent transform) {
        if (npcEntity == null || transform == null) return false;
        cleanupExpired();
        if (windows.isEmpty()) return false;

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;

        Vector3d pos = transform.getPosition();
        for (SummonWindow window : windows) {
            if (!window.roleNames.contains(roleName)) continue;
            if (distanceSquared(pos, window.center) <= window.radius * window.radius) {
                return true;
            }
        }
        return false;
    }

    private void cleanupExpired() {
        if (windows.isEmpty()) return;
        long now = System.currentTimeMillis();
        windows.removeIf(window -> window.expiresAtMs <= now);
    }

    private static double distanceSquared(Vector3d a, Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record SummonWindow(Vector3d center, double radius, Set<String> roleNames, long expiresAtMs) {}
}
