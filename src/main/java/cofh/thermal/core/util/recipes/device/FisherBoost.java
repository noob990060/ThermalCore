package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.FISHER_BOOST_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.FISHER_BOOST;

public class FisherBoost extends SerializableRecipe {

    protected final Ingredient ingredient;

    protected final ResourceLocation lootTable;
    protected final float outputMod;
    protected final float useChance;

    public FisherBoost(Ingredient inputItem, ResourceLocation lootTable, float outputMod, float useChance) {

        this.ingredient = inputItem;
        this.lootTable = lootTable;
        this.outputMod = outputMod;
        this.useChance = useChance;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return FISHER_BOOST_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {

        return true; // FisherBoost can be crafted in any dimension
    }

    @Override
    public RecipeType<?> getType() {

        return FISHER_BOOST.get();
    }

    // region GETTERS
    public Ingredient getIngredient() {

        return ingredient;
    }

    public ResourceLocation getLootTable() {

        return lootTable;
    }

    public float getOutputMod() {

        return outputMod;
    }

    public float getUseChance() {

        return useChance;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<FisherBoost> {

        public static final MapCodec<FisherBoost> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf(INGREDIENT).forGetter(recipe -> recipe.ingredient),
                        ResourceLocation.CODEC.optionalFieldOf(LOOT_TABLE, BuiltInLootTables.FISHING_FISH.location()).forGetter(recipe -> recipe.lootTable),
                        Codec.FLOAT.optionalFieldOf(OUTPUT_MOD, 1.0F).forGetter(recipe -> recipe.outputMod),
                        Codec.FLOAT.optionalFieldOf(USE_CHANCE, 1.0F).forGetter(recipe -> recipe.useChance)
                ).apply(builder, FisherBoost::new)
        );

        @Override
        public MapCodec<FisherBoost> codec() {

            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FisherBoost> streamCodec() {
            return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                FisherBoost::getIngredient,
                ResourceLocation.STREAM_CODEC,
                FisherBoost::getLootTable,
                ByteBufCodecs.FLOAT,
                FisherBoost::getOutputMod,
                ByteBufCodecs.FLOAT,
                FisherBoost::getUseChance,
                FisherBoost::new
            );
        }
        // endregion
    }
    // endregion
}
