package com.tterrag.registrate.providers;

import com.tterrag.registrate.AbstractRegistrate;
import lombok.experimental.Delegate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;
import java.util.function.Consumer;

/**
 * Advancements are a datapack registry; this wraps the {@link BootstrapContext} so builders can {@code save(prov, name)} directly.
 */
public class RegistrateAdvancementProvider implements BootstrapContext<Advancement>, Consumer<AdvancementHolder> {

    private final AbstractRegistrate<?> owner;
    @Delegate
    private final BootstrapContext<Advancement> context;

    public RegistrateAdvancementProvider(AbstractRegistrate<?> owner, BootstrapContext<Advancement> context) {
        this.owner = owner;
        this.context = context;
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return context.lookup(key.registryKey()).getOrThrow(key);
    }

    public MutableComponent title(String category, String name, String title) {
        return owner.addLang("advancements", Identifier.fromNamespaceAndPath(category, name), "title", title);
    }

    public MutableComponent desc(String category, String name, String desc) {
        return owner.addLang("advancements", Identifier.fromNamespaceAndPath(category, name), "description", desc);
    }

    @Override
    public void accept(AdvancementHolder holder) {
        holder.register(context);
    }

    public void withConditions(AdvancementHolder holder, List<ICondition> conditions) {
        holder.register(context, conditions.toArray(ICondition[]::new));
    }
}
