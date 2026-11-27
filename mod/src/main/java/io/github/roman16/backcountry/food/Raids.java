package io.github.roman16.backcountry.food;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Alert;
import io.github.roman16.backcountry.body.Alerts;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.client.Words;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class Raids {
    private static final long TICKS_AN_HOUR = 1000;

    private Raids() {}

    public static void check(ServerLevel level, BlockPos pos, CacheEntity cache) {
        Balance.Raids rules = Balance.get().raids();
        long now = level.getGameTime();
        if (now - cache.raidedAt < rules.raidHours() * TICKS_AN_HOUR || !cache.scented() || cache.reach() == CacheEntity.Reach.TALL) {
            return;
        }
        List<LivingEntity> raiders = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(rules.raidBlocks()),
                entity -> entity.isAlive() && rules.raiders().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
        for (LivingEntity raider : raiders) {
            boolean fouls = rules.fouling().contains(BuiltInRegistries.ENTITY_TYPE.getKey(raider.getType()).toString());
            boolean watched = level.players().stream().anyMatch(player -> player instanceof ServerPlayer survivor && Metabolism.lives(survivor)
                    && player.distanceToSqr(pos.getCenter()) <= rules.scareBlocks() * rules.scareBlocks());
            if (fouls && watched) {
                continue;
            }
            double before = cache.usableKcal();
            if (cache.raidedBy(raider.getType(), level.random)) {
                cache.raidedAt = now;
                cache.report();
                String eaten = Words.kcal(Math.max(0, before - cache.usableKcal())) + " kcal";
                for (ServerPlayer player : level.players()) {
                    if (Metabolism.lives(player)) {
                        Alerts.announce(player, fouls ? Alert.WOLVERINE_RAID : Alert.BEAR_AT_CACHE);
                        player.sendSystemMessage(Component.translatable(fouls ? "message.backcountry.raided_wolverine" : "message.backcountry.raided_bear", eaten));
                    }
                }
                return;
            }
        }
    }
}
