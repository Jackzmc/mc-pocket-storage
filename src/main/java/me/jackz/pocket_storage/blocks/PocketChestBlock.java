package me.jackz.pocket_storage.blocks;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import me.jackz.pocket_storage.Config;
import me.jackz.pocket_storage.Pocket_storage;
import me.jackz.pocket_storage.dim.RegionStorage;
import me.jackz.pocket_storage.dim.StorageNode;
import me.jackz.pocket_storage.registry.RegistryComponents;
import me.jackz.pocket_storage.registry.RegistryDims;
import me.jackz.pocket_storage.registry.RegistryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.core.Direction.NORTH;

public class PocketChestBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 3.0D, 14.0D, 11.0D, 13.0D);
    public static final MapCodec<PocketChestBlock> CODEC = simpleCodec(p -> new PocketChestBlock(p));

    public PocketChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, NORTH)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        // always drop item, even in creative:
        if(!level.isClientSide) {
            ItemStack cloneItemStack = getCloneItemStack(level, pos, state);
            popResource(level, pos, cloneItemStack);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    public static ItemStack getChestItem(@Nullable StorageNode node) {
        ItemStack item = new ItemStack(RegistryItems.POCKET_CHEST_ITEM.asItem());
        if (node != null) {
            item.set(RegistryComponents.NODE_ID, node.getId().toString());
            // Set lore
            Optional<GameProfile> profile = node.getOwnerProfile();
            Style style = Style.EMPTY
                .withItalic(false)
                .withColor(ChatFormatting.GRAY);

            List<Component> loreItems = new ArrayList<>();
            profile.ifPresent(gameProfile -> loreItems.add(
                Component.literal(gameProfile.getName())
                    .withStyle(style.withColor(ChatFormatting.GOLD))
                    .append("'s chest")
            ));
            loreItems.add(Component.literal(node.getId().toString()).withStyle(style));
            loreItems.add(Component.literal(node.getInnerSizeString()).withStyle(style));

            ItemLore lore = new ItemLore(loreItems);
            item.set(DataComponents.LORE, lore);
        }
        return item;
    }

    @Override
    @NotNull
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if(level.isClientSide()) return PocketChestBlock.getChestItem(null);
        StorageNode node = null;
        PocketChestBlockEntity blockEntity = getBlockEntity(level, pos);
        if(blockEntity != null) {
            RegionStorage store = RegionStorage.get((ServerLevel) level);
            node = store.getNode(blockEntity.getNodeId());
        }
        return PocketChestBlock.getChestItem(node);
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
                    // If item has a specific node its tied to, place that ID instead of generating a new node
                    node = store.getNode(UUID.fromString(id));
                    if(node == null) {
                        player.sendSystemMessage(Component.literal("Node ID is invalid").withColor(Color.RED.getRGB()));
                        return;
                    }
                } else {
                    // Attempt to create a node
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
    @NotNull
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null || player.isCrouching())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        ServerPlayer sp = (ServerPlayer) player;

        if(level.dimension() == RegistryDims.STORAGE_DIM) {
            // Inside dimension, always just return home
            StorageNode.restorePlayer(sp);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return ItemInteractionResult.SUCCESS;
        }

        boolean isUsingTool = stack.is(RegistryItems.POCKET_TOOL);
        // Outside dimension, check owner and teleport to
        PocketChestBlockEntity ent = getBlockEntity(level, pos);
        if(ent != null) {
            ent.check();
            RegionStorage store = RegionStorage.get((ServerLevel) level);
            StorageNode node = store.getNode(ent.getNodeId());
            if(isUsingTool) {
                StorageNode.inspectNode(sp, node);
            } else if(node != null) {
                Pocket_storage.LOGGER.debug("node {}. teleporting", node.getId());
                node.teleportPlayerTo(sp);
                level.playSound(sp, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                sp.playSound(SoundEvents.CHEST_OPEN, 1.0f, 1.0f);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }


    @Override
    @NotNull
    protected List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder params) {
        // We handle dropping manually - drop nothing
        return List.of();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
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
    public void tick(@NotNull BlockState pState, @NotNull ServerLevel pLevel, @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        // Ignore our own dim - it's just an exit
        if(pLevel.dimension() == RegistryDims.STORAGE_DIM) return;

        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof PocketChestBlockEntity chest) {
            chest.check();
        }
    }
}
