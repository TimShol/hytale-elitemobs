package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exception.EliteMobsException;
import com.frotty27.elitemobs.exception.EliteMobsSystemException;
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

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsAbilityDamageSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EliteMobsPlugin plugin;

    public EliteMobsAbilityDamageSystem(EliteMobsPlugin plugin) {
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
    public void handle(
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull Damage damage
    ) {
        try {
            processHandle(entityIndex, archetypeChunk, entityStore, commandBuffer, damage);
        } catch (EliteMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new EliteMobsSystemException("Error in EliteMobsAbilityDamageSystem", e);
        }
    }

    private void processHandle(
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull Damage damage
    ) {
        EliteMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(entityIndex);
        EliteMobsTierComponent tierComponent =
                entityStore.getComponent(victimRef, plugin.getEliteMobsComponent());
        if (tierComponent == null || tierComponent.tierIndex < 0) return;

        NPCEntity npcEntity = archetypeChunk.getComponent(entityIndex, NPCEntity.getComponentType());
        int tierIndex = clampTierIndex(tierComponent.tierIndex);
        long currentTick = plugin.getTickClock().getTick();

        Damage.Source damageSource = damage.getSource();
        if (damageSource instanceof Damage.EntitySource attackerEntitySource) {
            Ref<EntityStore> attackerRef = attackerEntitySource.getRef();
            if (attackerRef != null && attackerRef.isValid()) {
                tierComponent.lastAggroRef = attackerRef;
                tierComponent.lastAggroTick = currentTick;
                commandBuffer.replaceComponent(victimRef, plugin.getEliteMobsComponent(), tierComponent);
                if (config.debugConfig.isDebugModeEnabled) {
                    EliteMobsLogger.debug(
                            LOGGER,
                            "Aggro set: victimRole=%s attackerRef=%s tick=%d",
                            EliteMobsLogLevel.INFO,
                            npcEntity == null ? "null" : npcEntity.getRoleName(),
                            attackerRef.toString(),
                            currentTick
                    );
                }
            }
        }

        plugin.getFeatureRegistry().onDamageAll(
                plugin,
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