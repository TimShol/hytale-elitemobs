package com.frotty27.elitemobs.utils;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Locale;

public final class StringHelpers {

    private StringHelpers() {
    }

    public static String toLowerOrEmpty(String value) {
        return (value == null) ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static String normalizeLower(String value) {
        return (value == null) ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String safeRoleName(NPCEntity npcEntity) {
        if (npcEntity == null) return "";
        String roleName = npcEntity.getRoleName();
        return roleName == null ? "" : roleName;
    }
}
