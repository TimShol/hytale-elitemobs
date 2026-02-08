package com.frotty27.elitemobs.systems.death;

import com.frotty27.elitemobs.components.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exception.EliteMobsException;
import com.frotty27.elitemobs.exception.EliteMobsSystemException;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.utils.Constants;
import com.frotty27.elitemobs.utils.StoreHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.*;
import static com.frotty27.elitemobs.utils.Constants.UTILITY_SLOT_INDEX;
import static com.frotty27.elitemobs.utils.InventoryHelpers.copyExactSingle;

public final class EliteMobsDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double CLEANUP_RADIUS_BLOCKS = 2.0;
    private static final double DROP_SPAWN_Y_OFFSET = 1.0;
    private static final long CULL_WINDOW_TICKS = 2L;
    private static final long MIN_MOB_DROPS_SPAWN_DELAY_TICKS = CULL_WINDOW_TICKS + 1;

    private static final double[] EXTRA_DROPS_DELAY_SECONDS_BY_TIER = {0.0, 0.0, 0.0, 0.5, 1.0};

    private final EliteMobsPlugin plugin;
    private final Random random = new Random();
    private final EliteMobsDropsHandler dropsHandler = new EliteMobsDropsHandler(this);
    private final EliteMobsMinionDeathHandler minionDeathHandler = new EliteMobsMinionDeathHandler(this);

    public EliteMobsDeathSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), DeathComponent.getComponentType());
    }

    @Override
    public void onComponentAdded(
            Ref<EntityStore> ref,
            DeathComponent death,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {
        try {
            processDeath(ref, death, store, commandBuffer);
        } catch (EliteMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new EliteMobsSystemException("Error in EliteMobsDeathSystem", e);
        }
    }

    private void processDeath(
            Ref<EntityStore> ref,
            DeathComponent death,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {
        if (minionDeathHandler.handle(ref, death, store, commandBuffer)) {
            return;
        }
        
        dropsHandler.handle(ref, death, store, commandBuffer);
    }

    void processOnDeath(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        EliteMobsConfig cfg = plugin.getConfig();
        if (cfg == null) return;

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) return;

        EliteMobsTierComponent tier = store.getComponent(ref, plugin.getEliteMobsComponent());
        if (tier == null || tier.tierIndex < 0) return;
        if (tier.disableDrops) return;

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotation == null) return;

        var spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem != null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null && uuidComponent.getUuid() != null) {
                spawnSystem.queueMinionChainDespawn(uuidComponent.getUuid(), store, plugin.getTickClock().getTick());
            }
        }

        int tierId = clampTierIndex(tier.tierIndex);
        death.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);

        plugin.getMobDropsCleanupManager().addCullZone(
                transformComponent.getPosition().clone(),
                CLEANUP_RADIUS_BLOCKS,
                CULL_WINDOW_TICKS
        );

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();
        Inventory inv = npc.getInventory();
        if (inv != null) {
            addWeaponDrop(cfg, tierId, npc, inv, drops);
            addArmorDrops(cfg, inv, drops);
            addUtilityDrop(cfg, tierId, inv, drops);
        }

        if (drops.isEmpty()) return;

        var pos = transformComponent.getPosition().clone().add(0.0, DROP_SPAWN_Y_OFFSET, 0.0);
        var rot = headRotation.getRotation().clone();

        double seconds = 0.0;
        if (EXTRA_DROPS_DELAY_SECONDS_BY_TIER.length > tierId) {
            seconds = Math.max(0.0, EXTRA_DROPS_DELAY_SECONDS_BY_TIER[tierId]);
        }

        long requestedDelayTicks = Math.round(seconds * Constants.TICKS_PER_SECOND);
        long delayTicks = Math.max(MIN_MOB_DROPS_SPAWN_DELAY_TICKS, requestedDelayTicks);

        plugin.getExtraDropsScheduler().enqueueDrops(delayTicks, pos, rot, drops, null);
    }

    private void addWeaponDrop(EliteMobsConfig cfg, int tierId, NPCEntity npc, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropWeaponChance;
        if (random.nextDouble() > chance) return;
        byte slot = inv.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;
        ItemStack mainHand = inv.getHotbar().getItemStack((short) slot);
        if (mainHand != null && !mainHand.isEmpty()) {
            drops.add(copyExactSingle(mainHand));
        }
    }

    private void addArmorDrops(EliteMobsConfig cfg, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropArmorPieceChance;
        for (ItemArmorSlot slot : ItemArmorSlot.values()) {
            if (random.nextDouble() > chance) continue;
            ItemStack item = inv.getArmor().getItemStack((short) slot.ordinal());
            if (item != null && !item.isEmpty()) {
                drops.add(copyExactSingle(item));
            }
        }
    }

    private void addUtilityDrop(EliteMobsConfig cfg, int tierId, Inventory inv, List<ItemStack> drops) {
        double chance = cfg.lootConfig.dropOffhandItemChance;
        if (random.nextDouble() > chance) return;
        ItemStack utility = inv.getHotbar().getItemStack((short) UTILITY_SLOT_INDEX);
        if (utility != null && !utility.isEmpty()) {
            drops.add(copyExactSingle(utility));
        }
    }

    void decrementSummonerAliveCount(NPCEntity npc, EliteMobsSummonedMinionComponent minion, Store<EntityStore> store) {
        if (minion.summonerId == null) return;
        var world = npc.getWorld();
        if (world == null) return;
        Ref<EntityStore> summonerRef = world.getEntityRef(minion.summonerId);
        if (summonerRef == null || !summonerRef.isValid()) return;
        StoreHelpers.withEntity(store, summonerRef, (summonerChunk, summonerCb, summonerIndex) -> {
            EliteMobsTierComponent summonerTier = store.getComponent(summonerRef, plugin.getEliteMobsComponent());
            if (summonerTier == null) return;
            summonerTier.summonedAliveCount = Math.max(0, summonerTier.summonedAliveCount - 1);
            summonerCb.replaceComponent(summonerRef, plugin.getEliteMobsComponent(), summonerTier);
        });
    }

    ComponentType<EntityStore, EliteMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return plugin.getSummonedMinionComponent();
    }
}