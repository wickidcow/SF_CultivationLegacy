package dev.sefiraat.cultivation.api.interfaces;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import dev.sefiraat.cultivation.implementation.utils.DisplayGroupGenerators;
import dev.sefiraat.sefilib.entity.display.DisplayGroup;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public interface CultivationPlantHolder {

    String PLANT = "plant_display_present";
    String GROUP_PARENT = "plant_display_group";

    default boolean hasDisplayPlant(@Nonnull Block block) {
        return hasDisplayPlant(block.getLocation());
    }

    default boolean hasDisplayPlant(@Nonnull Location location) {
        String hasPlant = StorageCacheUtils.getData(location, PLANT);
        return Boolean.parseBoolean(hasPlant);
    }

    default boolean hasDisplayPlant(@Nonnull SlimefunBlockData config) {
        String hasPlant = config.getData(PLANT);
        return Boolean.parseBoolean(hasPlant);
    }

    default void removePlant(@Nonnull Location location) {
        removePlantDisplayGroup(location);
        StorageCacheUtils.removeData(location, PLANT);
        StorageCacheUtils.removeData(location, GROUP_PARENT);
    }

    default void addDisplayPlant(@Nonnull Location location) {
        DisplayGroup displayGroup = DisplayGroupGenerators.generatePlant(location.clone().add(0.5, 0, 0.5));
        StorageCacheUtils.setData(location, PLANT, "true");
        StorageCacheUtils.setData(location, GROUP_PARENT, displayGroup.getParentUUID().toString());
    }

    default void addItemsToDisplay(@Nonnull Location location, @Nonnull ItemStack itemStack) {
        if (hasDisplayPlant(location)) {
            DisplayGroup group = getPlantDisplayGroup(location);
            if (group != null) {
                DisplayGroupGenerators.growItemsInPlant(group, itemStack);
            }
        }
    }

    default void removeItems(@Nonnull Location location) {
        if (hasDisplayPlant(location)) {
            DisplayGroup group = getPlantDisplayGroup(location);
            if (group != null) {
                DisplayGroupGenerators.hideItemsInPlant(group);
            }
        }
    }

    @Nullable
    default UUID getPlantDisplayGroupUUID(@Nonnull Location location) {
        String uuid = StorageCacheUtils.getData(location, GROUP_PARENT);
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }

    @Nullable
    default DisplayGroup getPlantDisplayGroup(@Nonnull Location location) {
        UUID uuid = getPlantDisplayGroupUUID(location);
        if (uuid == null) {
            return null;
        }
        return DisplayGroup.fromUUID(uuid);
    }

    default void removePlantDisplayGroup(@Nonnull Location location) {
        DisplayGroup displayGroup = getPlantDisplayGroup(location);
        if (displayGroup != null) {
            displayGroup.remove();
        }
    }
}
