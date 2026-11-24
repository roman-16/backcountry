package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.hunt.Hunting;
import io.github.roman16.backcountry.weather.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class Mosquitoes {
    private static final int SCAN_STEP = 2;
    private static final int HOURS = 24;
    private static final int MIDNIGHT_OFFSET = 6;
    private static final DustParticleOptions SWARM = new DustParticleOptions(new Vector3f(0.1F, 0.1F, 0.1F), 0.5F);
    private static final int SPECKS = 6;

    private Mosquitoes() {}

    public static boolean biting(ServerPlayer player, Body body) {
        Balance.Mosquitoes mosquitoes = Balance.get().mosquitoes();
        Level level = player.level();
        int hour = (int) ((level.getDayTime() / 1000 + MIDNIGHT_OFFSET) % HOURS);
        boolean night = hour >= mosquitoes.firstHour() || hour <= mosquitoes.lastHour();
        if (!night || body.fire || level.isRaining() || !Hunting.months(Seasons.month(level), mosquitoes.months())
                || Weather.day(level, Calendar.day(level)).windKmh() >= mosquitoes.calmKmh()) {
            return false;
        }
        BlockPos feet = player.blockPosition();
        int reach = mosquitoes.waterBlocks();
        for (BlockPos at : BlockPos.betweenClosed(feet.offset(-reach, -2, -reach), feet.offset(reach, 1, reach))) {
            if ((at.getX() + at.getZ()) % SCAN_STEP == 0 && level.getFluidState(at).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public static void swarm(ServerPlayer player) {
        player.serverLevel().sendParticles(SWARM, player.getX(), player.getEyeY(), player.getZ(), SPECKS, 0.6, 0.4, 0.6, 0);
    }
}
