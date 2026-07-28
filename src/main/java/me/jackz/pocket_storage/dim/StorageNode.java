package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryAttachmentTypes;
import me.jackz.pocket_storage.registry.RegistryBlocks;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.util.LevelLocationAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static me.jackz.pocket_storage.util.Structure.buildBox;

public class StorageNode {
    private StorageNodeData data;
    private UUID id;
    public StorageNode(UUID nodeId, StorageNodeData data) {
        this.id = nodeId;
        this.data = data;
    }

    public BlockPos getCorner() {
        return data.cornerPos;
    }

    public BlockPos getBlockCenter() {
        return getBlockBottomCenter().relative(Direction.UP, RegionStorage.SIZE[1] / 2);
    }

    public BlockPos getBlockBottomCenter() {
        int x = data.cornerPos.getX() + (RegionStorage.SIZE[0] / 2);
        int y = data.cornerPos.getY();
        int z = data.cornerPos.getZ() + (RegionStorage.SIZE[2] / 2);
        return new BlockPos(x, y, z);
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
        player.setData(RegistryAttachmentTypes.LAST_LOCATION, attach);
        player.setData(RegistryAttachmentTypes.NODE_ID, id);

        Vec3 pos = findSafeSpawn(player);
        StorageDimTransition.enterStorageDimension(player, pos);

        player.sendSystemMessage(Component.literal("Sneak near the center to exit"));
    }

    public Vec3 findSafeSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos.MutableBlockPos cursor = getBlockBottomCenter().mutable();
        int yMin = data.cornerPos.getY();
        int yMax = yMin + RegionStorage.SIZE[1];
        for (int y = yMin; y < yMax; y++) {
            cursor.setY(y);
            // Use vanilla noCollision for the 2-tall player box
            AABB playerBox = player.getDimensions(Pose.STANDING).makeBoundingBox(cursor.getCenter());
            if (level.noCollision(playerBox)) {
                // Check there's something to stand on below
                cursor.below();
                if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                    return cursor.above().getCenter();
                }
            }
        }
        return getBlockBottomCenter().getCenter();
    }

    public static boolean restorePlayer(ServerPlayer player) {
        LevelLocationAttachment attachment = LevelLocationAttachment.fromPlayerPosition(player);
        player.removeData(RegistryAttachmentTypes.NODE_ID);
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
        BlockPos btmCenter = getBlockBottomCenter().above();
        // Add exit chest
        Block block = RegistryBlocks.POCKET_CHEST.get();
        level.setBlock(btmCenter.above(), block.defaultBlockState(), Block.UPDATE_NONE);

        BlockState wallBlock = Blocks.BEDROCK.defaultBlockState();
        buildBox(level, data.cornerPos, RegionStorage.SIZE[0], RegionStorage.SIZE[1], RegionStorage.SIZE[2], wallBlock, null);

        BlockState lightBlock = Blocks.TORCH.defaultBlockState();
        level.setBlock(btmCenter.east(), lightBlock, Block.UPDATE_NONE);
        level.setBlock(btmCenter.west(), lightBlock, Block.UPDATE_NONE);
        level.setBlock(btmCenter.north(), lightBlock, Block.UPDATE_NONE);
        level.setBlock(btmCenter.south(), lightBlock, Block.UPDATE_NONE);


        // for debug
        BlockState test = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:glowstone"))
                .defaultBlockState();
        level.setBlock(data.cornerPos, test, Block.UPDATE_NONE);
    }


}
