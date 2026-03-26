package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an RPGMobs elite deals damage to another entity. Cancellable - the damage
 * multiplier can be adjusted via {@link #setMultiplier(float)} or the event cancelled to
 * prevent damage entirely. The entityRef from the parent class is the attacker.
 * Final damage = baseDamage * multiplier.
 *
 * @since 1.0.0
 */
public final class RPGMobsDamageDealtEvent extends RPGMobsEvent implements ICancellable {

    private final Ref<EntityStore> victimRef;
    private final float baseDamage;
    private float multiplier;
    private boolean cancelled;

    /**
     * @param world       the world where the damage occurred
     * @param attackerRef reference to the attacking elite (same as {@link #getEntityRef()})
     * @param tier        tier index (0-based)
     * @param roleName    the attacker's NPC role name
     * @param victimRef   reference to the entity receiving damage
     * @param baseDamage  the unscaled damage amount
     * @param multiplier  the initial damage multiplier (final damage = baseDamage * multiplier)
     */
    public RPGMobsDamageDealtEvent(World world, Ref<EntityStore> attackerRef, int tier, String roleName,
                                   Ref<EntityStore> victimRef, float baseDamage, float multiplier) {
        super(world, attackerRef, tier, roleName);
        this.victimRef = victimRef;
        this.baseDamage = baseDamage;
        this.multiplier = multiplier;
    }

    /**
     * @return reference to the entity receiving damage
     */
    public Ref<EntityStore> getVictimRef() {
        return victimRef;
    }

    /**
     * @return the unscaled damage amount before the multiplier is applied
     */
    public float getBaseDamage() {
        return baseDamage;
    }

    /**
     * @return the current damage multiplier
     */
    public float getMultiplier() {
        return multiplier;
    }

    /**
     * Adjusts the damage multiplier. Final damage = {@link #getBaseDamage()} * multiplier.
     *
     * @param multiplier the new damage multiplier
     */
    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
