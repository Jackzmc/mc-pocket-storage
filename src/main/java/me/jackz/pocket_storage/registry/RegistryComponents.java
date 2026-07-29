package me.jackz.pocket_storage.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class RegistryComponents {
    private static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NODE_ID = REGISTER.registerComponentType(
            "node_id",
            builder -> builder
                    .persistent(Codec.STRING)
    );

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }

}
