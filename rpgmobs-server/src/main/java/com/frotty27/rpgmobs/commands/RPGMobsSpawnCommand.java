package com.frotty27.rpgmobs.commands;

import com.frotty27.rpgmobs.permissions.RPGMobsPermissions;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.spawn.RPGMobsSpawnSystem;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderInfo;
import com.hypixel.hytale.server.npc.commands.NPCCommand;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsSpawnCommand extends AbstractPlayerCommand {

    private final RPGMobsPlugin plugin;
    private final RequiredArg<BuilderInfo> roleArg;
    private final RequiredArg<Integer> tierArg;

    public RPGMobsSpawnCommand(RPGMobsPlugin plugin) {
        super("spawn", "Spawn a specific NPC role at a chosen RPGMobs tier.");
        this.plugin = plugin;

        requirePermission(RPGMobsPermissions.RPGMobs_SPAWN);

        roleArg = withRequiredArg("npcRole", "NPC role name.", NPCCommand.NPC_ROLE);
        tierArg = withRequiredArg("tier", "RPGMobs tier (1-5).", ArgTypes.INTEGER).addValidator(Validators.range(1, 5));
    }

    @Override
    protected void execute(@NonNull CommandContext ctx, @NonNull Store<EntityStore> entityStore,
                           @NonNull Ref<EntityStore> playerRef, @NonNull PlayerRef player, @NonNull World world) {
        if (plugin.getConfig() == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Config is not loaded yet."));
            return;
        }

        BuilderInfo roleInfo = roleArg.get(ctx);
        if (roleInfo == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Invalid NPC role."));
            return;
        }

        int tierNumber = tierArg.get(ctx);
        int tierIndex = Math.max(0, Math.min(4, tierNumber - 1));

        TransformComponent transform = entityStore.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Failed to read your position."));
            return;
        }

        Vector3d position = new Vector3d(transform.getPosition()).add(0.0, 0.1, 0.0);
        Vector3f rotation = new Vector3f(transform.getRotation());

        String roleName = roleInfo.getKeyName();
        Pair<Ref<EntityStore>, ?> spawned = NPCPlugin.get().spawnNPC(entityStore, roleName, null, position, rotation);

        if (spawned == null || spawned.first() == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Failed to spawn NPC role: " + roleName));
            return;
        }

        Ref<EntityStore> npcRef = spawned.first();
        RPGMobsSpawnSystem spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Spawn system not available yet."));
            return;
        }

        NPCEntity npcEntity = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null) {
            ctx.sendMessage(Message.raw("[RPGMobs] Spawned NPC, but entity data was not ready yet."));
            return;
        }

        AtomicBoolean applied = new AtomicBoolean(false);
        entityStore.forEachChunk((chunk, commandBuffer) -> {
            spawnSystem.applyTierFromCommand(plugin.getConfig(),
                                             npcRef,
                                             entityStore,
                                             commandBuffer,
                                             npcEntity,
                                             tierIndex
            );
            applied.set(true);
            return false;
        });

        if (!applied.get()) {
            ctx.sendMessage(Message.raw("[RPGMobs] Spawned NPC, but failed to apply RPGMobs tier."));
            return;
        }

        ctx.sendMessage(Message.raw(String.format("[RPGMobs] Spawned %s at tier %d.", roleName, tierIndex + 1)));
    }
}
