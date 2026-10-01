package dev.sefiraat.cultivation.managers;

import dev.sefiraat.cultivation.Cultivation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.logging.Level;

/**
 * This class is used to create and manage/save custom configuration files
 */
public class ConfigManager {

    public static final String FILE_NAME_EXP = "exp.yml";
    public static final String FILE_NAME_CODEX = "codex.yml";

    // Player exp
    private final FileConfiguration exp;
    private final FileConfiguration codex;

    public ConfigManager() {
        setupDefaultConfig();
        this.exp = getConfig(FILE_NAME_EXP);
        this.codex = getConfig(FILE_NAME_CODEX);
    }

    private void setupDefaultConfig() {
        Cultivation plugin = Cultivation.getInstance();
        InputStream inputStream = plugin.getResource("config.yml");

        if (inputStream == null) {
            // Not sure how? Regardless cannot copy over new keys
            return;
        }

        File existingFile = new File(plugin.getDataFolder(), "config.yml");
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            YamlConfiguration resourceConfig = new YamlConfiguration();
            resourceConfig.load(reader);
            FileConfiguration existingConfig = PlayerDataFile.load(existingFile.toPath());
            for (String key : resourceConfig.getKeys(false)) {
                checkKey(existingConfig, resourceConfig, key);
            }
            PlayerDataFile.save(existingConfig, existingFile.toPath());
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("Unable to load or update config.yml; original configuration is retained.", e);
        }
    }

    @ParametersAreNonnullByDefault
    private void checkKey(FileConfiguration existingConfig, FileConfiguration resourceConfig, String key) {
        Object currentValue = existingConfig.get(key);
        Object newValue = resourceConfig.get(key);
        if (newValue instanceof ConfigurationSection section) {
            for (String sectionKey : section.getKeys(false)) {
                checkKey(existingConfig, resourceConfig, key + "." + sectionKey);
            }
        } else if (currentValue == null) {
            existingConfig.set(key, newValue);
        }
    }

    @Nonnull
    private FileConfiguration getConfig(@Nonnull String fileName) {
        File file = new File(Cultivation.getInstance().getDataFolder(), fileName);
        try {
            if (Files.notExists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(file.toPath().getParent());
                Files.createFile(file.toPath());
            }
            return PlayerDataFile.load(file.toPath());
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("Unable to load " + fileName
                + "; Cultivation is stopping to protect existing player progress.", e);
        }
    }

    public void saveAll() {
        Cultivation.getInstance().getLogger().info("Cultivation saving data.");
        save(exp, FILE_NAME_EXP);
        save(codex, FILE_NAME_CODEX);
    }

    private void save(@Nonnull FileConfiguration config, @Nonnull String fileName) {
        File file = new File(Cultivation.getInstance().getDataFolder(), fileName);
        try {
            PlayerDataFile.save(config, file.toPath());
        } catch (IOException | RuntimeException exception) {
            Cultivation.getInstance().getLogger().log(Level.SEVERE,
                "Unable to save " + fileName + "; the previous data file was not intentionally truncated.", exception);
        }
    }

    public FileConfiguration getExp() {
        return exp;
    }

    public FileConfiguration getCodex() {
        return codex;
    }

    public boolean isAutoUpdate() {
        return Cultivation.getInstance().getConfig().getBoolean("auto-update");
    }

    public boolean isDebugMessages() {
        return Cultivation.getInstance().getConfig().getBoolean("debug-messages");
    }

}
