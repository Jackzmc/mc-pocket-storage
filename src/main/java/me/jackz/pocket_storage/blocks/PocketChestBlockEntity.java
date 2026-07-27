package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static me.jackz.pocket_storage.registry.BlockEntities.POCKET_CHEST;

public class PocketChestBlockEntity extends ChestBlockEntity {
    public PocketChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(POCKET_CHEST.get(), pos, blockState);
    }

}
