package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryAttachmentTypes;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

import static me.jackz.pocket_storage.Pocket_storage.LOGGER;

public class RegionStorage extends SavedData {
    public static final int DISTANCE_BETWEEN_NODES = 100;

    private final Map<UUID, StorageNodeData> nodesDataMap = new HashMap<>();
    private final Map<UUID, Set<UUID>> playerNodesMap = new HashMap<>();

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
            StorageNodeData nodeData = StorageNodeData.deserialize(entry);
            storage.nodesDataMap.put(nodeData.id, nodeData);

            // Track owners' ids
            // Create initial set
            if(!storage.playerNodesMap.containsKey(nodeData.ownerUUID)) {
                storage.playerNodesMap.put(nodeData.ownerUUID, new HashSet<>());
            }
            Set<UUID> nodeList = storage.playerNodesMap.get(nodeData.ownerUUID);
            nodeList.add(nodeData.id);
        }
        storage.nextPos = BlockPos.of(tag.getLong("next_pos"));
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, StorageNodeData> e : nodesDataMap.entrySet()) {
            CompoundTag entry = e.getValue().serialize();
            list.add(entry);
        }
        tag.put("regions", list);
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> e : playerNodesMap.entrySet()) {
            CompoundTag entry = new CompoundTag();

            ListTag nodeList = new ListTag();
            for(UUID uuid : e.getValue()) {
                CompoundTag node = new CompoundTag();
                node.putUUID("id", uuid);
                nodeList.add(node);
            }

            entry.putUUID("ownerUUID", e.getKey());
            entry.put("nodeIds", nodeList);

            playerList.add(entry);
        }
        tag.put("player_owned_regions", playerList);
        tag.putLong("next_pos", nextPos.asLong());
        return tag;
    }

    private static final int REGION_BLOCKS = 512;
    private static final int REGION_BLOCKS_HALF = REGION_BLOCKS / 2;

    private BlockPos advanceNextPosition() {
        int x = nextPos != null ? nextPos.getX() : -REGION_BLOCKS_HALF;
        int z = nextPos != null ? nextPos.getZ() : 0;
        nextPos = new BlockPos(x + DISTANCE_BETWEEN_NODES, 64, z);
        // Check for overflow on X, then jump value of Z
        if(nextPos.getX() >= REGION_BLOCKS_HALF) {
            nextPos = new BlockPos(-REGION_BLOCKS_HALF, 64, z + DISTANCE_BETWEEN_NODES);
        }
        return nextPos;
    }

    public StorageNode createNode(@NotNull ServerPlayer ownerPlayer, ResourceLocation templateId) {
        BlockPos nextPos = advanceNextPosition();
        ServerLevel dim = getStorageLevel(ownerPlayer.getServer());

        UUID id = UUID.randomUUID();
//        StorageNodeData data =  new StorageNodeData(ownerPlayer.getUUID(), nextPos, );
        StorageNode node = StorageNode.create(id, dim, ownerPlayer, templateId, nextPos);
        nodesDataMap.put(id, node.getData());

        Pocket_storage.LOGGER.info("Created new node {} owned by {} at {}", id, ownerPlayer, nextPos);

        this.setDirty();
        return node;
    }

    public Set<UUID> getNodeIds() {
        return nodesDataMap.keySet();
    }

    public Set<UUID> getNodeIdsForPlayer(UUID id) {
        return playerNodesMap.get(id);
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

        return StorageNode.fromData(data);
    }

    /**
     * Searches all nodes for the first one that is owned by given owner UUID
     */
    @Nullable
    public StorageNode findNodeByOwner(UUID ownerUUID) {
        for (Map.Entry<UUID, StorageNodeData> entry : nodesDataMap.entrySet()) {
            StorageNodeData data = entry.getValue();
            if(data.ownerUUID == ownerUUID) {
                return StorageNode.fromData(data);
            }
        }
        return null;
    }
    /**
     * Searches all nodes for the first one that is owned by given owner player
     */
    @Nullable
    public StorageNode findNodeByOwner(ServerPlayer player) {
        return findNodeByOwner(player.getUUID());
    }

    /**
     * Returns the node the player is currently in, if any
     */
    @Nullable
    public StorageNode getActiveNode(ServerPlayer player) {
        if(player.hasData(RegistryAttachmentTypes.NODE_ID)) {
            UUID id = player.getData(RegistryAttachmentTypes.NODE_ID);
            return getNode(id);
        }
        return null;
    }

    @Nullable
    public static StructureTemplate resolveTemplate(ServerLevel level, ResourceLocation templateId) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(templateId);
        if(template.isEmpty()) {
            LOGGER.error("Room template \"{}\" not found", templateId);
            return null;
        }
        return template.get();
    }
}
