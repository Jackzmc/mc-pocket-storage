package me.jackz.pocket_storage.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TextUtil {
    public static Component formatKeyValue(String key, String value) {
        Component valueComponent = Component.literal(value)
            .withStyle(style -> style
                    .withColor(ChatFormatting.GRAY)
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.COPY_TO_CLIPBOARD,
                            value
                    ))
            );
        return Component.literal(key)
                .append(": ")
                .append(valueComponent);
    }

    public static void sendKeyValueComponent(CommandSourceStack source, String key, String value) {
        source.sendSystemMessage(formatKeyValue(key, value));
    }
}
