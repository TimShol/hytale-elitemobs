package com.frotty27.rpgmobs.config;

import com.frotty27.rpgmobs.config.schema.Cfg;
import com.frotty27.rpgmobs.config.schema.Default;
import com.frotty27.rpgmobs.config.schema.FixedArraySize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InstancesConfig {

    @Default
    @Cfg(group = "Instances", file = "instances.yml",
         comment = """
                 Master switch for the per-instance override system.
                 When false: this entire file is ignored and all worlds and instances use global settings (spawning.yml, loot.yml, stats.yml, etc.).
                 When true: every world is resolved through the rules below, allowing per-instance customization.
                 \s
                 If you only use the persistent world and don't run any instances, you can safely set this to false.""")
    public boolean enabled = true;

    @Default
    @Cfg(group = "Instances", file = "instances.yml",
         comment = """
                 Per-instance-template overrides. Each key is matched against world names in two ways:
                   1. Exact match — the key is compared directly to the world name (e.g. 'Persistent' matches the persistent world).
                   2. Template match — instance worlds named 'instance-{Template}-{UUID}' are matched by the {Template} portion.
                      Example: a rule keyed 'Dungeon_Outlander' matches any 'instance-Dungeon_Outlander-{UUID}'.
                 \s
                 Any world or instance template NOT listed here is ignored by RPGMobs instance overrides
                 and will use global config files (spawning.yml, loot.yml, stats.yml, etc.) as normal.
                 \s
                 Every field inside an InstanceRule is nullable — set it to null (or omit it) to inherit from the global config.
                 Only set a field when you want that instance to differ from the global default.
                 \s
                 ==========================================
                  INSTANCE RULE FIELD REFERENCE
                 ==========================================
                 \s
                 ── General ──────────────────────────────
                 \s
                 enabled          (boolean)  Master switch for this instance.
                                             true  = RPGMobs elites spawn in this instance.
                                             false = RPGMobs is completely disabled here, no elites will spawn.
                 \s
                 ── Spawning ─────────────────────────────
                 \s
                 progressionStyle (string)   How elite tiers are picked. One of:
                                               "ENVIRONMENT"          - tier is based on the biome/environment zone
                                               "DISTANCE_FROM_SPAWN"  - tier scales with XZ distance from origin
                                               "NONE"                 - uses spawnChancePerTier weights directly
                                             Set to null to inherit the global spawning.yml style.
                 \s
                 spawnChancePerTier (double[5])  Weight per tier [T1, T2, T3, T4, T5].
                                                 Only used when progressionStyle is "NONE".
                                                 Example: [0, 0, 0, 0, 1.0] = 100% chance of T5 in this Instance.
                 \s
                 allowedTiersToSpawn (boolean[5])  Per-tier gate [T1, T2, T3, T4, T5].
                                                   false = that tier will NOT spawn for mobs resolved through global mobrules.
                                                   If NPCs are allowed to spawn in this Instance, transform them into the tiers that are allowed.
                                                   This does NOT affect mobs with a mobOverride that forces a specific tier —
                                                   a forced tier always bypasses this filter.
                                                   Requires a matching rule in mobrules.yml for the mob to be recognized as an RPG Mob.
                 \s
                 ── Distance From Spawn ─────────────────
                 These only apply when progressionStyle is "DISTANCE_FROM_SPAWN".
                 null = use the global spawning.yml values.
                 \s
                 distancePerTier              (double)  Blocks per tier transition.
                                                        Example: 500.0 = T1 at 0-499m, T2 at 500-999m, etc.
                 \s
                 distanceBonusInterval        (double)  Block interval for applying bonus stats.
                                                        Example: 100.0 = apply bonus every 100 blocks from spawn.
                 \s
                 distanceHealthBonusPerInterval (float)  HP multiplier bonus added per interval.
                                                        Example: 0.01 = +1% health per interval.
                 \s
                 distanceDamageBonusPerInterval (float)  Damage multiplier bonus added per interval.
                                                        Example: 0.005 = +0.5% damage per interval.
                 \s
                 distanceHealthBonusCap       (float)  Maximum health bonus multiplier from distance.
                                                       Example: 0.5 = +50% health max.
                 \s
                 distanceDamageBonusCap       (float)  Maximum damage bonus multiplier from distance.
                                                       Example: 0.5 = +50% damage max.
                 \s
                 ── Stats ────────────────────────────────
                 \s
                 healthMultiplierPerTier (float[5])  HP multiplier applied per tier [T1, T2, T3, T4, T5].
                                                     Example: [1, 2, 3, 4, 5] = T1 gets 1x HP, T5 gets 5x HP.
                                                     null = use the global stats.yml values.
                 \s
                 damageMultiplierPerTier (float[5])  Damage multiplier per tier [T1, T2, T3, T4, T5].
                                                     Example: [1, 2, 2.5, 3, 3.5]
                                                     null = use the global stats.yml values.
                 \s
                 ── Abilities ────────────────────────────
                 \s
                 abilitiesEnabled (boolean)  Master toggle for all abilities in this instance.
                                             false = no abilities fire, regardless of other settings.
                                             null  = use the global abilities.yml toggle.
                 \s
                 abilitiesEnabledPerTier (boolean[5])  Per-tier ability gate [T1, T2, T3, T4, T5].
                                                        false = abilities disabled for that tier.
                                                        Overridden by per-ability entries in abilityOverrides.
                 \s
                 abilityOverrides (map)  Per-ability per-tier toggle.
                                         Key   = ability id (e.g. "charge_leap", "heal_leap", "undead_summon")
                                         Value = boolean[5] per tier [T1, T2, T3, T4, T5]
                                         This takes priority over abilitiesEnabledPerTier for that specific ability.
                 \s
                 ── Loot ─────────────────────────────────
                 \s
                 vanillaDroplistExtraRollsPerTier (int[5])  Extra rolls on the mob's vanilla drop list per tier.
                                                             [0, 0, 0, 0, 0] = no extra vanilla drops.
                                                             null = use global loot.yml values.
                 \s
                 dropWeaponChance     (double 0.0-1.0)  Chance to drop the mob's equipped weapon on death.
                 dropArmorPieceChance (double 0.0-1.0)  Chance to drop each armor piece on death (rolled per slot).
                 dropOffhandItemChance(double 0.0-1.0)  Chance to drop the offhand/utility item on death.
                                                        null = use global loot.yml values.
                 \s
                 extraDrops (list)  Instance-wide extra drop rules. Each entry:
                                      itemId           = item identifier string
                                      chance           = drop probability 0.0-1.0
                                      minTierInclusive = minimum tier (inclusive) for this drop to apply
                                      maxTierInclusive = maximum tier (inclusive) for this drop to apply
                                      minQty / maxQty  = quantity range
                                    Overridden by per-mob drops in mobOverrides if present.
                 \s
                 ── Elite Behavior ───────────────────────
                 \s
                 eliteFriendlyFireDisabled (boolean)  When true, elites cannot damage other elites.
                                                       Also prevents elites from aggro-targeting other elites.
                 \s
                 eliteFallDamageDisabled   (boolean)  When true, elites take no fall/environment damage.
                 \s
                 ── Mob Overrides ────────────────────────
                 \s
                 mobOverrides (map)  Per-mob-role overrides. Each key is matched against the mob's role name
                                    using case-insensitive contains matching (longer key = higher priority).
                                    Requires the mob to also have a matching rule in the global mobrules.yml.
                 \s
                   forcedTier (int)  Force this mob to a specific tier index (0-4 for T1-T5).
                                    Set to -1 to not force any tier (use normal spawn logic).
                                    A forced tier BYPASSES allowedTiersToSpawn — even if only T5
                                    is allowed, forcing tier 2 (T3) will still work.
                 \s
                   drops (list)  Per-mob extra drops (same format as extraDrops above).
                                 When present, these REPLACE the instance-wide extraDrops for this mob.
                 \s
                 ==========================================
                  FULL EXAMPLE — Dungeon_Goblin
                 ==========================================
                 \s
                 This example shows how to add tiered RPG mobs to the default Goblin Dungeon.
                 All mobs default to T5 via spawnChancePerTier and allowedTiersToSpawn, but individual
                 goblin roles are overridden to specific tiers using mobOverrides — creating a difficulty
                 curve from weak Hermits (T1) up to the Duke boss (T5). Each role also gets custom loot.
                 \s
                   Dungeon_Goblin:
                     enabled: true
                     progressionStyle: "NONE"
                     spawnChancePerTier:
                       - 0.0
                       - 0.0
                       - 0.0
                       - 0.0
                       - 1.0
                     allowedTiersToSpawn:
                       - false
                       - false
                       - false
                       - false
                       - true
                     healthMultiplierPerTier:
                       - 1.0
                       - 2.0
                       - 3.0
                       - 4.0
                       - 5.0
                     damageMultiplierPerTier:
                       - 1.0
                       - 2.0
                       - 2.5
                       - 3.0
                       - 3.5
                     abilitiesEnabled: true
                     abilitiesEnabledPerTier:
                       - false
                       - false
                       - false
                       - true
                       - true
                     abilityOverrides:
                       charge_leap:
                         - false
                         - false
                         - false
                         - false
                         - true
                       heal_leap:
                         - false
                         - false
                         - false
                         - true
                         - true
                       undead_summon:
                         - false
                         - false
                         - false
                         - false
                         - true
                     vanillaDroplistExtraRollsPerTier:
                       - 0
                       - 0
                       - 0
                       - 0
                       - 0
                     dropWeaponChance: 0.0
                     dropArmorPieceChance: 0.0
                     dropOffhandItemChance: 0.0
                     eliteFriendlyFireDisabled: true
                     eliteFallDamageDisabled: true
                     mobOverrides:
                       Goblin_Hermit:
                         forcedTier: 0
                         drops:
                           -{ itemId: "Ore_Copper", chance: 0.3, minTierInclusive: 0, maxTierInclusive: 0, minQty: 1, maxQty: 3 }
                           -{ itemId: "Ingredient_Bar_Copper", chance: 0.15, minTierInclusive: 0, maxTierInclusive: 0, minQty: 1, maxQty: 2 }
                       Goblin_Scavenger:
                         forcedTier: 1
                         drops:
                           -{ itemId: "Potion_Health_Greater", chance: 0.1, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 1 }
                       Goblin_Scrapper:
                         forcedTier: 1
                         drops:
                           -{ itemId: "Potion_Health_Greater", chance: 0.1, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 1 }
                       Goblin_Miner:
                         forcedTier: 1
                         drops:
                           -{ itemId: "Potion_Health_Greater", chance: 0.1, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 1 }
                       Goblin_Lobber:
                         forcedTier: 2
                         drops:
                           -{ itemId: "Potion_Health_Greater", chance: 0.1, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 1 }
                       Goblin_Ogre:
                         forcedTier: 3
                         drops:
                           -{ itemId: "Ore_Silver", chance: 0.2, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 3 }
                           -{ itemId: "Ore_Gold", chance: 0.15, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 2 }
                           -{ itemId: "Potion_Health_Greater", chance: 0.1, minTierInclusive: 2, maxTierInclusive: 2, minQty: 1, maxQty: 1 }
                       Goblin_Duke:
                         forcedTier: 4
                         drops:
                           -{ itemId: "Ore_Mithril", chance: 0.25, minTierInclusive: 4, maxTierInclusive: 4, minQty: 1, maxQty: 5 }
                           -{ itemId: "Rock_Gem_Diamond", chance: 0.05, minTierInclusive: 4, maxTierInclusive: 4, minQty: 1, maxQty: 1 }
                           -{ itemId: "Ingredient_Life_Essence", chance: 1.0, minTierInclusive: 4, maxTierInclusive: 4, minQty: 3, maxQty: 7 }
                 \s
                 ==========================================""")
    public Map<String, InstanceRule> instanceRules = defaultInstanceRules();

    public static final class InstanceRule {
        public boolean enabled = true;
        public @Nullable String progressionStyle = null;
        @FixedArraySize(5) public double @Nullable [] spawnChancePerTier = null;
        @FixedArraySize(5) public boolean @Nullable [] allowedTiersToSpawn = null;
        public @Nullable Double distancePerTier = null;
        public @Nullable Double distanceBonusInterval = null;
        public @Nullable Float distanceHealthBonusPerInterval = null;
        public @Nullable Float distanceDamageBonusPerInterval = null;
        public @Nullable Float distanceHealthBonusCap = null;
        public @Nullable Float distanceDamageBonusCap = null;
        @FixedArraySize(5) public float @Nullable [] healthMultiplierPerTier = null;
        @FixedArraySize(5) public float @Nullable [] damageMultiplierPerTier = null;
        public @Nullable Boolean abilitiesEnabled = null;
        @FixedArraySize(5) public boolean @Nullable [] abilitiesEnabledPerTier = null;
        public @Nullable Map<String, boolean[]> abilityOverrides = null;
        @FixedArraySize(5) public int @Nullable [] vanillaDroplistExtraRollsPerTier = null;
        public @Nullable Double dropWeaponChance = null;
        public @Nullable Double dropArmorPieceChance = null;
        public @Nullable Double dropOffhandItemChance = null;
        public @Nullable List<RPGMobsConfig.ExtraDropRule> extraDrops = null;
        public @Nullable Boolean eliteFriendlyFireDisabled = null;
        public @Nullable Boolean eliteFallDamageDisabled = null;
        public @Nullable Map<String, MobOverride> mobOverrides = null;
    }

    public static final class MobOverride {
        public int forcedTier = -1;
        public @Nullable List<RPGMobsConfig.ExtraDropRule> drops = null;
    }

    public static @Nullable MobOverride resolveMobOverride(InstanceRule rule, @Nullable String roleName) {
        if (rule.mobOverrides == null || rule.mobOverrides.isEmpty() || roleName == null || roleName.isBlank()) {
            return null;
        }
        String roleLower = roleName.toLowerCase();
        MobOverride bestMatch = null;
        int bestLength = -1;

        for (Map.Entry<String, MobOverride> entry : rule.mobOverrides.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) continue;
            if (roleLower.contains(key.toLowerCase()) && key.length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = key.length();
            }
        }
        return bestMatch;
    }

    public static @Nullable String resolveInstanceTemplate(@Nullable String worldName) {
        if (worldName == null) return null;
        if (!worldName.startsWith("instance-")) return null;
        // "instance-" = 9 chars, "-" + UUID (36 chars) = 37 chars suffix
        if (worldName.length() <= 9 + 37) return null;
        return worldName.substring(9, worldName.length() - 37);
    }

    public @Nullable InstanceRule resolveRule(@Nullable String worldName) {
        if (instanceRules == null || worldName == null) return null;

        InstanceRule exact = instanceRules.get(worldName);
        if (exact != null) return exact;

        String template = resolveInstanceTemplate(worldName);
        if (template != null) {
            return instanceRules.get(template);
        }

        return null;
    }

    private static Map<String, InstanceRule> defaultInstanceRules() {
        Map<String, InstanceRule> map = new LinkedHashMap<>();

        // Example: fully configured dungeon instance
        InstanceRule exampleRule = new InstanceRule();
        exampleRule.enabled = true;
        exampleRule.progressionStyle = "NONE";
        exampleRule.spawnChancePerTier = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        exampleRule.allowedTiersToSpawn = new boolean[]{false, false, false, false, true};
        exampleRule.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        exampleRule.damageMultiplierPerTier = new float[]{1f, 2f, 2.5f, 3f, 3.5f};
        exampleRule.abilitiesEnabled = true;
        exampleRule.abilitiesEnabledPerTier = new boolean[]{false, false, false, true, true};
        exampleRule.abilityOverrides = new LinkedHashMap<>();
        exampleRule.abilityOverrides.put("charge_leap", new boolean[]{false, false, false, false, true});
        exampleRule.abilityOverrides.put("heal_leap", new boolean[]{false, false, false, true, true});
        exampleRule.abilityOverrides.put("undead_summon", new boolean[]{false, false, false, false, true});
        exampleRule.vanillaDroplistExtraRollsPerTier = new int[]{0, 0, 0, 0, 0};
        exampleRule.dropWeaponChance = 0.0;
        exampleRule.dropArmorPieceChance = 0.0;
        exampleRule.dropOffhandItemChance = 0.0;
        exampleRule.eliteFriendlyFireDisabled = true;
        exampleRule.eliteFallDamageDisabled = true;

        exampleRule.mobOverrides = new LinkedHashMap<>();

        MobOverride hermitOverride = new MobOverride();
        hermitOverride.forcedTier = 0;
        hermitOverride.drops = new ArrayList<>();
        hermitOverride.drops.add(createDrop("Ore_Copper", 0.3, 0, 0, 1, 3));
        hermitOverride.drops.add(createDrop("Ingredient_Bar_Copper", 0.15, 0, 0, 1, 2));
        exampleRule.mobOverrides.put("Goblin_Hermit", hermitOverride);

        MobOverride scavengerOverride = new MobOverride();
        scavengerOverride.forcedTier = 1;
        scavengerOverride.drops = new ArrayList<>();
        scavengerOverride.drops.add(createDrop("Potion_Health_Greater", 0.1, 2, 2, 1, 1));
        exampleRule.mobOverrides.put("Goblin_Scavenger", scavengerOverride);

        MobOverride scrapperOverride = new MobOverride();
        scrapperOverride.forcedTier = 1;
        scrapperOverride.drops = new ArrayList<>();
        scrapperOverride.drops.add(createDrop("Potion_Health_Greater", 0.1, 2, 2, 1, 1));
        exampleRule.mobOverrides.put("Goblin_Scrapper", scrapperOverride);

        MobOverride minerOverride = new MobOverride();
        minerOverride.forcedTier = 1;
        minerOverride.drops = new ArrayList<>();
        minerOverride.drops.add(createDrop("Potion_Health_Greater", 0.1, 2, 2, 1, 1));
        exampleRule.mobOverrides.put("Goblin_Miner", minerOverride);

         MobOverride lobberOverride = new MobOverride();
        lobberOverride.forcedTier = 2;
        lobberOverride.drops = new ArrayList<>();
        lobberOverride.drops.add(createDrop("Potion_Health_Greater", 0.1, 2, 2, 1, 1));
        exampleRule.mobOverrides.put("Goblin_Lobber", lobberOverride);

        MobOverride ogreOverride = new MobOverride();
        ogreOverride.forcedTier = 3;
        ogreOverride.drops = new ArrayList<>();
        ogreOverride.drops.add(createDrop("Ore_Silver", 0.2, 2, 2, 1, 3));
        ogreOverride.drops.add(createDrop("Ore_Gold", 0.15, 2, 2, 1, 2));
        ogreOverride.drops.add(createDrop("Potion_Health_Greater", 0.1, 2, 2, 1, 1));
        exampleRule.mobOverrides.put("Goblin_Ogre", ogreOverride);

        MobOverride dukeOverride = new MobOverride();
        dukeOverride.forcedTier = 4;
        dukeOverride.drops = new ArrayList<>();
        dukeOverride.drops.add(createDrop("Ore_Mithril", 0.25, 4, 4, 1, 5));
        dukeOverride.drops.add(createDrop("Rock_Gem_Diamond", 0.05, 4, 4, 1, 1));
        dukeOverride.drops.add(createDrop("Ingredient_Life_Essence", 1.0, 4, 4, 3, 7));
        exampleRule.mobOverrides.put("Goblin_Duke", dukeOverride);

        map.put("Basic", disabledRule());
        map.put("Challenge_Combat_1", disabledRule());
        map.put("CreativeHub", disabledRule());
        map.put("Default", disabledRule());
        map.put("Default_Flat", disabledRule());
        map.put("Default_Old", disabledRule());
        map.put("Default_Void", disabledRule());
        map.put("Dungeon_1", disabledRule());
        map.put("Dungeon_Goblin", exampleRule);
        map.put("Dungeon_Outlander", disabledRule());
        map.put("Forgotten_Temple", disabledRule());
        map.put("NPC_Faction_Gym", disabledRule());
        map.put("NPC_Gym", disabledRule());
        map.put("Persistent", disabledRule());
        map.put("Portals_Hedera", disabledRule());
        map.put("Portals_Henges", disabledRule());
        map.put("Portals_Jungles", disabledRule());
        map.put("Portals_Oasis", disabledRule());
        map.put("Portals_Taiga", disabledRule());
        map.put("ShortLived", disabledRule());
        map.put("ShortLivedSlow", disabledRule());
        map.put("TimeOut", disabledRule());
        map.put("Zone1_Plains1", disabledRule());
        map.put("Zone2_Desert1", disabledRule());
        map.put("Zone3_Taiga1", disabledRule());
        map.put("Zone4_Volcanic1", disabledRule());

        return map;
    }

    private static RPGMobsConfig.ExtraDropRule createDrop(String itemId, double chance, int minTier, int maxTier,
                                                             int minQty, int maxQty) {
        RPGMobsConfig.ExtraDropRule r = new RPGMobsConfig.ExtraDropRule();
        r.itemId = itemId;
        r.chance = chance;
        r.minTierInclusive = minTier;
        r.maxTierInclusive = maxTier;
        r.minQty = minQty;
        r.maxQty = maxQty;
        return r;
    }

    private static InstanceRule disabledRule() {
        InstanceRule rule = new InstanceRule();
        rule.enabled = false;
        return rule;
    }
}
