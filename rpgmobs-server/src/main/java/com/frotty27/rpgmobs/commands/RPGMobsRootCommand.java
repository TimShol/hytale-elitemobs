package com.frotty27.rpgmobs.commands;

import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public final class RPGMobsRootCommand extends AbstractCommand {

    public RPGMobsRootCommand(RPGMobsPlugin plugin) {
        super("RPGMobs", "RPGMobs root command.");

        addSubCommand(new RPGMobsReloadCommand(plugin));
        if (plugin.getConfig().debugConfig.isDebugModeEnabled) addSubCommand(new RPGMobsSpawnCommand(plugin));
        addAliases("em");
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
        ctx.sendMessage(Message.raw("[RPGMobs] Usage: /RPGMobs reload"));
        return CompletableFuture.completedFuture(null);
    }
}
