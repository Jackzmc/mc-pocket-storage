package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.blocks.PocketChestBlock;
import me.jackz.pocket_storage.registry.RegisterAttachmentTypes;
import me.jackz.pocket_storage.registry.RegistryBlocks;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.util.LevelLocationAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.UUID;

import static me.jackz.pocket_storage.util.Structure.buildBox;

public class StorageNode {
    private StorageNodeData data;
    private UUID id;
    public StorageNode(UUID nodeId, StorageNodeData data) {
        this.id = nodeId;
        this.data = data;
    }

    public BlockPos getBlockCenter() {
        int x = data.cornerPos.getX();
        int y = data.cornerPos.getY();
        int z = data.cornerPos.getY();
        int center = RegionStorage.SIZE / 2;
        return new BlockPos(x + center, y + center,z + center);
    }

    public BlockPos getBlockBottomCenter() {
        int x = data.cornerPos.getX();
        int z = data.cornerPos.getY();
        int center = RegionStorage.SIZE / 2;
        return new BlockPos(x + center, data.cornerPos.getY(),z + center);
    }

    public void teleportPlayerTo(ServerPlayer player) {
        // Store player's last location
        LevelLocationAttachment attach = LevelLocationAttachment.fromPlayerPosition(player);
        if(attach.dim == RegistryDims.STORAGE_DIM) {
            // Ensure we never restore back to storage world
            attach.dim = ServerLevel.OVERWORLD;
        }
        Pocket_storage.LOGGER.debug("dim set to {}", attach.dim);
        Pocket_storage.LOGGER.debug("stored last loc {}", attach);
        player.setData(RegisterAttachmentTypes.LAST_LOCATION, attach);
        player.setData(RegisterAttachmentTypes.NODE_ID, id);

        StorageDimTransition.enterStorageDimension(player, getBlockCenter().getCenter());
    }

    public static boolean restorePlayer(ServerPlayer player) {
        LevelLocationAttachment attachment = LevelLocationAttachment.fromPlayerPosition(player);
        player.removeData(RegisterAttachmentTypes.NODE_ID);
        if(attachment.dim == RegistryDims.STORAGE_DIM || !attachment.tryRestore(player)) {
            Pocket_storage.LOGGER.warn("No restore location found - using fallback");
            // Always ensure player leaves the storage world, by just putting them to their bed OR spawn
            BlockPos fallback = player.getRespawnPosition();
            if(fallback == null) {
                ServerLevel overworld = player.getServer().getLevel(ServerLevel.OVERWORLD);
                assert overworld != null;
                fallback = overworld.getSharedSpawnPos();
            }
            StorageDimTransition.enterOverworld(player, fallback.getCenter());
            return false;
        }
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return data.ownerUUID;
    }

    protected void createStructure(ServerLevel level) {
        BlockPos btmCenter = getBlockCenter();
        // Add exit chest
        Block block = RegistryBlocks.POCKET_CHEST.get();
        level.setBlock(btmCenter.above(), block.defaultBlockState(), Block.UPDATE_NONE);

        BlockState wallBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:bedrock"))
                .defaultBlockState();
        buildBox(level, data.cornerPos, RegionStorage.SIZE, RegionStorage.SIZE, RegionStorage.SIZE, wallBlock);

        BlockState lightBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:torch"))
                        .defaultBlockState();
        level.setBlock(btmCenter.east(), lightBlock, Block.UPDATE_NONE);
    }


}
