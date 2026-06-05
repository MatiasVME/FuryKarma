package furykarma.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtils {

    private MessageUtils() {
        // Private constructor to prevent instantiation
    }

    private static final java.util.regex.Pattern LEGACY_PATTERN = 
            java.util.regex.Pattern.compile("(&|§)(#[0-9a-fA-F]{6}|[0-9a-fA-Fk-oK-OrR])");

    /**
     * Translates a string message into a formatted Adventure Component.
     * Supports both modern MiniMessage tags and classic legacy color codes.
     *
     * @param input The raw input string.
     * @return The formatted Component.
     */
    public static Component format(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        
        // If it contains legacy color codes (e.g. &e or §a), parse as legacy.
        if (LEGACY_PATTERN.matcher(input).find()) {
            String translated = input.replace('§', '&');
            return LegacyComponentSerializer.legacyAmpersand().deserialize(translated);
        }
        
        // If the string contains tags that look like MiniMessage, deserialize it using MiniMessage.
        // Otherwise, fallback to translating legacy & color codes.
        if (input.contains("<") && input.contains(">")) {
            return MiniMessage.miniMessage().deserialize(input);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
    }
}
