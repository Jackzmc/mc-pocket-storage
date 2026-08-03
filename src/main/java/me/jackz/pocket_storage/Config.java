package me.jackz.pocket_storage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

@EventBusSubscriber(modid = MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final String DEFAULT_TEMPLATE = "13x11x13_plain";
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_STRUCTURE_TEMPLATE = BUILDER
            .comment("Name of the structure to use as the template for the pocket chest")
            .define("structure_template", String.format("%s:%s", MODID, DEFAULT_TEMPLATE));

    public static ResourceLocation DefaultStructureTemplate;

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        DefaultStructureTemplate = ResourceLocation.parse(DEFAULT_STRUCTURE_TEMPLATE.get());
    }
}
