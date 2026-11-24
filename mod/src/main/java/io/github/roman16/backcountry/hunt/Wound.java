package io.github.roman16.backcountry.hunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

public record Wound(long bleedsOutAt, Optional<UUID> hunter, List<ItemStack> arrows) {
    public static final Wound NONE = new Wound(0, Optional.empty(), List.of());
    public static final Codec<Wound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("bleeds_out_at").forGetter(Wound::bleedsOutAt),
            UUIDUtil.CODEC.optionalFieldOf("hunter").forGetter(Wound::hunter),
            ItemStack.CODEC.listOf().optionalFieldOf("arrows", List.of()).forGetter(Wound::arrows)).apply(instance, Wound::new));

    public boolean bleeding() {
        return bleedsOutAt > 0;
    }

    public Wound bleed(long until, Optional<UUID> by) {
        return new Wound(bleeding() ? Math.min(bleedsOutAt, until) : until, by.isPresent() ? by : hunter, arrows);
    }

    public Wound pierced(ItemStack arrow) {
        List<ItemStack> held = new ArrayList<>(arrows);
        held.add(arrow.copyWithCount(1));
        return new Wound(bleedsOutAt, hunter, List.copyOf(held));
    }
}
