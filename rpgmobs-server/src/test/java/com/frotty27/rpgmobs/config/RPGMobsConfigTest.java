package com.frotty27.rpgmobs.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RPGMobsConfigTest {

    @Test
    void extraDropRuleEqualsIdentical() {
        var a = new RPGMobsConfig.ExtraDropRule();
        a.itemId = "Iron_Sword";
        a.chance = 0.5;
        a.minQty = 1;
        a.maxQty = 3;
        a.enabledPerTier = new boolean[]{true, true, false, false, true};

        var b = new RPGMobsConfig.ExtraDropRule();
        b.itemId = "Iron_Sword";
        b.chance = 0.5;
        b.minQty = 1;
        b.maxQty = 3;
        b.enabledPerTier = new boolean[]{true, true, false, false, true};

        assertEquals(a, b);
    }

    @Test
    void extraDropRuleNotEqualsDifferentItemId() {
        var a = new RPGMobsConfig.ExtraDropRule();
        a.itemId = "Iron_Sword";

        var b = new RPGMobsConfig.ExtraDropRule();
        b.itemId = "Gold_Sword";

        assertNotEquals(a, b);
    }

    @Test
    void extraDropRuleNotEqualsDifferentEnabledPerTier() {
        var a = new RPGMobsConfig.ExtraDropRule();
        a.enabledPerTier = new boolean[]{true, true, true, true, true};

        var b = new RPGMobsConfig.ExtraDropRule();
        b.enabledPerTier = new boolean[]{true, true, true, true, false};

        assertNotEquals(a, b);
    }

    @Test
    void extraDropRuleHashCodeConsistent() {
        var a = new RPGMobsConfig.ExtraDropRule();
        a.itemId = "Potion";
        a.chance = 0.75;
        a.minQty = 2;
        a.maxQty = 5;

        var b = new RPGMobsConfig.ExtraDropRule();
        b.itemId = "Potion";
        b.chance = 0.75;
        b.minQty = 2;
        b.maxQty = 5;

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void mobRuleEqualsIdentical() {
        var a = new RPGMobsConfig.MobRule();
        a.enabled = true;
        a.matchExact = List.of("Goblin_Duke");
        a.weaponOverrideMode = RPGMobsConfig.WeaponOverrideMode.ALWAYS;

        var b = new RPGMobsConfig.MobRule();
        b.enabled = true;
        b.matchExact = List.of("Goblin_Duke");
        b.weaponOverrideMode = RPGMobsConfig.WeaponOverrideMode.ALWAYS;

        assertEquals(a, b);
    }

    @Test
    void mobRuleNotEqualsDifferentEnabled() {
        var a = new RPGMobsConfig.MobRule();
        a.enabled = true;

        var b = new RPGMobsConfig.MobRule();
        b.enabled = false;

        assertNotEquals(a, b);
    }

    @Test
    void mobRuleNotEqualsDifferentMatchExact() {
        var a = new RPGMobsConfig.MobRule();
        a.matchExact = List.of("Goblin");

        var b = new RPGMobsConfig.MobRule();
        b.matchExact = List.of("Skeleton");

        assertNotEquals(a, b);
    }

    @Test
    void mobRuleCategoryEqualsIdentical() {
        var a = new RPGMobsConfig.MobRuleCategory("Undead", List.of("Skeleton", "Zombie"));
        var b = new RPGMobsConfig.MobRuleCategory("Undead", List.of("Skeleton", "Zombie"));
        assertEquals(a, b);
    }

    @Test
    void mobRuleCategoryNotEqualsDifferentName() {
        var a = new RPGMobsConfig.MobRuleCategory("Undead", List.of("Skeleton"));
        var b = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Skeleton"));
        assertNotEquals(a, b);
    }

    @Test
    void lootTemplateEqualsIdentical() {
        var drop = new RPGMobsConfig.ExtraDropRule();
        drop.itemId = "Gold_Coin";
        drop.chance = 0.5;

        var a = new RPGMobsConfig.LootTemplate("Standard", List.of(drop), List.of("Goblin"));
        var b = new RPGMobsConfig.LootTemplate("Standard", List.of(drop), List.of("Goblin"));
        assertEquals(a, b);
    }

    @Test
    void lootTemplateNotEqualsDifferentDrops() {
        var dropA = new RPGMobsConfig.ExtraDropRule();
        dropA.itemId = "Gold_Coin";
        var dropB = new RPGMobsConfig.ExtraDropRule();
        dropB.itemId = "Silver_Coin";

        var a = new RPGMobsConfig.LootTemplate("Standard", List.of(dropA), List.of());
        var b = new RPGMobsConfig.LootTemplate("Standard", List.of(dropB), List.of());
        assertNotEquals(a, b);
    }

    @Test
    void lootTemplateCategoryEqualsIdentical() {
        var a = new RPGMobsConfig.LootTemplateCategory("Bosses", List.of("Dragon", "Lich"));
        var b = new RPGMobsConfig.LootTemplateCategory("Bosses", List.of("Dragon", "Lich"));
        assertEquals(a, b);
    }
}
