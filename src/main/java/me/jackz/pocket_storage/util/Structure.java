package me.jackz.pocket_storage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class Structure {
    /**
     * Builds a shell starting from corner, extending +X +Y +Z according to size.
     * @param level world to build in
     * @param corner starting corner
     * @param sizeX number of blocks in +X
     * @param sizeY number of blocks in +Y
     * @param sizeZ number of blocks in +Z
     * @param wallBlock the block to make wall of
     * @param insideBlock the block to fill inside with, or null to not replace any blocks inside
     */
    public static void buildBox(ServerLevel level, BlockPos corner, int sizeX, int sizeY, int sizeZ, BlockState wallBlock, @Nullable BlockState insideBlock) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for(int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    boolean isShell = x == 0 || x == sizeX - 1
                            || y == 0 || y == sizeY - 1
                            || z == 0 || z == sizeZ - 1;
                    cursor.setWithOffset(corner, x, y, z);
                    if (isShell) {
                        level.setBlock(cursor, wallBlock, Block.UPDATE_CLIENTS);
                    } else if(insideBlock != null) {
                        level.setBlock(cursor, insideBlock, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
