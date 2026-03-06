package com.frotty27.rpgmobs.config.templates;

import com.frotty27.rpgmobs.config.overlay.ConfigOverlay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTemplateTest {

    @Test
    void defaultPresetOnlyEnables() {
        var template = ConfigTemplate.get("full");
        assertNotNull(template);
        assertEquals("Default", template.getName());
        ConfigOverlay o = template.getOverlay();
        assertEquals(true, o.enabled);
        assertNull(o.healthMultiplierPerTier);
        assertNull(o.enableHealthScaling);
        assertNull(o.enableDamageScaling);
        assertNull(o.progressionStyle);
        assertNull(o.spawnChancePerTier);
    }

    @Test
    void emptyPresetDisablesEverything() {
        var template = ConfigTemplate.get("empty");
        assertNotNull(template);
        assertEquals("Empty", template.getName());
        ConfigOverlay o = template.getOverlay();
        assertEquals("NONE", o.progressionStyle);
        assertArrayEquals(new double[]{0, 0, 0, 0, 0}, o.spawnChancePerTier);
        assertEquals(false, o.enableHealthScaling);
        assertEquals(false, o.enableDamageScaling);
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f, 0f}, o.healthMultiplierPerTier);
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f, 0f}, o.damageMultiplierPerTier);
    }

    @Test
    void emptyPresetDisablesAllAbilities() {
        ConfigOverlay o = ConfigTemplate.get("empty").getOverlay();
        assertNotNull(o.abilityOverlays);
        assertEquals(3, o.abilityOverlays.size());
        for (String id : new String[]{"charge_leap", "heal_leap", "undead_summon"}) {
            assertTrue(o.abilityOverlays.containsKey(id));
            assertEquals(false, o.abilityOverlays.get(id).enabled);
        }
    }

    @Test
    void emptyPresetHasEmptyMobRulesAndLoot() {
        ConfigOverlay o = ConfigTemplate.get("empty").getOverlay();
        assertNotNull(o.mobRules);
        assertTrue(o.mobRules.isEmpty());
        assertNotNull(o.mobRuleCategoryTree);
        assertEquals("All", o.mobRuleCategoryTree.name);
        assertNotNull(o.lootTemplates);
        assertTrue(o.lootTemplates.isEmpty());
        assertNotNull(o.lootTemplateCategoryTree);
        assertEquals("All", o.lootTemplateCategoryTree.name);
    }

    @Test
    void getReturnsNullForUnknownPreset() {
        assertNull(ConfigTemplate.get("nonexistent"));
    }

    @Test
    void getAllContainsBothPresets() {
        var all = ConfigTemplate.getAll();
        assertEquals(2, all.size());
        assertTrue(all.containsKey("full"));
        assertTrue(all.containsKey("empty"));
    }
}
