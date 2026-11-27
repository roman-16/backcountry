package io.github.roman16.backcountry.client;

import com.mojang.datafixers.util.Pair;
import io.github.roman16.backcountry.Backcountry;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

public final class CanoeRenderer extends BoatRenderer {
    private static final ResourceLocation TEXTURE = Backcountry.id("textures/entity/canoe.png");
    private final Pair<ResourceLocation, ListModel<Boat>> canoe;

    public CanoeRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        canoe = Pair.of(TEXTURE, new BoatModel(context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.BIRCH))));
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return canoe;
    }
}
