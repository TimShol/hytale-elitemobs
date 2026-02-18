package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Abstract base class for all RPGMobs events.
 *
 * <p>Every RPG mob event carries the world, entity reference, entity UUID,
 * tier index, and role name of the RPG mob involved. Subclasses add
 * event-specific data such as damage amounts, positions, or target references.</p>
 *
 * @since 1.1.0
 */
public abstract class RPGMobsEvent {

    private final World world;
    private final Ref<EntityStore> entityRef;
    private final @Nullable UUID entityUuid;
    private final int tier;
    private final String roleName;

    /**
     * Constructs a new RPG mob event.
     *
     * @param world     the world in which this event occurred
     * @param entityRef the entity reference of the RPG mob involved in this event
     * @param tier      the tier index of the RPG mob
     * @param roleName  the role name (NPC type identifier) of the RPG mob
     */
    protected RPGMobsEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName) {
        this.world = world;
        this.entityRef = entityRef;
        this.tier = tier;
        this.roleName = roleName;

        UUID resolved = null;
        if (entityRef != null) {
            Store<EntityStore> store = entityRef.getStore();
            if (store != null) {
                UUIDComponent uuidComp = store.getComponent(entityRef, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    resolved = uuidComp.getUuid();
                }
            }
        }
        this.entityUuid = resolved;
    }

    /**
     * Returns the world in which this event occurred.
     *
     * @return the world, never {@code null}
     * @since 1.2.0
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns the entity reference of the RPG mob involved in this event.
     *
     * @return the entity reference, never {@code null}
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the UUID of the RPG mob involved in this event, if available.
     *
     * <p>Resolved eagerly from the entity's {@link UUIDComponent} at event
     * construction time.</p>
     *
     * @return the entity UUID, or {@code null} if not available
     * @since 1.2.0
     */
    public @Nullable UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Returns the tier index of the RPG mob.
     *
     * <p>Tier determines the mob's difficulty level, affecting health, damage,
     * model scale, and available abilities.</p>
     *
     * @return the zero-based tier index
     */
    public int getTier() {
        return tier;
    }

    /**
     * Returns the role name of the RPG mob.
     *
     * <p>The role name identifies the NPC type (e.g., {@code "zombie"}, {@code "skeleton"})
     * and is used for rule matching and asset resolution.</p>
     *
     * @return the role name, never {@code null}
     */
    public String getRoleName() {
        return roleName;
    }
}
