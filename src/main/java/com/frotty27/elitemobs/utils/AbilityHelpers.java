package com.frotty27.elitemobs.utils;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.PositionCache;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;

public final class AbilityHelpers {

    private AbilityHelpers() {
    }

    /**
     * Attempts to find the nearest visible player within a range.
     * This is used as a powerful fallback when AI markers are not yet set.
     */
    public static @Nullable Ref<EntityStore> findNearestVisiblePlayer(NPCEntity npcEntity, float maxRange) {
        World world = npcEntity.getWorld();
        if (world == null) return null;

        Ref<EntityStore> npcRef = npcEntity.getReference();
        if (npcRef == null || !npcRef.isValid()) return null;

        Store<EntityStore> store = npcRef.getStore();
        if (store == null) return null;
        TransformComponent npcTransform = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (npcTransform == null) return null;
        Vector3d npcPos = npcTransform.getPosition();
        double maxDistSq = (double) maxRange * (double) maxRange;

        Ref<EntityStore> nearestRef = null;
        double nearestDistSq = maxDistSq;

        Role role = npcEntity.getRole();
        PositionCache cache = (role != null) ? role.getPositionCache() : null;
        // Use getPlayers() which returns List<Player>.
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (player == null) continue;

            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid()) continue;

            Vector3d playerPos = player.getTransform().getPosition();
            double distSq = npcPos.distanceSquaredTo(playerPos);

            if (distSq < nearestDistSq) {
                // If we have a position cache, use it for an accurate line-of-sight check
                if (cache != null) {
                    if (cache.hasLineOfSight(npcRef, playerRef, store)) {
                        nearestDistSq = distSq;
                        nearestRef = playerRef;
                    }
                } else {
                    // Fallback to simple distance if cache is missing (unlikely for NPCs)
                    // We cannot easily raycast without the World raycast API
                    nearestDistSq = distSq;
                    nearestRef = playerRef;
                }
            }
        }

        return nearestRef;
    }

    public static @Nullable RootInteraction getRootInteraction(@Nullable String rootInteractionId) {
        if (rootInteractionId == null || rootInteractionId.isBlank()) return null;
        return RootInteraction.getAssetMap().getAsset(rootInteractionId);
    }

    /**
     * Checks for a target assigned by the NPC AI.
     */
    public static @Nullable Ref<EntityStore> getLockedTargetRef(NPCEntity npcEntity) {
        if (npcEntity == null) return null;

        try {
            Role npcRole = npcEntity.getRole();
            if (npcRole == null) return null;

            MarkedEntitySupport markedEntitySupport = npcRole.getMarkedEntitySupport();
            if (markedEntitySupport == null) return null;

            String[] targetKeys = {
                    "LockedTarget",
                    "Target",
                    "CombatTarget",
                    "AttackTarget",
                    "CurrentTarget",
                    "Enemy",
                    "Opponent",
                    "AggroTarget",
                    "Provoker",
                    "ChasedEntity",
                    "ChaseTarget",
                    "Focus",
                    "Threat",
                    "Aggro",
                    "Hostile"
            };

            for (String key : targetKeys) {
                Ref<EntityStore> target = markedEntitySupport.getMarkedEntityRef(key);
                if (target != null && target.isValid()) return target;
            }

        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isInteractionTypeRunning(
            Store<EntityStore> entityStore,
            Ref<EntityStore> npcRef,
            InteractionType interactionType
    ) {
        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                InteractionModule.get().getInteractionManagerComponent();
        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) return false;
        var chains = interactionManager.getChains();
        if (chains == null || chains.isEmpty()) return false;
        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) return true;
        }
        return false;
    }

    public static void cancelInteractionType(
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> npcRef,
            InteractionType interactionType
    ) {
        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                InteractionModule.get().getInteractionManagerComponent();
        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) return;

        var chains = interactionManager.getChains();
        if (chains == null || chains.isEmpty()) return;

        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) {
                interactionManager.cancelChains(chain);
            }
        }
        commandBuffer.replaceComponent(npcRef, interactionManagerComponentType, interactionManager);
    }

    public static void restorePreviousItemIfNeeded(
            NPCEntity npcEntity,
            EliteMobsTierComponent tierComponent
    ) {
        if (npcEntity == null || tierComponent == null) return;
        if (!tierComponent.healSwapActive) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = tierComponent.healSwapSlot;
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        ItemStack previous = tierComponent.healSwapPreviousItem;
        inventory.getHotbar().setItemStackForSlot(slot, previous);
        inventory.markChanged();

        tierComponent.healSwapActive = false;
        tierComponent.healSwapSlot = -1;
        tierComponent.healSwapPreviousItem = null;
    }

    public static float rollPercentInRange(
            Random random,
            float minPercent,
            float maxPercent,
            float fallback
    ) {
        if (random == null) return fallback;

        float min = clamp01(minPercent);
        float max = clamp01(maxPercent);
        if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        if (max <= 0f) return Math.max(0.01f, fallback);
        if (max == min) return max;
        return min + random.nextFloat() * (max - min);
    }
}
