package cofh.thermal.lib.util;

import cofh.thermal.lib.util.managers.IManager;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;

import java.util.ArrayList;
import java.util.List;

public class ThermalRecipeManagers {

    private static final ThermalRecipeManagers INSTANCE = new ThermalRecipeManagers();

    private RecipeManager clientRecipeManager;
    private RecipeManager serverRecipeManager;
    private final List<IManager> managers = new ArrayList<>();

    public static ThermalRecipeManagers instance() {

        return INSTANCE;
    }

    public void setClientRecipeManager(RecipeManager recipeManager) {

        this.clientRecipeManager = recipeManager;
    }

    public void setServerRecipeManager(RecipeManager recipeManager) {

        this.serverRecipeManager = recipeManager;
    }

    public static void registerManager(IManager manager) {

        if (!instance().managers.contains(manager)) {
            instance().managers.add(manager);
        }
    }

    public void config() {

        for (IManager sub : managers) {
            sub.config();
        }
    }

    public void refreshServer() {

        if (this.serverRecipeManager == null) {
            return;
        }
        for (IManager sub : managers) {
            sub.refresh(this.serverRecipeManager);
        }
    }

    public void refreshClient() {

        if (this.clientRecipeManager == null) {
            return;
        }
        for (IManager sub : managers) {
            sub.refresh(this.clientRecipeManager);
        }
    }
    /**
     * Recursively clears the itemStacks and stackingIds caches on an Ingredient,
     * including children of CompoundIngredient. This is necessary because
     * CompoundIngredient.getItems() delegates to child.getItems(), and each child
     * has its own separate cache that must also be invalidated.
     */
    public static void invalidateIngredientCache(Ingredient ingredient) {

        ingredient.itemStacks = null;
        ingredient.stackingIds = null;
        if (ingredient.isCustom() && ingredient.getCustomIngredient() instanceof CompoundIngredient compound) {
            for (Ingredient child : compound.children()) {
                invalidateIngredientCache(child);
            }
        }
    }
    // endregion
}
