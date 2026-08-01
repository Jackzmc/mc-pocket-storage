package me.jackz.pocket_storage.blocks;

import me.jackz.pocket_storage.Config;
import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageNode;
import me.jackz.pocket_storage.registry.RegistryComponents;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.registry.RegistryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class PocketChestBlock extends Block implements EntityBlock {
    public PocketChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState());

//        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    // TODO: move to diff method for survival to work
    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        if (player instanceof FakePlayer)
            return;
        if (world.isClientSide)
            return;
        if (world instanceof ServerLevel) {
            Pocket_storage.LOGGER.debug("popping");
            ItemStack cloneItemStack = getCloneItemStack(world, pos, state);
            world.destroyBlock(pos, false);
            popResource(world, pos, cloneItemStack);
        }
    }

    @Override
    @NotNull
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof FakePlayer) return null;
        if (level.isClientSide) return null;
        if (level instanceof ServerLevel) {
            Pocket_storage.LOGGER.debug("popping");
            ItemStack cloneItemStack = getCloneItemStack(level, pos, state);
            level.destroyBlock(pos, false);
            popResource(level, pos, cloneItemStack);
            return state;
        }
        return null;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player instanceof FakePlayer) return false;
        if (level.isClientSide) return false;
        if (level instanceof ServerLevel) {
            Pocket_storage.LOGGER.debug("popping");
//            ItemStack cloneItemStack = getCloneItemStack(level, pos, state);
//            level.destroyBlock(pos, false);
//            popResource(level, pos, cloneItemStack);
            return true;
        }
        return false;
    }

    public static ItemStack getChestItem(@Nullable UUID nodeId) {
        ItemStack item = new ItemStack(RegistryItems.POCKET_CHEST_ITEM.asItem());
        if (nodeId != null) {
            item.set(RegistryComponents.NODE_ID, nodeId.toString());

            ItemLore lore = new ItemLore(List.of(
                Component.literal("Node ").append(nodeId.toString())
            ));
            item.set(DataComponents.LORE, lore);
            Pocket_storage.LOGGER.debug("clone: set id {}",nodeId);
        }
        return item;
    }

    // TODO: static
    @Override
    @NotNull
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        UUID id = null;
        PocketChestBlockEntity blockEntity = getBlockEntity(level, pos);
        if(blockEntity != null) {
            RegionStorage store = RegionStorage.get((ServerLevel) level);
            StorageNode node = store.getNode(blockEntity.getNodeId());
            if(node != null) {
                id = node.getId();
            }
        }
        return PocketChestBlock.getChestItem(id);
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
                StorageNode node;
                String id = stack.get(RegistryComponents.NODE_ID);
                if(id != null) {
                    node = store.getNode(UUID.fromString(id));
                    if(node == null) {
                        player.sendSystemMessage(Component.literal("Node ID is invalid").withColor(Color.RED.getRGB()));
                        return;
                    }
                } else {
                    try {
                        node = store.createNode(player, Config.DefaultStructureTemplate);
                    } catch(Exception e) {
                        player.sendSystemMessage(
                            Component.literal("Unable to create room: ")
                                .append(e.toString())
                                .withColor(Color.RED.getRGB())
                        );
                        return;
                    }
                }
                if(node != null) ent.setNode(node);
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
                        sp.sendSystemMessage(Component.literal("Size: ").append(node.getSize().toShortString()));
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
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack item = new ItemStack(this.asItem(), 1);

        return List.of(item);
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
