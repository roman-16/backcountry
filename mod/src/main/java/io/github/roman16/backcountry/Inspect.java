package io.github.roman16.backcountry;

import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import io.github.roman16.backcountry.hunt.TrackEntity;
import io.github.roman16.backcountry.mixin.CampfireCookingAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class Inspect {
    public static final ResourceLocation POT = Backcountry.id("camp_pot");
    public static final ResourceLocation CAMPFIRE = Backcountry.id("campfire");
    public static final ResourceLocation SIGN = Backcountry.id("sign");
    public static final ResourceLocation CARCASS = Backcountry.id("carcass");
    public static final ResourceLocation WATER_KIND = Backcountry.id("water");
    public static final String AGE = "age";
    public static final String WATER = "water";
    public static final String STARTED = "started";
    public static final String NEEDLES = "needles";
    public static final String LIT = "lit";
    public static final String BURNING = "burning";
    public static final String QUEUED = "queued";
    public static final String COALS = "coals";
    public static final String COOKING = "cooking";
    public static final String ITEM = "item";
    public static final String TICKS_LEFT = "ticks_left";
    private static final int MINUTES_AN_HOUR = 60;

    private Inspect() {}

    public static final IServerDataProvider<BlockAccessor> POT_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof CampPotEntity pot) {
                Water.CODEC.encodeStart(NbtOps.INSTANCE, pot.water()).ifSuccess(water -> data.put(WATER, water));
                data.putInt(TICKS_LEFT, pot.ticksLeft(CampPotEntity.heat(accessor.getLevel(), accessor.getPosition())));
                data.putBoolean(STARTED, pot.started());
                data.putBoolean(NEEDLES, pot.needles());
                data.putBoolean(LIT, CampfireBlock.isLitCampfire(accessor.getLevel().getBlockState(accessor.getPosition().below())));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return POT;
        }
    };

    public static final IServerDataProvider<BlockAccessor> CAMPFIRE_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CampfireBlockEntity campfire)) {
                return;
            }
            Fire fire = Fire.of(campfire);
            boolean wet = Fire.rainedOn(accessor.getLevel(), accessor.getPosition());
            data.putBoolean(LIT, fire.lit());
            data.putInt(BURNING, minutes(fire.burningHours(wet)));
            data.putInt(QUEUED, minutes(fire.queuedHours(wet)));
            data.putInt(COALS, minutes(fire.coalHours()));
            CampfireCookingAccessor cooking = (CampfireCookingAccessor) campfire;
            ListTag pieces = new ListTag();
            for (int slot = 0; slot < campfire.getItems().size(); slot++) {
                ItemStack piece = campfire.getItems().get(slot);
                if (!piece.isEmpty()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString(ITEM, BuiltInRegistries.ITEM.getKey(piece.getItem()).toString());
                    entry.putInt(TICKS_LEFT, Math.max(0, cooking.backcountry$time()[slot] - cooking.backcountry$progress()[slot]));
                    pieces.add(entry);
                }
            }
            data.put(COOKING, pieces);
        }

        @Override
        public ResourceLocation getUid() {
            return CAMPFIRE;
        }
    };

    public static final IServerDataProvider<BlockAccessor> SIGN_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof TrackEntity sign) {
                data.putLong(AGE, sign.age());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return SIGN;
        }
    };

    public static final IServerDataProvider<BlockAccessor> CARCASS_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof CarcassEntity carcass) {
                data.putLong(TICKS_LEFT, carcass.ticksLeft());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return CARCASS;
        }
    };

    private static int minutes(double hours) {
        return (int) Math.round(hours * MINUTES_AN_HOUR);
    }
}
