package com.frotty27.elitemobs.api.event;

/**
 * Enumeration of elite mob ability types used by the legacy ability event.
 *
 * @deprecated Use {@link com.frotty27.elitemobs.api.query.AbilityType} and string-based ability IDs
 * from {@link com.frotty27.elitemobs.api.events.EliteMobAbilityStartedEvent#getAbilityId()} instead.
 * @since 1.1.0
 */
@Deprecated
public enum AbilityType {

    /** A charge leap attack where the mob leaps toward a target and deals area damage on impact. */
    CHARGE_LEAP,

    /** A healing leap where the mob jumps to safety and heals itself. */
    HEAL_LEAP,

    /** A summoning ability that spawns undead minions to fight alongside the elite mob. */
    UNDEAD_SUMMON
}
