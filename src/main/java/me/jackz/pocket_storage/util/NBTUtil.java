package me.jackz.pocket_storage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

public class NBTUtil {
    public static ListTag putVec3(CompoundTag tag, String key, Vec3 vec) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(vec.x));
        list.add(DoubleTag.valueOf(vec.y));
        list.add(DoubleTag.valueOf(vec.z));
        tag.put(key, list);
        return list;
    }


    public static Vec3 getVec3(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, DoubleTag.TAG_DOUBLE);
        return new Vec3(
                list.getDouble(0),
                list.getDouble(1),
                list.getDouble(2)
        );
    }

    public static ListTag putVec3i(CompoundTag tag, String key, Vec3i vec) {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(vec.getX()));
        list.add(IntTag.valueOf(vec.getY()));
        list.add(IntTag.valueOf(vec.getZ()));
        tag.put(key, list);
        return list;
    }


    public static Vec3i getVec3i(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, IntTag.TAG_INT);
        return new Vec3i(
                list.getInt(0),
                list.getInt(1),
                list.getInt(2)
        );
    }

    public static BlockPos getBlockPos(CompoundTag tag, String key) {
        Vec3i vec = getVec3i(tag, key);
        return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
    }

    public static ListTag putBlockPos(CompoundTag tag, String key, BlockPos pos) {
        return putVec3(tag, key, pos.getCenter());
    }
}
