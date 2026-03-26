package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an ability chain is interrupted before completion. The reason string indicates
 * the cause (e.g. "death", "deaggro", "heal_leap_cast"). Informational only.
 *
 * @since 1.0.0
 */
public final class RPGMobsAbilityInterruptedEvent {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;
    private final String reason;

    /**
     * @param entityRef reference to the elite whose ability was interrupted
     * @param abilityId the ability identifier
     * @param tierIndex tier index (0-based)
     * @param reason    the interruption cause (e.g. "death", "deaggro", "heal_leap_cast")
     */
    public RPGMobsAbilityInterruptedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex, String reason) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
        this.reason = reason;
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

    /**
     * @return the reason the ability was interrupted (e.g. "death", "deaggro")
     */
    public String getReason() {
        return reason;
    }
}
