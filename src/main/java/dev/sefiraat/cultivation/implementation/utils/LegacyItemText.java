package dev.sefiraat.cultivation.implementation.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges existing SefiLib legacy-formatted text to Paper's Adventure item-meta API.
 * This only changes presentation; item IDs, persistent data and saved profiles stay untouched.
 */
public final class LegacyItemText {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private LegacyItemText() {
        throw new IllegalStateException("Utility class");
    }

    public static Component parse(String legacyText) {
        return LEGACY.deserialize(legacyText);
    }

    public static Component prepend(String legacyPrefix, Component existingName) {
        String previous = existingName == null ? "" : LEGACY.serialize(existingName);
        return parse(legacyPrefix + previous);
    }

    public static List<Component> replaceLiteral(List<Component> original, String target, String replacement) {
        List<Component> result = new ArrayList<>(original.size());
        for (Component line : original) {
            result.add(parse(LEGACY.serialize(line).replace(target, replacement)));
        }
        return result;
    }
}
