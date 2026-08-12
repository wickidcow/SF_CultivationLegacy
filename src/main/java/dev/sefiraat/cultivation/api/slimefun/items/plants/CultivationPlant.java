package dev.sefiraat.cultivation.api.slimefun.items.plants;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import dev.sefiraat.cultivation.Registry;
import dev.sefiraat.cultivation.api.datatypes.FloraLevelProfileDataType;
import dev.sefiraat.cultivation.api.datatypes.instances.FloraLevelProfile;
import dev.sefiraat.cultivation.api.interfaces.CultivationCroppable;
import dev.sefiraat.cultivation.api.interfaces.CultivationFlora;
import dev.sefiraat.cultivation.api.interfaces.CultivationLevelProfileHolder;
import dev.sefiraat.cultivation.api.interfaces.CultivationPlantHolder;
import dev.sefiraat.cultivation.api.slimefun.RecipeTypes;
import dev.sefiraat.cultivation.api.slimefun.groups.CultivationGroups;
import dev.sefiraat.cultivation.api.slimefun.items.CultivationFloraItem;
import dev.sefiraat.cultivation.api.slimefun.plant.BreedResult;
import dev.sefiraat.cultivation.api.slimefun.plant.BreedingPair;
import dev.sefiraat.cultivation.api.slimefun.plant.Growth;
import dev.sefiraat.cultivation.api.slimefun.plant.PlantTheme;
import dev.sefiraat.cultivation.api.utils.LevelType;
import dev.sefiraat.cultivation.api.utils.StatisticUtils;
import dev.sefiraat.cultivation.implementation.utils.Keys;
import dev.sefiraat.sefilib.entity.display.DisplayInteractable;
import dev.sefiraat.sefilib.misc.ParticleUtils;
import dev.sefiraat.sefilib.string.Theme;
import dev.sefiraat.sefilib.world.LocationUtils;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.papermc.lib.PaperLib;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is used to define a CultivationPlant that will grow as a {@link CultivationFlora}
 */
