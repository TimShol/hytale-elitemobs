package com.frotty27.elitemobs.systems.combat;

import com.frotty27.elitemobs.api.event.EliteMobAggroEvent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.combat.EliteMobsCombatTrackingComponent;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class EliteMobsCombatStateSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EliteMobsPlugin plugin;

    public EliteMobsCombatStateSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatMap.getComponentType();
    }

    @Override
    public @NonNull Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
            new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
            new SystemGroupDependency<>(Order.BEFORE, DamageModule.get().getInspectDamageGroup())
        );
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {

        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityIndex);

        EliteMobsTierComponent tier = store.getComponent(victimRef, plugin.getEliteMobsComponent());
        if (tier == null) return;

        EliteMobsCombatTrackingComponent combat = store.getComponent(victimRef,
            plugin.getCombatTrackingComponent());
        if (combat == null) {
            combat = new EliteMobsCombatTrackingComponent();
        }

        Damage.Source damageSource = damage.getSource();
        Ref<EntityStore> attackerRef = null;
        if (damageSource instanceof Damage.EntitySource attackerEntitySource) {
            attackerRef = attackerEntitySource.getRef();
        }

        if (attackerRef == null) return;

        combat.updateDamageTarget(attackerRef, plugin.getTickClock().getTick());
        commandBuffer.replaceComponent(victimRef, plugin.getCombatTrackingComponent(), combat);
    }
}
