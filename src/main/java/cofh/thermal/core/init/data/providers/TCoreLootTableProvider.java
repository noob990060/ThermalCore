package cofh.thermal.core.init.data.providers;

import cofh.lib.init.data.LootTableProviderCoFH;
import cofh.thermal.core.init.data.tables.TCoreBlockLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TCoreLootTableProvider extends LootTableProviderCoFH {

    public TCoreLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, List.of(
                new LootTableProvider.SubProviderEntry(lookup -> new TCoreBlockLootTables(lookup), LootContextParamSets.BLOCK)
        ), lookupProvider);
    }

}
