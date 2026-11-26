package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.hunt.Tracks;
import io.github.roman16.backcountry.weather.Day;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlantFibreItem extends Item {
    private static final int STRANDS = 8;
    private static final double DRIFT_AT_CALM = 0;
    private static final double DRIFT_PER_KMH = 1.0 / 128;
    private static final double MOST_DRIFT = 0.25;
    private static final double SPREAD = 0.125;
    private static final double REACH = 0.5;

    public PlantFibreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer survivor) {
            Day day = Tracks.today(level);
            double drift = day.windKmh() < Balance.get().tracking().calmKmh() ? DRIFT_AT_CALM : Math.min(MOST_DRIFT, day.windKmh() * DRIFT_PER_KMH);
            Vec3 wind = Tracks.downwind(day).scale(drift);
            Vec3 held = player.getEyePosition().add(player.getLookAngle().scale(REACH));
            ItemParticleOption strand = new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1));
            for (int i = 0; i < STRANDS; i++) {
                double jitter = (level.random.nextDouble() - 0.5) * SPREAD;
                server.sendParticles(strand, held.x + jitter, held.y + jitter, held.z + jitter, 0, wind.x, wind.y, wind.z, 1);
            }
            Guide.did(survivor, Deed.CHECKED_WIND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
