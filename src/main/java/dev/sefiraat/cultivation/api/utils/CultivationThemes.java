package dev.sefiraat.cultivation.api.utils;

import dev.sefiraat.sefilib.string.Theme;
import net.kyori.adventure.text.format.TextColor;

public final class CultivationThemes {

    private CultivationThemes() {
        throw new IllegalStateException("Utility class");
    }

    public static final Theme MAIN = new Theme(TextColor.color(0x8a0e0e), "Cultivation");
    public static final Theme DISCOVEREY = new Theme(TextColor.color(0xa60e03), "Discovery");
    public static final Theme CRAFTING = new Theme(TextColor.color(0xdbcea9), "Crafting Material");
    public static final Theme RECIPE_TYPE = new Theme(TextColor.color(0xffe89c), "Recipe Type");
    public static final Theme MACHINE = new Theme(TextColor.color(0x3295a8), "Machine");
    public static final Theme TOOL = new Theme(TextColor.color(0x3295a8), "Tool");
    public static final Theme PLANT = new Theme(TextColor.color(0xa241bf), "Plant");
    public static final Theme BUSH = new Theme(TextColor.color(0xa241bf), "Bush");
    public static final Theme SAPLING = new Theme(TextColor.color(0xa241bf), "Sapling");
    public static final Theme PRODUCE = new Theme(TextColor.color(0xccd123), "Produce");
    public static final Theme BY_PRODUCT = new Theme(TextColor.color(0xccd123), "By-Products");
    public static final Theme FOOD = new Theme(TextColor.color(0xccd123), "Food");

    // Keep the public names, types and all existing RGB values unchanged.
    public static final Theme SEED_RED = new Theme(TextColor.color(0xc41d1d));
    public static final Theme SEED_ORANGE = new Theme(TextColor.color(0xc4761d));
    public static final Theme SEED_YELLOW = new Theme(TextColor.color(0xc4b31d));
    public static final Theme SEED_GREEN = new Theme(TextColor.color(0x4ac41d));
    public static final Theme SEED_BLUE = new Theme(TextColor.color(0x1d36c4));
    public static final Theme SEED_CYAN = new Theme(TextColor.color(0x1fcccc));
    public static final Theme SEED_PURPLE = new Theme(TextColor.color(0x811dc4));
}
