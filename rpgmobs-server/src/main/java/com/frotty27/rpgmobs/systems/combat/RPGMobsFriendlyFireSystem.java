package com.frotty27.rpgmobs.systems.combat;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.Constants;
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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(entityIndex);
        if (victimRef == null || !victimRef.isValid()) return;

        RPGMobsTierComponent victimTier = store.getComponent(victimRef, plugin.getRPGMobsComponentType());

        Damage.Source damageSource = damage.getSource();

        if (!(damageSource instanceof Damage.EntitySource attackerEntitySource)) {
            if (victimTier != null) {
                ResolvedConfig resolved = resolveConfig(store, victimRef);
                if (resolved != null && resolved.eliteFallDamageDisabled) {
                    damage.setAmount(0f);
                }
            }
            return;
        }

        Ref<EntityStore> attackerRef = attackerEntitySource.getRef();
        if (!attackerRef.isValid()) return;

        if (victimTier != null) {
            RPGMobsTierComponent attackerTier = store.getComponent(attackerRef, plugin.getRPGMobsComponentType());
            if (attackerTier != null) {
                ResolvedConfig resolved = resolveConfig(store, victimRef);
                if (resolved != null && (resolved.eliteFriendlyFireDisabled || resolved.eliteNoAggroOnElite)) {
                    damage.setAmount(0f);
                    return;
                }
            }
        }

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

    private @Nullable ResolvedConfig resolveConfig(Store<EntityStore> store,
                                                    Ref<EntityStore> entityRef) {
        NPCEntity npc = store.getComponent(entityRef, Constants.NPC_COMPONENT_TYPE);
        if (npc == null || npc.getWorld() == null) return null;

        return plugin.getResolvedConfig(npc.getWorld().getName());
    }

    private static UUID getEntityUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        return (uuidComponent != null) ? uuidComponent.getUuid() : null;
    }
}
