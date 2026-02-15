package com.frotty27.elitemobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an elite mob successfully completes an ability.
 *
 * <p>This event is informational and indicates that the ability ran to completion
 * without interruption. Listeners can use it to trigger follow-up effects or
 * track ability usage statistics.</p>
 *
 * @since 1.1.0
 */
public final class EliteMobAbilityCompletedEvent {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;

    /**
     * Constructs a new ability completed event.
     *
     * @param entityRef the entity reference of the elite mob that completed the ability
     * @param abilityId the string identifier of the ability that was completed
     * @param tierIndex the zero-based tier index of the elite mob
     */
    public EliteMobAbilityCompletedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
    }

    /**
     * Returns the entity reference of the elite mob that completed the ability.
     *
     * @return the entity reference, never {@code null}
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the string identifier of the ability that was completed.
     *
     * <p>This corresponds to the {@link com.frotty27.elitemobs.api.query.AbilityType}
     * enum names (e.g., {@code "CHARGE_LEAP"}).</p>
     *
     * @return the ability identifier, never {@code null}
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * Returns the zero-based tier index of the elite mob.
     *
     * @return the tier index
     */
    public int getTierIndex() {
        return tierIndex;
    }
}
