package me.jackz.pocket_storage.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.awt.*;

public class TextUtil {
    /**
     * Returns a text component with %key%: %value%, where value is light gray, clickable to copy value to clipboard
     */
    public static Component formatKeyValueComponent(String key, String value) {
        Component valueComponent = Component.literal(value)
            .withStyle(style -> style
                    .withColor(ChatFormatting.GRAY)
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.COPY_TO_CLIPBOARD,
                            value
                    ))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click to copy")
                    ))
            );
        return Component.literal(key)
                .append(": ")
                .append(valueComponent);
    }

    /**
     * Formats key value component and sends to player/source
     * @param source command source / player
     */
    public static void sendKeyValueComponent(CommandSourceStack source, String key, String value) {
        source.sendSystemMessage(formatKeyValueComponent(key, value));
    }
}
