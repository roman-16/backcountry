package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Rooms;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.Worn;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Spoilage {
    private static final double TICKS_A_DAY = 24000;
    private static final int STEP_TICKS = 20;

    private Spoilage() {}

    public static double rate(double airC) {
        Balance.Spoilage spoilage = Balance.get().spoilage();
        double rate = Math.pow(2, (airC - spoilage.referenceC()) / spoilage.doublingC());
        return airC < spoilage.frozenBelowC() ? rate * spoilage.frozenShare() : rate;
    }

    public static Optional<Double> life(ItemStack stack) {
        Balance balance = Balance.get();
        return Nutrition.of(stack).flatMap(Nutrition::days).map(days -> days * (stack.has(Things.SMOKED.get()) ? balance.spoilage().smokedLife() : 1)
                * (stack.has(Things.DRIED.get()) ? balance.drying().life() : 1) * (stack.has(Things.SALTED.get()) ? balance.drying().saltedLife() : 1));
    }

    public static double age(ItemStack stack) {
        Age age = stack.get(Things.AGE.get());
        return age == null ? 0 : age.days();
    }

    public static boolean spoiled(ItemStack stack) {
        return life(stack).map(life -> age(stack) >= life).orElse(false);
    }

    public static Optional<Double> daysLeft(ItemStack stack, double airC) {
        return life(stack).map(life -> Math.max(0, life - age(stack)) / rate(airC));
    }

    public static boolean age(ItemStack stack, double days, double airC) {
        Fish.freeze(stack, days, airC);
        Optional<Double> life = life(stack);
        if (life.isEmpty() || stack.isEmpty() || days <= 0) {
            return false;
        }
        double aged = Math.min(life.get(), age(stack) + days * rate(airC));
        stack.set(Things.AGE.get(), new Age((float) aged));
        return true;
    }

    public static double elapsedDays(IAttachmentHolder holder, Level level) {
        long now = level.getDayTime();
        long last = holder.getData(Things.FOOD_CLOCK);
        holder.setData(Things.FOOD_CLOCK, now);
        return last == Long.MIN_VALUE ? 0 : Math.max(0, now - last) / TICKS_A_DAY;
    }

    public static double airC(Level level, BlockPos pos) {
        return Rooms.airC(level, pos);
    }

    public static void ageContainer(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || level.isClientSide || !(entity instanceof Container container)) {
            return;
        }
        double days = elapsedDays(entity, level);
        if (days <= 0) {
            return;
        }
        double air = airC(level, entity.getBlockPos());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            age(container.getItem(slot), days, air);
        }
        entity.setChanged();
    }

    public static List<ItemStack> carried(ServerPlayer player) {
        List<ItemStack> carried = new ArrayList<>();
        for (int slot = 0; slot < Hotbar.ROW; slot++) {
            carried.add(player.getInventory().getItem(slot));
        }
        carried.add(player.getOffhandItem());
        return carried;
    }

    @SubscribeEvent
    static void carry(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % STEP_TICKS != 0) {
            return;
        }
        double days = elapsedDays(player, player.level());
        if (days <= 0 || !Metabolism.lives(player)) {
            return;
        }
        double air = airC(player.level(), player.blockPosition());
        carried(player).forEach(stack -> {
            Fish.land(player, stack);
            age(stack, days, air);
        });
        ItemStack pack = Worn.BACK.get(player);
        if (!pack.isEmpty()) {
            NonNullList<ItemStack> packed = Hotbar.contents(pack);
            boolean aged = false;
            for (ItemStack stack : packed) {
                aged |= age(stack, days, air);
            }
            if (aged) {
                Hotbar.store(pack, packed);
            }
        }
    }

    @SubscribeEvent
    static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().setData(Things.FOOD_CLOCK, event.getEntity().level().getDayTime());
    }

    @SubscribeEvent
    static void opened(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof BlockEntity entity && entity instanceof Container) {
            ageContainer(entity);
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction toOther = ChestBlock.getConnectedDirection(state);
            if (level.getBlockEntity(pos.relative(toOther)) instanceof BlockEntity other) {
                ageContainer(other);
            }
        }
    }
}
