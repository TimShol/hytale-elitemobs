package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.frotty27.rpgmobs.utils.Constants.DEFAULT_RARITY;

public final class EquipmentHelpers {

    private static final String SHIELD_TOKEN = "weapon_shield";
    private static final String ARMOR_PREFIX = "Armor_";
    private static final String[] ARMOR_SLOT_SUFFIXES = {"_Head", "_Chest", "_Hands", "_Legs"};

    private static final String ROOT_ATTACK_PREFIX = "Root_RPGMobs_Attack_";
    private static final String ROOT_ATTACK_FALLBACK = "Root_RPGMobs_Attack_Melee";

    private static final Set<String> KNOWN_WEAPON_CATEGORIES = Set.of(
            "Daggers", "Swords", "Longswords", "Axes", "Battleaxes", "Maces", "Clubs", "Spears"
    );

    private EquipmentHelpers() {}

    public static @Nullable String identifyArmorSlotType(String itemId) {
        if (itemId.endsWith("_Head")) return "HEAD";
        if (itemId.endsWith("_Chest")) return "CHEST";
        if (itemId.endsWith("_Hands")) return "HANDS";
        if (itemId.endsWith("_Legs")) return "LEGS";
        return null;
    }

    public static String classifyWeaponRarity(Map<String, String> weaponRarityRules, String itemId) {
        return classifyRarityByFragment(weaponRarityRules, itemId);
    }

    public static String classifyArmorRarity(Map<String, String> armorRarityRules, String armorMaterial) {
        return classifyRarityByFragment(armorRarityRules, armorMaterial);
    }

    private static String classifyRarityByFragment(Map<String, String> rarityRules, String identifier) {
        if (rarityRules == null || rarityRules.isEmpty()) return DEFAULT_RARITY;

        var lowercaseIdentifier = identifier.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : rarityRules.entrySet()) {
            var fragment = entry.getKey();
            if (fragment == null || fragment.isBlank()) continue;

            if (lowercaseIdentifier.contains(fragment.toLowerCase(Locale.ROOT))) {
                var rarity = entry.getValue();
                return (rarity == null || rarity.isBlank()) ? DEFAULT_RARITY : rarity;
            }
        }

