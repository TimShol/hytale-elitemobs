package com.frotty27.rpgmobs.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NameplateHelpersTest {

    @Test
    public void prettifyStringConvertsUnderscores() {
        assertEquals("Goblin Scout", NameplateHelpers.prettifyString("goblin_scout"));
    }

    @Test
    public void prettifyStringCapitalizesFirst() {
        assertEquals("Zombie", NameplateHelpers.prettifyString("zombie"));
    }

    @Test
    public void prettifyStringHandlesEmpty() {
        assertEquals("NPC", NameplateHelpers.prettifyString(""));
    }

    @Test
    public void prettifyStringHandlesNull() {
        assertEquals("NPC", NameplateHelpers.prettifyString(null));
    }

    @Test
    public void isNoiseSegmentDetectsNoise() {
        assertTrue(NameplateHelpers.isNoiseSegment("wander"));
        assertTrue(NameplateHelpers.isNoiseSegment("patrol"));
        assertTrue(NameplateHelpers.isNoiseSegment("big"));
        assertTrue(NameplateHelpers.isNoiseSegment("small"));
        assertTrue(NameplateHelpers.isNoiseSegment("Patrol"));
    }

    @Test
    public void isNoiseSegmentRejectsNormal() {
        assertFalse(NameplateHelpers.isNoiseSegment("warrior"));
        assertFalse(NameplateHelpers.isNoiseSegment("archer"));
        assertFalse(NameplateHelpers.isNoiseSegment("zombie"));
    }

    @Test
    public void isVariantSegmentDetectsVariants() {
        assertTrue(NameplateHelpers.isVariantSegment("burnt"));
        assertTrue(NameplateHelpers.isVariantSegment("frost"));
        assertTrue(NameplateHelpers.isVariantSegment("sand"));
        assertTrue(NameplateHelpers.isVariantSegment("Burnt"));
    }

    @Test
    public void isVariantSegmentRejectsNormal() {
        assertFalse(NameplateHelpers.isVariantSegment("duke"));
        assertFalse(NameplateHelpers.isVariantSegment("scout"));
        assertFalse(NameplateHelpers.isVariantSegment(null));
    }

    @Test
    public void classifyFamilyFindsLongestMatch() {
        Map<String, List<String>> families = new LinkedHashMap<>();
        families.put("zombie", List.of("T1", "T2", "T3", "T4", "T5"));
        families.put("zombie_burnt", List.of("T1", "T2", "T3", "T4", "T5"));
        families.put("default", List.of("T1", "T2", "T3", "T4", "T5"));

        assertEquals("zombie_burnt", NameplateHelpers.classifyFamily("Zombie_Burnt_Patrol", families));
    }

    @Test
    public void classifyFamilyFallsBackToDefault() {
        Map<String, List<String>> families = new LinkedHashMap<>();
        families.put("zombie", List.of("T1", "T2", "T3", "T4", "T5"));
        families.put("default", List.of("T1", "T2", "T3", "T4", "T5"));

        assertEquals("default", NameplateHelpers.classifyFamily("Unknown_Role", families));
    }

    @Test
    public void classifyFamilyHandlesNullRole() {
        Map<String, List<String>> families = new LinkedHashMap<>();
        families.put("zombie", List.of("T1"));

        assertEquals("default", NameplateHelpers.classifyFamily(null, families));
    }

    @Test
    public void resolveRoleWithoutFamilyStripsFamily() {
        assertEquals("Scout", NameplateHelpers.resolveRoleWithoutFamily("Zombie_Scout"));
    }

    @Test
    public void resolveRoleWithoutFamilyHandlesSimple() {
        assertEquals("Skeleton", NameplateHelpers.resolveRoleWithoutFamily("Skeleton"));
    }

    @Test
    public void resolveRoleWithoutFamilyHandlesNull() {
        assertEquals("NPC", NameplateHelpers.resolveRoleWithoutFamily(null));
    }

    @Test
    public void resolveRoleWithoutFamilyStripsVariantOnly() {
        assertEquals("Zombie", NameplateHelpers.resolveRoleWithoutFamily("Zombie_Burnt"));
    }

    @Test
    public void resolveDisplayRoleNameStripsNoiseAndFamily() {
        assertEquals("Scout", NameplateHelpers.resolveDisplayRoleName("Zombie_Scout_Patrol"));
    }

    @Test
    public void resolveDisplayRoleNameStripsVariantAndNoise() {
        assertEquals("Duke", NameplateHelpers.resolveDisplayRoleName("Zombie_Burnt_Duke_Wander"));
    }

    @Test
    public void resolveDisplayRoleNameHandlesVariantOnly() {
        assertEquals("Zombie", NameplateHelpers.resolveDisplayRoleName("Zombie_Burnt"));
    }

    @Test
    public void resolveDisplayRoleNameHandlesNull() {
        assertEquals("NPC", NameplateHelpers.resolveDisplayRoleName(null));
    }

    @Test
    public void resolveDisplayRoleNameHandlesSimple() {
        assertEquals("Skeleton", NameplateHelpers.resolveDisplayRoleName("Skeleton"));
    }

    @Test
    public void resolveDisplayRoleNameMultiSegmentRole() {
        assertEquals("Dark Knight", NameplateHelpers.resolveDisplayRoleName("Goblin_Dark_Knight"));
    }
}
