package com.frotty27.rpgmobs.config;

public enum CombatStyle {
    AUTO("auto"),
    DISCIPLINED("disciplined"),
    BERSERKER("berserker"),
    TACTICAL("tactical"),
    CHAOTIC("chaotic");

    private final String id;

    CombatStyle(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String factionName() {
        return switch (this) {
            case DISCIPLINED -> "Skeleton";
            case BERSERKER -> "Trork";
            case TACTICAL, AUTO -> "Outlander";
            case CHAOTIC -> "Goblin";
        };
    }

    public String displayName() {
        return switch (this) {
            case AUTO -> "Auto";
            case DISCIPLINED -> "Disciplined";
            case BERSERKER -> "Berserker";
            case TACTICAL -> "Tactical";
            case CHAOTIC -> "Chaotic";
        };
    }

    public static CombatStyle parse(String value) {
        if (value == null || value.isEmpty()) return AUTO;
        for (CombatStyle style : values()) {
            if (style.id.equalsIgnoreCase(value)) return style;
        }
        return AUTO;
    }

    public static CombatStyle next(CombatStyle current) {
        CombatStyle[] all = values();
        return all[(current.ordinal() + 1) % all.length];
    }
}
