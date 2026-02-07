package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.util.managers.device.TreeExtractorManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.TREE_EXTRACTOR_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_BOOST;

public class TreeExtractorBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected float outputMod;
    protected int cycles;

    public TreeExtractorBoost(Ingredient inputItem, float outputMod, int cycles) {

        this.ingredient = inputItem;
        this.outputMod = outputMod;
        this.cycles = cycles;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return TREE_EXTRACTOR_BOOST_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {

        return true; // TreeExtractorBoost can be crafted in any dimension
    }

    @Override
    public RecipeType<?> getType() {

        return TREE_EXTRACTOR_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public float getOutputMod() {

        return outputMod;
    }

    public int getCycles() {

        return cycles;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<TreeExtractorBoost> {

        public static final MapCodec<TreeExtractorBoost> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                        Codec.FLOAT.optionalFieldOf(OUTPUT_MOD, 1.0F).forGetter(recipe -> recipe.outputMod),
                        Codec.INT.optionalFieldOf(CYCLES, TreeExtractorManager.instance().getDefaultEnergy()).forGetter(recipe -> recipe.cycles)
                ).apply(builder, TreeExtractorBoost::new)
        );

        @Override
        public MapCodec<TreeExtractorBoost> codec() {

            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TreeExtractorBoost> streamCodec() {
            return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                TreeExtractorBoost::getIngredient,
                ByteBufCodecs.FLOAT,
                TreeExtractorBoost::getOutputMod,
                ByteBufCodecs.INT,
                TreeExtractorBoost::getCycles,
                TreeExtractorBoost::new
            );
        }
        // endregion
    }
    // endregion
}
