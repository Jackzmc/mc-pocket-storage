package me.jackz.pocket_storage.registry;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class RegistryItems {
    private static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(MODID);

    // Creates a new BlockItem with the id "pocket_storage:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> POCKET_CHEST_ITEM = REGISTER
            .registerSimpleBlockItem("pocket_chest", RegistryBlocks.POCKET_CHEST);

    // Creates a new food item with the id "pocket_storage:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> POCKET_TOOL = REGISTER
            .registerSimpleItem("pocket_tool", new Item.Properties().stacksTo(1));

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
