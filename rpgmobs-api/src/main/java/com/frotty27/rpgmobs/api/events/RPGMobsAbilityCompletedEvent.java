package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an ability chain finishes successfully. Informational only - cannot be cancelled.
 *
 * @since 1.0.0
 */
public final class RPGMobsAbilityCompletedEvent {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;

    /**
     * @param entityRef reference to the elite that completed the ability
     * @param abilityId the ability identifier
     * @param tierIndex tier index (0-based)
     */
    public RPGMobsAbilityCompletedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
    }

    /**
     * @return reference to the elite entity
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * @return the ability identifier
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * @return tier index (0-based)
     */
    public int getTierIndex() {
        return tierIndex;
    }
}
