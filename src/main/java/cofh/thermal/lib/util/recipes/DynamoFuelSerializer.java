package cofh.thermal.lib.util.recipes;

import cofh.lib.common.fluid.FluidIngredient;
import cofh.lib.util.crafting.IngredientWithCount;
import cofh.lib.util.helpers.MathHelper;
import cofh.lib.util.recipes.JsonMapCodec;
import cofh.thermal.core.ThermalCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;

public class DynamoFuelSerializer<T extends ThermalFuel> implements RecipeSerializer<T> {

    protected final int defaultEnergy;
    protected final int minEnergy;
    protected final int maxEnergy;
    protected final IFactory<T> factory;
    protected final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public DynamoFuelSerializer(IFactory<T> factory, int defaultEnergy, int minEnergy, int maxEnergy) {

        this.factory = factory;
        this.defaultEnergy = defaultEnergy;
        this.minEnergy = minEnergy;
        this.maxEnergy = maxEnergy;
        this.streamCodec = StreamCodec.of(this::toNetwork, this::fromNetwork);
    }

    @Override
    public MapCodec<T> codec() {

        return JsonMapCodec.INSTANCE
                .flatXmap(json -> {
                    try {
                        return DataResult.success(fromJson(json));
                    } catch (JsonParseException e) {
                        return DataResult.error(e::getMessage);
                    }
                }, recipe -> DataResult.success(toJson(recipe)));
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {

        return streamCodec;
    }

    protected T fromJson(JsonObject json) {

        int energy = defaultEnergy;

        ArrayList<IngredientWithCount> inputItems = new ArrayList<>();
        ArrayList<FluidIngredient> inputFluids = new ArrayList<>();

        /* INPUT */
        if (json.has(INGREDIENT)) {
            parseInputs(inputItems, inputFluids, json.get(INGREDIENT));
        } else if (json.has(INGREDIENTS)) {
            parseInputs(inputItems, inputFluids, json.get(INGREDIENTS));
        } else if (json.has(INPUT)) {
            parseInputs(inputItems, inputFluids, json.get(INPUT));
        } else if (json.has(INPUTS)) {
            parseInputs(inputItems, inputFluids, json.get(INPUTS));
        }

        /* ENERGY */
        if (json.has(ENERGY)) {
            energy = json.get(ENERGY).getAsInt();
        }
        if (json.has(ENERGY_MOD)) {
            energy *= json.get(ENERGY_MOD).getAsFloat();
        }
        if (energy < minEnergy || energy > maxEnergy) {
            energy = MathHelper.clamp(energy, minEnergy, maxEnergy);
            ThermalCore.LOG.warn("Energy value for a Dynamo Fuel was out of allowable range and has been clamped between + " + minEnergy + " and " + maxEnergy + ".");
        }
        if (inputItems.isEmpty() && inputFluids.isEmpty()) {
            // ThermalCore.LOG.warn("Invalid Thermal Series fuel! Please check your datapacks!");
        }
        return factory.create(energy, inputItems, inputFluids);
    }

    protected JsonObject toJson(T recipe) {

        return null;
    }

    @Nullable
    public T fromNetwork(RegistryFriendlyByteBuf buffer) {

        int energy = buffer.readVarInt();

        int numInputItems = buffer.readVarInt();
        ArrayList<IngredientWithCount> inputItems = new ArrayList<>(numInputItems);
        for (int i = 0; i < numInputItems; ++i) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            inputItems.add(new IngredientWithCount(ingredient, count));
        }

        int numInputFluids = buffer.readVarInt();
        ArrayList<FluidIngredient> inputFluids = new ArrayList<>(numInputFluids);
        for (int i = 0; i < numInputFluids; ++i) {
            inputFluids.add(FluidIngredient.fromNetwork(buffer));
        }
        return factory.create(energy, inputItems, inputFluids);
    }

    public void toNetwork(RegistryFriendlyByteBuf buffer, T recipe) {

        buffer.writeVarInt(recipe.energy);

        int numInputItems = recipe.inputItems.size();
        buffer.writeVarInt(numInputItems);
        for (int i = 0; i < numInputItems; ++i) {
            IngredientWithCount ingredient = recipe.inputItems.get(i);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient());
            buffer.writeVarInt(ingredient.count());
        }
        int numInputFluids = recipe.inputFluids.size();
        buffer.writeVarInt(numInputFluids);
        for (int i = 0; i < numInputFluids; ++i) {
            recipe.inputFluids.get(i).toNetwork(buffer);
        }
    }

    public interface IFactory<T extends ThermalFuel> {

        T create(int energy, List<IngredientWithCount> inputItems, List<FluidIngredient> inputFluids);

    }

}
