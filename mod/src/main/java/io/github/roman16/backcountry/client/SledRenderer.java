package io.github.roman16.backcountry.client;

import com.mojang.datafixers.util.Pair;
import io.github.roman16.backcountry.Backcountry;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

public final class SledRenderer extends BoatRenderer {
    private static final ResourceLocation TEXTURE = Backcountry.id("textures/entity/sled.png");

    public SledRenderer(EntityRendererProvider.Context context) {
        super(context, true);
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return Pair.of(TEXTURE, super.getModelWithLocation(boat).getSecond());
    }
}
