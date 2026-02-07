package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.util.managers.device.PotionDiffuserManager;
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
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.POTION_DIFFUSER_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.POTION_DIFFUSER_BOOST;

public class PotionDiffuserBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected int amplifier;
    protected float durationMod;
    protected int cycles;

    public PotionDiffuserBoost(Ingredient inputItem, int amplifier, float durationMod, int cycles) {

        this.ingredient = inputItem;
        this.amplifier = amplifier;
        this.durationMod = durationMod;
        this.cycles = cycles;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return POTION_DIFFUSER_BOOST_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {

        return true; // PotionDiffuserBoost can be crafted in any dimension
    }

    @Override
    public RecipeType<?> getType() {

        return POTION_DIFFUSER_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public int getAmplifier() {

        return amplifier;
    }

    public float getDurationMod() {

        return durationMod;
    }

    public int getCycles() {

        return cycles;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<PotionDiffuserBoost> {

        public static final MapCodec<PotionDiffuserBoost> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                Codec.INT.optionalFieldOf(AMPLIFIER, 0).forGetter(recipe -> recipe.amplifier),
                Codec.FLOAT.optionalFieldOf(DURATION_MOD, 0.0F).forGetter(recipe -> recipe.durationMod),
                Codec.INT.optionalFieldOf(CYCLES, PotionDiffuserManager.instance().getDefaultEnergy())
                        .forGetter(recipe -> recipe.cycles))
                .apply(builder, PotionDiffuserBoost::new));

        @Override
        public MapCodec<PotionDiffuserBoost> codec() {

            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PotionDiffuserBoost> streamCodec() {
            return StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    PotionDiffuserBoost::getIngredient,
                    ByteBufCodecs.INT,
                    PotionDiffuserBoost::getAmplifier,
                    ByteBufCodecs.FLOAT,
                    PotionDiffuserBoost::getDurationMod,
                    ByteBufCodecs.INT,
                    PotionDiffuserBoost::getCycles,
                    PotionDiffuserBoost::new);
        }
    }
    // endregion
}
