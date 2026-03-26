package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
        int position = linkedKeys.indexOf(oldKey);
        if (position >= 0) {
            linkedKeys.set(position, newKey);
        }
    }

    public static List<String> collectAllMobRuleKeys(RPGMobsConfig.MobRuleCategory category) {
        return collectAllKeys(category, c -> c.mobRuleKeys, c -> c.children);
    }

    public static RPGMobsConfig.MobRuleCategory findCategoryByName(RPGMobsConfig.MobRuleCategory root, String name) {
        return findByName(root, name, c -> c.name, c -> c.children);
    }

    public static List<String> searchMobRuleKeysRecursive(RPGMobsConfig.MobRuleCategory root, String filter) {
        return searchKeysRecursive(root, filter, c -> c.mobRuleKeys, c -> c.children);
    }

    public static boolean renameMobRuleKeyRecursive(RPGMobsConfig.MobRuleCategory root, String oldKey, String newKey) {
        return renameKeyRecursive(root, oldKey, newKey, c -> c.mobRuleKeys, c -> c.children);
    }

    public static boolean removeMobRuleKeyRecursive(RPGMobsConfig.MobRuleCategory root, String key) {
        return removeKeyRecursive(root, key, c -> c.mobRuleKeys, c -> c.children);
    }

    public static List<String> searchLootTemplateKeysRecursive(RPGMobsConfig.LootTemplateCategory root, String filter) {
        return searchKeysRecursive(root, filter, c -> c.templateKeys, c -> c.children);
    }

    public static boolean removeLootTemplateKeyRecursive(RPGMobsConfig.LootTemplateCategory root, String key) {
        return removeKeyRecursive(root, key, c -> c.templateKeys, c -> c.children);
    }

    public static List<String> collectAllGearItemKeys(RPGMobsConfig.GearCategory category) {
        return collectAllKeys(category, c -> c.itemKeys, c -> c.children);
    }

    public static RPGMobsConfig.GearCategory findGearCategoryByName(RPGMobsConfig.GearCategory root, String name) {
        return findByName(root, name, c -> c.name, c -> c.children);
    }

    public static List<String> searchGearItemKeysRecursive(RPGMobsConfig.GearCategory root, String filter) {
        return searchKeysRecursive(root, filter, c -> c.itemKeys, c -> c.children);
    }

    public static boolean removeGearItemKeyRecursive(RPGMobsConfig.GearCategory root, String key) {
        return removeKeyRecursive(root, key, c -> c.itemKeys, c -> c.children);
    }

    public static boolean renameGearItemKeyRecursive(RPGMobsConfig.GearCategory root, String oldKey, String newKey) {
        return renameKeyRecursive(root, oldKey, newKey, c -> c.itemKeys, c -> c.children);
    }

    private static <T> List<String> collectAllKeys(T node, Function<T, List<String>> getKeys,
                                                    Function<T, List<T>> getChildren) {
        var keys = new ArrayList<>(getKeys.apply(node));
        for (T child : getChildren.apply(node)) {
            keys.addAll(collectAllKeys(child, getKeys, getChildren));
        }
        return keys;
    }

    private static <T> T findByName(T node, String name, Function<T, String> getName,
                                     Function<T, List<T>> getChildren) {
        if (getName.apply(node).equals(name)) return node;
        for (T child : getChildren.apply(node)) {
            T found = findByName(child, name, getName, getChildren);
            if (found != null) return found;
        }
        return null;
    }

    private static <T> List<String> searchKeysRecursive(T node, String filter, Function<T, List<String>> getKeys,
                                                         Function<T, List<T>> getChildren) {
        var result = new ArrayList<String>();
        var lowerFilter = filter.toLowerCase();
        for (String key : getKeys.apply(node)) {
            if (key.toLowerCase().contains(lowerFilter)) {
                result.add(key);
            }
        }
        for (T child : getChildren.apply(node)) {
            result.addAll(searchKeysRecursive(child, filter, getKeys, getChildren));
        }
        return result;
    }

    private static <T> boolean removeKeyRecursive(T node, String key, Function<T, List<String>> getKeys,
                                                   Function<T, List<T>> getChildren) {
        if (getKeys.apply(node).remove(key)) return true;
        for (T child : getChildren.apply(node)) {
            if (removeKeyRecursive(child, key, getKeys, getChildren)) return true;
        }
        return false;
    }

    private static <T> boolean renameKeyRecursive(T node, String oldKey, String newKey,
                                                   Function<T, List<String>> getKeys,
                                                   Function<T, List<T>> getChildren) {
        var keys = getKeys.apply(node);
        int position = keys.indexOf(oldKey);
        if (position >= 0) {
            keys.set(position, newKey);
            return true;
        }
        for (T child : getChildren.apply(node)) {
            if (renameKeyRecursive(child, oldKey, newKey, getKeys, getChildren)) return true;
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
