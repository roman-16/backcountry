package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class SourceCategory implements IRecipeCategory<Source> {
    private static final int SLOT = 18;
    private static final int WIDTH = 176;
    private static final int ARROW_WIDTH = 24;
    private static final int OUTPUT_COLUMNS = 5;
    private static final int NOTE_LINES = 3;
    private static final int LINE = 10;
    private final Source.Page page;
    private final RecipeType<Source> type;
    private final IDrawable icon;

    public SourceCategory(Source.Page page, IGuiHelper gui, ItemStack icon) {
        this.page = page;
        this.type = type(page);
        this.icon = gui.createDrawableItemStack(icon);
    }

    public static RecipeType<Source> type(Source.Page page) {
        return RecipeType.create(Backcountry.MODID, page.id(), Source.class);
    }

    @Override
    public RecipeType<Source> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.backcountry.page." + page.id());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 2 * SLOT + 4 + NOTE_LINES * LINE;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Source source, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = source.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % 3 * SLOT, 1 + i / 3 * SLOT).addItemStacks(inputs.get(i)).setStandardSlotBackground();
        }
        int left = 3 * SLOT + ARROW_WIDTH + 8;
        List<ItemStack> outputs = source.outputs();
        for (int i = 0; i < outputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, left + i % OUTPUT_COLUMNS * SLOT, 1 + i / OUTPUT_COLUMNS * SLOT).addItemStack(outputs.get(i))
                    .setOutputSlotBackground();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Source source, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(3 * SLOT + 4, (SLOT - 16) / 2 + 1);
        if (!source.note().getString().isEmpty()) {
            builder.addText(List.of(source.note()), WIDTH, NOTE_LINES * LINE).setPosition(0, 2 * SLOT + 4).setColor(Look.JEI_NOTE);
        }
    }
}
