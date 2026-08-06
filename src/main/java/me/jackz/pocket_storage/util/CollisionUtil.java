package me.jackz.pocket_storage.util;

import me.jackz.pocket_storage.Pocket_storage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class CollisionUtil {
    /**
     * Clears a spot for player (2 blocks) above position
     * @param level world
     * @param pos the position to clear *above*
     */
    public static void clearSpotAbove(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mut = pos.mutable();
        for (int i = 0; i < 2; i++) {
            mut.setY(mut.getY() + 1);
            BlockState state = level.getBlockState(mut);
            // ensure we never break bedrock
            if (!state.isAir() && state != Blocks.BEDROCK.defaultBlockState()) {
                Pocket_storage.LOGGER.debug("Clear block ({}) at {}", state, mut);
                level.destroyBlock(mut, true);
            }
        }
    }

    /**
     * Attempts to find a safe location to teleport to, starting with center and checking sides after
     * @param level level to check in
     * @param startOrigin the center starting point
     * @param height the maximum height to search from startOrigin
     * @return position or null if none found
     */
    @Nullable
    public static Vec3 findSafePosition(ServerLevel level, BlockPos startOrigin, int height) {
        int yMin = startOrigin.getY();
        int yMax = yMin + height;

        // Scan starting at center, then +-x and +- z
        int cx = startOrigin.getX();
        int cz = startOrigin.getZ();
        int[][] offsets = {
                {0, 0},   // center first
                {-1, 0},  // -X
                {1, 0},   // +X
                {0, -1},  // -Z
                {0, 1}    // +Z
        };

        for (int[] off : offsets) {
            int x = cx + off[0];
            int z = cz + off[1];
            Vec3 result = scanColumn(level, x, z, yMin, yMax);
            if (result != null) return result;
        }
        return null;
    }

    public static Vec3 scanColumn(ServerLevel level, int x, int z, int yMin, int yMax) {
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        cur.setX(x);
        cur.setY(yMin);
        cur.setZ(z);

        Pocket_storage.LOGGER.trace("scanning column {}-{} at x={} y={}", yMin, yMax, x, z);

        // Scan from bottom to top for a safe spot to teleport onto
        for (int y = yMin + 1; y < yMax; y++) {
            cur.setY(y);
            Vec3 spot = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, cur, true);
            if(spot != null) {
                Pocket_storage.LOGGER.debug("found safe spot at {}", cur);
                return spot;
            }
        }
        return null;
    }

}
