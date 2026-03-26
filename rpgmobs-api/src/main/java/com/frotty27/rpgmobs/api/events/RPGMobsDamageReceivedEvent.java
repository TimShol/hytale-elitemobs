package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Fired when an RPGMobs elite receives damage from any source. Informational only - cannot
 * be cancelled or modified. The entityRef from the parent class is the victim.
 *
 * @since 1.0.0
 */
public final class RPGMobsDamageReceivedEvent extends RPGMobsEvent {

    private final @Nullable Ref<EntityStore> attackerRef;
    private final float damageAmount;

    /**
     * @param world        the world where the damage occurred
     * @param victimRef    reference to the elite that received damage (same as {@link #getEntityRef()})
     * @param tier         tier index (0-based)
     * @param roleName     the victim's NPC role name
     * @param attackerRef  reference to the attacking entity, or {@code null} for environmental damage
     * @param damageAmount the amount of damage received
     */
    public RPGMobsDamageReceivedEvent(World world, Ref<EntityStore> victimRef, int tier, String roleName,
                                      @Nullable Ref<EntityStore> attackerRef, float damageAmount) {
        super(world, victimRef, tier, roleName);
        this.attackerRef = attackerRef;
        this.damageAmount = damageAmount;
    }

    /**
     * @return the entity that dealt the damage, or {@code null} for environmental damage
     */
    public @Nullable Ref<EntityStore> getAttackerRef() {
        return attackerRef;
    }

    /**
     * @return the amount of damage received
     */
    public float getDamageAmount() {
        return damageAmount;
    }
}
