package me.jackz.pocket_storage.items;

import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageNode;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PocketToolItem extends Item {

    public PocketToolItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if(level.dimension() == RegistryDims.STORAGE_DIM) {
            RegionStorage store = RegionStorage.get((ServerLevel) level);
            StorageNode node = store.getActiveNode((ServerPlayer) player);
            StorageNode.inspectNode((ServerPlayer) player, node);
            return InteractionResultHolder.success(player.getMainHandItem());
        }
        return InteractionResultHolder.pass(player.getMainHandItem());
    }
}