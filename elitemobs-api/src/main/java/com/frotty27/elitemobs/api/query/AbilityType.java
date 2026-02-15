package com.frotty27.elitemobs.api.query;

/**
 * Enumeration of ability types available to elite mobs.
 *
 * <p>Each constant represents a distinct ability that an elite mob can perform.
 * The string-based {@linkplain #getId() ID} of each type matches the ability
 * identifier used in events such as
 * {@link com.frotty27.elitemobs.api.events.EliteMobAbilityStartedEvent}.</p>
 *
 * @since 1.1.0
 */
public enum AbilityType {

    /** A healing leap where the mob jumps to safety and heals itself. */
    HEAL_LEAP,

    /** A summoning ability that spawns undead minions to fight alongside the elite mob. */
    SUMMON_UNDEAD,

    /** A charge leap attack where the mob leaps toward a target and deals area damage on impact. */
    CHARGE_LEAP;

    /**
     * Returns the string identifier for this ability type.
     *
     * <p>The ID corresponds to the {@link #name()} of the enum constant and matches
     * the ability IDs used in ability lifecycle events.</p>
     *
     * @return the ability type identifier, never {@code null}
     */
    public String getId() {
        return this.name();
    }
}
