package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import static me.jackz.pocket_storage.registry.RegistryBlockEntities.POCKET_CHEST;

public class PocketChestBlockEntity extends BlockEntity {
    private UUID ownerId;

    public PocketChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(POCKET_CHEST.get(), pos, blockState);
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwner(Player player) {
        this.ownerId = player.getUUID();
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.loadAdditional(compound, provider);
        this.ownerId = UUID.fromString(compound.getString("ownerId"));
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        compound.putString("ownerId", ownerId.toString());
    }
}
