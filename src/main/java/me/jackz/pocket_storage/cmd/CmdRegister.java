package me.jackz.pocket_storage.cmd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static me.jackz.pocket_storage.Pocket_storage.MODID;
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
        return Commands.literal("pocket")
        .then(Commands.literal("chest")
            .executes(ctx -> giveChest(ctx.getSource(), ctx.getSource().getPlayer(), null))  // /pocket chest (no uuid)
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
}
