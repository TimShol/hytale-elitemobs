package com.frotty27.rpgmobs.systems.death;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RPGMobsDeathSystemTest {

    @Test
    public void isLinkedToMobDirectKeyMatch() {
        var linkedKeys = List.of("zombie", "skeleton");
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";

        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
    }

    @Test
    public void isLinkedToMobDirectKeyNoMatch() {
        var linkedKeys = List.of("zombie", "skeleton");
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";

        assertFalse(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "spider", tree));
    }

    @Test
    public void isLinkedToMobCategoryMatch() {
        var undeadCategory = new RPGMobsConfig.MobRuleCategory();
        undeadCategory.name = "Undead";
        undeadCategory.mobRuleKeys = new ArrayList<>(List.of("zombie", "skeleton"));

        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";
        tree.children = new ArrayList<>(List.of(undeadCategory));

        var linkedKeys = List.of("category:Undead");

        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "skeleton", tree));
    }

    @Test
    public void isLinkedToMobCategoryNoMatch() {
        var undeadCategory = new RPGMobsConfig.MobRuleCategory();
        undeadCategory.name = "Undead";
        undeadCategory.mobRuleKeys = new ArrayList<>(List.of("zombie", "skeleton"));

        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";
        tree.children = new ArrayList<>(List.of(undeadCategory));

        var linkedKeys = List.of("category:Undead");

        assertFalse(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "spider", tree));
    }

    @Test
    public void isLinkedToMobEmptyLinkedKeys() {
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";

        assertFalse(RPGMobsDeathSystem.isLinkedToMob(List.of(), "zombie", tree));
    }

    @Test
    public void isLinkedToMobMixedKeysAndCategories() {
        var beasts = new RPGMobsConfig.MobRuleCategory();
        beasts.name = "Beasts";
        beasts.mobRuleKeys = new ArrayList<>(List.of("wolf", "bear"));

        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";
        tree.children = new ArrayList<>(List.of(beasts));

        var linkedKeys = List.of("zombie", "category:Beasts");

        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "wolf", tree));
        assertFalse(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "spider", tree));
    }

    @Test
    public void isLinkedToMobNestedCategory() {
        var innerCategory = new RPGMobsConfig.MobRuleCategory();
        innerCategory.name = "Goblins";
        innerCategory.mobRuleKeys = new ArrayList<>(List.of("goblin_scout", "goblin_warrior"));

        var outerCategory = new RPGMobsConfig.MobRuleCategory();
        outerCategory.name = "Hostiles";
        outerCategory.children = new ArrayList<>(List.of(innerCategory));
        outerCategory.mobRuleKeys = new ArrayList<>(List.of("spider"));

        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";
        tree.children = new ArrayList<>(List.of(outerCategory));

        var linkedKeys = List.of("category:Hostiles");

        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "spider", tree));
        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "goblin_scout", tree));
    }

    @Test
    public void isLinkedToMobUnknownCategoryInEmptyTree() {
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";

        var linkedKeys = List.of("category:Undead");

        assertFalse(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
    }

    @Test
    public void isLinkedToMobDirectKeyTakesPriority() {
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";

        var linkedKeys = List.of("zombie", "category:NonExistent");

        assertTrue(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
    }

    @Test
    public void isLinkedToMobCategoryNameNotFoundReturnsFalse() {
        var tree = new RPGMobsConfig.MobRuleCategory();
        tree.name = "All";
        tree.mobRuleKeys = new ArrayList<>(List.of("zombie"));

        var linkedKeys = List.of("category:NonExistentCategory");

        assertFalse(RPGMobsDeathSystem.isLinkedToMob(linkedKeys, "zombie", tree));
    }
}
