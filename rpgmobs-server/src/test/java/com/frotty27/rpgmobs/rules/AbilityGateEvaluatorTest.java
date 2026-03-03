package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbilityGateEvaluatorTest {

    private static final String ABILITY_ID = "test_ability";

    @Test
    void allowsWhenMobKeyIsLinked() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout", "Goblin_Duke");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout", "Goblin_Duke");

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void deniesWhenMobKeyNotLinked() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Eye_Void", resolved));
    }

    @Test
    void deniesWhenLinkedKeysEmpty() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig();
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true});

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void deniesWhenDisabledGlobally() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        cfg.isEnabled = false;
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void deniesWhenDisabledInResolved() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(false, new boolean[]{true, true, true, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void deniesWhenTierDisabledForEntry() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, false, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void allowsWhenTierEnabledForEntry() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{false, false, false, true, true}, "Goblin_Scout");

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 3, "Goblin_Scout", resolved));
        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
    }

    @Test
    void perEntryTiersAreIndependent() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout", "Zombie_Basic");
        ResolvedConfig resolved = new ResolvedConfig();
        resolved.resolvedAbilities = new LinkedHashMap<>();
        ResolvedConfig.ResolvedAbilityConfig rac = new ResolvedConfig.ResolvedAbilityConfig();
        rac.enabled = true;
        rac.linkedMobEntries = new LinkedHashMap<>();
        rac.linkedMobEntries.put("Goblin_Scout", new boolean[]{true, true, true, true, true});
        rac.linkedMobEntries.put("Zombie_Basic", new boolean[]{false, false, false, true, true});
        resolved.resolvedAbilities.put(ABILITY_ID, rac);

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 0, "Goblin_Scout", resolved));
        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 0, "Zombie_Basic", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 3, "Zombie_Basic", resolved));
    }

    @Test
    void weaponGateDeniesWhenWeaponNotInAllowedCategories() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        cfg.gate.allowedWeaponCategories = List.of("Swords", "Axes");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");
        var swords = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron", "Weapon_Sword_Steel"));
        var axes = new RPGMobsConfig.GearCategory("Axes", List.of("Weapon_Axe_Iron"));
        var bows = new RPGMobsConfig.GearCategory("Bows", List.of("Weapon_Shortbow_Iron"));
        resolved.weaponCategoryTree = new RPGMobsConfig.GearCategory("All", List.of(), swords, axes, bows);

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "Weapon_Shortbow_Iron", 2, "Goblin_Scout", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "Weapon_Sword_Iron", 2, "Goblin_Scout", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "Weapon_Axe_Iron", 2, "Goblin_Scout", resolved));
    }

    @Test
    void weaponGateAllowsAllWhenNoCategoriesSpecified() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        cfg.gate.allowedWeaponCategories = List.of();
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");
        var swords = new RPGMobsConfig.GearCategory("Swords", List.of("Weapon_Sword_Iron"));
        resolved.weaponCategoryTree = new RPGMobsConfig.GearCategory("All", List.of(), swords);

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "Weapon_Shortbow_Iron", 1, "Goblin_Scout", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "Weapon_Sword_Iron", 1, "Goblin_Scout", resolved));
    }

    @Test
    void deniesWhenMatchedRuleKeyIsNull() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, null, resolved));
    }

    @Test
    void deniesWhenMatchedRuleKeyIsBlank() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "Goblin_Scout");

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "", resolved));
    }

    @Test
    void allowsWhenMobKeyIsInLinkedCategory() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("category:Goblins");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "category:Goblins");
        resolved.mobRuleCategoryTree = buildTestTree();

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Scout", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Goblin_Duke", resolved));
    }

    @Test
    void deniesWhenMobKeyNotInLinkedCategory() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("category:Goblins");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, true, true, true}, "category:Goblins");
        resolved.mobRuleCategoryTree = buildTestTree();

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Skeleton_Warrior", resolved));
    }

    @Test
    void individualEntryTakesPriorityOverCategory() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("Goblin_Scout", "category:Goblins");
        ResolvedConfig resolved = new ResolvedConfig();
        resolved.resolvedAbilities = new LinkedHashMap<>();
        resolved.mobRuleCategoryTree = buildTestTree();
        ResolvedConfig.ResolvedAbilityConfig rac = new ResolvedConfig.ResolvedAbilityConfig();
        rac.enabled = true;
        rac.linkedMobEntries = new LinkedHashMap<>();
        rac.linkedMobEntries.put("Goblin_Scout", new boolean[]{false, false, false, true, true});
        rac.linkedMobEntries.put("category:Goblins", new boolean[]{true, true, true, true, true});
        resolved.resolvedAbilities.put(ABILITY_ID, rac);

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 0, "Goblin_Scout", resolved));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 3, "Goblin_Scout", resolved));
    }

    @Test
    void categoryEntryTierTogglesWork() {
        RPGMobsConfig.AbilityConfig cfg = createAbilityConfig("category:Undead");
        ResolvedConfig resolved = createResolvedWithAbility(true, new boolean[]{true, true, false, false, false}, "category:Undead");
        resolved.mobRuleCategoryTree = buildTestTree();

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 1, "Skeleton_Warrior", resolved));
        assertFalse(AbilityGateEvaluator.isAllowed(cfg, ABILITY_ID, "weapon_sword", 2, "Skeleton_Warrior", resolved));
    }

    private static RPGMobsConfig.MobRuleCategory buildTestTree() {
        var goblins = new RPGMobsConfig.MobRuleCategory("Goblins", List.of("Goblin_Scout", "Goblin_Duke"));
        var undead = new RPGMobsConfig.MobRuleCategory("Undead", List.of("Skeleton_Warrior", "Zombie_Shambler"));
        var root = new RPGMobsConfig.MobRuleCategory("All", List.of());
        root.children.add(goblins);
        root.children.add(undead);
        return root;
    }

    private static RPGMobsConfig.AbilityConfig createAbilityConfig(String... linkedKeys) {
        RPGMobsConfig.AbilityConfig cfg = new RPGMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, true, true, true, true};
        cfg.gate = new RPGMobsConfig.AbilityGate();
        cfg.linkedMobRuleKeys = new ArrayList<>(List.of(linkedKeys));
        return cfg;
    }

    private static ResolvedConfig createResolvedWithAbility(boolean enabled, boolean[] enabledPerTier, String... linkedKeys) {
        ResolvedConfig resolved = new ResolvedConfig();
        resolved.resolvedAbilities = new LinkedHashMap<>();
        ResolvedConfig.ResolvedAbilityConfig abilityConfig = new ResolvedConfig.ResolvedAbilityConfig();
        abilityConfig.enabled = enabled;
        abilityConfig.linkedMobEntries = new LinkedHashMap<>();
        for (String key : linkedKeys) {
            abilityConfig.linkedMobEntries.put(key, Arrays.copyOf(enabledPerTier, enabledPerTier.length));
        }
        resolved.resolvedAbilities.put(ABILITY_ID, abilityConfig);
        return resolved;
    }
}
