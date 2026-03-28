package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired after health, damage, and model scaling are applied to an elite. Contains all
 * computed scaling values including base and final health. Tier index is 0-based
 * (0 = T1, 4 = T5).
 *
 * @since 1.0.0
 */
public final class RPGMobsScalingAppliedEvent extends RPGMobsEvent {

    private final float healthMultiplier;
    private final float damageMultiplier;
    private final float modelScale;
    private final float baseHealth;
    private final float finalHealth;
    private final boolean healthFinalized;

    /**
     * @param mobRef           reference to the elite entity
     * @param tierIndex        tier index (0-based, 0 = T1 through 4 = T5)
     * @param roleName         NPC role identifier
     * @param world            the world where the event occurred
     * @param healthMultiplier the health multiplier applied for this tier
     * @param damageMultiplier the damage multiplier applied for this tier
     * @param modelScale       the model scale factor (1.0 = no scaling)
     * @param baseHealth       the NPC's base health before scaling
     * @param finalHealth      the NPC's final health after the multiplier is applied
     * @param healthFinalized  {@code true} if health scaling has been finalized (stat value set)
     */
    public RPGMobsScalingAppliedEvent(com.hypixel.hytale.server.core.universe.world.World world,
                                      Ref<EntityStore> mobRef, int tierIndex, String roleName,
                                      float healthMultiplier, float damageMultiplier, float modelScale,
                                      float baseHealth, float finalHealth, boolean healthFinalized) {
        super(world, mobRef, tierIndex, roleName);
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.modelScale = modelScale;
        this.baseHealth = baseHealth;
        this.finalHealth = finalHealth;
        this.healthFinalized = healthFinalized;
    }

    /**
     * Returns the health multiplier applied for this tier.
     *
     * @return the health multiplier
     */
    public float getHealthMultiplier() {
        return healthMultiplier;
    }

    /**
     * Returns the damage multiplier applied for this tier.
     *
     * @return the damage multiplier
     */
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Returns the model scale factor (1.0 = no scaling).
     *
     * @return the model scale factor
     */
    public float getModelScale() {
        return modelScale;
    }

    /**
     * Returns the NPC's base health before scaling.
     *
     * @return the base health value
     */
    public float getBaseHealth() {
        return baseHealth;
    }

    /**
     * Returns the NPC's final health after the multiplier is applied.
     *
     * @return the final health value
     */
    public float getFinalHealth() {
        return finalHealth;
    }

    /**
     * Returns whether health scaling has been finalized (stat value set).
     *
     * @return {@code true} if health is finalized
     */
    public boolean isHealthFinalized() {
        return healthFinalized;
    }
}
