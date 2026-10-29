package dev.sefiraat.cultivation.implementation.utils;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyItemTextTest {

    @Test
    void prefixRetainsExistingFormatting() {
        Component original = LegacyItemText.parse("\u00a7aMint \u00a7lSeed");
        Component renamed = LegacyItemText.prepend("\u00a7eStored Seed Type: ", original);

        assertEquals(LegacyItemText.parse("\u00a7eStored Seed Type: \u00a7aMint \u00a7lSeed"), renamed);
    }

    @Test
    void prefixHandlesMissingName() {
        assertEquals(LegacyItemText.parse("\u00a7eRemove Seed: "),
            LegacyItemText.prepend("\u00a7eRemove Seed: ", null));
    }

    @Test
    void replacementPreservesColorsAndDoesNotChangeOriginalLore() {
        List<Component> original = List.of(LegacyItemText.parse("\u00a77Breed: {0}"));
        List<Component> replaced = LegacyItemText.replaceLiteral(original, "{0}", "\u00a7bBlueberry");

        assertEquals(LegacyItemText.parse("\u00a77Breed: \u00a7bBlueberry"), replaced.getFirst());
        assertEquals(LegacyItemText.parse("\u00a77Breed: {0}"), original.getFirst());
    }
}
