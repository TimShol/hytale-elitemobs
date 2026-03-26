package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.components.ability.EnrageAbilityComponent;
import com.frotty27.rpgmobs.components.ability.WeaponSwappable;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.Random;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clamp01;

public final class AbilityHelpers {

    private AbilityHelpers() {
    }

    public static @Nullable RootInteraction getRootInteraction(@Nullable String rootInteractionId) {
        if (rootInteractionId == null || rootInteractionId.isBlank()) return null;
        return RootInteraction.getAssetMap().getAsset(rootInteractionId);
    }

    public static boolean isInteractionTypeRunning(Store<EntityStore> entityStore, Ref<EntityStore> npcRef,
                                                   InteractionType interactionType) {
        ComponentType<EntityStore, InteractionManager> managerType = InteractionModule.get().getInteractionManagerComponent();
        InteractionManager manager = entityStore.getComponent(npcRef, managerType);
        if (manager == null) return false;
        var chains = manager.getChains();
        if (chains == null || chains.isEmpty()) return false;
        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) return true;
        }
        return false;
    }

    public static void cancelInteractionType(Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                             Ref<EntityStore> npcRef, InteractionType interactionType) {
        ComponentType<EntityStore, InteractionManager> managerType = InteractionModule.get().getInteractionManagerComponent();
        InteractionManager manager = entityStore.getComponent(npcRef, managerType);
        if (manager == null) return;

        var chains = manager.getChains();
        if (chains == null || chains.isEmpty()) return;

        for (InteractionChain chain : chains.values()) {
            if (chain != null && chain.getType() == interactionType) {
                manager.cancelChains(chain);
            }
        }
        commandBuffer.replaceComponent(npcRef, managerType, manager);
    }

    public static boolean swapWeaponInHand(NPCEntity npcEntity, WeaponSwappable swappable,
                                           @Nullable ItemStack replacementItem) {
        if (npcEntity == null || swappable == null) return false;
        if (swappable.isSwapActive()) return false;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return false;

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot == Inventory.INACTIVE_SLOT_INDEX) activeSlot = 0;

        ItemStack previousItem = inventory.getHotbar().getItemStack(activeSlot);

        inventory.getHotbar().setItemStackForSlot(activeSlot, replacementItem);
        inventory.markChanged();

        swappable.setSwapActive(true);
        swappable.setSwapSlot(activeSlot);
        swappable.setSwapPreviousItem(previousItem);

        return true;
    }

    public static boolean swapToItemInHand(NPCEntity npcEntity, WeaponSwappable swappable, String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        return swapWeaponInHand(npcEntity, swappable, new ItemStack(itemId, 1));
    }

    public static boolean unequipWeaponInHand(NPCEntity npcEntity, WeaponSwappable swappable) {
        return swapWeaponInHand(npcEntity, swappable, null);
    }

    public static void restoreWeaponIfNeeded(NPCEntity npcEntity, WeaponSwappable swappable) {
        if (npcEntity == null || swappable == null) return;
        if (!swappable.isSwapActive()) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = swappable.getSwapSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        inventory.getHotbar().setItemStackForSlot(slot, swappable.getSwapPreviousItem());
        inventory.markChanged();

        swappable.setSwapActive(false);
        swappable.setSwapSlot((byte) -1);
        swappable.setSwapPreviousItem(null);
    }

    public static void unequipUtilitySlotForEnrage(NPCEntity npcEntity, EnrageAbilityComponent enrageAbility) {
        if (npcEntity == null || enrageAbility == null) return;
        if (enrageAbility.utilitySwapActive) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        ItemContainer utility = inventory.getUtility();
        if (utility == null || utility.getCapacity() < 1) return;

        ItemStack utilItem = utility.getItemStack((short) 0);
        if (utilItem == null || utilItem.isEmpty()) return;

        utility.setItemStackForSlot((short) 0, null);
        inventory.markChanged();

        enrageAbility.utilitySwapActive = true;
        enrageAbility.utilitySwapPreviousItem = utilItem;
    }

    public static void restoreEnrageUtilityIfNeeded(NPCEntity npcEntity, EnrageAbilityComponent enrageAbility) {
        if (npcEntity == null || enrageAbility == null) return;
        if (!enrageAbility.utilitySwapActive) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        ItemContainer utility = inventory.getUtility();
        if (utility == null || utility.getCapacity() < 1) return;

        utility.setItemStackForSlot((short) 0, enrageAbility.utilitySwapPreviousItem);
        inventory.markChanged();

        enrageAbility.utilitySwapActive = false;
        enrageAbility.utilitySwapPreviousItem = null;
    }

    public static float rollPercentInRange(Random random, float minPercent, float maxPercent, float fallback) {
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
