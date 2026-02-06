package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public final class EliteMobsAssetLoader {

    private final EliteMobsPlugin plugin;
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public EliteMobsAssetLoader(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public <A> @Nullable A getAsset(@Nullable String id, Function<String, A> loader) {
        if (id == null || id.isBlank()) return null;

        A asset = loader.apply(id);
        EliteMobsConfig config = plugin.getConfig();
        if (asset == null && config != null && config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(logger, "Missing asset id=%s", EliteMobsLogLevel.WARNING, id);
        }
        return asset;
    }
}

