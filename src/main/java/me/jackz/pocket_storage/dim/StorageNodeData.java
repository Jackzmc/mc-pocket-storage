package me.jackz.pocket_storage.dim;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public class StorageNodeData {
    BlockPos cornerPos;
    UUID ownerUUID;

    protected StorageNodeData(UUID ownerUUID, BlockPos cornerPos) {
        this.ownerUUID = ownerUUID;
        this.cornerPos = cornerPos;
    }
}
