package me.jackz.pocket_storage.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class RegistryDims {
    public static final ResourceKey<Level> STORAGE_DIM =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(MODID, "pocket_storage_dim"));
}
