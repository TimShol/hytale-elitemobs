package com.frotty27.rpgmobs.utils;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Map;
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

    public static final ComponentType<EntityStore, NPCEntity> NPC_COMPONENT_TYPE = initNpcComponentType();

    private static ComponentType<EntityStore, NPCEntity> initNpcComponentType() {
        try {
            return Objects.requireNonNull(NPCEntity.getComponentType());
        } catch (Throwable e) {
            return null;
        }
    }

    public static final String VARIANT_SWORDS = "swords";
    public static final String VARIANT_LONGSWORDS = "longswords";
    public static final String VARIANT_DAGGERS = "daggers";
    public static final String VARIANT_BATTLEAXES = "battleaxes";
    public static final String VARIANT_AXES = "axes";
    public static final String VARIANT_MACES = "maces";
    public static final String VARIANT_CLUBS = "clubs";
    public static final String VARIANT_CLUBS_FLAIL = "clubsFlail";
    public static final String VARIANT_SPEARS = "spears";

    public static final String[] ALL_VARIANT_KEYS = {
            VARIANT_SWORDS, VARIANT_LONGSWORDS, VARIANT_DAGGERS, VARIANT_BATTLEAXES,
            VARIANT_AXES, VARIANT_MACES, VARIANT_CLUBS, VARIANT_SPEARS
    };

    public static final String[] ALL_VARIANT_LABELS = {
            "Swords", "Longswords", "Daggers", "Battleaxes", "Axes", "Maces", "Clubs", "Spears"
    };

    public static final Map<String, String> CATEGORY_TO_VARIANT = Map.of(
            "Daggers", VARIANT_DAGGERS,
            "Battleaxes", VARIANT_BATTLEAXES,
            "Axes", VARIANT_AXES,
            "Maces", VARIANT_MACES,
            "Clubs", VARIANT_CLUBS,
            "Spears", VARIANT_SPEARS,
            "Longswords", VARIANT_LONGSWORDS,
            "Swords", VARIANT_SWORDS
    );
}
