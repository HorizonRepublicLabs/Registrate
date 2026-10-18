package com.tterrag.registrate.providers.loot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RegistrateLootTableProvider implements SingleRegistryBootstrap<LootTable> {

    public interface LootType<T extends RegistrateLootTables> {

        LootType<RegistrateBlockLootTables> BLOCK = register("block", LootContextParamSets.BLOCK, RegistrateBlockLootTables::new);
        LootType<RegistrateEntityLootTables> ENTITY = register("entity", LootContextParamSets.ENTITY, RegistrateEntityLootTables::new);

        T getLootCreator(LootTableSubProvider.Context output, AbstractRegistrate<?> parent, Consumer<T> callback);

        ContextKeySet getLootSet();

        static <T extends RegistrateLootTables> LootType<T> register(String name, ContextKeySet set, TriFunction<LootTableSubProvider.Context, AbstractRegistrate<?>, Consumer<T>, T> factory) {
            LootType<T> type = new LootType<>() {
                @Override
                public T getLootCreator(LootTableSubProvider.Context output, AbstractRegistrate<?> parent, Consumer<T> callback) {
                    return factory.apply(output, parent, callback);
                }

                @Override
                public ContextKeySet getLootSet() {
                    return set;
                }
            };
            LOOT_TYPES.put(name, type);
            return type;
        }
    }

    private static final Map<String, LootType<?>> LOOT_TYPES = new HashMap<>();

    private final AbstractRegistrate<?> parent;

    private final Multimap<LootType<?>, Consumer<? super RegistrateLootTables>> specialLootActions = HashMultimap.create();
    private final Multimap<ContextKeySet, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>>> lootActions = HashMultimap.create();

    private @Nullable BootstrapContext<LootTable> context;

    public RegistrateLootTableProvider(AbstractRegistrate<?> parent) {
        this.parent = parent;
    }

    public <T> HolderGetter<T> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        if (context == null) throw new IllegalStateException("Cannot look up registries outside of loot table generation");
        return context.lookup(key);
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return lookup(key.registryKey()).getOrThrow(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends RegistrateLootTables> void addLootAction(LootType<T> type, NonNullConsumer<T> action) {
        this.specialLootActions.put(type, (Consumer<RegistrateLootTables>) action);
    }

    public void addLootAction(ContextKeySet set, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>> action) {
        this.lootActions.put(set, action);
    }

    @Override
    public void run(BootstrapContext<LootTable> context) {
        this.context = context;
        try {
            specialLootActions.clear();
            lootActions.clear();
            parent.genData(ProviderType.LOOT, this);
            List<LootTableProvider.SubProviderEntry> entries = new ArrayList<>();
            for (LootType<?> type : LOOT_TYPES.values()) {
                entries.add(new LootTableProvider.SubProviderEntry(output -> type.getLootCreator(output, parent, cons -> specialLootActions.get(type).forEach(c -> c.accept(cons))), type.getLootSet()));
            }
            for (ContextKeySet set : lootActions.keySet()) {
                entries.add(new LootTableProvider.SubProviderEntry(output -> () -> lootActions.get(set).forEach(a -> a.accept(output::accept)), set));
            }
            new LootTableProvider(Set.of(), entries).run(context);
        } finally {
            this.context = null;
        }
    }
}
