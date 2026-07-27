package me.jackz.pocket_storage.registry;

import me.jackz.pocket_storage.blocks.PocketChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class RegistryBlocks {
    private static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> POCKET_CHEST = REGISTER.registerBlock(
            "pocket_chest",
            (props) -> new PocketChestBlock(props
                    .mapColor(MapColor.WOOD)
                    .destroyTime(4.0f)
                    .explosionResistance(1000.0f)
                    .sound(SoundType.WOOD)
                    .isValidSpawn((blk, get, pos, type) -> false)
            )
    );
    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
