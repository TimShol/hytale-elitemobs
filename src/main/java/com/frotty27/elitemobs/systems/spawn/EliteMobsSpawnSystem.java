package com.frotty27.elitemobs.systems.spawn;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsSummonRiseComponent;
import com.frotty27.elitemobs.components.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.AbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonAbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonMarkerEntry;
import com.frotty27.elitemobs.equipment.EliteMobsEquipmentService;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.rules.MobRuleMatcher;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.frotty27.elitemobs.utils.Constants;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.frotty27.elitemobs.utils.WeightedIndexSelector;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.config.FlockAsset;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.*;

import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_HEAL_POTION_KEY;
import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_UNDEAD_SUMMON_KEY;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clamp01;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsSpawnSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double SUMMON_RISE_VELOCITY_Y = 3.0;
    private static final double SUMMON_RISE_SPAWN_OFFSET_Y = 0.6;
    private static final double SUMMON_SPAWN_RADIUS = 6.0;
    private static final int SUMMON_MIN_COUNT = 3;
    private static final int SUMMON_MAX_COUNT = 7;
    private static final long MINION_CHAIN_TICKS = Math.max(1L, Constants.TICKS_PER_SECOND / 3);
    private static final InteractionType HEAL_INTERACTION_TYPE = InteractionType.Ability2;

    private static final ComponentType<EntityStore, Velocity> VELOCITY_COMPONENT_TYPE = Velocity.getComponentType();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private final EliteMobsPlugin eliteMobsPlugin;
    private final Random random = new Random();
    private final EliteMobsSpawnTickHandler spawnTickHandler = new EliteMobsSpawnTickHandler(this);

    private final MobRuleMatcher mobRuleMatcher = new MobRuleMatcher();
    private final EliteMobsEquipmentService equipmentService = new EliteMobsEquipmentService();
    private final EliteMobsFeatureRegistry featureRegistry;

    private long lastReportTimestampMs = System.currentTimeMillis();
    private long mobsSeenCount;
    private long mobsMatchedCount;
    private long mobsAppliedCount;
    private long lastMinionCleanupTick = -1;
    private final Object minionRemovalLock = new Object();
    private final List<PendingMinionRemoval> pendingMinionRemovals = new ArrayList<>();
    // Minion removals are scheduled to avoid touching the store from systems.

    private record PendingMinionRemoval(Ref<EntityStore> minionRef, UUID summonerId, long scheduledTick) {
    }

    public EliteMobsSpawnSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
        this.featureRegistry = eliteMobsPlugin.getFeatureRegistry();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    public void tick(float deltaTimeSeconds, int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        spawnTickHandler.handle(deltaTimeSeconds, entityIndex, archetypeChunk, entityStore, commandBuffer);
    }

    void processTick(float deltaTimeSeconds, int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        EliteMobsConfig config = eliteMobsPlugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(entityIndex);

        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex,
                                                          Objects.requireNonNull(NPCEntity.getComponentType())
        );
        if (npcEntity == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        World world = npcEntity.getWorld();
        if (world != null) {
            long currentTick = eliteMobsPlugin.getTickClock().getTick();
            processPendingMinionRemovals(currentTick, world);
        }

        if (applySummonRiseIfNeeded(npcEntity, npcRef, entityStore, commandBuffer)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        EliteMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                    eliteMobsPlugin.getSummonedMinionComponent()
        );
        EliteMobsTierComponent existingTierComponent = entityStore.getComponent(npcRef,
                                                                                eliteMobsPlugin.getEliteMobsComponent()
        );

        if (minionComponent != null && (existingTierComponent == null || existingTierComponent.tierIndex < 0 || existingTierComponent.tierIndex > 1)) {
            int minTier = Math.max(0, minionComponent.minTierIndex);
            int maxTier = Math.max(minTier, minionComponent.maxTierIndex);
            int tierIndex = minTier + random.nextInt((maxTier - minTier) + 1);

            boolean applied = applyTierFromCommand(config,
                                                   npcRef,
                                                   entityStore,
                                                   commandBuffer,
                                                   npcEntity,
                                                   tierIndex,
                                                   true
            );
            minionComponent.tierApplied = applied;
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getSummonedMinionComponent(), minionComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        if (existingTierComponent != null && existingTierComponent.tierIndex >= 0) {
            tickExistingEliteMob(config, npcRef, entityStore, commandBuffer, existingTierComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            logNpcScanSummaryIfDue(config);
            return;
        }
        if (roleName.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsSeenCount++;

        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(config, roleName);
        if (matchResult == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsMatchedCount++;

        double[] spawnChances = resolveSpawnChancesForEnvironment(config, npcEntity);
        if (spawnChances == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        int pickedTierIndex = WeightedIndexSelector.pickWeightedIndex(spawnChances, random);
        int tierIndex = clampTierIndex(pickedTierIndex);

        mobsAppliedCount++;

        EliteMobsTierComponent newTierComponent = new EliteMobsTierComponent();
        newTierComponent.tierIndex = tierIndex;

        initializeAbilityStateIfNeeded(config, npcEntity, roleName, newTierComponent, tierIndex);

        equipmentService.buildAndApply(npcEntity, config, tierIndex, matchResult.mobRule());

        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getEliteMobsComponent(), newTierComponent);
        featureRegistry.applyAll(eliteMobsPlugin,
                                 config,
                                 npcRef,
                                 entityStore,
                                 commandBuffer,
                                 newTierComponent,
                                 roleName
        );

        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Elite applied: role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                  EliteMobsLogLevel.INFO,
                                  roleName,
                                  tierIndex,
                                  matchResult.key(),
                                  String.valueOf(matchResult.matchKind()),
                                  matchResult.score()
            );
        }

        logNpcScanSummaryIfDue(config);
    }


    private boolean applySummonRiseIfNeeded(NPCEntity npcEntity, Ref<EntityStore> npcRef,
                                            Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;
        boolean summonRole = roleName.startsWith(EliteMobsConfig.SUMMON_ROLE_PREFIX);
        EliteMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                    eliteMobsPlugin.getSummonedMinionComponent()
        );
        boolean trackedSummon = summonRole || minionComponent != null;
        if (!trackedSummon) return false;

        TransformComponent transform = entityStore.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);

        EliteMobsSummonRiseComponent riseComponent = entityStore.getComponent(npcRef,
                                                                              eliteMobsPlugin.getSummonRiseComponent()
        );
        if (riseComponent != null && riseComponent.applied) return true;

        if (transform != null) {
            TransformComponent updated = transform.clone();
            Vector3d pos = updated.getPosition();
            updated.setPosition(new Vector3d(pos.getX(), pos.getY() - SUMMON_RISE_SPAWN_OFFSET_Y, pos.getZ()));
            commandBuffer.putComponent(npcRef, TRANSFORM_COMPONENT_TYPE, updated);
        }

        Velocity velocity = entityStore.getComponent(npcRef, VELOCITY_COMPONENT_TYPE);
        if (velocity == null) {
            velocity = new Velocity();
        }
        double nextY = Math.max(velocity.getY(), SUMMON_RISE_VELOCITY_Y);
        velocity.set(velocity.getX(), nextY, velocity.getZ());
        commandBuffer.putComponent(npcRef, VELOCITY_COMPONENT_TYPE, velocity);

        if (riseComponent == null) riseComponent = new EliteMobsSummonRiseComponent();
        riseComponent.applied = true;
        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getSummonRiseComponent(), riseComponent);
        return true;
    }

    public void queueMinionChainDespawn(UUID summonerId, Store<EntityStore> entityStore, long startTick) {
        if (summonerId == null || entityStore == null) return;
        List<Ref<EntityStore>> minions = new ArrayList<>();
        entityStore.forEachChunk((chunk, cb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                EliteMobsSummonedMinionComponent minion = chunk.getComponent(i,
                                                                             eliteMobsPlugin.getSummonedMinionComponent()
                );
                if (minion == null || minion.summonerId == null) continue;
                if (!summonerId.equals(minion.summonerId)) continue;
                minions.add(ref);
            }
            return false;
        });
        if (minions.isEmpty()) return;

        long tick = startTick;
        synchronized (minionRemovalLock) {
            for (Ref<EntityStore> ref : minions) {
                pendingMinionRemovals.add(new PendingMinionRemoval(ref, summonerId, tick));
                tick += MINION_CHAIN_TICKS;
            }
        }
    }

    private void processPendingMinionRemovals(long currentTick, World world) {
        if (currentTick == lastMinionCleanupTick) return;
        lastMinionCleanupTick = currentTick;
        List<PendingMinionRemoval> ready = new ArrayList<>();
        synchronized (minionRemovalLock) {
            if (pendingMinionRemovals.isEmpty()) return;
            for (int i = pendingMinionRemovals.size() - 1; i >= 0; i--) {
                PendingMinionRemoval removal = pendingMinionRemovals.get(i);
                if (removal.scheduledTick() <= currentTick) {
                    ready.add(removal);
                    pendingMinionRemovals.remove(i);
                }
            }
        }
        if (ready.isEmpty()) return;

        for (PendingMinionRemoval removal : ready) {
            scheduleMinionRemoval(removal, world);
        }
    }

    private void scheduleMinionRemoval(PendingMinionRemoval removal, World world) {
        world.execute(() -> {
            EntityStore entityStoreProvider = world.getEntityStore();
            if (entityStoreProvider == null) return;
            Store<EntityStore> store = entityStoreProvider.getStore();
            StoreHelpers.withEntity(store, removal.minionRef(), (chunk, cb, index) -> {
                                        EliteMobsSummonedMinionComponent minion = store.getComponent(removal.minionRef(),
                                                                                                     eliteMobsPlugin.getSummonedMinionComponent()
                                        );
                                        if (minion == null || minion.summonerId == null) return;

                                        Ref<EntityStore> summonerRef = world.getEntityRef(minion.summonerId);
                                        if (summonerRef != null && summonerRef.isValid()) {
                                            StoreHelpers.withEntity(store, summonerRef, (summonerChunk, summonerCb, summonerIndex) -> {
                                                                        EliteMobsTierComponent summonerTier = store.getComponent(summonerRef,
                                                                                                                                 eliteMobsPlugin.getEliteMobsComponent()
                                                                        );
                                                                        if (summonerTier == null) return;
                                                                        summonerTier.summonedAliveCount = Math.max(0, summonerTier.summonedAliveCount - 1);
                                                                        summonerCb.replaceComponent(summonerRef, eliteMobsPlugin.getEliteMobsComponent(), summonerTier);
                                                                    }
                                            );
                                        }

                                        cb.removeEntity(removal.minionRef(), RemoveReason.REMOVE);
                                    }
            );
        });
    }

    public boolean applyTierFromCommand(EliteMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                        CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity, int tierIndex) {
        return applyTierFromCommand(config, npcRef, entityStore, commandBuffer, npcEntity, tierIndex, false);
    }

    public boolean applyTierFromCommand(EliteMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                        CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity, int tierIndex,
                                        boolean disableDrops) {
        if (config == null || npcEntity == null) return false;

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;

        int clampedTierIndex = clampTierIndex(tierIndex);

        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(config, roleName);

        EliteMobsTierComponent newTierComponent = new EliteMobsTierComponent();
        newTierComponent.tierIndex = clampedTierIndex;
        newTierComponent.disableDrops = disableDrops;

        initializeAbilityStateIfNeeded(config, npcEntity, roleName, newTierComponent, clampedTierIndex);

        if (matchResult != null) {
            equipmentService.buildAndApply(npcEntity, config, clampedTierIndex, matchResult.mobRule());
        }

        commandBuffer.putComponent(npcRef, eliteMobsPlugin.getEliteMobsComponent(), newTierComponent);
        featureRegistry.applyAll(eliteMobsPlugin,
                                 config,
                                 npcRef,
                                 entityStore,
                                 commandBuffer,
                                 newTierComponent,
                                 roleName
        );

        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Elite applied (command): role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                  EliteMobsLogLevel.INFO,
                                  roleName,
                                  clampedTierIndex,
                                  matchResult == null ? "none" : matchResult.key(),
                                  matchResult == null ? "none" : String.valueOf(matchResult.matchKind()),
                                  matchResult == null ? -1 : matchResult.score()
            );
        }

        return true;
    }

    private double[] resolveSpawnChancesForEnvironment(EliteMobsConfig config, NPCEntity npcEntity) {
        if (!config.spawning.enableEnvironmentTierSpawns) return config.spawning.spawnChancePerTier;
        if (config.spawning.environmentTierSpawns == null || config.spawning.environmentTierSpawns.isEmpty())
            return config.spawning.spawnChancePerTier;

        int envIndex = npcEntity.getEnvironment();
        Environment env = Environment.getAssetMap().getAsset(envIndex);
        String envId = env == null ? null : env.getId();

        EliteMobsConfig.EnvironmentTierRule rule = null;
        if (envId != null) {
            rule = config.spawning.environmentTierSpawns.get(envId);
        }
        if (rule == null) {
            rule = config.spawning.environmentTierSpawns.get("default");
        }
        if (rule == null) return config.spawning.spawnChancePerTier;
        if (!rule.enabled) return null;
        if (rule.spawnChancePerTier == null || rule.spawnChancePerTier.length == 0)
            return config.spawning.spawnChancePerTier;
        return rule.spawnChancePerTier;
    }

    private void tickExistingEliteMob(EliteMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                      CommandBuffer<EntityStore> commandBuffer, EliteMobsTierComponent tierComponent) {

        int tierIndex = clampTierIndex(tierComponent.tierIndex);

        boolean tierComponentChanged = false;
        long currentTick = eliteMobsPlugin.getTickClock().getTick();

        String roleName = null;
        NPCEntity npcEntity = entityStore.getComponent(npcRef, NPCEntity.getComponentType());
        if (npcEntity != null) roleName = npcEntity.getRoleName();

        tierComponentChanged |= initializeAbilityStateIfNeeded(config, npcEntity, roleName, tierComponent, tierIndex);

        tierComponentChanged |= tryHandlePendingSummon(config,
                                                       npcRef,
                                                       npcEntity,
                                                       entityStore,
                                                       tierComponent,
                                                       currentTick
        );

        tierComponentChanged |= finalizeHealSwapIfNeeded(npcRef,
                                                         npcEntity,
                                                         entityStore,
                                                         commandBuffer,
                                                         tierComponent,
                                                         currentTick
        );

        if (eliteMobsPlugin.shouldReconcileThisTick()) {
            featureRegistry.reconcileAll(eliteMobsPlugin,
                                         config,
                                         npcRef,
                                         entityStore,
                                         commandBuffer,
                                         tierComponent,
                                         roleName
            );
        }

        // Ticking features is now handled by EliteMobsAbilityTickSystem (or renamed EliteMobsFeatureTickSystem)
        // calling featureRegistry.tickAll(...)

        if (tierComponentChanged) {
            commandBuffer.replaceComponent(npcRef, eliteMobsPlugin.getEliteMobsComponent(), tierComponent);
        }
    }

    private boolean tryHandlePendingSummon(EliteMobsConfig config, Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                           Store<EntityStore> entityStore, EliteMobsTierComponent tierComponent,
                                           long currentTick) {
        boolean changed = false;
        if (tierComponent.pendingSummonTick < 0 || currentTick < tierComponent.pendingSummonTick) return false;
        tierComponent.pendingSummonTick = -1;
        changed = true;

        SummonAbilityConfig summonConfig = getSummonAbilityConfig(config);
        if (summonConfig == null || npcEntity == null) {
            tierComponent.pendingSummonRoleIdentifier = null;
            return changed;
        }

        String roleIdentifier = tierComponent.pendingSummonRoleIdentifier;
        tierComponent.pendingSummonRoleIdentifier = null;
        changed = true;

        List<SummonMarkerEntry> entries = resolveSummonEntries(summonConfig, roleIdentifier);
        if (entries.isEmpty()) {
            if (config.debug.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: no entries for role=%s",
                                      EliteMobsLogLevel.INFO,
                                      String.valueOf(roleIdentifier)
                );
            }
            return changed;
        }

        int maxAlive = clampSummonMaxAlive(summonConfig.maxAlive);
        int remaining = Math.max(0, maxAlive - tierComponent.summonedAliveCount);
        if (remaining <= 0) {
            if (config.debug.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: cap reached alive=%d max=%d",
                                      EliteMobsLogLevel.INFO,
                                      tierComponent.summonedAliveCount,
                                      maxAlive
                );
            }
            return changed;
        }

        TransformComponent transform = entityStore.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);
        if (transform == null) return changed;

        Vector3d center = transform.getPosition();
        int spawnCount = clampSummonCount(pickFlockSize(entries));
        spawnCount = Math.min(spawnCount, remaining);
        if (spawnCount <= 0) {
            if (config.debug.isDebugModeEnabled) {
                EliteMobsLogger.debug(LOGGER,
                                      "Pending summon skipped: spawnCount=0 remaining=%d",
                                      EliteMobsLogLevel.INFO,
                                      remaining
                );
            }
            return changed;
        }

        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(LOGGER,
                                  "Spawning minions: role=%s count=%d remaining=%d",
                                  EliteMobsLogLevel.INFO,
                                  String.valueOf(roleIdentifier),
                                  spawnCount,
                                  remaining
            );
        }

        if (tierComponent.summonedAliveCount < 0) tierComponent.summonedAliveCount = 0;
        tierComponent.summonedAliveCount += spawnCount;
        changed = true;
        var world = npcEntity.getWorld();
        if (world == null) return changed;
        var entityStoreProvider = world.getEntityStore();
        if (entityStoreProvider == null) return changed;
        UUIDComponent uuidComponent = entityStore.getComponent(npcRef, UUIDComponent.getComponentType());
        if (uuidComponent == null && npcEntity.getReference() != null) {
            Ref<EntityStore> ref = npcEntity.getReference();
            uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
        }
        UUID summonerId = uuidComponent == null ? null : uuidComponent.getUuid();
        if (summonerId == null) return changed;

        for (int i = 0; i < spawnCount; i++) {
            SummonMarkerEntry entry = pickWeightedEntry(entries);
            if (entry == null || entry.Name == null || entry.Name.isBlank()) continue;
            Vector3d pos = pickSpawnPosition(center);
            String roleName = entry.Name.trim();
            roleName = EliteMobsConfig.buildSummonVariantRoleId(roleName);
            String flockId = entry.Flock;
            final String roleNameFinal = roleName;
            final String flockIdFinal = flockId;
            final Vector3d posFinal = pos;
            world.execute(() -> {
                var spawned = NPCPlugin.get().spawnNPC(entityStoreProvider.getStore(),
                                                       roleNameFinal,
                                                       flockIdFinal,
                                                       posFinal,
                                                       new Vector3f(0f, 0f, 0f)
                );
                if (spawned == null || spawned.first() == null) {
                    if (config.debug.isDebugModeEnabled) {
                        EliteMobsLogger.debug(LOGGER,
                                              "Spawn failed: role=%s flock=%s",
                                              EliteMobsLogLevel.INFO,
                                              roleNameFinal,
                                              String.valueOf(flockIdFinal)
                        );
                    }
                    return;
                }
                Ref<EntityStore> spawnedRef = spawned.first();
                EntityStore storeProvider = world.getEntityStore();
                if (storeProvider == null) return;
                Store<EntityStore> store = storeProvider.getStore();

                StoreHelpers.withEntity(store, spawnedRef, (chunk, cb, index) -> {
                                            EliteMobsSummonedMinionComponent minion = store.getComponent(spawnedRef,
                                                                                                         eliteMobsPlugin.getSummonedMinionComponent()
                                            );
                                            if (minion == null) {
                                                minion = new EliteMobsSummonedMinionComponent();
                                            }
                                            minion.summonerId = summonerId;
                                            minion.minTierIndex = 0;
                                            minion.maxTierIndex = 1;
                                            minion.tierApplied = false;
                                            cb.putComponent(spawnedRef, eliteMobsPlugin.getSummonedMinionComponent(), minion);
                                        }
                );
            });
        }

        return changed;
    }

    private boolean finalizeHealSwapIfNeeded(Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                             Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                             EliteMobsTierComponent tierComponent, long currentTick) {
        if (npcEntity == null || tierComponent == null) return false;
        if (!tierComponent.healSwapActive) return false;

        boolean healChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore, npcRef, HEAL_INTERACTION_TYPE);

        if (tierComponent.healSwapRestoreTick >= 0 && currentTick >= tierComponent.healSwapRestoreTick) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
            return true;
        }

        if (!healChainActive && tierComponent.healInProgress) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
            return true;
        }

        return false;
    }

    private static int clampSummonCount(int count) {
        int clamped = Math.max(SUMMON_MIN_COUNT, count);
        return Math.min(SUMMON_MAX_COUNT, clamped);
    }

    private static int clampSummonMaxAlive(int value) {
        if (value < EliteMobsConfig.SUMMON_MAX_ALIVE_MIN) return EliteMobsConfig.SUMMON_MAX_ALIVE_MIN;
        if (value > EliteMobsConfig.SUMMON_MAX_ALIVE_MAX) return EliteMobsConfig.SUMMON_MAX_ALIVE_MAX;
        return value;
    }

    private SummonAbilityConfig getSummonAbilityConfig(EliteMobsConfig config) {
        AbilityConfig abilityConfig = getAbilityConfig(config, ABILITY_UNDEAD_SUMMON_KEY);
        if (abilityConfig instanceof SummonAbilityConfig summonAbilityConfig) return summonAbilityConfig;
        return null;
    }

    private List<SummonMarkerEntry> resolveSummonEntries(SummonAbilityConfig config, String roleIdentifier) {
        if (config == null) return List.of();
        String normalized = roleIdentifier == null ? "" : EliteMobsConfig.normalizeRoleIdentifier(roleIdentifier);
        if (config.spawnMarkerEntriesByRole != null && !normalized.isBlank()) {
            List<SummonMarkerEntry> byRole = config.spawnMarkerEntriesByRole.get(normalized);
            if (byRole != null && !byRole.isEmpty()) return byRole;
        }
        if (config.spawnMarkerEntriesByRole != null) {
            List<SummonMarkerEntry> fallback = config.spawnMarkerEntriesByRole.get("default");
            if (fallback != null && !fallback.isEmpty()) return fallback;
            fallback = config.spawnMarkerEntriesByRole.get("Default");
            if (fallback != null && !fallback.isEmpty()) return fallback;
        }
        if (config.spawnMarkerEntries != null && !config.spawnMarkerEntries.isEmpty()) return config.spawnMarkerEntries;
        return List.of();
    }

    private int pickFlockSize(List<SummonMarkerEntry> entries) {
        String flockId = null;
        for (SummonMarkerEntry entry : entries) {
            if (entry != null && entry.Flock != null && !entry.Flock.isBlank()) {
                flockId = entry.Flock.trim();
                break;
            }
        }
        if (flockId == null) return 1;
        FlockAsset flock = FlockAsset.getAssetMap().getAsset(flockId);
        if (flock == null) return 1;
        return Math.max(1, flock.pickFlockSize());
    }

    private SummonMarkerEntry pickWeightedEntry(List<SummonMarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;
        double total = 0.0;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null) continue;
            total += Math.max(0.0, entry.Weight);
        }
        if (total <= 0.0) return entries.get(0);
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null) continue;
            cumulative += Math.max(0.0, entry.Weight);
            if (roll <= cumulative) return entry;
        }
        return entries.get(entries.size() - 1);
    }

    private Vector3d pickSpawnPosition(Vector3d center) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * SUMMON_SPAWN_RADIUS;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        return new Vector3d(center.getX() + dx, center.getY(), center.getZ() + dz);
    }

    private boolean initializeAbilityStateIfNeeded(EliteMobsConfig config, NPCEntity npcEntity, String roleName,
                                                   EliteMobsTierComponent tierComponent, int tierIndex) {
        if (config == null || npcEntity == null || roleName == null || roleName.isBlank()) return false;

        boolean changed = false;

        AbilityConfig healConfig = getAbilityConfig(config, ABILITY_HEAL_POTION_KEY);
        if (healConfig != null && !tierComponent.healAbilityRollInitialized) {
            tierComponent.healAbilityEnabledRoll = rollAbilityEnabled(healConfig, roleName, tierIndex);
            tierComponent.healAbilityRollInitialized = true;
            changed = true;
        }

        if (tierComponent.healAbilityEnabledRoll && !tierComponent.healThresholdInitialized) {
            float min = 0.1f;
            float max = 0.4f;
            if (healConfig instanceof EliteMobsConfig.HealAbilityConfig healAbilityConfig) {
                min = healAbilityConfig.minHealthTriggerPercent;
                max = healAbilityConfig.maxHealthTriggerPercent;
            }
            tierComponent.healTriggerPercent = AbilityHelpers.rollPercentInRange(random, min, max, 0.5f);
            tierComponent.healThresholdInitialized = true;
            changed = true;
        }

        AbilityConfig summonConfig = getAbilityConfig(config, ABILITY_UNDEAD_SUMMON_KEY);
        if (summonConfig != null && !tierComponent.summonAbilityRollInitialized) {
            tierComponent.summonAbilityEnabledRoll = rollAbilityEnabled(summonConfig, roleName, tierIndex);
            tierComponent.summonAbilityRollInitialized = true;
            changed = true;
        }

        return changed;
    }

    private boolean rollAbilityEnabled(AbilityConfig abilityConfig, String roleName, int tierIndex) {
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(abilityConfig, tierIndex)) return false;
        if (!AbilityGateEvaluator.isAllowed(abilityConfig, roleName, "", tierIndex)) return false;
        float chance = AssetConfigHelpers.getFloatForTier(abilityConfig.chancePerTier, tierIndex, 1f);
        return random.nextFloat() < clamp01(chance);
    }

    private static AbilityConfig getAbilityConfig(EliteMobsConfig config, String key) {
        return (AbilityConfig) AssetConfigHelpers.getAssetConfig(config, AssetType.ABILITIES, key);
    }

    // ----------------- model scaling -----------------

    // ----------------- reporting -----------------

    private void logNpcScanSummaryIfDue(EliteMobsConfig config) {
        if (!config.debug.isDebugModeEnabled) return;

        long now = System.currentTimeMillis();
        long everyMs = Math.max(1, config.debug.debugMobRuleScanIntervalSeconds) * 1000L;
        if (now - lastReportTimestampMs < everyMs) return;

        lastReportTimestampMs = now;

        EliteMobsLogger.debug(LOGGER,
                              "Scan: seen=%d matched=%d applied=%d",
                              EliteMobsLogLevel.INFO,
                              mobsSeenCount,
                              mobsMatchedCount,
                              mobsAppliedCount
        );
    }

    // ----------------- abilities -----------------
}
