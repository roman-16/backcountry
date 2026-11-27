package io.github.roman16.backcountry.survivor;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.Worn;
import io.github.roman16.backcountry.net.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Arrival {
    private static final int CRATE_NEAREST = 2;
    private static final int HOTBAR_ISSUED = 3;
    private static final int CRATE_FARTHEST = 6;
    private static final int CRATE_DIRECTIONS = 16;

    private Arrival() {}

    @SubscribeEvent
    static void started(ServerStartedEvent event) {
        Team team = Team.get(event.getServer());
        if (!team.begun()) {
            Calendar.begin(event.getServer().overworld());
            event.getServer().getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, event.getServer());
            event.getServer().getGameRules().getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(true, event.getServer());
            team.begin();
        }
    }

    @SubscribeEvent
    static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Body body = Metabolism.body(player);
        if (!body.arrived && player.isCreative()) {
            arrive(player, List.of());
        } else if (!body.arrived) {
            await(player);
        } else if (body.ended != null) {
            Messages.summary(player, body.ended);
        }
    }

    @SubscribeEvent
    static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) {
            await(player);
        }
    }

    @SubscribeEvent
    static void respawnAt(PlayerRespawnPositionEvent event) {
        ServerLevel overworld = event.getEntity().getServer().overworld();
        BlockPos at = dropOff(overworld);
        event.setDimensionTransition(new DimensionTransition(overworld, Vec3.atBottomCenterOf(at), Vec3.ZERO, overworld.getSharedSpawnAngle(), 0,
                DimensionTransition.DO_NOTHING));
    }

    @SubscribeEvent
    static void noBedSpawn(PlayerSetSpawnEvent event) {
        if (event.getNewSpawn() != null) {
            event.setCanceled(true);
        }
    }

    public static BlockPos dropOff(ServerLevel level) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos());
    }

    public static void await(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        Body body = Metabolism.body(player);
        if (player.isSpectator()) {
            player.setGameMode(GameType.byId(body.gameModeBeforeEnd));
        }
        BlockPos at = dropOff(overworld);
        player.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
        player.getInventory().clearContent();
        for (Worn worn : Worn.values()) {
            worn.set(player, ItemStack.EMPTY);
        }
        body.arrived = false;
        body.ended = null;
        Messages.kit(player);
    }

    public static void arrive(ServerPlayer player, List<Kit> picks) {
        ServerLevel overworld = player.server.overworld();
        Body body = Metabolism.body(player);
        if (player.isSpectator()) {
            player.setGameMode(GameType.byId(body.gameModeBeforeEnd));
        }
        BlockPos at = dropOff(overworld);
        player.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
        int day = Calendar.day(overworld);
        body.reset(day, Calendar.year(day));
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        List<ItemStack> issued = Kit.issued();
        List<ItemStack> hotbar = new ArrayList<>(issued.subList(0, HOTBAR_ISSUED));
        List<ItemStack> packed = new ArrayList<>(issued.subList(HOTBAR_ISSUED, issued.size()));
        for (Kit pick : Kit.valid(picks)) {
            for (ItemStack stack : pick.items()) {
                (hotbar.size() < Hotbar.ROW ? hotbar : packed).add(stack);
            }
        }
        for (int slot = 0; slot < hotbar.size(); slot++) {
            inventory.setItem(slot, hotbar.get(slot));
        }
        Worn.NECK.set(player, new ItemStack(Things.BINOCULARS.get()));
        Worn.HANDS.set(player, ItemStack.EMPTY);
        Worn.BACK.set(player, canvasPack(packed));
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.fallDistance = 0;
        Metabolism.release(player);
        Team team = Team.get(player.server);
        team.arrived(player, body);
        if (team.crate().isEmpty()) {
            crateSpot(overworld, at).ifPresent(spot -> {
                overworld.setBlockAndUpdate(spot, Things.SUPPLY_CRATE.get().defaultBlockState());
                team.crate(spot);
            });
        }
        Messages.view(player, body);
    }

    private static Optional<BlockPos> crateSpot(ServerLevel level, BlockPos dropOff) {
        for (int reach = CRATE_NEAREST; reach <= CRATE_FARTHEST; reach++) {
            for (int direction = 0; direction < CRATE_DIRECTIONS; direction++) {
                double angle = 2 * Math.PI * direction / CRATE_DIRECTIONS;
                BlockPos column = dropOff.offset((int) Math.round(Math.cos(angle) * reach), 0, (int) Math.round(Math.sin(angle) * reach));
                BlockPos spot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                BlockPos below = spot.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(below).isEmpty()
                        && (level.getBlockState(spot).canBeReplaced() || level.getBlockState(spot).is(Blocks.SNOW)) && level.getFluidState(spot).isEmpty()) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    public static ItemStack canvasPack(List<ItemStack> packed) {
        ItemStack pack = new ItemStack(Things.CANVAS_PACK.get());
        NonNullList<ItemStack> items = Hotbar.contents(pack);
        for (int i = 0; i < packed.size() && i < items.size(); i++) {
            items.set(i, packed.get(i));
        }
        Hotbar.store(pack, items);
        return pack;
    }
}
