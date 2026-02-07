package cofh.thermal.core.compat.jei.device;

import cofh.core.util.helpers.RenderHelper;
import cofh.thermal.core.client.gui.device.DeviceHiveExtractorScreen;
import cofh.thermal.core.util.recipes.device.HiveExtractorMapping;
import cofh.thermal.lib.compat.jei.Drawables;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

import java.util.List;

import static cofh.lib.util.Constants.TANK_MEDIUM;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.compat.jei.TCoreJeiPlugin.defaultFluidTooltip;
import static cofh.thermal.core.compat.jei.TCoreJeiPlugin.tankSize;
import static cofh.thermal.lib.util.ThermalIDs.ID_DEVICE_HIVE_EXTRACTOR;

public class HiveExtractorCategory implements IRecipeCategory<RecipeHolder<HiveExtractorMapping>> {

    protected final RecipeType<RecipeHolder<HiveExtractorMapping>> type;
    protected IDrawable background;
    protected IDrawable icon;
    protected Component name;

    protected IDrawableStatic tankBackground;
    protected IDrawableStatic tankOverlay;
    protected IDrawableStatic progressFluidBackground;
    protected IDrawableAnimated progressFluid;

    public HiveExtractorCategory(IGuiHelper guiHelper, ItemStack icon, RecipeType<RecipeHolder<HiveExtractorMapping>> type) {

        this.type = type;
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);

        background = guiHelper.drawableBuilder(DeviceHiveExtractorScreen.TEXTURE, 86, 11, 80, 62)
                .addPadding(0, 0, 16, 68)
                .build();
        name = getTextComponent(BLOCKS.get(ID_DEVICE_HIVE_EXTRACTOR).getDescriptionId());

        tankBackground = Drawables.getDrawables(guiHelper).getTank(Drawables.TANK_MEDIUM);
        tankOverlay = Drawables.getDrawables(guiHelper).getTankOverlay(Drawables.TANK_MEDIUM);
        progressFluidBackground = Drawables.getDrawables(guiHelper).getProgressFill(Drawables.PROGRESS_DROP);
        progressFluid = guiHelper.createAnimatedDrawable(Drawables.getDrawables(guiHelper).getProgress(Drawables.PROGRESS_DROP), 100, IDrawableAnimated.StartDirection.LEFT, true);
    }

    // region IRecipeCategory
    @Override
    public RecipeType<RecipeHolder<HiveExtractorMapping>> getRecipeType() {

        return type;
    }

    @Override
    public Component getTitle() {

        return name;
    }

    @Override
    public IDrawable getBackground() {

        return background;
    }

    @Override
    public IDrawable getIcon() {

        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<HiveExtractorMapping> recipe, IFocusGroup focuses) {
        HiveExtractorMapping mapping = recipe.value();
        
        // Create ItemStack for the hive block
        Block hiveBlock = mapping.getHive();
        ItemStack hiveStack = hiveBlock.asItem().getDefaultInstance();
        
        // Get the output item and fluid
        ItemStack outputItem = mapping.getItem();
        var outputFluid = mapping.getFluid();

        // Add hive input slot
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 23).addItemStack(hiveStack);

        // Add item output slot if not empty
        if (!outputItem.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 23).addItemStack(outputItem);
        }

        // Add fluid output slot if not empty
        if (!outputFluid.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 5)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(outputFluid))
                    .setFluidRenderer(tankSize(TANK_MEDIUM), false, 16, 40)
                    .setOverlay(tankOverlay, 0, 0)
                    .addTooltipCallback(defaultFluidTooltip());
        }
    }

    @Override
    public void draw(RecipeHolder<HiveExtractorMapping> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {

        tankBackground.draw(guiGraphics, 115, 4);

        HiveExtractorMapping mapping = recipe.value();
        var fluid = mapping.getFluid();
        
        if (!fluid.isEmpty()) {
            RenderHelper.drawFluid(guiGraphics, 78, 18, fluid, 24, 16);
            progressFluidBackground.draw(guiGraphics, 78, 18);
            progressFluid.draw(guiGraphics, 78, 18);
        }
    }

}
