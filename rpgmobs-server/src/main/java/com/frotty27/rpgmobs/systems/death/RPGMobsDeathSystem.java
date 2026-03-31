package com.frotty27.rpgmobs.systems.death;

import com.frotty27.rpgmobs.api.events.RPGMobsDeathEvent;
import com.frotty27.rpgmobs.api.events.RPGMobsDropsEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.exceptions.RPGMobsException;
import com.frotty27.rpgmobs.exceptions.RPGMobsSystemException;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.Constants;
import com.frotty27.rpgmobs.utils.MobRuleCategoryHelpers;
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
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

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

        String worldName = npc.getWorld() != null ? npc.getWorld().getName() : "";
        ResolvedConfig resolved = plugin.getResolvedConfig(worldName);

        String roleName = npc.getRoleName() != null ? npc.getRoleName() : "";
        String matchedRuleKey = tier.matchedRuleKey != null ? tier.matchedRuleKey : "";

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();

        RPGMobsLogger.debug(LOGGER,
                "[DeathSystem] world=%s templates=%d matchedRule=%s defaultTemplate='%s' weaponChance=%.2f armorChance=%.2f",
                RPGMobsLogLevel.INFO,
                worldName,
                resolved.lootTemplates.size(),
                matchedRuleKey,
                resolved.defaultLootTemplate,
                resolved.dropWeaponChance,
                resolved.dropArmorPieceChance
        );

        Inventory inv = npc.getInventory();
        if (inv != null) {
            int before = drops.size();
            addWeaponDrop(resolved, inv, drops);
            addArmorDrops(resolved, inv, drops);
            addUtilityDrop(resolved, inv, drops);
            if (drops.size() > before) {
                RPGMobsLogger.debug(LOGGER,
                        "[DeathSystem] Equipment drops: %d items (weapon/armor/offhand)",
                        RPGMobsLogLevel.INFO,
                        drops.size() - before
                );
            }
        }

        int beforeVanilla = drops.size();
        addExtraVanillaDroplistRolls(tierId, npc, drops, resolved);
        if (drops.size() > beforeVanilla) {
            RPGMobsLogger.debug(LOGGER,
                    "[DeathSystem] Vanilla extra rolls: %d items",
                    RPGMobsLogLevel.INFO,
                    drops.size() - beforeVanilla
            );
        }

        int beforeLinked = drops.size();
        addLootFromLinkedTemplates(resolved, matchedRuleKey, tierId, drops);
        if (drops.size() > beforeLinked) {
            RPGMobsLogger.debug(LOGGER,
                    "[DeathSystem] Linked template drops: %d items",
                    RPGMobsLogLevel.INFO,
                    drops.size() - beforeLinked
            );
        }

        if (!resolved.defaultLootTemplate.isBlank()) {
            RPGMobsConfig.LootTemplate defaultTemplate = resolved.lootTemplates.get(resolved.defaultLootTemplate);
            if (defaultTemplate != null) {
                int beforeDefault = drops.size();
                addExtraDropsFromList(defaultTemplate.drops, tierId, drops);
                if (drops.size() > beforeDefault) {
                    RPGMobsLogger.debug(LOGGER,
                            "[DeathSystem] Default template '%s' drops: %d items",
                            RPGMobsLogLevel.INFO,
                            resolved.defaultLootTemplate,
                            drops.size() - beforeDefault
                    );
                }
            }
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

    private void addWeaponDrop(ResolvedConfig resolved,
                               Inventory inv, List<ItemStack> drops) {
        double chance = resolved.dropWeaponChance;
        if (random.nextDouble() > chance) return;
        var hotbar = inv.getHotbar();
        if (hotbar == null || hotbar.getCapacity() <= 0) return;
        byte slot = inv.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;
        if (slot >= hotbar.getCapacity()) return;
        ItemStack mainHand = hotbar.getItemStack(slot);
        if (mainHand != null && !mainHand.isEmpty()) {
            drops.add(copyExactSingle(mainHand));
        }
    }

    private void addArmorDrops(ResolvedConfig resolved,
                               Inventory inv, List<ItemStack> drops) {
        double chance = resolved.dropArmorPieceChance;
        var armorContainer = inv.getArmor();
        if (armorContainer == null || armorContainer.getCapacity() <= 0) return;
        for (ItemArmorSlot slot : ItemArmorSlot.values()) {
            if (random.nextDouble() > chance) continue;
            if (slot.ordinal() >= armorContainer.getCapacity()) continue;
            ItemStack item = armorContainer.getItemStack((short) slot.ordinal());
            if (item != null && !item.isEmpty()) {
                drops.add(copyExactSingle(item));
            }
        }
    }

    private void addUtilityDrop(ResolvedConfig resolved,
                                Inventory inv, List<ItemStack> drops) {
        double chance = resolved.dropOffhandItemChance;
        if (random.nextDouble() > chance) return;
        var utilityHotbar = inv.getHotbar();
        if (utilityHotbar == null || utilityHotbar.getCapacity() <= UTILITY_SLOT_INDEX) return;
        ItemStack utility = utilityHotbar.getItemStack((short) UTILITY_SLOT_INDEX);
        if (utility != null && !utility.isEmpty()) {
            drops.add(copyExactSingle(utility));
        }
    }

    private void addLootFromLinkedTemplates(ResolvedConfig resolved, String matchedRuleKey, int tierId,
                                              List<ItemStack> drops) {
        if (matchedRuleKey == null || matchedRuleKey.isBlank()) return;
        for (RPGMobsConfig.LootTemplate template : resolved.lootTemplates.values()) {
            if (template.linkedMobRuleKeys == null) continue;
            if (isLinkedToMob(template.linkedMobRuleKeys, matchedRuleKey, resolved.mobRuleCategoryTree)) {
                addExtraDropsFromList(template.drops, tierId, drops);
            }
        }
    }

    static boolean isLinkedToMob(List<String> linkedKeys, String matchedRuleKey,
                                   RPGMobsConfig.MobRuleCategory tree) {
        for (String key : linkedKeys) {
            if (MobRuleCategoryHelpers.isCategoryKey(key)) {
                String catName = MobRuleCategoryHelpers.fromCategoryKey(key);
                if (MobRuleCategoryHelpers.isMobKeyInCategory(tree, catName, matchedRuleKey)) {
                    return true;
                }
            } else if (key.equals(matchedRuleKey)) {
                return true;
            }
        }
        return false;
    }

    private void addExtraVanillaDroplistRolls(int tierId, NPCEntity npc,
                                              ObjectArrayList<ItemStack> drops,
                                              ResolvedConfig resolved) {
        var role = npc.getRole();
        if (role == null) return;

        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        ItemModule itemModule = ItemModule.get();
        if (itemModule == null || !itemModule.isEnabled()) return;

        int extraRolls = 0;
        int[] extraRollsPerTier = resolved.vanillaDroplistExtraRollsPerTier;
        if (extraRollsPerTier != null && extraRollsPerTier.length > tierId) {
            extraRolls = Math.max(0, extraRollsPerTier[tierId]);
        }

        // Always roll at least once to replace the culled vanilla drops.
        // extraRolls = 0 means vanilla-equivalent (1 base roll).
        // extraRolls = 1 means 2x (1 base + 1 extra), etc.
        int totalRolls = 1 + extraRolls;

        for (int i = 0; i < totalRolls; i++) {
            List<ItemStack> rolled = itemModule.getRandomItemDrops(dropListId);
            if (!rolled.isEmpty()) {
                drops.addAll(rolled);
            }
        }
    }

    private void addExtraDropsFromList(List<RPGMobsConfig.ExtraDropRule> rules, int tierId,
                                        List<ItemStack> drops) {
        if (rules == null || rules.isEmpty()) return;

        for (RPGMobsConfig.ExtraDropRule rule : rules) {
            if (rule == null) continue;
            if (rule.itemId == null || rule.itemId.isBlank()) continue;
            if (!rule.enabledPerTier[clampTierIndex(tierId)]) continue;
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
