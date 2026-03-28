package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigWriterTest {

    @Test
    void overlayToMapOmitsNullFields() {
        var overlay = new ConfigOverlay();
        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.isEmpty());
    }

    @Test
    void overlayToMapIncludesNonNullBoolean() {
        var overlay = new ConfigOverlay();
        overlay.enabled = true;

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("enabled"));
        assertEquals(true, map.get("enabled"));
    }

    @Test
    void overlayToMapIncludesNonNullFloat() {
        var overlay = new ConfigOverlay();
        overlay.healthRandomVariance = 0.2f;

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("healthRandomVariance"));
        assertInstanceOf(Double.class, map.get("healthRandomVariance"));
        assertEquals(0.2, (Double) map.get("healthRandomVariance"), 0.001);
    }

    @Test
    void overlayToMapSerializesFloatArrayAsDoubleList() {
        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{1.5f, 2.0f, 2.5f, 3.0f, 3.5f};

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("healthMultiplierPerTier"));
        @SuppressWarnings("unchecked")
        List<Double> list = (List<Double>) map.get("healthMultiplierPerTier");
        assertEquals(5, list.size());
        assertEquals(1.5, list.get(0), 0.001);
        assertEquals(2.0, list.get(1), 0.001);
        assertEquals(2.5, list.get(2), 0.001);
        assertEquals(3.0, list.get(3), 0.001);
        assertEquals(3.5, list.get(4), 0.001);
        assertInstanceOf(Double.class, list.get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlayToMapSerializesTierOverrides() {
        var overlay = new ConfigOverlay();
        overlay.tierOverrides = new LinkedHashMap<>();
        var to = new ConfigOverlay.TierOverride();
        to.allowedTiers = new boolean[]{true, false, true, false, true};
        overlay.tierOverrides.put("zombie", to);

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("tierOverrides"));
        Map<String, Object> toMap = (Map<String, Object>) map.get("tierOverrides");
        assertTrue(toMap.containsKey("zombie"));
        Map<String, Object> zombieMap = (Map<String, Object>) toMap.get("zombie");
        List<Boolean> allowedTiers = (List<Boolean>) zombieMap.get("allowedTiers");
        assertEquals(5, allowedTiers.size());
        assertTrue(allowedTiers.get(0));
        assertFalse(allowedTiers.get(1));
        assertTrue(allowedTiers.get(2));
        assertFalse(allowedTiers.get(3));
        assertTrue(allowedTiers.get(4));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlayToMapSerializesDisabledMobRuleKeys() {
        var overlay = new ConfigOverlay();
        overlay.disabledMobRuleKeys = new LinkedHashSet<>(List.of("zombie_scout", "skeleton_warrior"));

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("disabledMobRuleKeys"));
        List<String> keys = (List<String>) map.get("disabledMobRuleKeys");
        assertEquals(2, keys.size());
        assertTrue(keys.contains("zombie_scout"));
        assertTrue(keys.contains("skeleton_warrior"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlayToMapSerializesLootTemplates() {
        var overlay = new ConfigOverlay();
        overlay.lootTemplates = new LinkedHashMap<>();
        var tpl = new RPGMobsConfig.LootTemplate();
        tpl.name = "basic";
        tpl.linkedMobRuleKeys = new ArrayList<>(List.of("zombie"));
        var drop = new RPGMobsConfig.ExtraDropRule();
        drop.itemId = "gold_coin";
        drop.chance = 0.5;
        drop.enabledPerTier = new boolean[]{true, true, false, false, false};
        drop.minQty = 1;
        drop.maxQty = 3;
        tpl.drops = new ArrayList<>(List.of(drop));
        overlay.lootTemplates.put("basic", tpl);

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("lootTemplates"));
        Map<String, Object> tplsMap = (Map<String, Object>) map.get("lootTemplates");
        assertTrue(tplsMap.containsKey("basic"));
        Map<String, Object> basicMap = (Map<String, Object>) tplsMap.get("basic");
        assertEquals("basic", basicMap.get("name"));
        List<String> linkedKeys = (List<String>) basicMap.get("linkedMobRuleKeys");
        assertEquals("zombie", linkedKeys.get(0));
        List<Map<String, Object>> drops = (List<Map<String, Object>>) basicMap.get("drops");
        assertEquals(1, drops.size());
        assertEquals("gold_coin", drops.get(0).get("itemId"));
        assertEquals(0.5, drops.get(0).get("chance"));
        assertEquals(1, drops.get(0).get("minQty"));
        assertEquals(3, drops.get(0).get("maxQty"));
        List<Boolean> tierEnabled = (List<Boolean>) drops.get(0).get("enabledPerTier");
        assertTrue(tierEnabled.get(0));
        assertTrue(tierEnabled.get(1));
        assertFalse(tierEnabled.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlayToMapSerializesAbilityOverlays() {
        var overlay = new ConfigOverlay();
        overlay.abilityOverlays = new LinkedHashMap<>();
        var ao = new ConfigOverlay.AbilityOverlay();
        ao.enabled = true;
        var entry = new ConfigOverlay.AbilityLinkedEntry("zombie", new boolean[]{true, false, true, false, true});
        ao.linkedEntries = new ArrayList<>(List.of(entry));
        overlay.abilityOverlays.put("charge_leap", ao);

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("abilityOverlays"));
        Map<String, Object> aoMap = (Map<String, Object>) map.get("abilityOverlays");
        assertTrue(aoMap.containsKey("charge_leap"));
        Map<String, Object> leapMap = (Map<String, Object>) aoMap.get("charge_leap");
        assertEquals(true, leapMap.get("enabled"));
        List<Map<String, Object>> entries = (List<Map<String, Object>>) leapMap.get("linkedEntries");
        assertEquals(1, entries.size());
        assertEquals("zombie", entries.get(0).get("key"));
        List<Boolean> tiers = (List<Boolean>) entries.get(0).get("enabledPerTier");
        assertEquals(5, tiers.size());
        assertTrue(tiers.get(0));
        assertFalse(tiers.get(1));
    }

    @Test
    void overlayToMapSerializesProgressionStyle() {
        var overlay = new ConfigOverlay();
        overlay.progressionStyle = "DISTANCE_FROM_SPAWN";

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("progressionStyle"));
        assertEquals("DISTANCE_FROM_SPAWN", map.get("progressionStyle"));
    }

    @Test
    void writeOverlayCreatesFile(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.enabled = true;
        overlay.healthRandomVariance = 0.3f;

        Path filePath = tempDir.resolve("worlds").resolve("test.yml");

        ConfigWriter.writeOverlay(overlay, filePath);

        assertTrue(Files.exists(filePath));
        String content = Files.readString(filePath);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("enabled"));
        assertTrue(content.contains("healthRandomVariance"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overlayToMapSerializesEnvironmentTierRules() {
        var overlay = new ConfigOverlay();
        overlay.environmentTierRules = new LinkedHashMap<>();
        overlay.environmentTierRules.put("zone0", new double[]{100, 0, 0, 0, 0});
        overlay.environmentTierRules.put("zone1", new double[]{60, 25, 15, 0, 0});

        Map<String, Object> map = ConfigWriter.overlayToMap(overlay);

        assertTrue(map.containsKey("environmentTierRules"));
        Map<String, List<Double>> envMap = (Map<String, List<Double>>) map.get("environmentTierRules");
        assertEquals(2, envMap.size());
        assertTrue(envMap.containsKey("zone0"));
        assertTrue(envMap.containsKey("zone1"));
        List<Double> zone0 = envMap.get("zone0");
        assertEquals(5, zone0.size());
        assertEquals(100.0, zone0.get(0));
        assertEquals(0.0, zone0.get(1));
        List<Double> zone1 = envMap.get("zone1");
        assertEquals(60.0, zone1.get(0));
        assertEquals(25.0, zone1.get(1));
        assertEquals(15.0, zone1.get(2));
    }
}
