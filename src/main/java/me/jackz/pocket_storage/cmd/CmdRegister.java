package me.jackz.pocket_storage.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.jackz.pocket_storage.blocks.PocketChestBlock;
import me.jackz.pocket_storage.dim.RegionStorage;
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
                .executes(ctx -> listNodes(ctx.getSource()))))
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

    private static int listNodes(CommandSourceStack source) {
        RegionStorage store = RegionStorage.get(source.getLevel());
        Set<UUID> ids = store.getNodeIds();
        if(ids.isEmpty()) {
            source.sendSystemMessage(Component.literal("No nodes found").withColor(Color.RED.getRGB()));
        }
        for(UUID id : ids) {
            source.sendSystemMessage(Component.literal(id.toString()));
        }
        return 1;
    }
}
