package me.jackz.pocket_storage.util;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.StorageDimTransition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
    public long lastPos;          // BlockPos.asLong()
    public ResourceKey<Level> dim;

    public static LevelLocationAttachment fromPlayerPosition(ServerPlayer player) {
        LevelLocationAttachment att = new LevelLocationAttachment();
        att.dim = player.level().dimension();
        att.lastPos = player.getBlockPosBelowThatAffectsMyMovement().asLong();
        return att;
    }

    public boolean tryRestore(ServerPlayer player) {
        if(dim == null) return false;
        Vec3 pos = BlockPos.of(lastPos).getCenter();
        Pocket_storage.LOGGER.debug("attempting restore. target={} in={}", dim, player.level().dimension());
        if(dim != player.level().dimension()) {
            ServerLevel targetLvl = player.getServer().getLevel(player.level().dimension());
            if(targetLvl == null) {
                Pocket_storage.LOGGER.warn("Attempted to restore player to invalid dimension ({})", dim);
                return false;
            }
            player.changeDimension(StorageDimTransition.to(targetLvl, pos));
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
            att.lastPos = tag.getLong("pos");
            att.dim = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString("dim")));
            return att;
        }

        @Override
        public CompoundTag write(LevelLocationAttachment att, HolderLookup.Provider provider) {
            if (att.dim == null) return null;  // skip writing if uninitialized
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", att.lastPos);
            tag.putString("dim", att.dim.location().toString());
            return tag;
        }
    }
}
