package ci.cultivation;

import dev.sefiraat.cultivation.Cultivation;
import dev.sefiraat.cultivation.Registry;
import dev.sefiraat.cultivation.api.datatypes.SeedPackDataType;
import dev.sefiraat.cultivation.api.datatypes.instances.FloraLevelProfile;
import dev.sefiraat.cultivation.api.datatypes.instances.SeedPackInstance;
import dev.sefiraat.cultivation.api.slimefun.items.plants.CultivationPlant;
import dev.sefiraat.cultivation.api.slimefun.plant.BreedingPair;
import dev.sefiraat.cultivation.api.slimefun.plant.BreedResultType;
import dev.sefiraat.cultivation.api.utils.CultivationThemes;
import dev.sefiraat.cultivation.implementation.slimefun.CultivationStacks;
import dev.sefiraat.cultivation.implementation.slimefun.items.Machines;
import dev.sefiraat.cultivation.implementation.slimefun.tools.RecipeUnlock;
import dev.sefiraat.cultivation.implementation.slimefun.tools.SeedPack;
import dev.sefiraat.cultivation.implementation.utils.LegacyItemText;
import dev.sefiraat.sefilib.string.Theme;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/** CI-only integration probe. Never packaged inside the distributed Cultivation JAR. */
public final class RuntimeProbe extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getScheduler().runTask(this, () -> {
            try {
                runChecks();
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "CULTIVATION_PROBE_FAIL", failure);
            }
        });
    }

    private void runChecks() throws Exception {
        check(Cultivation.getInstance().isEnabled(), "Cultivation must really be enabled");
        check(CultivationThemes.PLANT.getTextColor().value() == 0xa241bf, "Vendored Adventure Theme not loaded");
        check(CultivationThemes.PLANT.getDustOptions(1).getColor().asRGB() == 0xa241bf, "Dust RGB changed");
        Registry registry = Registry.getInstance();
        check(!registry.getRegisteredPlants().isEmpty(), "Plant registry empty");
        check(!registry.getRegisteredBushes().isEmpty(), "Bush registry empty");
        check(!registry.getRegisteredTrees().isEmpty(), "Tree registry empty");
        check(!registry.getPlantBreedingPairs().isEmpty(), "Breeding registry empty");

        List<SlimefunItem> registered = new ArrayList<>();
        registered.addAll(registry.getRegisteredPlants());
        registered.addAll(registry.getRegisteredBushes());
        registered.addAll(registry.getRegisteredTrees());
        registered.addAll(registry.getRegisteredByProducts());
        for (SlimefunItem item : registered) {
            check(SlimefunItem.getById(item.getId()) != null, "Unregistered item " + item.getId());
            ItemStack restored = roundTrip(item.getItem());
            SlimefunItem recognized = SlimefunItem.getByItem(restored);
            check(recognized != null && item.getId().equals(recognized.getId()), "Persistent item identity changed " + item.getId());
        }

        int definitions = 0;
        SeedPack packType = null;
        for (Field field : CultivationStacks.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && SlimefunItemStack.class.isAssignableFrom(field.getType())) {
                ItemStack item = (ItemStack) field.get(null);
                roundTrip(item);
                definitions++;
                if (SlimefunItem.getByItem(item) instanceof SeedPack pack) packType = pack;
            }
        }
        check(definitions > 100, "Item definitions were unexpectedly lost");
        check(packType != null, "Seed Pack did not register");

        for (CultivationPlant plant : registry.getRegisteredPlants()) {
            ItemStack analyzed = CultivationPlant.getStack(plant, new FloraLevelProfile(4, 5, 6, true));
            FloraLevelProfile restored = FloraLevelProfile.fromItemStack(roundTrip(analyzed));
            check(restored.getLevel() == 4 && restored.getSpeed() == 5 && restored.getStrength() == 6
                && restored.isAnalyzed(), "Analyzed plant profile lost: " + plant.getId());
            check(analyzed.getItemMeta().lore().size() == plant.getItem().getItemMeta().lore().size() + 4,
                "Plant analysis lore layout changed: " + plant.getId());
        }

        for (BreedingPair pair : registry.getPlantBreedingPairs()) {
            check(pair.isBreedPossible(pair.getMotherId(), pair.getFatherId()), "Forward breeding pair rejected");
            check(pair.isBreedPossible(pair.getFatherId(), pair.getMotherId()), "Reverse breeding pair rejected");
            BreedingPair certain = new BreedingPair(pair.getChild(), pair.getMotherId(), pair.getFatherId(), 1, 1);
            check(certain.testBreed(pair.getMotherId(), pair.getFatherId()) == BreedResultType.SUCCESS,
                "Deterministic breeding result changed");
            check(certain.testBreed("__not_a_seed__", "__not_a_seed__") == BreedResultType.NOT_PAIR,
                "Invalid breeding pair accepted");
        }

        CultivationPlant plant = registry.getRegisteredPlants().getFirst();
        SeedPackInstance contents = new SeedPackInstance();
        contents.setStoredItemId(plant.getId());
        contents.add(1, 1, 1, 65);
        contents.add(4, 5, 6, 12);
        contents.takeOne(4, 5, 6);
        check(contents.getAmount(4, 5, 6) == 11, "Seed withdrawal changed quantity");
        ItemStack pack = plain(packType.getItem());
        ItemMeta packMeta = pack.getItemMeta();
        packMeta.getPersistentDataContainer().set(SeedPackDataType.KEY, SeedPackDataType.TYPE, contents);
        pack.setItemMeta(packMeta);
        pack = roundTrip(pack);
        SeedPackInstance decoded = pack.getItemMeta().getPersistentDataContainer().get(SeedPackDataType.KEY, SeedPackDataType.TYPE);
        check(decoded != null && decoded.getStoredItemId().equals(plant.getId())
            && decoded.getAmount(1, 1, 1) == 65 && decoded.getAmount(4, 5, 6) == 11,
            "Seed Pack typed data did not round-trip");

        SeedPack.SeedPackGui gui = packType.new SeedPackGui(pack);
        check(gui.getItemInSlot(25).getAmount() == 64, "Seed Pack display stack limit changed");
        check(Objects.equals(gui.getItemInSlot(19).getItemMeta().displayName(),
            LegacyItemText.prepend(Theme.CLICK_INFO.apply("Stored Seed Type: "), plant.getItem().getItemMeta().displayName())),
            "Seed Pack title lost RGB or wording");
        // These two button handlers do not access Player; use no fake player or server account.
        gui.getMenuClickHandler(21).onClick(null, 21, gui.getItemInSlot(21), new ClickAction(false, false));
        gui.getMenuClickHandler(21).onClick(null, 21, gui.getItemInSlot(21), new ClickAction(true, false));
        check(gui.getItemInSlot(25).getAmount() == 64, "Seed Pack level selector failed to restore output");
        check(pack.getItemMeta().getPersistentDataContainer().get(SeedPackDataType.KEY, SeedPackDataType.TYPE)
            .getAmount(1, 1, 1) == 65, "Browsing Seed Pack consumed seeds");

        Path persisted = getDataFolder().toPath().resolve("seed-pack.bin");
        Files.createDirectories(persisted.getParent());
        if (Files.exists(persisted)) {
            ItemStack previous = ItemStack.deserializeBytes(Files.readAllBytes(persisted));
            SeedPackInstance previousContents = previous.getItemMeta().getPersistentDataContainer()
                .get(SeedPackDataType.KEY, SeedPackDataType.TYPE);
            check(previousContents != null && previousContents.getAmount(1, 1, 1) == 65
                && previousContents.getAmount(4, 5, 6) == 11, "Seed Pack changed across real server restart");
        }
        Files.write(persisted, pack.serializeAsBytes());

        SlimefunItem unlockItem = SlimefunItem.getByItem(CultivationStacks.RECIPE_UNLOCK);
        check(unlockItem instanceof RecipeUnlock, "Recipe unlock item not registered");
        ItemStack scroll = ((RecipeUnlock) unlockItem).generateRandomRecipeUnlock();
        roundTrip(scroll);
        for (var line : scroll.getItemMeta().lore()) {
            check(!LegacyItemText.serializer().serialize(line).contains("{0}"), "Recipe placeholder was not substituted");
        }

        int recipes = 0;
        for (Field field : Machines.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !SlimefunItem.class.isAssignableFrom(field.getType())) continue;
            Object machine = field.get(null);
            Method recipeMap = findDeclared(machine.getClass(), "getRecipes");
            if (recipeMap == null) continue;
            recipeMap.setAccessible(true);
            for (var entry : ((Map<?, ?>) recipeMap.invoke(machine)).entrySet()) {
                Object input = entry.getKey();
                Class<?> inputType = input instanceof String ? String.class : ItemStack[].class;
                ItemStack output = (ItemStack) machine.getClass().getMethod("testRecipe", inputType).invoke(machine, input);
                check(output != null && output.isSimilar((ItemStack) entry.getValue()), "Kitchen recipe lookup changed: " + field.getName());
                recipes++;
            }
        }
        check(recipes > 0, "Kitchen recipes were not registered");
        check(Machines.GARDEN_CLOCHE.getCapacityLong() == 2500, "Garden Cloche capacity changed");
        check(Machines.COUNTER_OVEN.getCapacityLong() == 2500, "Kitchen capacity changed");
        getLogger().info("CULTIVATION_PROBE_PASS definitions=" + definitions + " registered=" + registered.size()
            + " plants=" + registry.getRegisteredPlants().size() + " breedingPairs=" + registry.getPlantBreedingPairs().size()
            + " kitchenRecipes=" + recipes + " seedPackMenus=1 persistence=pass");
    }

    private Method findDeclared(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredMethod(name); }
            catch (NoSuchMethodException ignored) { }
        }
        return null;
    }

    private ItemStack plain(ItemStack original) {
        ItemStack copy = new ItemStack(original.getType(), original.getAmount());
        if (original.hasItemMeta()) copy.setItemMeta(original.getItemMeta());
        return copy;
    }

    private ItemStack roundTrip(ItemStack original) {
        ItemStack copy = plain(original);
        ItemStack restored = ItemStack.deserializeBytes(copy.serializeAsBytes());
        check(copy.equals(restored), "Item metadata/amount changed during Paper serialization: " + original.getType());
        return restored;
    }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
