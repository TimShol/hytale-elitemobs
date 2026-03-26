package com.frotty27.rpgmobs.plugin;

import com.frotty27.rpgmobs.api.RPGMobsAPI;
import com.frotty27.rpgmobs.api.events.RPGMobsReconcileEvent;
import com.frotty27.rpgmobs.assets.RPGMobsAssetGenerator;
import com.frotty27.rpgmobs.assets.RPGMobsAssetRetriever;
import com.frotty27.rpgmobs.commands.RPGMobsRootCommand;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.*;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.effects.RPGMobsActiveEffectsComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsMigrationComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonRiseComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.GlobalConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.migration.ConfigMigrationV2;
import com.frotty27.rpgmobs.config.overlay.ConfigOverlay;
import com.frotty27.rpgmobs.config.overlay.ConfigResolver;
import com.frotty27.rpgmobs.config.overlay.ConfigWriter;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.config.schema.YamlSerializer;
import com.frotty27.rpgmobs.features.RPGMobsFeatureRegistry;
import com.frotty27.rpgmobs.features.RPGMobsSpawningFeature;
import com.frotty27.rpgmobs.integrations.RPGLevelingIntegration;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.services.RPGMobsEventBus;
import com.frotty27.rpgmobs.services.RPGMobsNameplateService;
import com.frotty27.rpgmobs.services.RPGMobsQueryAPI;
import com.frotty27.rpgmobs.systems.combat.RPGMobsAITargetPollingSystem;
import com.frotty27.rpgmobs.systems.combat.RPGMobsCombatStateSystem;
import com.frotty27.rpgmobs.systems.death.RPGMobsVanillaDropsCullZoneManager;
import com.frotty27.rpgmobs.systems.drops.RPGMobsExtraDropsScheduler;
import com.frotty27.rpgmobs.systems.migration.RPGMobsComponentMigrationSystem;
import com.frotty27.rpgmobs.systems.spawn.RPGMobsSpawnSystem;
import com.frotty27.rpgmobs.utils.PlayerAttackTracker;
import com.frotty27.rpgmobs.utils.TickClock;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetRegistryLoader;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class RPGMobsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PACK_NAME = "RPGMobsGenerated";
    private RPGMobsConfig config;
    private GlobalConfig globalConfig;
    private final ConfigResolver configResolver = new ConfigResolver();

    private ComponentType<EntityStore, RPGMobsTierComponent> RPGMobsComponentType;
    private ComponentType<EntityStore, RPGMobsProgressionComponent> progressionComponentType;
    private ComponentType<EntityStore, RPGMobsHealthScalingComponent> healthScalingComponentType;
    private ComponentType<EntityStore, RPGMobsModelScalingComponent> modelScalingComponentType;
    private ComponentType<EntityStore, RPGMobsActiveEffectsComponent> activeEffectsComponentType;
    private ComponentType<EntityStore, RPGMobsCombatTrackingComponent> combatTrackingComponentType;
    private ComponentType<EntityStore, RPGMobsMigrationComponent> migrationComponentType;
    private ComponentType<EntityStore, RPGMobsSummonedMinionComponent> summonedMinionComponentType;
    private ComponentType<EntityStore, RPGMobsSummonMinionTrackingComponent> summonMinionTrackingComponentType;
    private ComponentType<EntityStore, RPGMobsSummonRiseComponent> summonRiseComponentType;
    private ComponentType<EntityStore, ChargeLeapAbilityComponent> chargeLeapAbilityComponentType;
    private ComponentType<EntityStore, HealLeapAbilityComponent> healLeapAbilityComponentType;
    private ComponentType<EntityStore, SummonUndeadAbilityComponent> summonUndeadAbilityComponentType;
    private ComponentType<EntityStore, RPGMobsAbilityLockComponent> abilityLockComponentType;
    private ComponentType<EntityStore, DodgeRollAbilityComponent> dodgeRollAbilityComponentType;
    private ComponentType<EntityStore, MultiSlashShortComponent> multiSlashShortComponentType;
    private ComponentType<EntityStore, MultiSlashMediumComponent> multiSlashMediumComponentType;
    private ComponentType<EntityStore, MultiSlashLongComponent> multiSlashLongComponentType;
    private ComponentType<EntityStore, EnrageAbilityComponent> enrageAbilityComponentType;
    private ComponentType<EntityStore, VolleyAbilityComponent> volleyAbilityComponentType;
    private final TickClock tickClock = new TickClock();
    private final PlayerAttackTracker playerAttackTracker = new PlayerAttackTracker();
    private final RPGMobsVanillaDropsCullZoneManager cullZoneManager = new RPGMobsVanillaDropsCullZoneManager(tickClock);
    private final RPGMobsExtraDropsScheduler dropsScheduler = new RPGMobsExtraDropsScheduler(tickClock);
    private final RPGMobsNameplateService nameplateService = new RPGMobsNameplateService();
    private final RPGMobsAssetRetriever RPGMobsAssetRetriever = new RPGMobsAssetRetriever(this);
    private final RPGMobsFeatureRegistry featureRegistry = new RPGMobsFeatureRegistry(this);
    private final RPGMobsEventBus eventBus = new RPGMobsEventBus();
    private RPGLevelingIntegration rpgLevelingIntegration;

    private final AtomicBoolean reconcileEventPending = new AtomicBoolean(false);
    private boolean reconcileActiveThisTick = false;

    private final AtomicInteger configReloadCount = new AtomicInteger(0);

    public RPGMobsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        RPGMobsAPI.setEventBus(eventBus);
        RPGMobsAPI.setQueryAPI(new RPGMobsQueryAPI(this));

        loadOrCreateRPGMobsConfig();

        getEventRegistry().register(LoadAssetEvent.class, this::onLoadAssets);

        nameplateService.describeSegments(this);

        registerComponents();
        registerSystems();
        registerCommands();
        registerIntegrations();

        LOGGER.atInfo().log("Setup complete!");
    }

    private void onLoadAssets(LoadAssetEvent event) {
        try {
            loadOrCreateRPGMobsConfig();
            Path RPGMobsDirectory = getModDirectory();
            RPGMobsAssetGenerator.generateAll(RPGMobsDirectory, config, true);

            AssetPack assetPack = new AssetPack(RPGMobsDirectory,
                                                ASSET_PACK_NAME,
                                                RPGMobsDirectory,
                                                FileSystems.getDefault(),
                                                false,
                                                getManifest()
            );

            AssetRegistryLoader.loadAssets(event, assetPack);

            LOGGER.atInfo().log("Loaded generated AssetPack '%s' from: %s",
                                ASSET_PACK_NAME,
                                RPGMobsDirectory.toAbsolutePath()
            );

            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("onLoadAssets failed: %s", error.toString());
            error.printStackTrace();
        }
    }

    public void discoverWorldsAndInstances() {

        try {
            var universeWorlds = Universe.get().getWorlds();
            if (universeWorlds != null) {
                for (String worldName : universeWorlds.keySet()) {
                    configResolver.registerWorldIfAbsent(worldName);
                }
                LOGGER.atInfo().log("Discovered %d worlds from Universe.", universeWorlds.size());
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("Universe.get().getWorlds() failed: %s", t.getMessage());
        }

        try {
            var instanceAssets = InstancesPlugin.get().getInstanceAssets();
            if (instanceAssets != null) {
                for (String templateName : instanceAssets) {
                    configResolver.registerInstanceIfAbsent(templateName);
                }
                LOGGER.atInfo().log("Discovered %d instance templates from InstancesPlugin.", instanceAssets.size());
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("InstancesPlugin.get().getInstanceAssets() failed: %s", t.getMessage());
        }
    }

    public void reloadConfigAndAssets() {
        loadOrCreateRPGMobsConfig();

        RPGMobsConfig cfg = getConfig();
        if (cfg == null) throw new IllegalStateException("RPGMobsConfig is null after force reload!");

        Path RPGMobsDir = getModDirectory();
        RPGMobsAssetGenerator.generateAll(RPGMobsDir, cfg, true);

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            LOGGER.atWarning().log("AssetModule is null; cannot force reload asset pack.");
            return;
        }

        try {
            assetModule.registerPack(ASSET_PACK_NAME, RPGMobsDir, getManifest(), true);
            LOGGER.atInfo().log("Reloaded config & regenerated assets successfully!");
            reloadNpcRoleAssetsIfPossible();

        } catch (Throwable error) {
            LOGGER.atWarning().log("Failed to reload: %s", error.toString());
            error.printStackTrace();
        }
    }

    public void reloadConfigOnly() {
        loadOrCreateRPGMobsConfig();
        int count = configReloadCount.incrementAndGet();
        LOGGER.atInfo().log("Reloaded config (no asset regeneration). configReloadCount=%d", count);
    }

    public void writeGlobalConfig() {
        if (globalConfig == null) return;
        Path modDirectory = getModDirectory();
        YamlSerializer.writeOnly(modDirectory, globalConfig);
        LOGGER.atInfo().log("GlobalConfig written to disk.");
    }

    private void reloadNpcRoleAssetsIfPossible() {
    }

    public synchronized void loadOrCreateRPGMobsConfig() {
        Path modDirectory = getModDirectory();

        ConfigMigrationV2.migrateIfNeeded(modDirectory);

        Path baseDir = modDirectory.resolve("base");

        int oldFormatVersion = readConfigFormatVersion(modDirectory);
        if (oldFormatVersion < 3) {
            LOGGER.atWarning().log(
                    "[RPGMobs] Config format version %d → 3  - deleting entire config directory.", oldFormatVersion);
            deleteDirectoryContents(modDirectory);
        }

        String oldVersion = readConfigVersionFromAnyLocation(modDirectory, baseDir);
        if (needsFullWipe(oldVersion)) {
            LOGGER.atWarning().log(
                    "[RPGMobs] Incompatible config version '%s'  - deleting entire config directory.", oldVersion);
            deleteDirectoryContents(modDirectory);
        }

        try {
            if (!Files.isDirectory(baseDir)) Files.createDirectories(baseDir);
            Files.createDirectories(modDirectory.resolve("worlds"));
            Files.createDirectories(modDirectory.resolve("instances"));
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to create config directories: %s", e.getMessage());
        }

        Path configRoot = Files.isDirectory(baseDir) ? baseDir : modDirectory;

        RPGMobsConfig defaults = new RPGMobsConfig();
        try {
            Object versionObj = getManifest().getVersion();
            if (versionObj != null) defaults.configVersion = versionObj.toString();
        } catch (Throwable ignored) {
        }

        config = YamlSerializer.loadOrCreate(configRoot, defaults);

        globalConfig = YamlSerializer.loadOrCreate(modDirectory, new GlobalConfig());

        if (globalConfig != null && globalConfig.configFormatVersion < 3) {
            globalConfig.configFormatVersion = 3;
            writeGlobalConfig();
        }

        generateDefaultInstanceOverlays(modDirectory);

        if (config != null && globalConfig != null) {
            config.debugConfig.isDebugModeEnabled = globalConfig.isDebugModeEnabled;
            config.debugConfig.debugMobRuleScanIntervalSeconds = globalConfig.debugMobRuleScanIntervalSeconds;
        }

        if (config != null) {
            configResolver.loadAll(modDirectory, globalConfig, config);
        }

        discoverWorldsAndInstances();

        if (config != null) {
            config.migrate(oldVersion);

            config.populateSummonMarkerEntriesIfEmpty();
            config.populateSummonMarkerEntriesByRoleIfEmpty();
            config.upgradeSummonMarkerEntriesToVariantIds();
            if (config.isSummonMarkerEntriesEmpty()) {
                LOGGER.atWarning().log("Undead summon is enabled but no archer NPCs were found in mob rules.");
            }
        }

        RPGMobsLogger.init(config);
        requestReconcileOnNextWorldTick();

        LOGGER.atInfo().log("Config loaded/reloaded from: %s", modDirectory.toAbsolutePath());
    }

    public Path getModDirectory() {
        return getDataDirectory().getParent().resolve("RPGMobs");
    }

    private void generateDefaultInstanceOverlays(Path modDirectory) {
        Path instancesDir = modDirectory.resolve("instances");
        Path goblinOverlay = instancesDir.resolve("Dungeon_Goblin.yml");

        if (!Files.exists(goblinOverlay)) {
            try {
                ConfigOverlay overlay = buildDungeonGoblinOverlay();
                ConfigWriter.writeOverlay(overlay, goblinOverlay);
                LOGGER.atInfo().log("Generated default Dungeon_Goblin.yml overlay.");
            } catch (Throwable e) {
                LOGGER.atWarning().log("Failed to generate Dungeon_Goblin overlay: %s", e.getMessage());
            }
        }
    }

    private static final List<String> GOBLIN_MOB_RULE_KEYS = List.of(
            "Goblin_Duke", "Goblin_Hermit", "Goblin_Ogre",
            "Goblin_Lobber", "Goblin_Lobber_Patrol",
            "Goblin_Miner", "Goblin_Miner_Patrol",
            "Goblin_Scavenger", "Goblin_Scavenger_Battleaxe", "Goblin_Scavenger_Sword",
            "Goblin_Scrapper", "Goblin_Scrapper_Patrol",
            "Goblin_Thief", "Goblin_Thief_Patrol"
    );

    private static ConfigOverlay buildDungeonGoblinOverlay() {
        ConfigOverlay o = new ConfigOverlay();
        o.enabled = true;

        o.progressionStyle = "NONE";
        o.spawnChancePerTier = new double[]{0, 10, 25, 35, 30};

        o.enableHealthScaling = true;
        o.healthMultiplierPerTier = new float[]{0.5f, 1.0f, 2.0f, 3.5f, 5.0f};
        o.enableDamageScaling = true;
        o.damageMultiplierPerTier = new float[]{0.5f, 1.0f, 1.8f, 2.5f, 3.5f};
        o.healthRandomVariance = 0.08f;
        o.damageRandomVariance = 0.05f;

        o.abilityOverlays = new LinkedHashMap<>();
        for (String abilId : new String[]{"charge_leap", "heal_leap", "undead_summon"}) {
            ConfigOverlay.AbilityOverlay ao = new ConfigOverlay.AbilityOverlay();
            ao.enabled = true;
            o.abilityOverlays.put(abilId, ao);
        }

        o.vanillaDroplistExtraRollsPerTier = new int[]{0, 1, 2, 4, 6};
        o.dropWeaponChance = 0.12;
        o.dropArmorPieceChance = 0.10;
        o.dropOffhandItemChance = 0.06;
        o.droppedGearDurabilityMin = 0.3;
        o.droppedGearDurabilityMax = 0.8;

        o.eliteFallDamageDisabled = true;

        o.enableNameplates = true;
        o.nameplateTierEnabled = new boolean[]{true, true, true, true, true};
        o.tierPrefixesByFamily = new LinkedHashMap<>();
        o.tierPrefixesByFamily.put("goblin", List.of("Sneaky", "Cutthroat", "Brutal", "Overseer", "Overlord"));
        o.enableModelScaling = true;
        o.modelScalePerTier = new float[]{1.0f, 1.05f, 1.1f, 1.15f, 1.25f};
        o.modelScaleVariance = 0.04f;

        o.tierOverrides = new LinkedHashMap<>();
        addTierOverride(o, "category:Lobber", new boolean[]{false, false, true, false, false});
        addTierOverride(o, "category:Miner", new boolean[]{true, true, false, false, false});
        addTierOverride(o, "category:Scavenger", new boolean[]{false, false, true, false, false});
        addTierOverride(o, "category:Scrapper", new boolean[]{false, false, true, false, false});
        addTierOverride(o, "category:Thief", new boolean[]{true, true, false, false, false});
        addTierOverride(o, "Goblin_Hermit", new boolean[]{false, true, true, false, false});
        addTierOverride(o, "Goblin_Ogre", new boolean[]{false, false, false, true, false});
        addTierOverride(o, "Goblin_Duke", new boolean[]{false, false, false, false, true});

        o.mobRules = buildGoblinMobRules();
        o.mobRuleCategoryTree = buildGoblinCategoryTree();

        o.lootTemplates = buildGoblinLootTemplates();
        o.lootTemplateCategoryTree = new RPGMobsConfig.LootTemplateCategory("All",
                List.of("Goblin Dungeon Loot", "Goblin Boss Loot"));

        o.distancePerTier = 0.0;
        o.distanceBonusInterval = 0.0;
        o.distanceHealthBonusPerInterval = 0f;
        o.distanceDamageBonusPerInterval = 0f;
        o.distanceHealthBonusCap = 0f;
        o.distanceDamageBonusCap = 0f;

        return o;
    }

    private static Map<String, RPGMobsConfig.MobRule> buildGoblinMobRules() {
        Map<String, RPGMobsConfig.MobRule> base = RPGMobsConfig.defaultMobRules();
        Map<String, RPGMobsConfig.MobRule> goblinRules = new LinkedHashMap<>();
        for (String key : GOBLIN_MOB_RULE_KEYS) {
            RPGMobsConfig.MobRule rule = base.get(key);
            if (rule != null) goblinRules.put(key, rule);
        }
        return goblinRules;
    }

    private static RPGMobsConfig.MobRuleCategory buildGoblinCategoryTree() {
        var lobber = new RPGMobsConfig.MobRuleCategory("Lobber", List.of(
                "Goblin_Lobber", "Goblin_Lobber_Patrol"));
        var miner = new RPGMobsConfig.MobRuleCategory("Miner", List.of(
                "Goblin_Miner", "Goblin_Miner_Patrol"));
        var scavenger = new RPGMobsConfig.MobRuleCategory("Scavenger", List.of(
                "Goblin_Scavenger", "Goblin_Scavenger_Battleaxe", "Goblin_Scavenger_Sword"));
        var scrapper = new RPGMobsConfig.MobRuleCategory("Scrapper", List.of(
                "Goblin_Scrapper", "Goblin_Scrapper_Patrol"));
        var thief = new RPGMobsConfig.MobRuleCategory("Thief", List.of(
                "Goblin_Thief", "Goblin_Thief_Patrol"));
        var goblins = new RPGMobsConfig.MobRuleCategory("Goblins", List.of(
                "Goblin_Duke", "Goblin_Hermit", "Goblin_Ogre"),
                lobber, miner, scavenger, scrapper, thief);
        return new RPGMobsConfig.MobRuleCategory("All", List.of(), goblins);
    }

    private static Map<String, RPGMobsConfig.LootTemplate> buildGoblinLootTemplates() {
        Map<String, RPGMobsConfig.LootTemplate> templates = new LinkedHashMap<>();

        var generalDrops = new ArrayList<RPGMobsConfig.ExtraDropRule>();
        generalDrops.add(makeDropRule("Ingredient_Life_Essence", 1.0, true, true, true, true, true, 2, 5));
        generalDrops.add(makeDropRule("Ore_Copper", 0.15, true, true, true, true, true, 1, 3));
        generalDrops.add(makeDropRule("Ingredient_Bar_Iron", 0.10, false, true, true, true, true, 1, 2));
        generalDrops.add(makeDropRule("Ingredient_Leather_Light", 0.12, true, true, true, true, true, 1, 3));
        templates.put("Goblin Dungeon Loot", new RPGMobsConfig.LootTemplate(
                "Goblin Dungeon Loot", generalDrops,
                List.of("category:Goblins")));

        var bossDrops = new ArrayList<RPGMobsConfig.ExtraDropRule>();
        bossDrops.add(makeDropRule("Ingredient_Life_Essence", 1.0, true, true, true, true, true, 5, 15));
        bossDrops.add(makeDropRule("Ore_Gold", 0.20, false, false, false, true, true, 1, 3));
        bossDrops.add(makeDropRule("Rock_Gem_Ruby", 0.05, false, false, false, true, true, 1, 1));
        bossDrops.add(makeDropRule("Tool_Repair_Kit_Iron", 0.25, false, false, true, true, true, 1, 2));
        templates.put("Goblin Boss Loot", new RPGMobsConfig.LootTemplate(
                "Goblin Boss Loot", bossDrops,
                List.of("Goblin_Duke", "Goblin_Ogre")));

        return templates;
    }

    private static RPGMobsConfig.ExtraDropRule makeDropRule(String itemId, double chance,
            boolean t1, boolean t2, boolean t3, boolean t4, boolean t5, int minQty, int maxQty) {
        RPGMobsConfig.ExtraDropRule r = new RPGMobsConfig.ExtraDropRule();
        r.itemId = itemId;
        r.chance = chance;
        r.enabledPerTier = new boolean[]{t1, t2, t3, t4, t5};
        r.minQty = minQty;
        r.maxQty = maxQty;
        return r;
    }

    private static void addTierOverride(ConfigOverlay overlay, String key, boolean[] allowedTiers) {
        var to = new ConfigOverlay.TierOverride();
        System.arraycopy(allowedTiers, 0, to.allowedTiers, 0, allowedTiers.length);
        overlay.tierOverrides.put(key, to);
    }

    private void deleteDirectoryContents(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteDirectoryContents(entry);
                    Files.deleteIfExists(entry);
                } else {
                    Files.deleteIfExists(entry);
                }
            }
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to clean directory %s: %s", directory, e.getMessage());
        }
        LOGGER.atInfo().log("Deleted contents of: %s", directory);
    }

    private int readConfigFormatVersion(Path modDirectory) {
        String raw = YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configFormatVersion");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String readConfigVersionFromAnyLocation(Path modDirectory, Path baseDir) {
        if (Files.isDirectory(baseDir) && Files.exists(baseDir.resolve("core.yml"))) {
            return YamlSerializer.readConfigVersion(baseDir, "core.yml", "configVersion");
        }
        if (Files.exists(modDirectory.resolve("core.yml"))) {
            return YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configVersion");
        }
        return null;
    }

    private boolean needsFullWipe(String oldVersion) {
        if (oldVersion == null) return false;
        if ("0.0.0".equals(oldVersion)) return true;
        try {
            Object currentVersionObj = getManifest().getVersion();
            if (currentVersionObj == null) return false;
            String currentVersion = currentVersionObj.toString();
            int oldMajor = parseMajorVersion(oldVersion);
            int currentMajor = parseMajorVersion(currentVersion);
            return oldMajor >= 0 && currentMajor >= 0 && oldMajor != currentMajor;
        } catch (Throwable e) {
            return false;
        }
    }

    private static int parseMajorVersion(String version) {
        if (version == null || version.isBlank()) return -1;
        int dot = version.indexOf('.');
        if (dot <= 0) return -1;
        try {
            return Integer.parseInt(version.substring(0, dot));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void registerComponents() {

        RPGMobsComponentType = getEntityStoreRegistry().registerComponent(RPGMobsTierComponent.class,
                                                                          "RPGMobsTierComponent",
                                                                          RPGMobsTierComponent.CODEC
        );
        LOGGER.atInfo().log("[1/21] Registered RPGMobsTierComponent");

        progressionComponentType = getEntityStoreRegistry().registerComponent(RPGMobsProgressionComponent.class,
                                                                              "RPGMobsProgressionComponent",
                                                                              RPGMobsProgressionComponent.CODEC
        );
        LOGGER.atInfo().log("[2/21] Registered RPGMobsProgressionComponent");

        healthScalingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsHealthScalingComponent.class,
                                                                                "RPGMobsHealthScalingComponent",
                                                                                RPGMobsHealthScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[3/21] Registered RPGMobsHealthScalingComponent");

        modelScalingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsModelScalingComponent.class,
                                                                               "RPGMobsModelScalingComponent",
                                                                               RPGMobsModelScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[4/21] Registered RPGMobsModelScalingComponent");

        activeEffectsComponentType = getEntityStoreRegistry().registerComponent(RPGMobsActiveEffectsComponent.class,
                                                                                "RPGMobsActiveEffectsComponent",
                                                                                RPGMobsActiveEffectsComponent.CODEC
        );
        LOGGER.atInfo().log("[5/21] Registered RPGMobsActiveEffectsComponent");

        combatTrackingComponentType = getEntityStoreRegistry().registerComponent(RPGMobsCombatTrackingComponent.class,
                                                                                 "RPGMobsCombatTrackingComponent",
                                                                                 RPGMobsCombatTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[6/21] Registered RPGMobsCombatTrackingComponent");

        migrationComponentType = getEntityStoreRegistry().registerComponent(RPGMobsMigrationComponent.class,
                                                                            "RPGMobsMigrationComponent",
                                                                            RPGMobsMigrationComponent.CODEC
        );
        LOGGER.atInfo().log("[7/21] Registered RPGMobsMigrationComponent (for pre 1.1.0)");

        summonedMinionComponentType = getEntityStoreRegistry().registerComponent(RPGMobsSummonedMinionComponent.class,
                                                                                 "RPGMobsSummonedMinionComponent",
                                                                                 RPGMobsSummonedMinionComponent.CODEC
        );
        LOGGER.atInfo().log("[8/21] Registered RPGMobsSummonedMinionComponent");

        summonMinionTrackingComponentType = getEntityStoreRegistry().registerComponent(
                RPGMobsSummonMinionTrackingComponent.class,
                "RPGMobsSummonMinionTrackingComponent",
                RPGMobsSummonMinionTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[9/21] Registered RPGMobsSummonMinionTrackingComponent");

        summonRiseComponentType = getEntityStoreRegistry().registerComponent(RPGMobsSummonRiseComponent.class,
                                                                             "RPGMobsSummonRiseComponent",
                                                                             RPGMobsSummonRiseComponent.CODEC
        );
        LOGGER.atInfo().log("[10/21] Registered RPGMobsSummonRiseComponent");

        chargeLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(ChargeLeapAbilityComponent.class,
                                                                                    "ChargeLeapAbilityComponent",
                                                                                    ChargeLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[11/21] Registered ChargeLeapAbilityComponent");

        healLeapAbilityComponentType = getEntityStoreRegistry().registerComponent(HealLeapAbilityComponent.class,
                                                                                  "HealLeapAbilityComponent",
                                                                                  HealLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[12/21] Registered HealLeapAbilityComponent");

        summonUndeadAbilityComponentType = getEntityStoreRegistry().registerComponent(SummonUndeadAbilityComponent.class,
                                                                                      "SummonUndeadAbilityComponent",
                                                                                      SummonUndeadAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[13/21] Registered SummonUndeadAbilityComponent");

        abilityLockComponentType = getEntityStoreRegistry().registerComponent(RPGMobsAbilityLockComponent.class,
                                                                              "RPGMobsAbilityLockComponent",
                                                                              RPGMobsAbilityLockComponent.CODEC
        );
        LOGGER.atInfo().log("[14/21] Registered RPGMobsAbilityLockComponent");

        dodgeRollAbilityComponentType = getEntityStoreRegistry().registerComponent(DodgeRollAbilityComponent.class,
                                                                                    "DodgeRollAbilityComponent",
                                                                                    DodgeRollAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[15/21] Registered DodgeRollAbilityComponent");

        multiSlashShortComponentType = getEntityStoreRegistry().registerComponent(MultiSlashShortComponent.class,
                                                                                    "MultiSlashShortComponent",
                                                                                    MultiSlashShortComponent.CODEC
        );
        LOGGER.atInfo().log("[16/21] Registered MultiSlashShortComponent");

        multiSlashMediumComponentType = getEntityStoreRegistry().registerComponent(MultiSlashMediumComponent.class,
                                                                                    "MultiSlashMediumComponent",
                                                                                    MultiSlashMediumComponent.CODEC
        );
        LOGGER.atInfo().log("[17/21] Registered MultiSlashMediumComponent");

        multiSlashLongComponentType = getEntityStoreRegistry().registerComponent(MultiSlashLongComponent.class,
                                                                                  "MultiSlashLongComponent",
                                                                                  MultiSlashLongComponent.CODEC
        );
        LOGGER.atInfo().log("[18/21] Registered MultiSlashLongComponent");

        enrageAbilityComponentType = getEntityStoreRegistry().registerComponent(EnrageAbilityComponent.class,
                                                                                "EnrageAbilityComponent",
                                                                                EnrageAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[19/21] Registered EnrageAbilityComponent");

        volleyAbilityComponentType = getEntityStoreRegistry().registerComponent(VolleyAbilityComponent.class,
                                                                                "VolleyAbilityComponent",
                                                                                VolleyAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[20/21] Registered VolleyAbilityComponent");

        LOGGER.atInfo().log("Component registration complete: 21 total (10 core + 11 ability)");
    }

    @SuppressWarnings("unchecked")
    public void registerSystem(Object system) {
        if (system instanceof EntityTickingSystem) {
            getEntityStoreRegistry().registerSystem((EntityTickingSystem<EntityStore>) system);
        } else if (system instanceof DamageEventSystem) {
            getEntityStoreRegistry().registerSystem((DamageEventSystem) system);
        } else if (system instanceof DeathSystems.OnDeathSystem) {
            getEntityStoreRegistry().registerSystem((DeathSystems.OnDeathSystem) system);
        } else if (system instanceof com.hypixel.hytale.component.system.System) {
            getEntityStoreRegistry().registerSystem((com.hypixel.hytale.component.system.System<EntityStore>) system);
        } else {
            LOGGER.atWarning().log("Unknown system type: " + system.getClass().getName());
        }
    }

    private void registerSystems() {

        registerSystem(new RPGMobsComponentMigrationSystem(this));
        LOGGER.atInfo().log("Registered Migration System");
        registerSystem(new RPGMobsCombatStateSystem(this));
        registerSystem(new RPGMobsAITargetPollingSystem(this));
        LOGGER.atInfo().log("Registered Combat State Systems");

        featureRegistry.registerSystems(this);
        LOGGER.atInfo().log("Registered Feature Systems");
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new RPGMobsRootCommand(this));
        LOGGER.atInfo().log("Registered RPGMobs commands");
    }

    private void registerIntegrations() {
        RPGLevelingIntegration integration = new RPGLevelingIntegration(this);
        if (integration.tryActivate()) {
            rpgLevelingIntegration = integration;
        }
    }

    public RPGMobsVanillaDropsCullZoneManager getMobDropsCleanupManager() {
        return cullZoneManager;
    }

    public RPGMobsExtraDropsScheduler getExtraDropsScheduler() {
        return dropsScheduler;
    }

    public RPGMobsConfig getConfig() {
        return config;
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public ConfigResolver getConfigResolver() {
        return configResolver;
    }

    public ResolvedConfig getResolvedConfig(String worldName) {
        return configResolver.getResolvedConfig(worldName);
    }

    public TickClock getTickClock() {
        return tickClock;
    }

    public PlayerAttackTracker getPlayerAttackTracker() {
        return playerAttackTracker;
    }

    public ComponentType<EntityStore, RPGMobsTierComponent> getRPGMobsComponentType() {
        return RPGMobsComponentType;
    }

    public ComponentType<EntityStore, RPGMobsProgressionComponent> getProgressionComponentType() {
        return progressionComponentType;
    }

    public ComponentType<EntityStore, RPGMobsHealthScalingComponent> getHealthScalingComponentType() {
        return healthScalingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsModelScalingComponent> getModelScalingComponentType() {
        return modelScalingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsActiveEffectsComponent> getActiveEffectsComponentType() {
        return activeEffectsComponentType;
    }

    public ComponentType<EntityStore, RPGMobsCombatTrackingComponent> getCombatTrackingComponentType() {
        return combatTrackingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsMigrationComponent> getMigrationComponentType() {
        return migrationComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return summonedMinionComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonMinionTrackingComponent> getSummonMinionTrackingComponentType() {
        return summonMinionTrackingComponentType;
    }

    public ComponentType<EntityStore, RPGMobsSummonRiseComponent> getSummonRiseComponentType() {
        return summonRiseComponentType;
    }

    public ComponentType<EntityStore, ChargeLeapAbilityComponent> getChargeLeapAbilityComponentType() {
        return chargeLeapAbilityComponentType;
    }

    public ComponentType<EntityStore, HealLeapAbilityComponent> getHealLeapAbilityComponentType() {
        return healLeapAbilityComponentType;
    }

    public ComponentType<EntityStore, SummonUndeadAbilityComponent> getSummonUndeadAbilityComponentType() {
        return summonUndeadAbilityComponentType;
    }

    public ComponentType<EntityStore, RPGMobsAbilityLockComponent> getAbilityLockComponentType() {
        return abilityLockComponentType;
    }

    public ComponentType<EntityStore, DodgeRollAbilityComponent> getDodgeRollAbilityComponentType() {
        return dodgeRollAbilityComponentType;
    }

    public ComponentType<EntityStore, MultiSlashShortComponent> getMultiSlashShortComponentType() {
        return multiSlashShortComponentType;
    }

    public ComponentType<EntityStore, MultiSlashMediumComponent> getMultiSlashMediumComponentType() {
        return multiSlashMediumComponentType;
    }

    public ComponentType<EntityStore, MultiSlashLongComponent> getMultiSlashLongComponentType() {
        return multiSlashLongComponentType;
    }

    public ComponentType<EntityStore, EnrageAbilityComponent> getEnrageAbilityComponentType() {
        return enrageAbilityComponentType;
    }

    public ComponentType<EntityStore, VolleyAbilityComponent> getVolleyAbilityComponentType() {
        return volleyAbilityComponentType;
    }


    public RPGMobsNameplateService getNameplateService() {
        return nameplateService;
    }

    public RPGMobsSpawnSystem getSpawnSystem() {
        RPGMobsSpawningFeature spawning = (RPGMobsSpawningFeature) featureRegistry.getFeature("Spawning");
        return spawning != null ? spawning.getSpawnSystem() : null;
    }

    public RPGMobsAssetRetriever getRPGMobsAssetLoader() {
        return RPGMobsAssetRetriever;
    }

    public RPGMobsFeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public RPGMobsEventBus getEventBus() {
        return eventBus;
    }

    public int getConfigReloadCount() {
        return configReloadCount.get();
    }

    public void requestReconcileOnNextWorldTick() {
        reconcileEventPending.set(true);
    }

    public boolean shouldReconcileThisTick() {
        return reconcileActiveThisTick;
    }

    public void onWorldTick() {
        if (reconcileEventPending.getAndSet(false)) {
            reconcileActiveThisTick = true;
            eventBus.fire(new RPGMobsReconcileEvent());
            LOGGER.atInfo().log("Config changed  - reconciling loaded elites (configReloadCount=%d).", configReloadCount.get());
        } else {
            reconcileActiveThisTick = false;
        }
    }

}
