package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.StorageDimTransition;
import me.jackz.pocket_storage.dim.StorageManager;
import me.jackz.pocket_storage.registry.RegistryDims;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class PocketChestBlock extends Block implements EntityBlock {
    public PocketChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState());

//        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (worldIn.isClientSide) return;
        if (worldIn.dimension() == RegistryDims.STORAGE_DIM) return;
        // Set ID of owner before it's placed in world
        if((placer instanceof Player player)) {
            PocketChestBlockEntity ent = getBlockEntity(worldIn, pos);
            if(ent != null) {
                ent.setOwner(player);
                Pocket_storage.LOGGER.debug("set owner to {}", player.getUUID());
            }
        }
        super.setPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null || player.isCrouching())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if(level.dimension() == RegistryDims.STORAGE_DIM) {
            // Inside dimension, always just return home
            // TODO: REMOVE, TEMP
            StorageDimTransition.enterOverworld((ServerPlayer) player, Vec3.ZERO);
            return ItemInteractionResult.FAIL;
        } else {
            // Outside dimension, check owner and teleport to

            PocketChestBlockEntity ent = getBlockEntity(level, pos);
            if(ent != null) {
                ent.checkOwner();
                UUID ownerId = ent.getOwnerId();
                if(ownerId != null) {
                    Pocket_storage.LOGGER.debug("chest owner {}. teleporting", ownerId);
                    StorageDimTransition.enterStorageDimension((ServerPlayer) player, player.getPosition(0));
                }
            }
        }

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


    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof PocketChestBlockEntity chest) {
            chest.checkOwner();
        }
    }
}
