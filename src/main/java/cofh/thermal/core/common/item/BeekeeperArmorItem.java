package cofh.thermal.core.common.item;

import cofh.core.common.event.ArmorEvents;
import cofh.core.common.item.ArmorItemCoFH;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;

public class BeekeeperArmorItem extends ArmorItemCoFH {

    public BeekeeperArmorItem(Holder<ArmorMaterial> pMaterial, ArmorItem.Type pType, Item.Properties pProperties) {

        super(pMaterial, pType, pProperties);

        ArmorEvents.registerStingResistArmor(this, RESISTANCE_RATIO[getType().getSlot().getIndex()]);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.beekeeper_armor").withStyle(ChatFormatting.GOLD));
    }

}
