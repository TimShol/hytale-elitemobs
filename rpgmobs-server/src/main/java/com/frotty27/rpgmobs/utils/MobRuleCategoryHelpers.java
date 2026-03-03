package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MobRuleCategoryHelpers {

    public static final String CATEGORY_PREFIX = "category:";

    private MobRuleCategoryHelpers() {
    }

    public static boolean isCategoryKey(String key) {
        return key != null && key.startsWith(CATEGORY_PREFIX);
    }

    public static String toCategoryKey(String categoryName) {
        return CATEGORY_PREFIX + categoryName;
    }

    public static String fromCategoryKey(String categoryKey) {
        return categoryKey.substring(CATEGORY_PREFIX.length());
    }

    public static boolean isMobKeyInCategory(RPGMobsConfig.MobRuleCategory root, String categoryName, String mobKey) {
        RPGMobsConfig.MobRuleCategory cat = findCategoryByName(root, categoryName);
        if (cat == null) return false;
        return collectAllMobRuleKeys(cat).contains(mobKey);
    }

    public static void renameCategoryInLinkedKeys(List<String> linkedKeys, String oldName, String newName) {
        String oldKey = toCategoryKey(oldName);
        String newKey = toCategoryKey(newName);
        int idx = linkedKeys.indexOf(oldKey);
        if (idx >= 0) {
            linkedKeys.set(idx, newKey);
        }
    }

    public static List<String> collectAllMobRuleKeys(RPGMobsConfig.MobRuleCategory category) {
        List<String> keys = new ArrayList<>(category.mobRuleKeys);
        for (RPGMobsConfig.MobRuleCategory child : category.children) {
            keys.addAll(collectAllMobRuleKeys(child));
        }
        return keys;
    }

    public static RPGMobsConfig.MobRuleCategory findCategoryByName(RPGMobsConfig.MobRuleCategory root, String name) {
        if (root.name.equals(name)) return root;
        for (RPGMobsConfig.MobRuleCategory child : root.children) {
            RPGMobsConfig.MobRuleCategory found = findCategoryByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

    public static List<String> searchMobRuleKeysRecursive(RPGMobsConfig.MobRuleCategory root, String filter) {
        List<String> result = new ArrayList<>();
        String lowerFilter = filter.toLowerCase();
        for (String key : root.mobRuleKeys) {
            if (key.toLowerCase().contains(lowerFilter)) {
                result.add(key);
            }
        }
        for (RPGMobsConfig.MobRuleCategory child : root.children) {
            result.addAll(searchMobRuleKeysRecursive(child, filter));
        }
        return result;
    }

    public static boolean renameMobRuleKeyRecursive(RPGMobsConfig.MobRuleCategory root, String oldKey, String newKey) {
        int idx = root.mobRuleKeys.indexOf(oldKey);
        if (idx >= 0) {
            root.mobRuleKeys.set(idx, newKey);
            return true;
        }
        for (RPGMobsConfig.MobRuleCategory child : root.children) {
            if (renameMobRuleKeyRecursive(child, oldKey, newKey)) return true;
        }
        return false;
    }

    public static boolean removeMobRuleKeyRecursive(RPGMobsConfig.MobRuleCategory root, String key) {
        if (root.mobRuleKeys.remove(key)) return true;
        for (RPGMobsConfig.MobRuleCategory child : root.children) {
            if (removeMobRuleKeyRecursive(child, key)) return true;
        }
        return false;
    }

    public static List<String> searchLootTemplateKeysRecursive(RPGMobsConfig.LootTemplateCategory root, String filter) {
        List<String> result = new ArrayList<>();
        String lowerFilter = filter.toLowerCase();
        for (String key : root.templateKeys) {
            if (key.toLowerCase().contains(lowerFilter)) {
                result.add(key);
            }
        }
        for (RPGMobsConfig.LootTemplateCategory child : root.children) {
            result.addAll(searchLootTemplateKeysRecursive(child, filter));
        }
        return result;
    }

    public static boolean removeLootTemplateKeyRecursive(RPGMobsConfig.LootTemplateCategory root, String key) {
        if (root.templateKeys.remove(key)) return true;
        for (RPGMobsConfig.LootTemplateCategory child : root.children) {
            if (removeLootTemplateKeyRecursive(child, key)) return true;
        }
        return false;
    }

    public static List<String> collectAllGearItemKeys(RPGMobsConfig.GearCategory category) {
        List<String> keys = new ArrayList<>(category.itemKeys);
        for (RPGMobsConfig.GearCategory child : category.children) {
            keys.addAll(collectAllGearItemKeys(child));
        }
        return keys;
    }

    public static RPGMobsConfig.GearCategory findGearCategoryByName(RPGMobsConfig.GearCategory root, String name) {
        if (root.name.equals(name)) return root;
        for (RPGMobsConfig.GearCategory child : root.children) {
            RPGMobsConfig.GearCategory found = findGearCategoryByName(child, name);
            if (found != null) return found;
        }
        return null;
    }

    public static List<String> searchGearItemKeysRecursive(RPGMobsConfig.GearCategory root, String filter) {
        List<String> result = new ArrayList<>();
        String lowerFilter = filter.toLowerCase();
        for (String key : root.itemKeys) {
            if (key.toLowerCase().contains(lowerFilter)) {
                result.add(key);
            }
        }
        for (RPGMobsConfig.GearCategory child : root.children) {
            result.addAll(searchGearItemKeysRecursive(child, filter));
        }
        return result;
    }

    public static boolean removeGearItemKeyRecursive(RPGMobsConfig.GearCategory root, String key) {
        if (root.itemKeys.remove(key)) return true;
        for (RPGMobsConfig.GearCategory child : root.children) {
            if (removeGearItemKeyRecursive(child, key)) return true;
        }
        return false;
    }

    public static boolean renameGearItemKeyRecursive(RPGMobsConfig.GearCategory root, String oldKey, String newKey) {
        int idx = root.itemKeys.indexOf(oldKey);
        if (idx >= 0) {
            root.itemKeys.set(idx, newKey);
            return true;
        }
        for (RPGMobsConfig.GearCategory child : root.children) {
            if (renameGearItemKeyRecursive(child, oldKey, newKey)) return true;
        }
        return false;
    }

    public static void collectGearCategoryNames(RPGMobsConfig.GearCategory cat, Set<String> names) {
        names.add(cat.name);
        for (RPGMobsConfig.GearCategory child : cat.children) {
            collectGearCategoryNames(child, names);
        }
    }

    public static List<String> collectAllGearCategoryNames(RPGMobsConfig.GearCategory root) {
        List<String> names = new ArrayList<>();
        collectGearCategoryNamesIntoList(root, names);
        return names;
    }

    private static void collectGearCategoryNamesIntoList(RPGMobsConfig.GearCategory cat, List<String> names) {
        for (RPGMobsConfig.GearCategory child : cat.children) {
            names.add(child.name);
            collectGearCategoryNamesIntoList(child, names);
        }
    }

    private static final String ARMOR_PREFIX = "Armor_";
    private static final String[] ARMOR_SLOT_SUFFIXES = {"_Head", "_Chest", "_Hands", "_Legs"};

    public static void expandArmorMaterialsToFullIds(RPGMobsConfig.GearCategory cat) {
        List<String> expanded = new ArrayList<>();
        for (String key : cat.itemKeys) {
            if (key.startsWith(ARMOR_PREFIX)) {
                expanded.add(key);
            } else {
                for (String suffix : ARMOR_SLOT_SUFFIXES) {
                    expanded.add(ARMOR_PREFIX + key + suffix);
                }
            }
        }
        cat.itemKeys.clear();
        cat.itemKeys.addAll(expanded);
        for (RPGMobsConfig.GearCategory child : cat.children) {
            expandArmorMaterialsToFullIds(child);
        }
    }
}
