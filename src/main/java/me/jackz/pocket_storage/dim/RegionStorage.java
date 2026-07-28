package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import javax.swing.plaf.synth.Region;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RegionStorage extends SavedData {
    public static final int DISTANCE_BETWEEN = 100;
    public static final int SIZE = 50;

    private Map<UUID, StorageNodeData> nodesDataMap = new HashMap<>();

    private BlockPos nextPos;

    public static RegionStorage create() {
        return new RegionStorage();
    }

    public static ServerLevel getStorageLevel(MinecraftServer server) {
        return server.getLevel(RegistryDims.STORAGE_DIM);
    }

    public static RegionStorage get(ServerLevel level) {
        ServerLevel storageLvl = getStorageLevel(level.getServer());
        return storageLvl.getDataStorage()
                .computeIfAbsent(new Factory<>(RegionStorage::create, RegionStorage::load), "pocket_storage_dim_data");
    }

    // Load existing instance of saved data
    public static RegionStorage load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        RegionStorage storage = new RegionStorage();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID id = entry.getUUID("ownerUUID");
            BlockPos pos = BlockPos.of(entry.getLong("origin"));
            StorageNodeData nodeData = new StorageNodeData(id, pos);
            storage.nodesDataMap.put(id, nodeData);
        }
        storage.nextPos = BlockPos.of(tag.getLong("next_pos"));
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, StorageNodeData> e : nodesDataMap.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("ownerUUID", e.getValue().ownerUUID);
            entry.putLong("origin", e.getValue().cornerPos.asLong());
            list.add(entry);
        }
        tag.put("regions", list);
        tag.putLong("next_pos", nextPos.asLong());
        return tag;
    }

    private BlockPos advanceNextPosition() {
        int x = nextPos != null ? nextPos.getX() : 0;
        int z = nextPos != null ? nextPos.getZ() : 0;
        // TODO: make this grid instead of linear line
        nextPos = new BlockPos(x + DISTANCE_BETWEEN, 64, z);
        return nextPos;
    }

    public StorageNode createNode(Player ownerPlayer) {
        BlockPos nextPos = advanceNextPosition();

        UUID id = UUID.randomUUID();
        StorageNodeData data =  new StorageNodeData(ownerPlayer.getUUID(), nextPos);
        StorageNode node = new StorageNode(id, data);
        nodesDataMap.put(id, data);

        ServerLevel dim = getStorageLevel(ownerPlayer.getServer());

        node.createStructure(dim);

        Pocket_storage.LOGGER.info("Created new node {} owned by {} at {}", id, ownerPlayer, nextPos);

        this.setDirty();
        return node;
    }

    public void deleteNode(UUID id) {
        StorageNodeData data = nodesDataMap.get(id);
        if(data != null) {
            nodesDataMap.remove(id);
            // TODO: delete data.cornerPos
        }
        this.setDirty();
    }

    @Nullable
    public StorageNode getNode(UUID id) {
        StorageNodeData data = nodesDataMap.get(id);
        if(data == null) return null;

        return new StorageNode(id, data);
    }
}
