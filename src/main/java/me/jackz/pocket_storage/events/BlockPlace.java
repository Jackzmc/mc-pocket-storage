package me.jackz.pocket_storage.events;

import me.jackz.pocket_storage.registry.RegistryBlocks;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

@EventBusSubscriber(modid = MODID)
public class BlockPlace {
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
}
