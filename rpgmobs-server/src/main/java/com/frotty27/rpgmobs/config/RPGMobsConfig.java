package com.frotty27.rpgmobs.config;

import com.frotty27.rpgmobs.assets.AssetConfig;
import com.frotty27.rpgmobs.assets.AssetType;
import com.frotty27.rpgmobs.assets.TieredAssetConfig;
import com.frotty27.rpgmobs.config.schema.*;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.utils.Constants;
import com.frotty27.rpgmobs.utils.MobRuleCategoryHelpers;
import com.google.gson.Gson;

import java.util.*;

import static com.frotty27.rpgmobs.utils.Constants.TIERS_AMOUNT;

public final class RPGMobsConfig {

    private static final String CATEGORY_PREFIX = "category:";
    private static final List<String> WEAPON_CATS_MELEE = List.of(CATEGORY_PREFIX+"Axes", CATEGORY_PREFIX+"Battleaxes", CATEGORY_PREFIX+"Clubs",
            CATEGORY_PREFIX+"Daggers", CATEGORY_PREFIX+"Longswords", CATEGORY_PREFIX+"Maces", CATEGORY_PREFIX+"Spears", CATEGORY_PREFIX+"Swords");
    private static final List<String> WEAPON_CATS_SWORDS = List.of(CATEGORY_PREFIX+"Swords");
    private static final List<String> WEAPON_CATS_AXES = List.of(CATEGORY_PREFIX+"Axes");
    private static final List<String> WEAPON_CATS_LONGSWORDS = List.of(CATEGORY_PREFIX+"Longswords");
    private static final List<String> WEAPON_CATS_CLUBS = List.of(CATEGORY_PREFIX+"Clubs");
    private static final List<String> WEAPON_CATS_SPEARS = List.of(CATEGORY_PREFIX+"Spears");
    private static final List<String> WEAPON_CATS_DAGGERS = List.of(CATEGORY_PREFIX+"Daggers");
    private static final List<String> WEAPON_CATS_PICKAXES = List.of(CATEGORY_PREFIX+"Pickaxes");
    private static final List<String> WEAPON_CATS_SHARP = List.of(CATEGORY_PREFIX+"Swords", CATEGORY_PREFIX+"Axes", CATEGORY_PREFIX+"Longswords", CATEGORY_PREFIX+"Battleaxes");
    private static final List<String> WEAPON_CATS_TWO_HANDED_SHARP = List.of(CATEGORY_PREFIX+"Longswords", CATEGORY_PREFIX+"Battleaxes");
    private static final List<String> WEAPON_CATS_BOWS = List.of(CATEGORY_PREFIX+"Shortbows", CATEGORY_PREFIX+"Crossbows");
    private static final List<String> WEAPON_CATS_STAVES = List.of(CATEGORY_PREFIX+"Staves");
    private static final List<String> WEAPON_CATS_GUNS = List.of(CATEGORY_PREFIX+"Guns");
    private static final List<String> WEAPON_CATS_SPELLBOOKS = List.of(CATEGORY_PREFIX+"Spellbooks");

    public static final String SUMMON_ROLE_PREFIX = "RPGMobs_Summon_";
    public static final int DEFAULT_MAX_ALIVE_MINIONS_PER_SUMMONER = 5;
    public static final int SUMMON_MAX_ALIVE_MIN = 0;
    public static final int SUMMON_MAX_ALIVE_MAX = 50;

    @CfgVersion
    @Cfg(group = "System", file = "core.yml", comment = "Configuration version. Automatically updated by the mod. WARNING: If this field is missing or set to 0.0.0, ALL config files will be deleted and regenerated with fresh defaults on next startup. Do not remove this field.")
    public String configVersion = "0.0.0";

    public final SpawningConfig spawning = new SpawningConfig();
    public final MobsConfig mobsConfig = new MobsConfig();
    public final HealthConfig healthConfig = new HealthConfig();
    public final DamageConfig damageConfig = new DamageConfig();
    public final ModelConfig modelConfig = new ModelConfig();
    public final GearConfig gearConfig = new GearConfig();
    public final LootConfig lootConfig = new LootConfig();
    public final NameplatesConfig nameplatesConfig = new NameplatesConfig();
    public final AssetGeneratorConfig assetGenerator = new AssetGeneratorConfig();
    public final AbilitiesConfig abilitiesConfig = new AbilitiesConfig();
    public final EffectsConfig effectsConfig = new EffectsConfig();
    public final CombatAIConfig combatAIConfig = new CombatAIConfig();
    public final IntegrationsConfig integrationsConfig = new IntegrationsConfig();
    public final DebugConfig debugConfig = new DebugConfig();

    public enum ProgressionStyle {
        ENVIRONMENT, DISTANCE_FROM_SPAWN, NONE
    }

    private static Map<String, List<String>> defaultTierPrefixesByFamily() {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("zombie", List.of("Rotting", "Ravenous", "Putrid", "Monstrous", "Evolved"));
        m.put("zombie_burnt", List.of("Charred", "Smoldering", "Ashen", "Cinderborn", "Infernal"));
        m.put("zombie_frost", List.of("Chilled", "Rimed", "Frostbitten", "Glacial", "Permafrost"));
        m.put("zombie_sand", List.of("Dustworn", "Scoured", "Dune-Cursed", "Tomb-Woken", "Sunwithered"));
        m.put("zombie_aberrant", List.of("Twisted", "Warped", "Mutated", "Horrid", "Eldritch"));

        m.put("skeleton", List.of("Broken", "Reforged", "Grim", "Deathbound", "Ascendant"));
        m.put("skeleton_burnt", List.of("Charred", "Smoldering", "Ashen", "Cinderforged", "Infernal"));
        m.put("skeleton_frost", List.of("Chilled", "Rimed", "Frostbound", "Glacial", "Permafrost"));
        m.put("skeleton_sand", List.of("Dustworn", "Scoured", "Sun-Cursed", "Tomb-Bound", "Sandscoured"));
        m.put("skeleton_pirate", List.of("Bilge", "Saltstained", "Blackwater", "Drowned", "Forsaken"));
        m.put("skeleton_incandescent", List.of("Husk", "Vanguard", "Sentinel", "Warden", "Paragon"));

        m.put("goblin", List.of("Sneaky", "Cutthroat", "Brutal", "Overseer", "Overlord"));
        m.put("trork", List.of("Rough", "Hardened", "Blooded", "Warbound", "Warlord"));
        m.put("outlander", List.of("Ragged", "Veteran", "Battle-Scarred", "Ruthless", "Legendary"));

        m.put("void", List.of("Faded", "Shaded", "Umbral", "Abyssal", "Voidborn"));

        m.put("wraith", List.of("Dim", "Hollow", "Veiled", "Phantom", "Transcendent"));

        m.put("default", List.of("Common", "Uncommon", "Rare", "Epic", "Legendary"));
        return m;
    }

    private static Map<String, EnvironmentTierRule> defaultEnvironmentTierSpawns() {
        Map<String, EnvironmentTierRule> map = new LinkedHashMap<>();

        EnvironmentTierRule zone0 = new EnvironmentTierRule();
        zone0.enabled = true;
        zone0.spawnChancePerTier = new double[]{100, 0, 0, 0, 0};
        map.put("zone0", zone0);

        EnvironmentTierRule zone1 = new EnvironmentTierRule();
        zone1.enabled = true;
        zone1.spawnChancePerTier = new double[]{60, 25, 15, 0, 0};
        map.put("zone1", zone1);

        EnvironmentTierRule zone2 = new EnvironmentTierRule();
        zone2.enabled = true;
        zone2.spawnChancePerTier = new double[]{50, 25, 18, 7, 0};
        map.put("zone2", zone2);

        EnvironmentTierRule zone3 = new EnvironmentTierRule();
        zone3.enabled = true;
        zone3.spawnChancePerTier = new double[]{0, 32, 28, 22, 18};
        map.put("zone3", zone3);

        EnvironmentTierRule zone4 = new EnvironmentTierRule();
        zone4.enabled = true;
        zone4.spawnChancePerTier = new double[]{0, 0, 40, 33, 27};
        map.put("zone4", zone4);

        return map;
    }

    public static final class NameplatesConfig {
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Enable or disable nameplates globally.")
        public boolean enableMobNameplates = true;

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Nameplate style: RANKED_ROLE (recommended) or SIMPLE.")
        public NameplateMode nameplateMode = NameplateMode.RANKED_ROLE;

        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Enable nameplates for specific tiers.")
        public boolean[] mobNameplatesEnabledPerTier = {true, true, true, true, true};

        @FixedArraySize(TIERS_AMOUNT)
        @Default
        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Visual indicators for each tier.")
        public String[] monNameplatePrefixPerTier = {"[•]", "[• •]", "[• • •]", "[• • • •]", "[• • • • •]"};

        @Cfg(group = "Nameplates", file = "visuals.yml", comment = "Tier-based name prefixes per family (Zombie, Skeleton, etc.). Each list must have 5 values.")
        public Map<String, List<String>> defaultedTierPrefixesByFamily = defaultTierPrefixesByFamily();
    }

    public static final class IntegrationsConfig {

        public final RPGLevelingConfig rpgLeveling = new RPGLevelingConfig();

        public static final class RPGLevelingConfig {
            @Default
            @Cfg(group = "Integrations.RPGLeveling", file = "core.yml", comment = "Enable RPGLeveling XP integration. Requires RPGLeveling to be installed.")
            public boolean enabled = true;

            @Default
            @FixedArraySize(TIERS_AMOUNT)
            @Cfg(group = "Integrations.RPGLeveling", file = "core.yml",
                 comment = "XP multiplier per tier. Applied to the base XP determined by RPGLeveling.\n"
                           + "Example: Tier 3 with 2.0 means the player receives 2x the base XP.")
            public float[] xpMultiplierPerTier = {1.0f, 1.5f, 2.0f, 3.0f, 5.0f};

            @Default
            @Min(0.0)
            @Cfg(group = "Integrations.RPGLeveling", file = "core.yml",
                 comment = "Bonus XP added for each active ability on the killed elite.\n"
                           + "Abilities: Charge Leap, Heal Potion, Undead Summon.")
            public double xpBonusPerAbility = 2500.0;

            @Default
            @Min(0.0)
            @Cfg(group = "Integrations.RPGLeveling", file = "core.yml",
                 comment = "XP multiplier for summoned minion kills.\n"
                           + "0.05 = 5% of the base XP. Set to 0.0 to grant no XP for minions.")
            public double minionXPMultiplier = 0.05;
        }
    }

    public enum NameplateMode {
        SIMPLE, RANKED_ROLE
    }

    public static final class SpawningConfig {
        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Progression system: ENVIRONMENT (Zone-based), DISTANCE_FROM_SPAWN (Linear scaling), or NONE (Random).")
        public ProgressionStyle progressionStyle = ProgressionStyle.ENVIRONMENT;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Global tier spawn weights used for NONE style or as fallback. Higher = more likely relative to sum.")
        public double[] spawnChancePerTier = {46, 28, 16, 8, 4};

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Blocks required per tier transition (e.g. 1000m = Tier 1, 2000m = Tier 2).")
        public double distancePerTier = 1000.0;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Block interval for applying bonus stats (e.g. every 100 blocks).")
        public double distanceBonusInterval = 100.0;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Health multiplier bonus added per interval (0.01 = +1% health every 100m).")
        public float distanceHealthBonusPerInterval = 0.01f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Damage multiplier bonus added per interval (0.005 = +0.5% damage every 100m).")
        public float distanceDamageBonusPerInterval = 0.005f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Max bonus health multiplier added by distance progression (0.5 = +50% base health max).")
        public float distanceHealthBonusCap = 0.5f;

        @Default
        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Max bonus damage multiplier added by distance progression (0.5 = +50% base damage max).")
        public float distanceDamageBonusCap = 0.5f;

        @Cfg(group = "Spawning", file = "spawning.yml", comment = "Zone-based spawn weight rules. Key is a zone substring (e.g. zone1, zone2). Environments with no matching zone do not spawn elites.")
        public Map<String, EnvironmentTierRule> defaultEnvironmentTierSpawns = defaultEnvironmentTierSpawns();
    }

    public static final class EnvironmentTierRule {
        @Default
        public boolean enabled = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        public double[] spawnChancePerTier = {46, 28, 16, 8, 4};
    }

    public static final class HealthConfig {
        @Default
        @Cfg(group = "Health", file = "stats.yml", comment = "Enable or disable health scaling for RPGMobs.")
        public boolean enableMobHealthScaling = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Health", file = "stats.yml", comment = "Base health multiplier per tier.")
        public float[] mobHealthMultiplierPerTier = {0.6f, 1.0f, 1.5f, 2.1f, 2.8f};

        @Default
        @Min(0.0)
        @Max(1.0)
        @Cfg(group = "Health", file = "stats.yml", comment = "Random health variance multiplier (e.g. 0.05 = +/-5% health).")
        public float mobHealthRandomVariance = 0.05f;
    }

