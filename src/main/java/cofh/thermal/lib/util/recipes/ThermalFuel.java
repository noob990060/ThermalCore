package cofh.thermal.lib.util.recipes;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.lib.util.crafting.IngredientWithCount;
import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.ThermalCore;

import java.util.ArrayList;
import java.util.List;

public abstract class ThermalFuel extends SerializableRecipe {

    public final List<IngredientWithCount> inputItems = new ArrayList<>();
    public final List<FluidIngredient> inputFluids = new ArrayList<>();

    public int energy;

    protected ThermalFuel(int energy, List<IngredientWithCount> inputItems, List<FluidIngredient> inputFluids) {

        if ((inputItems == null || inputItems.isEmpty()) && (inputFluids == null || inputFluids.isEmpty())) {
            ThermalCore.LOG.warn("Invalid Thermal Series fuel! Please check your datapacks!");
        }
        this.energy = energy;

        if (inputItems != null) {
            this.inputItems.addAll(inputItems);
        }
        if (inputFluids != null) {
            this.inputFluids.addAll(inputFluids);
        }
        trim();
    }

    private void trim() {

        ((ArrayList<IngredientWithCount>) this.inputItems).trimToSize();
        ((ArrayList<FluidIngredient>) this.inputFluids).trimToSize();
    }

    // region GETTERS
    public List<IngredientWithCount> getInputItems() {

        return inputItems;
    }

    public List<FluidIngredient> getInputFluids() {

        return inputFluids;
    }

    public int getEnergy() {

        return energy;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // ThermalFuel can be crafted in any dimension
    }
    // endregion
}
