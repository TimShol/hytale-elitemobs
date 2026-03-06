package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobRuleCategoryHelpersTest {

    @Test
    void collectAllMobRuleKeysFromFlatCategory() {
        var cat = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke", "Goblin_Hermit"));
        List<String> keys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        assertEquals(List.of("Goblin_Duke", "Goblin_Hermit"), keys);
    }

    @Test
    void collectAllMobRuleKeysRecursive() {
        var child1 = new RPGMobsConfig.MobRuleCategory("Skeletons", List.of("Skeleton_Warrior", "Skeleton_Archer"));
        var child2 = new RPGMobsConfig.MobRuleCategory("Zombies", List.of("Zombie_Shambler"));
        var root = new RPGMobsConfig.MobRuleCategory("Undead", List.of("Wraith_Ghost"));
        root.children.add(child1);
        root.children.add(child2);

        List<String> keys = MobRuleCategoryHelpers.collectAllMobRuleKeys(root);
        assertEquals(4, keys.size());
        assertTrue(keys.contains("Wraith_Ghost"));
        assertTrue(keys.contains("Skeleton_Warrior"));
        assertTrue(keys.contains("Skeleton_Archer"));
        assertTrue(keys.contains("Zombie_Shambler"));
    }

    @Test
    void collectAllMobRuleKeysEmptyCategory() {
        var cat = new RPGMobsConfig.MobRuleCategory("Empty", List.of());
        List<String> keys = MobRuleCategoryHelpers.collectAllMobRuleKeys(cat);
        assertTrue(keys.isEmpty());
    }

    @Test
    void findCategoryByNameFindsRoot() {
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of());
        var found = MobRuleCategoryHelpers.findCategoryByName(root, "All");
        assertSame(root, found);
    }

    @Test
    void findCategoryByNameFindsNestedChild() {
        var grandchild = new RPGMobsConfig.MobRuleCategory("Deep", List.of("Deep_Mob"));
        var child = new RPGMobsConfig.MobRuleCategory("Mid", List.of());
        child.children.add(grandchild);
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of());
        root.children.add(child);

        var found = MobRuleCategoryHelpers.findCategoryByName(root, "Deep");
        assertSame(grandchild, found);
    }

    @Test
    void findCategoryByNameReturnsNullWhenNotFound() {
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Mob1"));
        var found = MobRuleCategoryHelpers.findCategoryByName(root, "NonExistent");
        assertNull(found);
    }

    @Test
    void searchMobRuleKeysRecursiveFindsMatchesAcrossLevels() {
        var child = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke", "Goblin_Hermit"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton_Warrior", "Goblin_Scout"));
        root.children.add(child);

        List<String> results = MobRuleCategoryHelpers.searchMobRuleKeysRecursive(root, "goblin");
        assertEquals(3, results.size());
        assertTrue(results.contains("Goblin_Scout"));
        assertTrue(results.contains("Goblin_Duke"));
        assertTrue(results.contains("Goblin_Hermit"));
    }

    @Test
    void searchMobRuleKeysRecursiveReturnsEmptyForNoMatch() {
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton_Warrior"));
        List<String> results = MobRuleCategoryHelpers.searchMobRuleKeysRecursive(root, "zombie");
        assertTrue(results.isEmpty());
    }

    @Test
    void renameMobRuleKeyRecursiveRenamesInChild() {
        var child = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke", "Goblin_Hermit"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        root.children.add(child);

        boolean renamed = MobRuleCategoryHelpers.renameMobRuleKeyRecursive(root, "Goblin_Duke", "Goblin_King");
        assertTrue(renamed);
        assertEquals(List.of("Goblin_King", "Goblin_Hermit"), child.mobRuleKeys);
        assertEquals(List.of("Skeleton"), root.mobRuleKeys);
    }

    @Test
    void renameMobRuleKeyRecursiveReturnsFalseWhenNotFound() {
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        boolean renamed = MobRuleCategoryHelpers.renameMobRuleKeyRecursive(root, "Missing", "New");
        assertFalse(renamed);
        assertEquals(List.of("Skeleton"), root.mobRuleKeys);
    }

    @Test
    void removeMobRuleKeyRecursiveRemovesFromChild() {
        var child = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke", "Goblin_Hermit"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        root.children.add(child);

        boolean removed = MobRuleCategoryHelpers.removeMobRuleKeyRecursive(root, "Goblin_Duke");
        assertTrue(removed);
        assertEquals(List.of("Goblin_Hermit"), child.mobRuleKeys);
        assertEquals(List.of("Skeleton"), root.mobRuleKeys);
    }

    @Test
    void removeMobRuleKeyRecursiveReturnsFalseWhenNotFound() {
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        boolean removed = MobRuleCategoryHelpers.removeMobRuleKeyRecursive(root, "Missing");
        assertFalse(removed);
    }

    @Test
    void searchLootTemplateKeysRecursiveFindsAcrossLevels() {
        var child = new RPGMobsConfig.LootTemplateCategory("Dungeons", List.of("Dungeon_Boss", "Dungeon_Minion"));
        var root = new RPGMobsConfig.LootTemplateCategory("All", List.of("Forest_Drop", "Dungeon_Chest"));
        root.children.add(child);

        List<String> results = MobRuleCategoryHelpers.searchLootTemplateKeysRecursive(root, "dungeon");
        assertEquals(3, results.size());
        assertTrue(results.contains("Dungeon_Chest"));
        assertTrue(results.contains("Dungeon_Boss"));
        assertTrue(results.contains("Dungeon_Minion"));
    }

    @Test
    void isCategoryKeyReturnsTrueForPrefixedKey() {
        assertTrue(MobRuleCategoryHelpers.isCategoryKey("category:Goblins"));
        assertTrue(MobRuleCategoryHelpers.isCategoryKey("category:All"));
    }

    @Test
    void isCategoryKeyReturnsFalseForMobKey() {
        assertFalse(MobRuleCategoryHelpers.isCategoryKey("Goblin_Scout"));
        assertFalse(MobRuleCategoryHelpers.isCategoryKey(""));
        assertFalse(MobRuleCategoryHelpers.isCategoryKey(null));
    }

    @Test
    void toCategoryKeyAndFromCategoryKeyRoundTrip() {
        String catKey = MobRuleCategoryHelpers.toCategoryKey("Goblins");
        assertEquals("category:Goblins", catKey);
        assertEquals("Goblins", MobRuleCategoryHelpers.fromCategoryKey(catKey));
    }

    @Test
    void isMobKeyInCategoryFindsInSubcategory() {
        var child = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke", "Goblin_Hermit"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        root.children.add(child);

        assertTrue(MobRuleCategoryHelpers.isMobKeyInCategory(root, "Goblins", "Goblin_Duke"));
        assertTrue(MobRuleCategoryHelpers.isMobKeyInCategory(root, "All", "Goblin_Duke"));
        assertTrue(MobRuleCategoryHelpers.isMobKeyInCategory(root, "All", "Skeleton"));
    }

    @Test
    void isMobKeyInCategoryReturnsFalseForMissingCategoryOrMob() {
        var child = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Duke"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of("Skeleton"));
        root.children.add(child);

        assertFalse(MobRuleCategoryHelpers.isMobKeyInCategory(root, "NonExistent", "Goblin_Duke"));
        assertFalse(MobRuleCategoryHelpers.isMobKeyInCategory(root, "Goblins", "Skeleton"));
    }

    @Test
    void renameCategoryInLinkedKeysUpdatesCategoryEntry() {
        List<String> keys = new ArrayList<>(List.of("category:Goblins", "Skeleton_Warrior", "category:Undead"));
        MobRuleCategoryHelpers.renameCategoryInLinkedKeys(keys, "Goblins", "GoblinTribe");
        assertEquals("category:GoblinTribe", keys.get(0));
        assertEquals("Skeleton_Warrior", keys.get(1));
        assertEquals("category:Undead", keys.get(2));
    }

    @Test
    void renameCategoryInLinkedKeysDoesNothingWhenNotFound() {
        List<String> keys = new ArrayList<>(List.of("category:Goblins", "Skeleton_Warrior"));
        MobRuleCategoryHelpers.renameCategoryInLinkedKeys(keys, "Missing", "New");
        assertEquals(List.of("category:Goblins", "Skeleton_Warrior"), keys);
    }

    @Test
    void removeLootTemplateKeyRecursiveRemovesFromChild() {
        var child = new RPGMobsConfig.LootTemplateCategory("Dungeons", List.of("Dungeon_Boss", "Dungeon_Minion"));
        var root = new RPGMobsConfig.LootTemplateCategory("All", List.of("Forest_Drop"));
        root.children.add(child);

        boolean removed = MobRuleCategoryHelpers.removeLootTemplateKeyRecursive(root, "Dungeon_Boss");
        assertTrue(removed);
        assertEquals(List.of("Dungeon_Minion"), child.templateKeys);
    }

    @Test
    void collectAllGearItemKeysFromFlatCategory() {
        var cat = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"));
        List<String> keys = MobRuleCategoryHelpers.collectAllGearItemKeys(cat);
        assertEquals(List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"), keys);
    }

    @Test
    void collectAllGearItemKeysRecursive() {
        var child = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron"));
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Axe_Iron"));
        root.children.add(child);

        List<String> keys = MobRuleCategoryHelpers.collectAllGearItemKeys(root);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("Weapon_Axe_Iron"));
        assertTrue(keys.contains("Weapon_Sword_Iron"));
    }

    @Test
    void collectAllGearItemKeysEmptyCategory() {
        var cat = new RPGMobsConfig.GearCategory("Empty", List.of());
        List<String> keys = MobRuleCategoryHelpers.collectAllGearItemKeys(cat);
        assertTrue(keys.isEmpty());
    }

    @Test
    void findGearCategoryByNameFindsRoot() {
        var root = new RPGMobsConfig.GearCategory("All", List.of());
        var found = MobRuleCategoryHelpers.findGearCategoryByName(root, "All");
        assertSame(root, found);
    }

    @Test
    void findGearCategoryByNameFindsNestedChild() {
        var grandchild = new RPGMobsConfig.GearCategory("Longswords", List.of("Weapon_Longsword_Iron"));
        var child = new RPGMobsConfig.GearCategory("Swords", List.of());
        child.children.add(grandchild);
        var root = new RPGMobsConfig.GearCategory("All", List.of());
        root.children.add(child);

        var found = MobRuleCategoryHelpers.findGearCategoryByName(root, "Longswords");
        assertSame(grandchild, found);
    }

    @Test
    void findGearCategoryByNameReturnsNullWhenNotFound() {
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Sword_Iron"));
        var found = MobRuleCategoryHelpers.findGearCategoryByName(root, "NonExistent");
        assertNull(found);
    }

    @Test
    void searchGearItemKeysRecursiveFindsMatchesAcrossLevels() {
        var child = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"));
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Axe_Iron", "Weapon_Sword_Copper"));
        root.children.add(child);

        List<String> results = MobRuleCategoryHelpers.searchGearItemKeysRecursive(root, "sword");
        assertEquals(3, results.size());
        assertTrue(results.contains("Weapon_Sword_Copper"));
        assertTrue(results.contains("Weapon_Sword_Iron"));
        assertTrue(results.contains("Weapon_Sword_Steel"));
    }

    @Test
    void searchGearItemKeysRecursiveReturnsEmptyForNoMatch() {
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Sword_Iron"));
        List<String> results = MobRuleCategoryHelpers.searchGearItemKeysRecursive(root, "bow");
        assertTrue(results.isEmpty());
    }

    @Test
    void removeGearItemKeyRecursiveRemovesFromChild() {
        var child = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"));
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Axe_Iron"));
        root.children.add(child);

        boolean removed = MobRuleCategoryHelpers.removeGearItemKeyRecursive(root, "Weapon_Sword_Iron");
        assertTrue(removed);
        assertEquals(List.of("Weapon_Sword_Steel"), child.itemKeys);
        assertEquals(List.of("Weapon_Axe_Iron"), root.itemKeys);
    }

    @Test
    void removeGearItemKeyRecursiveReturnsFalseWhenNotFound() {
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Sword_Iron"));
        boolean removed = MobRuleCategoryHelpers.removeGearItemKeyRecursive(root, "Missing");
        assertFalse(removed);
    }

    @Test
    void renameGearItemKeyRecursiveRenamesInChild() {
        var child = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"));
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Axe_Iron"));
        root.children.add(child);

        boolean renamed = MobRuleCategoryHelpers.renameGearItemKeyRecursive(root, "Weapon_Sword_Iron", "Weapon_Sword_Gold");
        assertTrue(renamed);
        assertEquals(List.of("Weapon_Sword_Gold", "Weapon_Sword_Steel"), child.itemKeys);
    }

    @Test
    void renameGearItemKeyRecursiveReturnsFalseWhenNotFound() {
        var root = new RPGMobsConfig.GearCategory("All", List.of("Weapon_Sword_Iron"));
        boolean renamed = MobRuleCategoryHelpers.renameGearItemKeyRecursive(root, "Missing", "New");
        assertFalse(renamed);
        assertEquals(List.of("Weapon_Sword_Iron"), root.itemKeys);
    }

    @Test
    void collectGearCategoryNamesIncludesAllDescendants() {
        var grandchild = new RPGMobsConfig.GearCategory("Longswords", List.of());
        var child = new RPGMobsConfig.GearCategory("Swords", List.of());
        child.children.add(grandchild);
        var root = new RPGMobsConfig.GearCategory("All", List.of());
        root.children.add(child);

        var names = new HashSet<String>();
        MobRuleCategoryHelpers.collectGearCategoryNames(root, names);
        assertTrue(names.contains("All"));
        assertTrue(names.contains("Swords"));
        assertTrue(names.contains("Longswords"));
        assertEquals(3, names.size());
    }

    @Test
    void collectAllGearCategoryNamesExcludesRoot() {
        var child1 = new RPGMobsConfig.GearCategory("Swords", List.of());
        var child2 = new RPGMobsConfig.GearCategory("Axes", List.of());
        var root = new RPGMobsConfig.GearCategory("All", List.of());
        root.children.add(child1);
        root.children.add(child2);

        List<String> names = MobRuleCategoryHelpers.collectAllGearCategoryNames(root);
        assertTrue(names.contains("Swords"));
        assertTrue(names.contains("Axes"));
        assertFalse(names.contains("All"));
        assertEquals(2, names.size());
    }
}
