package com.frotty27.rpgmobs.utils;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Locale;

public final class StringHelpers {

    private StringHelpers() {
    }

    public static String normalizeLower(String value) {
        return (value == null) ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String safeRoleName(NPCEntity npcEntity) {
        if (npcEntity == null) return "";
        String roleName = npcEntity.getRoleName();
        return roleName == null ? "" : roleName;
    }

    public static String toDisplayName(String snakeCaseId) {
        if (snakeCaseId == null || snakeCaseId.isBlank()) return "";
        String[] parts = snakeCaseId.split("_");
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String p = parts[i];
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
