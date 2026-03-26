package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired after health, damage, and model scaling are applied to an elite. Contains all
 * computed scaling values including base and final health. Tier index is 0-based
 * (0 = T1, 4 = T5).
 *
 * @param mobRef            reference to the elite entity
 * @param tierIndex         tier index (0-based, 0 = T1 through 4 = T5)
 * @param healthMultiplier  the health multiplier applied for this tier
 * @param damageMultiplier  the damage multiplier applied for this tier
 * @param modelScale        the model scale factor (1.0 = no scaling)
 * @param baseHealth        the NPC's base health before scaling
 * @param finalHealth       the NPC's final health after the multiplier is applied
 * @param healthFinalized   {@code true} if health scaling has been finalized (stat value set)
 * @since 1.0.0
 */
public record RPGMobsScalingAppliedEvent(Ref<EntityStore> mobRef, int tierIndex, float healthMultiplier,
                                         float damageMultiplier, float modelScale, float baseHealth, float finalHealth,
                                         boolean healthFinalized) {
}
