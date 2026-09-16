package dev.sefiraat.cultivation.implementation.slimefun.machines;

import city.norain.slimefun4.utils.TaskUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import dev.sefiraat.cultivation.api.datatypes.instances.FloraLevelProfile;
import dev.sefiraat.cultivation.api.slimefun.items.plants.HarvestablePlant;
import dev.sefiraat.cultivation.implementation.slimefun.items.Machines;
import dev.sefiraat.sefilib.entity.display.DisplayGroup;
import dev.sefiraat.sefilib.string.Theme;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GardenCloche extends SlimefunItem implements EnergyNetComponent {

    private static final String KEY_LEGACY_PLANT = "plant";
    private static final String KEY_LEGACY_UUID = "display-uuid";
    private static final String KEY_SPRITE_UUID = "cloche-sprite-uuid";
    private static final String KEY_VISUAL_VERSION = "cloche-visual-version";
    private static final String KEY_MIGRATION_PENDING = "cloche-entity-migration";
    private static final String VISUAL_VERSION = "1";
    private static final int PLANT_SLOT = 20;
    private static final int[] OUTPUT_SLOTS = new int[]{
        14, 15, 16, 23, 24, 25, 32, 33, 34
    };
    private static final int[] PLANT_SLOT_BACKGROUND = new int[]{
        10, 11, 12, 19, 21, 28, 29, 30
    };
    private static final int[] BACKGROUND = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 17, 18, 22, 26, 27, 31, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int POWER_REQUIREMENT = 100;

    public GardenCloche(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        item.setType(Material.GREEN_STAINED_GLASS);
    }

    @Override
    public void preRegister() {
        addItemHandler(
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@NotNull BlockPlaceEvent e) {
                    Location location = e.getBlock().getLocation();
                    cleanupLegacyDisplay(location);
                    cleanupSprite(location);
                    e.getBlock().setType(Material.GREEN_STAINED_GLASS);
                    createSprite(location);
                    StorageCacheUtils.setData(location, KEY_VISUAL_VERSION, VISUAL_VERSION);
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                    Location location = e.getBlock().getLocation();
                    cleanupLegacyDisplay(location);
                    cleanupSprite(location);
                    BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                    if (blockMenu != null) {
                        blockMenu.dropItems(location, PLANT_SLOT);
                        blockMenu.dropItems(location, OUTPUT_SLOTS);
                    }
                }
            },
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return false;
                }

                @Override
                public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                    Location location = block.getLocation();

                    // Upgrade old 5-entity cloches and the temporary entity-free build once.
                    // Normal ticking only compares the cached visual-version string afterwards.
                    if (ensureVisual(location, data)) {
                        return;
                    }

                    BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                    if (blockMenu == null) {
                        return;
                    }

                    ItemStack possiblePlant = blockMenu.getItemInSlot(PLANT_SLOT);
                    if (possiblePlant == null || possiblePlant.getType().isAir()) {
                        return;
                    }

                    SlimefunItem slimefunItem = SlimefunItem.getByItem(possiblePlant);
                    if (!(slimefunItem instanceof HarvestablePlant plant)) {
                        return;
                    }

                    if (getCharge(location) < POWER_REQUIREMENT) {
                        return;
                    }

                    FloraLevelProfile profile = FloraLevelProfile.fromItemStack(possiblePlant);
                    double growthRate = plant.getGrowthRate(profile);
                    if (ThreadLocalRandom.current().nextDouble() < growthRate) {
                        ItemStack itemStack = plant.getRandomItemWithDropModifier(profile);
                        if (itemStack != null) {
                            blockMenu.pushItem(itemStack, OUTPUT_SLOTS);
                            removeCharge(location, POWER_REQUIREMENT);
                        }
                    }
                }
            }
        );
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                ItemStack backgroundInput = new CustomItemStack(
                    Material.GREEN_STAINED_GLASS_PANE,
                    Theme.PASSIVE.apply("Insert Plant")
                );
                drawBackground(BACKGROUND);
                drawBackground(backgroundInput, PLANT_SLOT_BACKGROUND);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return Machines.GARDEN_CLOCHE.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return OUTPUT_SLOTS;
                }
                return new int[0];
            }
        };
    }

    private boolean ensureVisual(@Nonnull Location location, @Nonnull SlimefunBlockData data) {
        if (VISUAL_VERSION.equals(data.getData(KEY_VISUAL_VERSION))) {
            return false;
        }

        if (Boolean.parseBoolean(data.getData(KEY_MIGRATION_PENDING))) {
            return true;
        }

        String legacyUuid = data.getData(KEY_LEGACY_UUID);
        String oldSpriteUuid = data.getData(KEY_SPRITE_UUID);
        data.setData(KEY_MIGRATION_PENDING, "true");

        TaskUtil.runSyncMethod(location, () -> {
            removeLegacyDisplayEntity(legacyUuid);
            removeSpriteEntity(oldSpriteUuid);

            SlimefunItem currentItem = StorageCacheUtils.getSlimefunItem(location);
            if (currentItem != null && getId().equals(currentItem.getId())) {
                location.getBlock().setType(Material.GREEN_STAINED_GLASS);
                createSprite(location);
                StorageCacheUtils.setData(location, KEY_VISUAL_VERSION, VISUAL_VERSION);
            }

            StorageCacheUtils.removeData(location, KEY_LEGACY_PLANT);
            StorageCacheUtils.removeData(location, KEY_LEGACY_UUID);
            StorageCacheUtils.removeData(location, KEY_MIGRATION_PENDING);
        });
        return true;
    }

    private void createSprite(@Nonnull Location location) {
        Location spriteLocation = location.clone().add(0.5, 0.56, 0.5);
        ItemDisplay sprite = (ItemDisplay) location.getWorld().spawnEntity(spriteLocation, EntityType.ITEM_DISPLAY);
        sprite.setItemStack(new ItemStack(Material.SMALL_DRIPLEAF));
        sprite.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        sprite.setBillboard(Display.Billboard.CENTER);
        sprite.setGravity(false);
        sprite.setPersistent(true);
        sprite.setInvulnerable(true);
        sprite.setSilent(true);
        sprite.setShadowRadius(0.0F);
        sprite.setShadowStrength(0.0F);
        sprite.setViewRange(0.65F);
        sprite.addScoreboardTag("cultivation_cloche_sprite");

        var transformation = sprite.getTransformation();
        transformation.getScale().set(0.32F, 0.32F, 0.32F);
        sprite.setTransformation(transformation);

        StorageCacheUtils.setData(location, KEY_SPRITE_UUID, sprite.getUniqueId().toString());
    }

    private void cleanupLegacyDisplay(@Nonnull Location location) {
        removeLegacyDisplayEntity(StorageCacheUtils.getData(location, KEY_LEGACY_UUID));
        StorageCacheUtils.removeData(location, KEY_LEGACY_PLANT);
        StorageCacheUtils.removeData(location, KEY_LEGACY_UUID);
        StorageCacheUtils.removeData(location, KEY_MIGRATION_PENDING);
    }

    private void cleanupSprite(@Nonnull Location location) {
        removeSpriteEntity(StorageCacheUtils.getData(location, KEY_SPRITE_UUID));
        StorageCacheUtils.removeData(location, KEY_SPRITE_UUID);
        StorageCacheUtils.removeData(location, KEY_VISUAL_VERSION);
    }

    private void removeSpriteEntity(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return;
        }

        try {
            Entity entity = Bukkit.getEntity(UUID.fromString(uuidString));
            if (entity instanceof ItemDisplay) {
                entity.remove();
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed saved data should never stop the cloche from being usable or removable.
        }
    }

    private void removeLegacyDisplayEntity(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return;
        }

        try {
            DisplayGroup group = DisplayGroup.fromUUID(UUID.fromString(uuidString));
            if (group != null) {
                group.remove();
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed legacy UUID should never stop the cloche from being usable or removable.
        }
    }

    @NotNull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return 2500;
    }
}
