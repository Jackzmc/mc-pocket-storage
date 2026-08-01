package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryAttachmentTypes;
import me.jackz.pocket_storage.registry.RegistryBlocks;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.util.LevelLocationAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

import static me.jackz.pocket_storage.Pocket_storage.LOGGER;
import static me.jackz.pocket_storage.Pocket_storage.MODID;
import static me.jackz.pocket_storage.util.Structure.buildBox;

public class StorageNode {
    private StorageNodeData data;
    private StorageNode(StorageNodeData data) {
        this.data = data;
    }


    public static StorageNode fromData(StorageNodeData data) {
        return new StorageNode(data);
    }

    public StorageNodeData getData() {
        return data;
    }

    public static StorageNode create(UUID nodeId, ServerLevel level, ServerPlayer owner, ResourceLocation templateId, BlockPos cornerPos) {
        StructureTemplate template = RegionStorage.resolveTemplate(level, templateId);
        if(template == null) throw new IllegalArgumentException("template could not be resolved");

        StorageNodeData data = new StorageNodeData(nodeId, owner.getUUID(), cornerPos, template.getSize(), templateId);
        StorageNode node = new StorageNode(data);

        node.createStructure(level, template);

        Pocket_storage.LOGGER.info("Created new node {} owned by {} at {}", nodeId, owner.getUUID(), cornerPos);

        return node;
    }

    public BlockPos getCorner() {
        return data.cornerPos;
    }

    public BlockPos getBlockCenter() {
        return getBlockBottomCenter().relative(Direction.UP, getSize().getY() / 2);
    }

    public Vec3 getCenter() {
        Vec3i size = getSize();
        float x = (float)data.cornerPos.getX() + ((float)size.getX() / 2f);
        float y = data.cornerPos.getY();
        float z = data.cornerPos.getZ() + (size.getZ() / 2f);
        return new Vec3(x, y, z);
    }

    public BlockPos getBlockBottomCenter() {
        Vec3i size = getSize();
        int x = data.cornerPos.getX() + (size.getX() / 2);
        int y = data.cornerPos.getY();
        int z = data.cornerPos.getZ() + (size.getZ() / 2);
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
        Pocket_storage.LOGGER.debug("stored last loc {}, ang {}", attach.lastPos, attach.lastAng);
        player.setData(RegistryAttachmentTypes.LAST_LOCATION, attach);
        player.setData(RegistryAttachmentTypes.NODE_ID, this.getId());

        Vec3 pos = findSafeSpawn(player);
        StorageDimTransition.enterStorageDimension(player, pos);

        player.sendSystemMessage(Component.literal("Sneak near the center to exit"));
    }

    private Vec3 findSafeSpawn(ServerPlayer player) {
        ServerLevel level = player.getServer().getLevel(RegistryDims.STORAGE_DIM);
        BlockPos.MutableBlockPos cursor = getBlockBottomCenter().mutable();
        if(level == null) return cursor.getCenter();
        int yMin = data.cornerPos.getY();
        int yMax = yMin + getSize().getY();
        for (int y = yMax - 1; y > yMin; y--) {
            cursor.setY(y);
            if(!level.isInWorldBounds(cursor)) continue;
            Vec3 feetPos = new Vec3(cursor.getX() + 0.5, y, cursor.getZ() + 0.5);
            // Check there's something to stand on below
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                return feetPos.add(0, 1, 0);
            }
        }
        return new Vec3(cursor.getX() + 0.5, yMin + 1, cursor.getZ() + 0.5);
    }

    public static boolean restorePlayer(ServerPlayer player) {
        LevelLocationAttachment attachment = player.getData(RegistryAttachmentTypes.LAST_LOCATION);
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
        return data.id;
    }

    public UUID getOwnerId() {
        return data.ownerUUID;
    }

    public Vec3i getSize() {
        return data.size;
    }

    protected void createStructure(ServerLevel level, StructureTemplate template) {
        if(!applyRoomTemplate(template, level, data.cornerPos)) {
            Pocket_storage.LOGGER.warn("Failed to apply room template");
        }
        Vec3i size = template.getSize();

        BlockState wallBlock = Blocks.BEDROCK.defaultBlockState();
        buildBox(level, data.cornerPos, size.getX() + 2, size.getY() + 2, size.getZ() + 2, wallBlock, null);
        LOGGER.debug("bedbox corner={} size={}", data.cornerPos, size.offset(1, 1, 1));
//        BlockState lightBlock = Blocks.TORCH.defaultBlockState();
//        level.setBlock(btmCenter.east(), lightBlock, Block.UPDATE_NONE);
//        level.setBlock(btmCenter.west(), lightBlock, Block.UPDATE_NONE);
//        level.setBlock(btmCenter.north(), lightBlock, Block.UPDATE_NONE);
//        level.setBlock(btmCenter.south(), lightBlock, Block.UPDATE_NONE);
    }


    private static boolean applyRoomTemplate(StructureTemplate template, ServerLevel level, BlockPos origin) {
        Vec3i size = template.getSize();
        LOGGER.debug("template size={}", size);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true);
//                .setKnownShape(true);

        BlockPos innerOrigin = new BlockPos(origin.getX() + 1, origin.getY() + 1, origin.getZ() + 1);
        LOGGER.debug("innerOrigin={} SIZE={}", innerOrigin, size);
        return template.placeInWorld(level, innerOrigin, innerOrigin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
    }

}
