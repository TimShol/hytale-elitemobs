package com.frotty27.rpgmobs.commands;

import com.frotty27.rpgmobs.permissions.RPGMobsPermissions;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.ui.RPGMobsAdminPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class RPGMobsConfigCommand extends AbstractPlayerCommand {

    private final RPGMobsPlugin plugin;

    public RPGMobsConfigCommand(RPGMobsPlugin plugin) {
        super("config", "Open the RPGMobs admin configuration UI.");
        this.plugin = plugin;

        requirePermission(RPGMobsPermissions.RPGMOBS_CONFIG);
    }

    @Override
    protected void execute(@NonNull CommandContext ctx, @NonNull Store<EntityStore> store,
                           @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        if (!playerRef.isValid()) {
            ctx.sendMessage(Message.raw("[RPGMobs] Player reference is not available."));
            return;
        }

        if (plugin.getGlobalConfig() == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Config not loaded yet."));
            return;
        }

        CommandSender sender = ctx.sender();
        if (!(sender instanceof Player player)) {
            ctx.sendMessage(Message.raw("[RPGMobs] This command can only be used by players."));
            return;
        }

        RPGMobsAdminPage page = new RPGMobsAdminPage(playerRef, plugin);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
