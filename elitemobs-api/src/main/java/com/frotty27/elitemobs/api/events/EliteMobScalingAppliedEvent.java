package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event record fired after health, damage, and model scaling have been applied to an elite mob.
 *
 * <p>This event is informational and provides the computed scaling values for the mob.
 * It can be used to log, display, or react to the final scaling parameters after all
 * tier-based and distance-based calculations are complete.</p>
 *
 * @param mobRef           the entity reference of the elite mob that was scaled
 * @param tierIndex        the zero-based tier index of the elite mob
 * @param healthMultiplier the health multiplier applied to the mob's base health
 * @param damageMultiplier the damage multiplier applied to the mob's outgoing damage
 * @param modelScale       the visual model scale factor applied to the mob
 * @param baseHealth       the mob's original base health before scaling
 * @param finalHealth      the mob's final health after scaling has been applied
 * @param healthFinalized  whether the health value has been finalized (applied to the entity)
 * @since 1.1.0
 */
public record EliteMobScalingAppliedEvent(Ref<EntityStore> mobRef, int tierIndex, float healthMultiplier, float damageMultiplier,
                                          float modelScale, float baseHealth, float finalHealth, boolean healthFinalized) {

}
