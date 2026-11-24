package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Things;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

public class Bird extends Chicken {
    private static final int NEVER = Integer.MAX_VALUE / 2;
    private static final float LOWER = 0.7F;
    private static final long DUSK_FROM = 13000;
    private static final long DUSK_TO = 16000;

    public Bird(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        eggTime = NEVER;
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return getType().create(level) instanceof Chicken young ? young : null;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * LOWER;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (getType() != Things.LOON.get()) {
            return super.getAmbientSound();
        }
        long time = level().getDayTime() % 24000;
        return time >= DUSK_FROM && time <= DUSK_TO ? Things.LOON_CALL.get() : null;
    }
}
