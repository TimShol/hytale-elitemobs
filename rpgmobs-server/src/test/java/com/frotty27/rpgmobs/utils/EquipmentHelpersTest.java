package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class EquipmentHelpersTest {

    @Test
    public void identifyArmorSlotTypeHead() {
        assertEquals("HEAD", EquipmentHelpers.identifyArmorSlotType("Armor_Iron_Head"));
    }

    @Test
    public void identifyArmorSlotTypeChest() {
        assertEquals("CHEST", EquipmentHelpers.identifyArmorSlotType("Armor_Iron_Chest"));
    }

    @Test
    public void identifyArmorSlotTypeHands() {
        assertEquals("HANDS", EquipmentHelpers.identifyArmorSlotType("Armor_Iron_Hands"));
    }

    @Test
    public void identifyArmorSlotTypeLegs() {
        assertEquals("LEGS", EquipmentHelpers.identifyArmorSlotType("Armor_Iron_Legs"));
    }

    @Test
    public void identifyArmorSlotTypeReturnsNullForUnknown() {
        assertNull(EquipmentHelpers.identifyArmorSlotType("Weapon_Sword_Iron"));
    }

    @Test
    public void classifyWeaponRarityFirstMatchWins() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("scarab", "rare");
        rules.put("iron", "common");

        assertEquals("rare", EquipmentHelpers.classifyWeaponRarity(rules, "Weapon_Scarab_Sword"));
    }

    @Test
    public void classifyWeaponRarityDefaultsToCommon() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("iron", "common");

        assertEquals("common", EquipmentHelpers.classifyWeaponRarity(rules, "Weapon_Unknown_Sword"));
    }

    @Test
    public void classifyWeaponRarityNullRulesReturnsCommon() {
        assertEquals("common", EquipmentHelpers.classifyWeaponRarity(null, "Weapon_Sword"));
    }

    @Test
    public void classifyArmorRarityMatchesMaterial() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("iron", "uncommon");
        rules.put("thorium", "rare");

        assertEquals("uncommon", EquipmentHelpers.classifyArmorRarity(rules, "iron"));
        assertEquals("rare", EquipmentHelpers.classifyArmorRarity(rules, "thorium"));
    }

    @Test
    public void classifyArmorRarityDefaultsToCommon() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("iron", "uncommon");

        assertEquals("common", EquipmentHelpers.classifyArmorRarity(rules, "mythril_unknown"));
    }

    @Test
    public void isShieldItemIdDetectsShields() {
        assertTrue(EquipmentHelpers.isShieldItemId("Weapon_Shield_Tower_Iron"));
        assertTrue(EquipmentHelpers.isShieldItemId("weapon_shield_basic"));
    }

    @Test
    public void isShieldItemIdRejectsNonShields() {
        assertFalse(EquipmentHelpers.isShieldItemId("Weapon_Sword_Iron"));
        assertFalse(EquipmentHelpers.isShieldItemId(""));
        assertFalse(EquipmentHelpers.isShieldItemId(null));
    }

    @Test
    public void isOneHandedWeaponDetectsTwoHanded() {
        var twoHandedKeywords = List.of("battleaxe", "longsword", "spear");
        assertFalse(EquipmentHelpers.isOneHandedWeapon(twoHandedKeywords, "Weapon_Battleaxe_Iron"));
        assertFalse(EquipmentHelpers.isOneHandedWeapon(twoHandedKeywords, "Weapon_Longsword_Cobalt"));
    }

    @Test
    public void isOneHandedWeaponReturnsTrueForNormal() {
        var twoHandedKeywords = List.of("battleaxe", "longsword");
        assertTrue(EquipmentHelpers.isOneHandedWeapon(twoHandedKeywords, "Weapon_Sword_Iron"));
        assertTrue(EquipmentHelpers.isOneHandedWeapon(twoHandedKeywords, "Weapon_Axe_Cobalt"));
    }

    @Test
    public void isOneHandedWeaponRejectsShields() {
        var twoHandedKeywords = List.of("battleaxe");
        assertFalse(EquipmentHelpers.isOneHandedWeapon(twoHandedKeywords, "Weapon_Shield_Iron"));
    }

    @Test
    public void passesWeaponCategoryFilterAllowsAllWhenEmpty() {
        assertTrue(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Sword", List.of(), null));
        assertTrue(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Sword", null, null));
    }

    @Test
    public void passesWeaponCategoryFilterMatchesDirectEntry() {
        var allowed = List.of("Weapon_Sword_Iron");
        assertTrue(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Sword_Iron", allowed, null));
        assertFalse(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Axe_Iron", allowed, null));
    }

    @Test
    public void passesWeaponCategoryFilterMatchesCategoryEntry() {
        var swords = new RPGMobsConfig.GearCategory("Swords",
                List.of("Weapon_Sword_Iron", "Weapon_Sword_Cobalt"));
        var tree = new RPGMobsConfig.GearCategory("All", List.of(), swords);

        var allowed = List.of("category:Swords");
        assertTrue(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Sword_Iron", allowed, tree));
        assertFalse(EquipmentHelpers.passesWeaponCategoryFilter("Weapon_Axe_Iron", allowed, tree));
    }

    @Test
    public void filterArmorMaterialsReturnsAllWhenNoCategories() {
        var materials = List.of("Iron", "Cobalt");
        var result = EquipmentHelpers.filterArmorMaterialsByCategory(materials, List.of(), null);
        assertEquals(materials, result);
    }

    @Test
    public void filterArmorMaterialsKeepsMatchingCategories() {
        var ironArmor = new RPGMobsConfig.GearCategory("Iron",
                List.of("Armor_Iron_Head", "Armor_Iron_Chest", "Armor_Iron_Hands", "Armor_Iron_Legs"));
        var tree = new RPGMobsConfig.GearCategory("All", List.of(), ironArmor);

        var materials = List.of("Iron", "Cobalt");
        var allowed = List.of("category:Iron");
        var result = EquipmentHelpers.filterArmorMaterialsByCategory(materials, allowed, tree);

        assertEquals(1, result.size());
        assertEquals("Iron", result.getFirst());
    }

    @Test
    public void pickRarityForTierUsesWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("common", 0.0);
        weights.put("rare", 1.0);

        var tierWeights = List.of(weights);
        var random = new Random(42);

        assertEquals("rare", EquipmentHelpers.pickRarityForTier(tierWeights, 0, random));
    }

    @Test
    public void pickRarityForTierFallsBackToCommon() {
        assertEquals("common", EquipmentHelpers.pickRarityForTier(null, 0, new Random()));
        assertEquals("common", EquipmentHelpers.pickRarityForTier(List.of(), 0, new Random()));
    }

    @Test
    public void pickRarityForTierOutOfBoundsReturnsCommon() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("rare", 1.0);
        var tierWeights = List.of(weights);

        assertEquals("common", EquipmentHelpers.pickRarityForTier(tierWeights, 5, new Random()));
    }
}
