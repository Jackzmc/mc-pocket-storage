package me.jackz.pocket_storage.registry;

import com.mojang.serialization.Codec;
import me.jackz.pocket_storage.util.LevelLocationAttachment;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

import static me.jackz.pocket_storage.Pocket_storage.MODID;

public class RegisterAttachmentTypes {
    private static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /**
     * Store the last location + dim of player before entering a pocket node.
     * Serialized in case of sudden shutdown, but player should be removed on quit
     */
    public static final Supplier<AttachmentType<LevelLocationAttachment>> LAST_LOCATION = REGISTER.register(
            "last_location",
            () -> AttachmentType.builder(LevelLocationAttachment::new)
                    .serialize(new LevelLocationAttachment.LocationSerializer())
                    .build());

    public static final Supplier<AttachmentType<UUID>> NODE_ID = REGISTER.register(
            "node_id",
            () -> AttachmentType.builder(() -> (UUID)null).build()
    );

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
