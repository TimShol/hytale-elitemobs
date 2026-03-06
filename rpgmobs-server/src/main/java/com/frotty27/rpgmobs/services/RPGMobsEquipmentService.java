package com.frotty27.rpgmobs.services;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.utils.EquipmentHelpers;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

import java.util.*;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.*;
import static com.frotty27.rpgmobs.utils.Constants.*;
import static com.frotty27.rpgmobs.utils.InventoryHelpers.getContainerSizeSafe;

public final class RPGMobsEquipmentService {

    private static final String SHIELD_TOKEN = "weapon_shield";
    private static final String ARMOR_PREFIX = "Armor_";
    private static final int ARMOR_SLOT_COUNT = 4;

    private final Random random = new Random();
    private double activeDurabilityMin;
    private double activeDurabilityMax;

    public void clearAllEquipment(NPCEntity npcEntity) {
        if (npcEntity == null) return;
        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        boolean changed = false;

        ItemContainer armor = inventory.getArmor();
        if (armor != null) {
            for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
                try {
                    var existing = armor.getItemStack((short) i);
                    if (existing != null && !existing.isEmpty()) {
                        armor.setItemStackForSlot((short) i, null);
                        changed = true;
                    }
                } catch (Throwable ignored) {}
            }
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot != Inventory.INACTIVE_SLOT_INDEX) {
            ItemContainer hotbar = inventory.getHotbar();
            if (hotbar != null) {
                try {
                    var existing = hotbar.getItemStack(activeSlot);
                    if (existing != null && !existing.isEmpty()) {
                        hotbar.setItemStackForSlot(activeSlot, null);
                        changed = true;
                    }
                } catch (Throwable ignored) {}
            }
        }

        ItemContainer utility = inventory.getUtility();
        if (utility != null) {
            try {
                var existing = utility.getItemStack((short) 0);
                if (existing != null && !existing.isEmpty()) {
                    utility.setItemStackForSlot((short) 0, null);
                    changed = true;
                }
            } catch (Throwable ignored) {}
        }

