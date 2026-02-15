package com.frotty27.elitemobs.systems.ability.trigger;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TargetWithinRangeTrigger(float range) implements ITriggerCondition {

    public static final String TYPE = "target_within_range";

    public TargetWithinRangeTrigger {
        if (range <= 0.0f) {
            throw new IllegalArgumentException("Range must be positive, got: " + range);
        }
    }

    @Override
    public boolean evaluate(TriggerContext context) {
        Ref<EntityStore> mobRef = context.mobRef();
        Ref<EntityStore> targetRef = context.targetRef();
        Store<EntityStore> entityStore = context.entityStore();


        if (targetRef == null) {
            return false;
        }


        TransformComponent mobTransform = entityStore.getComponent(mobRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = entityStore.getComponent(targetRef, TransformComponent.getComponentType());

        if (mobTransform == null || targetTransform == null) {
            return false;
        }

        Vector3d mobPos = mobTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();


        double dx = targetPos.getX() - mobPos.getX();
        double dy = targetPos.getY() - mobPos.getY();
        double dz = targetPos.getZ() - mobPos.getZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        return distance <= range;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "TargetWithinRangeTrigger{range=" + range + " blocks}";
    }
}
