package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Fired when an elite begins an ability chain. Cancellable - prevents the ability from
 * starting. Does not extend {@link RPGMobsEvent} since ability triggers occur on the
 * evaluation thread without a World reference.
 *
 * <p>Common ability IDs: charge_leap, dodge_roll, multi_slash_short, multi_slash_medium,
 * multi_slash_long, enrage, volley, heal_leap, undead_summon.
 *
 * @since 1.0.0
 */
public final class RPGMobsAbilityStartedEvent implements ICancellable {

    private final Ref<EntityStore> entityRef;
    private final String abilityId;
    private final int tierIndex;
    private final @Nullable Ref<EntityStore> targetRef;
    private boolean cancelled;

    /**
     * @param entityRef reference to the elite starting the ability
     * @param abilityId the ability identifier (e.g. "charge_leap", "dodge_roll")
     * @param tierIndex tier index (0-based, 0 = T1 through 4 = T5)
     * @param targetRef the elite's current combat target, or {@code null} if none
     */
    public RPGMobsAbilityStartedEvent(Ref<EntityStore> entityRef, String abilityId, int tierIndex,
                                      @Nullable Ref<EntityStore> targetRef) {
        this.entityRef = entityRef;
        this.abilityId = abilityId;
        this.tierIndex = tierIndex;
        this.targetRef = targetRef;
    }

    /**
     * @return reference to the elite entity
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * @return the ability identifier (e.g. "charge_leap", "multi_slash_short")
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * @return tier index (0-based, 0 = T1 through 4 = T5)
     */
    public int getTierIndex() {
        return tierIndex;
    }

    /**
     * @return the elite's combat target, or {@code null} if none
     */
    public @Nullable Ref<EntityStore> getTargetRef() {
        return targetRef;
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
