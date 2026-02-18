package com.frotty27.rpgmobs.systems.combat;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class RPGMobsCombatStateSystem extends DamageEventSystem {

    private final RPGMobsPlugin plugin;

    public RPGMobsCombatStateSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatMap.getComponentType();
    }

    @Override
    public @NonNull Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
                      new SystemGroupDependency<>(Order.BEFORE, DamageModule.get().getInspectDamageGroup())
        );
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {

        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityIndex);

        RPGMobsTierComponent tier = store.getComponent(victimRef, plugin.getRPGMobsComponentType());
        if (tier == null) return;

        RPGMobsCombatTrackingComponent combat = store.getComponent(victimRef, plugin.getCombatTrackingComponentType());
        if (combat == null) {
            combat = new RPGMobsCombatTrackingComponent();
        }

        Damage.Source damageSource = damage.getSource();
        Ref<EntityStore> attackerRef = null;
        if (damageSource instanceof Damage.EntitySource attackerEntitySource) {
            attackerRef = attackerEntitySource.getRef();
        }

        if (attackerRef == null) return;

        combat.updateDamageTarget(attackerRef, plugin.getTickClock().getTick());
        commandBuffer.replaceComponent(victimRef, plugin.getCombatTrackingComponentType(), combat);
    }
}
