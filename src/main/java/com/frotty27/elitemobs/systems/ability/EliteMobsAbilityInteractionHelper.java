package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Random;

import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_HEAL_POTION_KEY;

final class EliteMobsAbilityInteractionHelper {

    private EliteMobsAbilityInteractionHelper() {
    }

    static boolean tryStartInteraction(
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionType interactionType,
            String rootInteractionId
    ) {
        RootInteraction rootInteraction = AbilityHelpers.getRootInteraction(rootInteractionId);
        if (rootInteraction == null) {
            return false;
        }

        ComponentType<EntityStore, InteractionManager> interactionManagerComponentType =
                InteractionModule.get().getInteractionManagerComponent();

        InteractionManager interactionManager =
                entityStore.getComponent(npcRef, interactionManagerComponentType);
        if (interactionManager == null) return false;

        InteractionContext interactionContext =
                InteractionContext.forInteraction(
                        interactionManager,
                        npcRef,
                        interactionType,
                        entityStore
                );

        boolean started = interactionManager.tryStartChain(
                npcRef,
                commandBuffer,
                interactionType,
                interactionContext,
                rootInteraction
        );

        if (started) {
            commandBuffer.replaceComponent(
                    npcRef,
                    interactionManagerComponentType,
                    interactionManager
            );
        }
        return started;
    }

    static void swapToPotionInHandIfPossible(
            NPCEntity npcEntity,
            EliteMobsConfig config,
            EliteMobsTierComponent tierComponent
    ) {
        if (npcEntity == null || config == null || tierComponent == null) return;
        if (tierComponent.healSwapActive) return;

        EliteMobsConfig.AbilityConfig healConfig = getHealConfig(config);
        if (!(healConfig instanceof EliteMobsConfig.HealAbilityConfig healAbilityConfig)) return;

        String potionItemId = healAbilityConfig.npcDrinkItemId;
        if (potionItemId == null || potionItemId.isBlank()) return;
        if (Item.getAssetMap().getAsset(potionItemId) == null) return;

        Inventory inventory = npcEntity.getInventory();
        if (inventory == null) return;

        byte slot = inventory.getActiveHotbarSlot();
        if (slot == Inventory.INACTIVE_SLOT_INDEX) slot = 0;

        ItemStack current;
        try {
            current = inventory.getHotbar().getItemStack((short) slot);
        } catch (Throwable ignored) {
            return;
        }
        tierComponent.healSwapPreviousItem = current;
        tierComponent.healSwapSlot = slot;
        tierComponent.healSwapActive = true;

        inventory.getHotbar().setItemStackForSlot(slot, new ItemStack(potionItemId, 1));
        inventory.markChanged();
    }

    static float rollHealTriggerPercent(Random random, EliteMobsConfig.AbilityConfig healConfig) {
        float min = 0.1f;
        float max = 0.4f;
        if (healConfig instanceof EliteMobsConfig.HealAbilityConfig healAbilityConfig) {
            min = healAbilityConfig.minHealthTriggerPercent;
            max = healAbilityConfig.maxHealthTriggerPercent;
        }
        return AbilityHelpers.rollPercentInRange(random, min, max, 0.5f);
    }

    static float resolveHealDrinkDurationSeconds(EliteMobsConfig.AbilityConfig healConfig) {
        if (healConfig instanceof EliteMobsConfig.HealAbilityConfig healAbilityConfig) {
            return healAbilityConfig.npcDrinkDurationSeconds;
        }
        return 0f;
    }

    private static EliteMobsConfig.AbilityConfig getHealConfig(EliteMobsConfig config) {
        return (EliteMobsConfig.AbilityConfig) AssetConfigHelpers.getAssetConfig(
                config,
                AssetType.ABILITIES,
                ABILITY_HEAL_POTION_KEY
        );
    }

}
