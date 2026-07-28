package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.StorageNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

import static me.jackz.pocket_storage.registry.RegistryBlockEntities.POCKET_CHEST;

public class PocketChestBlockEntity extends BlockEntity {
    private @Nullable UUID nodeId;
    private static final String TAG_NODE_ID = "nodeId";

    public PocketChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(POCKET_CHEST.get(), pos, blockState);
    }

    @Nullable
    public UUID getNodeId() {
        return nodeId;
    }

    protected void check() {
        if(nodeId == null) {
            Pocket_storage.LOGGER.error("Removing invalid pocket chest at {} in {} that has no node", this.worldPosition, this.level);
            if(this.level != null) this.level.destroyBlock(worldPosition, true);
        }
    }

    public void setNode(StorageNode node) {
        this.nodeId = node.getId();
        setChanged();
        Pocket_storage.LOGGER.debug("set node {} on blockEntity {}", nodeId, worldPosition);
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.loadAdditional(compound, provider);
        if (compound.contains(TAG_NODE_ID)) {
            this.nodeId = UUID.fromString(compound.getString(TAG_NODE_ID));
        }
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        if(nodeId != null) {
            Pocket_storage.LOGGER.debug("saving node {} on blockEntity {}", nodeId, worldPosition);
            compound.putString(TAG_NODE_ID, nodeId.toString());
        }
    }
}
