package dev.sefiraat.cultivation.api.utils;

import dev.sefiraat.sefilib.string.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CultivationThemesTest {

    @ParameterizedTest(name = "Hex color {1} is preserved")
    @MethodSource("palette")
    void preservesTheExistingLegacyHexColors(Theme theme, String hex) {
        assertEquals(legacyHex(hex), theme.toString());
    }

    private static Stream<Arguments> palette() {
        return Stream.of(
            arguments(CultivationThemes.MAIN, "#8a0e0e"),
            arguments(CultivationThemes.DISCOVEREY, "#a60e03"),
            arguments(CultivationThemes.CRAFTING, "#dbcea9"),
            arguments(CultivationThemes.RECIPE_TYPE, "#ffe89c"),
            arguments(CultivationThemes.MACHINE, "#3295a8"),
            arguments(CultivationThemes.TOOL, "#3295a8"),
            arguments(CultivationThemes.PLANT, "#a241bf"),
            arguments(CultivationThemes.BUSH, "#a241bf"),
            arguments(CultivationThemes.SAPLING, "#a241bf"),
            arguments(CultivationThemes.PRODUCE, "#ccd123"),
            arguments(CultivationThemes.BY_PRODUCT, "#ccd123"),
            arguments(CultivationThemes.FOOD, "#ccd123"),
            arguments(CultivationThemes.SEED_RED, "#c41d1d"),
            arguments(CultivationThemes.SEED_ORANGE, "#c4761d"),
            arguments(CultivationThemes.SEED_YELLOW, "#c4b31d"),
            arguments(CultivationThemes.SEED_GREEN, "#4ac41d"),
            arguments(CultivationThemes.SEED_BLUE, "#1d36c4"),
            arguments(CultivationThemes.SEED_CYAN, "#1fcccc"),
            arguments(CultivationThemes.SEED_PURPLE, "#811dc4")
        );
    }

    @Test
    void preservesExistingThemeLabelsAndPublicDiscoverySpelling() {
        assertEquals("Cultivation", CultivationThemes.MAIN.getLoreLine());
        assertEquals("Discovery", CultivationThemes.DISCOVEREY.getLoreLine());
        assertEquals("By-Products", CultivationThemes.BY_PRODUCT.getLoreLine());
        assertEquals("", CultivationThemes.SEED_GREEN.getLoreLine());
    }

    private static String legacyHex(String hex) {
        StringBuilder result = new StringBuilder("\u00a7x");
        for (int i = 1; i < hex.length(); i++) {
            result.append('\u00a7').append(hex.charAt(i));
        }
        return result.toString();
    }
}
