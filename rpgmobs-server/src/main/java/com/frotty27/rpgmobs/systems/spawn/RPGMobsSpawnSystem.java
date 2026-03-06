package com.frotty27.rpgmobs.systems.spawn;

import com.frotty27.rpgmobs.api.events.RPGMobsSpawnedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsMigrationComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonRiseComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig.AbilityConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig.SummonAbilityConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig.SummonMarkerEntry;
import com.frotty27.rpgmobs.config.overlay.ConfigOverlay;
import com.frotty27.rpgmobs.config.overlay.ConfigResolver;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.exceptions.EntityComponentException;
import com.frotty27.rpgmobs.exceptions.RPGMobsException;
import com.frotty27.rpgmobs.exceptions.RPGMobsSystemException;
import com.frotty27.rpgmobs.features.RPGMobsFeatureRegistry;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.rules.MobRuleMatcher;
import com.frotty27.rpgmobs.services.RPGMobsEquipmentService;
import com.frotty27.rpgmobs.utils.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembershipSystems;
import com.hypixel.hytale.server.flock.FlockPlugin;
import com.hypixel.hytale.server.flock.config.FlockAsset;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.frotty27.rpgmobs.systems.ability.AbilityIds.SUMMON_UNDEAD;
import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public final class RPGMobsSpawnSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double SUMMON_RISE_VELOCITY_Y = 3.0;
    private static final double SUMMON_RISE_SPAWN_OFFSET_Y = 0.6;
    private static final double SUMMON_SPAWN_RADIUS = 6.0;
    private static final int SUMMON_MIN_COUNT = 3;
    private static final int SUMMON_MAX_COUNT = 7;
    private static final InteractionType ABILITY_INTERACTION_TYPE = InteractionType.Ability2;

    private static final long CHAIN_DEATH_STAGGER_TICKS = Constants.TICKS_PER_SECOND / 3;

    private static final String CHAIN_DEATH_PARTICLE = "Explosion_Medium";

    private static final ComponentType<EntityStore, Velocity> VELOCITY_COMPONENT_TYPE = Velocity.getComponentType();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private final RPGMobsPlugin RPGMobsPlugin;
    private final Random random = new Random();

    private final MobRuleMatcher mobRuleMatcher = new MobRuleMatcher();
    private final RPGMobsEquipmentService equipmentService = new RPGMobsEquipmentService();
    private final RPGMobsFeatureRegistry featureRegistry;

    private long lastReportTimestampMs = System.currentTimeMillis();
    private long mobsSeenCount;
    private long mobsMatchedCount;
    private long mobsAppliedCount;
    private final Object minionRemovalLock = new Object();
    private final List<PendingSummonerDeath> pendingSummonerDeaths = new ArrayList<>();

    private static final class PendingSummonerDeath {
        final UUID summonerId;
        final long deathTick;
        int assignedCount;

        PendingSummonerDeath(UUID summonerId, long deathTick) {
            this.summonerId = summonerId;
            this.deathTick = deathTick;
            this.assignedCount = 0;
        }
    }

    public RPGMobsSpawnSystem(RPGMobsPlugin RPGMobsPlugin) {
        this.RPGMobsPlugin = RPGMobsPlugin;
        this.featureRegistry = RPGMobsPlugin.getFeatureRegistry();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Constants.NPC_COMPONENT_TYPE);
    }

    @Override
    public void tick(float deltaTimeSeconds, int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        try {
            NPCEntity npc = archetypeChunk.getComponent(entityIndex, Constants.NPC_COMPONENT_TYPE);
            if (npc == null) {
                throw new EntityComponentException("NPCEntity", entityIndex);
            }
            processTick(entityIndex, archetypeChunk, entityStore, commandBuffer);
        } catch (RPGMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new RPGMobsSystemException("Error in RPGMobsSpawnSystem tick", e);
        }
    }

    private void processTick(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                             @NonNull Store<EntityStore> entityStore,
                             @NonNull CommandBuffer<EntityStore> commandBuffer) {

        RPGMobsConfig config = RPGMobsPlugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(entityIndex);

        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex, Constants.NPC_COMPONENT_TYPE);
        if (npcEntity == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        long currentTick = RPGMobsPlugin.getTickClock().getTick();

        checkMinionOfDeadSummoner(npcRef, entityStore, commandBuffer, currentTick);
        clearProcessedSummonerDeaths(currentTick);

        if (applySummonRiseIfNeeded(npcEntity, npcRef, entityStore, commandBuffer)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        RPGMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                  RPGMobsPlugin.getSummonedMinionComponentType()
        );
        RPGMobsTierComponent existingTierComponent = entityStore.getComponent(npcRef,
                                                                              RPGMobsPlugin.getRPGMobsComponentType()
        );

        if (minionComponent != null && (existingTierComponent == null || existingTierComponent.tierIndex < 0 || existingTierComponent.tierIndex > 1)) {
            int minTier = Math.max(0, minionComponent.minTierIndex);
            int maxTier = Math.max(minTier, minionComponent.maxTierIndex);
            int tierIndex = minTier + random.nextInt((maxTier - minTier) + 1);

            minionComponent.tierApplied = applyTierFromCommand(config,
                                                               npcRef,
                                                               entityStore,
                                                               commandBuffer,
                                                               npcEntity,
                                                               tierIndex
            );
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonedMinionComponentType(), minionComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        if (existingTierComponent != null && existingTierComponent.tierIndex >= 0) {
            tickExistingRPGMob(config, npcRef, entityStore, commandBuffer, existingTierComponent);
            logNpcScanSummaryIfDue(config);
            return;
        }

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            logNpcScanSummaryIfDue(config);
            return;
        }
        if (roleName.startsWith(RPGMobsConfig.SUMMON_ROLE_PREFIX)) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsSeenCount++;

        String spawnWorldNameForMobRule = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolvedForMobRule = RPGMobsPlugin.getResolvedConfig(spawnWorldNameForMobRule);
        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(resolvedForMobRule.mobRules, roleName);
        if (matchResult == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        mobsMatchedCount++;

        double[] spawnChances = resolveSpawnChances(config, npcEntity);
        if (spawnChances == null) {
            logNpcScanSummaryIfDue(config);
            return;
        }

        int pickedTierIndex = WeightedIndexSelector.pickWeightedIndex(spawnChances, random);
        int tierIndex = clampTierIndex(pickedTierIndex);

        String matchedRuleKey = matchResult.key();
        int overrideTier = applyTierOverride(npcEntity, matchedRuleKey, tierIndex);
        if (overrideTier != tierIndex) {
            tierIndex = overrideTier;
        }

        mobsAppliedCount++;

        RPGMobsTierComponent newTierComponent = new RPGMobsTierComponent();
        newTierComponent.tierIndex = tierIndex;
        newTierComponent.matchedRuleKey = matchedRuleKey;
        newTierComponent.lastReconciledAt = RPGMobsPlugin.getConfigReloadCount();

        equipmentService.buildAndApply(npcEntity, config, tierIndex, matchResult.mobRule(),
                resolvedForMobRule.droppedGearDurabilityMin, resolvedForMobRule.droppedGearDurabilityMax);

        TransformComponent spawnTransform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        var spawnedEvent = new RPGMobsSpawnedEvent(npcEntity.getWorld(),
                                                   npcRef,
                                                   tierIndex,
                                                   roleName,
                                                   spawnTransform != null ? spawnTransform.getPosition().clone() : new Vector3d()
        );
        RPGMobsPlugin.getEventBus().fire(spawnedEvent);
        if (spawnedEvent.isCancelled()) return;

        commandBuffer.putComponent(npcRef, RPGMobsPlugin.getRPGMobsComponentType(), newTierComponent);

        createNewSchemaComponents(config, npcRef, commandBuffer, npcEntity);
        featureRegistry.applyAll(RPGMobsPlugin, config, resolvedForMobRule, npcRef, entityStore, commandBuffer, newTierComponent, roleName);

        if (config.debugConfig.isDebugModeEnabled) {
            RPGMobsLogger.debug(LOGGER,
                                "Elite applied: role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                RPGMobsLogLevel.INFO,
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
        boolean summonRole = roleName.startsWith(RPGMobsConfig.SUMMON_ROLE_PREFIX);
        RPGMobsSummonedMinionComponent minionComponent = entityStore.getComponent(npcRef,
                                                                                  RPGMobsPlugin.getSummonedMinionComponentType()
        );
        boolean trackedSummon = summonRole || minionComponent != null;
        if (!trackedSummon) return false;

        TransformComponent transform = entityStore.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);

        RPGMobsSummonRiseComponent riseComponent = entityStore.getComponent(npcRef,
                                                                            RPGMobsPlugin.getSummonRiseComponentType()
        );
        if (riseComponent != null && riseComponent.applied) return false;

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

        if (riseComponent == null) riseComponent = new RPGMobsSummonRiseComponent();
        riseComponent.applied = true;
        commandBuffer.putComponent(npcRef, RPGMobsPlugin.getSummonRiseComponentType(), riseComponent);
        return true;
    }

    public void queueSummonerDeath(UUID summonerId, long deathTick) {
        if (summonerId == null) return;
        synchronized (minionRemovalLock) {
            pendingSummonerDeaths.add(new PendingSummonerDeath(summonerId, deathTick));
        }
    }

    void checkMinionOfDeadSummoner(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer, long currentTick) {
        synchronized (minionRemovalLock) {
            if (pendingSummonerDeaths.isEmpty()) return;
        }
        RPGMobsSummonedMinionComponent minion = store.getComponent(npcRef,
                                                                   RPGMobsPlugin.getSummonedMinionComponentType()
        );
        if (minion == null || minion.summonerId == null) return;

        DeathComponent existingDeath = store.getComponent(npcRef, DeathComponent.getComponentType());
        if (existingDeath != null) return;

        if (minion.chainDeathAtTick == -1L) {

            return;
        }

        if (minion.chainDeathAtTick > 0) {

            if (currentTick >= minion.chainDeathAtTick) {
                RPGMobsLogger.debug(LOGGER,
                                    "[MinionDespawn] Chain-death firing: summoner=%s tick=%d",
                                    RPGMobsLogLevel.INFO,
                                    minion.summonerId,
                                    currentTick
                );
                spawnChainDeathExplosion(npcRef, store);
                killMinion(npcRef, commandBuffer);
                minion.chainDeathAtTick = -1L;
                commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonedMinionComponentType(), minion);
            }
            return;
        }

        synchronized (minionRemovalLock) {
            for (PendingSummonerDeath death : pendingSummonerDeaths) {
                if (death.summonerId.equals(minion.summonerId)) {
                    long scheduledTick = death.deathTick + (death.assignedCount * CHAIN_DEATH_STAGGER_TICKS) + 1;
                    death.assignedCount++;
                    minion.chainDeathAtTick = scheduledTick;
                    commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonedMinionComponentType(), minion);
                    RPGMobsLogger.debug(LOGGER,
                                        "[MinionDespawn] Scheduled chain-death: summoner=%s atTick=%d (index=%d)",
                                        RPGMobsLogLevel.INFO,
                                        minion.summonerId,
                                        scheduledTick,
                                        death.assignedCount - 1
                    );
                    return;
                }
            }
        }
    }

    void clearProcessedSummonerDeaths(long currentTick) {
        synchronized (minionRemovalLock) {
            pendingSummonerDeaths.removeIf(d -> currentTick - d.deathTick > Constants.TICKS_PER_SECOND * 5);
        }
    }

    private void spawnChainDeathExplosion(Ref<EntityStore> npcRef, Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(npcRef, TRANSFORM_COMPONENT_TYPE);
        if (transform == null) return;
        Vector3d pos = transform.getPosition();
        ParticleUtil.spawnParticleEffect(CHAIN_DEATH_PARTICLE, pos, store);
    }

    private void killMinion(Ref<EntityStore> npcRef, CommandBuffer<EntityStore> commandBuffer) {
        Damage damage = new Damage(Damage.NULL_SOURCE, DamageCause.ENVIRONMENT, Float.MAX_VALUE);
        DeathComponent.tryAddComponent(commandBuffer, npcRef, damage);
    }

    private void deElite(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                         CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                         @Nullable NPCEntity npcEntity) {
        if (npcEntity != null) {
            equipmentService.clearAllEquipment(npcEntity);
        }

        featureRegistry.cleanupAll(RPGMobsPlugin, config, npcRef, entityStore, commandBuffer);

        commandBuffer.tryRemoveComponent(npcRef, RPGMobsPlugin.getRPGMobsComponentType());
        commandBuffer.tryRemoveComponent(npcRef, RPGMobsPlugin.getProgressionComponentType());
        commandBuffer.tryRemoveComponent(npcRef, RPGMobsPlugin.getMigrationComponentType());
        commandBuffer.tryRemoveComponent(npcRef, RPGMobsPlugin.getSummonedMinionComponentType());
        commandBuffer.tryRemoveComponent(npcRef, RPGMobsPlugin.getSummonRiseComponentType());

        RPGMobsLogger.debug(LOGGER,
                            "[DeElite] Stripped elite status: role=%s tier=T%d",
                            RPGMobsLogLevel.INFO,
                            npcEntity != null ? npcEntity.getRoleName() : "unknown",
                            tierComponent.tierIndex + 1
        );
    }

    public boolean applyTierFromCommand(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                        CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity, int tierIndex) {
        if (config == null || npcEntity == null) return false;

        String roleName = npcEntity.getRoleName();
        if (roleName == null || roleName.isBlank()) return false;

        int clampedTierIndex = clampTierIndex(tierIndex);

        String cmdWorldNameForMobRule = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolvedForMobRule = RPGMobsPlugin.getResolvedConfig(cmdWorldNameForMobRule);
        MobRuleMatcher.MatchResult matchResult = mobRuleMatcher.findBestMatch(resolvedForMobRule.mobRules, roleName);

        if (matchResult == null) {
            RPGMobsLogger.debug(LOGGER,
                                "Command spawn skipped — no enabled mob rule for role=%s in world=%s",
                                RPGMobsLogLevel.INFO, roleName, String.valueOf(cmdWorldNameForMobRule));
            return false;
        }

        RPGMobsTierComponent newTierComponent = new RPGMobsTierComponent();
        newTierComponent.tierIndex = clampedTierIndex;
        newTierComponent.matchedRuleKey = matchResult.key();
        newTierComponent.lastReconciledAt = RPGMobsPlugin.getConfigReloadCount();

        equipmentService.buildAndApply(npcEntity, config, clampedTierIndex, matchResult.mobRule(),
                resolvedForMobRule.droppedGearDurabilityMin, resolvedForMobRule.droppedGearDurabilityMax);

        commandBuffer.putComponent(npcRef, RPGMobsPlugin.getRPGMobsComponentType(), newTierComponent);

        createNewSchemaComponents(config, npcRef, commandBuffer, npcEntity);

        featureRegistry.applyAll(RPGMobsPlugin, config, resolvedForMobRule, npcRef, entityStore, commandBuffer, newTierComponent, roleName);

        TransformComponent spawnTransform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());
        RPGMobsPlugin.getEventBus().fire(new RPGMobsSpawnedEvent(npcEntity.getWorld(),
                                                                 npcRef,
                                                                 clampedTierIndex,
                                                                 roleName,
                                                                 spawnTransform != null ? spawnTransform.getPosition().clone() : new Vector3d()
        ));

        if (config.debugConfig.isDebugModeEnabled) {
            RPGMobsLogger.debug(LOGGER,
                                "Elite applied (command): role=%s tier=%d ruleKey=%s matchKind=%s score=%d",
                                RPGMobsLogLevel.INFO,
                                roleName,
                                clampedTierIndex,
                                matchResult == null ? "none" : matchResult.key(),
                                matchResult == null ? "none" : String.valueOf(matchResult.matchKind()),
                                matchResult == null ? -1 : matchResult.score()
            );
        }

        return true;
    }

    private double @Nullable [] resolveSpawnChances(RPGMobsConfig config, NPCEntity npcEntity) {
        String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);

        if (config.debugConfig.isDebugModeEnabled) {
            String template = ConfigResolver.resolveInstanceTemplate(worldName);
            RPGMobsLogger.debug(LOGGER,
                                "[InstanceRule] worldName='%s' template='%s'",
                                RPGMobsLogLevel.INFO,
                                String.valueOf(worldName),
                                String.valueOf(template)
            );
        }

        if (!resolved.enabled) return null;

        if (resolved.hasCustomSpawnChances) {
            return resolved.spawnChancePerTier;
        }

        return resolveSpawnChancesForStyle(config, npcEntity, resolved.progressionStyle);
    }

    private int applyTierOverride(NPCEntity npcEntity, String matchedRuleKey, int tierIndex) {
        String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);

        ConfigOverlay.TierOverride tierOverride = resolved.tierOverrides.get(matchedRuleKey);

        if (tierOverride == null) {
            for (var entry : resolved.tierOverrides.entrySet()) {
                if (MobRuleCategoryHelpers.isCategoryKey(entry.getKey())) {
                    String catName = MobRuleCategoryHelpers.fromCategoryKey(entry.getKey());
                    if (MobRuleCategoryHelpers.isMobKeyInCategory(
                            resolved.mobRuleCategoryTree, catName, matchedRuleKey)) {
                        tierOverride = entry.getValue();
                        break;
                    }
                }
            }
        }

        if (tierOverride == null) return tierIndex;

        if (tierIndex >= 0 && tierIndex < tierOverride.allowedTiers.length && tierOverride.allowedTiers[tierIndex]) {
            return tierIndex;
        }
        for (int i = 0; i < tierOverride.allowedTiers.length; i++) {
            if (tierOverride.allowedTiers[i]) return clampTierIndex(i);
        }
        return tierIndex;
    }

    private double @Nullable [] resolveSpawnChancesForStyle(RPGMobsConfig config, NPCEntity npcEntity,
                                                           RPGMobsConfig.ProgressionStyle style) {
        if (style == RPGMobsConfig.ProgressionStyle.DISTANCE_FROM_SPAWN) {
            return resolveSpawnChancesByDistance(npcEntity);
        }
        if (style == RPGMobsConfig.ProgressionStyle.NONE) {
            String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
            return RPGMobsPlugin.getResolvedConfig(worldName).spawnChancePerTier;
        }
        return resolveSpawnChancesForEnvironment(config, npcEntity);
    }

    private double[] resolveSpawnChancesByDistance(NPCEntity npcEntity) {
        double dist = getXZDistance(npcEntity);
        String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);

        double distPerTier = Math.max(1.0, resolved.distancePerTier);
        int tier = (int) (dist / distPerTier);
        tier = clampTierIndex(tier);

        double[] chances = new double[Constants.TIERS_AMOUNT];
        chances[tier] = 1.0;
        return chances;
    }

    private double getXZDistance(NPCEntity npcEntity) {
        if (npcEntity == null) return 0.0;

        Ref<EntityStore> ref = npcEntity.getReference();
        if (ref == null) return 0.0;
        Store<EntityStore> store = ref.getStore();
        if (store == null) return 0.0;
        TransformComponent t = store.getComponent(ref, TRANSFORM_COMPONENT_TYPE);
        if (t == null) return 0.0;

        Vector3d pos = t.getPosition();
        return Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
    }

    private String resolveEnvironmentId(NPCEntity npcEntity) {
        RPGMobsConfig config = RPGMobsPlugin.getConfig();

        var world = npcEntity.getWorld();
        if (world != null) {
            Ref<EntityStore> ref = npcEntity.getReference();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            TransformComponent transform = store != null
                    ? store.getComponent(ref, TRANSFORM_COMPONENT_TYPE) : null;
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
                WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
                if (chunk != null) {
                    int envIndex = chunk.getBlockChunk().getEnvironment(pos);
                    if (envIndex != 0) {
                        Environment env = Environment.getAssetMap().getAsset(envIndex);
                        if (env != null) {
                            if (config != null && config.debugConfig.isDebugModeEnabled) {
                                RPGMobsLogger.debug(LOGGER,
                                                    "[EnvResolve] position lookup: envId='%s' role='%s'",
                                                    RPGMobsLogLevel.INFO,
                                                    env.getId(),
                                                    npcEntity.getRoleName() != null ? npcEntity.getRoleName() : "?"
                                );
                            }
                            return env.getId();
                        }
                    }
                }
            }
        }

        int cachedIndex = npcEntity.getEnvironment();
        if (cachedIndex != 0) {
            Environment env = Environment.getAssetMap().getAsset(cachedIndex);
            if (env != null) {
                if (config != null && config.debugConfig.isDebugModeEnabled) {
                    RPGMobsLogger.debug(LOGGER,
                                        "[EnvResolve] cached fallback: envId='%s' role='%s'",
                                        RPGMobsLogLevel.INFO,
                                        env.getId(),
                                        npcEntity.getRoleName() != null ? npcEntity.getRoleName() : "?"
                    );
                }
                return env.getId();
            }
        }

        return null;
    }

    private double @Nullable [] resolveSpawnChancesForEnvironment(RPGMobsConfig config, NPCEntity npcEntity) {
        String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);

        Map<String, double[]> envRules = resolved.environmentTierRules;
        if (envRules == null || envRules.isEmpty()) return null;

        String envId = resolveEnvironmentId(npcEntity);

        double[] matchedChances = null;
        String matchedKey = null;
        if (envId != null) {
            String envIdLower = envId.toLowerCase(Locale.ROOT);
            int bestLen = -1;
            for (Map.Entry<String, double[]> entry : envRules.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) continue;
                String keyLower = key.toLowerCase(Locale.ROOT);
                if (envIdLower.contains(keyLower) && keyLower.length() > bestLen) {
                    bestLen = keyLower.length();
                    matchedChances = entry.getValue();
                    matchedKey = key;
                }
            }
        }

        if (config.debugConfig.isDebugModeEnabled) {
            RPGMobsLogger.debug(LOGGER,
                                "[EnvSpawn] envId='%s' matchedKey='%s' roleName='%s'",
                                RPGMobsLogLevel.INFO,
                                String.valueOf(envId),
                                String.valueOf(matchedKey),
                                npcEntity.getRoleName() != null ? npcEntity.getRoleName() : "?"
            );
        }

        if (matchedChances == null || matchedChances.length == 0) return null;
        return matchedChances;
    }

    private void tickExistingRPGMob(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                    CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent) {

        boolean tierComponentChanged = false;

        String roleName = null;
        NPCEntity npcEntity = entityStore.getComponent(npcRef, Constants.NPC_COMPONENT_TYPE);
        if (npcEntity != null) roleName = npcEntity.getRoleName();

        if (RPGMobsPlugin.shouldReconcileThisTick() && npcEntity != null) {
            String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
            ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);
            if (!resolved.enabled) {
                RPGMobsLogger.debug(LOGGER,
                                    "[Reconcile] De-eliting NPC in disabled world: role=%s world=%s tier=%d",
                                    RPGMobsLogLevel.INFO,
                                    roleName,
                                    String.valueOf(worldName),
                                    tierComponent.tierIndex
                );
                deElite(config, npcRef, entityStore, commandBuffer, tierComponent, npcEntity);
                return;
            }
        }

        decrementAbilityCooldowns(npcRef, entityStore, commandBuffer);

        tierComponentChanged |= tryHandlePendingSummon(config, npcRef, npcEntity, entityStore, commandBuffer);

        tierComponentChanged |= finalizeHealSwapIfNeeded(npcRef, npcEntity, entityStore, commandBuffer);

        finalizeSummonSwapIfNeeded(npcRef, npcEntity, entityStore, commandBuffer);

        boolean needsReconcile = RPGMobsPlugin.shouldReconcileThisTick()
                || tierComponent.lastReconciledAt < RPGMobsPlugin.getConfigReloadCount();

        if (needsReconcile) {
            String reconcileWorldName = npcEntity != null && npcEntity.getWorld() != null
                    ? npcEntity.getWorld().getName() : null;
            ResolvedConfig resolvedForReconcile = RPGMobsPlugin.getResolvedConfig(reconcileWorldName);

            if (npcEntity != null && roleName != null) {
                MobRuleMatcher.MatchResult newMatch = mobRuleMatcher.findBestMatch(resolvedForReconcile.mobRules, roleName);

                if (newMatch == null) {
                    RPGMobsLogger.debug(LOGGER,
                                        "[Reconcile] De-eliting NPC — mob rule removed: role=%s oldRuleKey=%s tier=T%d",
                                        RPGMobsLogLevel.INFO,
                                        roleName,
                                        String.valueOf(tierComponent.matchedRuleKey),
                                        tierComponent.tierIndex + 1
                    );
                    deElite(config, npcRef, entityStore, commandBuffer, tierComponent, npcEntity);
                    return;
                }

                String oldRuleKey = tierComponent.matchedRuleKey;
                tierComponent.matchedRuleKey = newMatch.key();

                int newTier = applyTierOverride(npcEntity, newMatch.key(), tierComponent.tierIndex);
                boolean tierChanged = newTier != tierComponent.tierIndex;
                if (tierChanged) {
                    tierComponent.tierIndex = newTier;
                }

                boolean ruleChanged = oldRuleKey == null || !oldRuleKey.equals(newMatch.key());
                equipmentService.buildAndApply(npcEntity, config, tierComponent.tierIndex, newMatch.mobRule(),
                        resolvedForReconcile.droppedGearDurabilityMin, resolvedForReconcile.droppedGearDurabilityMax);

                if (config.debugConfig.isDebugModeEnabled) {
                    RPGMobsLogger.debug(LOGGER,
                                        "[Reconcile] Updated elite: role=%s ruleKey=%s tier=T%d%s",
                                        RPGMobsLogLevel.INFO,
                                        roleName,
                                        newMatch.key(),
                                        tierComponent.tierIndex + 1,
                                        ruleChanged ? " (rule changed from " + oldRuleKey + ")" : ""
                    );
                }
            }

            featureRegistry.reconcileAll(RPGMobsPlugin,
                                         config,
                                         resolvedForReconcile,
                                         npcRef,
                                         entityStore,
                                         commandBuffer,
                                         tierComponent,
                                         roleName
            );

            tierComponent.lastReconciledAt = RPGMobsPlugin.getConfigReloadCount();
            tierComponentChanged = true;
        }

        if (tierComponentChanged) {
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getRPGMobsComponentType(), tierComponent);
        }
    }

    private void decrementAbilityCooldowns(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                           CommandBuffer<EntityStore> commandBuffer) {
        ChargeLeapAbilityComponent chargeLeap = entityStore.getComponent(npcRef,
                                                                         RPGMobsPlugin.getChargeLeapAbilityComponentType()
        );
        if (chargeLeap != null && chargeLeap.cooldownTicksRemaining > 0) {
            chargeLeap.cooldownTicksRemaining--;
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getChargeLeapAbilityComponentType(), chargeLeap);
        }

        HealLeapAbilityComponent healLeap = entityStore.getComponent(npcRef,
                                                                     RPGMobsPlugin.getHealLeapAbilityComponentType()
        );
        if (healLeap != null && healLeap.cooldownTicksRemaining > 0) {
            healLeap.cooldownTicksRemaining--;
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getHealLeapAbilityComponentType(), healLeap);
        }

        SummonUndeadAbilityComponent summon = entityStore.getComponent(npcRef,
                                                                       RPGMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summon != null) {
            boolean changed = false;
            if (summon.cooldownTicksRemaining > 0) {
                summon.cooldownTicksRemaining--;
                changed = true;
            }
            if (summon.pendingSummonTicksRemaining > 0) {
                summon.pendingSummonTicksRemaining--;
                changed = true;
            }
            if (changed) {
                commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonUndeadAbilityComponentType(), summon);
            }
        }
    }

    private boolean tryHandlePendingSummon(RPGMobsConfig config, Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                           Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        boolean changed = false;

        DeathComponent deathCheck = entityStore.getComponent(npcRef, DeathComponent.getComponentType());
        if (deathCheck != null) return false;

        SummonUndeadAbilityComponent summonAbility = entityStore.getComponent(npcRef,
                                                                              RPGMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summonAbility == null) return false;

        if (summonAbility.pendingSummonRole == null || summonAbility.pendingSummonTicksRemaining > 0) return false;
        summonAbility.pendingSummonTicksRemaining = 0L;

        SummonAbilityConfig summonConfig = getSummonAbilityConfig(config);
        if (summonConfig == null || npcEntity == null) {
            summonAbility.pendingSummonRole = null;
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonUndeadAbilityComponentType(), summonAbility);
            return false;
        }

        String roleIdentifier = summonAbility.pendingSummonRole;
        summonAbility.pendingSummonRole = null;
        commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonUndeadAbilityComponentType(), summonAbility);

        List<SummonMarkerEntry> entries = resolveSummonEntries(summonConfig, roleIdentifier);
        if (entries.isEmpty()) {
            if (config.debugConfig.isDebugModeEnabled) {
                RPGMobsLogger.debug(LOGGER,
                                    "Pending summon skipped: no entries for role=%s",
                                    RPGMobsLogLevel.INFO,
                                    String.valueOf(roleIdentifier)
                );
            }
            return changed;
        }

        RPGMobsSummonMinionTrackingComponent summonTracking = entityStore.getComponent(npcRef,
                                                                                       RPGMobsPlugin.getSummonMinionTrackingComponentType()
        );
        int summonedAliveCount = summonTracking != null ? summonTracking.summonedAliveCount : 0;

        int maxAlive = clampSummonMaxAlive(summonConfig.maxAliveMinionsPerSummoner);
        int remaining = Math.max(0, maxAlive - summonedAliveCount);
        if (remaining == 0) {
            if (config.debugConfig.isDebugModeEnabled) {
                RPGMobsLogger.debug(LOGGER,
                                    "Pending summon skipped: cap reached alive=%d max=%d",
                                    RPGMobsLogLevel.INFO,
                                    summonedAliveCount,
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
            if (config.debugConfig.isDebugModeEnabled) {
                RPGMobsLogger.debug(LOGGER,
                                    "Pending summon skipped: spawnCount=0 remaining=%d",
                                    RPGMobsLogLevel.INFO,
                                    remaining
                );
            }
            return changed;
        }

        if (config.debugConfig.isDebugModeEnabled) {
            RPGMobsLogger.debug(LOGGER,
                                "Spawning minions: role=%s count=%d remaining=%d",
                                RPGMobsLogLevel.INFO,
                                String.valueOf(roleIdentifier),
                                spawnCount,
                                remaining
            );
        }

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
            if (roleName.startsWith(RPGMobsConfig.SUMMON_ROLE_PREFIX)) {
                roleName = roleName.substring(RPGMobsConfig.SUMMON_ROLE_PREFIX.length()).trim();
            }
            final String roleNameFinal = roleName;
            final Vector3d posFinal = pos;
            world.execute(() -> {
                var spawned = NPCPlugin.get().spawnNPC(entityStoreProvider.getStore(),
                                                       roleNameFinal,
                                                       null,
                                                       posFinal,
                                                       new Vector3f(0f, 0f, 0f)
                );
                if (spawned == null || spawned.first() == null) {
                    if (config.debugConfig.isDebugModeEnabled) {
                        RPGMobsLogger.debug(LOGGER, "Spawn failed: role=%s", RPGMobsLogLevel.INFO, roleNameFinal);
                    }
                    return;
                }
                Ref<EntityStore> spawnedRef = spawned.first();
                EntityStore storeProvider = world.getEntityStore();
                if (storeProvider == null) return;
                Store<EntityStore> store = storeProvider.getStore();

                StoreHelpers.withEntity(store, spawnedRef, (_, cb, _) -> {
                                            RPGMobsSummonedMinionComponent minion = store.getComponent(spawnedRef,
                                                                                                       RPGMobsPlugin.getSummonedMinionComponentType()
                                            );
                                            if (minion == null) {
                                                minion = new RPGMobsSummonedMinionComponent();
                                            }
                                            minion.summonerId = summonerId;
                                            minion.minTierIndex = 0;
                                            minion.maxTierIndex = 2;
                                            minion.tierApplied = false;
                                            cb.putComponent(spawnedRef, RPGMobsPlugin.getSummonedMinionComponentType(), minion);
                                        }
                );

                joinMinionToSummonerFlock(store, npcRef, spawnedRef);

                if (npcRef.isValid()) {
                    StoreHelpers.withEntity(store, npcRef, (_, cb, _) -> {
                                                RPGMobsSummonMinionTrackingComponent tracking = store.getComponent(npcRef,
                                                                                                                   RPGMobsPlugin.getSummonMinionTrackingComponentType()
                                                );
                                                if (tracking == null) tracking = new RPGMobsSummonMinionTrackingComponent();
                                                if (tracking.summonedAliveCount < 0) tracking.summonedAliveCount = 0;
                                                tracking.summonedAliveCount++;
                                                cb.replaceComponent(npcRef, RPGMobsPlugin.getSummonMinionTrackingComponentType(), tracking);
                                            }
                    );
                }
            });
        }

        return changed;
    }

    private boolean finalizeHealSwapIfNeeded(Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                             Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        if (npcEntity == null) return false;

        HealLeapAbilityComponent healLeapAbility = entityStore.getComponent(npcRef,
                                                                            RPGMobsPlugin.getHealLeapAbilityComponentType()
        );
        if (healLeapAbility == null || !healLeapAbility.swapActive) return false;

        boolean healChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore, npcRef, ABILITY_INTERACTION_TYPE);

        if (!healChainActive && healLeapAbility.swapActive) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, healLeapAbility);
            healLeapAbility.swapActive = false;
            healLeapAbility.swapSlot = -1;
            healLeapAbility.swapPreviousItem = null;
            healLeapAbility.hitsTaken = 0;
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getHealLeapAbilityComponentType(), healLeapAbility);
            return false;
        }

        return false;
    }

    private void finalizeSummonSwapIfNeeded(Ref<EntityStore> npcRef, NPCEntity npcEntity,
                                            Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        if (npcEntity == null) return;

        SummonUndeadAbilityComponent summonAbility = entityStore.getComponent(npcRef,
                                                                              RPGMobsPlugin.getSummonUndeadAbilityComponentType()
        );
        if (summonAbility == null || !summonAbility.swapActive) return;

        boolean summonChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore, npcRef, ABILITY_INTERACTION_TYPE);

        if (!summonChainActive) {
            AbilityHelpers.restoreSummonWeaponIfNeeded(npcEntity, summonAbility);
            commandBuffer.replaceComponent(npcRef, RPGMobsPlugin.getSummonUndeadAbilityComponentType(), summonAbility);
        }
    }

    private static int clampSummonCount(int count) {
        int clamped = Math.max(SUMMON_MIN_COUNT, count);
        return Math.min(SUMMON_MAX_COUNT, clamped);
    }

    private static int clampSummonMaxAlive(int value) {
        if (value < RPGMobsConfig.SUMMON_MAX_ALIVE_MIN) return RPGMobsConfig.SUMMON_MAX_ALIVE_MIN;
        return Math.min(value, RPGMobsConfig.SUMMON_MAX_ALIVE_MAX);
    }

    private SummonAbilityConfig getSummonAbilityConfig(RPGMobsConfig config) {
        if (config.abilitiesConfig.defaultAbilities == null) return null;
        AbilityConfig raw = config.abilitiesConfig.defaultAbilities.get(SUMMON_UNDEAD);
        return (raw instanceof SummonAbilityConfig summonAbilityConfig) ? summonAbilityConfig : null;
    }

    private List<SummonMarkerEntry> resolveSummonEntries(SummonAbilityConfig config, String roleIdentifier) {
        if (config == null) return List.of();
        String normalized = roleIdentifier == null ? "" : RPGMobsConfig.normalizeRoleIdentifier(roleIdentifier);
        RPGMobsLogger.debug(LOGGER,
                            "[SummonResolve] role='%s' normalized='%s' mapNull=%b mapSize=%d flatSize=%d",
                            RPGMobsLogLevel.INFO,
                            String.valueOf(roleIdentifier),
                            normalized,
                            config.spawnMarkerEntriesByRole == null,
                            config.spawnMarkerEntriesByRole != null ? config.spawnMarkerEntriesByRole.size() : -1,
                            config.spawnMarkerEntries != null ? config.spawnMarkerEntries.size() : -1
        );
        if (config.spawnMarkerEntriesByRole != null && !config.spawnMarkerEntriesByRole.isEmpty()) {
            RPGMobsLogger.debug(LOGGER,
                                "[SummonResolve] mapKeys=%s",
                                RPGMobsLogLevel.INFO,
                                config.spawnMarkerEntriesByRole.keySet()
            );
        }
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
        if (total <= 0.0) return entries.getFirst();
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        for (SummonMarkerEntry entry : entries) {
            if (entry == null) continue;
            cumulative += Math.max(0.0, entry.Weight);
            if (roll <= cumulative) return entry;
        }
        return entries.getLast();
    }

    private Vector3d pickSpawnPosition(Vector3d center) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * SUMMON_SPAWN_RADIUS;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        return new Vector3d(center.getX() + dx, center.getY(), center.getZ() + dz);
    }

    private void joinMinionToSummonerFlock(Store<EntityStore> store, Ref<EntityStore> summonerRef,
                                           Ref<EntityStore> minionRef) {
        if (!summonerRef.isValid() || !minionRef.isValid()) return;

        try {

            Ref<EntityStore> flockRef = FlockPlugin.getFlockReference(summonerRef, store);
            if (flockRef == null || !flockRef.isValid()) {
                NPCEntity summonerNpc = store.getComponent(summonerRef, Constants.NPC_COMPONENT_TYPE);
                if (summonerNpc == null || summonerNpc.getRole() == null) return;
                flockRef = FlockPlugin.createFlock(store, summonerNpc.getRole());
                if (flockRef == null || !flockRef.isValid()) {
                    LOGGER.atWarning().log("[FlockFollow] Failed to create flock for summoner");
                    return;
                }
                FlockMembershipSystems.join(summonerRef, flockRef, store);
                RPGMobsLogger.debug(LOGGER,
                                    "[FlockFollow] Created flock for summoner, joined as leader",
                                    RPGMobsLogLevel.INFO
                );
            }

            FlockMembershipSystems.join(minionRef, flockRef, store);
            RPGMobsLogger.debug(LOGGER, "[FlockFollow] Minion joined summoner's flock", RPGMobsLogLevel.INFO);
        } catch (Exception e) {
            LOGGER.atWarning().log("[FlockFollow] Failed to join minion to flock: %s", e.getMessage());
        }
    }

    private void logNpcScanSummaryIfDue(RPGMobsConfig config) {
        if (!config.debugConfig.isDebugModeEnabled) return;

        long now = System.currentTimeMillis();
        long everyMs = Math.max(1, config.debugConfig.debugMobRuleScanIntervalSeconds) * 1000L;
        if (now - lastReportTimestampMs < everyMs) return;

        lastReportTimestampMs = now;

        RPGMobsLogger.debug(LOGGER,
                            "Scan: seen=%d matched=%d applied=%d",
                            RPGMobsLogLevel.INFO,
                            mobsSeenCount,
                            mobsMatchedCount,
                            mobsAppliedCount
        );
    }

    private void createNewSchemaComponents(RPGMobsConfig config, Ref<EntityStore> npcRef,
                                           CommandBuffer<EntityStore> commandBuffer, NPCEntity npcEntity) {

        RPGMobsMigrationComponent migration = new RPGMobsMigrationComponent(2);
        commandBuffer.putComponent(npcRef, RPGMobsPlugin.getMigrationComponentType(), migration);

        if (config.spawning.progressionStyle == RPGMobsConfig.ProgressionStyle.DISTANCE_FROM_SPAWN) {
            double dist = getXZDistance(npcEntity);
            String worldName = npcEntity.getWorld() != null ? npcEntity.getWorld().getName() : null;
            ResolvedConfig resolved = RPGMobsPlugin.getResolvedConfig(worldName);
            RPGMobsProgressionComponent progression = getProgressionComponent(resolved, dist);
            commandBuffer.putComponent(npcRef, RPGMobsPlugin.getProgressionComponentType(), progression);
        }
    }

    private static @NonNull RPGMobsProgressionComponent getProgressionComponent(ResolvedConfig resolved,
                                                                                double dist) {
        double interval = Math.max(1.0, resolved.distanceBonusInterval);
        int intervals = (int) (dist / interval);

        float healthBonusPerInterval = resolved.distanceHealthBonusPerInterval;
        float damageBonusPerInterval = resolved.distanceDamageBonusPerInterval;
        float healthCap = resolved.distanceHealthBonusCap;
        float damageCap = resolved.distanceDamageBonusCap;

        float healthBonus = 0f;
        float damageBonus = 0f;
        if (intervals > 0) {
            healthBonus = intervals * healthBonusPerInterval;
            damageBonus = intervals * damageBonusPerInterval;

            if (healthBonus > healthCap) {
                healthBonus = healthCap;
            }
            if (damageBonus > damageCap) {
                damageBonus = damageCap;
            }
        }

        return new RPGMobsProgressionComponent(healthBonus, damageBonus, (float) dist);
    }
}
