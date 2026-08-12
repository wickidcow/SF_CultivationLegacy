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

public interface CultivationBushHolder {

    String BUSH = "bush_display_present";
    String GROUP_PARENT = "bush_display_group";

    default boolean hasDisplayBush(@Nonnull Block block) {
        return hasDisplayBush(block.getLocation());
    }

    default boolean hasDisplayBush(@Nonnull Location location) {
        String hasBush = StorageCacheUtils.getData(location, BUSH);
        return Boolean.parseBoolean(hasBush);
    }

    default boolean hasDisplayBush(@Nonnull SlimefunBlockData config) {
        String hasBush = config.getData(BUSH);
        return Boolean.parseBoolean(hasBush);
    }

    default void removeBush(@Nonnull Location location) {
        removeBushDisplayGroup(location);
        StorageCacheUtils.removeData(location, BUSH);
        StorageCacheUtils.removeData(location, GROUP_PARENT);
    }

    default void addDisplayBush(@Nonnull Location location) {
        DisplayGroup displayGroup = DisplayGroupGenerators.generateBush(location.clone().add(0.5, 0, 0.5));
        StorageCacheUtils.setData(location, BUSH, "true");
        StorageCacheUtils.setData(location, GROUP_PARENT, displayGroup.getParentUUID().toString());
    }

    default void setAge(@Nonnull Location location, int age) {
        DisplayGroup displayGroup = getBushDisplayGroup(location);
        if (displayGroup != null) {
            DisplayGroupGenerators.setBushAge(displayGroup, age);
        }
    }

    default void addItemsToDisplay(@Nonnull Location location, @Nonnull ItemStack itemStack) {
        if (hasDisplayBush(location)) {
            DisplayGroup group = getBushDisplayGroup(location);
            if (group != null) {
                DisplayGroupGenerators.growItemsInBush(group, itemStack);
            }
        }
    }

    default void removeItems(@Nonnull Location location) {
        if (hasDisplayBush(location)) {
            DisplayGroup group = getBushDisplayGroup(location);
            if (group != null) {
                DisplayGroupGenerators.hideItemsInPlant(group);
            }
        }
    }

    @Nullable
    default UUID getBushDisplayGroupUUID(@Nonnull Location location) {
        String uuid = StorageCacheUtils.getData(location, GROUP_PARENT);
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }

    @Nullable
    default DisplayGroup getBushDisplayGroup(@Nonnull Location location) {
        UUID uuid = getBushDisplayGroupUUID(location);
        if (uuid == null) {
            return null;
        }
        return DisplayGroup.fromUUID(uuid);
    }

    default void removeBushDisplayGroup(@Nonnull Location location) {
        DisplayGroup displayGroup = getBushDisplayGroup(location);
        if (displayGroup != null) {
            displayGroup.remove();
        }
    }
}
