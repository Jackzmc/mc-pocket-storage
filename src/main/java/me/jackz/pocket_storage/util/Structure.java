package me.jackz.pocket_storage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class Structure {
    public static void buildBox(ServerLevel level, BlockPos corner, int sizeX, int sizeY, int sizeZ, BlockState wallBlock) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for(int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    boolean isShell = x == 0 || x == sizeX - 1
                            || y == 0 || y == sizeY - 1
                            || z == 0 || z == sizeZ - 1;
                    if (isShell) {
                        cursor.setWithOffset(corner, x, y, z);
                        level.setBlock(cursor, wallBlock, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
