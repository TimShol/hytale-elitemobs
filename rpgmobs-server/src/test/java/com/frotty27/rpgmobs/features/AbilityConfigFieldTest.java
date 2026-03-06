package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbilityConfigFieldTest {

    @Test
    void perTierFloatDetectsChange() {
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        edit.chancePerTier = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        saved.chancePerTier = new float[]{1f, 1f, 1f, 1f, 1f};
        var field = new AbilityConfigField.PerTierFloat("chance",
                c -> c.chancePerTier, (c, v) -> c.chancePerTier = v);
        assertTrue(field.hasChanged(edit, saved));

        saved.chancePerTier = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void perTierFloatDeepCopiesIndependently() {
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        src.chancePerTier = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        var field = new AbilityConfigField.PerTierFloat("chance",
                c -> c.chancePerTier, (c, v) -> c.chancePerTier = v);
        field.deepCopy(src, dst);
        assertArrayEquals(src.chancePerTier, dst.chancePerTier);

        src.chancePerTier[0] = 9.9f;
        assertNotEquals(src.chancePerTier[0], dst.chancePerTier[0]);
    }

    @Test
    void perTierIntDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, int[]> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, new int[]{1, 2, 3, 4, 5});
        holder.put(saved, new int[]{5, 4, 3, 2, 1});
        var field = new AbilityConfigField.PerTierInt("damage",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, new int[]{1, 2, 3, 4, 5});
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void perTierIntDeepCopiesIndependently() {
        Map<RPGMobsConfig.AbilityConfig, int[]> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, new int[]{10, 20, 30, 40, 50});
        holder.put(dst, new int[]{0, 0, 0, 0, 0});
        var field = new AbilityConfigField.PerTierInt("damage",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertArrayEquals(holder.get(src), holder.get(dst));

        holder.get(src)[0] = 999;
        assertNotEquals(holder.get(src)[0], holder.get(dst)[0]);
    }

    @Test
    void scalarFloatDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, Float> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, 1.5f);
        holder.put(saved, 2.5f);
        var field = new AbilityConfigField.ScalarFloat("range",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, 1.5f);
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void scalarFloatDeepCopies() {
        Map<RPGMobsConfig.AbilityConfig, Float> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, 3.14f);
        holder.put(dst, 0f);
        var field = new AbilityConfigField.ScalarFloat("range",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertEquals(3.14f, holder.get(dst));
    }

    @Test
    void scalarIntDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, Integer> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, 10);
        holder.put(saved, 20);
        var field = new AbilityConfigField.ScalarInt("count",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, 10);
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void scalarIntDeepCopies() {
        Map<RPGMobsConfig.AbilityConfig, Integer> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, 42);
        holder.put(dst, 0);
        var field = new AbilityConfigField.ScalarInt("count",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertEquals(42, holder.get(dst));
    }

    @Test
    void scalarDoubleDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, Double> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, 1.0);
        holder.put(saved, 2.0);
        var field = new AbilityConfigField.ScalarDouble("multiplier",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, 1.0);
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void scalarDoubleDeepCopies() {
        Map<RPGMobsConfig.AbilityConfig, Double> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, 99.9);
        holder.put(dst, 0.0);
        var field = new AbilityConfigField.ScalarDouble("multiplier",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertEquals(99.9, holder.get(dst));
    }

    @Test
    void scalarBooleanDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, Boolean> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, true);
        holder.put(saved, false);
        var field = new AbilityConfigField.ScalarBoolean("faceTarget",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, true);
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void scalarBooleanDeepCopies() {
        Map<RPGMobsConfig.AbilityConfig, Boolean> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, true);
        holder.put(dst, false);
        var field = new AbilityConfigField.ScalarBoolean("faceTarget",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertEquals(true, holder.get(dst));
    }

    @Test
    void scalarStringDetectsChange() {
        Map<RPGMobsConfig.AbilityConfig, String> holder = new IdentityHashMap<>();
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        holder.put(edit, "alpha");
        holder.put(saved, "beta");
        var field = new AbilityConfigField.ScalarString("name",
                holder::get, holder::put);
        assertTrue(field.hasChanged(edit, saved));

        holder.put(saved, "alpha");
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void scalarStringDeepCopies() {
        Map<RPGMobsConfig.AbilityConfig, String> holder = new IdentityHashMap<>();
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        holder.put(src, "hello");
        holder.put(dst, "");
        var field = new AbilityConfigField.ScalarString("name",
                holder::get, holder::put);
        field.deepCopy(src, dst);
        assertEquals("hello", holder.get(dst));
    }

    @Test
    void stringListDetectsChange() {
        var edit = new RPGMobsConfig.AbilityConfig();
        var saved = new RPGMobsConfig.AbilityConfig();
        edit.linkedMobRuleKeys = new ArrayList<>(List.of("Goblin", "Trork"));
        saved.linkedMobRuleKeys = new ArrayList<>(List.of("Goblin"));
        var field = new AbilityConfigField.StringList("linked",
                c -> c.linkedMobRuleKeys, (c, v) -> c.linkedMobRuleKeys = v);
        assertTrue(field.hasChanged(edit, saved));

        saved.linkedMobRuleKeys = new ArrayList<>(List.of("Goblin", "Trork"));
        assertFalse(field.hasChanged(edit, saved));
    }

    @Test
    void stringListDeepCopiesIndependently() {
        var src = new RPGMobsConfig.AbilityConfig();
        var dst = new RPGMobsConfig.AbilityConfig();
        src.linkedMobRuleKeys = new ArrayList<>(List.of("Skeleton", "Zombie"));
        var field = new AbilityConfigField.StringList("linked",
                c -> c.linkedMobRuleKeys, (c, v) -> c.linkedMobRuleKeys = v);
        field.deepCopy(src, dst);
        assertEquals(src.linkedMobRuleKeys, dst.linkedMobRuleKeys);

        src.linkedMobRuleKeys.add("Wraith");
        assertNotEquals(src.linkedMobRuleKeys.size(), dst.linkedMobRuleKeys.size());
    }
}
