package io.github.roman16.backcountry.guide;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record Thing(Predicate<ItemStack> test, Supplier<ItemStack> icon) {
    public static Thing of(Supplier<? extends ItemLike> item) {
        return new Thing(stack -> stack.is(item.get().asItem()), () -> new ItemStack(item.get()));
    }

    public static Thing of(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        return new Thing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(key), () -> new ItemStack(BuiltInRegistries.ITEM.get(key)));
    }

    public static Thing of(TagKey<Item> tag) {
        return new Thing(stack -> stack.is(tag),
                () -> BuiltInRegistries.ITEM.getTag(tag).flatMap(set -> set.stream().findFirst()).map(holder -> new ItemStack(holder)).orElse(ItemStack.EMPTY));
    }

    public static Thing any(Thing... things) {
        return new Thing(stack -> Arrays.stream(things).anyMatch(thing -> thing.matches(stack)), things[0].icon());
    }

    public static Thing with(Supplier<? extends DataComponentType<?>> component, Supplier<? extends ItemLike> icon) {
        return new Thing(stack -> stack.has(component.get()), () -> new ItemStack(icon.get()));
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && test.test(stack);
    }

    public ItemStack stack() {
        return icon.get();
    }
}
