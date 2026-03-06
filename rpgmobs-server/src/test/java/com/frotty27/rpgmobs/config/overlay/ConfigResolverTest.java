package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigResolverTest {

    @Test
    void resolveInstanceTemplateExtractsTemplateName() {
        String world = "instance-Dungeon_Goblin-12345678-1234-1234-1234-123456789012";
        assertEquals("Dungeon_Goblin", ConfigResolver.resolveInstanceTemplate(world));
    }

    @Test
    void resolveInstanceTemplateReturnsNullForNonInstance() {
        assertNull(ConfigResolver.resolveInstanceTemplate("default"));
    }

    @Test
    void resolveInstanceTemplateReturnsNullForNull() {
        assertNull(ConfigResolver.resolveInstanceTemplate(null));
    }

    @Test
    void resolveInstanceTemplateReturnsNullForShortString() {
        assertNull(ConfigResolver.resolveInstanceTemplate("instance-X"));
    }

    @Test
    void getDoubleOrNullParsesNumber() {
        Map<String, Object> map = Map.of("key", 1.5);
        assertEquals(1.5, ConfigResolver.getDoubleOrNull(map, "key"));
    }

    @Test
    void getDoubleOrNullReturnsNullForMissing() {
        Map<String, Object> map = Map.of();
        assertNull(ConfigResolver.getDoubleOrNull(map, "key"));
    }

    @Test
    void getDoubleOrNullReturnsNullForNonNumber() {
        Map<String, Object> map = Map.of("key", "text");
        assertNull(ConfigResolver.getDoubleOrNull(map, "key"));
    }

    @Test
    void getFloatOrNullParsesNumber() {
        Map<String, Object> map = Map.of("key", 1.5);
        assertEquals(1.5f, ConfigResolver.getFloatOrNull(map, "key"));
    }

    @Test
    void getBooleanOrNullParsesBoolean() {
        Map<String, Object> map = Map.of("key", true);
        assertEquals(true, ConfigResolver.getBooleanOrNull(map, "key"));
    }

    @Test
    void getBooleanOrNullParsesBooleanString() {
        Map<String, Object> map = Map.of("key", "true");
        assertEquals(true, ConfigResolver.getBooleanOrNull(map, "key"));
    }

    @Test
    void getStringListReturnsEmptyForMissing() {
        Map<String, Object> map = Map.of();
        var result = ConfigResolver.getStringList(map, "key");
        assertTrue(result.isEmpty());
    }

    @Test
    void getStringListExtractsList() {
        Map<String, Object> map = Map.of("key", List.of("a", "b"));
        var result = ConfigResolver.getStringList(map, "key");
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void parseExtraDropRuleWithEnabledPerTier() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", "sword");
        map.put("chance", 0.5);
        map.put("enabledPerTier", List.of(true, false, true, false, true));
        map.put("minQty", 2);
        map.put("maxQty", 5);

        RPGMobsConfig.ExtraDropRule rule = ConfigResolver.parseExtraDropRule(map);

        assertNotNull(rule);
        assertEquals("sword", rule.itemId);
        assertEquals(0.5, rule.chance);
        assertArrayEquals(new boolean[]{true, false, true, false, true}, rule.enabledPerTier);
        assertEquals(2, rule.minQty);
        assertEquals(5, rule.maxQty);
    }

    @Test
    void parseExtraDropRuleLegacyTierRange() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", "sword");
        map.put("minTierInclusive", 1);
        map.put("maxTierInclusive", 3);

        RPGMobsConfig.ExtraDropRule rule = ConfigResolver.parseExtraDropRule(map);

        assertNotNull(rule);
        assertArrayEquals(new boolean[]{false, true, true, true, false}, rule.enabledPerTier);
    }

    @Test
    void parseExtraDropRuleDefaultsAllTiersEnabled() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", "sword");

        RPGMobsConfig.ExtraDropRule rule = ConfigResolver.parseExtraDropRule(map);

        assertNotNull(rule);
        assertArrayEquals(new boolean[]{true, true, true, true, true}, rule.enabledPerTier);
    }

    @Test
    void parseExtraDropRuleReturnsNullForMissingItemId() {
        Map<String, Object> map = new HashMap<>();
        map.put("chance", 0.5);

        assertNull(ConfigResolver.parseExtraDropRule(map));
    }

    @Test
    void parseMobRuleExtractsAllFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", true);
        map.put("matchExact", List.of("zombie"));
        map.put("matchStartsWith", List.of("Skeleton"));
        map.put("matchContains", List.of("warrior"));
        map.put("matchExcludes", List.of("baby"));
        map.put("enableWeaponOverrideForTier", List.of(true, false, true, false, true));
        map.put("weaponOverrideMode", "ONLY_IF_EMPTY");
        map.put("allowedWeaponCategories", List.of("category:Swords"));
        map.put("allowedArmorSlots", List.of("Head", "Chest"));

        RPGMobsConfig.MobRule rule = ConfigResolver.parseMobRule(map);

        assertTrue(rule.enabled);
        assertEquals(List.of("zombie"), rule.matchExact);
        assertEquals(List.of("Skeleton"), rule.matchStartsWith);
        assertEquals(List.of("warrior"), rule.matchContains);
        assertEquals(List.of("baby"), rule.matchExcludes);
        assertArrayEquals(new boolean[]{true, false, true, false, true}, rule.enableWeaponOverrideForTier);
        assertEquals(RPGMobsConfig.WeaponOverrideMode.ONLY_IF_EMPTY, rule.weaponOverrideMode);
        assertEquals(List.of("category:Swords"), rule.allowedWeaponCategories);
        assertEquals(List.of("Head", "Chest"), rule.allowedArmorSlots);
    }

    @Test
    void parseMobRuleCategoryRecursive() {
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("name", "Undead");
        childMap.put("mobRuleKeys", List.of("skeleton"));

        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("name", "Root");
        rootMap.put("mobRuleKeys", List.of("zombie"));
        rootMap.put("children", List.of(childMap));

        RPGMobsConfig.MobRuleCategory cat = ConfigResolver.parseMobRuleCategory(rootMap);

        assertEquals("Root", cat.name);
        assertEquals(List.of("zombie"), cat.mobRuleKeys);
        assertEquals(1, cat.children.size());
        assertEquals("Undead", cat.children.get(0).name);
        assertEquals(List.of("skeleton"), cat.children.get(0).mobRuleKeys);
    }

    @Test
    void getDoubleOrReturnsDefaultForMissing() {
        Map<String, Object> map = Map.of();
        assertEquals(5.0, ConfigResolver.getDoubleOr(map, "key", 5.0));
    }

    @Test
    void getIntOrReturnsDefaultForMissing() {
        Map<String, Object> map = Map.of();
        assertEquals(10, ConfigResolver.getIntOr(map, "key", 10));
    }

    @Test
    void parseMobRuleDefaultsWeaponOverrideModeToAlways() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", true);

        RPGMobsConfig.MobRule rule = ConfigResolver.parseMobRule(map);

        assertEquals(RPGMobsConfig.WeaponOverrideMode.ALWAYS, rule.weaponOverrideMode);
    }
}
