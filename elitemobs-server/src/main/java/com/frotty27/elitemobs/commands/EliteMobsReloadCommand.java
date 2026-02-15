package com.frotty27.elitemobs.commands;

import java.util.concurrent.CompletableFuture;

import com.frotty27.elitemobs.permissions.EliteMobsPermissions;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jspecify.annotations.NonNull;

public final class EliteMobsReloadCommand extends AbstractCommand {

    private final EliteMobsPlugin plugin;

    public EliteMobsReloadCommand(EliteMobsPlugin plugin) {
        super("reload", "Reload EliteMobs YAML config and regenerate assets.");
        this.plugin = plugin;

        requirePermission(EliteMobsPermissions.ELITEMOBS_RELOAD);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
        try {
            plugin.reloadConfigAndAssets();

            ctx.sendMessage(Message.raw("[EliteMobs] Reloaded config & regenerated assets."));
        } catch (Throwable t) {
            ctx.sendMessage(Message.raw("[EliteMobs] Reload failed: " + t));
            t.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }
}
