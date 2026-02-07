package cofh.thermal.core.util.recipes.device;

import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.util.managers.device.RockGenManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.ROCK_GEN_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.ROCK_GEN_MAPPING;

public class RockGenMapping extends SerializableRecipe {

    protected final int time;
    protected final Block below;
    protected final Block adjacent;
    protected final ItemStack result;

    public RockGenMapping(int time, Block below, Block adjacent, ItemStack result) {

        this.time = time;
        this.below = below;
        this.adjacent = adjacent;
        this.result = result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return ROCK_GEN_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {

        return true; // RockGenMapping can be crafted in any dimension
    }

    @Override
    public RecipeType<?> getType() {

        return ROCK_GEN_MAPPING.get();
    }

    // region GETTERS
    public int getTime() {

        return time;
    }

    public Block getBelow() {

        return below;
    }

    public Block getAdjacent() {

        return adjacent;
    }

    public ItemStack getResult() {

        return result;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<RockGenMapping> {

        public static final MapCodec<RockGenMapping> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                        Codec.INT.optionalFieldOf("time", RockGenManager.instance().getDefaultEnergy()).forGetter(recipe -> recipe.time),
                        BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("below", Blocks.AIR).forGetter(recipe -> recipe.below),
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("adjacent").forGetter(recipe -> recipe.adjacent),
                        ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
                ).apply(builder, RockGenMapping::new)
        );

        @Override
        public MapCodec<RockGenMapping> codec() {

            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RockGenMapping> streamCodec() {
            StreamCodec<RegistryFriendlyByteBuf, Block> blockCodec = StreamCodec.of(
                (buf, block) -> buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block)),
                buf -> BuiltInRegistries.BLOCK.get(buf.readResourceLocation())
            );
            
            return StreamCodec.composite(
                ByteBufCodecs.INT,
                RockGenMapping::getTime,
                blockCodec,
                RockGenMapping::getBelow,
                blockCodec,
                RockGenMapping::getAdjacent,
                ItemStack.STREAM_CODEC,
                RockGenMapping::getResult,
                RockGenMapping::new
            );
        }
        // endregion
    }
    // endregion
}
