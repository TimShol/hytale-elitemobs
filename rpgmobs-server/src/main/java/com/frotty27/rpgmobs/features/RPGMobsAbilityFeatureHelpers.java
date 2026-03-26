package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.AbilityEnabledComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig.AbilityConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig.SummonAbilityConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.frotty27.rpgmobs.utils.Constants;
import com.hypixel.hytale.builtin.npccombatactionevaluator.NPCCombatActionEvaluatorPlugin;
import com.hypixel.hytale.builtin.npccombatactionevaluator.evaluator.CombatActionEvaluator;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.CombatSupport;
import org.jspecify.annotations.Nullable;

public final class RPGMobsAbilityFeatureHelpers {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEFAULT_IDENTIFIER = "default";

    private RPGMobsAbilityFeatureHelpers() {
    }

    public static ResolvedConfig resolveConfig(RPGMobsPlugin plugin,
                                               Ref<EntityStore> npcRef,
                                               Store<EntityStore> entityStore) {
        NPCEntity npc = entityStore.getComponent(npcRef, Constants.NPC_COMPONENT_TYPE);
        String worldName = (npc != null && npc.getWorld() != null) ? npc.getWorld().getName() : null;
        return plugin.getResolvedConfig(worldName);
    }

    public static <C extends AbilityEnabledComponent> @Nullable C getReadyAbilityComponent(
            TriggerContext context,
            ComponentType<EntityStore, C> componentType) {
        C comp = context.store().getComponent(context.entityRef(), componentType);
        if (comp == null || !comp.isAbilityEnabled() || comp.getCooldownTicksRemaining() > 0) return null;
        return comp;
    }

    public static String resolveWeaponId(Ref<EntityStore> npcRef, Store<EntityStore> entityStore) {
        NPCEntity npc = entityStore.getComponent(npcRef, Constants.NPC_COMPONENT_TYPE);
        if (npc == null) return "";
        var inventory = npc.getInventory();
        if (inventory == null) return "";
        var itemInHand = inventory.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty()) return "";
        String itemId = itemInHand.getItemId();
        return itemId != null ? itemId : "";
    }

    public static String resolveSummonRoleIdentifier(AbilityConfig summonConfig, String roleName) {
        if (!(summonConfig instanceof SummonAbilityConfig s)) return DEFAULT_IDENTIFIER;
        if (roleName == null || roleName.isBlank()) return DEFAULT_IDENTIFIER;
        if (s.roleIdentifiers == null || s.roleIdentifiers.isEmpty()) return DEFAULT_IDENTIFIER;

        String roleLower = roleName.toLowerCase();
        for (String identifier : s.roleIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            if (roleLower.contains(identifier.toLowerCase())) {
                return identifier;
            }
        }
        return DEFAULT_IDENTIFIER;
    }

    public static boolean tryStartInteraction(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                              CommandBuffer<EntityStore> commandBuffer, InteractionType interactionType,
                                              String rootInteractionId) {
        RootInteraction rootInteraction = AbilityHelpers.getRootInteraction(rootInteractionId);
        if (rootInteraction == null) {
            LOGGER.atWarning().log("rootInteraction not found for id='%s'", rootInteractionId);
            return false;
        }

        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType = InteractionModule.get().getInteractionManagerComponent();

        InteractionManager interactionManager = entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) {
            LOGGER.atWarning().log("InteractionManager is null for rootId='%s'",
                                   rootInteractionId
            );
            return false;
        }

        var chains = interactionManager.getChains();
        if (!chains.isEmpty()) {
            for (var entry : chains.entrySet()) {
                InteractionChain chain = entry.getValue();
                if (chain != null) {
                    RPGMobsLogger.debug(LOGGER,
                                        "Active chain: type=%s for rootId='%s'",
                                        RPGMobsLogLevel.INFO,
                                        chain.getType() != null ? chain.getType().name() : "null",
                                        rootInteractionId
                    );
                }
            }
        } else {
            RPGMobsLogger.debug(LOGGER,
                                "No active chains before starting '%s'",
                                RPGMobsLogLevel.INFO,
                                rootInteractionId
            );
        }

        terminateCurrentCombatActionEvaluatorAction(npcRef, entityStore, commandBuffer, rootInteractionId);

        if (!chains.isEmpty()) {
            for (InteractionChain chain : chains.values()) {
                if (chain != null) {
                    RPGMobsLogger.debug(LOGGER,
                                        "Pre-cancelling %s chain before starting '%s'",
                                        RPGMobsLogLevel.INFO,
                                        chain.getType() != null ? chain.getType().name() : "null",
                                        rootInteractionId
                    );
                    interactionManager.cancelChains(chain);
                }
            }
        }

        InteractionContext interactionContext = InteractionContext.forInteraction(interactionManager,
                                                                                  npcRef,
                                                                                  interactionType,
                                                                                  entityStore
        );

        boolean started = interactionManager.tryStartChain(npcRef,
                                                           commandBuffer,
                                                           interactionType,
                                                           interactionContext,
                                                           rootInteraction
        );

        if (!started) {
            LOGGER.atWarning().log("tryStartChain returned false for rootId='%s' type=%s",
                                   rootInteractionId,
                                   interactionType.name()
            );
        } else {
            blockNormalAttacksDuringChain(npcRef, entityStore, interactionManager, interactionType);
        }

        commandBuffer.replaceComponent(npcRef, interactionManagerComponentType, interactionManager);

        return started;
    }

    public static boolean tryStartGuardInteraction(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                                      CommandBuffer<EntityStore> commandBuffer,
                                                      String guardRootId) {
        // Use Ability2 slot (proven to work for NPCs) instead of Secondary
        // The parry is short (0.83s) and blocks abilities during that time
        return tryStartInteraction(npcRef, entityStore, commandBuffer, InteractionType.Ability2, guardRootId);
    }

    private static void blockNormalAttacksDuringChain(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                                       InteractionManager interactionManager,
                                                       InteractionType interactionType) {
        InteractionChain abilityChain = null;
        var chainsAfterStart = interactionManager.getChains();
        for (InteractionChain chain : chainsAfterStart.values()) {
            if (chain != null && chain.getType() == interactionType) {
                abilityChain = chain;
                break;
            }
        }
        if (abilityChain == null) return;

        NPCEntity npc = entityStore.getComponent(npcRef, Constants.NPC_COMPONENT_TYPE);
        if (npc == null) return;

        Role role = npc.getRole();
        if (role == null) return;

        CombatSupport combatSupport = role.getCombatSupport();
        if (combatSupport == null) return;

        combatSupport.setExecutingAttack(abilityChain, false, 0.0);
    }

    private static void terminateCurrentCombatActionEvaluatorAction(Ref<EntityStore> npcRef,
                                                                     Store<EntityStore> entityStore,
                                                                     CommandBuffer<EntityStore> commandBuffer,
                                                                     String rootInteractionId) {
        var caeComponentType = NPCCombatActionEvaluatorPlugin.get().getCombatActionEvaluatorComponentType();
        CombatActionEvaluator combatActionEvaluator = entityStore.getComponent(npcRef, caeComponentType);
        if (combatActionEvaluator == null) {
            return;
        }

        Object currentAction = combatActionEvaluator.getCurrentAction();
        if (currentAction != null) {
            combatActionEvaluator.terminateCurrentAction();
            commandBuffer.replaceComponent(npcRef, caeComponentType, combatActionEvaluator);
            RPGMobsLogger.debug(LOGGER,
                                "Terminated CAE action before starting ability '%s'",
                                RPGMobsLogLevel.INFO,
                                rootInteractionId);
        }
    }
}
