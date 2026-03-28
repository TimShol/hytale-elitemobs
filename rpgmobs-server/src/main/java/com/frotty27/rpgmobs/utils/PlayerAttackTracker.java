package com.frotty27.rpgmobs.utils;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.DodgeRollAbilityComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.RPGMobsAbilityTriggerListener;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAttackTracker {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double MELEE_RANGE = 8.0;
    private static final double RANGED_RANGE = 20.0;
    private static final double MELEE_CONE_THRESHOLD = 0.6;
    private static final double RANGED_CONE_THRESHOLD = 0.8;
    private static final double MIN_DISTANCE = 1.5;

    private static final String PARRY_ROOT_WEAPON = "Root_RPGMobs_Guard_Weapon_Parry";
    private static final String PARRY_ROOT_SHIELD = "Root_RPGMobs_Guard_Shield_Parry";

    // Per-tier guard chance (T1 = no guard, T5 = 70% chance to react)
    private static final double[] GUARD_CHANCE_PER_TIER = {0.0, 0.15, 0.30, 0.50, 0.70};

    // Cooldown after guard break (30 seconds = 900 ticks)
    private static final int GUARD_BREAK_COOLDOWN_TICKS = 900;

    // Track which entities are currently guarding or on guard cooldown
    private final ConcurrentHashMap<Long, GuardState> guardStates = new ConcurrentHashMap<>();

    // Pending guard requests (queued by detection thread, consumed by world thread)
    private final ConcurrentHashMap<Long, Ref<EntityStore>> pendingGuardRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, DelayedGuardRequest> delayedGuardRequests = new ConcurrentHashMap<>();

    private static class DelayedGuardRequest {
        final Ref<EntityStore> entityRef;
        int ticksRemaining;

        DelayedGuardRequest(Ref<EntityStore> entityRef, int delayTicks) {
            this.entityRef = entityRef;
            this.ticksRemaining = delayTicks;
        }
    }

    private long lastDelayedGuardTick = -1;

    public void tickDelayedGuardRequests(long currentTick) {
        // Only tick once per game tick (called from per-entity loop but must run globally once)
        if (currentTick == lastDelayedGuardTick) return;
        lastDelayedGuardTick = currentTick;

        var iterator = delayedGuardRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var request = entry.getValue();
            request.ticksRemaining--;
            if (request.ticksRemaining <= 0) {
                pendingGuardRequests.put(entry.getKey(), request.entityRef);
                iterator.remove();
            }
        }
    }

    private final Set<Ref<EntityStore>> registeredEntities = ConcurrentHashMap.newKeySet();

    private @Nullable RPGMobsPlugin plugin;
    private @Nullable RPGMobsAbilityTriggerListener triggerListener;

    public void initialize(RPGMobsPlugin plugin, RPGMobsAbilityTriggerListener triggerListener) {
        this.plugin = plugin;
        this.triggerListener = triggerListener;
    }

    private final Set<Ref<EntityStore>> registeredGuardEntities = ConcurrentHashMap.newKeySet();

    public void registerDodgeEntity(Ref<EntityStore> entityRef) {
        registeredEntities.add(entityRef);
    }

    public void unregisterDodgeEntity(Ref<EntityStore> entityRef) {
        registeredEntities.remove(entityRef);
    }

    public void registerGuardEntity(Ref<EntityStore> entityRef) {
        registeredGuardEntities.add(entityRef);
    }

    public void unregisterGuardEntity(Ref<EntityStore> entityRef) {
        registeredGuardEntities.remove(entityRef);
    }

    public void ensureRegistered(Ref<EntityStore> entityRef, Store<EntityStore> entityStore, RPGMobsPlugin plugin) {
        DodgeRollAbilityComponent dodge = entityStore.getComponent(entityRef, plugin.getDodgeRollAbilityComponentType());
        if (dodge != null && dodge.abilityEnabled) {
            registeredEntities.add(entityRef);
        }
        RPGMobsAbilityLockComponent lock = entityStore.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null) {
            registeredGuardEntities.add(entityRef);
        }
    }

    public void pruneInvalidRefs() {
        registeredEntities.removeIf(ref -> !ref.isValid());
        registeredGuardEntities.removeIf(ref -> !ref.isValid());
    }

    // Per-weapon parry delay in ticks (based on player weapon wind-up time)
    // Reduced from raw wind-up to activate parry EARLIER so the block is clearly visible
    // before the hit lands. The NPC raises guard, THEN the hit connects into the guard.
    private static final Map<String, Integer> WEAPON_PARRY_DELAY_TICKS = Map.ofEntries(
            Map.entry("Daggers", 1),     // 0.069s wind-up - near instant
            Map.entry("Sword", 2),       // 0.117s wind-up
            Map.entry("Spear", 3),       // 0.223s wind-up
            Map.entry("Longsword", 3),   // 0.229s wind-up
            Map.entry("Axe", 4),         // 0.244s wind-up
            Map.entry("Club", 4),        // 0.267s wind-up
            Map.entry("Battleaxe", 6),   // 0.400s wind-up
            Map.entry("Mace", 8)         // 0.600s wind-up
    );
    private static final int DEFAULT_PARRY_DELAY_TICKS = 3;

    private static int getParryDelayForWeapon(@Nullable String weaponId) {
        if (weaponId == null) return DEFAULT_PARRY_DELAY_TICKS;
        String lowerWeapon = weaponId.toLowerCase();
        // Match weapon category from item ID (e.g., "Weapon_Mace_Iron" -> "Mace")
        if (lowerWeapon.contains("daggers") || lowerWeapon.contains("dagger")) return WEAPON_PARRY_DELAY_TICKS.get("Daggers");
        if (lowerWeapon.contains("longsword")) return WEAPON_PARRY_DELAY_TICKS.get("Longsword");
        if (lowerWeapon.contains("battleaxe")) return WEAPON_PARRY_DELAY_TICKS.get("Battleaxe");
        if (lowerWeapon.contains("sword")) return WEAPON_PARRY_DELAY_TICKS.get("Sword");
        if (lowerWeapon.contains("mace")) return WEAPON_PARRY_DELAY_TICKS.get("Mace");
        if (lowerWeapon.contains("spear")) return WEAPON_PARRY_DELAY_TICKS.get("Spear");
        if (lowerWeapon.contains("club") || lowerWeapon.contains("flail")) return WEAPON_PARRY_DELAY_TICKS.get("Club");
        if (lowerWeapon.contains("axe")) return WEAPON_PARRY_DELAY_TICKS.get("Axe");
        return DEFAULT_PARRY_DELAY_TICKS;
    }

    public void processPlayerAttack(Store<EntityStore> entityStore,
                                    double playerPosX, double playerPosY, double playerPosZ,
                                    double playerDirX, double playerDirY, double playerDirZ,
                                    boolean isMelee, boolean isCharged, long playerRefHash,
                                    @Nullable String playerWeaponId) {
        if (plugin == null || triggerListener == null) return;

        int parryDelay = getParryDelayForWeapon(playerWeaponId);

        // Evaluate guard for ALL registered guard entities (separate from dodge)
        evaluateGuardForNearbyEntities(entityStore, playerPosX, playerPosY, playerPosZ,
                playerDirX, playerDirY, playerDirZ, isMelee, isCharged, playerRefHash, parryDelay);

        if (registeredEntities.isEmpty()) return;

        double maxRange = isMelee ? MELEE_RANGE : RANGED_RANGE;
        double maxRangeSq = maxRange * maxRange;
        double minDistSq = MIN_DISTANCE * MIN_DISTANCE;
        double threshold = isMelee ? MELEE_CONE_THRESHOLD : RANGED_CONE_THRESHOLD;

        for (Ref<EntityStore> entityRef : registeredEntities) {
            if (!entityRef.isValid()) continue;
            if (entityRef.hashCode() == playerRefHash) continue;

            DodgeRollAbilityComponent dodge = entityStore.getComponent(entityRef, plugin.getDodgeRollAbilityComponentType());
            if (dodge == null || !dodge.abilityEnabled || dodge.cooldownTicksRemaining > 0) continue;

            RPGMobsTierComponent tier = entityStore.getComponent(entityRef, plugin.getRPGMobsComponentType());
            if (tier == null || tier.tierIndex < 0) continue;

            RPGMobsCombatTrackingComponent combat = entityStore.getComponent(entityRef, plugin.getCombatTrackingComponentType());
            if (combat == null || !combat.isInCombat()) continue;

            RPGMobsAbilityLockComponent lock = entityStore.getComponent(entityRef, plugin.getAbilityLockComponentType());
            if (lock != null && lock.isLocked()) {
                if (!AbilityIds.MULTI_SLASH_SHORT.equals(lock.activeAbilityId)) continue;
            }

            TransformComponent npcTransform = entityStore.getComponent(entityRef, TransformComponent.getComponentType());
            if (npcTransform == null) continue;

            Vector3d npcPos = npcTransform.getPosition();
            double dx = npcPos.getX() - playerPosX;
            double dy = npcPos.getY() - playerPosY;
            double dz = npcPos.getZ() - playerPosZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > maxRangeSq || distSq < minDistSq) continue;

            double dist = Math.sqrt(distSq);
            double normX = dx / dist;
            double normY = dy / dist;
            double normZ = dz / dist;

            double dot = playerDirX * normX + playerDirY * normY + playerDirZ * normZ;
            if (dot < threshold) continue;

            AbilityTriggerSource source = isCharged
                    ? AbilityTriggerSource.PLAYER_CHARGED_ATTACK_NEARBY
                    : AbilityTriggerSource.PLAYER_ATTACK_NEARBY;
            triggerListener.evaluateAbilitiesForEntity(entityRef, source);
        }
    }

    private void evaluateGuardForNearbyEntities(Store<EntityStore> entityStore,
                                                double playerPosX, double playerPosY, double playerPosZ,
                                                double playerDirX, double playerDirY, double playerDirZ,
                                                boolean isMelee, boolean isCharged, long playerRefHash,
                                                int parryDelayTicks) {
        if (registeredGuardEntities.isEmpty()) return;

        double maxRange = isMelee ? MELEE_RANGE : RANGED_RANGE;
        double maxRangeSq = maxRange * maxRange;
        double minDistSq = MIN_DISTANCE * MIN_DISTANCE;

        for (Ref<EntityStore> entityRef : registeredGuardEntities) {
            if (!entityRef.isValid()) continue;
            if (entityRef.hashCode() == playerRefHash) continue;

            RPGMobsTierComponent tier = entityStore.getComponent(entityRef, plugin.getRPGMobsComponentType());
            // For test variants without tier component, default to T5 guard chance
            int effectiveTierIndex = (tier != null && tier.tierIndex >= 0) ? tier.tierIndex : 4;

            // Only check combat state if component exists (test variants won't have it)
            RPGMobsCombatTrackingComponent combat = entityStore.getComponent(entityRef, plugin.getCombatTrackingComponentType());
            if (combat != null && !combat.isInCombat()) continue;

            RPGMobsAbilityLockComponent lock = entityStore.getComponent(entityRef, plugin.getAbilityLockComponentType());

            TransformComponent npcTransform = entityStore.getComponent(entityRef, TransformComponent.getComponentType());
            if (npcTransform == null) continue;

            Vector3d npcPos = npcTransform.getPosition();
            double dx = npcPos.getX() - playerPosX;
            double dy = npcPos.getY() - playerPosY;
            double dz = npcPos.getZ() - playerPosZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > maxRangeSq || distSq < minDistSq) continue;

            double dist = Math.sqrt(distSq);
            double normX = dx / dist;
            double normZ = dz / dist;
            double dot = playerDirX * normX + playerDirZ * normZ;

            // NPC must be roughly in front of the player (within attack cone)
            if (dot < (isMelee ? MELEE_CONE_THRESHOLD : RANGED_CONE_THRESHOLD)) continue;

            // Use UUID for stable entity key (Ref.hashCode() changes between ticks)
            UUIDComponent uuidComp = entityStore.getComponent(entityRef, UUIDComponent.getComponentType());
            if (uuidComp == null) continue;
            long stableKey = uuidComp.getUuid().getMostSignificantBits();

            evaluateGuardForEntity(entityRef, stableKey, effectiveTierIndex, lock, isCharged, parryDelayTicks);
        }
    }

    // Debounce: only evaluate guard once per swing (not per chain operation change)
    private static final int GUARD_EVAL_DEBOUNCE_TICKS = 5; // ~0.17s between evaluations
    private final ConcurrentHashMap<Long, Long> lastGuardEvalTick = new ConcurrentHashMap<>();

    private void evaluateGuardForEntity(Ref<EntityStore> entityRef, long stableEntityKey, int tierIndex,
                                        @Nullable RPGMobsAbilityLockComponent lock,
                                        boolean isCharged, int parryDelayTicks) {
        if (plugin == null) return;

        // Don't guard while an ability is running
        if (lock != null && lock.isLocked()) return;

        tierIndex = ClampingHelpers.clampTierIndex(tierIndex);

        // Per-tier guard chance
        double guardChance = GUARD_CHANCE_PER_TIER[tierIndex];
        if (guardChance <= 0.0) return;

        // Charged attacks are harder to guard (halved chance)
        if (isCharged) {
            guardChance *= 0.5;
        }

        // Debounce: skip if we already evaluated guard for this entity recently
        // (one player swing generates 3-5 detection events from chain operation changes)
        long currentTick = plugin.getTickClock().getTick();
        Long lastEval = lastGuardEvalTick.get(stableEntityKey);
        if (lastEval != null && (currentTick - lastEval) < GUARD_EVAL_DEBOUNCE_TICKS) return;
        lastGuardEvalTick.put(stableEntityKey, currentTick);

        // Check guard cooldown (after guard break or parry rest)
        GuardState state = guardStates.get(stableEntityKey);
        if (state != null && state.onCooldown) return;

        // Already guarding - don't restart
        if (state != null && state.isGuarding) return;

        // Already has a delayed guard pending - don't duplicate
        if (delayedGuardRequests.containsKey(stableEntityKey)) return;

        // Already has a pending guard request waiting to be processed
        if (pendingGuardRequests.containsKey(stableEntityKey)) return;

        // Random chance roll
        if (Math.random() >= guardChance) return;

        // Queue guard request immediately - setCurrentInteraction on CAE handles timing
        // The CAE's ChargeFor controls guard duration, not our delay system
        pendingGuardRequests.put(stableEntityKey, entityRef);

        RPGMobsLogger.debug(LOGGER, "Guard scheduled for tier=%d charged=%b chance=%.2f delay=%d ticks",
                RPGMobsLogLevel.INFO, tierIndex + 1, isCharged, guardChance, parryDelayTicks);
    }

    public @Nullable Ref<EntityStore> consumeGuardRequest(long entityKey) {
        return pendingGuardRequests.remove(entityKey);
    }

    public Set<Long> getPendingGuardKeys() {
        return pendingGuardRequests.keySet();
    }

    public String resolveGuardRoot(NPCEntity npcEntity) {
        // Use vanilla Root_NPC_Shield_Block for ALL guard types.
        // The vanilla interaction chain handles: stamina check, Stamina_Broken effect check,
        // 0.05s animation delay, then Shield_Block_Damage Wielding (from shield InteractionVars).
        // For weapon-only guard (no shield), use our custom Root_RPGMobs_Guard_Weapon.
        Inventory inventory = npcEntity.getInventory();
        if (inventory != null) {
            var utilityContainer = inventory.getUtility();
            if (utilityContainer != null) {
                ItemStack offhand = utilityContainer.getItemStack((short) 0);
                if (offhand != null && !offhand.isEmpty()) {
                    String offhandId = offhand.getItemId();
                    if (offhandId != null && offhandId.toLowerCase().contains("shield")) {
                        return PARRY_ROOT_SHIELD;
                    }
                }
            }
        }
        return PARRY_ROOT_WEAPON;
    }

    public boolean isGuarding(long entityKey) {
        GuardState state = guardStates.get(entityKey);
        return state != null && state.isGuarding;
    }

    public boolean isOnGuardCooldown(long entityKey) {
        GuardState state = guardStates.get(entityKey);
        return state != null && state.onCooldown;
    }

    private static final int PARRY_REST_COOLDOWN_TICKS = 5; // ~0.17s between parries (minimal gap)

    public void markGuardEnded(long entityKey) {
        // Very short gap between parries to prevent same-tick restart
        var state = new GuardState(false, true);
        state.cooldownTicksRemaining = PARRY_REST_COOLDOWN_TICKS;
        guardStates.put(entityKey, state);
    }

    public void tickGuardCooldowns() {
        guardStates.entrySet().removeIf(entry -> {
            GuardState state = entry.getValue();
            if (state.onCooldown) {
                state.cooldownTicksRemaining--;
                return state.cooldownTicksRemaining <= 0;
            }
            return false;
        });
    }

    private static final int PARRY_DURATION_TICKS = 25; // ~0.83 seconds - long enough to visually see the guard

    private static class GuardState {
        boolean isGuarding;
        boolean onCooldown;
        boolean isParry;
        int cooldownTicksRemaining;
        int parryTicksRemaining;

        GuardState(boolean isGuarding, boolean onCooldown) {
            this.isGuarding = isGuarding;
            this.onCooldown = onCooldown;
            if (onCooldown) {
                this.cooldownTicksRemaining = GUARD_BREAK_COOLDOWN_TICKS;
            }
        }

        static GuardState parry() {
            var state = new GuardState(true, false);
            state.isParry = true;
            state.parryTicksRemaining = PARRY_DURATION_TICKS;
            return state;
        }
    }

    public void markParryStarted(long entityKey) {
        guardStates.put(entityKey, GuardState.parry());
        RPGMobsLogger.debug(LOGGER, "markParryStarted key=%d guardStates.size=%d",
                RPGMobsLogLevel.INFO, entityKey, guardStates.size());
    }

    public boolean shouldCancelParry(long entityKey) {
        GuardState state = guardStates.get(entityKey);
        if (state == null) return false;
        if (!state.isParry) return false;
        state.parryTicksRemaining--;
        if (state.parryTicksRemaining % 5 == 0 || state.parryTicksRemaining <= 2) {
            RPGMobsLogger.debug(LOGGER, "Parry countdown key=%d remaining=%d",
                    RPGMobsLogLevel.INFO, entityKey, state.parryTicksRemaining);
        }
        if (state.parryTicksRemaining <= 0) {
            RPGMobsLogger.debug(LOGGER, "Parry countdown EXPIRED for key=%d, cancelling",
                    RPGMobsLogLevel.INFO, entityKey);
            return true;
        }
        return false;
    }
}
