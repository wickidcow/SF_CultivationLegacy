package dev.sefiraat.cultivation.implementation.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** Existing legacy text remains a boundary format; RGB colors must never be downsampled. */
public final class LegacyItemText {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private LegacyItemText() {
        throw new IllegalStateException("Utility class");
    }

    public static LegacyComponentSerializer serializer() {
        return LEGACY;
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
