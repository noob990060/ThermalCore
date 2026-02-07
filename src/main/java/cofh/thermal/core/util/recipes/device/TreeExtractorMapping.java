package cofh.thermal.core.util.recipes.device;

import cofh.lib.common.block.BlockIngredient;
import cofh.lib.util.recipes.JsonMapCodec;
import cofh.lib.util.recipes.SerializableRecipe;
import cofh.thermal.core.ThermalCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;

import static cofh.lib.util.recipes.RecipeJsonUtils.*;
import static cofh.thermal.core.init.registries.TCoreRecipeSerializers.TREE_EXTRACTOR_SERIALIZER;
import static cofh.thermal.core.init.registries.TCoreRecipeTypes.TREE_EXTRACTOR_MAPPING;

public class TreeExtractorMapping extends SerializableRecipe {

    protected final Block sapling;
    protected final BlockIngredient trunk;
    protected final BlockIngredient leaves;
    protected final FluidStack fluid;
    protected final int minHeight;
    protected final int maxHeight;
    protected final int minLeaves;
    protected final int maxLeaves;

    public TreeExtractorMapping(BlockIngredient trunk, BlockIngredient leaves, Block sapling, FluidStack fluid,
            int minHeight, int maxHeight, int minLeaves, int maxLeaves) {

        this.trunk = trunk;
        this.leaves = leaves;
        this.sapling = sapling;
        this.fluid = fluid;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minLeaves = minLeaves;
        this.maxLeaves = maxLeaves;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {

        return TREE_EXTRACTOR_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {

        return true; // TreeExtractorMapping can be crafted in any dimension
    }

    @Override
    public RecipeType<?> getType() {

        return TREE_EXTRACTOR_MAPPING.get();
    }

    // region GETTERS
    public BlockIngredient getTrunk() {

        return trunk;
    }

    public BlockIngredient getLeaves() {

        return leaves;
    }

    public Block getSapling() {

        return sapling;
    }

    public FluidStack getFluid() {

        return fluid;
    }

    public int getMinLeaves() {

        return minLeaves;
    }

    public int getMaxLeaves() {

        return maxLeaves;
    }

    public int getMinHeight() {

        return minHeight;
    }

    public int getMaxHeight() {

        return maxHeight;
    }
    // endregion

    // region SERIALIZER
    public static class Serializer implements RecipeSerializer<TreeExtractorMapping> {

        public Serializer() {
            System.out.println("[TREE_EXTRACTOR DEBUG] TreeExtractorMapping.Serializer instantiated!");
        }

        @Override
        public MapCodec<TreeExtractorMapping> codec() {
            System.out.println("[TREE_EXTRACTOR DEBUG] TreeExtractorMapping.Serializer.codec() called! This means recipe parsing is happening!");
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
        public StreamCodec<RegistryFriendlyByteBuf, TreeExtractorMapping> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {
                        recipe.trunk.toNetwork(buf);
                        recipe.leaves.toNetwork(buf);
                        buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(recipe.sapling));
                        FluidStack.STREAM_CODEC.encode(buf, recipe.fluid);
                        buf.writeInt(recipe.minHeight);
                        buf.writeInt(recipe.maxHeight);
                        buf.writeInt(recipe.minLeaves);
                        buf.writeInt(recipe.maxLeaves);
                    },
                    buf -> {
                        BlockIngredient trunk = BlockIngredient.fromNetwork(buf);
                        BlockIngredient leaves = BlockIngredient.fromNetwork(buf);
                        Block sapling = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
                        FluidStack fluid = FluidStack.STREAM_CODEC.decode(buf);
                        int minHeight = buf.readInt();
                        int maxHeight = buf.readInt();
                        int minLeaves = buf.readInt();
                        int maxLeaves = buf.readInt();
                        return new TreeExtractorMapping(trunk, leaves, sapling, fluid, minHeight, maxHeight, minLeaves,
                                maxLeaves);
                    });
        }

        protected TreeExtractorMapping fromJson(JsonObject json) {
            System.out.println("[TREE_EXTRACTOR DEBUG] TreeExtractorMapping.Serializer.fromJson() called with JSON: " + json);
            
            BlockIngredient trunk = BlockIngredient.EMPTY;
            BlockIngredient leaves = BlockIngredient.EMPTY;
            Block sapling = Blocks.AIR;
            FluidStack fluid = FluidStack.EMPTY;
            int minHeight = 3;
            int maxHeight = 3;
            int minLeaves = 3;
            int maxLeaves = 3;

            if (json.has(TRUNK)) {
                trunk = getAsBlockIngredient(json, TRUNK);
            }

            if (json.has(LEAVES)) {
                leaves = getAsBlockIngredient(json, LEAVES);
            } else if (json.has(LEAF)) {
                leaves = getAsBlockIngredient(json, LEAF);
            }

            if (json.has(SAPLING)) {
                sapling = parseBlock(json.get(SAPLING));
            }

            if (json.has(RESULT)) {
                fluid = parseFluidStack(json.get(RESULT));
            } else if (json.has(FLUID)) {
                fluid = parseFluidStack(json.get(FLUID));
            }

            if (json.has(MIN_HEIGHT)) {
                minHeight = json.get(MIN_HEIGHT).getAsInt();
            }
            if (json.has(MAX_HEIGHT)) {
                maxHeight = json.get(MAX_HEIGHT).getAsInt();
            }
            if (json.has(MIN_LEAVES)) {
                minLeaves = json.get(MIN_LEAVES).getAsInt();
            }
            if (json.has(MAX_LEAVES)) {
                maxLeaves = json.get(MAX_LEAVES).getAsInt();
            }
            
            return new TreeExtractorMapping(trunk, leaves, sapling, fluid, minHeight, maxHeight, minLeaves, maxLeaves);
        }

        protected JsonObject toJson(TreeExtractorMapping recipe) {
            JsonObject json = new JsonObject();
            
            if (recipe.trunk != BlockIngredient.EMPTY) {
                json.add(TRUNK, recipe.trunk.toJson());
            }
            if (recipe.leaves != BlockIngredient.EMPTY) {
                json.add(LEAVES, recipe.leaves.toJson());
            }
            if (recipe.sapling != Blocks.AIR) {
                json.addProperty(SAPLING, BuiltInRegistries.BLOCK.getKey(recipe.sapling).toString());
            }
            if (!recipe.fluid.isEmpty()) {
                JsonObject fluidJson = new JsonObject();
                fluidJson.addProperty("fluid", BuiltInRegistries.FLUID.getKey(recipe.fluid.getFluid()).toString());
                fluidJson.addProperty("amount", recipe.fluid.getAmount());
                json.add(RESULT, fluidJson);
            }
            if (recipe.minHeight != 3) {
                json.addProperty(MIN_HEIGHT, recipe.minHeight);
            }
            if (recipe.maxHeight != 3) {
                json.addProperty(MAX_HEIGHT, recipe.maxHeight);
            }
            if (recipe.minLeaves != 3) {
                json.addProperty(MIN_LEAVES, recipe.minLeaves);
            }
            if (recipe.maxLeaves != 3) {
                json.addProperty(MAX_LEAVES, recipe.maxLeaves);
            }
            
            return json;
        }
    }
    // endregion
}
