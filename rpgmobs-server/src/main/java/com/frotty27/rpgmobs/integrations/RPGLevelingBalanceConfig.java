package com.frotty27.rpgmobs.integrations;

import com.frotty27.rpgmobs.config.schema.Cfg;
import com.frotty27.rpgmobs.config.schema.Default;
import com.frotty27.rpgmobs.config.schema.FixedArraySize;
import com.frotty27.rpgmobs.config.schema.Min;

/**
 * Balance configuration for RPGLeveling integration.
 * Only loaded when RPGLeveling is detected. Stored in {@code rpgleveling.yml}.
 */
public final class RPGLevelingBalanceConfig {

    // ── XP ──────────────────────────────────────────────────────────────────────

    @Default
    @FixedArraySize(5)
    @Cfg(group = "XP", file = "rpgleveling.yml",
         comment = "XP multiplier per tier. Applied to the base XP determined by RPGLeveling.\n"
                   + "Example: Tier 3 with 2.0 means the player receives 2x the base XP.")
    public float[] xpMultiplierPerTier = {1.0f, 1.5f, 2.0f, 3.0f, 5.0f};

    @Default
    @Min(0.0)
    @Cfg(group = "XP", file = "rpgleveling.yml",
         comment = "Bonus XP added for each active ability on the killed elite.\n"
                   + "Abilities: Charge Leap, Heal Potion, Undead Summon.")
    public double xpBonusPerAbility = 1000.0;

    @Default
    @Min(0.0)
    @Cfg(group = "XP", file = "rpgleveling.yml",
         comment = "XP multiplier for summoned minion kills.\n"
                   + "0.05 = 5% of the base XP. Set to 0.0 to grant no XP for minions.")
    public double minionXPMultiplier = 0.05;
}
