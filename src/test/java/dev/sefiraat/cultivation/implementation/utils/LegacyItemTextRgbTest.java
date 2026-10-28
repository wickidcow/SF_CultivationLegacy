package dev.sefiraat.cultivation.implementation.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyItemTextRgbTest {
    private static final String RGB = "\u00a7x\u00a7a\u00a72\u00a74\u00a71\u00a7b\u00a7f";

    @Test
    void prefixDoesNotDownsamplePlantColor() {
        Component name = LegacyItemText.parse(RGB + "Magic Seed");
        Component renamed = LegacyItemText.prepend("\u00a7eStored Seed Type: ", name);
        assertEquals(LegacyItemText.parse("\u00a7eStored Seed Type: " + RGB + "Magic Seed"), renamed);
    }

    @Test
    void placeholderKeepsTemplateAndInsertedHexColors() {
        String other = "\u00a7x\u00a71\u00a7d\u00a73\u00a76\u00a7c\u00a74";
        List<Component> original = List.of(LegacyItemText.parse(RGB + "Breed: {0}"));
        List<Component> changed = LegacyItemText.replaceLiteral(original, "{0}", other + "Blue Seed");
        assertEquals(LegacyItemText.parse(RGB + "Breed: " + other + "Blue Seed"), changed.getFirst());
        assertEquals(LegacyItemText.parse(RGB + "Breed: {0}"), original.getFirst());
    }

    @Test
    void serializerEmitsFullRgbInsteadOfNearestNamedColor() {
        String serialized = LegacyItemText.serializer().serialize(Component.text("Seed", TextColor.color(0xa241bf)));
        assertTrue(serialized.startsWith(RGB), serialized);
    }
}
