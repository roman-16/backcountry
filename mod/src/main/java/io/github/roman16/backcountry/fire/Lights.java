package io.github.roman16.backcountry.fire;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Lights extends SavedData {
    private static final String NAME = "backcountry_lights";
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final Map<UUID, Long> HELD_CLOCKS = new HashMap<>();

    private final Map<BlockPos, Double> hours = new HashMap<>();
    private final Map<BlockPos, Long> clocks = new HashMap<>();

    public enum Kind {
        TORCH(() -> Items.TORCH, () -> Items.CHARCOAL, "message.backcountry.torch"),
        LANTERN(() -> Items.LANTERN, () -> Things.RENDERED_FAT.get(), "message.backcountry.lantern");

        private final Supplier<Item> item;
        private final Supplier<Item> fuel;
        private final String message;

        Kind(Supplier<Item> item, Supplier<Item> fuel, String message) {
            this.item = item;
            this.fuel = fuel;
            this.message = message;
        }

        public Item item() {
            return item.get();
        }

        public Item fuel() {
            return fuel.get();
        }

        public double fullHours() {
            Balance.Lights lights = Balance.get().lights();
            return this == TORCH ? lights.torchHours() : lights.lanternHours();
        }

        public static Optional<Kind> of(BlockState state) {
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                return Optional.of(TORCH);
            }
            if (state.is(Blocks.LANTERN) || state.is(Things.EMPTY_LANTERN.get())) {
                return Optional.of(LANTERN);
            }
            return Optional.empty();
        }

        public static Optional<Kind> of(ItemStack stack) {
            if (stack.is(Items.TORCH)) {
                return Optional.of(TORCH);
            }
            if (stack.is(Items.LANTERN)) {
                return Optional.of(LANTERN);
            }
            return Optional.empty();
        }
    }

    public static Lights get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Lights::new, Lights::load, null), NAME);
    }

    public static double left(ItemStack stack) {
        return Kind.of(stack).map(kind -> (double) stack.getOrDefault(Things.BURN.get(), (float) kind.fullHours())).orElse(0.0);
    }

    public static Optional<Double> left(MinecraftServer server, BlockPos pos) {
        return Optional.ofNullable(get(server).hours.get(pos));
    }

    public static void light(MinecraftServer server, BlockPos pos, double hoursLeft) {
        Lights lights = get(server);
        lights.hours.put(pos.immutable(), hoursLeft);
        lights.clocks.remove(pos);
        lights.setDirty();
    }

    private static Lights load(CompoundTag tag, HolderLookup.Provider provider) {
        Lights lights = new Lights();
        for (Tag entry : tag.getList("lights", Tag.TAG_COMPOUND)) {
            CompoundTag light = (CompoundTag) entry;
            lights.hours.put(BlockPos.of(light.getLong("pos")), light.getDouble("hours"));
        }
        return lights;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        hours.forEach((pos, left) -> {
            CompoundTag light = new CompoundTag();
            light.putLong("pos", pos.asLong());
            light.putDouble("hours", left);
            list.add(light);
        });
        tag.put("lights", list);
        return tag;
    }

    @SubscribeEvent
    static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Kind.of(event.getPlacedBlock()).filter(kind -> !event.getPlacedBlock().is(Things.EMPTY_LANTERN.get())).ifPresent(kind -> {
            InteractionHand hand = player.getMainHandItem().is(kind.item()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack held = player.getItemInHand(hand);
            light(player.server, event.getPos(), held.is(kind.item()) ? left(held) : kind.fullHours());
            player.server.execute(() -> player.getItemInHand(hand).remove(Things.BURN.get()));
        });
    }

    @SubscribeEvent
    static void used(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        Optional<Kind> found = Kind.of(state);
        if (found.isEmpty()) {
            return;
        }
        Kind kind = found.get();
        ItemStack held = event.getItemStack();
        boolean refills = held.is(kind.fuel());
        if (!refills && !held.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (refills) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            refill(player.serverLevel(), event.getPos());
        }
        tell(player, kind, state.is(Things.EMPTY_LANTERN.get()) && !refills ? Optional.empty() : Optional.of(left(player.server, event.getPos()).orElse(kind.fullHours())));
    }

    public static void refill(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Kind.of(state).ifPresent(kind -> {
            if (state.is(Things.EMPTY_LANTERN.get())) {
                level.setBlockAndUpdate(pos, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, state.getValue(LanternBlock.HANGING))
                        .setValue(LanternBlock.WATERLOGGED, state.getValue(LanternBlock.WATERLOGGED)));
            }
            light(level.getServer(), pos, kind.fullHours());
        });
    }

    private static void tell(Player player, Kind kind, Optional<Double> left) {
        player.displayClientMessage(left.map(hours -> Component.translatable(kind.message, (int) Math.ceil(hours)))
                .orElse(Component.translatable("message.backcountry.empty_lantern", Things.RENDERED_FAT.get().getDescription())), true);
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % STEP_TICKS != 0) {
            return;
        }
        Lights lights = get(event.getServer());
        long now = level.getDayTime();
        Map<BlockPos, Double> burning = new HashMap<>();
        Iterator<Map.Entry<BlockPos, Double>> entries = lights.hours.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BlockPos, Double> entry = entries.next();
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                lights.clocks.remove(pos);
                continue;
            }
            BlockState state = level.getBlockState(pos);
            Optional<Kind> kind = Kind.of(state).filter(found -> !state.is(Things.EMPTY_LANTERN.get()));
            if (kind.isEmpty()) {
                entries.remove();
                lights.clocks.remove(pos);
                lights.setDirty();
                continue;
            }
            Long clock = lights.clocks.put(pos, now);
            if (clock != null && now > clock) {
                burning.put(pos, (now - clock) / TICKS_AN_HOUR);
            }
        }
        burning.forEach((pos, hours) -> burn(level, pos, hours));
    }

    public static void burn(ServerLevel level, BlockPos pos, double hours) {
        Lights lights = get(level.getServer());
        Double before = lights.hours.get(pos);
        BlockState state = level.getBlockState(pos);
        Optional<Kind> kind = Kind.of(state).filter(found -> !state.is(Things.EMPTY_LANTERN.get()));
        if (before == null || kind.isEmpty()) {
            return;
        }
        double left = before - hours * rate(kind.get(), level, pos);
        lights.setDirty();
        if (left > 0) {
            lights.hours.put(pos, left);
            return;
        }
        lights.hours.remove(pos);
        lights.clocks.remove(pos);
        burnOut(level, pos, state);
    }

    private static double rate(Kind kind, Level level, BlockPos pos) {
        return kind == Kind.TORCH && Fire.rainedOn(level, pos) ? Balance.get().fire().rainBurn() : 1;
    }

    private static void burnOut(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.LANTERN)) {
            level.setBlockAndUpdate(pos, Things.EMPTY_LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, state.getValue(LanternBlock.HANGING))
                    .setValue(LanternBlock.WATERLOGGED, state.getValue(LanternBlock.WATERLOGGED)));
        } else {
            level.removeBlock(pos, false);
        }
    }

    @SubscribeEvent
    static void held(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % STEP_TICKS != 0) {
            return;
        }
        long now = player.level().getDayTime();
        Long clock = HELD_CLOCKS.put(player.getUUID(), now);
        if (clock == null || !Metabolism.lives(player)) {
            return;
        }
        if (now > clock) {
            burnHeld(player, (now - clock) / TICKS_AN_HOUR);
        }
    }

    public static void burnHeld(ServerPlayer player, double hours) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            Kind.of(stack).ifPresent(kind -> burnInHand(player, hand, stack, kind, hours * rate(kind, player.level(), player.blockPosition())));
        }
    }

    @SubscribeEvent
    static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD_CLOCKS.remove(event.getEntity().getUUID());
    }

    private static void burnInHand(ServerPlayer player, InteractionHand hand, ItemStack stack, Kind kind, double hours) {
        double left = left(stack) - hours;
        if (left > 0) {
            stack.set(Things.BURN.get(), (float) left);
            return;
        }
        stack.remove(Things.BURN.get());
        if (kind == Kind.TORCH) {
            stack.shrink(1);
        } else if (stack.getCount() == 1) {
            player.setItemInHand(hand, new ItemStack(Things.EMPTY_LANTERN_ITEM.get()));
        } else {
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(new ItemStack(Things.EMPTY_LANTERN_ITEM.get()));
        }
    }

    public static final class Drop extends LootModifier {
        public static final MapCodec<Drop> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, Drop::new));

        public Drop(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            if (state == null || origin == null) {
                return loot;
            }
            Kind.of(state).ifPresent(kind -> {
                Lights lights = get(context.getLevel().getServer());
                BlockPos pos = BlockPos.containing(origin);
                Double left = lights.hours.remove(pos);
                lights.clocks.remove(pos);
                lights.setDirty();
                if (left != null && left < kind.fullHours()) {
                    loot.stream().filter(stack -> stack.is(kind.item())).forEach(stack -> stack.set(Things.BURN.get(), left.floatValue()));
                }
            });
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
