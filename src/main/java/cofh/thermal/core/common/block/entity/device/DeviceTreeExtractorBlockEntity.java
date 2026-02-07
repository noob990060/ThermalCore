package cofh.thermal.core.common.block.entity.device;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.AugmentDataHelper;
import cofh.lib.api.block.entity.ITickableTile;
import cofh.lib.common.fluid.FluidStorageCoFH;
import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.core.common.inventory.device.DeviceTreeExtractorMenu;
import cofh.thermal.core.util.managers.device.TreeExtractorManager;
import cofh.thermal.core.util.recipes.device.TreeExtractorMapping;
import cofh.thermal.lib.common.block.entity.DeviceBlockEntity;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static cofh.core.client.renderer.model.ModelUtils.FLUID;
import static cofh.lib.api.StorageGroup.INPUT;
import static cofh.lib.api.StorageGroup.OUTPUT;
import static cofh.lib.util.Constants.TANK_MEDIUM;
import static cofh.lib.util.constants.BlockStatePropertiesCoFH.FACING_HORIZONTAL;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.core.init.registries.TCoreBlockEntities.DEVICE_TREE_EXTRACTOR_TILE;
import static cofh.thermal.lib.util.ThermalAugmentRules.createAllowValidator;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class DeviceTreeExtractorBlockEntity extends DeviceBlockEntity implements ITickableTile.IServerTickable {

    public static final BiPredicate<ItemStack, List<ItemStack>> AUG_VALIDATOR = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE, TAG_AUGMENT_TYPE_FLUID, TAG_AUGMENT_TYPE_FILTER);

    protected static final int LEAF_SEARCH_DIST = 6;
    protected static final Vec3i[] TRUNK_SEARCH = {Vec3i.ZERO,
            new Vec3i(1, 0, 0), new Vec3i(-1, 0, 1), new Vec3i(-1, 0, -1), new Vec3i(1, 0, -1),
            new Vec3i(1, 0, 0), new Vec3i(0, 0, 2), new Vec3i(-2, 0, 0), new Vec3i(0, 0, -2)};
    protected static int timeConstant = 500;

    protected ItemStorageCoFH inputSlot = new ItemStorageCoFH(item -> filter.valid(item) && TreeExtractorManager.instance().validBoost(item));
    protected FluidStorageCoFH outputTank = new FluidStorageCoFH(TANK_MEDIUM);

    protected boolean cached;

    protected BlockPos[] logs;
    protected BlockPos[] leaves;
    protected TreeExtractorMapping recipe;

    protected int process = timeConstant / 2;

    protected int boostCycles;
    protected int boostMax = TreeExtractorManager.instance().getDefaultEnergy();
    protected float boostMult;

    public static void setTimeConstant(int configConstant) {

        timeConstant = configConstant;
    }

    public DeviceTreeExtractorBlockEntity(BlockPos pos, BlockState state) {

        super(DEVICE_TREE_EXTRACTOR_TILE.get(), pos, state);

        inventory.addSlot(inputSlot, INPUT);

        tankInv.addTank(outputTank, OUTPUT);

        addAugmentSlots(ThermalCoreConfig.deviceAugments);
        initHandlers();

        logs = new BlockPos[0];
        leaves = new BlockPos[0];
    }

    @Override
    protected void updateValidity() {

        if (level == null || !level.isAreaLoaded(worldPosition, 1) || level.isClientSide) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Skipping update - level null/not loaded/client side");
            return;
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] Starting validity check at position: " + worldPosition);
        cached = true;
        if (isValid()) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Already have valid recipe, checking if tree still valid...");
            Predicate<BlockState> validLeaf = recipe.getLeaves();
            Predicate<BlockState> validLog = recipe.getTrunk();
            boolean leavesValid = Arrays.stream(leaves).allMatch(pos -> validLeaf.test(level.getBlockState(pos)));
            boolean logsValid = Arrays.stream(logs).allMatch(pos -> validLog.test(level.getBlockState(pos)));
            System.out.println("[TREE_EXTRACTOR DEBUG] Leaves valid: " + leavesValid + ", Logs valid: " + logsValid);
            System.out.println("[TREE_EXTRACTOR DEBUG] Found " + leaves.length + " leaves, " + logs.length + " logs");
            if (leavesValid && logsValid) {
                System.out.println("[TREE_EXTRACTOR DEBUG] Tree still valid, keeping current recipe");
                return;
            }
            System.out.println("[TREE_EXTRACTOR DEBUG] Tree structure changed, need to redetect");
        }
        Direction[] searchDirs = getSearchOrder();
        System.out.println("[TREE_EXTRACTOR DEBUG] Searching in directions: " + Arrays.toString(searchDirs));
        for (Direction dir : searchDirs) {
            BlockPos treePos = worldPosition.relative(dir);
            System.out.println("[TREE_EXTRACTOR DEBUG] Checking direction " + dir + " at position: " + treePos);
            TreeInfo info = detectTree(treePos);
            if (info != null) {
                System.out.println("[TREE_EXTRACTOR DEBUG] Found valid tree!");
                System.out.println("[TREE_EXTRACTOR DEBUG] Recipe fluid: " + info.recipe.getFluid().getFluid());
                System.out.println("[TREE_EXTRACTOR DEBUG] Tree has " + info.logs.length + " logs and " + info.leaves.length + " leaves");
                leaves = info.leaves;
                logs = info.logs;
                recipe = info.recipe;
                renderFluid = recipe.getFluid();
                return;
            }
            System.out.println("[TREE_EXTRACTOR DEBUG] No valid tree found in direction " + dir);
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] No valid trees found in any direction, clearing recipe");
        logs = new BlockPos[0];
        leaves = new BlockPos[0];
        recipe = null;
        renderFluid = FluidStack.EMPTY;
    }

    @Override
    protected void updateActiveState() {

        if (!cached) {
            updateValidity();
        }
        super.updateActiveState();
    }

    @Override
    protected boolean isValid() {

        return recipe != null;
    }

    @Override
    public void tickServer() {

        updateActiveState();

        --process;
        if (process > 0 || !isActive) {
            if (process > 0) {
                System.out.println("[TREE_EXTRACTOR DEBUG] Processing, ticks remaining: " + process);
            } else if (!isActive) {
                System.out.println("[TREE_EXTRACTOR DEBUG] Not active, skipping tick");
            }
            return;
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] Processing complete, starting extraction");
        updateValidity();
        process = getTimeConstant();
        Fluid curFluid = renderFluid.getFluid();
        System.out.println("[TREE_EXTRACTOR DEBUG] Time constant set to: " + process + " ticks");

        if (isValid()) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Valid recipe found, checking boosts");
            if (boostCycles > 0) {
                --boostCycles;
                System.out.println("[TREE_EXTRACTOR DEBUG] Using boost, cycles remaining: " + boostCycles + "/" + boostMax + ", multiplier: " + boostMult);
            } else if (!inputSlot.isEmpty()) {
                System.out.println("[TREE_EXTRACTOR DEBUG] Consuming boost item: " + inputSlot.getItemStack());
                boostCycles = TreeExtractorManager.instance().getBoostCycles(inputSlot.getItemStack());
                boostMax = boostCycles;
                boostMult = TreeExtractorManager.instance().getBoostOutputMod(inputSlot.getItemStack());
                inputSlot.consume(1);
                System.out.println("[TREE_EXTRACTOR DEBUG] New boost cycles: " + boostCycles + ", multiplier: " + boostMult);
            } else {
                System.out.println("[TREE_EXTRACTOR DEBUG] No boost available, using base multiplier");
                boostCycles = 0;
                boostMult = 1.0F;
            }
            float sizeMult = MathHelper.sqrt((float) Math.min(logs.length, recipe.getMaxHeight()) * Math.min(leaves.length, recipe.getMaxLeaves()) / (recipe.getMinHeight() * recipe.getMinLeaves()));
            System.out.println("[TREE_EXTRACTOR DEBUG] Size multiplier: " + sizeMult + " (logs: " + logs.length + "/" + recipe.getMaxHeight() + ", leaves: " + leaves.length + "/" + recipe.getMaxLeaves() + ")");
            int outputAmount = (int) (renderFluid.getAmount() * baseMod * boostMult * sizeMult);
            System.out.println("[TREE_EXTRACTOR DEBUG] Producing " + outputAmount + " mB of " + renderFluid.getFluid());
            outputTank.fill(new FluidStack(renderFluid.getFluid(), outputAmount), EXECUTE);
        } else {
            System.out.println("[TREE_EXTRACTOR DEBUG] No valid recipe, skipping extraction");
        }
        if (curFluid != renderFluid.getFluid()) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Fluid type changed, sending update packet");
            TileStatePacket.sendToClient(this);
        }
    }

    @Nonnull
    @Override
    public ModelData getModelData() {

        return ModelData.builder()
                .with(FLUID, renderFluid)
                .build();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new DeviceTreeExtractorMenu(i, level, worldPosition, inventory, player);
    }

    // region GUI
    @Override
    public int getScaledDuration(int scale) {

        return !isActive || boostCycles <= 0 || boostMax <= 0 ? 0 : scale * boostCycles / boostMax;
    }
    // endregion

    // region NETWORK
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {

        super.onDataPacket(net, pkt, lookupProvider);

        if (level != null) {
            level.getModelDataManager().requestRefresh(this);
        }
    }

    // CONTROL
    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        super.handleControlPacket(buffer);

        if (level != null) {
            level.getModelDataManager().requestRefresh(this);
        }
    }

    // GUI
    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        super.getGuiPacket(buffer);

        buffer.writeInt(boostCycles);
        buffer.writeInt(boostMax);
        buffer.writeFloat(boostMult);

        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        super.handleGuiPacket(buffer);

        boostCycles = buffer.readInt();
        boostMax = buffer.readInt();
        boostMult = buffer.readFloat();
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        super.getStatePacket(buffer);

        buffer.writeInt(process);

        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        super.handleStatePacket(buffer);

        process = buffer.readInt();

        if (level != null) {
            level.getModelDataManager().requestRefresh(this);
        }
    }
    // endregion

    // region NBT
    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider lookupProvider) {

        super.loadAdditional(nbt, lookupProvider);

        boostCycles = nbt.getInt(TAG_BOOST_CYCLES);
        boostMax = nbt.getInt(TAG_BOOST_MAX);
        boostMult = nbt.getFloat(TAG_BOOST_MULT);
        process = nbt.getInt(TAG_PROCESS);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider lookupProvider) {

        // Prevent crash when lookupProvider is null
        if (lookupProvider == null) {
            return;
        }

        super.saveAdditional(nbt, lookupProvider);

        nbt.putInt(TAG_BOOST_CYCLES, boostCycles);
        nbt.putInt(TAG_BOOST_MAX, boostMax);
        nbt.putFloat(TAG_BOOST_MULT, boostMult);
        nbt.putInt(TAG_PROCESS, process);
    }
    // endregion

    // region HELPERS
    protected int getTimeConstant() {

        if (level == null || !isValid()) {
            return timeConstant;
        }
        int count = 2;
        BlockPos base = logs[0];
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isTreeExtractor(level.getBlockState(base.relative(direction)))) {
                ++count;
            }
        }
        return count * timeConstant / 2;
    }

    protected boolean isTreeExtractor(BlockState state) {

        return state.getBlock() == this.getBlockState().getBlock();
    }

    protected Direction[] getSearchOrder() {

        Direction base = getBlockState().getValue(FACING_HORIZONTAL);
        return new Direction[]{base.getOpposite(), base.getClockWise(), base.getCounterClockWise()};
    }

    @Nullable
    protected TreeInfo detectTree(BlockPos basePos) {

        System.out.println("[TREE_EXTRACTOR DEBUG] Detecting tree at base position: " + basePos);
        BlockState base = level.getBlockState(basePos);
        System.out.println("[TREE_EXTRACTOR DEBUG] Base block state: " + base);
        
        // Debug: Show all available recipes
        System.out.println("[TREE_EXTRACTOR DEBUG] Available recipes:");
        TreeExtractorManager.instance().getRecipes().forEach(recipe -> {
            System.out.println("[TREE_EXTRACTOR DEBUG] - Recipe trunk: " + recipe.getTrunk() + ", leaves: " + recipe.getLeaves() + ", fluid: " + recipe.getFluid().getFluid());
        });
        
        // Find recipes matching trunk
        TreeExtractorMapping[] recipes = TreeExtractorManager.instance().getRecipes()
                .filter(recipe -> {
                    boolean matches = recipe.getTrunk().test(base);
                    System.out.println("[TREE_EXTRACTOR DEBUG] Testing recipe trunk " + recipe.getTrunk() + " against " + base + ": " + matches);
                    return matches;
                })
                .toArray(TreeExtractorMapping[]::new);
        System.out.println("[TREE_EXTRACTOR DEBUG] Found " + recipes.length + " recipes matching trunk block");
        if (recipes.length <= 0) {
            System.out.println("[TREE_EXTRACTOR DEBUG] No recipes match this trunk block");
            return null;
        }

        // Split recipes by growth direction
        System.out.println("[TREE_EXTRACTOR DEBUG] Trying UP direction first");
        TreeInfo result = detectTreeDirection(recipes, basePos, Direction.UP);
        if (result == null) {
            System.out.println("[TREE_EXTRACTOR DEBUG] UP direction failed, trying DOWN direction");
            result = detectTreeDirection(recipes, basePos, Direction.DOWN);
        }
        return result;
    }

    @Nullable
    protected TreeInfo detectTreeDirection(TreeExtractorMapping[] recipes, BlockPos base, Direction growth) {

        System.out.println("[TREE_EXTRACTOR DEBUG] Detecting tree direction " + growth + " from base " + base);
        // Traverse tree to find logs
        int min = level.getMinBuildHeight();
        int max = level.getMaxBuildHeight();
        List<BlockPos> logs = new ArrayList<>();
        logs.add(base.immutable());
        BlockPos.MutableBlockPos cursor = base.mutable();
        System.out.println("[TREE_EXTRACTOR DEBUG] Starting trunk traversal, build limits: " + min + " to " + max);
        scan:
        while (cursor.getY() < max && cursor.getY() > min) {
            cursor.move(growth);
            System.out.println("[TREE_EXTRACTOR DEBUG] Checking position " + cursor + " for trunk continuation");
            for (Vec3i offset : TRUNK_SEARCH) {
                cursor.move(offset);
                BlockState state = level.getBlockState(cursor);
                System.out.println("[TREE_EXTRACTOR DEBUG] Testing offset " + offset + " at " + cursor + ", state: " + state);
                TreeExtractorMapping[] matching = Arrays.stream(recipes)
                        .filter(recipe -> recipe.getTrunk().test(state))
                        .toArray(TreeExtractorMapping[]::new);
                if (matching.length > 0) {
                    System.out.println("[TREE_EXTRACTOR DEBUG] Found trunk continuation! " + matching.length + " recipes match");
                    logs.add(cursor.immutable());
                    recipes = matching;
                    continue scan;
                }
                cursor.move(-offset.getX(), -offset.getY(), -offset.getZ());
            }
            System.out.println("[TREE_EXTRACTOR DEBUG] No trunk continuation found, ending trunk search");
            break;
        }
        int height = logs.size();
        System.out.println("[TREE_EXTRACTOR DEBUG] Trunk traversal complete, found " + height + " logs");
        recipes = Arrays.stream(recipes)
                .filter(recipe -> height >= recipe.getMinHeight())
                .toArray(TreeExtractorMapping[]::new);
        System.out.println("[TREE_EXTRACTOR DEBUG] " + recipes.length + " recipes accept this height");
        if (recipes.length <= 0) {
            System.out.println("[TREE_EXTRACTOR DEBUG] No recipes accept height " + height);
            return null;
        }

        // Find number of leaves around top log
        System.out.println("[TREE_EXTRACTOR DEBUG] Starting leaf detection from top log at " + cursor);
        Reference2ReferenceMap<TreeExtractorMapping, LongList> leaves = new Reference2ReferenceArrayMap<>();
        for (TreeExtractorMapping recipe : recipes) {
            System.out.println("[TREE_EXTRACTOR DEBUG] Recipe requires min " + recipe.getMinLeaves() + " leaves, max " + recipe.getMaxLeaves());
            leaves.put(recipe, new LongArrayList(recipe.getMaxLeaves()));
        }
        cursor = logs.get(logs.size() - 1).mutable();
        LongPriorityQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(cursor.asLong());
        LongSet visited = new LongOpenHashSet(128);
        visited.add(queue.firstLong());
        int x = cursor.getX();
        int y = cursor.getY();
        int z = cursor.getZ();
        System.out.println("[TREE_EXTRACTOR DEBUG] Starting BFS leaf search within " + LEAF_SEARCH_DIST + " blocks");
        while (!queue.isEmpty()) {
            long pos = queue.dequeueLong();
            for (Direction dir : Direction.values()) {
                long adj = BlockPos.offset(pos, dir);
                if (distManhattan(adj, x, y, z) <= LEAF_SEARCH_DIST && visited.add(adj)) {
                    BlockState state = level.getBlockState(cursor.set(adj));
                    boolean valid = false;
                    for (Reference2ReferenceMap.Entry<TreeExtractorMapping, LongList> entry : leaves.reference2ReferenceEntrySet()) {
                        TreeExtractorMapping recipe = entry.getKey();
                        if (recipe.getLeaves().test(state)) {
                            LongList blocks = entry.getValue();
                            blocks.add(adj);
                            valid = true;
                            System.out.println("[TREE_EXTRACTOR DEBUG] Found leaf at " + cursor.set(adj) + " for recipe, total: " + blocks.size());
                            if (blocks.size() >= recipe.getMaxLeaves()) {
                                System.out.println("[TREE_EXTRACTOR DEBUG] Found max leaves, returning tree info");
                                return new TreeInfo(recipe, logs, blocks);
                            }
                        } else if (recipe.getTrunk().test(state)) {
                            valid = true;
                        }
                    }
                    if (valid) {
                        queue.enqueue(adj);
                    }
                }
            }
        }
        System.out.println("[TREE_EXTRACTOR DEBUG] BFS complete, checking leaf counts");
        return leaves.reference2ReferenceEntrySet().stream()
                .filter(entry -> entry.getValue().size() >= entry.getKey().getMinLeaves())
                .peek(entry -> System.out.println("[TREE_EXTRACTOR DEBUG] Recipe has " + entry.getValue().size() + " leaves (min: " + entry.getKey().getMinLeaves() + ")"))
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(entry -> new TreeInfo(entry.getKey(), logs, entry.getValue()))
                .orElse(null);
    }

    protected static int distManhattan(long pos, int x, int y, int z) {

        return Math.abs(BlockPos.getX(pos) - x) + Math.abs(BlockPos.getY(pos) - y) + Math.abs(BlockPos.getZ(pos) - z);
    }

    public record TreeInfo(TreeExtractorMapping recipe, BlockPos[] logs, BlockPos[] leaves) {

        public TreeInfo(TreeExtractorMapping recipe, Collection<BlockPos> logs, LongCollection leaves) {

            this(recipe, logs.toArray(BlockPos[]::new), leaves.longStream().limit(recipe.getMaxLeaves()).mapToObj(BlockPos::of).toArray(BlockPos[]::new));
        }

    }
    // endregion

    // region AUGMENTS
    @Override
    protected Predicate<ItemStack> augValidator() {

        return item -> AugmentDataHelper.hasAugmentData(item) && AUG_VALIDATOR.test(item, getAugmentsAsList());
    }
    // endregion
}
