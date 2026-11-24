package io.github.roman16.backcountry;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class RealisticMining {
    private RealisticMining() {}

    @SubscribeEvent
    static void breakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (!(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES))) {
            return;
        }
        event.getPosition().ifPresent(pos -> {
            Balance.Mining mining = Balance.get().mining();
            if (!openBehind(event.getEntity(), pos, mining.openBlocks())) {
                event.setNewSpeed((float) (event.getNewSpeed() * mining.speed()));
            }
        });
    }

    private static boolean openBehind(Player player, BlockPos pos, int reach) {
        Level level = player.level();
        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 away = centre.subtract(player.getEyePosition()).normalize();
        for (int step = 1; step <= reach; step++) {
            BlockPos behind = BlockPos.containing(centre.add(away.scale(step)));
            if (!behind.equals(pos) && level.getBlockState(behind).getCollisionShape(level, behind).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
