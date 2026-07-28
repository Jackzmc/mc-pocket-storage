package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;


public class StorageDimTransition {
    public static DimensionTransition to(ServerLevel targetLevel, Vec3 pos) {
        return new DimensionTransition(targetLevel, pos, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING);
    }

    public static DimensionTransition to(ServerLevel targetLevel, Vec3 pos, Vec2 rotation) {
        return new DimensionTransition(targetLevel, pos, Vec3.ZERO, rotation.y, rotation.x, DimensionTransition.DO_NOTHING);
    }

    public static void enterStorageDimension(ServerPlayer player, Vec3 pos) {
        ServerLevel level = player.getServer().getLevel(RegistryDims.STORAGE_DIM);
        if(level == null) {
            Pocket_storage.LOGGER.error("Storage dimension level does not exist");
            return;
        }
        player.changeDimension(
                new DimensionTransition(level, pos, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING)
        );
    }

    public static void enterOverworld(ServerPlayer player, Vec3 pos) {
        ServerLevel level = player.getServer().getLevel(ServerLevel.OVERWORLD);
        if(level == null) {
            Pocket_storage.LOGGER.error("Overworld dimension level does not exist");
            return;
        }
        player.changeDimension(
                new DimensionTransition(level, pos, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING)
        );
    }

    public static class TransitionException extends Exception {
        public TransitionException(String message) {
            super(message);
        }
    }
}
