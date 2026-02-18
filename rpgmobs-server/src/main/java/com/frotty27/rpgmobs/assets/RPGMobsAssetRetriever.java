package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public final class RPGMobsAssetRetriever {

    private final RPGMobsPlugin plugin;
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public RPGMobsAssetRetriever(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public <A> @Nullable A getAsset(@Nullable String id, Function<String, A> loader) {
        if (id == null || id.isBlank()) return null;

        A asset = loader.apply(id);
        RPGMobsConfig config = plugin.getConfig();
        if (asset == null && config != null && config.debugConfig.isDebugModeEnabled) {
            RPGMobsLogger.debug(logger, "Missing asset id=%s", RPGMobsLogLevel.WARNING, id);
        }
        return asset;
    }
}