    public static final class AssetGeneratorConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @YamlIgnore
        public transient String[] tierSuffixes = {"Tier_1", "Tier_2", "Tier_3", "Tier_4", "Tier_5"};
    }

    public static final class GearConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Gear", file = "gear.yml", comment = "Number of armor slots to fill per tier (0-4).")
        public int[] armorPiecesToEquipPerTier = {0, 1, 2, 3, 4};

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Gear", file = "gear.yml", comment = "Probability of equipping a utility item (shield/torch) per tier.")
        public double[] shieldUtilityChancePerTier = {0.0, 0.0, 0.20, 0.40, 0.60};

        @Min(0.001)
        @Max(1.0)
        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Minimum item durability fraction on spawn (0.0 to 1.0).")
        public double spawnGearDurabilityMin = 0.02;

        @Min(0.001)
        @Max(1.0)
        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Maximum item durability fraction on spawn (0.0 to 1.0).")
        public double spawnGearDurabilityMax = 0.30;

        @Default
        @Cfg(group = "Gear", file = "gear.yml", comment = "Weapon ID's that contain these words will be marked as two-handed (no shield).")
        public List<String> twoHandedWeaponIds = new ArrayList<>(List.of("shortbow",
                                                                         "crossbow",
                                                                         "spear",
                                                                         "staff",
                                                                         "battleaxe",
                                                                         "longsword",
                                                                         "bomb"
        ));

        @Cfg(group = "Gear", file = "gear.yml", comment = "Weapon rarity rules: maps ID fragments to rarities. First match wins.")
        public Map<String, String> defaultWeaponRarityRules = defaultWeaponRarityRules();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Armor rarity rules: maps ID fragments to rarities. First match wins.")
        public Map<String, String> defaultArmorRarityRules = defaultArmorRarityRules();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Allowed rarities per tier (Tier 1-5).")
        public List<List<String>> defaultTierAllowedRarities = defaultTierAllowedRarities();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Probability of equipping a rarity per tier.")
        public List<Map<String, Double>> defaultTierEquipmentRarityWeights = defaultTierEquipmentRarityWeights();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Valid weapon IDs for Elite generation.")
        public List<String> defaultWeaponCatalog = defaultWeaponCatalog();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Valid armor materials for Elite generation.")
        public List<String> defaultArmorMaterials = defaultArmorMaterials();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Weapon category tree: organizes weapon IDs into hierarchical groups.")
        public GearCategory weaponCategoryTree = defaultWeaponCategoryTree();

        @Cfg(group = "Gear", file = "gear.yml", comment = "Armor category tree: organizes full armor IDs (Armor_Material_Slot) into hierarchical groups.")
        public GearCategory armorCategoryTree = defaultArmorCategoryTree();
    }

    private static Map<String, String> defaultWeaponRarityRules() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("scarab", "common");
        m.put("silversteel", "common");
        m.put("iron_rusty", "common");
        m.put("steel_rusty", "common");
        m.put("wood", "common");
        m.put("crude", "common");
        m.put("copper", "common");

        m.put("flail", "epic");
        m.put("iron", "uncommon");
        m.put("stone", "uncommon");
        m.put("steel", "uncommon");
        m.put("scrap", "uncommon");
        m.put("bronze_ancient", "uncommon");
        m.put("bronze", "uncommon");
        m.put("potion_poison", "uncommon");

        m.put("thorium", "rare");
        m.put("spectral", "rare");
        m.put("bone", "rare");
        m.put("doomed", "rare");
        m.put("cobalt", "rare");
        m.put("ancient_steel", "rare");
        m.put("steel_ancient", "rare");
        m.put("tribal", "rare");
        m.put("bomb_stun", "rare");

        m.put("adamantite", "epic");
        m.put("onyxium", "epic");
        m.put("mithril", "epic");
        m.put("void", "epic");
        m.put("Halloween_Broomstick", "epic");
        m.put("bomb_continuous", "epic");
        m.put("praetorian", "epic");

        m.put("flame", "legendary");

        return m;
    }

    private static Map<String, String> defaultArmorRarityRules() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cotton", "common");
        m.put("linen", "common");
        m.put("silk", "common");
        m.put("wool", "common");
        m.put("wood", "common");
        m.put("copper", "common");

        m.put("diving", "uncommon");
        m.put("kweebec", "uncommon");
        m.put("leather", "uncommon");
        m.put("trork", "uncommon");
        m.put("iron", "uncommon");
        m.put("steel", "uncommon");
        m.put("bronze", "uncommon");

        m.put("thorium", "rare");
        m.put("cobalt", "rare");
        m.put("steel_ancient", "rare");
        m.put("cindercloth", "rare");
        m.put("bronze_ornate", "rare");

        m.put("prisma", "epic");
        m.put("adamantite", "epic");
        m.put("mithril", "epic");
        m.put("praetorian", "epic");
        m.put("onyxium", "epic");

        return m;
    }

    private static List<List<String>> defaultTierAllowedRarities() {
        return List.of(List.of("common"),
                       List.of("uncommon"),
                       List.of("rare"),
                       List.of("epic"),
                       List.of("epic", "legendary")
        );
    }

    private static List<Map<String, Double>> defaultTierEquipmentRarityWeights() {
        return List.of(mapOf("common", 1.0),
                       mapOf("uncommon", 1.0),
                       mapOf("rare", 0.70, "uncommon", 0.30),
                       mapOf("epic", 0.70, "rare", 0.30),
                       mapOf("legendary", 0.40, "epic", 0.60)
        );
    }

    private static List<String> defaultWeaponCatalog() {
        return new ArrayList<>(List.of("Weapon_Axe_Adamantite",
                                       "Weapon_Axe_Bone",
                                       "Weapon_Axe_Cobalt",
                                       "Weapon_Axe_Copper",
                                       "Weapon_Axe_Crude",
                                       "Weapon_Axe_Doomed",
                                       "Weapon_Axe_Iron_Rusty",
                                       "Weapon_Axe_Iron",
                                       "Weapon_Axe_Mithril",
                                       "Weapon_Axe_Onyxium",
                                       "Weapon_Axe_Stone_Trork",
                                       "Weapon_Axe_Thorium",
                                       "Weapon_Axe_Tribal",

                                       "Weapon_Battleaxe_Adamantite",
                                       "Weapon_Battleaxe_Cobalt",
                                       "Weapon_Battleaxe_Copper",
                                       "Weapon_Battleaxe_Crude",
                                       "Weapon_Battleaxe_Doomed",
                                       "Weapon_Battleaxe_Iron",
                                       "Weapon_Battleaxe_Mithril",
                                       "Weapon_Battleaxe_Onyxium",
                                       "Weapon_Battleaxe_Scarab",
                                       "Weapon_Battleaxe_Scythe_Void",
                                       "Weapon_Battleaxe_Steel_Rusty",
                                       "Weapon_Battleaxe_Stone_Trork",
                                       "Weapon_Battleaxe_Thorium",
                                       "Weapon_Battleaxe_Tribal",
                                       "Weapon_Battleaxe_Wood_Fence",

                                       "Weapon_Club_Adamantite",
                                       "Weapon_Club_Cobalt",
                                       "Weapon_Club_Copper",
                                       "Weapon_Club_Crude",
                                       "Weapon_Club_Doomed",
                                       "Weapon_Club_Iron_Rusty",
                                       "Weapon_Club_Iron",
                                       "Weapon_Club_Mithril",
                                       "Weapon_Club_Onyxium",
                                       "Weapon_Club_Scrap",
                                       "Weapon_Club_Steel_Flail_Rusty",
                                       "Weapon_Club_Stone_Trork",
                                       "Weapon_Club_Thorium",
                                       "Weapon_Club_Tribal",
                                       "Weapon_Club_Zombie_Arm",
                                       "Weapon_Club_Zombie_Burnt_Arm",
                                       "Weapon_Club_Zombie_Burnt_Leg",
                                       "Weapon_Club_Zombie_Frost_Arm",
                                       "Weapon_Club_Zombie_Frost_Leg",
                                       "Weapon_Club_Zombie_Leg",
                                       "Weapon_Club_Zombie_Sand_Arm",
                                       "Weapon_Club_Zombie_Sand_Leg",

                                       "Weapon_Crossbow_Ancient_Steel",
                                       "Weapon_Crossbow_Iron",

                                       "Weapon_Daggers_Adamantite_Saurian",
                                       "Weapon_Daggers_Adamantite",
                                       "Weapon_Daggers_Bone",
                                       "Weapon_Daggers_Bronze_Ancient",
                                       "Weapon_Daggers_Bronze",
                                       "Weapon_Daggers_Claw_Bone",
                                       "Weapon_Daggers_Cobalt",
                                       "Weapon_Daggers_Copper",
                                       "Weapon_Daggers_Crude",
                                       "Weapon_Daggers_Doomed",
                                       "Weapon_Daggers_Fang_Doomed",
                                       "Weapon_Daggers_Iron",
                                       "Weapon_Daggers_Mithril",
                                       "Weapon_Daggers_Onyxium",
                                       "Weapon_Daggers_Stone_Trork",
                                       "Weapon_Daggers_Thorium",

                                       "Weapon_Longsword_Adamantite_Saurian",
                                       "Weapon_Longsword_Adamantite",
                                       "Weapon_Longsword_Cobalt",
                                       "Weapon_Longsword_Copper",
                                       "Weapon_Longsword_Crude",
                                       "Weapon_Longsword_Flame",
                                       "Weapon_Longsword_Iron",
                                       "Weapon_Longsword_Katana",
                                       "Weapon_Longsword_Mithril",
                                       "Weapon_Longsword_Onyxium",
                                       "Weapon_Longsword_Praetorian",
                                       "Weapon_Longsword_Scarab",
                                       "Weapon_Longsword_Spectral",
                                       "Weapon_Longsword_Stone_Trork",
                                       "Weapon_Longsword_Thorium",
                                       "Weapon_Longsword_Tribal",
                                       "Weapon_Longsword_Void",

                                       "Weapon_Mace_Adamantite",
                                       "Weapon_Mace_Cobalt",
                                       "Weapon_Mace_Copper",
                                       "Weapon_Mace_Crude",
                                       "Weapon_Mace_Iron",
                                       "Weapon_Mace_Mithril",
                                       "Weapon_Mace_Onyxium",
                                       "Weapon_Mace_Prisma",
                                       "Weapon_Mace_Scrap",
                                       "Weapon_Mace_Stone_Trork",
                                       "Weapon_Mace_Thorium",

                                       "Weapon_Shield_Adamantite",
                                       "Weapon_Shield_Cobalt",
                                       "Weapon_Shield_Copper",
                                       "Weapon_Shield_Doomed",
                                       "Weapon_Shield_Iron",
                                       "Weapon_Shield_Mithril",
                                       "Weapon_Shield_Onyxium",
                                       "Weapon_Shield_Orbis_Incandescent",
                                       "Weapon_Shield_Orbis_Knight",
                                       "Weapon_Shield_Praetorian",
                                       "Weapon_Shield_Rusty",
                                       "Weapon_Shield_Scrap_Spiked",
                                       "Weapon_Shield_Scrap",
                                       "Weapon_Shield_Thorium",
                                       "Weapon_Shield_Wood",

                                       "Weapon_Shortbow_Adamantite",
                                       "Weapon_Shortbow_Bomb",
                                       "Weapon_Shortbow_Bronze",
                                       "Weapon_Shortbow_Cobalt",
                                       "Weapon_Shortbow_Combat",
                                       "Weapon_Shortbow_Copper",
                                       "Weapon_Shortbow_Crude",
                                       "Weapon_Shortbow_Doomed",
                                       "Weapon_Shortbow_Flame",
                                       "Weapon_Shortbow_Frost",
                                       "Weapon_Shortbow_Iron_Rusty",
                                       "Weapon_Shortbow_Iron",
                                       "Weapon_Shortbow_Mithril",
                                       "Weapon_Shortbow_Onyxium",
                                       "Weapon_Shortbow_Pull",
                                       "Weapon_Shortbow_Ricochet",
                                       "Weapon_Shortbow_Thorium",
                                       "Weapon_Shortbow_Vampire",

                                       "Weapon_Spear_Adamantite_Saurian",
                                       "Weapon_Spear_Adamantite",
                                       "Weapon_Spear_Bone",
                                       "Weapon_Spear_Bronze",
                                       "Weapon_Spear_Cobalt",
                                       "Weapon_Spear_Copper",
                                       "Weapon_Spear_Crude",
                                       "Weapon_Spear_Double_Incandescent",
                                       "Weapon_Spear_Fishbone",
                                       "Weapon_Spear_Iron",
                                       "Weapon_Spear_Leaf",
                                       "Weapon_Spear_Mithril",
                                       "Weapon_Spear_Onyxium",
                                       "Weapon_Spear_Scrap",
                                       "Weapon_Spear_Stone_Trork",
                                       "Weapon_Spear_Thorium",
                                       "Weapon_Spear_Tribal",

                                       "Halloween_Broomstick",
                                       "Weapon_Staff_Adamantite",
                                       "Weapon_Staff_Bo_Bamboo",
                                       "Weapon_Staff_Bo_Wood",
                                       "Weapon_Staff_Bone",
                                       "Weapon_Staff_Bronze",
                                       "Weapon_Staff_Cane",
                                       "Weapon_Staff_Cobalt",
                                       "Weapon_Staff_Copper",
                                       "Weapon_Staff_Crystal_Fire_Trork",
                                       "Weapon_Staff_Crystal_Flame",
                                       "Weapon_Staff_Crystal_Ice",
                                       "Weapon_Staff_Crystal_Purple",
                                       "Weapon_Staff_Crystal_Red",
                                       "Weapon_Staff_Doomed",
                                       "Weapon_Staff_Frost",
                                       "Weapon_Staff_Iron",
                                       "Weapon_Staff_Mithril",
                                       "Weapon_Staff_Onion",
                                       "Weapon_Staff_Onyxium",
                                       "Weapon_Staff_Thorium",
                                       "Weapon_Staff_Wizard",
                                       "Weapon_Staff_Wood_Kweebec",
                                       "Weapon_Staff_Wood_Rotten",
                                       "Weapon_Staff_Wood",

                                       "Weapon_Sword_Adamantite",
                                       "Weapon_Sword_Bone",
                                       "Weapon_Sword_Bronze_Ancient",
                                       "Weapon_Sword_Bronze",
                                       "Weapon_Sword_Cobalt",
                                       "Weapon_Sword_Copper",
                                       "Weapon_Sword_Crude",
                                       "Weapon_Sword_Cutlass",
                                       "Weapon_Sword_Doomed",
                                       "Weapon_Sword_Frost",
                                       "Weapon_Sword_Iron",
                                       "Weapon_Sword_Mithril",
                                       "Weapon_Sword_Nexus",
                                       "Weapon_Sword_Onyxium",
                                       "Weapon_Sword_Runic",
                                       "Weapon_Sword_Scrap",
                                       "Weapon_Sword_Silversteel",
                                       "Weapon_Sword_Steel_Incandescent",
                                       "Weapon_Sword_Steel_Rusty",
                                       "Weapon_Sword_Steel",
                                       "Weapon_Sword_Stone_Trork",
                                       "Weapon_Sword_Thorium",
                                       "Weapon_Sword_Wood",

                                       "Tool_Pickaxe_Adamantite",
                                       "Tool_Pickaxe_Cobalt",
                                       "Tool_Pickaxe_Copper",
                                       "Tool_Pickaxe_Crude",
                                       "Tool_Pickaxe_Iron",
                                       "Tool_Pickaxe_Mithril",
                                       "Tool_Pickaxe_Onyxium",
                                       "Tool_Pickaxe_Scrap",
                                       "Tool_Pickaxe_Thorium",
                                       "Tool_Pickaxe_Wood",

                                       "Weapon_Bomb",
                                       "Weapon_Bomb_Stun",
                                       "Weapon_Bomb_Potion_Poison",
                                       "Weapon_Bomb_Continuous",

                                       "Weapon_Kunai",

                                       "Weapon_Gun_Blunderbuss",
                                       "Weapon_Gun_Blunderbuss_Rusty",

                                       "Weapon_Spellbook_Demon",
                                       "Weapon_Spellbook_Fire",
                                       "Weapon_Spellbook_Grimoire_Brown",
                                       "Weapon_Spellbook_Grimoire_Purple",
                                       "Weapon_Spellbook_Rekindle_Embers"
        ));
    }

    private static List<String> defaultArmorMaterials() {
        return new ArrayList<>(List.of("Adamantite",
                                       "Bronze",
                                       "Bronze_Ornate",
                                       "Cloth_Cindercloth",
                                       "Cloth_Cotton",
                                       "Cloth_Linen",
                                       "Cloth_Silk",
                                       "Cloth_Wool",
                                       "Cobalt",
                                       "Copper",
                                       "Diving_Crude",
                                       "Iron",
                                       "Kweebec",
                                       "Leather_Heavy",
                                       "Leather_Light",
                                       "Leather_Medium",
                                       "Leather_Raven",
                                       "Leather_Soft",
                                       "Mithril",
                                       "Onyxium",
                                       "Prisma",
                                       "Steel",
                                       "Steel_Ancient",
                                       "Thorium",
                                       "Trork",
                                       "Wood"
        ));
    }

    private static GearCategory defaultWeaponCategoryTree() {
        List<String> catalog = defaultWeaponCatalog();
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        Map<String, String> prefixToCategory = new LinkedHashMap<>();
        prefixToCategory.put("Weapon_Axe_", "Axes");
        prefixToCategory.put("Weapon_Battleaxe_", "Battleaxes");
        prefixToCategory.put("Weapon_Club_", "Clubs");
        prefixToCategory.put("Weapon_Crossbow_", "Crossbows");
        prefixToCategory.put("Weapon_Daggers_", "Daggers");
        prefixToCategory.put("Weapon_Longsword_", "Longswords");
        prefixToCategory.put("Weapon_Mace_", "Maces");
        prefixToCategory.put("Weapon_Shield_", "Shields");
        prefixToCategory.put("Weapon_Shortbow_", "Shortbows");
        prefixToCategory.put("Weapon_Spear_", "Spears");
        prefixToCategory.put("Weapon_Staff_", "Staves");
        prefixToCategory.put("Weapon_Sword_", "Swords");
        prefixToCategory.put("Tool_Pickaxe_", "Pickaxes");
        prefixToCategory.put("Weapon_Bomb", "Bombs");
        prefixToCategory.put("Weapon_Kunai", "Other");
        prefixToCategory.put("Weapon_Gun_", "Guns");
        prefixToCategory.put("Weapon_Spellbook_", "Spellbooks");
        prefixToCategory.put("Halloween_Broomstick", "Staves");
        prefixToCategory.put("Halloween_", "Other");

        for (String weaponId : catalog) {
            String assigned = null;
            for (var entry : prefixToCategory.entrySet()) {
                if (weaponId.startsWith(entry.getKey())) {
                    assigned = entry.getValue();
                    break;
                }
            }
            if (assigned == null) assigned = "Other";
            buckets.computeIfAbsent(assigned, _ -> new ArrayList<>()).add(weaponId);
        }

        List<GearCategory> children = new ArrayList<>();
        for (String catName : List.of("Axes", "Battleaxes", "Clubs", "Crossbows", "Daggers",
                "Guns", "Longswords", "Maces", "Pickaxes", "Shields", "Shortbows", "Spears",
                "Spellbooks", "Staves", "Swords", "Bombs", "Other")) {
            List<String> items = buckets.get(catName);
            if (items != null && !items.isEmpty()) {
                children.add(new GearCategory(catName, items));
            }
        }
        return new GearCategory("All", List.of(), children.toArray(new GearCategory[0]));
    }

    private static GearCategory defaultArmorCategoryTree() {
        String[] materials = {
                "Adamantite", "Bronze", "Bronze_Ornate", "Cloth_Cindercloth", "Cloth_Cotton",
                "Cloth_Linen", "Cloth_Silk", "Cloth_Wool", "Cobalt", "Copper",
                "Diving_Crude", "Iron", "Kweebec", "Leather_Heavy", "Leather_Light",
                "Leather_Medium", "Leather_Raven", "Leather_Soft", "Mithril", "Onyxium",
                "Prisma", "Steel", "Steel_Ancient", "Thorium", "Trork", "Wood"
        };
        var children = new ArrayList<GearCategory>();
        for (String material : materials) {
            children.add(new GearCategory(material, armorIdsForMaterials(material)));
        }
        return new GearCategory("All", List.of(), children.toArray(new GearCategory[0]));
    }

    private static List<String> armorIdsForMaterials(String... materials) {
        List<String> ids = new ArrayList<>();
        for (String material : materials) {
            ids.add("Armor_" + material + "_Head");
            ids.add("Armor_" + material + "_Chest");
            ids.add("Armor_" + material + "_Hands");
            ids.add("Armor_" + material + "_Legs");
        }
        return ids;
    }

    public static final class ModelConfig {
        @Default
        @Cfg(group = "Model", file = "visuals.yml", comment = "Enable or disable physical size scaling per tier.")
        public boolean enableMobModelScaling = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Model", file = "visuals.yml", comment = "Physical scale multiplier per tier.")
        public float[] mobModelScaleMultiplierPerTier = {0.74f, 0.85f, 0.96f, 1.07f, 1.18f};

        @Min(0.0)
        @Max(0.2)
        @Default
        @Cfg(group = "Model", file = "visuals.yml", comment = "Random size variance (e.g., 0.04 = +/-4% size).")
        public float mobModelScaleRandomVariance = 0.04f;
    }

    public static final class LootConfig {
        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Loot", file = "loot.yml", comment = "Extra rolls of the mob's vanilla drop table per tier. 0 = no extra drops.")
        public int[] vanillaDroplistExtraRollsPerTier = {0, 0, 1, 1, 2};

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop its main-hand weapon.")
        public double dropWeaponChance = 0.05;

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop an armor piece.")
        public double dropArmorPieceChance = 0.05;

        @Min(0.0)
        @Max(1.0)
        @Default
        @Cfg(group = "Loot", file = "loot.yml", comment = "Chance for an Elite to drop its off-hand item (shields, torches, etc.).")
        public double dropOffhandItemChance = 0.05;

        @Cfg(group = "Loot", file = "loot.yml", comment = "Loot templates: named reusable drop tables linked to mob rule categories.")
        public Map<String, LootTemplate> lootTemplates = defaultLootTemplates();

        @Cfg(group = "Loot", file = "loot.yml", comment = "Loot template category tree for UI organization.")
        public LootTemplateCategory lootTemplateTree = defaultLootTemplateTree();
    }

    public static final class DamageConfig {
        @Default
        @Cfg(group = "Damage", file = "stats.yml", comment = "Enable or disable damage scaling.")
        public boolean enableMobDamageMultiplier = true;

        @Default
        @FixedArraySize(TIERS_AMOUNT)
        @Cfg(group = "Damage", file = "stats.yml", comment = "Base damage multiplier per tier.")
        public float[] mobDamageMultiplierPerTier = {0.6f, 1.1f, 1.6f, 2.1f, 2.6f};

        @Default
        @Min(0.0)
        @Max(1.0)
        @Cfg(group = "Damage", file = "stats.yml", comment = "Random damage variance multiplier (e.g. 0.05 = +/-5% damage).")
        public float mobDamageRandomVariance = 0.05f;
    }

    private static List<ExtraDropRule> tier1DropRules() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 0, 0, 1, 2));
        return list;
    }

    private static List<ExtraDropRule> tier2DropRules() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 1, 1, 1, 2));
        list.add(createExtraDropRule("Ore_Copper", 0.1, 1, 1, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Copper", 0.07, 1, 1, 1, 4));
        list.add(createExtraDropRule("Ore_Iron", 0.1, 1, 1, 1, 4));
        list.add(createExtraDropRule("Ingredient_Bar_Iron", 0.07, 1, 1, 1, 3));
        return list;
    }

    private static List<ExtraDropRule> tier3DropRules() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 2, 2, 2, 4));
        list.add(createExtraDropRule("Ore_Copper", 0.1, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Copper", 0.07, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ore_Iron", 0.1, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Iron", 0.07, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ore_Silver", 0.07, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Silver", 0.05, 2, 2, 1, 1));
        list.add(createExtraDropRule("Ore_Gold", 0.07, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Gold", 0.05, 2, 2, 1, 1));
        list.add(createExtraDropRule("Ingredient_Bar_Bronze", 0.05, 2, 2, 1, 1));
        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.1, 2, 2, 1, 2));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.13, 2, 2, 1, 2));
        return list;
    }

    private static List<ExtraDropRule> tier4DropRules() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 3, 3, 6, 11));
        list.add(createExtraDropRule("Ore_Iron", 0.1, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Iron", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ore_Silver", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Silver", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ore_Gold", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Gold", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ore_Cobalt", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Cobalt", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ingredient_Bar_Bronze", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ore_Thorium", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Thorium", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ore_Prisma", 0.07, 3, 3, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Prisma", 0.05, 3, 3, 1, 1));
        list.add(createExtraDropRule("Ore_Adamantite", 0.15, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Adamantite", 0.1, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Leather_Heavy", 0.09, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.11, 3, 3, 1, 3));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.15, 3, 3, 1, 3));
        list.add(createExtraDropRule("Rock_Gem_Ruby", 0.02, 3, 3, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Sapphire", 0.02, 3, 3, 1, 1));
        return list;
    }

    private static List<ExtraDropRule> tier5DropRules() {
        List<ExtraDropRule> list = new ArrayList<>();
        list.add(createExtraDropRule("Ingredient_Life_Essence", 1, 4, 4, 6, 11));
        list.add(createExtraDropRule("Ore_Silver", 0.07, 4, 4, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Silver", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Ore_Gold", 0.07, 4, 4, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Gold", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Ore_Prisma", 0.07, 4, 4, 1, 2));
        list.add(createExtraDropRule("Ingredient_Bar_Prisma", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Ore_Adamantite", 0.15, 4, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Adamantite", 0.1, 4, 4, 1, 3));
        list.add(createExtraDropRule("Ore_Mithril", 0.15, 4, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Bar_Mithril", 0.1, 4, 4, 1, 3));
        list.add(createExtraDropRule("Ore_Onyxium", 0.1, 4, 4, 1, 1));
        list.add(createExtraDropRule("Ingredient_Bar_Onyxium", 0.07, 4, 4, 1, 3));
        list.add(createExtraDropRule("Ingredient_Leather_Heavy", 0.15, 4, 4, 2, 4));
        list.add(createExtraDropRule("Ingredient_Leather_Medium", 0.2, 4, 4, 2, 4));
        list.add(createExtraDropRule("Ingredient_Leather_Light", 0.25, 4, 4, 2, 4));
        list.add(createExtraDropRule("Tool_Repair_Kit_Iron", 0.15, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Mana", 0.04, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Regen_Health", 0.04, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Health_Greater", 0.1, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Stamina_Greater", 0.1, 4, 4, 1, 2));
        list.add(createExtraDropRule("Potion_Mana_Large", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Regen_Health_Large", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Regen_Stamina_Large", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Health_Large", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Potion_Stamina_Large", 0.05, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Diamond", 0.02, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Ruby", 0.03, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Sapphire", 0.03, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Voidstone", 0.02, 4, 4, 1, 1));
        list.add(createExtraDropRule("Rock_Gem_Zephyr", 0.02, 4, 4, 1, 1));
        return list;
    }

    private static Map<String, LootTemplate> defaultLootTemplates() {
        Map<String, LootTemplate> templates = new LinkedHashMap<>();
        List<String> linkedAll = List.of(MobRuleCategoryHelpers.toCategoryKey("All"));

        templates.put("Tier 1 Loot", new LootTemplate("Tier 1 Loot", tier1DropRules(), linkedAll));
        templates.put("Tier 2 Loot", new LootTemplate("Tier 2 Loot", tier2DropRules(), linkedAll));
        templates.put("Tier 3 Loot", new LootTemplate("Tier 3 Loot", tier3DropRules(), linkedAll));
        templates.put("Tier 4 Loot", new LootTemplate("Tier 4 Loot", tier4DropRules(), linkedAll));
        templates.put("Tier 5 Loot", new LootTemplate("Tier 5 Loot", tier5DropRules(), linkedAll));

        return templates;
    }

    private static LootTemplateCategory defaultLootTemplateTree() {
        List<String> perTierKeys = new ArrayList<>(List.of(
                "Tier 1 Loot", "Tier 2 Loot", "Tier 3 Loot", "Tier 4 Loot", "Tier 5 Loot"
        ));
        var perTierCategory = new LootTemplateCategory("Per Tier", perTierKeys);
        return new LootTemplateCategory("All", List.of(), perTierCategory);
    }

    private static MobRuleCategory defaultCategoryTree() {

        var goblinLobber = new MobRuleCategory("Lobber", List.of(
                "Goblin_Lobber", "Goblin_Lobber_Patrol"
        ));
        var goblinMiner = new MobRuleCategory("Miner", List.of(
                "Goblin_Miner", "Goblin_Miner_Patrol"
        ));
        var goblinScavenger = new MobRuleCategory("Scavenger", List.of(
                "Goblin_Scavenger", "Goblin_Scavenger_Battleaxe", "Goblin_Scavenger_Sword"
        ));
        var goblinScrapper = new MobRuleCategory("Scrapper", List.of(
                "Goblin_Scrapper", "Goblin_Scrapper_Patrol"
        ));
        var goblinThief = new MobRuleCategory("Thief", List.of(
                "Goblin_Thief", "Goblin_Thief_Patrol"
        ));
        var goblins = new MobRuleCategory("Goblins", List.of(
                "Goblin_Duke", "Goblin_Hermit", "Goblin_Ogre"
        ), goblinLobber, goblinMiner, goblinScavenger, goblinScrapper, goblinThief);

        var outlanders = new MobRuleCategory("Outlanders", List.of(
                "Outlander_Berserker", "Outlander_Brute", "Outlander_Cultist",
                "Outlander_Hunter", "Outlander_Marauder", "Outlander_Peon",
                "Outlander_Priest", "Outlander_Sorcerer", "Outlander_Stalker"
        ));

        var trorkSentry = new MobRuleCategory("Sentry", List.of(
                "Trork_Sentry", "Trork_Sentry_Patrol"
        ));
        var trorkWarrior = new MobRuleCategory("Warrior", List.of(
                "Trork_Warrior", "Trork_Warrior_Patrol"
        ));
        var trorks = new MobRuleCategory("Trorks", List.of(
                "Trork_Brawler", "Trork_Chieftain", "Trork_Doctor_Witch", "Trork_Guard",
                "Trork_Hunter", "Trork_Mauler", "Trork_Shaman", "Trork_Unarmed"
        ), trorkSentry, trorkWarrior);

        var skelArcher = new MobRuleCategory("Archer", List.of(
                "Skeleton_Archer", "Skeleton_Archer_Patrol", "Skeleton_Archer_Wander"
        ));
        var skelArchmage = new MobRuleCategory("Archmage", List.of(
                "Skeleton_Archmage", "Skeleton_Archmage_Patrol", "Skeleton_Archmage_Wander"
        ));
        var skelFighter = new MobRuleCategory("Fighter", List.of(
                "Skeleton_Fighter", "Skeleton_Fighter_Patrol", "Skeleton_Fighter_Wander"
        ));
        var skelKnight = new MobRuleCategory("Knight", List.of(
                "Skeleton_Knight", "Skeleton_Knight_Patrol", "Skeleton_Knight_Wander"
        ));
        var skelMage = new MobRuleCategory("Mage", List.of(
                "Skeleton_Mage", "Skeleton_Mage_Patrol", "Skeleton_Mage_Wander"
        ));
        var skelRanger = new MobRuleCategory("Ranger", List.of(
                "Skeleton_Ranger", "Skeleton_Ranger_Patrol", "Skeleton_Ranger_Wander"
        ));
        var skelScout = new MobRuleCategory("Scout", List.of(
                "Skeleton_Scout", "Skeleton_Scout_Patrol", "Skeleton_Scout_Wander"
        ));
        var skelSoldier = new MobRuleCategory("Soldier", List.of(
                "Skeleton_Soldier", "Skeleton_Soldier_Patrol", "Skeleton_Soldier_Wander"
        ));

        var burntAlchemist = new MobRuleCategory("Alchemist", List.of(
                "Skeleton_Burnt_Alchemist", "Skeleton_Burnt_Alchemist_Patrol", "Skeleton_Burnt_Alchemist_Wander"
        ));
        var burntArcher = new MobRuleCategory("Archer", List.of(
                "Skeleton_Burnt_Archer", "Skeleton_Burnt_Archer_Patrol", "Skeleton_Burnt_Archer_Wander"
        ));
        var burntGunner = new MobRuleCategory("Gunner", List.of(
                "Skeleton_Burnt_Gunner", "Skeleton_Burnt_Gunner_Patrol", "Skeleton_Burnt_Gunner_Wander"
        ));
        var burntKnight = new MobRuleCategory("Knight", List.of(
                "Skeleton_Burnt_Knight", "Skeleton_Burnt_Knight_Patrol", "Skeleton_Burnt_Knight_Wander"
        ));
        var burntLancer = new MobRuleCategory("Lancer", List.of(
                "Skeleton_Burnt_Lancer", "Skeleton_Burnt_Lancer_Patrol", "Skeleton_Burnt_Lancer_Wander"
        ));
        var burntPraetorian = new MobRuleCategory("Praetorian", List.of(
                "Skeleton_Burnt_Praetorian", "Skeleton_Burnt_Praetorian_Patrol", "Skeleton_Burnt_Praetorian_Wander"
        ));
        var burntSoldier = new MobRuleCategory("Soldier", List.of(
                "Skeleton_Burnt_Soldier", "Skeleton_Burnt_Soldier_Patrol", "Skeleton_Burnt_Soldier_Wander"
        ));
        var burntWizard = new MobRuleCategory("Wizard", List.of(
                "Skeleton_Burnt_Wizard", "Skeleton_Burnt_Wizard_Patrol", "Skeleton_Burnt_Wizard_Wander"
        ));
        var skeletonsBurnt = new MobRuleCategory("Burnt", List.of(),
                burntAlchemist, burntArcher, burntGunner, burntKnight, burntLancer, burntPraetorian, burntSoldier, burntWizard);

        var frostArcher = new MobRuleCategory("Archer", List.of(
                "Skeleton_Frost_Archer", "Skeleton_Frost_Archer_Patrol", "Skeleton_Frost_Archer_Wander"
        ));
        var frostArchmage = new MobRuleCategory("Archmage", List.of(
                "Skeleton_Frost_Archmage", "Skeleton_Frost_Archmage_Patrol", "Skeleton_Frost_Archmage_Wander"
        ));
        var frostFighter = new MobRuleCategory("Fighter", List.of(
                "Skeleton_Frost_Fighter", "Skeleton_Frost_Fighter_Patrol", "Skeleton_Frost_Fighter_Wander"
        ));
        var frostKnight = new MobRuleCategory("Knight", List.of(
                "Skeleton_Frost_Knight", "Skeleton_Frost_Knight_Patrol", "Skeleton_Frost_Knight_Wander"
        ));
        var frostMage = new MobRuleCategory("Mage", List.of(
                "Skeleton_Frost_Mage", "Skeleton_Frost_Mage_Patrol", "Skeleton_Frost_Mage_Wander"
        ));
        var frostRanger = new MobRuleCategory("Ranger", List.of(
                "Skeleton_Frost_Ranger", "Skeleton_Frost_Ranger_Patrol", "Skeleton_Frost_Ranger_Wander"
        ));
        var frostScout = new MobRuleCategory("Scout", List.of(
                "Skeleton_Frost_Scout", "Skeleton_Frost_Scout_Patrol", "Skeleton_Frost_Scout_Wander"
        ));
        var frostSoldier = new MobRuleCategory("Soldier", List.of(
                "Skeleton_Frost_Soldier", "Skeleton_Frost_Soldier_Patrol", "Skeleton_Frost_Soldier_Wander"
        ));
        var skeletonsFrost = new MobRuleCategory("Frost", List.of(),
                frostArcher, frostArchmage, frostFighter, frostKnight, frostMage, frostRanger, frostScout, frostSoldier);

        var sandArcher = new MobRuleCategory("Archer", List.of(
                "Skeleton_Sand_Archer", "Skeleton_Sand_Archer_Patrol", "Skeleton_Sand_Archer_Wander"
        ));
        var sandArchmage = new MobRuleCategory("Archmage", List.of(
                "Skeleton_Sand_Archmage", "Skeleton_Sand_Archmage_Patrol", "Skeleton_Sand_Archmage_Wander"
        ));
        var sandAssassin = new MobRuleCategory("Assassin", List.of(
                "Skeleton_Sand_Assassin", "Skeleton_Sand_Assassin_Patrol", "Skeleton_Sand_Assassin_Wander"
        ));
        var sandGuard = new MobRuleCategory("Guard", List.of(
                "Skeleton_Sand_Guard", "Skeleton_Sand_Guard_Patrol", "Skeleton_Sand_Guard_Wander"
        ));
        var sandMage = new MobRuleCategory("Mage", List.of(
                "Skeleton_Sand_Mage", "Skeleton_Sand_Mage_Patrol", "Skeleton_Sand_Mage_Wander"
        ));
        var sandRanger = new MobRuleCategory("Ranger", List.of(
                "Skeleton_Sand_Ranger", "Skeleton_Sand_Ranger_Patrol", "Skeleton_Sand_Ranger_Wander"
        ));
        var sandScout = new MobRuleCategory("Scout", List.of(
                "Skeleton_Sand_Scout", "Skeleton_Sand_Scout_Patrol", "Skeleton_Sand_Scout_Wander"
        ));
        var sandSoldier = new MobRuleCategory("Soldier", List.of(
                "Skeleton_Sand_Soldier", "Skeleton_Sand_Soldier_Patrol", "Skeleton_Sand_Soldier_Wander"
        ));
        var skeletonsSand = new MobRuleCategory("Sand", List.of(),
                sandArcher, sandArchmage, sandAssassin, sandGuard, sandMage, sandRanger, sandScout, sandSoldier);

        var pirateCaptain = new MobRuleCategory("Captain", List.of(
                "Skeleton_Pirate_Captain", "Skeleton_Pirate_Captain_Patrol", "Skeleton_Pirate_Captain_Wander"
        ));
        var pirateGunner = new MobRuleCategory("Gunner", List.of(
                "Skeleton_Pirate_Gunner", "Skeleton_Pirate_Gunner_Patrol", "Skeleton_Pirate_Gunner_Wander"
        ));
        var pirateStriker = new MobRuleCategory("Striker", List.of(
                "Skeleton_Pirate_Striker", "Skeleton_Pirate_Striker_Patrol", "Skeleton_Pirate_Striker_Wander"
        ));
        var skeletonsPirate = new MobRuleCategory("Pirate", List.of(),
                pirateCaptain, pirateGunner, pirateStriker);

        var incanFighter = new MobRuleCategory("Fighter", List.of(
                "Skeleton_Incandescent_Fighter", "Skeleton_Incandescent_Fighter_Patrol", "Skeleton_Incandescent_Fighter_Wander"
        ));
        var incanFootman = new MobRuleCategory("Footman", List.of(
                "Skeleton_Incandescent_Footman", "Skeleton_Incandescent_Footman_Patrol", "Skeleton_Incandescent_Footman_Wander"
        ));
        var incanMage = new MobRuleCategory("Mage", List.of(
                "Skeleton_Incandescent_Mage", "Skeleton_Incandescent_Mage_Patrol", "Skeleton_Incandescent_Mage_Wander"
        ));
        var skeletonsIncandescent = new MobRuleCategory("Incandescent", List.of(
                "Skeleton_Incandescent_Head"
        ), incanFighter, incanFootman, incanMage);

        var skeletons = new MobRuleCategory("Skeletons", List.of("Skeleton"),
                skelArcher, skelArchmage, skelFighter, skelKnight, skelMage, skelRanger, skelScout, skelSoldier,
                skeletonsBurnt, skeletonsFrost, skeletonsSand, skeletonsPirate, skeletonsIncandescent);

        var zombiesAberrant = new MobRuleCategory("Aberrant", List.of(
                "Zombie_Aberrant", "Zombie_Aberrant_Big", "Zombie_Aberrant_Small"
        ));
        var zombies = new MobRuleCategory("Zombies", List.of(
                "Zombie", "Zombie_Burnt", "Zombie_Frost", "Zombie_Sand"
        ), zombiesAberrant);

        var wraiths = new MobRuleCategory("Wraiths", List.of("Wraith"));

        var voidMobs = new MobRuleCategory("Void", List.of(
                "Crawler_Void", "Eye_Void", "Spawn_Void", "Spectre_Void"
        ));

        return new MobRuleCategory("All", List.of(),
                goblins, outlanders, trorks, skeletons, zombies, wraiths, voidMobs
        );
    }

    public static final class EffectsConfig {
        @Default
        @Cfg(group = "Effects", file = "effects.yml", comment = "Effects configuration lives here. Say hello.")
        public Map<String, EntityEffectConfig> defaultEntityEffects = defaultEntityEffects();
    }

    private static Map<String, EntityEffectConfig> defaultEntityEffects() {
        Map<String, EntityEffectConfig> m = new LinkedHashMap<>();
        EntityEffectConfig projectileResistance = new EntityEffectConfig();
        projectileResistance.isEnabled = true;
        projectileResistance.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        projectileResistance.amountMultiplierPerTier = new float[]{0f, 0f, 0f, 0.7f, 0.85f};
        projectileResistance.infinite = true;
        projectileResistance.templates.add("projectile_resistance",
                                           "Entity/Effects/RPGMobs/RPGMobs_Effect_ProjectileResistance.template.json"
        );
        m.put("projectile_resistance", projectileResistance);

        return m;
    }

    public static final class CombatAIConfig {

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "How long (seconds) the CAE remembers hostile targets.")
        public int targetMemoryDuration = 10;

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "Minimum utility score for the CAE to run an evaluation cycle.")
        public double minRunUtility = 0.5;

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "Minimum utility score for an individual action to be selected.")
        public double minActionUtility = 0.01;

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "Combat styles defining how each faction fights. Key = style name (Disciplined, Berserker, Tactical, Chaotic).")
        public Map<String, FactionStyle> factionStyles = defaultFactionStyles();

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "Per-tier behavior escalation. Index 0 = T1, index 4 = T5.")
        public List<TierBehavior> tierBehaviors = defaultTierBehaviors();

        @Default
        @Cfg(group = "CombatAI", file = "combat.yml", comment = "Per-weapon combat parameters. Key = weapon category name (Swords, Daggers, Shortbows, etc.).")
        public Map<String, WeaponCombatParams> weaponParams = defaultWeaponParams();
    }

    public static final class FactionStyle {
        @Default public double attackCooldownMin = 1.0;
        @Default public double attackCooldownMax = 2.0;
        @Default public double shieldChargeFor = 1.5;
        @Default public double shieldSwitchPoint = 0.5;
        @Default public double backOffDistanceMin = 5.0;
        @Default public double backOffDistanceMax = 8.0;
        @Default public double backOffSwitchPoint = 6.0;
        @Default public double healthRetreatDistanceMin = 10.0;
        @Default public double healthRetreatDistanceMax = 15.0;
        @Default public double healthRetreatWeight = 5.0;
        @Default public double reEngageXRangeMin = 0.0;
        @Default public double reEngageXRangeMax = 10.0;
        @Default public double reEngageRandomiserMin = 0.4;
        @Default public double reEngageRandomiserMax = 0.6;
        @Default public double strafeCooldownMin = 1.5;
        @Default public double strafeCooldownMax = 3.0;
        @Default public boolean enableGroupObserve = true;
        @Default public boolean enableFlanking = false;
        @Default public String npcGroupName = "";
        @Default public double guardRandomiserMin = 0.4;
        @Default public double guardRandomiserMax = 1.0;
        @Default public double backOffRandomiserMin = 0.6;
        @Default public double backOffRandomiserMax = 1.0;
        @Default public double retreatCooldown = 15.0;
        @Default public double reEngageDistanceMin = 2.0;
        @Default public double reEngageDistanceMax = 3.0;
        @Default public double groupObserveDistanceMin = 8.0;
        @Default public double groupObserveDistanceMax = 12.0;

        public FactionStyle() {}

        public FactionStyle(double atkMin, double atkMax, double shieldCharge, double shieldSwitch,
                            double boMin, double boMax, double boSwitch,
                            double hrMin, double hrMax, double hrWeight,
                            double reMin, double reMax, double reRandMin, double reRandMax,
                            double strMin, double strMax, boolean observe, boolean flank, String group,
                            double guardRandMin, double guardRandMax, double boRandMin, double boRandMax,
                            double retCd, double reEngDistMin, double reEngDistMax,
                            double obsDistMin, double obsDistMax) {
            this.attackCooldownMin = atkMin; this.attackCooldownMax = atkMax;
            this.shieldChargeFor = shieldCharge; this.shieldSwitchPoint = shieldSwitch;
            this.backOffDistanceMin = boMin; this.backOffDistanceMax = boMax; this.backOffSwitchPoint = boSwitch;
            this.healthRetreatDistanceMin = hrMin; this.healthRetreatDistanceMax = hrMax; this.healthRetreatWeight = hrWeight;
            this.reEngageXRangeMin = reMin; this.reEngageXRangeMax = reMax;
            this.reEngageRandomiserMin = reRandMin; this.reEngageRandomiserMax = reRandMax;
            this.strafeCooldownMin = strMin; this.strafeCooldownMax = strMax;
            this.enableGroupObserve = observe; this.enableFlanking = flank; this.npcGroupName = group;
            this.guardRandomiserMin = guardRandMin; this.guardRandomiserMax = guardRandMax;
            this.backOffRandomiserMin = boRandMin; this.backOffRandomiserMax = boRandMax;
            this.retreatCooldown = retCd;
            this.reEngageDistanceMin = reEngDistMin; this.reEngageDistanceMax = reEngDistMax;
            this.groupObserveDistanceMin = obsDistMin; this.groupObserveDistanceMax = obsDistMax;
        }
    }

    public static final class TierBehavior {
        @Default public double cooldownMin = 1.5;
        @Default public double cooldownMax = 2.5;
        @Default public double strafeCooldownMin = 2.5;
        @Default public double strafeCooldownMax = 4.0;
        @Default public boolean hasShield = false;
        @Default public boolean hasBackOff = false;
        @Default public boolean hasRetreat = false;
        @Default public boolean hasGroupObserve = false;
        @Default public boolean hasFlanking = false;
        @Default public double shieldChargeFor = 0.0;
        @Default public double shieldGuardCooldown = 0.0;
        @Default public double retreatHealthThreshold = 0.0;
        @Default public double movementSpeedMultiplier = 1.0;

        public TierBehavior() {}

        public TierBehavior(double cooldownMin, double cooldownMax, double strafeCdMin, double strafeCdMax,
                            boolean shield, boolean backOff, boolean retreat, boolean observe, boolean flank,
                            double shieldCharge, double guardCd, double retreatThreshold, double moveSpeedMult) {
            this.cooldownMin = cooldownMin; this.cooldownMax = cooldownMax;
            this.strafeCooldownMin = strafeCdMin; this.strafeCooldownMax = strafeCdMax;
            this.hasShield = shield; this.hasBackOff = backOff; this.hasRetreat = retreat;
            this.hasGroupObserve = observe; this.hasFlanking = flank;
            this.shieldChargeFor = shieldCharge; this.shieldGuardCooldown = guardCd;
            this.retreatHealthThreshold = retreatThreshold;
            this.movementSpeedMultiplier = moveSpeedMult;
        }
    }

    public static final class WeaponCombatParams {
        @Default public double maxRange = 2.5;
        @Default public double speedMultiplier = 1.0;
        @Default public String attackRootInteraction = "Root_RPGMobs_Attack_Melee";
        @Default public boolean isRanged = false;
        @Default public String animationSetId = "Sword";
        @Default public List<String> attackChainAnimations = new ArrayList<>(List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"));
        @Default public String swingSoundId = "";
        @Default public String impactSoundId = "";
        @Default public String weaponTrailId = "";
        @Default public String hitParticleId = "";
        @Default public double hitboxEndDistance = 2.8;
        @Default public int hitboxConeLength = 60;
        @Default public int hitboxConeYaw = 30;
        @Default public double swingWindUpTime = 0.20;
        @Default public double swingRecoveryTime = 0.17;
        @Default public int combatSpeed = 8;

        public WeaponCombatParams() {}

        public WeaponCombatParams(double maxRange, double speedMult, String attackRoot, boolean ranged) {
            this.maxRange = maxRange; this.speedMultiplier = speedMult;
            this.attackRootInteraction = attackRoot; this.isRanged = ranged;
        }

        public WeaponCombatParams(double maxRange, double speedMult, String attackRoot, boolean ranged,
                                  String animSetId, List<String> chain, String swingSound, String impactSound,
                                  String trail, String hitParticle,
                                  double endDist, int coneLen, int coneYaw, double windUp, double recovery, int speed) {
            this.maxRange = maxRange; this.speedMultiplier = speedMult;
            this.attackRootInteraction = attackRoot; this.isRanged = ranged;
            this.animationSetId = animSetId;
            this.attackChainAnimations = new ArrayList<>(chain);
            this.swingSoundId = swingSound; this.impactSoundId = impactSound;
            this.weaponTrailId = trail; this.hitParticleId = hitParticle;
            this.hitboxEndDistance = endDist; this.hitboxConeLength = coneLen;
            this.hitboxConeYaw = coneYaw; this.swingWindUpTime = windUp;
            this.swingRecoveryTime = recovery; this.combatSpeed = speed;
        }
    }

    private static Map<String, FactionStyle> defaultFactionStyles() {
        Map<String, FactionStyle> m = new LinkedHashMap<>();
        m.put("Disciplined", new FactionStyle(1.0, 2.0, 1.5, 0.5, 5, 8, 8, 10, 15, 5.0, 0, 12, 0.4, 0.6, 1.5, 3.0, true, false, "Skeleton",
                0.4, 1.0, 0.6, 1.0, 15.0, 2.0, 3.0, 8.0, 12.0));
        m.put("Berserker",   new FactionStyle(0.5, 0.8, 2.0, 3.0, 3, 5, 14, 6, 8, 1.5, 0, 5, 0.6, 0.9, 0.8, 1.5, false, false, "Trork",
                0.7, 1.0, 0.8, 1.0, 25.0, 1.5, 2.5, 5.0, 8.0));
        m.put("Tactical",    new FactionStyle(0.8, 1.2, 1.0, 0.3, 6, 10, 6, 12, 18, 8.0, 0, 14, 0.3, 0.5, 1.2, 2.5, true, true, "Outlander",
                0.3, 0.9, 0.4, 0.8, 10.0, 3.0, 5.0, 10.0, 16.0));
        m.put("Chaotic",     new FactionStyle(0.5, 2.0, 0.8, 1.0, 3, 12, 5, 8, 20, 4.0, 0, 8, 0.2, 0.9, 0.5, 2.5, false, false, "Goblin",
                0.6, 1.0, 0.3, 1.0, 12.0, 2.0, 6.0, 6.0, 14.0));
        return m;
    }

    private static List<TierBehavior> defaultTierBehaviors() {
        List<TierBehavior> list = new ArrayList<>();
        list.add(new TierBehavior(0.4, 0.8, 1.0, 2.0, false, false, false, false, false, 0, 0, 0, 1.20));
        list.add(new TierBehavior(0.6, 1.0, 1.2, 2.5, true, true, false, false, false, 2.0, 8.0, 0, 1.10));
        list.add(new TierBehavior(0.8, 1.4, 1.5, 3.0, true, true, true, true, false, 3.0, 6.0, 0.30, 1.0));
        list.add(new TierBehavior(1.0, 1.8, 2.0, 3.5, true, true, true, true, false, 4.0, 5.0, 0.25, 0.85));
        list.add(new TierBehavior(1.2, 2.2, 2.5, 4.0, true, true, true, true, true, 5.0, 4.0, 0.20, 0.70));
        return list;
    }

    private static Map<String, WeaponCombatParams> defaultWeaponParams() {
        Map<String, WeaponCombatParams> m = new LinkedHashMap<>();
        m.put("Swords",     new WeaponCombatParams(2.5, 0.8, "Root_RPGMobs_Attack_Swords", false,
                "Sword", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Sword_T1_Swing", "SFX_Sword_T2_Impact", "Medium_Default", "Impact_Sword_Basic",
                2.8, 60, 30, 0.20, 0.17, 8));
        m.put("Longswords", new WeaponCombatParams(2.8, 1.0, "Root_RPGMobs_Attack_Longswords", false,
                "Longsword", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Sword_T1_Swing", "SFX_Longsword_Steel_Impact", "Sword_Distortion", "Impact_Sword_Basic",
                3.0, 65, 30, 0.25, 0.20, 8));
        m.put("Daggers",    new WeaponCombatParams(2.0, 0.5, "Root_RPGMobs_Attack_Daggers", false,
                "Daggers", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Daggers_T1_Swing", "SFX_Daggers_T2_Slash_Impact", "Daggers_Dash", "Impact_Dagger_Slash",
                2.2, 55, 30, 0.15, 0.12, 9));
        m.put("Axes",       new WeaponCombatParams(2.5, 0.9, "Root_RPGMobs_Attack_Axes", false,
                "Axe", List.of("SwingLeft", "SwingRight", "SwingDownLeft", "SwingRight", "SwingLeft"),
                "SFX_Axe_Iron_Swing", "SFX_Axe_Iron_Impact", "Medium_Default", "Impact_Sword_Basic",
                2.8, 55, 25, 0.20, 0.17, 8));
        m.put("Battleaxes", new WeaponCombatParams(3.0, 1.3, "Root_RPGMobs_Attack_Battleaxes", false,
                "Battleaxe", List.of("SwingLeft", "SwingRight", "SwingDownLeft", "SwingDownRight", "SwingLeft"),
                "SFX_Battleaxe_T1_Swing", "SFX_Battleaxe_T2_Impact", "Large_Charged_Red", "Impact_Battleaxe_Bash",
                2.8, 70, 35, 0.35, 0.30, 7));
        m.put("Maces",      new WeaponCombatParams(2.5, 1.1, "Root_RPGMobs_Attack_Maces", false,
                "Mace", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Mace_T1_Swing", "SFX_Mace_T2_Impact", "Medium_Mace_Charged", "Impact_Mace_Basic",
                2.8, 60, 30, 0.50, 0.25, 7));
        m.put("Clubs",      new WeaponCombatParams(2.5, 1.0, "Root_RPGMobs_Attack_Clubs", false,
                "Club", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Mace_T1_Swing", "SFX_T1_Impact_Blunt", "Large_Charged", "Impact_Mace_Basic",
                2.8, 55, 25, 0.22, 0.17, 8));
        m.put("ClubsFlail", new WeaponCombatParams(2.8, 1.1, "Root_RPGMobs_Attack_ClubsFlail", false,
                "Club_Flail", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Flail_Swing", "SFX_T1_Impact_Blunt", "Large_Charged", "Impact_Mace_Basic",
                2.8, 60, 30, 0.25, 0.20, 7));
        m.put("Spears",     new WeaponCombatParams(3.0, 0.85, "Root_RPGMobs_Attack_Spears", false,
                "Spear", List.of("SwingLeft", "SwingRight", "SwingLeft", "SwingRight"),
                "SFX_Spear_Lunge", "SFX_Spear_Impact", "Medium_Default", "Impact_Sword_Basic",
                3.0, 70, 15, 0.25, 0.20, 8));
        m.put("Pickaxes",   new WeaponCombatParams(2.3, 0.9, "Root_RPGMobs_Attack_Swords", false,
                "Pickaxe", List.of("SwingLeft", "SwingRight", "SwingDown", "SwingLeft", "SwingRight"),
                "SFX_Sword_T1_Swing", "SFX_Sword_T2_Impact", "Mace_Signature", "Impact_Sword_Basic",
                2.8, 60, 30, 0.20, 0.17, 8));
        m.put("Other",      new WeaponCombatParams(3.0, 1.0, "Root_RPGMobs_Attack_Melee", false,
                "Sword", List.of("SwingLeft", "SwingRight", "SwingDown"),
                "SFX_Sword_T1_Swing", "SFX_Sword_T2_Impact", "Medium_Default", "Impact_Sword_Basic",
                2.8, 60, 30, 0.20, 0.17, 8));
        m.put("Shortbows",  new WeaponCombatParams(18.0, 1.1, "", true));
        m.put("Crossbows",  new WeaponCombatParams(22.0, 0.8, "", true));
        m.put("Guns",       new WeaponCombatParams(25.0, 0.7, "", true));
        m.put("Staves",     new WeaponCombatParams(16.0, 0.9, "", true));
        m.put("Spellbooks", new WeaponCombatParams(14.0, 1.0, "", true));
        return m;
    }

    public static final class EntityEffectConfig extends TieredAssetConfig {
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] amountMultiplierPerTier = new float[]{1f, 1f, 1f, 1f, 1f};
        public boolean infinite = true;

        @Override
        public AssetType namespace() {
            return AssetType.EFFECTS;
        }
    }

    public static final class DebugConfig {
        @Default
        @Cfg(group = "Debug", file = "core.yml", comment = "Enables debug mode.")
        public boolean isDebugModeEnabled = false;

        @Min(1.0)
        @Default
        @Cfg(group = "Debug", file = "core.yml", comment = "Debug interval for scanning NPC's that match the MobRules in seconds.")
        public int debugMobRuleScanIntervalSeconds = 5;
    }

    public static final class AbilitiesConfig {
        @Cfg(file = "abilities.yml", comment = "Ability configuration lives here. Say hello.")
        public Map<String, AbilityConfig> defaultAbilities = defaultAbilities();

        @Default
        @Min(0.0)
        @Max(30.0)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum seconds of global cooldown between abilities. After one ability completes, the mob waits a random time between min and max before using another.")
        public float globalCooldownMinSeconds = 2.0f;

        @Default
        @Min(0.0)
        @Max(30.0)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum seconds of global cooldown between abilities.")
        public float globalCooldownMaxSeconds = 4.0f;

    }

    private static List<String> buildDefaultLinkedKeysAllCategories(MobRuleCategory defaultTree) {
        List<String> keys = new ArrayList<>();
        for (MobRuleCategory child : defaultTree.children) {
            keys.add(MobRuleCategoryHelpers.toCategoryKey(child.name));
        }
        return keys;
    }

    private static List<String> buildDefaultLinkedKeysLivingHumanoids(MobRuleCategory defaultTree) {
        List<String> keys = new ArrayList<>();
        for (MobRuleCategory child : defaultTree.children) {
            String name = child.name;
            if ("Outlanders".equals(name) || "Goblins".equals(name) || "Trorks".equals(name)) {
                keys.add(MobRuleCategoryHelpers.toCategoryKey(name));
            }
        }
        return keys;
    }

    private static List<String> buildDefaultSummonLinkedKeys(MobRuleCategory defaultTree) {
        List<String> keys = new ArrayList<>();
        for (MobRuleCategory child : defaultTree.children) {
            if ("Skeletons".equals(child.name) || "Zombies".equals(child.name)
                    || "Wraiths".equals(child.name) || "Void".equals(child.name)) {
                keys.add(MobRuleCategoryHelpers.toCategoryKey(child.name));
            }
        }
        keys.add("Goblin_Duke");
        keys.add("Trork_Shaman");
        return keys;
    }

    private static Map<String, AbilityConfig> defaultAbilities() {
        Map<String, AbilityConfig> m = new LinkedHashMap<>();

        MobRuleCategory defaultTree = defaultCategoryTree();

        ChargeLeapAbilityConfig chargeLeap = new ChargeLeapAbilityConfig();
        chargeLeap.gate.allowedWeaponCategories = WEAPON_CATS_MELEE;
        chargeLeap.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        chargeLeap.excludeLinkedMobRuleKeys = new ArrayList<>(List.of(
                "Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void",
                "Skeleton_Burnt_Alchemist", "Skeleton_Burnt_Alchemist_Patrol", "Skeleton_Burnt_Alchemist_Wander"));

        chargeLeap.isEnabled = true;
        chargeLeap.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        chargeLeap.chancePerTier = new float[]{0f, 0f, 0f, 0.50f, 1.00f};
        chargeLeap.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 16f, 20f};

        chargeLeap.minRange = 9.0f;
        chargeLeap.maxRange = 30.0f;
        chargeLeap.faceTarget = true;

        chargeLeap.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Root.template.json"
        );
        chargeLeap.templates.add(ChargeLeapAbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Entry.template.json"
        );
        chargeLeap.templates.add(ChargeLeapAbilityConfig.TEMPLATE_DAMAGE_INTERACTION,
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Damage.template.json"
        );

        chargeLeap.templates.add("rootSwords",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Swords_Root.template.json");
        chargeLeap.templates.add("entrySwords",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Swords_Entry.template.json");
        chargeLeap.templates.add("rootLongswords",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Longswords_Root.template.json");
        chargeLeap.templates.add("entryLongswords",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Longswords_Entry.template.json");
        chargeLeap.templates.add("rootDaggers",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Daggers_Root.template.json");
        chargeLeap.templates.add("entryDaggers",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Daggers_Entry.template.json");
        chargeLeap.templates.add("rootBattleaxes",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Battleaxes_Root.template.json");
        chargeLeap.templates.add("entryBattleaxes",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Battleaxes_Entry.template.json");
        chargeLeap.templates.add("rootAxes",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Axes_Root.template.json");
        chargeLeap.templates.add("entryAxes",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Axes_Entry.template.json");
        chargeLeap.templates.add("rootMaces",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Maces_Root.template.json");
        chargeLeap.templates.add("entryMaces",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Maces_Entry.template.json");
        chargeLeap.templates.add("rootClubs",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Clubs_Root.template.json");
        chargeLeap.templates.add("entryClubs",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Clubs_Entry.template.json");
        chargeLeap.templates.add("rootSpears",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Spears_Root.template.json");
        chargeLeap.templates.add("entrySpears",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_Spears_Entry.template.json");
        chargeLeap.templates.add("rootClubsFlail",
                                 "Item/RootInteractions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_ClubsFlail_Root.template.json");
        chargeLeap.templates.add("entryClubsFlail",
                                 "Item/Interactions/NPCs/RPGMobs/ChargeLeap/RPGMobs_Ability_ChargeLeap_ClubsFlail_Entry.template.json");

        chargeLeap.slamRangePerTier = new float[]{0f, 0f, 0f, 3f, 4f};
        chargeLeap.slamBaseDamagePerTier = new int[]{0, 0, 0, 20, 30};

        chargeLeap.applyForcePerTier = new float[]{0f, 0f, 0f, 530f, 530f};
        chargeLeap.knockbackLiftPerTier = new float[]{0f, 0f, 0f, 3f, 6f};
        chargeLeap.knockbackPushAwayPerTier = new float[]{0f, 0f, 0f, -3f, -6f};
        chargeLeap.knockbackForcePerTier = new float[]{0f, 0f, 0f, 20f, 26f};

        m.put(AbilityIds.CHARGE_LEAP, chargeLeap);

        HealLeapAbilityConfig healLeap = new HealLeapAbilityConfig();
        healLeap.linkedMobRuleKeys = buildDefaultLinkedKeysLivingHumanoids(defaultTree);
        healLeap.excludeLinkedMobRuleKeys = new ArrayList<>(List.of("Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void"));
        healLeap.isEnabled = true;
        healLeap.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        healLeap.chancePerTier = new float[]{0f, 0f, 0f, 1.00f, 1.00f};
        healLeap.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 15f, 15f};

        healLeap.minHealthTriggerPercent = 0.50f;
        healLeap.maxHealthTriggerPercent = 0.50f;
        healLeap.instantHealChance = 1.00f;
        healLeap.instantHealAmountPerTier = new float[]{0f, 0f, 0f, 25f, 40f};
        healLeap.npcDrinkDurationSeconds = 3.0f;

        healLeap.applyForcePerTier = new float[]{0f, 0f, 0f, 830f, 930f};

        healLeap.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                               "Item/RootInteractions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealLeap_Root.template.json"
        );
        healLeap.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                               "Item/Interactions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealLeap_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_ROOT_INTERACTION_CANCEL,
                               "Item/RootInteractions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealLeap_Cancel_Root.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_ENTRY_INTERACTION_CANCEL,
                               "Item/Interactions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealLeap_Cancel_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_EFFECT_INSTANT_HEAL,
                               "Entity/Effects/RPGMobs/RPGMobs_Effect_InstantHeal.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_POTION_ROOT,
                               "Item/RootInteractions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealPotion_Root.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_POTION_ENTRY,
                               "Item/Interactions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_HealPotion_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_STANDING_HEAL_ROOT,
                               "Item/RootInteractions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_StandingHeal_Root.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_STANDING_HEAL_ENTRY,
                               "Item/Interactions/NPCs/RPGMobs/HealLeap/RPGMobs_Ability_StandingHeal_Entry.template.json"
        );
        healLeap.templates.add(HealLeapAbilityConfig.TEMPLATE_EFFECT_STANDING_HEAL,
                               "Entity/Effects/RPGMobs/RPGMobs_Effect_StandingHeal.template.json"
        );

        m.put(AbilityIds.HEAL_LEAP, healLeap);

        SummonAbilityConfig undeadSummon = new SummonAbilityConfig();
        undeadSummon.isEnabled = true;
        undeadSummon.isEnabledPerTier = new boolean[]{false, false, false, true, true};
        undeadSummon.chancePerTier = new float[]{0f, 0f, 0f, 0.50f, 1.00f};
        undeadSummon.cooldownSecondsPerTier = new float[]{0f, 0f, 0f, 25f, 25f};
        undeadSummon.linkedMobRuleKeys = buildDefaultSummonLinkedKeys(defaultTree);
        undeadSummon.excludeLinkedMobRuleKeys = new ArrayList<>(List.of(
                "Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void",
                "Skeleton_Burnt_Alchemist", "Skeleton_Burnt_Alchemist_Patrol", "Skeleton_Burnt_Alchemist_Wander"));

        undeadSummon.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                                   "Item/RootInteractions/NPCs/RPGMobs/UndeadSummon/RPGMobs_Ability_UndeadSummon_Root.template.json"
        );
        undeadSummon.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                                   "Item/Interactions/NPCs/RPGMobs/UndeadSummon/RPGMobs_Ability_UndeadSummon_Entry.template.json"
        );
        undeadSummon.templates.add("rootV2",
                                   "Item/RootInteractions/NPCs/RPGMobs/UndeadSummon/RPGMobs_Ability_UndeadSummon_V2_Root.template.json"
        );
        undeadSummon.templates.add("entryV2",
                                   "Item/Interactions/NPCs/RPGMobs/UndeadSummon/RPGMobs_Ability_UndeadSummon_V2_Entry.template.json"
        );
        undeadSummon.templates.add(SummonAbilityConfig.TEMPLATE_SUMMON_MARKER,
                                   "NPC/Spawn/Markers/RPGMobs/RPGMobs_UndeadBow_Summon_Marker.template.json"
        );
        undeadSummon.templates.add(SummonAbilityConfig.TEMPLATE_SUMMON_RISE_ROOT,
                                   "Item/RootInteractions/NPCs/RPGMobs/SummonRise/RPGMobs_SummonRise_Root.template.json"
        );
        undeadSummon.templates.add(SummonAbilityConfig.TEMPLATE_SUMMON_RISE_ENTRY,
                                   "Item/Interactions/NPCs/RPGMobs/SummonRise/RPGMobs_SummonRise_Entry.template.json"
        );

        m.put(AbilityIds.SUMMON_UNDEAD, undeadSummon);

        DodgeRollAbilityConfig dodgeRoll = new DodgeRollAbilityConfig();
        dodgeRoll.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        dodgeRoll.excludeLinkedMobRuleKeys = new ArrayList<>(List.of(
                "Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void"));
        dodgeRoll.isEnabled = true;
        dodgeRoll.isEnabledPerTier = new boolean[]{true, true, true, true, true};
        dodgeRoll.chancePerTier = new float[]{0.30f, 0.40f, 0.50f, 1.00f, 1.00f};
        dodgeRoll.cooldownSecondsPerTier = new float[]{12f, 10f, 8f, 6f, 5f};

        dodgeRoll.templates.add("rootBack",
                                "Item/RootInteractions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Back_Root.template.json");
        dodgeRoll.templates.add("entryBack",
                                "Item/Interactions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Back_Entry.template.json");
        dodgeRoll.templates.add("rootLeft",
                                "Item/RootInteractions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Left_Root.template.json");
        dodgeRoll.templates.add("entryLeft",
                                "Item/Interactions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Left_Entry.template.json");
        dodgeRoll.templates.add("rootRight",
                                "Item/RootInteractions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Right_Root.template.json");
        dodgeRoll.templates.add("entryRight",
                                "Item/Interactions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Right_Entry.template.json");
        dodgeRoll.templates.add("rootForward",
                                "Item/RootInteractions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Forward_Root.template.json");
        dodgeRoll.templates.add("entryForward",
                                "Item/Interactions/NPCs/RPGMobs/DodgeRoll/RPGMobs_Ability_DodgeRoll_Forward_Entry.template.json");
        dodgeRoll.templates.add(DodgeRollAbilityConfig.TEMPLATE_EFFECT_INVULNERABILITY,
                                "Entity/Effects/RPGMobs/RPGMobs_Effect_DodgeRoll_Invulnerability.template.json");

        m.put(AbilityIds.DODGE_ROLL, dodgeRoll);

        var defaultExcludes = new ArrayList<>(List.of(
                "Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void"));

        MultiSlashAbilityConfig msShort = new MultiSlashAbilityConfig();
        msShort.gate.allowedWeaponCategories = WEAPON_CATS_MELEE;
        msShort.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        msShort.excludeLinkedMobRuleKeys = new ArrayList<>(defaultExcludes);
        msShort.isEnabled = true;
        msShort.isEnabledPerTier = new boolean[]{true, true, true, true, true};
        msShort.chancePerTier = new float[]{0.40f, 0.50f, 0.60f, 0.80f, 1.00f};
        msShort.cooldownSecondsPerTier = new float[]{8f, 7f, 6f, 5f, 4f};
        msShort.slashTriggerChancePerTier = new float[]{0.15f, 0.20f, 0.25f, 0.30f, 0.35f};
        msShort.baseDamagePerHitPerTier = new int[]{2, 3, 4, 6, 8};
        msShort.forwardDriftForcePerTier = new float[]{0f, 0f, 2f, 3f, 4f};
        msShort.knockbackForcePerTier = new float[]{0f, 2f, 3f, 4f, 5f};
        msShort.meleeRange = 3.5f;

        msShort.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION,
                "Item/Interactions/NPCs/RPGMobs/MultiSlashShort/RPGMobs_Ability_MultiSlashShort_Damage.template.json");
        MultiSlashAbilityConfig.registerVariantTemplates(msShort, "MultiSlashShort", 3);
        msShort.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION + "ClubsFlail",
                "Item/Interactions/NPCs/RPGMobs/MultiSlashShort/RPGMobs_Ability_MultiSlashShort_Damage_ClubsFlail.template.json");
        for (int v = 1; v <= 3; v++) {
            msShort.templates.add("rootClubsFlailV" + v,
                    "Item/RootInteractions/NPCs/RPGMobs/MultiSlashShort/RPGMobs_Ability_MultiSlashShort_ClubsFlail_V" + v + "_Root.template.json");
            msShort.templates.add("entryClubsFlailV" + v,
                    "Item/Interactions/NPCs/RPGMobs/MultiSlashShort/RPGMobs_Ability_MultiSlashShort_ClubsFlail_V" + v + "_Entry.template.json");
        }

        msShort.variantConfigs.put(Constants.VARIANT_SWORDS, new MultiSlashVariantConfig(
                new float[]{0.15f, 0.20f, 0.25f, 0.30f, 0.35f}, new float[]{8f, 7f, 6f, 5f, 4f},
                new int[]{2, 3, 4, 6, 8}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 2f, 3f, 4f, 5f}, 3.5f));
        msShort.variantConfigs.put(Constants.VARIANT_LONGSWORDS, new MultiSlashVariantConfig(
                new float[]{0.12f, 0.18f, 0.22f, 0.28f, 0.32f}, new float[]{9f, 8f, 7f, 6f, 5f},
                new int[]{3, 4, 5, 7, 10}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 3f, 4f, 5f, 7f}, 4.0f));
        msShort.variantConfigs.put(Constants.VARIANT_DAGGERS, new MultiSlashVariantConfig(
                new float[]{0.20f, 0.25f, 0.30f, 0.35f, 0.40f}, new float[]{6f, 5f, 5f, 4f, 3f},
                new int[]{1, 2, 3, 4, 6}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 1f, 2f, 3f, 4f}, 3.0f));
        msShort.variantConfigs.put(Constants.VARIANT_BATTLEAXES, new MultiSlashVariantConfig(
                new float[]{0.08f, 0.12f, 0.16f, 0.20f, 0.25f}, new float[]{12f, 10f, 9f, 8f, 7f},
                new int[]{4, 6, 8, 10, 14}, new float[]{0f, 0f, 3f, 4f, 5f}, new float[]{0f, 4f, 6f, 8f, 10f}, 4.0f));
        msShort.variantConfigs.put(Constants.VARIANT_AXES, new MultiSlashVariantConfig(
                new float[]{0.12f, 0.18f, 0.22f, 0.28f, 0.32f}, new float[]{9f, 8f, 7f, 6f, 5f},
                new int[]{3, 4, 5, 7, 10}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 3f, 4f, 5f, 7f}, 3.5f));
        msShort.variantConfigs.put(Constants.VARIANT_MACES, new MultiSlashVariantConfig(
                new float[]{0.10f, 0.15f, 0.20f, 0.25f, 0.30f}, new float[]{10f, 9f, 8f, 7f, 6f},
                new int[]{3, 5, 7, 10, 14}, new float[]{0f, 0f, 3f, 4f, 5f}, new float[]{0f, 4f, 5f, 7f, 10f}, 3.5f));
        msShort.variantConfigs.put(Constants.VARIANT_CLUBS, new MultiSlashVariantConfig(
                new float[]{0.12f, 0.18f, 0.22f, 0.26f, 0.32f}, new float[]{9f, 8f, 7f, 6f, 5f},
                new int[]{2, 3, 5, 7, 10}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 3f, 5f, 7f, 9f}, 3.5f));
        msShort.variantConfigs.put(Constants.VARIANT_SPEARS, new MultiSlashVariantConfig(
                new float[]{0.18f, 0.22f, 0.28f, 0.32f, 0.38f}, new float[]{7f, 6f, 5f, 5f, 4f},
                new int[]{2, 3, 4, 5, 7}, new float[]{0f, 0f, 2f, 3f, 4f}, new float[]{0f, 2f, 3f, 4f, 6f}, 4.5f));

        m.put(AbilityIds.MULTI_SLASH_SHORT, msShort);

        MultiSlashAbilityConfig msMedium = new MultiSlashAbilityConfig();
        msMedium.gate.allowedWeaponCategories = WEAPON_CATS_MELEE;
        msMedium.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        msMedium.excludeLinkedMobRuleKeys = new ArrayList<>(defaultExcludes);
        msMedium.isEnabled = true;
        msMedium.isEnabledPerTier = new boolean[]{false, true, true, true, true};
        msMedium.chancePerTier = new float[]{0f, 0.30f, 0.40f, 0.60f, 1.00f};
        msMedium.cooldownSecondsPerTier = new float[]{0f, 14f, 12f, 10f, 8f};
        msMedium.slashTriggerChancePerTier = new float[]{0f, 0.10f, 0.14f, 0.18f, 0.25f};
        msMedium.baseDamagePerHitPerTier = new int[]{0, 4, 6, 9, 14};
        msMedium.forwardDriftForcePerTier = new float[]{0f, 3f, 4f, 5f, 6f};
        msMedium.knockbackForcePerTier = new float[]{0f, 4f, 6f, 8f, 10f};
        msMedium.meleeRange = 4.0f;

        msMedium.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION,
                "Item/Interactions/NPCs/RPGMobs/MultiSlashMedium/RPGMobs_Ability_MultiSlashMedium_Damage.template.json");
        MultiSlashAbilityConfig.registerVariantTemplates(msMedium, "MultiSlashMedium", 2);
        msMedium.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION + "ClubsFlail",
                "Item/Interactions/NPCs/RPGMobs/MultiSlashMedium/RPGMobs_Ability_MultiSlashMedium_Damage_ClubsFlail.template.json");
        for (int v = 1; v <= 2; v++) {
            msMedium.templates.add("rootClubsFlailV" + v,
                    "Item/RootInteractions/NPCs/RPGMobs/MultiSlashMedium/RPGMobs_Ability_MultiSlashMedium_ClubsFlail_V" + v + "_Root.template.json");
            msMedium.templates.add("entryClubsFlailV" + v,
                    "Item/Interactions/NPCs/RPGMobs/MultiSlashMedium/RPGMobs_Ability_MultiSlashMedium_ClubsFlail_V" + v + "_Entry.template.json");
        }

        msMedium.variantConfigs.put(Constants.VARIANT_SWORDS, new MultiSlashVariantConfig(
                new float[]{0f, 0.10f, 0.14f, 0.18f, 0.25f}, new float[]{0f, 14f, 12f, 10f, 8f},
                new int[]{0, 4, 6, 9, 14}, new float[]{0f, 3f, 4f, 5f, 6f}, new float[]{0f, 4f, 6f, 8f, 10f}, 4.0f));
        msMedium.variantConfigs.put(Constants.VARIANT_LONGSWORDS, new MultiSlashVariantConfig(
                new float[]{0f, 0.08f, 0.12f, 0.16f, 0.22f}, new float[]{0f, 16f, 14f, 12f, 10f},
                new int[]{0, 5, 8, 12, 16}, new float[]{0f, 3f, 4f, 6f, 8f}, new float[]{0f, 5f, 7f, 9f, 12f}, 4.5f));
        msMedium.variantConfigs.put(Constants.VARIANT_DAGGERS, new MultiSlashVariantConfig(
                new float[]{0f, 0.14f, 0.18f, 0.24f, 0.30f}, new float[]{0f, 10f, 9f, 8f, 6f},
                new int[]{0, 3, 4, 6, 10}, new float[]{0f, 2f, 3f, 4f, 5f}, new float[]{0f, 2f, 3f, 4f, 6f}, 3.5f));
        msMedium.variantConfigs.put(Constants.VARIANT_BATTLEAXES, new MultiSlashVariantConfig(
                new float[]{0f, 0.06f, 0.08f, 0.12f, 0.18f}, new float[]{0f, 20f, 18f, 16f, 14f},
                new int[]{0, 8, 12, 16, 22}, new float[]{0f, 4f, 5f, 7f, 9f}, new float[]{0f, 6f, 8f, 12f, 16f}, 4.5f));
        msMedium.variantConfigs.put(Constants.VARIANT_AXES, new MultiSlashVariantConfig(
                new float[]{0f, 0.10f, 0.14f, 0.18f, 0.24f}, new float[]{0f, 15f, 13f, 11f, 9f},
                new int[]{0, 5, 7, 10, 14}, new float[]{0f, 3f, 4f, 5f, 7f}, new float[]{0f, 4f, 6f, 8f, 11f}, 4.0f));
        msMedium.variantConfigs.put(Constants.VARIANT_MACES, new MultiSlashVariantConfig(
                new float[]{0f, 0.06f, 0.10f, 0.14f, 0.20f}, new float[]{0f, 18f, 16f, 14f, 12f},
                new int[]{0, 6, 9, 13, 18}, new float[]{0f, 4f, 5f, 6f, 8f}, new float[]{0f, 6f, 8f, 10f, 14f}, 4.0f));
        msMedium.variantConfigs.put(Constants.VARIANT_CLUBS, new MultiSlashVariantConfig(
                new float[]{0f, 0.08f, 0.12f, 0.16f, 0.22f}, new float[]{0f, 16f, 14f, 12f, 10f},
                new int[]{0, 4, 7, 10, 14}, new float[]{0f, 3f, 4f, 5f, 7f}, new float[]{0f, 5f, 7f, 10f, 13f}, 3.8f));
        msMedium.variantConfigs.put(Constants.VARIANT_SPEARS, new MultiSlashVariantConfig(
                new float[]{0f, 0.12f, 0.16f, 0.22f, 0.28f}, new float[]{0f, 13f, 11f, 9f, 7f},
                new int[]{0, 4, 6, 8, 12}, new float[]{0f, 3f, 4f, 5f, 7f}, new float[]{0f, 3f, 5f, 7f, 9f}, 5.0f));

        m.put(AbilityIds.MULTI_SLASH_MEDIUM, msMedium);

        MultiSlashAbilityConfig msLong = new MultiSlashAbilityConfig();
        msLong.gate.allowedWeaponCategories = WEAPON_CATS_MELEE;
        msLong.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        msLong.excludeLinkedMobRuleKeys = new ArrayList<>(defaultExcludes);
        msLong.isEnabled = true;
        msLong.isEnabledPerTier = new boolean[]{false, false, true, true, true};
        msLong.chancePerTier = new float[]{0f, 0f, 0.30f, 0.50f, 1.00f};
        msLong.cooldownSecondsPerTier = new float[]{0f, 0f, 22f, 20f, 18f};
        msLong.slashTriggerChancePerTier = new float[]{0f, 0f, 0.08f, 0.12f, 0.18f};
        msLong.baseDamagePerHitPerTier = new int[]{0, 0, 6, 10, 16};
        msLong.forwardDriftForcePerTier = new float[]{0f, 0f, 4f, 5f, 6f};
        msLong.knockbackForcePerTier = new float[]{0f, 0f, 6f, 8f, 10f};
        msLong.meleeRange = 4.0f;

        msLong.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION,
                "Item/Interactions/NPCs/RPGMobs/MultiSlashLong/RPGMobs_Ability_MultiSlashLong_Damage.template.json");
        MultiSlashAbilityConfig.registerVariantTemplates(msLong, "MultiSlashLong", 1);
        msLong.templates.add(MultiSlashAbilityConfig.TEMPLATE_DAMAGE_INTERACTION + "ClubsFlail",
                "Item/Interactions/NPCs/RPGMobs/MultiSlashLong/RPGMobs_Ability_MultiSlashLong_Damage_ClubsFlail.template.json");
        msLong.templates.add("rootClubsFlail",
                "Item/RootInteractions/NPCs/RPGMobs/MultiSlashLong/RPGMobs_Ability_MultiSlashLong_ClubsFlail_Root.template.json");
        msLong.templates.add("entryClubsFlail",
                "Item/Interactions/NPCs/RPGMobs/MultiSlashLong/RPGMobs_Ability_MultiSlashLong_ClubsFlail_Entry.template.json");

        msLong.variantConfigs.put(Constants.VARIANT_SWORDS, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.08f, 0.12f, 0.18f}, new float[]{0f, 0f, 22f, 20f, 18f},
                new int[]{0, 0, 6, 10, 16}, new float[]{0f, 0f, 4f, 5f, 6f}, new float[]{0f, 0f, 6f, 8f, 10f}, 4.0f));
        msLong.variantConfigs.put(Constants.VARIANT_LONGSWORDS, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.07f, 0.10f, 0.16f}, new float[]{0f, 0f, 24f, 22f, 20f},
                new int[]{0, 0, 8, 12, 18}, new float[]{0f, 0f, 4f, 6f, 8f}, new float[]{0f, 0f, 7f, 9f, 12f}, 4.5f));
        msLong.variantConfigs.put(Constants.VARIANT_DAGGERS, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.10f, 0.14f, 0.22f}, new float[]{0f, 0f, 18f, 16f, 14f},
                new int[]{0, 0, 4, 7, 12}, new float[]{0f, 0f, 3f, 4f, 5f}, new float[]{0f, 0f, 3f, 5f, 8f}, 3.5f));
        msLong.variantConfigs.put(Constants.VARIANT_BATTLEAXES, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.05f, 0.08f, 0.14f}, new float[]{0f, 0f, 28f, 25f, 22f},
                new int[]{0, 0, 10, 16, 24}, new float[]{0f, 0f, 5f, 7f, 9f}, new float[]{0f, 0f, 8f, 12f, 16f}, 4.5f));
        msLong.variantConfigs.put(Constants.VARIANT_AXES, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.08f, 0.12f, 0.18f}, new float[]{0f, 0f, 22f, 20f, 18f},
                new int[]{0, 0, 7, 10, 15}, new float[]{0f, 0f, 4f, 5f, 7f}, new float[]{0f, 0f, 6f, 8f, 11f}, 4.0f));
        msLong.variantConfigs.put(Constants.VARIANT_MACES, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.06f, 0.10f, 0.16f}, new float[]{0f, 0f, 26f, 22f, 20f},
                new int[]{0, 0, 8, 13, 20}, new float[]{0f, 0f, 5f, 6f, 8f}, new float[]{0f, 0f, 8f, 10f, 14f}, 4.0f));
        msLong.variantConfigs.put(Constants.VARIANT_CLUBS, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.08f, 0.12f, 0.18f}, new float[]{0f, 0f, 24f, 22f, 20f},
                new int[]{0, 0, 6, 10, 16}, new float[]{0f, 0f, 4f, 5f, 7f}, new float[]{0f, 0f, 7f, 10f, 13f}, 3.8f));
        msLong.variantConfigs.put(Constants.VARIANT_SPEARS, new MultiSlashVariantConfig(
                new float[]{0f, 0f, 0.10f, 0.14f, 0.20f}, new float[]{0f, 0f, 20f, 18f, 16f},
                new int[]{0, 0, 5, 8, 12}, new float[]{0f, 0f, 4f, 5f, 7f}, new float[]{0f, 0f, 5f, 7f, 9f}, 5.0f));

        m.put(AbilityIds.MULTI_SLASH_LONG, msLong);

        EnrageAbilityConfig enrage = new EnrageAbilityConfig();
        enrage.gate.allowedWeaponCategories = WEAPON_CATS_MELEE;
        enrage.linkedMobRuleKeys = buildDefaultLinkedKeysLivingHumanoids(defaultTree);
        enrage.excludeLinkedMobRuleKeys = new ArrayList<>();
        enrage.isEnabled = true;
        enrage.isEnabledPerTier = new boolean[]{false, true, true, true, true};
        enrage.chancePerTier = new float[]{0f, 0.30f, 0.50f, 0.70f, 1.00f};
        enrage.cooldownSecondsPerTier = new float[]{0f, 60f, 60f, 60f, 60f};

        enrage.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                             "Item/RootInteractions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V1_Root.template.json");
        enrage.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V1_Entry.template.json");
        enrage.templates.add("rootV1",
                             "Item/RootInteractions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V1_Root.template.json");
        enrage.templates.add("entryV1",
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V1_Entry.template.json");
        enrage.templates.add("rootV2",
                             "Item/RootInteractions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V2_Root.template.json");
        enrage.templates.add("entryV2",
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V2_Entry.template.json");
        enrage.templates.add("rootV3",
                             "Item/RootInteractions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V3_Root.template.json");
        enrage.templates.add("entryV3",
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_V3_Entry.template.json");
        enrage.templates.add(EnrageAbilityConfig.TEMPLATE_LIGHT_DAMAGE_INTERACTION,
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_LightDamage.template.json");
        enrage.templates.add(EnrageAbilityConfig.TEMPLATE_HEAVY_DAMAGE_INTERACTION,
                             "Item/Interactions/NPCs/RPGMobs/Enrage/RPGMobs_Ability_Enrage_HeavyDamage.template.json");
        enrage.templates.add(EnrageAbilityConfig.TEMPLATE_EFFECT_RED_EYES,
                             "Entity/Effects/RPGMobs/RPGMobs_Effect_Enrage_RedEyes.template.json");
        m.put(AbilityIds.ENRAGE, enrage);

        VolleyAbilityConfig volley = new VolleyAbilityConfig();
        volley.gate.allowedWeaponCategories = new ArrayList<>(List.of(CATEGORY_PREFIX+"Shortbows", CATEGORY_PREFIX+"Crossbows", CATEGORY_PREFIX+"Guns"));
        volley.linkedMobRuleKeys = buildDefaultLinkedKeysAllCategories(defaultTree);
        volley.excludeLinkedMobRuleKeys = new ArrayList<>(List.of(
                "Skeleton_Incandescent_Head", "Crawler_Void", "Eye_Void", "Spectre_Void"));
        volley.isEnabled = true;
        volley.isEnabledPerTier = new boolean[]{false, false, true, true, true};
        volley.chancePerTier = new float[]{0f, 0f, 0.40f, 0.60f, 1.00f};
        volley.cooldownSecondsPerTier = new float[]{0f, 0f, 18f, 15f, 12f};

        volley.templates.add(AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                             "Item/RootInteractions/NPCs/RPGMobs/Volley/RPGMobs_Ability_Volley_Root.template.json"
        );
        volley.templates.add(AbilityConfig.TEMPLATE_ENTRY_INTERACTION,
                             "Item/Interactions/NPCs/RPGMobs/Volley/RPGMobs_Ability_Volley_Entry.template.json"
        );
        volley.templates.add(VolleyAbilityConfig.TEMPLATE_PROJECTILE_DEFINITION,
                             "Projectiles/NPCs/RPGMobs/RPGMobs_Volley_Arrow.json"
        );

        m.put(AbilityIds.VOLLEY, volley);

        return m;
    }

    public static class AbilityConfig extends TieredAssetConfig {
        public static final String TEMPLATE_ROOT_INTERACTION = "rootInteraction";
        public static final String TEMPLATE_ENTRY_INTERACTION = "entryInteraction";

        public AbilityGate gate = new AbilityGate();
        public List<String> linkedMobRuleKeys = new ArrayList<>();
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Mob rule keys excluded from this ability even if they are inside a linked category.")
        public List<String> excludeLinkedMobRuleKeys = new ArrayList<>();

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance per tier for this ability to be active on an elite (roll happens once on spawn).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] chancePerTier = {1f, 1f, 1f, 1f, 1f};
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Cooldown per tier (seconds).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] cooldownSecondsPerTier = {10f, 10f, 10f, 10f, 10f};

        @Override
        public AssetType namespace() {
            return AssetType.ABILITIES;
        }
    }

    public static final class ChargeLeapAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_DAMAGE_INTERACTION = "damageInteraction";

        public float minRange = 0f;
        public float maxRange = 0f;
        public boolean faceTarget = false;

        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] slamRangePerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] slamBaseDamagePerTier = {0, 0, 0, 0, 0};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] applyForcePerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackLiftPerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackPushAwayPerTier = {0f, 0f, 0f, 0f, 0f};
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackForcePerTier = {0f, 0f, 0f, 0f, 0f};
    }

    public static final class HealLeapAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_ROOT_INTERACTION_CANCEL = "rootInteractionCancel";
        public static final String TEMPLATE_ENTRY_INTERACTION_CANCEL = "entryInteractionCancel";
        public static final String TEMPLATE_EFFECT_INSTANT_HEAL = "effectInstantHeal";
        public static final String TEMPLATE_POTION_ROOT = "potionRootInteraction";
        public static final String TEMPLATE_POTION_ENTRY = "potionEntryInteraction";
        public static final String TEMPLATE_STANDING_HEAL_ROOT = "rootStandingHeal";
        public static final String TEMPLATE_STANDING_HEAL_ENTRY = "entryStandingHeal";
        public static final String TEMPLATE_EFFECT_STANDING_HEAL = "effectStandingHeal";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum health percent at which the heal can trigger (rolled once per elite on spawn).")
        public float minHealthTriggerPercent = 0.5f;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum health percent at which the heal can trigger (rolled once per elite on spawn).")
        public float maxHealthTriggerPercent = 0.5f;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance to use instant heal instead of regeneration.")
        public float instantHealChance = 1.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Instant heal amount per tier (percent of max health).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] instantHealAmountPerTier = {0f, 0f, 0f, 25f, 25f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "NPC potion drinking duration in seconds.")
        public float npcDrinkDurationSeconds = 4.0f;

        @Default
        @Min(0.0f)
        @Max(30.0f)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum distance from target to use stationary heal (potion only, no leap). During retreat, if target is farther than this, NPC stands still and drinks. Set to 0 to disable retreat healing.")
        public float retreatHealMinDistance = 10.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Item id shown in NPC hand while drinking.")
        public String npcDrinkItemId = "Potion_Health_Greater";

        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] applyForcePerTier = {0f, 0f, 0f, 0f, 0f};

        @Default
        @Min(1)
        @Max(20)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Number of hits required to interrupt the Heal Leap ability while the NPC is drinking.")
        public int interruptHitCount = 3;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Fraction of max health healed per tier when using standing heal variant (0.0 to 1.0).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] standingHealPercentPerTier = {0.0f, 0.0f, 0.15f, 0.20f, 0.25f};

        @Default
        @Min(0.0f)
        @Max(30.0f)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum distance to target in blocks to trigger standing heal when not retreating/recovering.")
        public float standingHealMinDistance = 8.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum health percentage to trigger standing heal variant (0.0 to 1.0).")
        public float standingHealMaxHealthTriggerPercent = 0.60f;
    }

    public static final class SummonAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_SUMMON_MARKER = "summonMarker";
        public static final String TEMPLATE_SUMMON_RISE_ROOT = "summonRiseRoot";
        public static final String TEMPLATE_SUMMON_RISE_ENTRY = "summonRiseEntry";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum number of active summoned minions per summoner (0 disables summoning). Clamped to 0..50.")
        public int maxAliveMinionsPerSummoner = DEFAULT_MAX_ALIVE_MINIONS_PER_SUMMONER;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Role identifiers used to pick which minions get summoned. First match (role name contains this text) wins. 'default' is used if none match.")
        public List<String> roleIdentifiers = new ArrayList<>(List.of(
                "Skeleton_Frost_Mage",
                "Skeleton_Sand_Mage",
                "Skeleton_Incandescent_Mage",
                "Skeleton_Mage",
                "Skeleton_Frost_Archmage",
                "Skeleton_Sand_Archmage",
                "Skeleton_Archmage",
                "Skeleton_Frost",
                "Skeleton_Sand",
                "Skeleton_Burnt",
                "Skeleton_Incandescent",
                "Skeleton_Pirate",
                "Skeleton",
                "Zombie_Burnt",
                "Zombie_Frost",
                "Zombie_Sand",
                "Zombie",
                "Goblin_",
                "Trork_"
        ));
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for role-matched skeleton archers in the summon pool.")
        public double skeletonArcherWeight = 100;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for extra zombies added to all pools (0 = disabled, only zombie summoners get zombies from primary entries).")
        public double zombieWeight = 50;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for wraiths in the skeleton summon pool.")
        public double wraithWeight = 10;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight for aberrant zombies in the zombie summon pool (~20% chance).")
        public double aberrantWeight = 25;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Roles excluded from the auto-generated summon pool (prevents summoners from summoning themselves).")
        public List<String> excludeFromSummonPool = new ArrayList<>(List.of("Trork_Shaman", "Goblin_Duke", "Zombie_Aberrant", "Zombie_Aberrant_Big", "Zombie_Aberrant_Small"));

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Optional explicit spawn marker entries (advanced). If empty, RPGMobs builds this automatically from mob rules.")
        public List<SummonMarkerEntry> spawnMarkerEntries = new ArrayList<>();
        @YamlIgnore
        public String spawnMarkerEntriesJson = "[]";

        @YamlIgnore
        public Map<String, List<SummonMarkerEntry>> spawnMarkerEntriesByRole = new LinkedHashMap<>();

        @Default
        @Min(1)
        @Max(50)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum number of minions to summon per cast.")
        public int summonMinCount = 2;

        @Default
        @Min(1)
        @Max(50)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum number of minions to summon per cast.")
        public int summonMaxCount = 5;

        @Default
        @Min(1.0)
        @Max(30.0)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Radius (in blocks) around the summoner where minions spawn.")
        public double summonSpawnRadius = 6.0;

        @Default
        @Min(0)
        @Max(4)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum tier index (0-based, 0=T1 through 4=T5) for summoned minions.")
        public int minionMinTier = 0;

        @Default
        @Min(0)
        @Max(4)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum tier index (0-based, 0=T1 through 4=T5) for summoned minions.")
        public int minionMaxTier = 2;
    }

    public static final class DodgeRollAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_EFFECT_INVULNERABILITY = "effectInvulnerability";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Dodge chance per tier (0.0 to 1.0). Rolled on each DAMAGE_RECEIVED event.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] dodgeChancePerTier = {0.20f, 0.30f, 0.40f, 0.50f, 1.00f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Lateral dodge force applied when dodge triggers.")
        public float dodgeForce = 370.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Invulnerability duration (seconds) applied via effect during dodge.")
        public float invulnerabilityDuration = 0.9f;

        @Default
        @Min(0.0f)
        @Max(20.0f)
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Dodge chance multiplier when T4+ intelligence detects target is attacking or charging (preemptive dodge).")
        public float chargedAttackDodgeMultiplier = 2.5f;
    }

    public static final class MultiSlashVariantConfig {
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance per tick evaluation to trigger this variant (0.0 to 1.0).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] slashTriggerChancePerTier;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Cooldown per tier (seconds) for this variant.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] cooldownSecondsPerTier;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Base damage per hit per tier for this variant.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] baseDamagePerHitPerTier;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Forward drift force applied on the last hits per tier for this variant.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] forwardDriftForcePerTier;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Knockback force applied to hit targets per tier for this variant.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackForcePerTier;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum distance to target (blocks) for this variant to trigger.")
        public float meleeRange;

        public MultiSlashVariantConfig() {
            this.slashTriggerChancePerTier = new float[]{0f, 0.08f, 0.12f, 0.18f, 0.25f};
            this.cooldownSecondsPerTier = new float[]{15f, 12f, 10f, 8f, 6f};
            this.baseDamagePerHitPerTier = new int[]{0, 3, 5, 8, 12};
            this.forwardDriftForcePerTier = new float[]{0f, 3f, 4f, 5f, 6f};
            this.knockbackForcePerTier = new float[]{0f, 4f, 6f, 8f, 10f};
            this.meleeRange = 4.0f;
        }

        public MultiSlashVariantConfig(float[] triggerChance, float[] cooldown, int[] damage,
                                       float[] drift, float[] knockback, float range) {
            this.slashTriggerChancePerTier = triggerChance;
            this.cooldownSecondsPerTier = cooldown;
            this.baseDamagePerHitPerTier = damage;
            this.forwardDriftForcePerTier = drift;
            this.knockbackForcePerTier = knockback;
            this.meleeRange = range;
        }
    }

    public static final class MultiSlashAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_DAMAGE_INTERACTION = "damageInteraction";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Default trigger chance (used when no variant-specific override). Chance per tick evaluation to trigger Multi Slash (0.0 to 1.0).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] slashTriggerChancePerTier = {0f, 0.08f, 0.12f, 0.18f, 0.25f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Default max melee range (used when no variant-specific override).")
        public float meleeRange = 4.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Default base damage per hit (used when no variant-specific override).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] baseDamagePerHitPerTier = {0, 3, 5, 8, 12};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Default forward drift force (used when no variant-specific override).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] forwardDriftForcePerTier = {0f, 3f, 4f, 5f, 6f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Default knockback force (used when no variant-specific override).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] knockbackForcePerTier = {0f, 4f, 6f, 8f, 10f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Per-weapon-variant config overrides. Each key is a variant name (swords, daggers, battleaxes, axes, maces, spears, longswords, clubs).")
        public Map<String, MultiSlashVariantConfig> variantConfigs = new LinkedHashMap<>();

        public MultiSlashVariantConfig getVariantOrDefault(String variant) {
            if (variant != null && variantConfigs != null) {
                var vc = variantConfigs.get(variant);
                if (vc != null) return vc;
                if (Constants.VARIANT_CLUBS_FLAIL.equals(variant)) {
                    var clubsCfg = variantConfigs.get(Constants.VARIANT_CLUBS);
                    if (clubsCfg != null) return clubsCfg;
                }
            }
            var fallback = new MultiSlashVariantConfig();
            fallback.slashTriggerChancePerTier = slashTriggerChancePerTier;
            fallback.cooldownSecondsPerTier = cooldownSecondsPerTier;
            fallback.baseDamagePerHitPerTier = baseDamagePerHitPerTier;
            fallback.forwardDriftForcePerTier = forwardDriftForcePerTier;
            fallback.knockbackForcePerTier = knockbackForcePerTier;
            fallback.meleeRange = meleeRange;
            return fallback;
        }

        public static void registerVariantTemplates(MultiSlashAbilityConfig config, String abilityFilePrefix,
                                                     int variationCount) {
            String[] variants = {"Swords", "Longswords", "Daggers", "Battleaxes", "Axes", "Maces", "Clubs", "ClubsFlail", "Spears"};
            for (String cap : variants) {
                String lower = Character.toLowerCase(cap.charAt(0)) + cap.substring(1);
                String rootCap = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
                config.templates.add(TEMPLATE_DAMAGE_INTERACTION + rootCap,
                        "Item/Interactions/NPCs/RPGMobs/" + abilityFilePrefix + "/RPGMobs_Ability_" + abilityFilePrefix + "_Damage_" + cap + ".template.json");
                if (variationCount == 1) {
                    config.templates.add("root" + rootCap,
                            "Item/RootInteractions/NPCs/RPGMobs/" + abilityFilePrefix + "/RPGMobs_Ability_" + abilityFilePrefix + "_" + cap + "_Root.template.json");
                    config.templates.add("entry" + rootCap,
                            "Item/Interactions/NPCs/RPGMobs/" + abilityFilePrefix + "/RPGMobs_Ability_" + abilityFilePrefix + "_" + cap + "_Entry.template.json");
                } else {
                    for (int v = 1; v <= variationCount; v++) {
                        config.templates.add("root" + rootCap + "V" + v,
                                "Item/RootInteractions/NPCs/RPGMobs/" + abilityFilePrefix + "/RPGMobs_Ability_" + abilityFilePrefix + "_" + cap + "_V" + v + "_Root.template.json");
                        config.templates.add("entry" + rootCap + "V" + v,
                                "Item/Interactions/NPCs/RPGMobs/" + abilityFilePrefix + "/RPGMobs_Ability_" + abilityFilePrefix + "_" + cap + "_V" + v + "_Entry.template.json");
                    }
                }
            }
        }
    }

    public static final class EnrageAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_LIGHT_DAMAGE_INTERACTION = "lightDamage";
        public static final String TEMPLATE_HEAVY_DAMAGE_INTERACTION = "heavyDamage";
        public static final String TEMPLATE_EFFECT_RED_EYES = "effectRedEyes";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Health percent threshold (0.0 to 1.0) at which Enrage triggers per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] triggerHealthPercentPerTier = {0f, 0.40f, 0.35f, 0.30f, 0.25f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Per-hit damage for light punches (rapid jabs) per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] lightPunchDamagePerTier = {0f, 2f, 3f, 4f, 6f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Per-hit damage for heavy punches (uppercuts/slams) per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] heavyPunchDamagePerTier = {0f, 6f, 9f, 12f, 18f};
    }

    public static final class VolleyAbilityConfig extends AbilityConfig {
        public static final String TEMPLATE_PROJECTILE_DEFINITION = "projectileDefinition";

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Chance per tick evaluation to trigger Volley (0.0 to 1.0).")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] volleyTriggerChancePerTier = {0f, 0f, 0.08f, 0.12f, 0.18f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Minimum distance to target (blocks) for Volley to trigger.")
        public float minRange = 8.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Maximum distance to target (blocks) for Volley to trigger.")
        public float maxRange = 30.0f;

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Number of projectiles fired per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] projectileCountPerTier = {0, 0, 2, 3, 5};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Spread angle in degrees for projectile deviation per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public float[] spreadAnglePerTier = {0f, 0f, 15f, 12f, 10f};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Base damage per projectile per tier.")
        @FixedArraySize(value = TIERS_AMOUNT)
        public int[] baseDamagePerProjectilePerTier = {0, 0, 6, 10, 15};

        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Item ID of the crossbow the NPC equips during Volley.")
        public String npcCrossbowItemId = "Weapon_Crossbow_Iron";
    }


    public static final class SummonMarkerEntry {
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "NPC id to spawn.")
        public String Name = "";
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Weight in the spawn marker pool.")
        public double Weight = 100;
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Flock asset id used to pick the group size.")
        public String Flock = "RPGMobs_Summon_3_7";
        @Cfg(group = "Abilities", file = "abilities.yml", comment = "Spawn timing for this entry (ISO-8601 duration, e.g. PT1S).")
        public String SpawnAfterGameTime = "PT0S";
    }

    public static final class AbilityGate {
        public List<String> allowedWeaponCategories = List.of();
    }

    public static final class MobsConfig {
        @Cfg(group = "MobRules", file = "mobrules.yml", comment = "Mob rules: decide what to do if our scan found a NPC Entity with this id. If the id of the mob is not on the list, it will get the fist (it won't be transformed into an RPGMob. First mobRule match wins btw)")
        public Map<String, MobRule> defaultMobRules = defaultMobRules();

        @Cfg(group = "MobRules", file = "mobrules.yml", comment = "Mob rule category tree for organizing mob rules into groups.")
        public MobRuleCategory categoryTree = defaultCategoryTree();
    }

    public static Map<String, MobRule> defaultMobRules() {
        Map<String, MobRule> m = new LinkedHashMap<>();

        m.put("Goblin_Duke",
              mobRule(true,
                      List.of("Goblin_Duke"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_CLUBS

              )
        );

        m.put("Goblin_Hermit",
              mobRule(true,
                      List.of("Goblin_Hermit"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Goblin_Lobber_Patrol",
              mobRule(true,
                      List.of("Goblin_Lobber_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.ONLY_IF_EMPTY,
                      List.of()
              )
        );

        m.put("Goblin_Lobber",
              mobRule(true,
                      List.of("Goblin_Lobber"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.ONLY_IF_EMPTY,
                      List.of()
              )
        );

        m.put("Goblin_Miner_Patrol",
              mobRule(true,
                      List.of("Goblin_Miner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_PICKAXES
              )
        );

        m.put("Goblin_Miner",
              mobRule(true,
                      List.of("Goblin_Miner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_PICKAXES
              )
        );

        m.put("Goblin_Ogre",
              mobRule(true,
                      List.of("Goblin_Ogre"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_DAGGERS
              )
        );

        m.put("Goblin_Scavenger_Battleaxe",
              mobRule(true,
                      List.of("Goblin_Scavenger_Battleaxe"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Goblin_Scavenger_Sword",
              mobRule(true,
                      List.of("Goblin_Scavenger_Sword"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Goblin_Scavenger",
              mobRule(true,
                      List.of("Goblin_Scavenger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Goblin_Scrapper_Patrol",
              mobRule(true,
                      List.of("Goblin_Scrapper_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Goblin_Scrapper",
              mobRule(true,
                      List.of("Goblin_Scrapper"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Goblin_Thief_Patrol",
              mobRule(true,
                      List.of("Goblin_Thief_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Goblin_Thief",
              mobRule(true,
                      List.of("Goblin_Thief"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Outlander_Berserker",
              mobRule(true,
                      List.of("Outlander_Berserker"),
                      List.of(),
                      List.of(),
                      List.of("wolf"),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Outlander_Brute",
              mobRule(true,
                      List.of("Outlander_Brute"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Outlander_Cultist",
              mobRule(true,
                      List.of("Outlander_Cultist"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_DAGGERS
              )
        );

        m.put("Outlander_Hunter",
              mobRule(true,
                      List.of("Outlander_Hunter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Outlander_Marauder",
              mobRule(true,
                      List.of("Outlander_Marauder"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SHARP
              )
        );

        m.put("Outlander_Peon",
              mobRule(true,
                      List.of("Outlander_Peon"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_PICKAXES
              )
        );

        m.put("Outlander_Priest",
              mobRule(true,
                      List.of("Outlander_Priest"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Outlander_Sorcerer",
              mobRule(true,
                      List.of("Outlander_Sorcerer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Outlander_Stalker",
              mobRule(true,
                      List.of("Outlander_Stalker"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of("Shortbows", "Crossbows", "Daggers")
              )
        );

        m.put("Trork_Brawler",
              mobRule(true,
                      List.of("Trork_Brawler"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_CLUBS
              )
        );

        m.put("Trork_Chieftain",
              mobRule(true,
                      List.of("Trork_Chieftain"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Trork_Doctor_Witch",
              mobRule(true,
                      List.of("Trork_Doctor_Witch"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Trork_Guard",
              mobRule(true,
                      List.of("Trork_Guard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      List.of("Spears", "Daggers")
              )
        );

        m.put("Trork_Hunter",
              mobRule(true,
                      List.of("Trork_Hunter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Trork_Mauler",
              mobRule(true,
                      List.of("Trork_Mauler"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_CLUBS
              )
        );

        m.put("Trork_Sentry_Patrol",
              mobRule(true,
                      List.of("Trork_Sentry_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPEARS
              )
        );

        m.put("Trork_Sentry",
              mobRule(true,
                      List.of("Trork_Sentry"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPEARS
              )
        );

        m.put("Trork_Shaman",
              mobRule(true,
                      List.of("Trork_Shaman"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Trork_Unarmed",
              mobRule(true,
                      List.of("Trork_Unarmed"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Trork_Warrior_Patrol",
              mobRule(true,
                      List.of("Trork_Warrior_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_TWO_HANDED_SHARP
              )
        );

        m.put("Trork_Warrior",
              mobRule(true,
                      List.of("Trork_Warrior"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_TWO_HANDED_SHARP
              )
        );

        m.put("Skeleton_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Archer",
              mobRule(true,
                      List.of("Skeleton_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Archmage",
              mobRule(true,
                      List.of("Skeleton_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Fighter",
              mobRule(true,
                      List.of("Skeleton_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Knight",
              mobRule(true,
                      List.of("Skeleton_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Mage",
              mobRule(true,
                      List.of("Skeleton_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Ranger",
              mobRule(true,
                      List.of("Skeleton_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Scout",
              mobRule(true,
                      List.of("Skeleton_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Soldier",
              mobRule(true,
                      List.of("Skeleton_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Alchemist_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Alchemist_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Alchemist",
              mobRule(true,
                      List.of("Skeleton_Burnt_Alchemist"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Skeleton_Burnt_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Burnt_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Burnt_Archer",
              mobRule(true,
                      List.of("Skeleton_Burnt_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Burnt_Gunner_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Burnt_Gunner_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Burnt_Gunner",
              mobRule(true,
                      List.of("Skeleton_Burnt_Gunner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Burnt_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Knight",
              mobRule(true,
                      List.of("Skeleton_Burnt_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Lancer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Skeleton_Burnt_Lancer_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Skeleton_Burnt_Lancer",
              mobRule(true,
                      List.of("Skeleton_Burnt_Lancer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_AXES
              )
        );

        m.put("Skeleton_Burnt_Praetorian_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Praetorian_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Praetorian",
              mobRule(true,
                      List.of("Skeleton_Burnt_Praetorian"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Soldier",
              mobRule(true,
                      List.of("Skeleton_Burnt_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Burnt_Wizard_Patrol",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Burnt_Wizard_Wander",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Burnt_Wizard",
              mobRule(true,
                      List.of("Skeleton_Burnt_Wizard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Frost_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Archer",
              mobRule(true,
                      List.of("Skeleton_Frost_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Frost_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Frost_Archmage",
              mobRule(true,
                      List.of("Skeleton_Frost_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Frost_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Fighter",
              mobRule(true,
                      List.of("Skeleton_Frost_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Knight_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Knight_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Knight",
              mobRule(true,
                      List.of("Skeleton_Frost_Knight"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Frost_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Frost_Mage",
              mobRule(true,
                      List.of("Skeleton_Frost_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Frost_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Ranger",
              mobRule(true,
                      List.of("Skeleton_Frost_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Scout",
              mobRule(true,
                      List.of("Skeleton_Frost_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Frost_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Frost_Soldier",
              mobRule(true,
                      List.of("Skeleton_Frost_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Incandescent_Fighter_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Incandescent_Fighter_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Incandescent_Fighter",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Fighter"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Incandescent_Footman_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPEARS
              )
        );

        m.put("Skeleton_Incandescent_Footman_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPEARS
              )
        );

        m.put("Skeleton_Incandescent_Footman",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Footman"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPEARS
              )
        );

        m.put("Skeleton_Incandescent_Head",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Head"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of("Head")
              )
        );

        m.put("Skeleton_Incandescent_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Incandescent_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Incandescent_Mage",
              mobRule(true,
                      List.of("Skeleton_Incandescent_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Pirate_Captain_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Pirate_Captain_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Pirate_Captain",
              mobRule(true,
                      List.of("Skeleton_Pirate_Captain"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Pirate_Gunner_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Pirate_Gunner_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Pirate_Gunner",
              mobRule(true,
                      List.of("Skeleton_Pirate_Gunner"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_GUNS
              )
        );

        m.put("Skeleton_Pirate_Striker_Patrol",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Pirate_Striker_Wander",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Pirate_Striker",
              mobRule(true,
                      List.of("Skeleton_Pirate_Striker"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Sand_Archer_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Archer_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Archer",
              mobRule(true,
                      List.of("Skeleton_Sand_Archer"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Archmage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Sand_Archmage_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Sand_Archmage",
              mobRule(true,
                      List.of("Skeleton_Sand_Archmage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_STAVES
              )
        );

        m.put("Skeleton_Sand_Assassin_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_DAGGERS
              )
        );

        m.put("Skeleton_Sand_Assassin_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_DAGGERS
              )
        );

        m.put("Skeleton_Sand_Assassin",
              mobRule(true,
                      List.of("Skeleton_Sand_Assassin"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_DAGGERS
              )
        );

        m.put("Skeleton_Sand_Guard_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Sand_Guard_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Sand_Guard",
              mobRule(true,
                      List.of("Skeleton_Sand_Guard"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SWORDS
              )
        );

        m.put("Skeleton_Sand_Mage_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Sand_Mage_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Sand_Mage",
              mobRule(true,
                      List.of("Skeleton_Sand_Mage"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_SPELLBOOKS
              )
        );

        m.put("Skeleton_Sand_Ranger_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Ranger_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Ranger",
              mobRule(true,
                      List.of("Skeleton_Sand_Ranger"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Scout_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Scout_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Scout",
              mobRule(true,
                      List.of("Skeleton_Sand_Scout"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_BOWS
              )
        );

        m.put("Skeleton_Sand_Soldier_Patrol",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier_Patrol"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Sand_Soldier_Wander",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier_Wander"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton_Sand_Soldier",
              mobRule(true,
                      List.of("Skeleton_Sand_Soldier"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Skeleton",
              mobRule(true,
                      List.of("Skeleton"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        m.put("Zombie_Aberrant_Big",
              mobRule(true,
                      List.of("Zombie_Aberrant_Big"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie_Aberrant_Small",
              mobRule(true,
                      List.of("Zombie_Aberrant_Small"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie_Aberrant",
              mobRule(true,
                      List.of("Zombie_Aberrant"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie_Burnt",
              mobRule(true,
                      List.of("Zombie_Burnt"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie_Frost",
              mobRule(true,
                      List.of("Zombie_Frost"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie_Sand",
              mobRule(true,
                      List.of("Zombie_Sand"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Zombie",
              mobRule(true,
                      List.of("Zombie"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of()
              )
        );

        m.put("Crawler_Void",
              mobRule(true,
                      List.of("Crawler_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of("Head", "Chest", "Hands")
              )
        );

        m.put("Eye_Void",
              mobRule(true,
                      List.of("Eye_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of("Head")
              )
        );

        m.put("Spawn_Void",
              mobRule(true,
                      List.of("Spawn_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, true, true},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_LONGSWORDS
              )
        );

        m.put("Spectre_Void",
              mobRule(true,
                      List.of("Spectre_Void"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{false, false, false, false, false},
                      WeaponOverrideMode.NONE,
                      List.of(),
                      List.of("Head", "Chest", "Hands")
              )
        );

        m.put("Wraith",
              mobRule(true,
                      List.of("Wraith"),
                      List.of(),
                      List.of(),
                      List.of(),
                      new boolean[]{true, true, true, false, false},
                      WeaponOverrideMode.ALWAYS,
                      WEAPON_CATS_MELEE
              )
        );

        return m;
    }

    public enum WeaponOverrideMode {
        NONE, ONLY_IF_EMPTY, ALWAYS,
    }

    public static final class ExtraDropRule {
        public String itemId = "";
        public double chance = 0.0;
        @FixedArraySize(TIERS_AMOUNT)
        public boolean[] enabledPerTier = {true, true, true, true, true};
        public int minQty = 1;
        public int maxQty = 1;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExtraDropRule that)) return false;
            return Double.compare(that.chance, chance) == 0
                    && Arrays.equals(enabledPerTier, that.enabledPerTier)
                    && minQty == that.minQty
                    && maxQty == that.maxQty
                    && Objects.equals(itemId, that.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemId, chance, Arrays.hashCode(enabledPerTier), minQty, maxQty);
        }
    }

    public static final class GearCategory {
        public String name = "";
        public List<GearCategory> children = new ArrayList<>();
        public List<String> itemKeys = new ArrayList<>();

        public GearCategory() {}

        public GearCategory(String name, List<String> itemKeys, GearCategory... children) {
            this.name = name;
            this.itemKeys = new ArrayList<>(itemKeys);
            this.children = new ArrayList<>(List.of(children));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GearCategory that)) return false;
            return Objects.equals(name, that.name)
                    && Objects.equals(children, that.children)
                    && Objects.equals(itemKeys, that.itemKeys);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, children, itemKeys);
        }
    }

    public static final class MobRuleCategory {
        public String name = "";
        public List<MobRuleCategory> children = new ArrayList<>();
        public List<String> mobRuleKeys = new ArrayList<>();

        public MobRuleCategory() {}

        public MobRuleCategory(String name, List<String> mobRuleKeys, MobRuleCategory... children) {
            this.name = name;
            this.mobRuleKeys = new ArrayList<>(mobRuleKeys);
            this.children = new ArrayList<>(List.of(children));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MobRuleCategory that)) return false;
            return Objects.equals(name, that.name)
                    && Objects.equals(children, that.children)
                    && Objects.equals(mobRuleKeys, that.mobRuleKeys);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, children, mobRuleKeys);
        }
    }

    public static final class LootTemplate {
        public String name = "";
        public List<ExtraDropRule> drops = new ArrayList<>();
        public List<String> linkedMobRuleKeys = new ArrayList<>();

        public LootTemplate() {}

        public LootTemplate(String name, List<ExtraDropRule> drops, List<String> linkedMobRuleKeys) {
            this.name = name;
            this.drops = new ArrayList<>(drops);
            this.linkedMobRuleKeys = new ArrayList<>(linkedMobRuleKeys);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LootTemplate that)) return false;
            return Objects.equals(name, that.name)
                    && Objects.equals(drops, that.drops)
                    && Objects.equals(linkedMobRuleKeys, that.linkedMobRuleKeys);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, drops, linkedMobRuleKeys);
        }
    }

    public static final class LootTemplateCategory {
        public String name = "";
        public List<LootTemplateCategory> children = new ArrayList<>();
        public List<String> templateKeys = new ArrayList<>();

        public LootTemplateCategory() {}

        public LootTemplateCategory(String name, List<String> templateKeys, LootTemplateCategory... children) {
            this.name = name;
            this.templateKeys = new ArrayList<>(templateKeys);
            this.children = new ArrayList<>(List.of(children));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LootTemplateCategory that)) return false;
            return Objects.equals(name, that.name)
                    && Objects.equals(children, that.children)
                    && Objects.equals(templateKeys, that.templateKeys);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, children, templateKeys);
        }
    }

    public static final class MobRule {
        public boolean enabled = true;

        public List<String> matchExact = List.of();
        public List<String> matchStartsWith = List.of();
        public List<String> matchContains = List.of();
        public List<String> matchExcludes = List.of();

        public boolean[] enableWeaponOverrideForTier = new boolean[]{true, true, true, true, true};
        public WeaponOverrideMode weaponOverrideMode = WeaponOverrideMode.ALWAYS;

        public List<String> allowedWeaponCategories = List.of();

        public List<String> allowedArmorCategories = List.of();

        public List<String> allowedArmorSlots = List.of();

        public String combatStyle = "auto";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MobRule that)) return false;
            return enabled == that.enabled
                    && Arrays.equals(enableWeaponOverrideForTier, that.enableWeaponOverrideForTier)
                    && weaponOverrideMode == that.weaponOverrideMode
                    && Objects.equals(matchExact, that.matchExact)
                    && Objects.equals(matchStartsWith, that.matchStartsWith)
                    && Objects.equals(matchContains, that.matchContains)
                    && Objects.equals(matchExcludes, that.matchExcludes)
                    && Objects.equals(allowedWeaponCategories, that.allowedWeaponCategories)
                    && Objects.equals(allowedArmorCategories, that.allowedArmorCategories)
                    && Objects.equals(allowedArmorSlots, that.allowedArmorSlots)
                    && Objects.equals(combatStyle, that.combatStyle);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(enabled, weaponOverrideMode, matchExact, matchStartsWith,
                    matchContains, matchExcludes, allowedWeaponCategories, allowedArmorCategories,
                    allowedArmorSlots, combatStyle);
            result = 31 * result + Arrays.hashCode(enableWeaponOverrideForTier);
            return result;
        }
    }

    private static MobRule mobRule(boolean enabled, List<String> matchExact, List<String> matchStartsWith,
                                   List<String> contains, List<String> excludes, boolean[] enableWeaponOverrideForTier,
                                   WeaponOverrideMode overrideMode, List<String> allowedWeaponCategories) {
        return mobRule(enabled, matchExact, matchStartsWith, contains, excludes, enableWeaponOverrideForTier,
                       overrideMode, allowedWeaponCategories, List.of());
    }

    private static MobRule mobRule(boolean enabled, List<String> matchExact, List<String> matchStartsWith,
                                   List<String> contains, List<String> excludes, boolean[] enableWeaponOverrideForTier,
                                   WeaponOverrideMode overrideMode, List<String> allowedWeaponCategories,
                                   List<String> allowedArmorSlots) {
        MobRule r = new MobRule();
        r.matchExact = matchExact;
        r.matchStartsWith = matchStartsWith;
        r.enabled = enabled;
        r.matchContains = contains;
        r.matchExcludes = excludes;
        r.enableWeaponOverrideForTier = enableWeaponOverrideForTier;
        r.weaponOverrideMode = overrideMode;
        r.allowedWeaponCategories = allowedWeaponCategories;
        r.allowedArmorSlots = allowedArmorSlots;
        return r;
    }

    private static ExtraDropRule createExtraDropRule(String itemId, double chance, int minTier, int maxTier, int minQty,
                                                     int maxQty) {
        ExtraDropRule r = new ExtraDropRule();
        r.itemId = itemId;
        r.chance = chance;
        for (int i = 0; i < 5; i++) {
            r.enabledPerTier[i] = i >= minTier && i <= maxTier;
        }
        r.minQty = minQty;
        r.maxQty = maxQty;
        return r;
    }

    private static Map<String, Double> mapOf(String k1, double v1) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return m;
    }

    private static Map<String, Double> mapOf(String k1, double v1, String k2, double v2) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    public void populateSummonMarkerEntriesIfEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntries != null && !summonConfig.spawnMarkerEntries.isEmpty()) {
            summonConfig.spawnMarkerEntriesJson = toJson(summonConfig.spawnMarkerEntries);
        }

        ensureRoleIdentifier(summonConfig, "Goblin_");
        ensureRoleIdentifier(summonConfig, "Trork_");
        ensureSummonPoolExclusion(summonConfig, "Trork_Shaman");

        populateSummonMarkerEntriesByRoleIfEmpty();
        if (summonConfig.spawnMarkerEntries == null || summonConfig.spawnMarkerEntries.isEmpty()) {
            List<SummonMarkerEntry> defaultEntries = summonConfig.spawnMarkerEntriesByRole.get("default");
            if (defaultEntries != null) {
                summonConfig.spawnMarkerEntries = defaultEntries;
                summonConfig.spawnMarkerEntriesJson = toJson(defaultEntries);
            }
        }
    }

    public void populateSummonMarkerEntriesByRoleIfEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntriesByRole != null && !summonConfig.spawnMarkerEntriesByRole.isEmpty()) return;
        if (mobsConfig.defaultMobRules == null || mobsConfig.defaultMobRules.isEmpty()) return;

        LinkedHashSet<String> archerNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> allNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> zombieNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> wraithNpcIds = new LinkedHashSet<>();
        LinkedHashSet<String> aberrantNpcIds = new LinkedHashSet<>();

        for (Map.Entry<String, MobRule> entry : mobsConfig.defaultMobRules.entrySet()) {
            if (entry == null) continue;
            MobRule rule = entry.getValue();
            if (rule == null || !rule.enabled) continue;

            List<String> ids = new ArrayList<>();
            if (rule.matchExact != null && !rule.matchExact.isEmpty()) {
                for (String id : rule.matchExact) {
                    if (id == null || id.isBlank()) continue;
                    String cleaned = stripSummonPrefix(id);
                    if (cleaned.isBlank()) continue;
                    ids.add(cleaned);
                }
            } else {
                String key = entry.getKey();
                if (key != null && !key.isBlank()) {
                    String cleaned = stripSummonPrefix(key);
                    if (!cleaned.isBlank()) ids.add(cleaned);
                }
            }

            for (String id : ids) {
                String lower = id.toLowerCase(Locale.ROOT);
                allNpcIds.add(id);
                if (lower.contains("aberrant")) {
                    aberrantNpcIds.add(id);
                } else if (lower.contains("zombie")) {
                    zombieNpcIds.add(id);
                }
                if (lower.contains("wraith")) wraithNpcIds.add(id);
            }

            if (!isTierEnabled(rule.enableWeaponOverrideForTier, 0)) continue;
            if (!hasBowWeaponConstraint(rule.allowedWeaponCategories)) continue;

            archerNpcIds.addAll(ids);
        }

        if (archerNpcIds.isEmpty() && !allNpcIds.isEmpty()) {
            for (String id : allNpcIds) {
                String lower = id.toLowerCase(Locale.ROOT);
                if (lower.contains("archer") || lower.contains("ranger") || lower.contains("scout") || lower.contains(
                        "bow")) {
                    archerNpcIds.add(id);
                }
            }
        }

        if (archerNpcIds.isEmpty()) {
            List<String> fallbackIds = List.of("Skeleton_Archer",
                                               "Skeleton_Archer_Patrol",
                                               "Skeleton_Archer_Wander",
                                               "Skeleton_Ranger",
                                               "Skeleton_Ranger_Patrol",
                                               "Skeleton_Ranger_Wander",
                                               "Skeleton_Scout",
                                               "Skeleton_Scout_Patrol",
                                               "Skeleton_Scout_Wander",
                                               "Skeleton_Frost_Archer",
                                               "Skeleton_Frost_Archer_Patrol",
                                               "Skeleton_Frost_Archer_Wander",
                                               "Skeleton_Frost_Ranger",
                                               "Skeleton_Frost_Ranger_Patrol",
                                               "Skeleton_Frost_Ranger_Wander",
                                               "Skeleton_Frost_Scout",
                                               "Skeleton_Frost_Scout_Patrol",
                                               "Skeleton_Frost_Scout_Wander",
                                               "Skeleton_Burnt_Archer",
                                               "Skeleton_Burnt_Archer_Patrol",
                                               "Skeleton_Burnt_Archer_Wander",
                                               "Skeleton_Sand_Archer",
                                               "Skeleton_Sand_Archer_Patrol",
                                               "Skeleton_Sand_Archer_Wander",
                                               "Skeleton_Sand_Ranger",
                                               "Skeleton_Sand_Ranger_Patrol",
                                               "Skeleton_Sand_Ranger_Wander",
                                               "Skeleton_Sand_Scout",
                                               "Skeleton_Sand_Scout_Patrol",
                                               "Skeleton_Sand_Scout_Wander"
            );
            archerNpcIds.addAll(fallbackIds);
        }

        if (archerNpcIds.isEmpty()) return;

        ArrayList<String> roleIdentifiers = new ArrayList<>();
        if (summonConfig.roleIdentifiers != null) {
            for (String identifier : summonConfig.roleIdentifiers) {
                if (identifier == null || identifier.isBlank()) continue;
                roleIdentifiers.add(identifier.trim());
            }
        }
        roleIdentifiers.add("default");

        summonConfig.spawnMarkerEntriesByRole = new LinkedHashMap<>();

        List<String> moreSpecificIdentifiers = new ArrayList<>();

        for (String identifier : roleIdentifiers) {
            String normalizedIdentifier = normalizeRoleIdentifier(identifier);
            if (normalizedIdentifier.isBlank()) continue;

            ArrayList<String> roleBowIds = new ArrayList<>();
            ArrayList<String> roleNpcIds = new ArrayList<>();
            if ("default".equalsIgnoreCase(identifier)) {
                roleBowIds.addAll(archerNpcIds);
                roleNpcIds.addAll(allNpcIds);
            } else {
                String identifierLower = identifier.toLowerCase(Locale.ROOT);
                for (String id : allNpcIds) {
                    String idLower = id.toLowerCase(Locale.ROOT);
                    if (!idLower.contains(identifierLower)) continue;
                    if (matchesMoreSpecificIdentifier(idLower, identifierLower, moreSpecificIdentifiers)) continue;
                    roleNpcIds.add(id);
                }
                for (String id : archerNpcIds) {
                    String idLower = id.toLowerCase(Locale.ROOT);
                    if (!idLower.contains(identifierLower)) continue;
                    if (matchesMoreSpecificIdentifier(idLower, identifierLower, moreSpecificIdentifiers)) continue;
                    roleBowIds.add(id);
                }
                if (roleNpcIds.isEmpty()) roleNpcIds.addAll(allNpcIds);
                if (roleBowIds.isEmpty()) roleBowIds.addAll(archerNpcIds);
            }

            moreSpecificIdentifiers.add(identifier.toLowerCase(Locale.ROOT));

            List<String> excludeList = summonConfig.excludeFromSummonPool;
            if (excludeList != null && !excludeList.isEmpty()) {
                roleNpcIds.removeIf(id -> isExcludedFromSummonPool(id, excludeList));
                roleBowIds.removeIf(id -> isExcludedFromSummonPool(id, excludeList));
            }

            ArrayList<SummonMarkerEntry> entries = new ArrayList<>();
            double skeletonWeight = Math.max(0.0, summonConfig.skeletonArcherWeight);
            ArrayList<String> skeletonRoleIds = roleNpcIds.isEmpty() ? roleBowIds : roleNpcIds;
            for (String npcId : skeletonRoleIds) {
                SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                markerEntry.Name = npcId;
                markerEntry.Weight = skeletonWeight;
                markerEntry.Flock = "RPGMobs_Summon_3_7";
                markerEntry.SpawnAfterGameTime = "PT0S";
                entries.add(markerEntry);
            }

            double zombieWeight = Math.max(0.0, summonConfig.zombieWeight);
            if (!zombieNpcIds.isEmpty() && zombieWeight > 0.0) {
                for (String npcId : zombieNpcIds) {
                    if (!roleNpcIds.isEmpty()) {
                        String lower = npcId.toLowerCase(Locale.ROOT);
                        String identifierLower = identifier.toLowerCase(Locale.ROOT);
                        if (!lower.contains(identifierLower)) continue;
                    }
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = zombieWeight;
                    markerEntry.Flock = "RPGMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            double wraithWeight = Math.max(0.0, summonConfig.wraithWeight);
            if (!wraithNpcIds.isEmpty() && wraithWeight > 0.0) {
                for (String npcId : wraithNpcIds) {
                    if (!roleNpcIds.isEmpty()) {
                        String lower = npcId.toLowerCase(Locale.ROOT);
                        String identifierLower = identifier.toLowerCase(Locale.ROOT);
                        if (!lower.contains(identifierLower)) continue;
                    }
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = wraithWeight;
                    markerEntry.Flock = "RPGMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            double aberrantWeight = Math.max(0.0, summonConfig.aberrantWeight);
            if (!aberrantNpcIds.isEmpty() && aberrantWeight > 0.0 && identifier.toLowerCase(Locale.ROOT).contains(
                    "zombie")) {
                for (String npcId : aberrantNpcIds) {
                    SummonMarkerEntry markerEntry = new SummonMarkerEntry();
                    markerEntry.Name = npcId;
                    markerEntry.Weight = aberrantWeight;
                    markerEntry.Flock = "RPGMobs_Summon_3_7";
                    markerEntry.SpawnAfterGameTime = "PT0S";
                    entries.add(markerEntry);
                }
            }

            summonConfig.spawnMarkerEntriesByRole.put(normalizedIdentifier, entries);
        }
    }

    private static boolean matchesMoreSpecificIdentifier(String npcIdLower, String currentIdentifierLower,
                                                         List<String> moreSpecificIdentifiers) {
        for (String specific : moreSpecificIdentifiers) {
            if (specific.equals(currentIdentifierLower)) continue;
            if (specific.length() > currentIdentifierLower.length() && specific.contains(currentIdentifierLower) && npcIdLower.contains(
                    specific)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExcludedFromSummonPool(String npcId, List<String> excludeList) {
        String npcIdLower = npcId.toLowerCase(Locale.ROOT);
        for (String excluded : excludeList) {
            if (excluded == null || excluded.isBlank()) continue;
            if (npcIdLower.contains(excluded.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static void ensureRoleIdentifier(SummonAbilityConfig summonConfig, String identifier) {
        if (summonConfig.roleIdentifiers == null) {
            summonConfig.roleIdentifiers = new ArrayList<>();
        }
        for (String existing : summonConfig.roleIdentifiers) {
            if (existing != null && existing.equalsIgnoreCase(identifier)) return;
        }
        summonConfig.roleIdentifiers.add(identifier);
    }

    private static void ensureSummonPoolExclusion(SummonAbilityConfig summonConfig, String excluded) {
        if (summonConfig.excludeFromSummonPool == null) {
            summonConfig.excludeFromSummonPool = new ArrayList<>();
        }
        for (String existing : summonConfig.excludeFromSummonPool) {
            if (existing != null && existing.equalsIgnoreCase(excluded)) return;
        }
        summonConfig.excludeFromSummonPool.add(excluded);
    }

    public void upgradeSummonMarkerEntriesToVariantIds() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return;
        if (summonConfig.spawnMarkerEntriesByRole != null) {
            for (List<SummonMarkerEntry> entries : summonConfig.spawnMarkerEntriesByRole.values()) {
                upgradeSummonEntries(entries);
            }
        }
        if (summonConfig.spawnMarkerEntries != null) {
            upgradeSummonEntries(summonConfig.spawnMarkerEntries);
        }
    }

    private static void upgradeSummonEntries(List<SummonMarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null || entry.Name == null) continue;
            String name = stripSummonPrefix(entry.Name);
            if (name.isEmpty()) continue;
            entry.Name = name;
        }
    }

    public boolean isSummonMarkerEntriesEmpty() {
        SummonAbilityConfig summonConfig = null;
        if (abilitiesConfig.defaultAbilities != null) {
            AbilityConfig abilityConfig = abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
            if (abilityConfig instanceof SummonAbilityConfig s) summonConfig = s;
        }
        if (summonConfig == null) return true;
        boolean emptyBase = summonConfig.spawnMarkerEntries == null || summonConfig.spawnMarkerEntries.isEmpty();
        boolean emptyByRole = summonConfig.spawnMarkerEntriesByRole == null || summonConfig.spawnMarkerEntriesByRole.isEmpty();
        return emptyBase && emptyByRole;
    }

    public static String normalizeRoleIdentifier(String identifier) {
        if (identifier == null) return "";
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.equalsIgnoreCase("default")) return "Default";
        String normalized = trimmed.replaceAll("[^A-Za-z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        return normalized;
    }

    private static String stripSummonPrefix(String id) {
        if (id == null) return "";
        String trimmed = id.trim();
        if (trimmed.startsWith(SUMMON_ROLE_PREFIX)) {
            return trimmed.substring(SUMMON_ROLE_PREFIX.length()).trim();
        }
        return trimmed;
    }

    public static String buildSummonVariantRoleId(String baseRoleId) {
        if (baseRoleId == null || baseRoleId.isBlank()) return baseRoleId;
        if (baseRoleId.startsWith(SUMMON_ROLE_PREFIX)) return baseRoleId;
        return SUMMON_ROLE_PREFIX + baseRoleId;
    }

    private static String toJson(Object value) {
        try {
            return new Gson().toJson(value);
        } catch (Throwable ignored) {
            return "[]";
        }
    }

    private static boolean isTierEnabled(boolean[] enabledPerTier, int tierIndex) {
        if (enabledPerTier == null || tierIndex < 0 || tierIndex >= enabledPerTier.length) return false;
        return enabledPerTier[tierIndex];
    }

    private static boolean hasBowWeaponConstraint(List<String> allowedCategories) {
        if (allowedCategories == null || allowedCategories.isEmpty()) return false;
        for (String entry : allowedCategories) {
            if (entry == null) continue;
            String name = entry.startsWith("category:") ? entry.substring("category:".length()) : entry;
            if (name.equals("Shortbows") || name.equals("Crossbows")) return true;
        }
        return false;
    }

    public Map<String, ? extends AssetConfig> getAssetConfigForType(AssetType type) {
        if (type == null) return null;
        return switch (type) {
            case ABILITIES -> abilitiesConfig.defaultAbilities;
            case EFFECTS -> effectsConfig.defaultEntityEffects;
        };
    }

    public void migrate(String fromVersion) {
        if (fromVersion == null || fromVersion.equals(configVersion)) return;

        if (isOlder(fromVersion, "1.1.0")) {
            AbilityConfig heal = abilitiesConfig.defaultAbilities.get("heal_leap");
            if (heal instanceof HealLeapAbilityConfig h) {
                if (h.minHealthTriggerPercent == 0.1f) h.minHealthTriggerPercent = 0.50f;
                if (h.maxHealthTriggerPercent == 0.4f) h.maxHealthTriggerPercent = 0.50f;
                if (h.instantHealChance == 0.5f) h.instantHealChance = 1.00f;
            }
        }

    }

    private static boolean isOlder(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int n1 = Integer.parseInt(parts1[i]);
                int n2 = Integer.parseInt(parts2[i]);
                if (n1 < n2) return true;
                if (n1 > n2) return false;
            }
            return parts1.length < parts2.length;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
