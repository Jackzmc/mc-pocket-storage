package me.jackz.pocket_storage.dim;

import me.jackz.pocket_storage.util.NBTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;
import java.util.UUID;

import static me.jackz.pocket_storage.Pocket_storage.LOGGER;

public class StorageNodeData {
    UUID id;
    BlockPos cornerPos;
    Vec3i size;
    UUID ownerUUID;
    ResourceLocation templateId;

    protected StorageNodeData(UUID id, UUID ownerUUID, BlockPos cornerPos, Vec3i size, ResourceLocation templateId) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.cornerPos = cornerPos;
        this.size = size;
        this.templateId = templateId;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ownerUUID", ownerUUID);
        NBTUtil.putBlockPos(tag, "origin", cornerPos);
        tag.putString("template", templateId.toString());
        NBTUtil.putVec3i(tag, "size", size);
        return tag;
    }

    public static StorageNodeData deserialize(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        UUID ownerUUID = tag.getUUID("ownerUUID");
        BlockPos pos = NBTUtil.getBlockPos(tag, "origin");
        String templateId = tag.getString("template");
        Vec3i size = NBTUtil.getVec3i(tag, "size");
        return new StorageNodeData(id, ownerUUID, pos, size, ResourceLocation.parse(templateId));
    }

}
