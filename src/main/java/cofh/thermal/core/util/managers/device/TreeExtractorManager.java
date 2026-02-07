package cofh.thermal.core.util.managers.device;

import cofh.lib.util.crafting.ComparableItemStack;
import cofh.thermal.core.util.recipes.device.TreeExtractorBoost;
import cofh.thermal.core.util.recipes.device.TreeExtractorMapping;
import cofh.thermal.lib.util.managers.AbstractManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_BOOST;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_MAPPING;

public class TreeExtractorManager extends AbstractManager {

    private static final TreeExtractorManager INSTANCE = new TreeExtractorManager();

    protected Map<ComparableItemStack, Pair<Integer, Float>> boostMap = new Object2ObjectOpenHashMap<>();

    protected List<TreeExtractorMapping> recipes = new ArrayList<>();

    protected TreeExtractorManager() {

        super(8);
    }

    public static TreeExtractorManager instance() {

        return INSTANCE;
    }

    protected void clear() {

        boostMap.clear();
        recipes.clear();
    }

    public Stream<TreeExtractorMapping> getRecipes() {

        return recipes.stream();
    }

    // region BOOSTS
    public boolean validBoost(ItemStack item) {

        return boostMap.containsKey(makeNBTComparable(item));
    }

    public void addBoost(TreeExtractorBoost boost) {

        for (ItemStack ingredient : boost.getIngredient().getItems()) {
            boostMap.put(makeNBTComparable(ingredient), Pair.of(boost.getCycles(), boost.getOutputMod()));
        }
    }

    public int getBoostCycles(ItemStack item) {

        return validBoost(item) ? boostMap.get(makeNBTComparable(item)).getLeft() : 0;
    }

    public float getBoostOutputMod(ItemStack item) {

        return validBoost(item) ? boostMap.get(makeNBTComparable(item)).getRight() : 1.0F;
    }
    // endregion

    // region IManager
    @Override
    public void refresh(RecipeManager recipeManager) {

        System.out.println("[TREE_EXTRACTOR DEBUG] TreeExtractorManager.refresh() called");
        System.out.println("[TREE_EXTRACTOR DEBUG] Thread: " + Thread.currentThread().getName());
        System.out.println("[TREE_EXTRACTOR DEBUG] RecipeManager: " + recipeManager);
        System.out.println("[TREE_EXTRACTOR DEBUG] TREE_EXTRACTOR_MAPPING type: " + TREE_EXTRACTOR_MAPPING.get());
        System.out.println("[TREE_EXTRACTOR DEBUG] TREE_EXTRACTOR_BOOST type: " + TREE_EXTRACTOR_BOOST.get());
        
        // Debug: Show all available recipe types
        System.out.println("[TREE_EXTRACTOR DEBUG] Available recipe types in RecipeManager:");
        int thermalRecipes = 0;
        int totalRecipes = 0;
        for (var recipe : recipeManager.getRecipes()) {
            totalRecipes++;
            String recipeId = recipe.id().toString();
            if (recipeId.startsWith("thermal:")) {
                thermalRecipes++;
                System.out.println("[TREE_EXTRACTOR DEBUG] THERMAL RECIPE: " + recipeId + ": " + recipe.value());
            }
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] Total recipes: " + totalRecipes + ", Thermal recipes: " + thermalRecipes);
        
        // If no thermal recipes and this is server side, that's the problem!
        if (thermalRecipes == 0 && Thread.currentThread().getName().equals("Server thread")) {
            System.out.println("[TREE_EXTRACTOR DEBUG] CRITICAL: Server side has no Thermal recipes! This is the bug!");
            System.out.println("[TREE_EXTRACTOR DEBUG] Server RecipeManager: " + recipeManager);
        }
        
        clear();
        System.out.println("[TREE_EXTRACTOR DEBUG] Looking for TREE_EXTRACTOR_MAPPING recipes...");
        var mappingRecipes = recipeManager.getAllRecipesFor(TREE_EXTRACTOR_MAPPING.get());
        System.out.println("[TREE_EXTRACTOR DEBUG] RecipeManager returned " + mappingRecipes.size() + " mapping recipes");
        for (var mapping : mappingRecipes) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Found mapping recipe: " + mapping.value());
            recipes.add(mapping.value());
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] Looking for TREE_EXTRACTOR_BOOST recipes...");
        var boostRecipes = recipeManager.getAllRecipesFor(TREE_EXTRACTOR_BOOST.get());
        System.out.println("[TREE_EXTRACTOR DEBUG] RecipeManager returned " + boostRecipes.size() + " boost recipes");
        for (var boost : boostRecipes) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Found boost recipe: " + boost.value());
            addBoost(boost.value());
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] TreeExtractorManager refresh complete. Total recipes: " + recipes.size() + ", Total boosts: " + boostMap.size());
    }
    // endregion
}
