package me.jackz.pocket_storage.dim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class StorageManager {
    // load data where UUID maps to specific corner (bottom left) X/Z, and size
    private static final String DATA_NAME = "storage_regions";

    // uuid -> region corner
    /**
     * TODO:
     * 1. on place, call some method to get ID and location
     * 2. register it somehow
     */

    public static final int SIZE = 40;

    // The distance between each node
    private static final int OFFSET_SIZE = 100;

}
