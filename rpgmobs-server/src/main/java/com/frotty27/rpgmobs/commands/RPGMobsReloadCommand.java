package com.frotty27.rpgmobs.commands;

import com.frotty27.rpgmobs.permissions.RPGMobsPermissions;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public final class RPGMobsReloadCommand extends AbstractCommand {

    private final RPGMobsPlugin plugin;

    public RPGMobsReloadCommand(RPGMobsPlugin plugin) {
        super("reload", "Reload RPGMobs YAML config and regenerate assets.");
        this.plugin = plugin;

        requirePermission(RPGMobsPermissions.RPGMobs_RELOAD);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
        try {
            plugin.reloadConfigAndAssets();

            ctx.sendMessage(Message.raw("[RPGMobs] Reloaded config & regenerated assets."));
        } catch (Throwable t) {
            ctx.sendMessage(Message.raw("[RPGMobs] Reload failed: " + t));
            t.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }
}
