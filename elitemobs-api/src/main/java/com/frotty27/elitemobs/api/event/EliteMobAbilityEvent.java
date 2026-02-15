package com.frotty27.elitemobs.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Event fired when an elite mob uses an ability.
 *
 * @deprecated Use {@link com.frotty27.elitemobs.api.events.EliteMobAbilityStartedEvent},
 * {@link com.frotty27.elitemobs.api.events.EliteMobAbilityCompletedEvent}, and
 * {@link com.frotty27.elitemobs.api.events.EliteMobAbilityInterruptedEvent} instead.
 * This event is never fired and will be removed in a future version.
 * @since 1.1.0
 */
@Deprecated
public final class EliteMobAbilityEvent extends EliteMobEvent implements ICancellable {

    private final AbilityType abilityType;
    private final @Nullable Ref<EntityStore> targetRef;
    private boolean cancelled;

    /**
     * Constructs a new ability event.
     *
     * @param entityRef   the entity reference of the elite mob using the ability
     * @param tier        the tier index of the elite mob
     * @param roleName    the role name of the elite mob
     * @param abilityType the type of ability being used
     * @param targetRef   the entity reference of the ability target, or {@code null}
     *                    if the ability has no specific target
     */
    public EliteMobAbilityEvent(Ref<EntityStore> entityRef, int tier, String roleName,
                                AbilityType abilityType, @Nullable Ref<EntityStore> targetRef) {
        super(entityRef, tier, roleName);
        this.abilityType = abilityType;
        this.targetRef = targetRef;
    }

    /**
     * Returns the type of ability being used.
     *
     * @return the ability type, never {@code null}
     */
    public AbilityType getAbilityType() {
        return abilityType;
    }

    /**
     * Returns the entity reference of the ability target, if any.
     *
     * @return the target's entity reference, or {@code null} if the ability has no
     *         specific target
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
