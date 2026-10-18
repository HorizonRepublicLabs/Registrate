package com.tterrag.registrate.providers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.neoforged.neoforge.data.event.DatapackRegistryGatherer;
import net.minecraft.resources.ResourceKey;

import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DataProviderInitializer {

	private final List<Consumer<DatapackRegistryGatherer>> datapackEntryProviders = new ArrayList<>();

	private final Map<ProviderType<?>, ProviderType<? extends RegistrateLookupFillerProvider>> providerDependencies = new ConcurrentHashMap<>();

	public DataProviderInitializer() {
		addDependency(ProviderType.ITEM_TAGS, ProviderType.BLOCK_TAGS);
	}

	/**
	 * Adds every registered datapack registry bootstrap to the gatherer.
	 */
	public void gatherDatapackEntries(DatapackRegistryGatherer gatherer) {
		datapackEntryProviders.forEach(c -> c.accept(gatherer));
	}

	protected List<Sorted> getSortedProviders() {
		List<Sorted> ans = new ArrayList<>();
		Set<ProviderType<?>> added = new HashSet<>();
		List<Map.Entry<String, ProviderType<?>>> remain = new ArrayList<>(RegistrateDataProvider.TYPES.entrySet());
		while (!remain.isEmpty()) {
			if (!remain.removeIf(e -> {
				ProviderType<?> type = e.getValue();
				var parent = providerDependencies.get(type);
				if (parent == null || added.contains(parent)) {
					ans.add(new Sorted(e.getKey(), type, parent));
					added.add(type);
					return true;
				}
				return false;
			})) throw new IllegalStateException("Looping dependency detected: " + remain);
		}
		return ans;
	}

	public <T> DataProviderInitializer add(ResourceKey<? extends Registry<T>> registry, SingleRegistryBootstrap<T> provider) {
		datapackEntryProviders.add(g -> g.add(registry, provider));
		return this;
	}

	public DataProviderInitializer add(MultiRegistryBootstrap provider) {
		datapackEntryProviders.add(g -> g.add(provider));
		return this;
	}

	public void addDependency(ProviderType<?> dependent, ProviderType<? extends RegistrateLookupFillerProvider> parent) {
		var old = providerDependencies.put(dependent, parent);
		if (old != null) throw new IllegalStateException("Providers can have only 1 prerequisite");
	}

	public record Sorted(
			String id, ProviderType<?> type,
			@Nullable ProviderType<? extends RegistrateLookupFillerProvider> parent
	) {
	}

}