        return DEFAULT_RARITY;
    }

    public static boolean isShieldItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        return itemId.toLowerCase(Locale.ROOT).contains(SHIELD_TOKEN);
    }

    public static boolean isOneHandedWeapon(List<String> twoHandedWeaponIds, String weaponItemId) {
        String lowercaseWeaponId = weaponItemId.toLowerCase(Locale.ROOT);
        if (lowercaseWeaponId.contains(SHIELD_TOKEN)) return false;

        if (twoHandedWeaponIds == null) return true;

        for (String twoHandedFragment : twoHandedWeaponIds) {
            if (twoHandedFragment == null || twoHandedFragment.isBlank()) continue;
            if (lowercaseWeaponId.contains(twoHandedFragment.toLowerCase(Locale.ROOT))) return false;
        }

        return true;
    }

    public static boolean passesWeaponCategoryFilter(String weaponId, List<String> allowedCategories,
                                                      RPGMobsConfig.GearCategory weaponTree) {
        if (allowedCategories == null || allowedCategories.isEmpty()) return true;
        for (String entry : allowedCategories) {
            if (MobRuleCategoryHelpers.isCategoryKey(entry)) {
                if (weaponTree == null) continue;
                String catName = MobRuleCategoryHelpers.fromCategoryKey(entry);
                RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(weaponTree, catName);
                if (cat != null && MobRuleCategoryHelpers.collectAllGearItemKeys(cat).contains(weaponId)) return true;
            } else {
                if (entry.equals(weaponId)) return true;
            }
        }
        return false;
    }

    public static String resolveBasicAttackRootForWeapon(@Nullable String weaponId,
                                                            RPGMobsConfig.@Nullable GearCategory weaponTree) {
        if (weaponId == null || weaponId.isBlank() || weaponTree == null) return ROOT_ATTACK_FALLBACK;

        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (MobRuleCategoryHelpers.collectAllGearItemKeys(category).contains(weaponId)) {
                if (KNOWN_WEAPON_CATEGORIES.contains(category.name)) {
                    return ROOT_ATTACK_PREFIX + category.name;
                }
                break;
            }
        }
        return ROOT_ATTACK_FALLBACK;
    }

    public static @Nullable String resolveWeaponCategory(@Nullable String weaponId,
                                                          RPGMobsConfig.@Nullable GearCategory weaponTree) {
        if (weaponId == null || weaponId.isBlank() || weaponTree == null) return null;

        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (MobRuleCategoryHelpers.collectAllGearItemKeys(category).contains(weaponId)) {
                if (KNOWN_WEAPON_CATEGORIES.contains(category.name)) {
                    return category.name;
                }
                break;
            }
        }
        return null;
    }

    public static @Nullable String detectFactionFromRoleName(@Nullable String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        String lowerRole = roleName.toLowerCase();
        if (lowerRole.contains("trork") || lowerRole.contains("chieftain")) return "Trork";
        if (lowerRole.contains("outlander") || lowerRole.contains("marauder")
                || lowerRole.contains("berserker") || lowerRole.contains("cultist")
                || lowerRole.contains("stalker") || lowerRole.contains("peon")
                || lowerRole.contains("brute")) return "Outlander";
        if (lowerRole.contains("goblin") || lowerRole.contains("scrapper")
                || lowerRole.contains("scavenger") || lowerRole.contains("lobber")
                || lowerRole.contains("hermit") || lowerRole.contains("thief")) return "Goblin";
        if (lowerRole.contains("skeleton") || lowerRole.contains("risen")
                || lowerRole.contains("wraith")) return "Skeleton";
        return null;
    }

    public static List<String> filterArmorMaterialsByCategory(List<String> materials,
                                                               List<String> allowedCategories,
                                                               RPGMobsConfig.GearCategory armorTree) {
        if (allowedCategories == null || allowedCategories.isEmpty()) return materials;
        Set<String> allowed = new HashSet<>();
        Set<String> directItems = new HashSet<>();
        for (String entry : allowedCategories) {
            if (MobRuleCategoryHelpers.isCategoryKey(entry)) {
                if (armorTree == null) continue;
                String catName = MobRuleCategoryHelpers.fromCategoryKey(entry);
                RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(armorTree, catName);
                if (cat != null) {
                    allowed.addAll(MobRuleCategoryHelpers.collectAllGearItemKeys(cat));
                }
            } else {
                directItems.add(entry);
            }
        }
        List<String> filtered = new ArrayList<>();
        for (String material : materials) {
            boolean matched = false;
            for (String suffix : ARMOR_SLOT_SUFFIXES) {
                String fullId = ARMOR_PREFIX + material + suffix;
                if (allowed.contains(fullId) || directItems.contains(fullId)) {
                    matched = true;
                    break;
                }
            }
            if (matched) filtered.add(material);
        }
        return filtered;
    }

    public static String pickRarityForTier(List<Map<String, Double>> tierRarityWeights, int tierIndex, Random random) {
        if (tierRarityWeights == null || tierRarityWeights.isEmpty()) return DEFAULT_RARITY;
        if (tierIndex < 0 || tierIndex >= tierRarityWeights.size()) return DEFAULT_RARITY;

        Map<String, Double> weights = tierRarityWeights.get(tierIndex);
        if (weights == null || weights.isEmpty()) return DEFAULT_RARITY;

        double totalWeight = 0.0;
        for (double weight : weights.values()) totalWeight += Math.max(0.0, weight);
        if (totalWeight <= 0.0) return DEFAULT_RARITY;

        double selection = random.nextDouble() * totalWeight;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            selection -= Math.max(0.0, entry.getValue());
            if (selection <= 0.0) return entry.getKey();
        }

        return weights.keySet().iterator().next();
    }
}
