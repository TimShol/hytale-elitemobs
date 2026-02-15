package com.frotty27.elitemobs.plugin;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.frotty27.elitemobs.api.EliteMobsAPI;
import com.frotty27.elitemobs.api.EliteMobsEventBus;
import com.frotty27.elitemobs.api.IEliteMobsEventListener;
import com.frotty27.elitemobs.api.query.EliteMobsQueryAPI;
import com.frotty27.elitemobs.assets.EliteMobsAssetGenerator;
import com.frotty27.elitemobs.assets.EliteMobsAssetRetriever;
import com.frotty27.elitemobs.commands.EliteMobsRootCommand;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.components.effects.EliteMobsActiveEffectsComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsMigrationComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsHealthScalingComponent;
import com.frotty27.elitemobs.components.lifecycle.EliteMobsModelScalingComponent;
import com.frotty27.elitemobs.components.progression.EliteMobsProgressionComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonMinionTrackingComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonRiseComponent;
import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.schema.YamlSerializer;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.nameplates.EliteMobsNameplateService;
import com.frotty27.elitemobs.systems.ability.SummonRiseTracker;
import com.frotty27.elitemobs.systems.migration.EliteMobsComponentMigrationSystem;
import com.frotty27.elitemobs.systems.spawn.EliteMobsSpawnSystem;
import com.frotty27.elitemobs.utils.TickClock;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetRegistryLoader;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PACK_NAME = "EliteMobsGenerated";
    private EliteMobsConfig config;

    private ComponentType<EntityStore, EliteMobsTierComponent> eliteMobsComponent;
    private ComponentType<EntityStore, EliteMobsProgressionComponent> progressionComponent;
    private ComponentType<EntityStore, EliteMobsHealthScalingComponent> healthScalingComponent;
    private ComponentType<EntityStore, EliteMobsModelScalingComponent> modelScalingComponent;
    private ComponentType<EntityStore, EliteMobsActiveEffectsComponent> activeEffectsComponent;
    private ComponentType<EntityStore, EliteMobsCombatTrackingComponent> combatTrackingComponent;
    private ComponentType<EntityStore, EliteMobsMigrationComponent> migrationComponent;
    private ComponentType<EntityStore, EliteMobsSummonedMinionComponent> summonedMinionComponent;
    private ComponentType<EntityStore, EliteMobsSummonMinionTrackingComponent> summonMinionTrackingComponent;
    private ComponentType<EntityStore, EliteMobsSummonRiseComponent> summonRiseComponent;

    private ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent> chargeLeapAbilityComponent;
    private ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent> healLeapAbilityComponent;
    private ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent> summonUndeadAbilityComponent;
    private ComponentType<EntityStore, EliteMobsAbilityLockComponent> abilityLockComponent;

    private final TickClock tickClock = new TickClock();
    private final com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager cullZoneManager = new com.frotty27.elitemobs.systems.death.EliteMobsVanillaDropsCullZoneManager(tickClock);
    private final com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler dropsScheduler = new com.frotty27.elitemobs.systems.drops.EliteMobsExtraDropsScheduler(tickClock);
    private final EliteMobsNameplateService nameplateService = new EliteMobsNameplateService();
    private final EliteMobsAssetRetriever eliteMobsAssetRetriever = new EliteMobsAssetRetriever(this);
    private final EliteMobsFeatureRegistry featureRegistry = new EliteMobsFeatureRegistry(this);
    private final SummonRiseTracker summonRiseTracker = new SummonRiseTracker();
    private final EliteMobsEventBus eventBus = new EliteMobsEventBus();


    private com.frotty27.elitemobs.systems.visual.HealthScalingSystem healthScalingSystem;
    private com.frotty27.elitemobs.systems.visual.ModelScalingSystem modelScalingSystem;
    private com.frotty27.elitemobs.systems.ability.EliteMobsAbilityTriggerListener abilityTriggerListener;

    private final AtomicBoolean reconcileRequested = new AtomicBoolean(false);
    private final AtomicInteger reconcileTicksRemaining = new AtomicInteger(0);
    private boolean reconcileActive = false;

    public EliteMobsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        EliteMobsAPI.setEventBus(eventBus);
        EliteMobsAPI.setQueryAPI(new EliteMobsQueryAPI(this));

        loadOrCreateEliteMobsConfig();

        getEventRegistry().register(LoadAssetEvent.class, this::onLoadAssets);
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        nameplateService.describeSegments(this);

        registerComponents();

        abilityTriggerListener = new com.frotty27.elitemobs.systems.ability.EliteMobsAbilityTriggerListener(this);

        registerSystems();
        registerCommands();
        registerEventListeners();

        LOGGER.atInfo().log("Setup complete!");
    }

    private void registerEventListeners() {
        eventBus.registerListener(new IEliteMobsEventListener() {
            @Override
            public void onEliteMobSpawned(com.frotty27.elitemobs.api.event.EliteMobSpawnedEvent event) {
                if (event.isCancelled()) {
                    LOGGER.atInfo().log("[SpawnEvent] Spawn event cancelled, skipping health scaling");
                    return;
                }

                LOGGER.atInfo().log("[SpawnEvent] Received spawn event tier=%d healthSystem=%b modelSystem=%b",
                        event.getTier(), healthScalingSystem != null, modelScalingSystem != null);

                if (healthScalingSystem != null) {
                    com.hypixel.hytale.component.Ref<EntityStore> npcRef = event.getEntityRef();
                    com.hypixel.hytale.component.Store<EntityStore> store = npcRef.getStore();

                    com.hypixel.hytale.server.npc.entities.NPCEntity npcEntity = store.getComponent(npcRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                    LOGGER.atInfo().log("[SpawnEvent] npcEntity=%b world=%b",
                            npcEntity != null, npcEntity != null && npcEntity.getWorld() != null);

                    if (npcEntity != null && npcEntity.getWorld() != null) {
                        npcEntity.getWorld().execute(() -> {
                            LOGGER.atInfo().log("[SpawnEvent] Deferred callback executing");
                            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
                            if (entityStoreProvider == null) {
                                LOGGER.atInfo().log("[SpawnEvent] entityStoreProvider is null!");
                                return;
                            }
                            com.hypixel.hytale.component.Store<EntityStore> entityStore = entityStoreProvider.getStore();

                            com.frotty27.elitemobs.utils.StoreHelpers.withEntity(entityStore, npcRef, (chunk, commandBuffer, index) -> {
                                LOGGER.atInfo().log("[SpawnEvent] Inside withEntity - applying scaling");

                                if (healthScalingSystem != null) {
                                    healthScalingSystem.applyHealthScalingOnSpawn(npcRef, entityStore, commandBuffer);
                                }

                                if (modelScalingSystem != null) {
                                    modelScalingSystem.applyModelScalingOnSpawn(npcRef, entityStore, commandBuffer);
                                }
                            });
                        });
                    }
                }
            }
        });

        LOGGER.atInfo().log("Registered event listeners for event-driven scaling.");

        eventBus.registerListener(abilityTriggerListener);
        LOGGER.atInfo().log("Registered EliteMobsAbilityTriggerListener for event-driven ability triggers.");
    }

    private void onLoadAssets(LoadAssetEvent event) {
        try {
            loadOrCreateEliteMobsConfig();
            Path eliteMobsDirectory = getModDirectory();
            EliteMobsAssetGenerator.generateAll(eliteMobsDirectory, config, true);

            AssetPack assetPack = new AssetPack(eliteMobsDirectory,
                                           ASSET_PACK_NAME,
                                           eliteMobsDirectory,
                                           FileSystems.getDefault(),
                                           false,
                                           getManifest()
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
    }

    public synchronized void loadOrCreateEliteMobsConfig() {
        Path modDirectory = getModDirectory();

        String oldVersion = YamlSerializer.readConfigVersion(modDirectory, "core.yml", "configVersion");

        EliteMobsConfig defaults = new EliteMobsConfig();
        try {
            Object versionObj = getManifest().getVersion();
            if (versionObj != null) defaults.configVersion = versionObj.toString();
        } catch (Throwable ignored) {}

        config = YamlSerializer.loadOrCreate(modDirectory, defaults);

        if (config != null) {
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

        eliteMobsComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsTierComponent.class,
                "EliteMobsTierComponent",
                EliteMobsTierComponent.CODEC
        );
        LOGGER.atInfo().log("[1/14] Registered EliteMobsTierComponent");

        progressionComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsProgressionComponent.class,
                "EliteMobsProgressionComponent",
                EliteMobsProgressionComponent.CODEC
        );
        LOGGER.atInfo().log("[2/14] Registered EliteMobsProgressionComponent");

        healthScalingComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsHealthScalingComponent.class,
                "EliteMobsHealthScalingComponent",
                EliteMobsHealthScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[3/14] Registered EliteMobsHealthScalingComponent");

        modelScalingComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsModelScalingComponent.class,
                "EliteMobsModelScalingComponent",
                EliteMobsModelScalingComponent.CODEC
        );
        LOGGER.atInfo().log("[4/14] Registered EliteMobsModelScalingComponent");

        activeEffectsComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsActiveEffectsComponent.class,
                "EliteMobsActiveEffectsComponent",
                EliteMobsActiveEffectsComponent.CODEC
        );
        LOGGER.atInfo().log("[5/14] Registered EliteMobsActiveEffectsComponent");

        combatTrackingComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsCombatTrackingComponent.class,
                "EliteMobsCombatTrackingComponent",
                EliteMobsCombatTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[6/14] Registered EliteMobsCombatTrackingComponent (with marker-based aggro)");

        migrationComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsMigrationComponent.class,
                "EliteMobsMigrationComponent",
                EliteMobsMigrationComponent.CODEC
        );
        LOGGER.atInfo().log("[7/14] Registered EliteMobsMigrationComponent (temporary)");

        summonedMinionComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonedMinionComponent.class,
                "EliteMobsSummonedMinionComponent",
                EliteMobsSummonedMinionComponent.CODEC
        );
        LOGGER.atInfo().log("[8/14] Registered EliteMobsSummonedMinionComponent");

        summonMinionTrackingComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonMinionTrackingComponent.class,
                "EliteMobsSummonMinionTrackingComponent",
                EliteMobsSummonMinionTrackingComponent.CODEC
        );
        LOGGER.atInfo().log("[9/14] Registered EliteMobsSummonMinionTrackingComponent");

        summonRiseComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsSummonRiseComponent.class,
                "EliteMobsSummonRiseComponent",
                EliteMobsSummonRiseComponent.CODEC
        );
        LOGGER.atInfo().log("[10/14] Registered EliteMobsSummonRiseComponent");

        chargeLeapAbilityComponent = getEntityStoreRegistry().registerComponent(
                com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent.class,
                "ChargeLeapAbilityComponent",
                com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[11/14] Registered ChargeLeapAbilityComponent (unified: enabled + cooldown)");

        healLeapAbilityComponent = getEntityStoreRegistry().registerComponent(
                com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent.class,
                "HealLeapAbilityComponent",
                com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[12/14] Registered HealLeapAbilityComponent (unified: replaces 5 components)");

        summonUndeadAbilityComponent = getEntityStoreRegistry().registerComponent(
                com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent.class,
                "SummonUndeadAbilityComponent",
                com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent.CODEC
        );
        LOGGER.atInfo().log("[13/14] Registered SummonUndeadAbilityComponent (unified: replaces 4 components)");

        abilityLockComponent = getEntityStoreRegistry().registerComponent(
                EliteMobsAbilityLockComponent.class,
                "EliteMobsAbilityLockComponent",
                EliteMobsAbilityLockComponent.CODEC
        );
        LOGGER.atInfo().log("[14/14] Registered EliteMobsAbilityLockComponent");

        LOGGER.atInfo().log("Component registration complete: 14 total (10 core + 4 ability)");
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

        registerSystem(new EliteMobsComponentMigrationSystem(this));
        LOGGER.atInfo().log("Registered Migration System.");

        registerSystem(new com.frotty27.elitemobs.systems.combat.EliteMobsCombatStateSystem(this));
        registerSystem(new com.frotty27.elitemobs.systems.combat.EliteMobsAITargetPollingSystem(this));
        LOGGER.atInfo().log("Registered Combat State Systems (event-driven + AI polling).");

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

    public ComponentType<EntityStore, EliteMobsProgressionComponent> getProgressionComponent() {
        return progressionComponent;
    }

    public ComponentType<EntityStore, EliteMobsHealthScalingComponent> getHealthScalingComponent() {
        return healthScalingComponent;
    }

    public ComponentType<EntityStore, EliteMobsModelScalingComponent> getModelScalingComponent() {
        return modelScalingComponent;
    }

    public ComponentType<EntityStore, EliteMobsActiveEffectsComponent> getActiveEffectsComponent() {
        return activeEffectsComponent;
    }

    public ComponentType<EntityStore, EliteMobsCombatTrackingComponent> getCombatTrackingComponent() {
        return combatTrackingComponent;
    }

    public ComponentType<EntityStore, EliteMobsMigrationComponent> getMigrationComponent() {
        return migrationComponent;
    }

    public ComponentType<EntityStore, EliteMobsSummonedMinionComponent> getSummonedMinionComponent() {
        return summonedMinionComponent;
    }

    public ComponentType<EntityStore, EliteMobsSummonMinionTrackingComponent> getSummonMinionTrackingComponent() {
        return summonMinionTrackingComponent;
    }

    public ComponentType<EntityStore, EliteMobsSummonRiseComponent> getSummonRiseComponent() {
        return summonRiseComponent;
    }

    public ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.ChargeLeapAbilityComponent> getChargeLeapAbilityComponent() {
        return chargeLeapAbilityComponent;
    }

    public ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.HealLeapAbilityComponent> getHealLeapAbilityComponent() {
        return healLeapAbilityComponent;
    }

    public ComponentType<EntityStore, com.frotty27.elitemobs.components.ability.SummonUndeadAbilityComponent> getSummonUndeadAbilityComponent() {
        return summonUndeadAbilityComponent;
    }

    public ComponentType<EntityStore, EliteMobsAbilityLockComponent> getAbilityLockComponent() {
        return abilityLockComponent;
    }

    public com.frotty27.elitemobs.systems.ability.EliteMobsAbilityTriggerListener getAbilityTriggerListener() {
        return abilityTriggerListener;
    }

    public com.frotty27.elitemobs.systems.visual.HealthScalingSystem getHealthScalingSystem() {
        return healthScalingSystem;
    }

    public void setHealthScalingSystem(com.frotty27.elitemobs.systems.visual.HealthScalingSystem system) {
        this.healthScalingSystem = system;
    }

    public com.frotty27.elitemobs.systems.visual.ModelScalingSystem getModelScalingSystem() {
        return modelScalingSystem;
    }

    public void setModelScalingSystem(com.frotty27.elitemobs.systems.visual.ModelScalingSystem system) {
        this.modelScalingSystem = system;
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

    public EliteMobsEventBus getEventBus() {
        return eventBus;
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
            if (windowTicks > 0) {
                eventBus.fire(new com.frotty27.elitemobs.api.event.EliteMobReconcileEvent());
            }
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

}
