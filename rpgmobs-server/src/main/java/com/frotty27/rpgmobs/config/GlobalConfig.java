package com.frotty27.rpgmobs.config;

import com.frotty27.rpgmobs.config.schema.*;

public final class GlobalConfig {

    @CfgVersion
    @Cfg(group = "System", file = "core.yml", comment = "Configuration version. Automatically updated by the mod. WARNING: If this field is missing or set to 0.0.0, ALL config files will be deleted and regenerated with fresh defaults on next startup. Do not remove this field.")
    public String configVersion = "0.0.0";

    @Default
    @Cfg(group = "System", file = "core.yml", comment = """
            Config format version. When this value is lower than the version expected by the mod,
            ALL config files (base, world overlays, instance overlays) are wiped and regenerated.
            This is NOT tied to plugin versions — it only changes when the config layout breaks.
            Do not change this manually.""")
    public int configFormatVersion = 3;

    @Default
    @Cfg(group = "Worlds", file = "core.yml", comment = """
            When true (default): worlds/instances without an explicit overlay file use the base config.
            When false: worlds/instances without an overlay file have RPGMobs completely disabled (no elites spawn).
            \s
            Use false if you want RPGMobs enabled only in specific worlds/instances that you've created overlay files for.""")
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
