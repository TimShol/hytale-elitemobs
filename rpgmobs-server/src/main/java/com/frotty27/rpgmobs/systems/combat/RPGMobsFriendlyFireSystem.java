package com.frotty27.rpgmobs.systems.combat;

import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.UUID;

public final class RPGMobsFriendlyFireSystem extends DamageEventSystem {

    private final RPGMobsPlugin plugin;

    public RPGMobsFriendlyFireSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatMap.getComponentType();
    }

    @Override
    public @NonNull Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
                      new SystemGroupDependency<>(Order.BEFORE, DamageModule.get().getFilterDamageGroup())
        );
    }

    @Override
    public void handle(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer,
                       @NonNull Damage damage) {
        Damage.Source damageSource = damage.getSource();
        if (!(damageSource instanceof Damage.EntitySource attackerEntitySource)) return;

        Ref<EntityStore> attackerRef = attackerEntitySource.getRef();
        if (!attackerRef.isValid()) return;

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(entityIndex);
        if (victimRef == null || !victimRef.isValid()) return;

        RPGMobsSummonedMinionComponent attackerMinion = store.getComponent(attackerRef,
                                                                           plugin.getSummonedMinionComponentType()
        );
        RPGMobsSummonedMinionComponent victimMinion = store.getComponent(victimRef,
                                                                         plugin.getSummonedMinionComponentType()
        );

        if (attackerMinion != null && victimMinion != null) {
            if (attackerMinion.summonerId != null && victimMinion.summonerId != null && attackerMinion.summonerId.equals(
                    victimMinion.summonerId)) {
                damage.setAmount(0f);
                return;
            }
        }

        if (attackerMinion != null && attackerMinion.summonerId != null) {
            UUID victimUuid = getEntityUuid(store, victimRef);
            if (attackerMinion.summonerId.equals(victimUuid)) {
                damage.setAmount(0f);
                return;
            }
        }

        if (victimMinion != null && victimMinion.summonerId != null) {
            UUID attackerUuid = getEntityUuid(store, attackerRef);
            if (victimMinion.summonerId.equals(attackerUuid)) {
                damage.setAmount(0f);
                return;
            }
        }

        if (attackerMinion != null) {
            RPGMobsSummonMinionTrackingComponent victimTracking = store.getComponent(victimRef,
                                                                                     plugin.getSummonMinionTrackingComponentType()
            );
            if (victimTracking != null) {
                damage.setAmount(0f);
                return;
            }
        }

        if (victimMinion != null) {
            RPGMobsSummonMinionTrackingComponent attackerTracking = store.getComponent(attackerRef,
                                                                                       plugin.getSummonMinionTrackingComponentType()
            );
            if (attackerTracking != null) {
                damage.setAmount(0f);
            }
        }
    }

    private static UUID getEntityUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        return (uuidComponent != null) ? uuidComponent.getUuid() : null;
    }
}
