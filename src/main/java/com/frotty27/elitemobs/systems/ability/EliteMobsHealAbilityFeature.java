package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.rules.AbilityGateEvaluator;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_HEAL_POTION_KEY;
import static com.frotty27.elitemobs.utils.Constants.TICKS_PER_SECOND;

public final class EliteMobsHealAbilityFeature implements EliteMobsAbilityFeature {

    private static final InteractionType HEAL_INTERACTION_TYPE = InteractionType.Ability2;

    @Override
    public String id() {
        return "heal_potion";
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new EliteMobsHealAbilitySystem(plugin));
    }

    @Override
    public void onDamage(EliteMobsPlugin plugin, EliteMobsConfig config, Ref<EntityStore> victimRef,
                         Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                         EliteMobsTierComponent tierComponent, @Nullable NPCEntity npcEntity, int tierIndex,
                         long currentTick, Damage damage) {
        if (npcEntity == null) return;

        boolean healChainActive = AbilityHelpers.isInteractionTypeRunning(entityStore,
                                                                          victimRef,
                                                                          HEAL_INTERACTION_TYPE
        );
        boolean changed = false;

        if (tierComponent.healInProgress && !healChainActive) {
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
            changed = true;
        } else if (!tierComponent.healInProgress && healChainActive) {
            tierComponent.healInProgress = true;
            tierComponent.healHitsTaken = 0;
            changed = true;
        }

        if (!tierComponent.healInProgress || !healChainActive) {
            if (changed) {
                commandBuffer.replaceComponent(victimRef, plugin.getEliteMobsComponent(), tierComponent);
            }
            return;
        }

        // Only interrupt if we are actually in the DRINKING phase (after leap)
        EliteMobsConfig.AbilityConfig healConfig = getHealConfig(config);
        float drinkDuration = EliteMobsAbilityInteractionHelper.resolveHealDrinkDurationSeconds(healConfig);
        long drinkStartTick = tierComponent.healSwapRestoreTick - secondsToTicks(drinkDuration);

        if (currentTick < drinkStartTick) {
            return; // Too early to interrupt
        }

        tierComponent.healHitsTaken++;

        if (tierComponent.healHitsTaken >= AbilityConstants.HEAL_INTERRUPT_HIT_COUNT) {
            AbilityHelpers.cancelInteractionType(entityStore, commandBuffer, victimRef, HEAL_INTERACTION_TYPE);
            playHealBreakSound(config, npcEntity, victimRef, entityStore, commandBuffer, tierIndex);
            AbilityHelpers.restorePreviousItemIfNeeded(npcEntity, tierComponent);

            long cooldownTicks = 0L;
            if (healConfig != null) {
                cooldownTicks = secondsToTicks(AssetConfigHelpers.getFloatForTier(healConfig.cooldownSecondsPerTier,
                                                                                  tierIndex,
                                                                                  0f
                ));
            }
            tierComponent.nextHealAllowedTick = currentTick + cooldownTicks;
            tierComponent.healInProgress = false;
            tierComponent.healHitsTaken = 0;
            tierComponent.healSwapRestoreTick = -1;
        }

        commandBuffer.replaceComponent(victimRef, plugin.getEliteMobsComponent(), tierComponent);
    }

    private void playHealBreakSound(EliteMobsConfig config, NPCEntity npcEntity, Ref<EntityStore> victimRef,
                                    Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                    int tierIndex) {
        EliteMobsConfig.AbilityConfig healConfig = getHealConfig(config);
        if (healConfig == null) return;
        if (!AssetConfigHelpers.isTieredAssetConfigEnabledForTier(healConfig, tierIndex)) return;
        if (!AbilityGateEvaluator.isAllowed(healConfig, safeRoleName(npcEntity), "", tierIndex)) return;

        String rootInteractionId = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(config,
                                                                                      healConfig,
                                                                                      EliteMobsConfig.HealAbilityConfig.TEMPLATE_ROOT_INTERACTION_BREAK,
                                                                                      tierIndex
        );
        if (rootInteractionId == null || rootInteractionId.isBlank()) return;

        EliteMobsAbilityInteractionHelper.tryStartInteraction(victimRef,
                                                              entityStore,
                                                              commandBuffer,
                                                              HEAL_INTERACTION_TYPE,
                                                              rootInteractionId
        );
    }

    private static EliteMobsConfig.AbilityConfig getHealConfig(EliteMobsConfig config) {
        return (EliteMobsConfig.AbilityConfig) AssetConfigHelpers.getAssetConfig(config,
                                                                                 AssetType.ABILITIES,
                                                                                 ABILITY_HEAL_POTION_KEY
        );
    }


    private static long secondsToTicks(float seconds) {
        if (seconds <= 0f) return 0L;
        return Math.max(1L, (long) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    private static String safeRoleName(NPCEntity npcEntity) {
        if (npcEntity == null) return "";
        String roleName = npcEntity.getRoleName();
        return roleName == null ? "" : roleName;
    }
}