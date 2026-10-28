/*
 * Adapted from SefiLib 0.3.0 by Sefiraat, GPL-3.0.
 * Upstream: Sefiraat/SefiLib, Theme.java blob ac114ae0244f1f59edd0434b259a68985617eb61.
 * Modified for Cultivation on 2026-10-28: Adventure color storage with the original
 * public binary interface retained. See THIRD_PARTY_NOTICES.txt and LICENSE.
 */
package dev.sefiraat.sefilib.string;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * SefiLib's binary-compatible theme facade, backed by Adventure colors.
 *
 * <p>Do not relocate or change existing public descriptors: Cultivation exposes
 * this type in its public API. Only the three explicitly deprecated compatibility
 * members below reference Bungee types; new code uses TextColor or legacyCode().
 * No warning suppression or reflective color lookup is used in production.
 */
public class Theme {
    public static final Theme WARNING = new Theme(NamedTextColor.YELLOW, "Warning");
    public static final Theme ERROR = new Theme(NamedTextColor.RED, "Error");
    public static final Theme NOTICE = new Theme(NamedTextColor.WHITE, "Notice");
    public static final Theme PASSIVE = new Theme(NamedTextColor.GRAY);
    public static final Theme SUCCESS = new Theme(NamedTextColor.GREEN, "Success");
    public static final Theme CLICK_INFO = new Theme(TextColor.color(0xe4ed32), "Click here");

    private final TextColor textColor;
    private final String legacyCode;
    private final String loreLine;

    public Theme(@Nonnull TextColor color) {
        this(color, "");
    }

    public Theme(@Nonnull TextColor color, @Nonnull String loreLine) {
        this.textColor = Objects.requireNonNull(color, "color");
        this.loreLine = Objects.requireNonNull(loreLine, "loreLine");
        if (color instanceof NamedTextColor) {
            String encoded = LegacyComponentSerializer.legacySection().serialize(Component.text("_", color));
            this.legacyCode = encoded.substring(0, encoded.length() - 1);
        } else {
            StringBuilder encoded = new StringBuilder("\u00a7x");
            for (char digit : color.asHexString().substring(1).toCharArray()) {
                encoded.append('\u00a7').append(digit);
            }
            this.legacyCode = encoded.toString();
        }
    }

    /** @deprecated Binary compatibility only; new callers should pass TextColor. */
    @Deprecated(since = "1.1.24", forRemoval = false)
    public Theme(@Nonnull net.md_5.bungee.api.ChatColor color) {
        this(color, "");
    }

    /** @deprecated Binary compatibility only; new callers should pass TextColor. */
    @Deprecated(since = "1.1.24", forRemoval = false)
    public Theme(@Nonnull net.md_5.bungee.api.ChatColor color, @Nonnull String loreLine) {
        Objects.requireNonNull(color, "color");
        this.legacyCode = color.toString();
        // Legacy callers could pass formatting tokens such as BOLD or RESET.
        // Preserve their text behavior; such tokens did not have a dust color.
        this.textColor = color.getColor() == null ? null
            : TextColor.color(color.getColor().getRGB() & 0xffffff);
        this.loreLine = Objects.requireNonNull(loreLine, "loreLine");
    }

    /** @deprecated Retained for old compiled addons; prefer getTextColor() or legacyCode(). */
    @Deprecated(since = "1.1.24", forRemoval = false)
    @Nonnull
    public net.md_5.bungee.api.ChatColor getColor() {
        if (legacyCode.length() == 2) {
            return net.md_5.bungee.api.ChatColor.getByChar(legacyCode.charAt(1));
        }
        StringBuilder hex = new StringBuilder("#");
        for (int index = 3; index < legacyCode.length(); index += 2) {
            hex.append(legacyCode.charAt(index));
        }
        return net.md_5.bungee.api.ChatColor.of(hex.toString());
    }

    @Nonnull
    public TextColor getTextColor() {
        return Objects.requireNonNull(textColor, "A legacy formatting token is not an RGB color");
    }

