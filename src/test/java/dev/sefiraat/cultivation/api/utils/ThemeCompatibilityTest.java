package dev.sefiraat.cultivation.api.utils;

import dev.sefiraat.sefilib.string.Theme;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ThemeCompatibilityTest {
    private static final String TYPE = "dev.sefiraat.sefilib.string.Theme";

    // Load only the original Theme in isolation. Its other dependencies stay in
    // the normal test loader. Reflection here tests an old binary contract; the
    // production implementation never uses reflection to access deprecated APIs.
    private URLClassLoader originalLoader() throws Exception {
        URL jar = Path.of(System.getProperty("user.home"), ".m2", "repository", "dev", "sefiraat",
            "SefiLib", "0.3.0", "SefiLib-0.3.0.jar").toUri().toURL();
        return new URLClassLoader(new URL[]{jar}, Theme.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(TYPE)) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
    }

    private Set<String> publicApi(Class<?> type) {
        Set<String> result = new HashSet<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) result.add("F:" + field.getName() + ":"
                + field.getType().getName() + ":" + Modifier.isStatic(field.getModifiers()));
        }
        for (Constructor<?> constructor : type.getConstructors()) {
            result.add("C:" + parameters(constructor.getParameterTypes()));
        }
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) result.add("M:" + method.getName() + ":"
                + parameters(method.getParameterTypes()) + ":" + method.getReturnType().getName()
                + ":" + Modifier.isStatic(method.getModifiers()));
        }
        return result;
    }

    private String parameters(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getName).collect(Collectors.joining(","));
    }

    @Test
    void everyOriginalPublicBinaryDescriptorRemainsAvailable() throws Exception {
        try (URLClassLoader loader = originalLoader()) {
            Set<String> missing = publicApi(loader.loadClass(TYPE));
            missing.removeAll(publicApi(Theme.class));
            assertTrue(missing.isEmpty(), "Removed SefiLib API: " + missing);
        }
    }

    @Test
    void originalStaticThemesHaveExactlyTheSameTextAndLabels() throws Exception {
        try (URLClassLoader loader = originalLoader()) {
            Class<?> original = loader.loadClass(TYPE);
            for (String name : new String[]{"WARNING", "ERROR", "NOTICE", "PASSIVE", "SUCCESS", "CLICK_INFO"}) {
                Object old = original.getField(name).get(null);
                Theme current = (Theme) Theme.class.getField(name).get(null);
                assertEquals(old.toString(), current.toString(), name);
                assertEquals(original.getMethod("getLoreLine").invoke(old), current.getLoreLine(), name);
                assertEquals(original.getMethod("asTitle", Object.class, Object.class).invoke(old, "Level", 4),
                    current.asTitle("Level", 4), name);
            }
        }
    }

    @Test
    void legacyConstructorsAndGetterStillRoundTripRgbAndFormatting() throws Exception {
        Class<?> chatColor = Class.forName("net.md_5.bungee.api.ChatColor");
        try (URLClassLoader loader = originalLoader()) {
            Class<?> original = loader.loadClass(TYPE);
            Object[] colors = {chatColor.getMethod("of", String.class).invoke(null, "#a241bf"),
                chatColor.getField("GREEN").get(null), chatColor.getField("BOLD").get(null),
                chatColor.getField("RESET").get(null)};
            for (Object color : colors) {
                Object old = original.getConstructor(chatColor, String.class).newInstance(color, "Old label");
                Theme current = (Theme) Theme.class.getConstructor(chatColor, String.class).newInstance(color, "Old label");
                assertEquals(old.toString(), current.toString());
                assertEquals(original.getMethod("apply", Object.class).invoke(old, "Name"), current.apply("Name"));
                assertEquals(color, Theme.class.getMethod("getColor").invoke(current));
            }
        }
    }

    @Test
    void modernColorsAndDustUseExactRgb() {
        Theme modern = new Theme(TextColor.color(0xa241bf), "Plant");
        assertEquals(0xa241bf, modern.getTextColor().value());
        assertEquals(0xa241bf, modern.getDustOptions(1.0f).getColor().asRGB());
        assertEquals("\u00a7a", new Theme(NamedTextColor.GREEN).legacyCode());
    }
}
