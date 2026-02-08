package com.frotty27.elitemobs.commands;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public final class EliteMobsRootCommand extends AbstractCommand {

    public EliteMobsRootCommand(EliteMobsPlugin plugin) {
        super("elitemobs", "EliteMobs root command.");

        addSubCommand(new EliteMobsReloadCommand(plugin));
        if (plugin.getConfig().debugConfig.isDebugModeEnabled)
            addSubCommand(new EliteMobsSpawnCommand(plugin));
        addAliases("em");
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
        ctx.sendMessage(Message.raw("[EliteMobs] Usage: /elitemobs reload"));
        return CompletableFuture.completedFuture(null);
    }
}
