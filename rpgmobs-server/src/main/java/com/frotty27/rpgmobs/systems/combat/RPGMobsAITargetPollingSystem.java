package com.frotty27.rpgmobs.systems.combat;

import com.frotty27.rpgmobs.api.events.RPGMobsAggroEvent;
import com.frotty27.rpgmobs.api.events.RPGMobsDeaggroEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsAITargetPollingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long AI_POLL_INTERVAL_TICKS = 5;

    private final RPGMobsPlugin plugin;

    public RPGMobsAITargetPollingSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(plugin.getRPGMobsComponentType(), plugin.getCombatTrackingComponentType());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> mobRef = chunk.getReferenceTo(entityIndex);

        RPGMobsCombatTrackingComponent combat = chunk.getComponent(entityIndex,
                                                                   plugin.getCombatTrackingComponentType()
        );
        if (combat == null) return;

        long currentTick = plugin.getTickClock().getTick();

        if (currentTick - combat.lastTargetUpdateTick >= AI_POLL_INTERVAL_TICKS) {
            combat.lastTargetUpdateTick = currentTick;

            NPCEntity npc = store.getComponent(mobRef, NPC_COMPONENT_TYPE);
            if (npc != null) {
                Ref<EntityStore> aiTarget = getAITarget(npc);

                // Filter out own-minion targets — prevents summoner from aggro-ing its minions
                if (aiTarget != null && isOwnMinion(store, mobRef, aiTarget)) {
                    aiTarget = null;
                }

                if (aiTarget != null) {
                    boolean wasIdle = combat.state == RPGMobsCombatTrackingComponent.CombatState.IDLE;
                    combat.updateAITarget(aiTarget);

                    if (wasIdle) {
                        RPGMobsTierComponent tier = store.getComponent(mobRef, plugin.getRPGMobsComponentType());
                        if (tier != null) {
                            combat.transitionToInCombat(aiTarget, currentTick);
                            commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);

                            String roleName = npc.getRoleName();
                            plugin.getEventBus().fire(new RPGMobsAggroEvent(mobRef,
                                                                            aiTarget,
                                                                            tier.tierIndex,
                                                                            roleName != null ? roleName : ""
                            ));

                            if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                                RPGMobsLogger.debug(LOGGER,
                                                    "Combat state: %s IDLE → IN_COMBAT (AI marker acquired) tier=%d",
                                                    RPGMobsLogLevel.INFO,
                                                    npc.getRoleName(),
                                                    tier.tierIndex
                                );
                            }
                        }
                    } else {
                        commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);
                    }
                } else if (combat.state == RPGMobsCombatTrackingComponent.CombatState.IN_COMBAT) {
                    combat.transitionToIdle(currentTick);
                    commandBuffer.replaceComponent(mobRef, plugin.getCombatTrackingComponentType(), combat);

                    RPGMobsTierComponent deaggroTier = store.getComponent(mobRef, plugin.getRPGMobsComponentType());
                    int deaggroTierIndex = (deaggroTier != null) ? deaggroTier.tierIndex : 0;
                    String deaggroRole = npc.getRoleName();
                    plugin.getEventBus().fire(new RPGMobsDeaggroEvent(mobRef,
                                                                      deaggroTierIndex,
                                                                      deaggroRole != null ? deaggroRole : ""
                    ));

                    if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                        RPGMobsLogger.debug(LOGGER,
                                            "Combat state: %s IN_COMBAT → IDLE (AI marker lost)",
                                            RPGMobsLogLevel.INFO,
                                            npc.getRoleName()
                        );
                    }
                }
            }
        }

    }

    private @Nullable Ref<EntityStore> getAITarget(NPCEntity npc) {
        Role role = npc.getRole();
        if (role == null) return null;

        MarkedEntitySupport markedEntitySupport = role.getMarkedEntitySupport();
        if (markedEntitySupport == null) return null;

        String[] primaryKeys = {"LockedTarget", "Target", "CombatTarget"};
        for (String key : primaryKeys) {
            Ref<EntityStore> target = markedEntitySupport.getMarkedEntityRef(key);
            if (target != null && target.isValid()) {
                return target;
            }
        }

        return null;
    }

    /**
     * Returns true if the target is a summoned minion belonging to the mob at mobRef.
     * This prevents summoners from targeting (and aggro-ing on) their own minions.
     */
    private boolean isOwnMinion(Store<EntityStore> store, Ref<EntityStore> mobRef, Ref<EntityStore> targetRef) {
        RPGMobsSummonedMinionComponent targetMinion = store.getComponent(targetRef,
                                                                         plugin.getSummonedMinionComponentType()
        );
        if (targetMinion == null || targetMinion.summonerId == null) return false;

        UUIDComponent mobUuid = store.getComponent(mobRef, UUIDComponent.getComponentType());
        return mobUuid != null && targetMinion.summonerId.equals(mobUuid.getUuid());
    }
}
