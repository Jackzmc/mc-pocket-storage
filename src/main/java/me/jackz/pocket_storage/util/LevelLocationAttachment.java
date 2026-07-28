package me.jackz.pocket_storage.util;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.StorageDimTransition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public class LevelLocationAttachment {
    public ResourceKey<Level> dim;
    public BlockPos lastPos;          // BlockPos.asLong()
    public Vec3 lastAng;

    public static LevelLocationAttachment fromPlayerPosition(ServerPlayer player) {
        LevelLocationAttachment att = new LevelLocationAttachment();
        att.dim = player.level().dimension();
        att.lastPos = player.getBlockPosBelowThatAffectsMyMovement().above();
        att.lastAng = player.getLookAngle();
        return att;
    }

    public boolean tryRestore(ServerPlayer player) {
        if(dim == null) return false;
        Vec3 pos = lastPos.getCenter();
        Pocket_storage.LOGGER.debug("attempting restore. target={} in={}", dim, player.level().dimension());
        if(dim != player.level().dimension()) {
            ServerLevel targetLvl = player.getServer().getLevel(dim);
            if(targetLvl == null) {
                Pocket_storage.LOGGER.warn("Attempted to restore player to invalid dimension ({})", dim);
                return false;
            }
            player.changeDimension(StorageDimTransition.to(targetLvl, pos, lastAng));
            Pocket_storage.LOGGER.debug("teleporting to {} in {}", pos, targetLvl);
        } else {
            Pocket_storage.LOGGER.debug("player in same dimension - just teleporting. {}", pos);
            player.teleportTo(pos.x, pos.y, pos.z);
        }
        return true;
    }

    public static class LocationSerializer implements IAttachmentSerializer<CompoundTag, LevelLocationAttachment> {

        @Override
        public LevelLocationAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            LevelLocationAttachment att = new LevelLocationAttachment();
            att.dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dim")));
            att.lastPos = BlockPos.containing(LevelLocationAttachment.getVec(tag, "pos"));
            att.lastAng = LevelLocationAttachment.getVec(tag, "ang");
            return att;
        }

        @Override
        public CompoundTag write(LevelLocationAttachment att, HolderLookup.Provider provider) {
            if (att.dim == null) {
                Pocket_storage.LOGGER.warn("not saving invalid last location attachment (dim is null)");
                return null;  // skip writing if uninitialized
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("dim", att.dim.location().toString());
            LevelLocationAttachment.putVec(tag, "pos", att.lastPos.getCenter());
            LevelLocationAttachment.putVec(tag, "ang", att.lastAng);
            Pocket_storage.LOGGER.debug("saving pos={} {}", att.lastPos, att.lastPos.getCenter());
            return tag;
        }
    }

    public static ListTag putVec(CompoundTag tag, String key, Vec3 vec) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(vec.x));
        list.add(DoubleTag.valueOf(vec.y));
        list.add(DoubleTag.valueOf(vec.z));
        tag.put(key, list);
        return list;
    }

    public static Vec3 getVec(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, DoubleTag.TAG_DOUBLE);
        return new Vec3(
            list.getDouble(0),
            list.getDouble(1),
            list.getDouble(2)
        );
    }
}
