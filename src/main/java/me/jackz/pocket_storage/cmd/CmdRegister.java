package me.jackz.pocket_storage.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.jackz.pocket_storage.Config;
import me.jackz.pocket_storage.blocks.PocketChestBlock;
import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Set;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static me.jackz.pocket_storage.Pocket_storage.MODID;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.UuidArgument.getUuid;
import static net.minecraft.commands.arguments.UuidArgument.uuid;

@EventBusSubscriber(modid = MODID)
public class CmdRegister {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(build());
    }

    private static final SuggestionProvider<CommandSourceStack> NODE_IDS = (ctx, b) -> {
        RegionStorage store = RegionStorage.get(ctx.getSource().getLevel());
        for(UUID id : store.getNodeIds()) {
            b.suggest(id.toString());
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PLAYER_NAMES = (ctx, b) -> {
        for(String name : ctx.getSource().getLevel().getServer().getPlayerNames()) {
            b.suggest(name);
        }
        return b.buildFuture();
    };

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("pocket")
        .then(literal("nodes")
            .then(literal("list")
                .executes(ctx -> listNodes(ctx.getSource()))
                .then(Commands.argument("player", string())
                        .suggests(PLAYER_NAMES)
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getLevel().getServer();
                            ServerPlayer player = server.getPlayerList().getPlayerByName(getString(ctx, "player"));
                            if(player == null) {
                                ctx.getSource().sendFailure(Component.literal("No player found").withColor(Color.RED.getRGB()));
                                return -1;
                            }
                            return listNodesForPlayer(ctx.getSource(), player.getUUID());
                        })
                )
            )
            .then(literal("create")
                .executes(ctx -> createNode(ctx.getSource())))
            .then(literal("enter")
                .then(argument("nodeid", uuid())
                    .suggests(NODE_IDS)
                    .executes(ctx -> enterNode(ctx.getSource(), getUuid(ctx, "nodeid")))
                )
            )
            .then(literal("info")
                .executes(ctx -> infoNode(ctx.getSource(), null))
                .then(argument("nodeid", uuid())
                    .suggests(NODE_IDS)
                    .executes(ctx -> infoNode(ctx.getSource(), getUuid(ctx, "nodeid")))
                )
            )
        )
        .then(literal("chest")
            .then(literal("new")
                .executes(ctx -> giveChest(ctx.getSource(), ctx.getSource().getPlayer(), null))
            )
            .then(Commands.argument("id", uuid())
                .suggests(NODE_IDS)
                .executes(ctx -> giveChest(ctx.getSource(), ctx.getSource().getPlayer(), getUuid(ctx, "id"))))
                .then(Commands.argument("player", string())
                    .suggests(PLAYER_NAMES)
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getLevel().getServer();
                        ServerPlayer player = server.getPlayerList().getPlayerByName(getString(ctx, "player"));
                        return giveChest(ctx.getSource(), player, getUuid(ctx, "id"));
                    })
                )
        );
    }

    private static int giveChest(CommandSourceStack source, @Nullable ServerPlayer player, @Nullable UUID nodeId) {
        if(player != null) {
            ItemStack item = PocketChestBlock.getChestItem(nodeId);
            player.getInventory().add(item);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Player not found").withColor(Color.RED.getRGB()));
            return -1;
        }
    }

    private static int createNode(CommandSourceStack source) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("Must be a player").withColor(Color.RED.getRGB()));
            return -1;
        }
        StorageNode node = store.createNode(source.getPlayer(), Config.DefaultStructureTemplate);
        source.sendSystemMessage(Component.literal("Node: ").append(node.getId().toString()));
        source.sendSystemMessage(Component.literal("Corner: ").append(node.getCorner().toShortString()));
        return 1;
    }

    private static int enterNode(CommandSourceStack source, UUID nodeId) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("Must be a player").withColor(Color.RED.getRGB()));
            return -1;
        }
        StorageNode node = store.getNode(nodeId);
        if(node == null) {
            source.sendFailure(Component.literal("No node found with id").withColor(Color.RED.getRGB()));
            return -2;
        }
        node.teleportPlayerTo(source.getPlayer());
        return 1;
    }

    private static int infoNode(CommandSourceStack source, @Nullable UUID nodeId) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        StorageNode node;

        if(nodeId != null) {
            node = store.getNode(nodeId);
        } else if(source.isPlayer()) {
            node = store.getActiveNode(source.getPlayer());
            if(node == null) {
                source.sendFailure(Component.literal("You are not in a node").withColor(Color.RED.getRGB()));
                return -1;
            }
        } else {
            source.sendFailure(Component.literal("Must be a player").withColor(Color.RED.getRGB()));
            return -1;
        }
        StorageNode.inspectNode(source, node);
        return 1;
    }

    private static int listNodes(CommandSourceStack source) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        Set<UUID> ids = store.getNodeIds();
        if(ids.isEmpty()) {
            source.sendSystemMessage(Component.literal("No nodes exist").withColor(Color.RED.getRGB()));
        } else {
            source.sendSystemMessage(Component.literal("Found " + ids.size() + " nodes").withColor(Color.GREEN.getRGB()));
        }
        for(UUID id : ids) {
            source.sendSystemMessage(Component.literal(id.toString()));
        }
        return 1;
    }

    private static int listNodesForPlayer(CommandSourceStack source, UUID playerId) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        Set<UUID> ids = store.getNodeIdsForPlayer(playerId);
        if(ids.isEmpty()) {
            source.sendSystemMessage(Component.literal("No nodes found for player").withColor(Color.RED.getRGB()));
        } else {
            source.sendSystemMessage(Component.literal("Player has " + ids.size() + " nodes").withColor(Color.GREEN.getRGB()));
        }
        for(UUID id : ids) {
            source.sendSystemMessage(Component.literal(id.toString()));
        }
        return 1;
    }
}
