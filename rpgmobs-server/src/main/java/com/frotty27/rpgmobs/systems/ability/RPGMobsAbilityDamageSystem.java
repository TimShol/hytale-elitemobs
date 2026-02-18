package com.frotty27.rpgmobs.systems.ability;

import com.frotty27.rpgmobs.api.events.RPGMobsDamageReceivedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.exceptions.RPGMobsException;
import com.frotty27.rpgmobs.exceptions.RPGMobsSystemException;
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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsAbilityDamageSystem extends DamageEventSystem {

    private final RPGMobsPlugin plugin;

    public RPGMobsAbilityDamageSystem(RPGMobsPlugin plugin) {
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
    public void handle(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer,
                       @NonNull Damage damage) {
        try {
            processHandle(entityIndex, archetypeChunk, entityStore, commandBuffer, damage);
        } catch (RPGMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new RPGMobsSystemException("Error in RPGMobsAbilityDamageSystem", e);
        }
    }

    private void processHandle(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                               @NonNull Store<EntityStore> entityStore,
                               @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull Damage damage) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(entityIndex);
        RPGMobsTierComponent tierComponent = entityStore.getComponent(victimRef, plugin.getRPGMobsComponentType());
        if (tierComponent == null || tierComponent.tierIndex < 0) return;

        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex, NPC_COMPONENT_TYPE);
        int tierIndex = clampTierIndex(tierComponent.tierIndex);
        long currentTick = plugin.getTickClock().getTick();

        Damage.Source damageSource = damage.getSource();
        com.hypixel.hytale.component.Ref<EntityStore> dmgAttackerRef = null;
        if (damageSource instanceof Damage.EntitySource src) {
            dmgAttackerRef = src.getRef();
        }
        String victimRole = npcEntity != null && npcEntity.getRoleName() != null ? npcEntity.getRoleName() : "";
        plugin.getEventBus().fire(new RPGMobsDamageReceivedEvent(npcEntity != null ? npcEntity.getWorld() : null,
                                                                 victimRef,
                                                                 tierIndex,
                                                                 victimRole,
                                                                 dmgAttackerRef,
                                                                 damage.getAmount()
        ));

        plugin.getFeatureRegistry().onDamageAll(plugin,
                                                config,
                                                victimRef,
                                                entityStore,
                                                commandBuffer,
                                                tierComponent,
                                                npcEntity,
                                                tierIndex,
                                                currentTick,
                                                damage
        );
    }
}
