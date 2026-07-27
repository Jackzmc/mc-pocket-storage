package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PocketChestBlock extends Block implements EntityBlock {
    public PocketChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState());

//        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isClientSide)
            return;
        if (stack == null)
            return;
        if((placer instanceof Player player)) {
            PocketChestBlockEntity ent = getBlockEntity(worldIn, pos);
            if(ent != null) {
                ent.setOwner(player);
                Pocket_storage.LOGGER.debug("set owner to {}", player.getUUID());
            }
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null || player.isCrouching())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        PocketChestBlockEntity ent = getBlockEntity(level, pos);
        if(ent != null) {
            UUID ownerId = player.getUUID();
            Pocket_storage.LOGGER.debug("chest owner is  = {}", ownerId);
        }
//        Pocket_storage.LOGGER.debug("item used on chest block. owner id = {}", this.ownerPlayer.getUUID());

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PocketChestBlockEntity(blockPos, blockState);
    }

    @Nullable
    private PocketChestBlockEntity getBlockEntity(BlockGetter worldIn, BlockPos pos) {
        BlockEntity blockEntity = worldIn.getBlockEntity(pos);
        if(blockEntity instanceof PocketChestBlockEntity pe){
            return pe;
        }
        return null;
    }
}
