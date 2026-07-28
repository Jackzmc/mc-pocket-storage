package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

import static me.jackz.pocket_storage.registry.RegistryBlockEntities.POCKET_CHEST;

public class PocketChestBlockEntity extends BlockEntity {
    private @Nullable UUID ownerId;
    private static String TAG_OWNER_ID = "ownerId";

    public PocketChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(POCKET_CHEST.get(), pos, blockState);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    protected void checkOwner() {
        if(ownerId == null) {
            Pocket_storage.LOGGER.error("Removing invalid pocket chest at {} that has no owner", this.worldPosition);
            if(this.level != null) this.level.destroyBlock(worldPosition, true);
        }
    }

    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.loadAdditional(compound, provider);
        if (compound.contains(TAG_OWNER_ID)) {
            this.ownerId = UUID.fromString(compound.getString(TAG_OWNER_ID));
        } else {
            checkOwner();
        }
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        if(ownerId != null)
            compound.putString(TAG_OWNER_ID, ownerId.toString());
    }
}
