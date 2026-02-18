package com.frotty27.rpgmobs.integrations;

import com.hypixel.hytale.logger.HytaleLogger;
import org.zuxaw.plugin.api.EntityKillContext;
import org.zuxaw.plugin.api.ExperienceGainedEvent;
import org.zuxaw.plugin.api.RPGLevelingAPI;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class RPGLevelingBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static boolean isAvailable() {
        try {
            return RPGLevelingAPI.isAvailable();
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    public void registerXPListener(BiConsumer<ExperienceGainedEvent, UUID> handler) {
        try {
            RPGLevelingAPI api = RPGLevelingAPI.get();
            if (api == null) return;
            api.registerExperienceGainedListener(event -> {
                EntityKillContext ctx = event.getEntityKillContext();
                if (ctx != null && ctx.getEntityUuid() != null) {
                    handler.accept(event, ctx.getEntityUuid());
                }
            });
        } catch (Exception e) {
            LOGGER.atWarning().log("[RPGMobs] Failed to register RPGLeveling XP listener: %s", e.getMessage());
        }
    }
}
