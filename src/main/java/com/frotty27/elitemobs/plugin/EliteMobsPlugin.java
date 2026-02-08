package com.frotty27.elitemobs.plugin;

import com.frotty27.elitemobs.assets.EliteMobsAssetGenerator;
import com.frotty27.elitemobs.assets.EliteMobsAssetRetriever;
import com.frotty27.elitemobs.commands.EliteMobsRootCommand;
import com.frotty27.elitemobs.components.EliteMobsLeapAbilityStateComponent;
import com.frotty27.elitemobs.components.EliteMobsSummonRiseComponent;
import com.frotty27.elitemobs.components.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.schema.YamlSerializer;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.nameplates.EliteMobsNameplateService;
import com.frotty27.elitemobs.systems.ability.SummonRiseTracker;
import com.frotty27.elitemobs.systems.spawn.EliteMobsSpawnSystem;
import com.frotty27.elitemobs.utils.TickClock;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetRegistryLoader;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class EliteMobsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PACK_NAME = "EliteMobsGenerated";
    private EliteMobsConfig config;

    private ComponentType<EntityStore, EliteMobsTierComponent> eliteMobsComponent;
    private ComponentType<EntityStore, EliteMobsLeapAbilityStateComponent> leapAbilityComponent;
    private ComponentType<EntityStore, EliteMobsSummonRiseComponent> summonRiseComponent;
    private ComponentType<EntityStore, EliteMobsSummonedMinionComponent> summonedMinionComponent;

    private final TickClock tickClock = new TickClock();
    private final com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager cullZoneManager = new com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager(tickClock);
    private final com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler dropsScheduler = new com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler(tickClock);
    private final EliteMobsNameplateService nameplateService = new EliteMobsNameplateService();
    private final EliteMobsAssetRetriever eliteMobsAssetRetriever = new EliteMobsAssetRetriever(this);
    private final EliteMobsFeatureRegistry featureRegistry = new EliteMobsFeatureRegistry(this);
    private final SummonRiseTracker summonRiseTracker = new SummonRiseTracker();
    
    private final AtomicBoolean reconcileRequested = new AtomicBoolean(false);
    private final AtomicInteger reconcileTicksRemaining = new AtomicInteger(0);
    private boolean reconcileActive = false;
    private boolean warnedMissingCompat = false;
    private boolean rpgLevelingDetected = false;
    private boolean compatDetected = false;
    private static final int COMPAT_WARNING_WIDTH = 70;

    public EliteMobsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        loadOrCreateEliteMobsConfig();

        getEventRegistry().register(LoadAssetEvent.class, this::onLoadAssets);
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        registerComponents();
        registerSystems();
        registerCommands();

        detectRpgCompat();
        warnIfCompatMissing();

        LOGGER.atInfo().log("Setup complete!");
    }

    private void onLoadAssets(LoadAssetEvent event) {
        try {
            loadOrCreateEliteMobsConfig();
            Path eliteMobsDirectory = getModDirectory();
            EliteMobsAssetGenerator.generateAll(eliteMobsDirectory, config, true);

            AssetPack assetPack = new AssetPack(eliteMobsDirectory,                    // packLocation
                                           ASSET_PACK_NAME,                 // name
                                           eliteMobsDirectory,                    // root
                                           FileSystems.getDefault(),        // fs
                                           false,                           // immutable
                                           getManifest()                    // plugin manifest
            );

            AssetRegistryLoader.loadAssets(event, assetPack);

            LOGGER.atInfo().log("Loaded generated AssetPack '%s' from: %s",
                                ASSET_PACK_NAME,
                                eliteMobsDirectory.toAbsolutePath()
            );
            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("onLoadAssets failed: %s", error.toString());
            error.printStackTrace();
        }
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();
        if (player == null) return;
        EliteMobsConfig cfg = config;
        if (cfg != null && cfg.compatConfig != null && !cfg.compatConfig.showCompatJoinMessages) return;
        if (shouldWarnCompat()) {
            player.sendMessage(Message.raw("[EliteMobs] Compatibility notice: This server uses EliteMobs and RPGLevelingSystem without a compatibility plugin. Please inform the server owner to install the EliteMobsRPGLevelingCompat plugin.")
                                        .color("#FFD700"));
            return;
        }

        if (rpgLevelingDetected && compatDetected) {
            player.sendMessage(Message.raw("[EliteMobs] Compatibility active: EliteMobs + RPGLevelingSystem patch detected.")
                                        .color("#00C853"));
        }
    }

    public void reloadConfigAndAssets() {
        loadOrCreateEliteMobsConfig();

        EliteMobsConfig cfg = getConfig();
        if (cfg == null) throw new IllegalStateException("EliteMobsConfig is null after force reload!");

        Path eliteMobsDir = getModDirectory();
        EliteMobsAssetGenerator.generateAll(eliteMobsDir, cfg, true);

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            LOGGER.atWarning().log("[EliteMobs] AssetModule is null; cannot force reload asset pack.");
            return;
        }

        try {
            assetModule.registerPack(ASSET_PACK_NAME, eliteMobsDir, getManifest());
            LOGGER.atInfo().log("[EliteMobs] Reloaded config & regenerated assets successfully!");
            reloadNpcRoleAssetsIfPossible();
        } catch (Throwable error) {
            LOGGER.atWarning().log("[EliteMobs] Failed to reload: %s", error.toString());
            error.printStackTrace();
        }
    }

    private void reloadNpcRoleAssetsIfPossible() {
        // NPCPlugin auto-reloads assets; manual reload is noisy and unnecessary here.
    }

    public synchronized void loadOrCreateEliteMobsConfig() {
        Path modDirectory = getModDirectory();
        
        // 1. Read existing file version for migration
        String oldVersion = YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configVersion");

        // 2. Prepare defaults with current mod version
        EliteMobsConfig defaults = new EliteMobsConfig();
        try {
            Object versionObj = getManifest().getVersion();
            if (versionObj != null) defaults.configVersion = versionObj.toString();
        } catch (Throwable ignored) {}

        // 3. Load & Merge
        config = YamlSerializer.loadOrCreate(modDirectory, defaults);
        
        if (config != null) {
            // 4. Run Migration Hook
            config.migrate(oldVersion);

            config.populateSummonMarkerEntriesIfEmpty();
            config.populateSummonMarkerEntriesByRoleIfEmpty();
            config.upgradeSummonMarkerEntriesToVariantIds();
            if (config.isSummonMarkerEntriesEmpty()) {
                LOGGER.atWarning().log("[EliteMobs] Undead summon is enabled but no bow NPCs were found in mob rules.");
            }
        }

        EliteMobsLogger.init(config);
        requestReconcileOnNextWorldTick();

        LOGGER.atInfo().log("Config loaded/reloaded from: %s", modDirectory.toAbsolutePath());
    }

    private Path getModDirectory() {
        return getDataDirectory().getParent().resolve("EliteMobs");
    }

    private void registerComponents() {
        eliteMobsComponent = getEntityStoreRegistry().registerComponent(EliteMobsTierComponent.class,
                                                                        "EliteMobsTierComponent",
                                                                        EliteMobsTierComponent.CODEC
        );
        LOGGER.atInfo().log("Registered EliteMobsTierComponent");

        leapAbilityComponent = getEntityStoreRegistry().registerComponent(EliteMobsLeapAbilityStateComponent.class,
                                                                          "EliteLeapAbilityStateComponent",
                                                                          EliteMobsLeapAbilityStateComponent.CODEC
        );
        LOGGER.atInfo().log("Registered EliteMobsLeapAbilityStateComponent.");

        summonRiseComponent = getEntityStoreRegistry().registerComponent(EliteMobsSummonRiseComponent.class,
                                                                         "EliteMobsSummonRiseComponent",
                                                                         EliteMobsSummonRiseComponent.CODEC
        );
        LOGGER.atInfo().log("Registered EliteMobsSummonRiseComponent.");

        summonedMinionComponent = getEntityStoreRegistry().registerComponent(EliteMobsSummonedMinionComponent.class,
                                                                             "EliteMobsSummonedMinionComponent",
                                                                             EliteMobsSummonedMinionComponent.CODEC
        );
        LOGGER.atInfo().log("Registered EliteMobsSummonedMinionComponent.");
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
        featureRegistry.registerSystems(this);
        LOGGER.atInfo().log("Registered Feature Systems.");
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new EliteMobsRootCommand(this));
        LOGGER.atInfo().log("Registered EliteMobs commands.");
    }

    public com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager getMobDropsCleanupManager() {
        return cullZoneManager;
    }

    public com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler getExtraDropsScheduler() {
        return dropsScheduler;
    }

    public EliteMobsConfig getConfig() {
        return config;
    }

    public TickClock getTickClock() {
        return tickClock;
    }

    public SummonRiseTracker getSummonRiseTracker() {
        return summonRiseTracker;
    }

    public ComponentType<EntityStore, EliteMobsTierComponent> getEliteMobsComponent() {
        return eliteMobsComponent;
    }

    public ComponentType<EntityStore, EliteMobsLeapAbilityStateComponent> getLeapAbilityComponent() {
        return leapAbilityComponent;
    }

    public ComponentType<EntityStore, EliteMobsSummonRiseComponent> getSummonRiseComponent() {
        return summonRiseComponent;
    }

    public ComponentType<EntityStore, EliteMobsSummonedMinionComponent> getSummonedMinionComponent() {
        return summonedMinionComponent;
    }

    public EliteMobsNameplateService getNameplateService() {
        return nameplateService;
    }

    public EliteMobsSpawnSystem getSpawnSystem() {
        com.frotty27.elitemobs.features.EliteMobsSpawningFeature spawning = (com.frotty27.elitemobs.features.EliteMobsSpawningFeature) featureRegistry.getFeature("Spawning");
        return spawning != null ? spawning.getSpawnSystem() : null;
    }

    public EliteMobsAssetRetriever getEliteMobsAssetLoader() {
        return eliteMobsAssetRetriever;
    }

    public EliteMobsFeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public void requestReconcileOnNextWorldTick() {
        reconcileRequested.set(true);
    }

    public boolean shouldReconcileThisTick() {
        return reconcileTicksRemaining.get() > 0;
    }

    public void onWorldTick() {
        EliteMobsConfig cfg = config;
        if (cfg == null) return;

        if (reconcileRequested.getAndSet(false)) {
            int windowTicks = Math.max(0, cfg.reconcileConfig.reconcileWindowTicks);
            reconcileTicksRemaining.set(windowTicks);
            reconcileActive = windowTicks > 0;
            if (cfg.reconcileConfig.announceReconcile) {
                if (windowTicks > 0) {
                    LOGGER.atInfo().log("[EliteMobs] Reconcile started (%d ticks).", windowTicks);
                } else {
                    LOGGER.atInfo().log("[EliteMobs] Reconcile skipped (window=0).");
                }
            }
            return;
        }

        int remaining = reconcileTicksRemaining.updateAndGet(value -> value > 0 ? value - 1 : 0);
        if (reconcileActive && remaining == 0) {
            reconcileActive = false;
            if (cfg.reconcileConfig.announceReconcile) {
                LOGGER.atInfo().log("[EliteMobs] Reconcile finished.");
            }
        }
    }

    private void detectRpgCompat() {
        try {
            Class<?> pluginManagerClass = Class.forName("com.hypixel.hytale.server.core.plugin.PluginManager");
            var getMethod = pluginManagerClass.getMethod("get");
            Object pluginManager = getMethod.invoke(null);
            if (pluginManager == null) return;

            var pluginsField = pluginManagerClass.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            Object pluginsObj = pluginsField.get(pluginManager);
            if (!(pluginsObj instanceof Map<?, ?> plugins)) return;

            for (Object value : plugins.values()) {
                if (value == null) continue;
                String className = value.getClass().getName();
                if (className.equals("org.zuxaw.plugin.RPGLevelingPlugin")) {
                    rpgLevelingDetected = true;
                } else if (className.equals("com.frotty27.elitemobscompat.EliteMobsRPGLevelingCompatPlugin")) {
                    compatDetected = true;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean shouldWarnCompat() {
        return rpgLevelingDetected && !compatDetected;
    }

    private void warnIfCompatMissing() {
        if (warnedMissingCompat) return;
        if (!shouldWarnCompat()) return;
        warnedMissingCompat = true;

        String border = "=".repeat(COMPAT_WARNING_WIDTH);
        LOGGER.atWarning().log(border);
        LOGGER.atWarning().log(formatCompatLine("EliteMobs + RPGLevelingSystem detected; no compat plugin."));
        LOGGER.atWarning().log(formatCompatLine("Please install EliteMobsRPGLevelingCompat plugin."));
        LOGGER.atWarning().log(border);
    }

    private static String formatCompatLine(String message) {
        String msg = (message == null) ? "" : message.trim();
        int innerWidth = Math.max(0, COMPAT_WARNING_WIDTH - 4);
        int textWidth = Math.max(0, innerWidth - 2);
        if (msg.length() > textWidth) {
            msg = msg.substring(0, textWidth);
        }
        int padding = textWidth - msg.length();
        int left = padding / 2;
        int right = padding - left;
        return "==" + "=".repeat(left) + " " + msg + " " + "=".repeat(right) + "==";
    }
}