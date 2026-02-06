package com.frotty27.elitemobs.systems.combat;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Random;
import java.util.Set;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.elitemobs.utils.Constants.TIERS_AMOUNT;

public final class EliteMobsDamageDealtSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final float DEFAULT_DAMAGE_MULTIPLIER = 1.0f;
    private static final long DEBUG_EVERY_EVENTS = 200;
    private static final String ATTACKER_KIND_NPC = "NPC";
    private static final String ATTACKER_KIND_PLAYER = "Player";
    private static final String ATTACKER_KIND_PROJECTILE = "Projectile";
    private static final String ATTACKER_KIND_OTHER = "Other";

    private static final ComponentType<EntityStore, NPCEntity> NPC_COMPONENT_TYPE = NPCEntity.getComponentType();
    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_COMPONENT_TYPE = PlayerRef.getComponentType();
    private static final ComponentType<EntityStore, ProjectileComponent> PROJECTILE_COMPONENT_TYPE = ProjectileComponent.getComponentType();

    private final EliteMobsPlugin eliteMobsPlugin;
    private final Random random = new Random();
    private final EliteMobsDamageScalingHandler damageScalingHandler = new EliteMobsDamageScalingHandler(this);

    private long damageEventsSeenCount;
    private long damageEventsScaledCount;
    private long skippedNotEntitySourceCount;
    private long skippedInvalidAttackerRefCount;
    private long skippedAttackerNotEliteCount;

    public EliteMobsDamageDealtSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Keep as-is: system runs on entities with stats (damage pipeline requirement)
        return EntityStatMap.getComponentType();
    }

    @Override
    public @NonNull Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
                new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
                new SystemGroupDependency<>(Order.BEFORE, DamageModule.get().getFilterDamageGroup())
        );
    }

    @Override
    public void handle(
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull Damage damage
    ) {
        damageScalingHandler.handle(entityIndex, archetypeChunk, entityStore, commandBuffer, damage);
    }

    void processHandle(
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull Damage damage
    ) {
        EliteMobsConfig config = eliteMobsPlugin.getConfig();
        if (config == null) return;
        if (!config.damage.enableMobDamageMultiplier) return;

        damageEventsSeenCount++;

        boolean debugEnabled = config.debug.isDebugModeEnabled;
        if (debugEnabled && (damageEventsSeenCount % DEBUG_EVERY_EVENTS == 0)) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Summary damageEventsSeenAmount=%d damageEventsAppliedAmount=%d skipNotEntitySource=%d skipInvalidAttackerRef=%d skipAttackerNotElite=%d",
                    EliteMobsLogLevel.INFO,
                    damageEventsSeenCount,
                    damageEventsScaledCount,
                    skippedNotEntitySourceCount,
                    skippedInvalidAttackerRefCount,
                    skippedAttackerNotEliteCount
            );
        }

        String victimRoleName;
        if (debugEnabled) {
            assert NPC_COMPONENT_TYPE != null;
            victimRoleName = safeRoleName(archetypeChunk.getComponent(entityIndex, NPC_COMPONENT_TYPE));
        } else {
            victimRoleName = null;
        }

        Damage.Source damageSource = damage.getSource();
        if (!(damageSource instanceof Damage.EntitySource attackerEntitySource)) {
            skippedNotEntitySourceCount++;

            if (debugEnabled) {
                EliteMobsLogger.debug(
                        LOGGER,
                        "Skip: non-entity source src=%s victimRole=%s amount=%.2f",
                        EliteMobsLogLevel.INFO,
                        damageSource.getClass().getName(),
                        victimRoleName,
                        damage.getAmount()
                );
            }
            return;
        }

        Ref<EntityStore> attackerEntityRef = attackerEntitySource.getRef();
        if (!attackerEntityRef.isValid()) {
            skippedInvalidAttackerRefCount++;

            if (debugEnabled) {
                EliteMobsLogger.debug(
                        LOGGER,
                        "Skip: attackerRef invalid victimRole=%s amount=%.2f",
                        EliteMobsLogLevel.INFO,
                        victimRoleName,
                        damage.getAmount()
                );
            }
            return;
        }

        EliteMobsTierComponent attackerTierComponent =
                entityStore.getComponent(attackerEntityRef, eliteMobsPlugin.getEliteMobsComponent());
        if (attackerTierComponent == null || attackerTierComponent.tierIndex < 0) {
            skippedAttackerNotEliteCount++;

            if (debugEnabled) {
                NPCEntity attackerNpcEntity = entityStore.getComponent(attackerEntityRef, NPC_COMPONENT_TYPE);

                EliteMobsLogger.debug(
                        LOGGER,
                        "Skip: attacker not elite attackerRole=%s attackerKind=%s victimRole=%s amount=%.2f",
                        EliteMobsLogLevel.INFO,
                        safeRoleName(attackerNpcEntity),
                        classifyAttackerKind(entityStore, attackerEntityRef),
                        victimRoleName,
                        damage.getAmount()
                );
            }
            return;
        }

        // Update aggro on the attacker so they remember this target for abilities
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(entityIndex);
        if (victimRef != null && victimRef.isValid()) {
            attackerTierComponent.lastAggroRef = victimRef;
            attackerTierComponent.lastAggroTick = eliteMobsPlugin.getTickClock().getTick();
            commandBuffer.replaceComponent(attackerEntityRef, eliteMobsPlugin.getEliteMobsComponent(), attackerTierComponent);
        }

        int clampedTierIndex = clampTierIndex(attackerTierComponent.tierIndex);

        // Multiplier per tier (ConfigIO should guarantee sizing, but keep safe fallback)
        float damageMultiplier = DEFAULT_DAMAGE_MULTIPLIER;
        if (config.damage.mobDamageMultiplierPerTier != null && config.damage.mobDamageMultiplierPerTier.length >= TIERS_AMOUNT) {
            damageMultiplier = config.damage.mobDamageMultiplierPerTier[clampedTierIndex];
        }

        // Optional random variance
        float damageRandomVariance = config.damage.mobDamageRandomVariance;
        if (damageRandomVariance > 0f) {
            damageMultiplier += (random.nextFloat() * 2f - 1f) * damageRandomVariance;
        }

        if (damageMultiplier < 0f) damageMultiplier = 0f;

        float damageBeforeScaling = damage.getAmount();
        float damageAfterScaling = damageBeforeScaling * damageMultiplier;

        damage.setAmount(damageAfterScaling);
        damageEventsScaledCount++;

        if (debugEnabled) {
            NPCEntity attackerNpcEntity = entityStore.getComponent(attackerEntityRef, NPC_COMPONENT_TYPE);

            EliteMobsLogger.debug(
                    LOGGER,
                    "APPLY tier=%d mult=%.3f dmg %.2f -> %.2f attackerRole=%s victimRole=%s",
                    EliteMobsLogLevel.INFO,
                    clampedTierIndex,
                    damageMultiplier,
                    damageBeforeScaling,
                    damageAfterScaling,
                    safeRoleName(attackerNpcEntity),
                    victimRoleName
            );
        }
    }


    private static String classifyAttackerKind(Store<EntityStore> entityStore, Ref<EntityStore> attackerEntityRef) {
        assert NPC_COMPONENT_TYPE != null;
        if (entityStore.getComponent(attackerEntityRef, NPC_COMPONENT_TYPE) != null) return ATTACKER_KIND_NPC;
        if (entityStore.getComponent(attackerEntityRef, PLAYER_REF_COMPONENT_TYPE) != null) return ATTACKER_KIND_PLAYER;
        if (entityStore.getComponent(attackerEntityRef, PROJECTILE_COMPONENT_TYPE) != null) return ATTACKER_KIND_PROJECTILE;
        return ATTACKER_KIND_OTHER;
    }

    private static @Nullable String safeRoleName(@Nullable NPCEntity npcEntity) {
        if (npcEntity == null) return null;

        try {
            return npcEntity.getRoleName();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
