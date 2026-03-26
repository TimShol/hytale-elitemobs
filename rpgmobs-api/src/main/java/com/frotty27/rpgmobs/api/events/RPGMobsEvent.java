package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Base class for RPGMobs events that are tied to a specific world and entity.
 *
 * <p>Provides common fields: the world, entity reference, resolved UUID,
 * tier index (0-4), and NPC role name. The entity UUID is resolved eagerly
 * in the constructor from the entity's {@link UUIDComponent}.</p>
 *
 * <p>Tier values are 0-based internally (0 = T1, 4 = T5).</p>
 *
 * @since 1.0.0
 */
public abstract class RPGMobsEvent {

    private final World world;
    private final Ref<EntityStore> entityRef;
    private final @Nullable UUID entityUuid;
    private final int tier;
    private final String roleName;

    /**
     * @param world     the world where the event occurred
     * @param entityRef ECS reference to the entity
     * @param tier      tier index (0-4)
     * @param roleName  NPC role identifier (e.g. "Skeleton_Guard")
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
     * Returns the world where this event occurred.
     *
     * @return the world instance
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns the ECS entity reference for the mob involved in this event.
     *
     * @return the entity reference
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Returns the entity's persistent UUID, or null if it could not be resolved.
     *
     * @return the entity UUID, or null
     */
    public @Nullable UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Returns the mob's tier index (0-based: 0 = T1, 4 = T5).
     *
     * @return tier index in range [0, 4]
     */
    public int getTier() {
        return tier;
    }

    /**
     * Returns the NPC role name (e.g. "Skeleton_Guard", "Trork_Warrior").
     *
     * @return the NPC role identifier
     */
    public String getRoleName() {
        return roleName;
    }
}
