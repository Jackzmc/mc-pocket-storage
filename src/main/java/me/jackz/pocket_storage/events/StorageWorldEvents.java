package me.jackz.pocket_storage.events;

import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageNode;
import me.jackz.pocket_storage.registry.RegistryBlocks;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

@EventBusSubscriber(modid = MODID)
public class StorageWorldEvents {
    @SubscribeEvent
    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getPlacedBlock().is(RegistryBlocks.POCKET_CHEST.get())) return;

        LevelAccessor level = event.getLevel();
        // Prevent placing block inside dimension
        if(level instanceof ServerLevelAccessor sla) {
            if(sla.getLevel().dimension().equals(RegistryDims.STORAGE_DIM)) {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal("Cannot place in this world")));
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent()
    private static void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        // Restore player back to position outside of storage world on quit
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if(player.level().dimension() ==  RegistryDims.STORAGE_DIM) {
            StorageNode.restorePlayer(player);
        }
    }

    @SubscribeEvent()
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel voidLevel = server.getLevel(RegistryDims.STORAGE_DIM);
        if (voidLevel == null) return;

        RegionStorage store = RegionStorage.get(voidLevel);
        List<ServerPlayer> players = new ArrayList<>(voidLevel.players());
        for (ServerPlayer player : players) {
            if(player.getY() < 0) {
                voidLevel.getServer().execute(() -> {
                    StorageNode.restorePlayer(player);
                });
                continue;
            }
            if (!player.isShiftKeyDown() || !player.onGround()) continue;
            // Require to stand still, mostly to reduce multiple calls
            if (player.getDeltaMovement().lengthSqr() > 0.01) continue;
            // 3. In the target region?
            BlockPos pos = player.blockPosition();
            // TODO: impl check for specific center region
            StorageNode node = store.getActiveNode(player);
            if(node != null) {
                if(pos.distSqr(node.getBlockBottomCenter()) < 8f) {
                    voidLevel.getServer().execute(() -> {
                        StorageNode.restorePlayer(player);
                    });
                }
            }
        }
    }
}