        if (changed) inventory.markChanged();
    }

    public void buildAndApply(NPCEntity npcEntity, RPGMobsConfig config, int tierIndex,
                              RPGMobsConfig.MobRule mobRule, double durabilityMin, double durabilityMax) {
        if (npcEntity == null || config == null || mobRule == null) return;

        this.activeDurabilityMin = durabilityMin;
        this.activeDurabilityMax = durabilityMax;

        int clampedTierIndex = clampTierIndex(tierIndex);

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) {
            inventory = new Inventory();
            npcEntity.setInventory(inventory);
        }

        boolean inventoryChanged = false;

        inventoryChanged |= clearDisallowedArmor(inventory.getArmor(), mobRule.allowedArmorSlots);
        inventoryChanged |= equipArmor(inventory.getArmor(), config, clampedTierIndex, mobRule);

        ItemStack chosenWeapon = maybePickWeapon(inventory, config, clampedTierIndex, mobRule);
        if (chosenWeapon != null) {
            setInHand(inventory, chosenWeapon);
            inventoryChanged = true;
        }

        inventoryChanged |= applyInHandDurability(inventory, activeDurabilityMin, activeDurabilityMax);

        ItemStack chosenShield = maybeEquipUtilityShield(inventory, config, clampedTierIndex);
        if (chosenShield != null) inventoryChanged = true;

        if (inventoryChanged) inventory.markChanged();
    }

    private boolean clearDisallowedArmor(ItemContainer armorContainer, List<String> allowedArmorSlots) {
        if (armorContainer == null) return false;
        if (allowedArmorSlots == null || allowedArmorSlots.isEmpty()) return false;

        boolean noneMode = allowedArmorSlots.size() == 1
                && "NONE".equalsIgnoreCase(allowedArmorSlots.getFirst());

        if (noneMode) {
            boolean changed = false;
            for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
                try {
                    var existing = armorContainer.getItemStack((short) i);
                    if (existing != null && !existing.isEmpty()) {
                        armorContainer.setItemStackForSlot((short) i, null);
                        changed = true;
                    }
                } catch (Throwable ignored) {}
            }
            return changed;
        }

        Set<String> allowed = new HashSet<>();
        for (String s : allowedArmorSlots) {
            if (s != null) allowed.add(s.toUpperCase(Locale.ROOT));
        }

        boolean changed = false;
        for (int i = 0; i < ARMOR_SLOT_COUNT; i++) {
            try {
                var existing = armorContainer.getItemStack((short) i);
                if (existing == null || existing.isEmpty()) continue;

                String itemId = existing.getItemId();
                if (itemId == null || itemId.isBlank()) continue;

                String slotType = identifyArmorSlotType(itemId);
                if (slotType != null && !allowed.contains(slotType)) {
                    armorContainer.setItemStackForSlot((short) i, null);
                    changed = true;
                }
            } catch (Throwable ignored) {}
        }

        return changed;
    }

    private static String identifyArmorSlotType(String itemId) {
        return EquipmentHelpers.identifyArmorSlotType(itemId);
    }

    private boolean equipArmor(ItemContainer armorContainer, RPGMobsConfig config, int tierIndex,
                               RPGMobsConfig.MobRule mobRule) {
        if (armorContainer == null) return false;

        if (config.gearConfig.armorPiecesToEquipPerTier == null || config.gearConfig.armorPiecesToEquipPerTier.length <= tierIndex) {
            return false;
        }

        int armorSlotsToFill = clampArmorSlots(config.gearConfig.armorPiecesToEquipPerTier[tierIndex]);
        if (armorSlotsToFill == 0) return false;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        List<String> armorMaterials = pickArmorMaterialsOfRarity(config, wantedRarity);
        if (armorMaterials.isEmpty()) {
            for (String allowedRarity : allowedRaritiesForTier(config, tierIndex)) {
                armorMaterials = pickArmorMaterialsOfRarity(config, allowedRarity);
                if (!armorMaterials.isEmpty()) break;
            }
        }
        if (armorMaterials.isEmpty()) return false;

        if (mobRule.allowedArmorCategories != null && !mobRule.allowedArmorCategories.isEmpty()) {
            armorMaterials = filterArmorMaterialsByCategory(armorMaterials, mobRule.allowedArmorCategories,
                    config.gearConfig.armorCategoryTree);
            if (armorMaterials.isEmpty()) return false;
        }

        enum ArmorSlot {
            HEAD("Head"), CHEST("Chest"), HANDS("Hands"), LEGS("Legs");

            final String itemIdSuffix;

            ArmorSlot(String itemIdSuffix) {
                this.itemIdSuffix = itemIdSuffix;
            }
        }

        List<ArmorSlot> availableSlots = new ArrayList<>(List.of(ArmorSlot.values()));

        List<String> allowedArmorSlots = mobRule.allowedArmorSlots;
        if (allowedArmorSlots != null && !allowedArmorSlots.isEmpty()) {
            Set<String> allowed = new HashSet<>();
            for (String s : allowedArmorSlots) {
                if (s != null) allowed.add(s.toUpperCase(Locale.ROOT));
            }
            availableSlots.removeIf(slot -> !allowed.contains(slot.name()));
        }
        if (availableSlots.isEmpty()) return false;

        Collections.shuffle(availableSlots, random);

        boolean changed = false;

        int slotsToEquip = Math.min(armorSlotsToFill, availableSlots.size());
        for (int slotIndex = 0; slotIndex < slotsToEquip; slotIndex++) {
            ArmorSlot armorSlot = availableSlots.get(slotIndex);
            String material = armorMaterials.get(random.nextInt(armorMaterials.size()));

            String itemId = ARMOR_PREFIX + material + "_" + armorSlot.itemIdSuffix;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;

            ItemStack armorPiece = new ItemStack(itemId, 1);
            armorPiece = withRandomDurabilityFraction(armorPiece, activeDurabilityMin, activeDurabilityMax);

            InventoryHelper.useArmor(armorContainer, armorPiece);
            changed = true;
        }

        return changed;
    }

    private ItemStack maybePickWeapon(Inventory inventory, RPGMobsConfig config, int tierIndex,
                                      RPGMobsConfig.MobRule mobRule) {
        if (!mobRule.enabled) return null;

        if (mobRule.enableWeaponOverrideForTier == null || mobRule.enableWeaponOverrideForTier.length < TIERS_AMOUNT) {
            return null;
        }

        if (!mobRule.enableWeaponOverrideForTier[tierIndex]) return null;

        ItemStack itemInHand = inventory.getItemInHand();
        boolean isHandEmpty = (itemInHand == null || itemInHand.isEmpty());
        boolean isHandShield = !isHandEmpty && isShieldItemId(itemInHand.getItemId());

        boolean shouldEquipWeapon = switch (mobRule.weaponOverrideMode) {
            case NONE -> false;
            case ONLY_IF_EMPTY -> isHandEmpty || isHandShield;
            case ALWAYS -> true;
        };
        if (!shouldEquipWeapon) return null;

        String weaponItemId = pickWeaponForRuleAndTier(config, mobRule, tierIndex);
        if (weaponItemId == null || weaponItemId.isBlank()) {
            if (isHandEmpty) {
                int fallbackTier = Math.max(0, tierIndex - 1);
                if (fallbackTier != tierIndex) {
                    weaponItemId = pickWeaponForRuleAndTier(config, mobRule, fallbackTier);
                }
            }
            if (weaponItemId == null || weaponItemId.isBlank()) return null;
        }

        ItemStack weapon = new ItemStack(weaponItemId, 1);
        return withRandomDurabilityFraction(weapon, activeDurabilityMin, activeDurabilityMax);
    }

    private boolean applyInHandDurability(Inventory inventory, double minFraction, double maxFraction) {
        ItemStack itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) return false;
        if (itemInHand.getMaxDurability() <= 0) return false;

        ItemStack updatedInHand = withRandomDurabilityFraction(itemInHand, minFraction, maxFraction);

        byte activeHotbarSlot = inventory.getActiveHotbarSlot();
        if (activeHotbarSlot == Inventory.INACTIVE_SLOT_INDEX) return false;

        inventory.getHotbar().setItemStackForSlot(activeHotbarSlot, updatedInHand);
        return true;
    }

    private ItemStack withRandomDurabilityFraction(ItemStack itemStack, double minFraction, double maxFraction) {
        double durabilityFraction = minFraction + random.nextDouble() * (maxFraction - minFraction);
        durabilityFraction = clampDouble(durabilityFraction, 0.0, 1.0);

        double maxDurability = itemStack.getMaxDurability();
        if (maxDurability <= 0) return itemStack;

        return itemStack.withDurability(maxDurability * durabilityFraction);
    }

    private void setInHand(Inventory inventory, ItemStack itemStack) {
        byte activeHotbarSlot = inventory.getActiveHotbarSlot();
        if (activeHotbarSlot == Inventory.INACTIVE_SLOT_INDEX) {
            activeHotbarSlot = 0;
            inventory.setActiveHotbarSlot(activeHotbarSlot);
        }
        inventory.getHotbar().setItemStackForSlot(activeHotbarSlot, itemStack);
    }

    private ItemStack maybeEquipUtilityShield(Inventory inventory, RPGMobsConfig config, int tierIndex) {

        if (config.gearConfig.shieldUtilityChancePerTier == null || config.gearConfig.shieldUtilityChancePerTier.length < TIERS_AMOUNT) {
            return null;
        }

        int clampedTierIndex = clampTierIndex(tierIndex);
        double chance = clampDouble(config.gearConfig.shieldUtilityChancePerTier[clampedTierIndex], 0.0, 1.0);
        if (chance <= 0.0) return null;

        ItemContainer utilityContainer = inventory.getUtility();
        if (utilityContainer == null) return null;

        int utilityContainerSize = getContainerSizeSafe(utilityContainer);
        if (utilityContainerSize <= 0) return null;

        ItemStack itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) {

            itemInHand = inventory.getHotbar().getItemStack((short) 0);
            if (itemInHand == null || itemInHand.isEmpty()) return null;

            inventory.setActiveHotbarSlot((byte) 0);
        }

        String weaponItemId = itemInHand.getItemId();
        if (weaponItemId.isBlank()) return null;

        if (!isOneHandedWeaponIdInternal(config, weaponItemId)) return null;

        if (random.nextDouble() >= chance) return null;

        int utilitySlotIndex = clampInt(UTILITY_SLOT_INDEX, 0, utilityContainerSize - 1);
        short utilitySlot = (short) utilitySlotIndex;

        ItemStack existingUtilityItem;
        try {
            existingUtilityItem = utilityContainer.getItemStack(utilitySlot);
        } catch (Throwable ignored) {
            return null;
        }
        if (existingUtilityItem != null && !existingUtilityItem.isEmpty()) return null;

        String shieldItemId = pickShieldForTier(config, tierIndex);
        if (shieldItemId == null || shieldItemId.isBlank()) return null;

        ItemStack shield = new ItemStack(shieldItemId, 1);
        shield = withRandomDurabilityFraction(shield, activeDurabilityMin, activeDurabilityMax);

        try {
            utilityContainer.setItemStackForSlot(utilitySlot, shield);

            inventory.setActiveUtilitySlot((byte) utilitySlot);
        } catch (Throwable ignored) {
            return null;
        }

        return shield;
    }

    private boolean isOneHandedWeaponIdInternal(RPGMobsConfig config, String weaponItemId) {
        return EquipmentHelpers.isOneHandedWeapon(config.gearConfig.twoHandedWeaponIds, weaponItemId);
    }

    private boolean isShieldItemId(String itemId) {
        return EquipmentHelpers.isShieldItemId(itemId);
    }

    private String pickShieldForTier(RPGMobsConfig config, int tierIndex) {

        if (config.gearConfig.defaultWeaponCatalog == null || config.gearConfig.defaultWeaponCatalog.isEmpty())
            return null;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        ArrayList<String> shieldCandidates = new ArrayList<>();
        for (String itemId : config.gearConfig.defaultWeaponCatalog) {
            if (itemId == null || itemId.isBlank()) continue;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;

            String lowercaseItemId = itemId.toLowerCase(Locale.ROOT);
            if (!lowercaseItemId.contains(SHIELD_TOKEN)) continue;

            if (wantedRarity.equals(classifyWeaponRarity(config, itemId))) {
                shieldCandidates.add(itemId);
            }
        }

        if (shieldCandidates.isEmpty()) {
            for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                if (itemId == null || itemId.isBlank()) continue;
                if (Item.getAssetMap().getAsset(itemId) == null) continue;
                if (itemId.toLowerCase(Locale.ROOT).contains(SHIELD_TOKEN)) shieldCandidates.add(itemId);
            }
        }

        if (shieldCandidates.isEmpty()) return null;
        return shieldCandidates.get(random.nextInt(shieldCandidates.size()));
    }

    private String pickWeaponForRuleAndTier(RPGMobsConfig config, RPGMobsConfig.MobRule mobRule, int tierIndex) {
        if (config.gearConfig.defaultWeaponCatalog == null || config.gearConfig.defaultWeaponCatalog.isEmpty())
            return null;

        String wantedRarity = pickRarityForTier(config, tierIndex);

        List<String> allowedCategories = mobRule.allowedWeaponCategories;
        RPGMobsConfig.GearCategory weaponTree = config.gearConfig.weaponCategoryTree;
        boolean hasCategories = allowedCategories != null && !allowedCategories.isEmpty();

        ArrayList<String> weaponCandidates = new ArrayList<>();

        for (String itemId : config.gearConfig.defaultWeaponCatalog) {
            if (itemId == null || itemId.isBlank()) continue;
            if (Item.getAssetMap().getAsset(itemId) == null) continue;
            if (isShieldItemId(itemId)) continue;
            if (!wantedRarity.equals(classifyWeaponRarity(config, itemId))) continue;
            if (passesWeaponCategoryFilter(itemId, allowedCategories, weaponTree))
                weaponCandidates.add(itemId);
        }

        if (weaponCandidates.isEmpty()) {
            for (String allowedRarity : allowedRaritiesForTier(config, tierIndex)) {
                for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                    if (itemId == null || itemId.isBlank()) continue;
                    if (Item.getAssetMap().getAsset(itemId) == null) continue;
                    if (isShieldItemId(itemId)) continue;
                    if (!allowedRarity.equals(classifyWeaponRarity(config, itemId))) continue;
                    if (passesWeaponCategoryFilter(itemId, allowedCategories, weaponTree))
                        weaponCandidates.add(itemId);
                }
                if (!weaponCandidates.isEmpty()) break;
            }
        }

        if (weaponCandidates.isEmpty() && hasCategories) return null;

        if (weaponCandidates.isEmpty()) {
            for (String itemId : config.gearConfig.defaultWeaponCatalog) {
                if (itemId == null || itemId.isBlank()) continue;
                if (Item.getAssetMap().getAsset(itemId) == null) continue;
                if (isShieldItemId(itemId)) continue;
                if (passesWeaponCategoryFilter(itemId, allowedCategories, weaponTree))
                    weaponCandidates.add(itemId);
            }
        }

        if (weaponCandidates.isEmpty()) return null;
        return weaponCandidates.get(random.nextInt(weaponCandidates.size()));
    }

    private static boolean passesWeaponCategoryFilter(String weaponId, List<String> allowedCategories,
                                                      RPGMobsConfig.GearCategory weaponTree) {
        return EquipmentHelpers.passesWeaponCategoryFilter(weaponId, allowedCategories, weaponTree);
    }

    private static List<String> filterArmorMaterialsByCategory(List<String> materials,
                                                                List<String> allowedCategories,
                                                                RPGMobsConfig.GearCategory armorTree) {
        return EquipmentHelpers.filterArmorMaterialsByCategory(materials, allowedCategories, armorTree);
    }

    private String pickRarityForTier(RPGMobsConfig config, int tierIndex) {
        return EquipmentHelpers.pickRarityForTier(config.gearConfig.defaultTierEquipmentRarityWeights, tierIndex, random);
    }

    private List<String> allowedRaritiesForTier(RPGMobsConfig config, int tierIndex) {
        if (config.gearConfig.defaultTierAllowedRarities == null) return List.of(DEFAULT_RARITY);
        if (tierIndex < 0 || tierIndex >= config.gearConfig.defaultTierAllowedRarities.size()) {
            return List.of(DEFAULT_RARITY);
        }

        List<String> rarities = config.gearConfig.defaultTierAllowedRarities.get(tierIndex);
        return (rarities == null || rarities.isEmpty()) ? List.of(DEFAULT_RARITY) : rarities;
    }

    private List<String> pickArmorMaterialsOfRarity(RPGMobsConfig config, String wantedRarity) {
        if (config.gearConfig.defaultArmorMaterials == null || config.gearConfig.defaultArmorMaterials.isEmpty())
            return List.of();

        ArrayList<String> materials = new ArrayList<>();
        for (String material : config.gearConfig.defaultArmorMaterials) {
            if (material == null || material.isBlank()) continue;
            if (wantedRarity.equals(classifyArmorRarity(config, material))) materials.add(material);
        }

        return materials;
    }

    private String classifyArmorRarity(RPGMobsConfig config, String armorMaterial) {
        return EquipmentHelpers.classifyArmorRarity(config.gearConfig.defaultArmorRarityRules, armorMaterial);
    }

    private String classifyWeaponRarity(RPGMobsConfig config, String itemId) {
        return EquipmentHelpers.classifyWeaponRarity(config.gearConfig.defaultWeaponRarityRules, itemId);
    }
}
