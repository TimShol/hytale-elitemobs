package com.frotty27.elitemobs.systems.death;

import com.frotty27.elitemobs.components.EliteMobsSummonedMinionComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
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
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.frotty27.elitemobs.utils.ClampingHelpers.*;
import static com.frotty27.elitemobs.utils.Constants.TIERS_AMOUNT;
import static com.frotty27.elitemobs.utils.Constants.UTILITY_SLOT_INDEX;
import static com.frotty27.elitemobs.utils.InventoryHelpers.copyExactSingle;
import static com.frotty27.elitemobs.utils.InventoryHelpers.getContainerSizeSafe;

public final class EliteMobsDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double DROP_CHANCE_DIVISOR = 2.0;

    private static final double CLEANUP_RADIUS_BLOCKS = 2.0;
    private static final double DROP_SPAWN_Y_OFFSET = 1.0;
    private static final String ORE_PREFIX = "Ore_";
    private static final String INGOT_PREFIX = "Ingredient_Bar_";

    private static final long CULL_WINDOW_TICKS = 2L;

    private static final long MIN_MOB_DROPS_SPAWN_DELAY_TICKS = CULL_WINDOW_TICKS + 1;

    private static final double[] EXTRA_DROPS_DELAY_SECONDS_BY_TIER = {
            0.0, // tier 0
            0.0, // tier 1
            0.0, // tier 2
            0.0, // tier 3
            0.0  // tier 4
    };

    private final EliteMobsPlugin plugin;
    private final Random rng = new Random();
    private final EliteMobsMinionDeathHandler minionDeathHandler = new EliteMobsMinionDeathHandler(this);
    private final EliteMobsDropsHandler dropsHandler = new EliteMobsDropsHandler(this);

    public EliteMobsDeathSystem(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                NPCEntity.getComponentType(),
                TransformComponent.getComponentType(),
                HeadRotation.getComponentType()
        );
    }

    @Override
    public void onComponentAdded(
            @NonNull Ref<EntityStore> ref,
            @NonNull DeathComponent death,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> cb
    ) {
        if (minionDeathHandler.handle(ref, death, store, cb)) return;
        dropsHandler.handle(ref, death, store, cb);
    }

    void processOnDeath(
            @NonNull Ref<EntityStore> ref,
            @NonNull DeathComponent death,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> cb
    ) {
        EliteMobsConfig cfg = plugin.getConfig();
        if (cfg == null) return;

        NPCEntity npc = store.getComponent(ref, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) return;

        EliteMobsTierComponent tier = store.getComponent(ref, plugin.getEliteMobsComponent());
        if (tier == null || tier.tierIndex < 0) return;
        if (tier.disableDrops) return;

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        if (npc == null || transformComponent == null || headRotation == null) return;

        var spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem != null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null && uuidComponent.getUuid() != null) {
                spawnSystem.queueMinionChainDespawn(uuidComponent.getUuid(), store, plugin.getTickClock().getTick());
            }
        }

        int tierId = clampTierIndex(tier.tierIndex);

        // Prevent vanilla equipment loss rules from mutating inventory (we handle drops ourselves)
        death.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);

        // Cull vanilla item entities around the death position for a short window
        plugin.getMobDropsCleanupManager().addCullZone(
                transformComponent.getPosition().clone(),
                CLEANUP_RADIUS_BLOCKS,
                CULL_WINDOW_TICKS
        );

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();

        addExtraVanillaDroplistRolls(cfg, tierId, npc, drops);

        Inventory inv = npc.getInventory();
        if (inv != null) {
            addWeaponDrop(cfg, tierId, npc, inv, drops);
            addArmorDrops(cfg, inv, drops);
            addUtilityDrop(cfg, tierId, inv, drops);
        }

        addExtraDrops(cfg, tierId, drops);

        if (drops.isEmpty()) return;

        var pos = transformComponent.getPosition().clone().add(0.0, DROP_SPAWN_Y_OFFSET, 0.0);
        var rot = headRotation.getRotation().clone();

        double seconds = 0.0;
        if (EXTRA_DROPS_DELAY_SECONDS_BY_TIER.length > tierId) {
            seconds = Math.max(0.0, EXTRA_DROPS_DELAY_SECONDS_BY_TIER[tierId]);
        }

        long requestedDelayTicks = Math.round(seconds * Constants.TICKS_PER_SECOND);
        long delayTicks = Math.max(MIN_MOB_DROPS_SPAWN_DELAY_TICKS, requestedDelayTicks);

        if (cfg.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "drops delay tier=%d seconds=%.3f => ticks=%d (min=%d)",
                    EliteMobsLogLevel.INFO,
                    tierId,
                    seconds,
                    delayTicks,
                    MIN_MOB_DROPS_SPAWN_DELAY_TICKS
            );
        }

        plugin.getExtraDropsScheduler().enqueueDrops(delayTicks, pos, rot, drops, null);
    }

    void decrementSummonerAliveCount(
            NPCEntity npc,
            EliteMobsSummonedMinionComponent minion,
            Store<EntityStore> store
    ) {
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


    private void addExtraVanillaDroplistRolls(
            EliteMobsConfig cfg,
            int tierId,
            NPCEntity npc,
            ObjectArrayList<ItemStack> out
    ) {
        int[] rollsByTier = cfg.loot.vanillaDroplistMultiplierPerTier;
        if (rollsByTier == null || rollsByTier.length < TIERS_AMOUNT) return;

        int rolls = Math.max(0, rollsByTier[tierId]);
        if (rolls == 0) return;

        var role = npc.getRole();
        if (role == null) return;

        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        ItemModule itemModule = ItemModule.get();
        if (itemModule == null || !itemModule.isEnabled()) return;

        for (int r = 0; r < rolls; r++) {
            List<ItemStack> reroll = itemModule.getRandomItemDrops(dropListId);
            if (!reroll.isEmpty()) out.addAll(reroll);
        }
    }

    private void addWeaponDrop(
            EliteMobsConfig cfg,
            int tierId,
            NPCEntity npc,
            Inventory inv,
            ObjectArrayList<ItemStack> out
    ) {
        double chance = clampDouble(cfg.loot.dropWeaponChance) / DROP_CHANCE_DIVISOR;
        double roll = rng.nextDouble();

        if (cfg.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "weapon roll=%.5f chance=%.5f role=%s tier=%d",
                    EliteMobsLogLevel.INFO,
                    roll,
                    chance,
                    npc.getRoleName(),
                    tierId
            );
        }

        if (roll >= chance) return;

        ItemStack inHand = inv.getItemInHand();
        if (inHand == null || inHand.isEmpty()) return;

        out.add(copyExactSingle(inHand));
    }

    private void addArmorDrops(EliteMobsConfig cfg, Inventory inv, ObjectArrayList<ItemStack> out) {
        ItemContainer armor = inv.getArmor();
        if (armor == null) return;

        double perPieceChance = clampDouble(cfg.loot.dropArmorPieceChance) / DROP_CHANCE_DIVISOR;

        int equippedCount = 0;
        int droppedCount = 0;

        for (ItemArmorSlot slot : ItemArmorSlot.values()) {
            ItemStack equipped = armor.getItemStack((short) slot.ordinal());
            if (equipped == null || equipped.isEmpty()) continue;

            equippedCount++;

            double roll = rng.nextDouble();
            if (roll >= perPieceChance) continue;

            droppedCount++;
            out.add(copyExactSingle(equipped));
        }

        if (cfg.debug.isDebugModeEnabled && equippedCount > 0) {
            double atLeastOne = 1.0 - Math.pow(1.0 - perPieceChance, equippedCount);
            EliteMobsLogger.debug(
                    LOGGER,
                    "armor equipped=%d dropped=%d perPieceChance=%.3f => P(atLeastOne)=%.3f",
                    EliteMobsLogLevel.INFO,
                    equippedCount,
                    droppedCount,
                    perPieceChance,
                    atLeastOne
            );
        }
    }

    private void addUtilityDrop(EliteMobsConfig cfg, int tierId, Inventory inv, ObjectArrayList<ItemStack> out) {
        ItemContainer util = inv.getUtility();
        if (util == null) return;

        int size = getContainerSizeSafe(util);
        if (size <= 0) return;

        int slot = clampInt(UTILITY_SLOT_INDEX, 0, size - 1);

        ItemStack item = util.getItemStack((short) slot);
        if (item == null || item.isEmpty()) return;

        double chance = clampDouble(cfg.loot.dropOffhandItemChance);
        double roll = rng.nextDouble();

        if (cfg.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "utility roll=%.5f chance=%.5f slot=%d/%d item=%s tier=%d",
                    EliteMobsLogLevel.INFO,
                    roll,
                    chance,
                    slot,
                    size,
                    item.getItemId(),
                    tierId
            );
        }

        if (roll >= chance) return;

        out.add(copyExactSingle(item));
    }

    private void addExtraDrops(EliteMobsConfig cfg, int tierId, ObjectArrayList<ItemStack> out) {
        if (cfg.loot.extraDrops == null || cfg.loot.extraDrops.isEmpty()) return;

        for (EliteMobsConfig.ExtraDropRule rule : cfg.loot.extraDrops) {
            if (rule == null) continue;

            String itemId = rule.itemId;
            if (itemId == null || itemId.isBlank()) continue;

            int minTier = clampTierIndex(rule.minTierInclusive);
            int maxTier = clampTierIndex(rule.maxTierInclusive);
            if (tierId < minTier || tierId > maxTier) continue;

            double chance = clampDouble(rule.chance);
            if (isOreOrIngot(itemId)) {
                chance = chance / DROP_CHANCE_DIVISOR;
            }
            double roll = rng.nextDouble();
            if (roll >= chance) continue;

            int minQty = Math.max(1, rule.minQty);
            int maxQty = Math.max(minQty, rule.maxQty);
            int qty = (maxQty == minQty) ? minQty : (minQty + rng.nextInt(maxQty - minQty + 1));

            out.add(new ItemStack(itemId, qty));

            if (cfg.debug.isDebugModeEnabled) {
                EliteMobsLogger.debug(
                        LOGGER,
                        "extraDrop HIT item=%s qty=%d tier=%d roll=%.5f chance=%.5f minTier=%d maxTier=%d",
                        EliteMobsLogLevel.INFO,
                        itemId,
                        qty,
                        tierId,
                        roll,
                        chance,
                        minTier,
                        maxTier
                );
            }
        }
    }

    private static boolean isOreOrIngot(String itemId) {
        if (itemId == null) return false;
        return itemId.startsWith(ORE_PREFIX) || itemId.startsWith(INGOT_PREFIX);
    }
}
