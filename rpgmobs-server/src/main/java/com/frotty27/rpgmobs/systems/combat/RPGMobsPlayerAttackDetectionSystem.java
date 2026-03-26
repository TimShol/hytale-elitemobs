package com.frotty27.rpgmobs.systems.combat;

import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.PlayerAttackTracker;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RPGMobsPlayerAttackDetectionSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE = TransformComponent.getComponentType();
    private static final ComponentType<EntityStore, HeadRotation> HEAD_ROTATION_TYPE = HeadRotation.getComponentType();

    private static final int MAX_TRACKED_PLAYERS = 200;
    private static final int QUICK_ATTACK_THRESHOLD_TICKS = 8;
    private static final long MAX_TRACK_TICKS = 150;

    private final RPGMobsPlugin plugin;
    private final Map<Long, TrackedAttack> trackedAttacks = new ConcurrentHashMap<>();

    public RPGMobsPlayerAttackDetectionSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        PlayerAttackTracker tracker = plugin.getPlayerAttackTracker();
        if (tracker == null) return;

        Ref<EntityStore> playerRef = chunk.getReferenceTo(entityIndex);
        if (playerRef == null || !playerRef.isValid()) return;

        long playerHash = playerRef.hashCode();
        long currentTick = plugin.getTickClock().getTick();

        AttackChainInfo chainInfo = readAttackChain(playerRef, store);

        TrackedAttack tracked = trackedAttacks.get(playerHash);

        // No chain running
        if (chainInfo == null) {
            if (tracked != null) {
                // Chain just ended - fire if we haven't for the last hit
                if (tracked.isCharging && !tracked.hasFired) {
                    firePlayerAttack(tracked.playerRef, store, tracker, tracked.isMelee, true, playerHash,
                            "charged attack released (chain ended, hit #%d)", tracked.fireCount + 1);
                }
                trackedAttacks.remove(playerHash);
            }
            return;
        }

        // Chain is running - start tracking if new
        if (tracked == null) {
            tracked = new TrackedAttack(playerRef, chainInfo.isMelee, currentTick);
            trackedAttacks.put(playerHash, tracked);
        }

        long ticksTracked = currentTick - tracked.startTick;

        // Expire stale tracking
        if (ticksTracked > MAX_TRACK_TICKS || !tracked.playerRef.isValid()) {
            trackedAttacks.remove(playerHash);
            return;
        }

        int currentOpIndex = chainInfo.operationIndex;

        // Detect NEW hit in combo: operationIndex jumped significantly since last fire.
        // Each hit in a combo has ~3 operations (wind-up + selector + recovery).
        // We fire ONCE per hit when the opIndex first advances past the previous fire point.
        boolean isNewHit = (currentOpIndex > tracked.lastFiredOperationIndex + 1)
                && (currentOpIndex != tracked.lastFiredOperationIndex);

        if (isNewHit && !tracked.isCharging) {
            tracked.hasFired = true;
            tracked.lastFiredOperationIndex = currentOpIndex;
            tracked.fireCount++;

            RPGMobsLogger.debug(LOGGER,
                    "HIT #%d detected: opIndex=%d after %d ticks melee=%b",
                    RPGMobsLogLevel.INFO,
                    tracked.fireCount, currentOpIndex, ticksTracked, tracked.isMelee);

            firePlayerAttack(tracked.playerRef, store, tracker, tracked.isMelee, false, playerHash,
                    "combo hit #%d (opIndex %d after %d ticks)",
                    tracked.fireCount, currentOpIndex, ticksTracked);
        }

        // Detect charging (held at opIndex 0 for a while)
        if (currentOpIndex == 0 && !tracked.isCharging && ticksTracked >= QUICK_ATTACK_THRESHOLD_TICKS) {
            tracked.isCharging = true;
            RPGMobsLogger.debug(LOGGER,
                    "Classified as CHARGING after %d ticks at opIndex=0",
                    RPGMobsLogLevel.INFO, ticksTracked);
        }

        // Charged attack released (opIndex advances after charging)
        if (tracked.isCharging && currentOpIndex > 0 && currentOpIndex > tracked.lastFiredOperationIndex) {
            tracked.hasFired = true;
            tracked.lastFiredOperationIndex = currentOpIndex;
            tracked.fireCount++;

            RPGMobsLogger.debug(LOGGER,
                    "CHARGED hit #%d released: opIndex=%d after %d ticks",
                    RPGMobsLogLevel.INFO,
                    tracked.fireCount, currentOpIndex, ticksTracked);

            firePlayerAttack(tracked.playerRef, store, tracker, tracked.isMelee, true, playerHash,
                    "charged hit #%d (opIndex %d after %d ticks)",
                    tracked.fireCount, currentOpIndex, ticksTracked);
            tracked.isCharging = false;
        }

        if (trackedAttacks.size() > MAX_TRACKED_PLAYERS) {
            trackedAttacks.clear();
        }
    }

    private AttackChainInfo readAttackChain(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        ComponentType<EntityStore, InteractionManager> interactionManagerType =
                InteractionModule.get().getInteractionManagerComponent();
        InteractionManager interactionManager = store.getComponent(playerRef, interactionManagerType);
        if (interactionManager == null) return null;

        var chains = interactionManager.getChains();
        if (chains == null || chains.isEmpty()) return null;

        for (InteractionChain chain : chains.values()) {
            if (chain == null) continue;
            InteractionType type = chain.getType();
            if (type == InteractionType.Primary) {
                return new AttackChainInfo(chain.getOperationIndex(), chain.getTimeInSeconds(), true);
            }
            if (type == InteractionType.ProjectileSpawn) {
                return new AttackChainInfo(chain.getOperationIndex(), chain.getTimeInSeconds(), false);
            }
        }
        return null;
    }

    private void firePlayerAttack(Ref<EntityStore> playerRef, Store<EntityStore> store,
                                  PlayerAttackTracker tracker, boolean isMelee, boolean isCharged,
                                  long playerHash, String reason, Object... args) {
        TransformComponent transform = store.getComponent(playerRef, TRANSFORM_TYPE);
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        double dirX, dirY, dirZ;

        HeadRotation headRot = store.getComponent(playerRef, HEAD_ROTATION_TYPE);
        if (headRot != null) {
            Vector3d dir = headRot.getDirection();
            dirX = dir.getX();
            dirY = dir.getY();
            dirZ = dir.getZ();
        } else {
            Vector3f rotation = transform.getRotation();
            double pitch = Math.toRadians(rotation.getPitch());
            double yaw = Math.toRadians(rotation.getYaw());
            double cosPitch = Math.cos(pitch);
            dirX = -Math.sin(yaw) * cosPitch;
            dirY = -Math.sin(pitch);
            dirZ = Math.cos(yaw) * cosPitch;
        }

        String formattedReason = args.length > 0 ? String.format(reason, args) : reason;

        // Read player's weapon for parry delay calculation.
        // Get weapon from the interaction chain's root interaction name
        String playerWeaponId = null;
        var playerImComponent = store.getComponent(playerRef,
                InteractionModule.get().getInteractionManagerComponent());
        if (playerImComponent != null) {
            for (var chain : playerImComponent.getChains().values()) {
                if (chain != null && chain.getType() == InteractionType.Primary) {
                    RootInteraction rootInt = chain.getRootInteraction();
                    if (rootInt != null) {
                        playerWeaponId = rootInt.toString();
                    }
                    break;
                }
            }
        }

        RPGMobsLogger.debug(LOGGER,
                "FIRE dodge trigger: %s charged=%b melee=%b weapon=%s pos=(%.1f, %.1f, %.1f)",
                RPGMobsLogLevel.INFO,
                formattedReason, isCharged, isMelee, playerWeaponId,
                pos.getX(), pos.getY(), pos.getZ());

        tracker.processPlayerAttack(store,
                pos.getX(), pos.getY(), pos.getZ(),
                dirX, dirY, dirZ,
                isMelee, isCharged, playerHash, playerWeaponId
        );
    }

    private record AttackChainInfo(int operationIndex, float chainTime, boolean isMelee) {}

    private static final class TrackedAttack {
        final Ref<EntityStore> playerRef;
        final boolean isMelee;
        final long startTick;
        boolean isCharging;
        boolean hasFired;
        int lastFiredOperationIndex;
        int fireCount;

        TrackedAttack(Ref<EntityStore> playerRef, boolean isMelee, long startTick) {
            this.playerRef = playerRef;
            this.isMelee = isMelee;
            this.startTick = startTick;
            this.isCharging = false;
            this.hasFired = false;
            this.lastFiredOperationIndex = -1;
            this.fireCount = 0;
        }
    }
}
