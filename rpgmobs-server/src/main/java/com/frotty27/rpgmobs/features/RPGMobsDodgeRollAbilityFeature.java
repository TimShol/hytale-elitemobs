package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.DodgeRollAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGMobsDodgeRollAbilityFeature
        extends AbstractGatedAbilityFeature<DodgeRollAbilityComponent, RPGMobsConfig.DodgeRollAbilityConfig> {

    private static final double FORWARD_DODGE_MIN_DISTANCE = 7.0;

    private static final String[] CLOSE_RANGE_DIRECTIONS = {
            "rootBack", "rootLeft", "rootRight"
    };
    private static final String[] ALL_DIRECTIONS = {
            "rootBack", "rootLeft", "rootRight", "rootForward"
    };

    @Override
    public String id() {
        return AbilityIds.DODGE_ROLL;
    }

    @Override
    public String displayName() {
        return "Dodge Roll";
    }

    @Override
    public String description() {
        return "Quick lateral dodge when detecting incoming attack";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.PLAYER_ATTACK_NEARBY, AbilityTriggerSource.PLAYER_CHARGED_ATTACK_NEARBY);
    }

    @Override
    public boolean isAllowedForMinions() {
        return false;
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var dodgeRoll = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getDodgeRollAbilityComponentType());
        if (dodgeRoll == null) return false;

        float effectiveChance = dodgeRoll.dodgeChance;
        if (context.source() == AbilityTriggerSource.PLAYER_CHARGED_ATTACK_NEARBY) {
            RPGMobsConfig.DodgeRollAbilityConfig drConfig = context.config() != null
                    ? (RPGMobsConfig.DodgeRollAbilityConfig) context.config().abilitiesConfig.defaultAbilities.get(AbilityIds.DODGE_ROLL)
                    : null;
            float multiplier = drConfig != null ? drConfig.chargedAttackDodgeMultiplier : 2.5f;
            effectiveChance = Math.min(1.0f, effectiveChance * multiplier);
        }

        return ThreadLocalRandom.current().nextFloat() < effectiveChance;
    }

    @Override
    public String resolveRootTemplateKey(TriggerContext context) {
        String[] directions = resolveDirections(context);
        int directionIndex = ThreadLocalRandom.current().nextInt(directions.length);
        return directions[directionIndex];
    }

    private String[] resolveDirections(TriggerContext context) {
        Ref<EntityStore> targetRef = context.targetRef();
        if (targetRef == null || !targetRef.isValid()) return CLOSE_RANGE_DIRECTIONS;

        TransformComponent npcTransform = context.store().getComponent(
                context.entityRef(), TransformComponent.getComponentType());
        TransformComponent targetTransform = context.store().getComponent(
                targetRef, TransformComponent.getComponentType());

        if (npcTransform == null || targetTransform == null) return CLOSE_RANGE_DIRECTIONS;

        Vector3d npcPos = npcTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();
        double dx = npcPos.getX() - targetPos.getX();
        double dy = npcPos.getY() - targetPos.getY();
        double dz = npcPos.getZ() - targetPos.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq >= FORWARD_DODGE_MIN_DISTANCE * FORWARD_DODGE_MIN_DISTANCE) {
            return ALL_DIRECTIONS;
        }
        return CLOSE_RANGE_DIRECTIONS;
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.DodgeRollAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.PerTierFloat("Dodge Chance",
                        config -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).dodgeChancePerTier,
                        (config, value) -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).dodgeChancePerTier = value),
                new AbilityConfigField.ScalarFloat("Dodge Force",
                        config -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).dodgeForce,
                        (config, value) -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).dodgeForce = value),
                new AbilityConfigField.ScalarFloat("Invulnerability Duration",
                        config -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).invulnerabilityDuration,
                        (config, value) -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).invulnerabilityDuration = value),
                new AbilityConfigField.ScalarFloat("Charged Attack Dodge Multiplier",
                        config -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).chargedAttackDodgeMultiplier,
                        (config, value) -> ((RPGMobsConfig.DodgeRollAbilityConfig) config).chargedAttackDodgeMultiplier = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.DodgeRollAbilityConfig> configClass() {
        return RPGMobsConfig.DodgeRollAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, DodgeRollAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getDodgeRollAbilityComponentType();
    }

    @Override
    protected DodgeRollAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                      RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getDodgeRollAbilityComponentType());
    }

    @Override
    protected DodgeRollAbilityComponent createComponent(RPGMobsConfig.DodgeRollAbilityConfig abilityConfig,
                                                         int tierIndex, boolean enabled, Random random) {
        float dodgeChance = tierIndex < abilityConfig.dodgeChancePerTier.length
                ? abilityConfig.dodgeChancePerTier[tierIndex] : 0f;

        var component = new DodgeRollAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.dodgeChance = dodgeChance;
        return component;
    }

    @Override
    protected void afterApply(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                              Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        plugin.getPlayerAttackTracker().registerDodgeEntity(npcRef);
    }

    @Override
    protected void afterRemove(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                               Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        plugin.getPlayerAttackTracker().unregisterDodgeEntity(npcRef);
    }
}
