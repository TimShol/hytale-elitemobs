package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.components.ability.EnrageAbilityComponent;
import com.frotty27.rpgmobs.components.ability.WeaponSwappable;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.RPGMobsAbilityFeatureHelpers;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import static com.frotty27.rpgmobs.utils.Constants.*;


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

        var hotbar = inventory.getHotbar();
        if (hotbar == null || hotbar.getCapacity() <= 0) return false;

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot == Inventory.INACTIVE_SLOT_INDEX) activeSlot = 0;
        if (activeSlot >= hotbar.getCapacity()) return false;

        ItemStack previousItem = hotbar.getItemStack(activeSlot);

        hotbar.setItemStackForSlot(activeSlot, replacementItem);
        npcEntity.invalidateEquipmentNetwork();

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

        var hotbar = inventory.getHotbar();
        if (hotbar == null || hotbar.getCapacity() <= 0) return;

        byte slot = swappable.getSwapSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;
        if (slot >= hotbar.getCapacity()) return;

        hotbar.setItemStackForSlot(slot, swappable.getSwapPreviousItem());
        npcEntity.invalidateEquipmentNetwork();

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
        npcEntity.invalidateEquipmentNetwork();

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
        npcEntity.invalidateEquipmentNetwork();

        enrageAbility.utilitySwapActive = false;
        enrageAbility.utilitySwapPreviousItem = null;
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static float calculateDistance(Ref<EntityStore> entityRef, Ref<EntityStore> targetRef,
                                          Store<EntityStore> store) {
        TransformComponent mobTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (mobTransform == null || targetTransform == null) return Float.MAX_VALUE;

        Vector3d mobPos = mobTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();
        double dx = targetPos.getX() - mobPos.getX();
        double dy = targetPos.getY() - mobPos.getY();
        double dz = targetPos.getZ() - mobPos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static String resolveWeaponVariant(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                                              Store<EntityStore> entityStore) {
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        if (weaponId.isEmpty()) {
            RPGMobsLogger.debug(LOGGER, "[WeaponVariant] weaponId empty, defaulting to swords",
                    RPGMobsLogLevel.INFO);
            return VARIANT_SWORDS;
        }

        RPGMobsConfig config = plugin.getConfig();
        if (config == null || config.gearConfig == null || config.gearConfig.weaponCategoryTree == null) {
            RPGMobsLogger.debug(LOGGER, "[WeaponVariant] no config/gearConfig, defaulting to swords",
                    RPGMobsLogLevel.INFO);
            return VARIANT_SWORDS;
        }

        RPGMobsConfig.GearCategory weaponTree = config.gearConfig.weaponCategoryTree;
        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (category.itemKeys.contains(weaponId)) {
                String variant = CATEGORY_TO_VARIANT.get(category.name);
                if (variant != null) {
                    if (VARIANT_CLUBS.equals(variant) && weaponId.toLowerCase().contains("flail")) {
                        RPGMobsLogger.debug(LOGGER,
                                "[WeaponVariant] weaponId=%s -> category=%s -> variant=clubsFlail",
                                RPGMobsLogLevel.INFO, weaponId, category.name);
                        return VARIANT_CLUBS_FLAIL;
                    }
                    RPGMobsLogger.debug(LOGGER,
                            "[WeaponVariant] weaponId=%s -> category=%s -> variant=%s",
                            RPGMobsLogLevel.INFO, weaponId, category.name, variant);
                    return variant;
                }
                RPGMobsLogger.debug(LOGGER,
                        "[WeaponVariant] weaponId=%s found in category=%s but no variant mapping",
                        RPGMobsLogLevel.WARNING, weaponId, category.name);
            }
        }

        RPGMobsLogger.debug(LOGGER,
                "[WeaponVariant] weaponId=%s not found in any category, defaulting to swords",
                RPGMobsLogLevel.WARNING, weaponId);
        return VARIANT_SWORDS;
    }
}
