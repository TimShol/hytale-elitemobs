package com.frotty27.elitemobs.utils;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Objects;

public class Constants {
    public static final int TIERS_AMOUNT = 5;
    public static final int TIER_MIN = 0;
    public static final int TIER_MAX = 4;
    public static final int MIN_ARMOR_SLOTS = 0;
    public static final int MAX_ARMOR_SLOTS = 4;
    public static final int TICKS_PER_SECOND = 30;
    public static final int UTILITY_SLOT_INDEX = 0;
    public static final String DEFAULT_RARITY = "common";

    public static final ComponentType<EntityStore, NPCEntity> NPC_COMPONENT_TYPE = Objects.requireNonNull(NPCEntity.getComponentType());
}
