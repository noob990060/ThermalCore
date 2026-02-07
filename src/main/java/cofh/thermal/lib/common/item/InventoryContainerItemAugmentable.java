package cofh.thermal.lib.common.item;

import cofh.core.common.item.IAugmentableItem;
import cofh.core.common.item.InventoryContainerItem;
import cofh.core.util.helpers.AugmentDataHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntSupplier;

import static cofh.core.util.helpers.AugmentableHelper.getPropertyWithDefault;
import static cofh.core.util.helpers.AugmentableHelper.setAttributeFromAugmentMax;
import static cofh.lib.util.constants.NBTTags.*;

public class InventoryContainerItemAugmentable extends InventoryContainerItem implements IAugmentableItem {

    protected IntSupplier numSlots = () -> 0;
    protected BiPredicate<ItemStack, List<ItemStack>> augValidator = (e, f) -> true;

    public InventoryContainerItemAugmentable(Properties builder, int slots) {

        super(builder, slots);
    }

    public InventoryContainerItemAugmentable setNumSlots(IntSupplier numSlots) {

        this.numSlots = numSlots;
        return this;
    }

    public InventoryContainerItemAugmentable setAugValidator(BiPredicate<ItemStack, List<ItemStack>> augValidator) {

        this.augValidator = augValidator;
        return this;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {

        return Math.round(super.getEnchantmentValue(stack) * getBaseMod(stack));
    }

    protected float getBaseMod(ItemStack stack) {

        return getPropertyWithDefault(stack, TAG_AUGMENT_BASE_MOD, 1.0F);
    }

    protected void setAttributesFromAugment(ItemStack container, CompoundTag augmentData) {

        CompoundTag nbt = container.has(DataComponents.CUSTOM_DATA) 
                ? container.get(DataComponents.CUSTOM_DATA).copyTag()
                : new CompoundTag();
        CompoundTag subTag = nbt.getCompound(TAG_PROPERTIES);
        if (subTag.isEmpty()) {
            return;
        }
        setAttributeFromAugmentMax(subTag, augmentData, TAG_AUGMENT_BASE_MOD);
        setAttributeFromAugmentMax(subTag, augmentData, TAG_AUGMENT_ITEM_STORAGE);
        setAttributeFromAugmentMax(subTag, augmentData, TAG_AUGMENT_ITEM_CREATIVE);
        nbt.put(TAG_PROPERTIES, subTag);
        container.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    protected void tooltipDelegate(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        // Default implementation - can be overridden by subclasses
    }

    // region IInventoryContainerItem
    @Override
    public int getContainerSlots(ItemStack container) {

        float base = getPropertyWithDefault(container, TAG_AUGMENT_BASE_MOD, 1.0F);
        float mod = getPropertyWithDefault(container, TAG_AUGMENT_ITEM_STORAGE, 1.0F);
        return Math.round(slots * mod * base);
    }
    // endregion

    // region IAugmentableItem
    @Override
    public int getAugmentSlots(ItemStack augmentable) {

        return numSlots.getAsInt();
    }

    @Override
    public boolean validAugment(ItemStack augmentable, ItemStack augment, List<ItemStack> augments) {

        return augValidator.test(augment, augments);
    }

    @Override
    public void updateAugmentState(ItemStack container, List<ItemStack> augments) {

        CompoundTag nbt = container.has(DataComponents.CUSTOM_DATA) 
                ? container.get(DataComponents.CUSTOM_DATA).copyTag()
                : new CompoundTag();
        nbt.put(TAG_PROPERTIES, new CompoundTag());
        container.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        
        for (ItemStack augment : augments) {
            CompoundTag augmentData = AugmentDataHelper.getAugmentData(augment);
            if (augmentData == null) {
                continue;
            }
            setAttributesFromAugment(container, augmentData);
        }
    }
    // endregion
}
