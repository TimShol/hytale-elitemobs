package com.frotty27.rpgmobs.systems.death;

import com.frotty27.rpgmobs.api.events.RPGMobsDeathEvent;
import com.frotty27.rpgmobs.api.events.RPGMobsDropsEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.InstancesConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.exceptions.RPGMobsException;
import com.frotty27.rpgmobs.exceptions.RPGMobsSystemException;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.Constants;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.Constants.UTILITY_SLOT_INDEX;
import static com.frotty27.rpgmobs.utils.InventoryHelpers.copyExactSingle;

public final class RPGMobsDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double CLEANUP_RADIUS_BLOCKS = 2.0;
    private static final double DROP_SPAWN_Y_OFFSET = 1.0;
    private static final long CULL_WINDOW_TICKS = 2L;
    private static final long MIN_MOB_DROPS_SPAWN_DELAY_TICKS = CULL_WINDOW_TICKS + 1;

    private static final double[] EXTRA_DROPS_DELAY_SECONDS_BY_TIER = {0.0, 0.0, 0.0, 0.5, 1.0};

    private final RPGMobsPlugin plugin;
    private final Random random = new Random();
    private final RPGMobsDropsHandler dropsHandler = new RPGMobsDropsHandler(this);
    private final RPGMobsMinionDeathHandler minionDeathHandler = new RPGMobsMinionDeathHandler(this);

    public RPGMobsDeathSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Constants.NPC_COMPONENT_TYPE, DeathComponent.getComponentType());
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death,
                                 @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        try {
            processDeath(ref, death, store, commandBuffer);
        } catch (RPGMobsException e) {
            throw e;
        } catch (Exception e) {
            throw new RPGMobsSystemException("Error in RPGMobsDeathSystem", e);
        }
    }

    private void processDeath(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store,
                              CommandBuffer<EntityStore> commandBuffer) {
        if (minionDeathHandler.handle(ref, death, store, commandBuffer)) {
            return;
        }

        dropsHandler.handle(ref, death, store);
    }

    void processOnDeath(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store) {
        RPGMobsConfig cfg = plugin.getConfig();
        if (cfg == null) return;

        NPCEntity npc = store.getComponent(ref, Constants.NPC_COMPONENT_TYPE);
        if (npc == null) return;

        RPGMobsTierComponent tier = store.getComponent(ref, plugin.getRPGMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;


        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotation == null) return;

        RPGMobsSummonMinionTrackingComponent tracking = store.getComponent(ref,
                                                                           plugin.getSummonMinionTrackingComponentType()
        );

        var spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem != null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                UUID deadSummonerId = uuidComponent.getUuid();
                long deathTick = plugin.getTickClock().getTick();
                int aliveCount = tracking != null ? tracking.summonedAliveCount : 0;
                RPGMobsLogger.debug(LOGGER,
                                    "[DeathSystem] Summoner died, queuing minion chain despawn for summonerId=%s alive=%d",
                                    RPGMobsLogLevel.INFO,
                                    deadSummonerId,
                                    aliveCount
                );
                spawnSystem.queueSummonerDeath(deadSummonerId, deathTick);
            }
        }

        int tierId = clampTierIndex(tier.tierIndex);
        death.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);

        plugin.getMobDropsCleanupManager().addCullZone(transformComponent.getPosition().clone(),
                                                       CLEANUP_RADIUS_BLOCKS,
                                                       CULL_WINDOW_TICKS
        );

        // Resolve instance rule for loot overrides
        InstancesConfig.InstanceRule instanceRule = resolveInstanceLootRule(npc);

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();
        Inventory inv = npc.getInventory();
        if (inv != null) {
            addWeaponDrop(cfg, instanceRule, inv, drops);
            addArmorDrops(cfg, instanceRule, inv, drops);
            addUtilityDrop(cfg, instanceRule, inv, drops);
        }

        addExtraVanillaDroplistRolls(cfg, tierId, npc, drops, instanceRule);

        // Per-role drops > instance-wide extraDrops > global defaultExtraDrops
        String roleName = npc.getRoleName() != null ? npc.getRoleName() : "";
        InstancesConfig.MobOverride mobOverride = (instanceRule != null)
                ? InstancesConfig.resolveMobOverride(instanceRule, roleName) : null;

        if (mobOverride != null && mobOverride.drops != null) {
            addExtraDropsFromList(mobOverride.drops, tierId, drops);
        } else if (instanceRule != null && instanceRule.extraDrops != null) {
            addExtraDropsFromList(instanceRule.extraDrops, tierId, drops);
        } else {
            addExtraDropsFromList(cfg.lootConfig.defaultExtraDrops, tierId, drops);
        }

        var pos = transformComponent.getPosition().clone().add(0.0, DROP_SPAWN_Y_OFFSET, 0.0);
        var rot = headRotation.getRotation().clone();


        RPGMobsCombatTrackingComponent combatTracking = store.getComponent(ref,
                                                                           plugin.getCombatTrackingComponentType()
        );
        Ref<EntityStore> killerRef = (combatTracking != null) ? combatTracking.getBestTarget() : null;
        if (killerRef != null && !killerRef.isValid()) killerRef = null;
        plugin.getEventBus().fire(new RPGMobsDeathEvent(npc.getWorld(),
                                                        ref,
                                                        tierId,
                                                        roleName,
                                                        killerRef,
                                                        transformComponent.getPosition().clone()
        ));

        if (drops.isEmpty()) return;


        var dropsEvent = new RPGMobsDropsEvent(npc.getWorld(), ref, tierId, roleName, drops, pos.clone());
        plugin.getEventBus().fire(dropsEvent);
        if (dropsEvent.isCancelled() || drops.isEmpty()) return;

        double seconds = 0.0;
        if (EXTRA_DROPS_DELAY_SECONDS_BY_TIER.length > tierId) {
            seconds = Math.max(0.0, EXTRA_DROPS_DELAY_SECONDS_BY_TIER[tierId]);
        }

        long requestedDelayTicks = Math.round(seconds * Constants.TICKS_PER_SECOND);
        long delayTicks = Math.max(MIN_MOB_DROPS_SPAWN_DELAY_TICKS, requestedDelayTicks);

        plugin.getExtraDropsScheduler().enqueueDrops(delayTicks, pos, rot, drops, null);
    }

    private void addWeaponDrop(RPGMobsConfig cfg, InstancesConfig.@Nullable InstanceRule instanceRule,
                               Inventory inv, List<ItemStack> drops) {
        double chance = (instanceRule != null && instanceRule.dropWeaponChance != null)
                ? instanceRule.dropWeaponChance : cfg.lootConfig.dropWeaponChance;
        if (random.nextDouble() > chance) return;
        byte slot = inv.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;
        ItemStack mainHand = inv.getHotbar().getItemStack(slot);
        if (mainHand != null && !mainHand.isEmpty()) {
            drops.add(copyExactSingle(mainHand));
        }
    }

    private void addArmorDrops(RPGMobsConfig cfg, InstancesConfig.@Nullable InstanceRule instanceRule,
                               Inventory inv, List<ItemStack> drops) {
        double chance = (instanceRule != null && instanceRule.dropArmorPieceChance != null)
                ? instanceRule.dropArmorPieceChance : cfg.lootConfig.dropArmorPieceChance;
        for (ItemArmorSlot slot : ItemArmorSlot.values()) {
            if (random.nextDouble() > chance) continue;
            ItemStack item = inv.getArmor().getItemStack((short) slot.ordinal());
            if (item != null && !item.isEmpty()) {
                drops.add(copyExactSingle(item));
            }
        }
    }

    private void addUtilityDrop(RPGMobsConfig cfg, InstancesConfig.@Nullable InstanceRule instanceRule,
                                Inventory inv, List<ItemStack> drops) {
        double chance = (instanceRule != null && instanceRule.dropOffhandItemChance != null)
                ? instanceRule.dropOffhandItemChance : cfg.lootConfig.dropOffhandItemChance;
        if (random.nextDouble() > chance) return;
        ItemStack utility = inv.getHotbar().getItemStack((short) UTILITY_SLOT_INDEX);
        if (utility != null && !utility.isEmpty()) {
            drops.add(copyExactSingle(utility));
        }
    }

    private void addExtraDrops(RPGMobsConfig cfg, int tierId, List<ItemStack> drops) {
        addExtraDropsFromList(cfg.lootConfig.defaultExtraDrops, tierId, drops);
    }

    private void addExtraVanillaDroplistRolls(RPGMobsConfig cfg, int tierId, NPCEntity npc,
                                               ObjectArrayList<ItemStack> drops,
                                               InstancesConfig.@Nullable InstanceRule instanceRule) {
        // Instance override takes priority over global
        int[] extraRollsPerTier = (instanceRule != null && instanceRule.vanillaDroplistExtraRollsPerTier != null)
                ? instanceRule.vanillaDroplistExtraRollsPerTier
                : cfg.lootConfig.vanillaDroplistExtraRollsPerTier;
        if (extraRollsPerTier == null || extraRollsPerTier.length < Constants.TIERS_AMOUNT) return;

        int extraRolls = Math.max(0, extraRollsPerTier[tierId]);
        if (extraRolls == 0) return;

        var role = npc.getRole();
        if (role == null) return;

        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        ItemModule itemModule = ItemModule.get();
        if (itemModule == null || !itemModule.isEnabled()) return;

        for (int i = 0; i < extraRolls; i++) {
            List<ItemStack> rolled = itemModule.getRandomItemDrops(dropListId);
            if (rolled != null && !rolled.isEmpty()) {
                drops.addAll(rolled);
            }
        }
    }

    private InstancesConfig.InstanceRule resolveInstanceLootRule(NPCEntity npc) {
        InstancesConfig instancesConfig = plugin.getInstancesConfig();
        if (instancesConfig == null || !instancesConfig.enabled) return null;
        if (npc.getWorld() == null) return null;
        return instancesConfig.resolveRule(npc.getWorld().getName());
    }

    private void addExtraDropsFromList(List<RPGMobsConfig.ExtraDropRule> rules, int tierId,
                                        List<ItemStack> drops) {
        if (rules == null || rules.isEmpty()) return;

        for (RPGMobsConfig.ExtraDropRule rule : rules) {
            if (rule == null) continue;
            if (rule.itemId == null || rule.itemId.isBlank()) continue;
            if (tierId < rule.minTierInclusive || tierId > rule.maxTierInclusive) continue;
            if (rule.chance <= 0.0) continue;
            if (rule.chance < 1.0 && random.nextDouble() > rule.chance) continue;

            int min = Math.max(1, rule.minQty);
            int max = Math.max(min, rule.maxQty);
            int qty = (min == max) ? min : min + random.nextInt((max - min) + 1);

            drops.add(new ItemStack(rule.itemId, qty));
        }
    }

    void decrementSummonerAliveCount(NPCEntity npc, RPGMobsSummonedMinionComponent minion, Store<EntityStore> store,
                                     CommandBuffer<EntityStore> commandBuffer) {
        if (minion.summonerId == null) return;
        var world = npc.getWorld();
        if (world == null) return;
        Ref<EntityStore> summonerRef = world.getEntityRef(minion.summonerId);
        if (summonerRef == null || !summonerRef.isValid()) return;
        RPGMobsSummonMinionTrackingComponent summonerTracking = store.getComponent(summonerRef,
                                                                                   plugin.getSummonMinionTrackingComponentType()
        );
        if (summonerTracking == null) return;
        summonerTracking.decrementCount();
        commandBuffer.replaceComponent(summonerRef, plugin.getSummonMinionTrackingComponentType(), summonerTracking);
    }

    RPGMobsPlugin getPlugin() {
        return plugin;
    }

    ComponentType<EntityStore, RPGMobsSummonedMinionComponent> getSummonedMinionComponentType() {
        return plugin.getSummonedMinionComponentType();
    }
}
