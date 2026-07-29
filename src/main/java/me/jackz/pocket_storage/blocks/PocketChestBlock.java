package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageDimTransition;
import me.jackz.pocket_storage.dim.StorageNode;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.registry.RegistryItems;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ColorRGBA;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
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
        if((placer instanceof ServerPlayer player) && worldIn instanceof ServerLevel sLevel) {
            PocketChestBlockEntity ent = getBlockEntity(worldIn, pos);
            if(ent != null) {
                RegionStorage store = RegionStorage.get(sLevel);
                StorageNode node = store.createNode(player, RegionStorage.TEMPLATE_ROOM_20x20x20_PLAIN);
                ent.setNode(node);
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

        ServerPlayer sp = (ServerPlayer) player;
        boolean isUsingTool = stack.is(RegistryItems.POCKET_TOOL);

        if(level.dimension() == RegistryDims.STORAGE_DIM) {
            // Inside dimension, always just return home
            StorageNode.restorePlayer(sp);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return ItemInteractionResult.SUCCESS;
        } else {
            // Outside dimension, check owner and teleport to
            PocketChestBlockEntity ent = getBlockEntity(level, pos);
            if(ent != null) {
                ent.check();
                RegionStorage store = RegionStorage.get((ServerLevel) level);
                StorageNode node = store.getNode(ent.getNodeId());
                if(isUsingTool) {
                    if(node != null) {
                        sp.sendSystemMessage(Component.literal("Node ID: ").append(node.getId().toString()));
                        sp.sendSystemMessage(Component.literal("Owner UUID: ").append(node.getOwnerId().toString()));
                        sp.sendSystemMessage(Component.literal("Center Pos: ").append(node.getBlockCenter().toShortString()));
                        sp.sendSystemMessage(Component.literal("Corner Pos: ").append(node.getCorner().toShortString()));
                        sp.sendSystemMessage(Component.literal("Size: ").append(Arrays.toString(RegionStorage.SIZE)));
                    } else {
                        sp.sendSystemMessage(Component.literal("No node attached").withColor(Color.RED.getRGB()));
                    }
                } else if(node != null) {
                    Pocket_storage.LOGGER.debug("node {}. teleporting", node.getId());
                    node.teleportPlayerTo(sp);
                    level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
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
        // Ignore our own dim - it's just an exit
        if(pLevel.dimension() == RegistryDims.STORAGE_DIM) return;

        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof PocketChestBlockEntity chest) {
            chest.check();
        }
    }
}
