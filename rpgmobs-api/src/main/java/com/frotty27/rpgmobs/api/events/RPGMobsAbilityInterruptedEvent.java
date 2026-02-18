package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an RPG mob's ability is interrupted before it completes.
 *
 * <p>Interruption can occur for various reasons, such as the target becoming
 * invalid, the mob being stunned, or the mob dying mid-ability. The specific
 * reason is available via {@link #getReason()}.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsAbilityInterruptedEvent {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;
    private final String reason;

    /**
     * Constructs a new ability interrupted event.
     *
     * @param entityRef the entity reference of the RPG mob whose ability was interrupted
     * @param abilityId the string identifier of the ability that was interrupted
     * @param tierIndex the zero-based tier index of the RPG mob
     * @param reason    a human-readable description of why the ability was interrupted
     */
    public RPGMobsAbilityInterruptedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex, String reason) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
        this.reason = reason;
    }

    /**
     * Returns the entity reference of the RPG mob whose ability was interrupted.
     *
     * @return the entity reference, never {@code null}
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the string identifier of the ability that was interrupted.
     *
     * <p>This corresponds to the {@link com.frotty27.rpgmobs.api.query.AbilityType}
     * enum names (e.g., {@code "CHARGE_LEAP"}).</p>
     *
     * @return the ability identifier, never {@code null}
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * Returns the zero-based tier index of the RPG mob.
     *
     * @return the tier index
     */
    public int getTierIndex() {
        return tierIndex;
    }

    /**
     * Returns a human-readable description of why the ability was interrupted.
     *
     * @return the interruption reason, never {@code null}
     */
    public String getReason() {
        return reason;
    }
}
