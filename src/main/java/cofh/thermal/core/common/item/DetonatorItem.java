package cofh.thermal.core.common.item;

import cofh.core.common.item.IMultiModeItem;
import cofh.core.common.item.ItemCoFH;
import cofh.core.util.ProxyUtils;
import cofh.lib.api.ITNTFactory;
import cofh.lib.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static cofh.lib.util.constants.NBTTags.TAG_PRIMED;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;

public class DetonatorItem extends ItemCoFH implements IMultiModeItem {

    protected static final Map<Block, ITNTFactory<? extends PrimedTnt>> TNT_MAP = new IdentityHashMap<>();
    protected static final int MAX_PRIMED = 16;

    public static void registerTNT(Block block, ITNTFactory<? extends PrimedTnt> factory) {

        if (block == null) {
            return;
        }
        TNT_MAP.put(block, factory);
    }

    public DetonatorItem(Properties builder) {

        super(builder);
        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("primed"), (stack, world, living, seed) -> (getMode(stack) == 0 && getPrimedCount(stack) > 0 ? 1.0F : 0.0F));
        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("armed"), (stack, world, living, seed) -> (getMode(stack) == 1 && getPrimedCount(stack) > 0 ? 1.0F : 0.0F));
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(getTextComponent("info.thermal.detonator.use." + getMode(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(getTextComponent("info.thermal.detonator.primed").withStyle(ChatFormatting.GRAY).append(getTextComponent(" " + getPrimedCount(stack) + "/" + MAX_PRIMED).withStyle(getPrimedCount(stack) <= 0 ? ChatFormatting.RED : getMode(stack) == 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN)));
        tooltip.add(getTextComponent("info.thermal.detonator.use.sneak").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(getTextComponent("info.thermal.detonator.mode." + getMode(stack)).withStyle(ChatFormatting.ITALIC));
        if (getPrimedCount(stack) > 0) {
            addModeChangeTooltip(this, stack, null, tooltip, flagIn);
        }

        super.tooltipDelegate(stack, context, tooltip, flagIn);
    }

    protected boolean useDelegate(ItemStack stack, UseOnContext context) {

        if (getMode(stack) == 0) {
            return primeTNT(stack, context.getLevel(), context.getClickedPos(), context.getPlayer());
        }
        return detonateTNT(stack, context.getLevel(), context.getPlayer());
    }

    protected boolean primeTNT(ItemStack stack, Level world, BlockPos pos, Player player) {

        if (player == null || world.isEmptyBlock(pos)) {
            return false;
        }
        if (TNT_MAP.containsKey(world.getBlockState(pos).getBlock())) {
            CompoundTag nbt = stack.has(DataComponents.CUSTOM_DATA) 
                    ? stack.get(DataComponents.CUSTOM_DATA).copyTag()
                    : new CompoundTag();
            ListTag primedTNT = nbt.getList(TAG_PRIMED, TAG_COMPOUND);
            CompoundTag tntPos = new CompoundTag();
            tntPos.putLong("pos", pos.asLong());
            
            // Check if this position is already in the list
            boolean alreadyExists = false;
            for (int i = 0; i < primedTNT.size(); i++) {
                BlockPos existingPos = BlockPos.of(primedTNT.getCompound(i).getLong("pos"));
                if (existingPos.equals(pos)) {
                    alreadyExists = true;
                    break;
                }
            }
            
            if (primedTNT.size() >= MAX_PRIMED || alreadyExists) {
                return false;
            }
            primedTNT.add(tntPos);
            nbt.put(TAG_PRIMED, primedTNT);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            return true;
        }
        return false;
    }

    protected boolean detonateTNT(ItemStack stack, Level world, Player player) {

        if (player == null) {
            return false;
        }
        if (Utils.isServerWorld(world)) {
            CompoundTag nbt = stack.has(DataComponents.CUSTOM_DATA) 
                    ? stack.get(DataComponents.CUSTOM_DATA).copyTag()
                    : new CompoundTag();
            ListTag primedTNT = nbt.getList(TAG_PRIMED, TAG_COMPOUND);
            for (int i = 0; i < primedTNT.size(); ++i) {
                BlockPos tntPos = BlockPos.of(primedTNT.getCompound(i).getLong("pos"));
                attemptDetonate(world, tntPos, player, 0);
            }
            nbt.remove(TAG_PRIMED);
            if (!nbt.isEmpty()) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            } else {
                stack.remove(DataComponents.CUSTOM_DATA);
            }
        }
        setMode(stack, 0);
        return true;
    }

    protected void attemptDetonate(Level world, BlockPos pos, Player player, int fuse) {

        if (!world.isLoaded(pos)) {
            return;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (TNT_MAP.containsKey(block)) {
            world.removeBlock(pos, false);
            PrimedTnt tnt = TNT_MAP.get(block).createTNT(world, pos.getX(), pos.getY(), pos.getZ(), player);
            tnt.setFuse(fuse);
            world.addFreshEntity(tnt);
            world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    protected int getPrimedCount(ItemStack stack) {

        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return 0;
        }
        CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        ListTag primedTNT = nbt.getList(TAG_PRIMED, TAG_COMPOUND);
        return primedTNT.size();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        return player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack) && useDelegate(stack, context) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                nbt.remove(TAG_PRIMED);
                if (!nbt.isEmpty()) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                } else {
                    stack.remove(DataComponents.CUSTOM_DATA);
                }
            }
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 0.3F);
            return InteractionResultHolder.success(stack);
        }
        if (getMode(stack) == 1 && detonateTNT(stack, world, player)) {
            player.swing(hand);
            player.playSound(SoundEvents.LEVER_CLICK, 0.4F, 1.0F);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    // region IMultiModeItem
    @Override
    public int getNumModes(ItemStack stack) {

        return getPrimedCount(stack) > 0 ? 2 : 1;
    }

    @Override
    public void onModeChange(Player player, ItemStack stack) {

        player.level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, 1.0F - 0.3F * getMode(stack));
        ProxyUtils.setOverlayMessage(player, Component.translatable("info.thermal.detonator.mode." + getMode(stack)));
    }
    // endregion
}