    @Nonnull
    public String legacyCode() {
        return legacyCode;
    }

    public String color(@Nonnull String string) {
        return this + string;
    }

    @Nonnull
    public String apply(@Nonnull Object value) {
        return legacyCode + String.valueOf(value);
    }

    @Nonnull
    public String asTitle(@Nonnull Object value1, @Nonnull Object value2) {
        return legacyCode + String.valueOf(value1) + ": " + PASSIVE + value2;
    }

    @Nonnull
    public Particle.DustOptions getDustOptions(float size) {
        return new Particle.DustOptions(Color.fromRGB(getTextColor().value()), size);
    }

    @Override
    @Nonnull
    public String toString() {
        return legacyCode;
    }

    @Nonnull
    public String getLoreLine() {
        return loreLine;
    }

    @Nonnull
    public static String applyThemeToString(@Nonnull Theme themeType, @Nonnull Object value) {
        return themeType.legacyCode() + String.valueOf(value);
    }

    @Nonnull
    public static String applyThemeAsTitle(Theme themeType, String string1, Object value) {
        return themeType.legacyCode() + string1 + ": " + PASSIVE + value;
    }

    @Nonnull
    public static SlimefunItemStack themedSlimefunItemStack(String id, ItemStack itemStack,
            Theme themeType, String name, String... lore) {
        return themedSlimefunItemStack(id, itemStack, themeType, name, Arrays.asList(lore));
    }

    @Nonnull
    public static SlimefunItemStack themedSlimefunItemStack(String id, ItemStack itemStack,
            Theme themeType, String name, List<String> lore) {
        return new SlimefunItemStack(id, itemStack, applyThemeToString(themeType, name),
            itemLore(themeType, lore, List.of()));
    }

    @Nonnull
    public static SlimefunItemStack themedSlimefunItemStack(String id, Material material,
            Theme themeType, String name, String... lore) {
        return themedSlimefunItemStack(id, material, themeType, name, Arrays.asList(lore));
    }

    @Nonnull
    public static SlimefunItemStack themedSlimefunItemStack(String id, Material material,
            Theme themeType, String name, List<String> lore) {
        return new SlimefunItemStack(id, material, applyThemeToString(themeType, name),
            itemLore(themeType, lore, List.of()));
    }

    @Nonnull
    public static SlimefunItemStack themedSeed(String id, ItemStack seedStack, Theme themeType,
            String name, String[] lore, String[] validPlacements) {
        return themedSeed(id, seedStack, themeType, name, Arrays.asList(lore), Arrays.asList(validPlacements));
    }

    @Nonnull
    public static SlimefunItemStack themedSeed(String id, ItemStack seedStack, Theme themeType,
            String name, List<String> lore, List<String> validPlacements) {
        return new SlimefunItemStack(id, seedStack, applyThemeToString(themeType, name),
            itemLore(themeType, lore, validPlacements));
    }

    private static String[] itemLore(Theme themeType, List<String> lore, List<String> placements) {
        List<String> result = new ArrayList<>();
        result.add("");
        for (String line : lore) result.add(PASSIVE + line);
        for (String line : placements) result.add(PASSIVE + line);
        result.add("");
        result.add(applyThemeToString(CLICK_INFO, themeType.getLoreLine()));
        return result.toArray(String[]::new);
    }

    @Nonnull
    public static ItemStack themedItemStack(Material material, Theme themeType, String name,
            @Nullable List<String> lore) {
        // Unlike Slimefun item lore, this helper never added a trailing empty line.
        List<String> result = new ArrayList<>();
        if (lore != null) {
            result.add("");
            for (String line : lore) result.add(PASSIVE + line);
        }
        result.add(applyThemeToString(CLICK_INFO, themeType.getLoreLine()));
        return new CustomItemStack(material, applyThemeToString(themeType, name), result.toArray(String[]::new));
    }
}