public abstract class CultivationPlant extends CultivationFloraItem<CultivationPlant>
    implements CultivationFlora, CultivationLevelProfileHolder, CultivationCroppable, CultivationPlantHolder,
               DisplayInteractable {

    private static final String WIKI_PAGE = "plants";

    @Nonnull
    public static final Set<BlockFace> BREEDING_DIRECTIONS = Set.of(
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.EAST,
        BlockFace.WEST
    );

    @Nonnull
    protected Set<BreedingPair> breedingPairs = new HashSet<>();

    @ParametersAreNonnullByDefault
    protected CultivationPlant(SlimefunItemStack item, Growth growth) {
        this(item, RecipeTypes.PLANT_BREEDING, new ItemStack[0], growth);
    }

    @ParametersAreNonnullByDefault
    protected CultivationPlant(SlimefunItemStack item,
                               RecipeType recipeType,
                               ItemStack[] recipe,
                               Growth growth
    ) {
        this(item, recipeType, recipe, growth, null);
    }

    @ParametersAreNonnullByDefault
    protected CultivationPlant(SlimefunItemStack item,
                               RecipeType recipeType,
                               ItemStack[] recipe,
                               Growth growth,
                               @Nullable ItemStack recipeOutput
    ) {
        super(CultivationGroups.PLANTS, item, recipeType, recipe, recipeOutput, growth);
    }

    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler(
            new BlockBreakHandler(false, false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerBreak(BlockBreakEvent blockBreakEvent, ItemStack itemStack, List<ItemStack> list) {
                    onBreak(blockBreakEvent);
                }
            }
        );
    }

    @Override
    public void postRegister() {
        super.postRegister();
        addWikiPage(WIKI_PAGE);
    }

    @Override
    protected void tryBreed(@Nonnull Block motherBlock, @Nonnull CultivationPlant plant) {
        double breedChance = ThreadLocalRandom.current().nextDouble();
        if (breedChance > getDefaultGrowthRate()) {
            // No breed attempted this tick
            return;
        }

        for (BlockFace face : BREEDING_DIRECTIONS) {
            Block middleBlock = motherBlock.getRelative(face);
            // There must be space for the new block
            if (middleBlock.getType() != Material.AIR || StorageCacheUtils.hasBlock(middleBlock.getLocation())) {
                continue;
            }
            Block potentialMate = middleBlock.getRelative(face);
            SlimefunItem mateItem = StorageCacheUtils.getSfItem(potentialMate.getLocation());

            if (mateItem instanceof CultivationPlant mate) {
                testBreed(plant, mate, middleBlock, motherBlock, potentialMate);
            }
        }
    }

    @Override
    public void whenPlaced(@NotNull BlockPlaceEvent event) {
        super.whenPlaced(event);

        Location location = event.getBlock().getLocation();
        ItemStack itemStack = event.getItemInHand();
        ItemMeta itemMeta = itemStack.getItemMeta();

        FloraLevelProfile profile = PersistentDataAPI.get(
            itemMeta,
            FloraLevelProfileDataType.KEY,
            FloraLevelProfileDataType.TYPE,
            new FloraLevelProfile(1, 1, 1, false)
        );

        setLevelProfile(location, profile);
        PROFILE_MAP.put(location, profile);
    }

    @OverridingMethodsMustInvokeSuper
    public void onBreak(@NotNull BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        ItemStack itemToDrop = getDroppedItemStack(location);
        removeCropped(location);
        removePlant(location);
        location.getWorld().dropItem(location.clone().add(0.5, 0.5, 0.5), itemToDrop);
        removeLevelProfile(location);
        event.setDropItems(false);
    }

    public ItemStack getDroppedItemStack(@Nonnull Location location) {
        return getStack(this, getLevelProfile(location));
    }

    @Override
    @ParametersAreNonnullByDefault
    protected boolean canGrow(Block block, CultivationPlant flora, SlimefunBlockData data, Location location, int growthStage) {
        // Preserve the original Crop Stick behavior.
        if (isCropped(data)) {
            return true;
        }

        // AlbionMC Legacy convenience: ordinary Cultivation plants may grow when
        // planted directly above vanilla farmland. Crop Sticks remain available
        // for the addon's advanced crop and crossbreeding mechanics.
        return block.getRelative(BlockFace.DOWN).getType() == Material.FARMLAND;
    }

    @Override
    public double getGrowthRate(@Nonnull Location location) {
        return getDefaultGrowthRate() * getLevelProfile(location).getLevel();
    }

    public double getGrowthRate(@Nonnull FloraLevelProfile profile) {
        return getDefaultGrowthRate() * profile.getLevel();
    }

    @ParametersAreNonnullByDefault
    private void testBreed(CultivationPlant mother,
                           CultivationPlant mate,
                           Block middleBlock,
                           Block motherBlock,
                           Block fatherBlock
    ) {
        BreedResult result = Registry.getInstance().getBreedResult(mother.getId(), mate.getId());

        if (!isMature(motherBlock)
            || !isMature(fatherBlock)
            || !isCrossCropped(motherBlock)
            || !isCrossCropped(fatherBlock)
        ) {
            return;
        }

        switch (result.getResultType()) {
            case NO_PAIRS ->
                // No matching breeding pairs, lets feedback to the player then move to the next direction
                breedInvalidDisplay(middleBlock.getLocation());
            case SUCCESS -> {
                // Breed was a success - spawn child, log discovery
                CultivationPlant child = result.getMatchedPair().getChild();
                trySetChildSeed(motherBlock.getLocation(), middleBlock, child);
                StatisticUtils.incrementExp(getOwner(motherBlock.getLocation()), LevelType.HORTICULTURALIST, 2);
            }
            case SPREAD_NO_MUTATE -> {
                // Breed failed, spread success - spawn copy of mother
                trySetChildSeed(motherBlock.getLocation(), middleBlock, mother);
                StatisticUtils.incrementExp(getOwner(motherBlock.getLocation()), LevelType.HORTICULTURALIST, 1);
            }
            case SPREAD_MUTATE -> {
                // Breed not possible, but mutation possible.
                FloraLevelProfile motherProfile = getLevelProfile(motherBlock.getLocation());
                FloraLevelProfile fatherProfile = getLevelProfile(fatherBlock.getLocation());
                trySetChildSeed(motherBlock.getLocation(), middleBlock, mother);
                tryMutate(middleBlock, motherProfile, fatherProfile);
                StatisticUtils.incrementExp(getOwner(motherBlock.getLocation()), LevelType.HORTICULTURALIST, 1);
            }
        }
    }

    private void breedInvalidDisplay(@Nonnull Location location) {
        ParticleUtils.displayParticleRandomly(
            LocationUtils.centre(location),
            0.5,
            2,
            new Particle.DustOptions(Color.BLACK, 1)
        );
    }

    @ParametersAreNonnullByDefault
    private void trySetChildSeed(Location motherLocation, Block cloneBlock, CultivationPlant childSeed) {
        PlantTheme theme = childSeed.growth.getTheme();

        if (theme == null) {
            return;
        }

        UUID owner = getOwner(motherLocation);
        Location cloneBlockLocation = cloneBlock.getLocation();

        cloneBlock.setType(Material.PLAYER_HEAD);
        PlayerHead.setSkin(cloneBlock, theme.getSeed().getPlayerSkin(), false);
        PaperLib.getBlockState(cloneBlock, false).getState().update(true, false);
        Slimefun.getDatabaseManager().getBlockDataController().createBlock(cloneBlockLocation, childSeed.getId());
        StorageCacheUtils.setData(cloneBlockLocation, Keys.FLORA_GROWTH_STAGE, "0");
        StorageCacheUtils.setData(cloneBlockLocation, Keys.FLORA_OWNER, owner.toString());
        StatisticUtils.unlockDiscovery(owner, childSeed.getId());
        breedSuccess(cloneBlock.getLocation());
    }

    @ParametersAreNonnullByDefault
    private void tryMutate(Block cloneBlock, FloraLevelProfile motherProfile, FloraLevelProfile fatherProfile) {
        FloraLevelProfile profile = FloraLevelProfile.testMutation(motherProfile, fatherProfile);
        setLevelProfile(cloneBlock.getLocation(), profile);
    }

    protected void breedSuccess(@Nonnull Location location) {
        ParticleUtils.displayParticleRandomly(LocationUtils.centre(location), Particle.SLIME, 0.5, 4);
    }

    /**
     * Adds a possible BreedingPair that will result in this seed as a child.
     * Can have multiple pairs resulting in the same child.
     *
     * @param mother       The ID of the potential Mother
     * @param father       The ID of the potential Mother
     * @param breedChance  The chance for the breed to return this plant
     * @param spreadChance The chance that the Mother will spread
     * @return Returns self
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public CultivationPlant addBreedingPair(String mother, String father, double breedChance, double spreadChance) {
        this.breedingPairs.add(new BreedingPair(this, mother, father, breedChance, spreadChance));
        return this;
    }

    /**
     * Gets all the possible ways this plant can be bred
     *
     * @return The {@link Set} of {@link BreedingPair}s this plant can be bred from
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public Set<BreedingPair> getBreedingPairs() {
        return this.breedingPairs;
    }

    @Override
    public int getMaxGrowthStages() {
        return 2;
    }

    @Override
    public CultivationPlant tryRegister(@NotNull SlimefunAddon addon) {
        Registry.getInstance().addPlant(this);
        return super.tryRegister(addon);
    }

    public static ItemStack getStack(@Nonnull CultivationPlant plant, @Nonnull FloraLevelProfile profile) {
        ItemStack itemToDrop = plant.getItem().clone();
        ItemMeta itemMeta = itemToDrop.getItemMeta();

        PersistentDataAPI.set(
            itemMeta,
            FloraLevelProfileDataType.KEY,
            FloraLevelProfileDataType.TYPE,
            profile
        );

        if (profile.isAnalyzed()) {
            List<String> lore = itemMeta.getLore();
            lore.add("");
            lore.add(Theme.CLICK_INFO.asTitle("等级", profile.getLevel()));
            lore.add(Theme.CLICK_INFO.asTitle("速率", profile.getSpeed()));
            lore.add(Theme.CLICK_INFO.asTitle("强度", profile.getStrength()));
            itemMeta.setLore(lore);
        }

        itemToDrop.setItemMeta(itemMeta);
        return itemToDrop;
    }
}
