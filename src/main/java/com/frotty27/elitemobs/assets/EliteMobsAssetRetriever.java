package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public final class EliteMobsAssetRetriever {

    private final EliteMobsPlugin plugin;
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public EliteMobsAssetRetriever(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public <A> @Nullable A getAsset(@Nullable String id, Function<String, A> loader) {
        if (id == null || id.isBlank()) return null;

        A asset = loader.apply(id);
        EliteMobsConfig config = plugin.getConfig();
        if (asset == null && config != null && config.debugConfig.isDebugModeEnabled) {
            EliteMobsLogger.debug(logger, "Missing asset id=%s", EliteMobsLogLevel.WARNING, id);
        }
        return asset;
    }
}

