package io.github.roman16.backcountry.survivor;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.gear.Hotbar;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.net.Messages;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Arrival {
    private static final int CRATE_NEAREST = 2;
    private static final ResourceLocation HOOK = ResourceLocation.fromNamespaceAndPath("riverfishing", "hook_10");
    private static final ResourceLocation LINE = ResourceLocation.fromNamespaceAndPath("riverfishing", "line_mono_010");
    private static final ResourceLocation SLEEPING_BAG = ResourceLocation.fromNamespaceAndPath("sleeping_bags", "brown_sleeping_bag");
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
        if (!body.arrived) {
            arrive(player);
        } else if (body.ended != null) {
            Messages.summary(player, body.ended);
        }
    }

    @SubscribeEvent
    static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) {
            arrive(player);
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

    public static void arrive(ServerPlayer player) {
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
        inventory.setItem(0, new ItemStack(Things.SATELLITE_MESSENGER.get()));
        inventory.setItem(1, new ItemStack(Things.BEAR_SPRAY.get()));
        ItemStack bottle = new ItemStack(Things.WATER_BOTTLE.get());
        bottle.set(Things.WATER.get(), Water.boiled((float) Balance.get().water().bottleL()));
        inventory.setItem(2, bottle);
        inventory.setItem(3, new ItemStack(Things.PILOT_BREAD.get(), Balance.get().kit().pilotBread()));
        inventory.setItem(4, new ItemStack(Things.CAMP_POT.get()));
        body.binoculars = new ItemStack(Things.BINOCULARS.get());
        body.pack = canvasPack();
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

    public static ItemStack canvasPack() {
        ItemStack pack = new ItemStack(Things.CANVAS_PACK.get());
        NonNullList<ItemStack> items = Hotbar.contents(pack);
        List<ItemStack> kit = List.of(new ItemStack(BuiltInRegistries.ITEM.get(SLEEPING_BAG)), new ItemStack(BuiltInRegistries.ITEM.get(LINE)),
                new ItemStack(BuiltInRegistries.ITEM.get(HOOK), Balance.get().kit().hooks()), new ItemStack(Things.MED_KIT.get()), new ItemStack(Things.HEADLAMP.get()));
        for (int i = 0; i < kit.size(); i++) {
            items.set(i, kit.get(i));
        }
        Hotbar.store(pack, items);
        return pack;
    }
}
