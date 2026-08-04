package me.jackz.pocket_storage.dim;

import com.mojang.authlib.GameProfile;
import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.registry.RegistryAttachmentTypes;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.util.LevelLocationAttachment;
import me.jackz.pocket_storage.util.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
import java.util.UUID;

import static me.jackz.pocket_storage.Pocket_storage.LOGGER;
import static me.jackz.pocket_storage.Pocket_storage.MODID;
import static me.jackz.pocket_storage.util.Structure.buildBox;

public class StorageNode {
    private final StorageNodeData data;
    private StorageNode(StorageNodeData data) {
        this.data = data;
    }

    public static StorageNode fromData(StorageNodeData data) {
        return new StorageNode(data);
    }

    public static void inspectNode(CommandSourceStack source, @Nullable StorageNode node) {
        if(node != null) {
            Optional<GameProfile> profile = node.getOwnerProfile();
            TextUtil.sendKeyValueComponent(source, "Node ID", node.getId().toString());
            TextUtil.sendKeyValueComponent(source, "Owner UUID", node.getOwnerId().toString());
            profile.ifPresent(gameProfile -> TextUtil.sendKeyValueComponent(source, "Owner", gameProfile.getName()));
            TextUtil.sendKeyValueComponent(source, "Center Pos", node.getBlockCenter().toShortString());
            TextUtil.sendKeyValueComponent(source, "Corner Pos", node.getCorner().toShortString());
            TextUtil.sendKeyValueComponent(source, "Size", node.getSizeString());
        } else {
            source.sendSystemMessage(Component.literal("No node was found").withColor(Color.RED.getRGB()));
        }
    }

    public static void inspectNode(ServerPlayer player, @Nullable StorageNode node) {
        inspectNode(player.createCommandSourceStack(), node);
    }

    public StorageNodeData getData() {
        return data;
    }

    /**
     * Check if a given position is inside the node. Ignores Y position
     * @param pos the position
     * @return true if within bounds, false if not
     */
    public boolean isVecInNode(Vec3 pos) {
        BlockPos opposite = getOppositeCorner();
        // We ignore Y-height, just check horizontal
        return pos.x >= data.cornerPos.getX()
                && pos.z >= data.cornerPos.getZ()
                && pos.x <= opposite.getX()
                && pos.z <= opposite.getZ();
    }

    /**
     * Creates a new node at the specific corner, applying the supplied template
     * @param nodeId id of the node
     * @param level world to create in
     * @param owner player who owns the node
     * @param templateId ID of the template to apply and calculate size from
     * @param cornerPos origin point, template applied towards +x +z
     * @return the new storage node
     */
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

    /**
     * Returns the opposite corner of box, with the highest Y height
     */
    public BlockPos getOppositeCorner() {
        Vec3i size = getSize();
        return data.cornerPos.offset(size.getX(), size.getY(), size.getZ());
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
        player.setData(RegistryAttachmentTypes.LAST_LOCATION, attach);
        player.setData(RegistryAttachmentTypes.ACTIVE_NODE_ID, this.getId());

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
        if(player.level().dimension() != RegistryDims.STORAGE_DIM) return false;

        LevelLocationAttachment attachment = player.getData(RegistryAttachmentTypes.LAST_LOCATION);
        player.removeData(RegistryAttachmentTypes.ACTIVE_NODE_ID);
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

    public Optional<GameProfile> getOwnerProfile() {
        return ServerLifecycleHooks.getCurrentServer()
                .getProfileCache()
                .get(data.ownerUUID);
    }

    public Vec3i getSize() {
        return data.size;
    }
    public String getSizeString() {
        return String.format("%dx%dx%d", data.size.getX(), data.size.getY(),  data.size.getZ());
    }

    protected void createStructure(ServerLevel level, StructureTemplate template) {
        if(!applyRoomTemplate(template, level, data.cornerPos)) {
            Pocket_storage.LOGGER.warn("Failed to apply room template");
        }
        Vec3i size = template.getSize();

        BlockState wallBlock = Blocks.BEDROCK.defaultBlockState();
        buildBox(level, data.cornerPos, size.getX() + 2, size.getY() + 2, size.getZ() + 2, wallBlock, null);
        LOGGER.debug("bedbox corner={} size={}", data.cornerPos, size.offset(1, 1, 1));
        String version = ModList.get().getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        placeSign(level, data.cornerPos.north(), new Component[] {
            Component.literal(data.id.toString().substring(0, 13)),
            Component.literal(data.ownerUUID.toString().substring(0, 13)),
            Component.literal(data.size.toShortString()),
            Component.literal(version)
        });
    }

    private void placeSign(ServerLevel level, BlockPos pos, Component[] lines) {
        BlockState state = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        level.getBlockEntity(pos, BlockEntityType.SIGN).ifPresent(sign -> {
            sign.updateText(text -> {
                for(int i = 0; i < lines.length ; i++) {
                    text = text.setMessage(i, lines[i]);
                }
                return text;
            }, true);
        });
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
