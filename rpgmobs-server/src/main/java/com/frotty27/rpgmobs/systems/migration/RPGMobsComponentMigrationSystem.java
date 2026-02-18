package com.frotty27.rpgmobs.systems.migration;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.effects.RPGMobsActiveEffectsComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsMigrationComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public final class RPGMobsComponentMigrationSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int CURRENT_MIGRATION_VERSION = 2;

    private final RPGMobsPlugin plugin;
    private final Random random = new Random();

    public RPGMobsComponentMigrationSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getRPGMobsComponentType());
    }

    @Override
    public void tick(float v, int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> entityStore,
                     @NonNull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);
        RPGMobsTierComponent tier = chunk.getComponent(entityIndex, plugin.getRPGMobsComponentType());

        RPGMobsMigrationComponent migration = entityStore.getComponent(entityRef, plugin.getMigrationComponentType());
        int currentVersion = (migration != null) ? migration.migrationVersion : 0;

        if (currentVersion >= CURRENT_MIGRATION_VERSION) {
            return;
        }

        if (currentVersion == 0) {
            migrateFromV0(entityRef, tier, commandBuffer);
        } else if (currentVersion == 1) {
            migrateFromV1(entityRef, entityStore, commandBuffer);
        }

        RPGMobsMigrationComponent updatedMigration = new RPGMobsMigrationComponent(CURRENT_MIGRATION_VERSION);
        commandBuffer.putComponent(entityRef, plugin.getMigrationComponentType(), updatedMigration);

        RPGMobsLogger.debug(LOGGER,
                            "Migrated entity from version %d to %d (tier %d)",
                            RPGMobsLogLevel.INFO,
                            currentVersion,
                            CURRENT_MIGRATION_VERSION,
                            tier.tierIndex
        );
    }

    private void migrateFromV0(Ref<EntityStore> entityRef, RPGMobsTierComponent tier,
                               CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        int tierIndex = tier.tierIndex;

        RPGMobsLogger.debug(LOGGER,
                            "Migrating entity from v0 (tier %d) - creating all components with defaults",
                            RPGMobsLogLevel.INFO,
                            tierIndex
        );

        migrateHealLeapAbility(entityRef, config, tierIndex, commandBuffer);
        migrateChargeLeapAbility(entityRef, config, tierIndex, commandBuffer);
        migrateSummonUndeadAbility(entityRef, config, tierIndex, commandBuffer);

        if (config.healthConfig.enableMobHealthScaling) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getHealthScalingComponentType(),
                                       new RPGMobsHealthScalingComponent()
            );
        }
        if (config.modelConfig.enableMobModelScaling) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getModelScalingComponentType(),
                                       new RPGMobsModelScalingComponent()
            );
        }

        commandBuffer.putComponent(entityRef, plugin.getProgressionComponentType(), new RPGMobsProgressionComponent());

        commandBuffer.putComponent(entityRef,
                                   plugin.getActiveEffectsComponentType(),
                                   new RPGMobsActiveEffectsComponent()
        );

        commandBuffer.putComponent(entityRef,
                                   plugin.getCombatTrackingComponentType(),
                                   new RPGMobsCombatTrackingComponent()
        );

        commandBuffer.putComponent(entityRef, plugin.getAbilityLockComponentType(), new RPGMobsAbilityLockComponent());
    }

    private void migrateFromV1(Ref<EntityStore> entityRef, Store<EntityStore> entityStore,
                               CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        RPGMobsLogger.debug(LOGGER,
                            "Migrating entity from v1 - creating split scaling components with defaults",
                            RPGMobsLogLevel.INFO
        );

        if (config.healthConfig.enableMobHealthScaling) {
            RPGMobsHealthScalingComponent healthScaling = entityStore.getComponent(entityRef,
                                                                                   plugin.getHealthScalingComponentType()
            );
            if (healthScaling == null) {
                commandBuffer.putComponent(entityRef,
                                           plugin.getHealthScalingComponentType(),
                                           new RPGMobsHealthScalingComponent()
                );
            }
        }

        if (config.modelConfig.enableMobModelScaling) {
            RPGMobsModelScalingComponent modelScaling = entityStore.getComponent(entityRef,
                                                                                 plugin.getModelScalingComponentType()
            );
            if (modelScaling == null) {
                commandBuffer.putComponent(entityRef,
                                           plugin.getModelScalingComponentType(),
                                           new RPGMobsModelScalingComponent()
                );
            }
        }

        RPGMobsAbilityLockComponent abilityLock = entityStore.getComponent(entityRef,
                                                                           plugin.getAbilityLockComponentType()
        );
        if (abilityLock == null) {
            commandBuffer.putComponent(entityRef,
                                       plugin.getAbilityLockComponentType(),
                                       new RPGMobsAbilityLockComponent()
            );
        }
    }

    private void migrateHealLeapAbility(Ref<EntityStore> entityRef, RPGMobsConfig config, int tierIndex,
                                        CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig.HealLeapAbilityConfig abilityConfig = (RPGMobsConfig.HealLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(
                AbilityIds.HEAL_LEAP);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        HealLeapAbilityComponent component = new HealLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.triggerHealthPercent = 0.25f;
        component.cooldownTicksRemaining = 0L;
        commandBuffer.putComponent(entityRef, plugin.getHealLeapAbilityComponentType(), component);
    }

    private void migrateChargeLeapAbility(Ref<EntityStore> entityRef, RPGMobsConfig config, int tierIndex,
                                          CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig.ChargeLeapAbilityConfig abilityConfig = (RPGMobsConfig.ChargeLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(
                AbilityIds.CHARGE_LEAP);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        ChargeLeapAbilityComponent component = new ChargeLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        commandBuffer.putComponent(entityRef, plugin.getChargeLeapAbilityComponentType(), component);
    }

    private void migrateSummonUndeadAbility(Ref<EntityStore> entityRef, RPGMobsConfig config, int tierIndex,
                                            CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig.SummonAbilityConfig abilityConfig = (RPGMobsConfig.SummonAbilityConfig) config.abilitiesConfig.defaultAbilities.get(
                AbilityIds.SUMMON_UNDEAD);

        if (abilityConfig == null || !abilityConfig.isEnabled) {
            return;
        }

        if (tierIndex < 0 || tierIndex >= abilityConfig.isEnabledPerTier.length) {
            return;
        }

        if (!abilityConfig.isEnabledPerTier[tierIndex]) {
            return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        SummonUndeadAbilityComponent component = new SummonUndeadAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.pendingSummonTicksRemaining = 0L;
        component.pendingSummonRole = null;
        commandBuffer.putComponent(entityRef, plugin.getSummonUndeadAbilityComponentType(), component);

        RPGMobsSummonMinionTrackingComponent trackingComponent = new RPGMobsSummonMinionTrackingComponent();
        commandBuffer.putComponent(entityRef, plugin.getSummonMinionTrackingComponentType(), trackingComponent);
    }
}
