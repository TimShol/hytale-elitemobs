package com.frotty27.rpgmobs.config;

import com.frotty27.rpgmobs.config.schema.Cfg;
import com.frotty27.rpgmobs.config.schema.Default;
import com.frotty27.rpgmobs.config.schema.Max;
import com.frotty27.rpgmobs.config.schema.Min;

public final class GlobalConfig {

    @Default
    @Cfg(group = "System", file = "core.yml", comment = """
            Config format version. When this value is lower than the version expected by the mod,
            ALL config files (base, world overlays, instance overlays) are wiped and regenerated.
            This is NOT tied to plugin versions  - it only changes when the config layout breaks.
            Do not change this manually.""")
    public int configFormatVersion = 3;

    @Default
    @Cfg(group = "Worlds", file = "core.yml", comment = """
            Master switch. When false, RPGMobs is completely disabled in ALL worlds and instances,
            regardless of per-world overlay settings. No elites will spawn and existing elites will
            be de-elited on next reconcile. When true (default), per-world/instance overlays control
            their own enabled state.""")
    public boolean globalEnabled = true;

    @Default
    @Cfg(group = "Worlds", file = "core.yml", comment = """
            Controls the fallback for worlds/instances without an explicit overlay file.
            When true (default): unconfigured worlds use the base config (RPGMobs active).
            When false: unconfigured worlds have RPGMobs disabled (no elites spawn).
            \s
            Has no effect when globalEnabled is false (everything is disabled regardless).
            Use false if you want RPGMobs enabled only in specific worlds that you've configured.""")
    public boolean enabledByDefault = true;

    @Default
    @Cfg(group = "Debug", file = "core.yml", comment = "Enable or disable debug mode for verbose logging.")
    public boolean isDebugModeEnabled = false;

    @Default
    @Min(1.0)
    @Max(600.0)
    @Cfg(group = "Debug", file = "core.yml", comment = "Interval in seconds for debug mob-rule scanning. Only used if debug mode is on.")
    public int debugMobRuleScanIntervalSeconds = 10;
}
