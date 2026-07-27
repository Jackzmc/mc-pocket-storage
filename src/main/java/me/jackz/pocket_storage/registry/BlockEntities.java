package me.jackz.pocket_storage.registry;

import me.jackz.pocket_storage.blocks.PocketChestBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class BlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PocketChestBlockEntity>> POCKET_CHEST = REGISTER
            .register("pocket_chest", () -> BlockEntityType.Builder.of(PocketChestBlockEntity::new, Blocks.POCKET_CHEST.get())
            .build(null));

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
