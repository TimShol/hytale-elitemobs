package com.frotty27.elitemobs.systems.ability.trigger;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TriggerContext(
    Ref<EntityStore> mobRef,
    Store<EntityStore> entityStore,
    EliteMobsPlugin plugin,
    long currentTick,
    float currentHealthPercent,
    Ref<EntityStore> targetRef,
    boolean isInCombat,
    long cooldownTicksRemaining
) {


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Ref<EntityStore> mobRef;
        private Store<EntityStore> entityStore;
        private EliteMobsPlugin plugin;
        private long currentTick;
        private float currentHealthPercent = 1.0f;
        private Ref<EntityStore> targetRef;
        private boolean isInCombat = false;
        private long cooldownTicksRemaining = 0;

        public Builder mobRef(Ref<EntityStore> mobRef) {
            this.mobRef = mobRef;
            return this;
        }

        public Builder entityStore(Store<EntityStore> entityStore) {
            this.entityStore = entityStore;
            return this;
        }

        public Builder plugin(EliteMobsPlugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public Builder currentTick(long currentTick) {
            this.currentTick = currentTick;
            return this;
        }

        public Builder currentHealthPercent(float currentHealthPercent) {
            this.currentHealthPercent = currentHealthPercent;
            return this;
        }

        public Builder targetRef(Ref<EntityStore> targetRef) {
            this.targetRef = targetRef;
            return this;
        }

        public Builder isInCombat(boolean isInCombat) {
            this.isInCombat = isInCombat;
            return this;
        }

        public Builder cooldownTicksRemaining(long cooldownTicksRemaining) {
            this.cooldownTicksRemaining = cooldownTicksRemaining;
            return this;
        }

        public TriggerContext build() {
            return new TriggerContext(
                mobRef,
                entityStore,
                plugin,
                currentTick,
                currentHealthPercent,
                targetRef,
                isInCombat,
                cooldownTicksRemaining
            );
        }
    }
}
