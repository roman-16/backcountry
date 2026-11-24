package io.github.roman16.backcountry;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.gear.BandageItem;
import io.github.roman16.backcountry.gear.BearSprayItem;
import io.github.roman16.backcountry.gear.CampPotBlock;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.CampPotItem;
import io.github.roman16.backcountry.gear.LeftPackBlock;
import io.github.roman16.backcountry.gear.LeftPackEntity;
import io.github.roman16.backcountry.gear.MedKitItem;
import io.github.roman16.backcountry.gear.PlantFibreModifier;
import io.github.roman16.backcountry.gear.SupplyCrateBlock;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public final class Things {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Backcountry.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Backcountry.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Backcountry.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Backcountry.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Backcountry.MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
            Backcountry.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Backcountry.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Backcountry.MODID);

    public static final DataMapType<Item, Nutrition> FOOD = DataMapType.builder(Backcountry.id("food"), Registries.ITEM, Nutrition.CODEC).synced(Nutrition.CODEC, true).build();

    public static final Supplier<AttachmentType<Body>> BODY = ATTACHMENTS.register("body", () -> AttachmentType.serializable(Body::new).build());
    public static final Supplier<AttachmentType<Long>> SPRAYED_UNTIL = ATTACHMENTS.register("sprayed_until", () -> AttachmentType.builder(() -> 0L).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Water>> WATER = COMPONENTS.registerComponentType("water",
            builder -> builder.persistent(Water.CODEC).networkSynchronized(Water.STREAM_CODEC));

    public static final Holder<ArmorMaterial> HIDE = ARMOR_MATERIALS.register("hide", () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.LEGGINGS, 1);
        defense.put(ArmorItem.Type.CHESTPLATE, 2);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.BODY, 2);
    }), 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER), List.of(new ArmorMaterial.Layer(Backcountry.id("hide"))), 0, 0));

    public static final DeferredBlock<CampPotBlock> CAMP_POT_BLOCK = BLOCKS.registerBlock("camp_pot", CampPotBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.LANTERN).noOcclusion());
    public static final DeferredBlock<LeftPackBlock> LEFT_PACK = BLOCKS.registerBlock("left_pack", LeftPackBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE = BLOCKS.registerBlock("supply_crate", SupplyCrateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0F, 3600000.0F).sound(SoundType.WOOD).noLootTable());

    public static final Supplier<BlockEntityType<CampPotEntity>> CAMP_POT_ENTITY = BLOCK_ENTITIES.register("camp_pot",
            () -> BlockEntityType.Builder.of(CampPotEntity::new, CAMP_POT_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<LeftPackEntity>> LEFT_PACK_ENTITY = BLOCK_ENTITIES.register("left_pack",
            () -> BlockEntityType.Builder.of(LeftPackEntity::new, LEFT_PACK.get()).build(null));

    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage", BandageItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BEAR_SPRAY = ITEMS.registerItem("bear_spray", BearSprayItem::new, new Item.Properties().durability(Balance.get().bearSpray().bursts()));
    public static final DeferredItem<Item> CAMP_POT = ITEMS.registerItem("camp_pot", CampPotItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> HIDE_COAT = ITEMS.registerItem("hide_coat", properties -> new ArmorItem(HIDE, ArmorItem.Type.CHESTPLATE, properties),
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(8)));
    public static final DeferredItem<Item> HIDE_HAT = ITEMS.registerItem("hide_hat", properties -> new ArmorItem(HIDE, ArmorItem.Type.HELMET, properties),
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(8)));
    public static final DeferredItem<Item> HIDE_PACK = ITEMS.registerSimpleItem("hide_pack", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> HIDE_PANTS = ITEMS.registerItem("hide_pants", properties -> new ArmorItem(HIDE, ArmorItem.Type.LEGGINGS, properties),
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(8)));
    public static final DeferredItem<Item> HIDE_PONCHO = ITEMS.registerItem("hide_poncho", properties -> new ArmorItem(HIDE, ArmorItem.Type.CHESTPLATE, properties),
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(4)));
    public static final DeferredItem<Item> MED_KIT = ITEMS.registerItem("med_kit", MedKitItem::new, new Item.Properties().durability(4));
    public static final DeferredItem<Item> PILOT_BREAD = ITEMS.registerSimpleItem("pilot_bread",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre", new Item.Properties());
    public static final DeferredItem<Item> SATELLITE_MESSENGER = ITEMS.registerSimpleItem("satellite_messenger", new Item.Properties().stacksTo(1));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SUPPLY_CRATE_ITEM = ITEMS.registerSimpleBlockItem(SUPPLY_CRATE);
    public static final DeferredItem<Item> WATER_BOTTLE = ITEMS.registerItem("water_bottle",
            properties -> new WaterVesselItem(properties, Balance.get().water().bottleL()), new Item.Properties().stacksTo(1));

    public static final Supplier<MapCodec<PlantFibreModifier>> PLANT_FIBRE_MODIFIER = LOOT_MODIFIERS.register("plant_fibre", () -> PlantFibreModifier.CODEC);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("backcountry", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.backcountry"))
            .icon(() -> new ItemStack(SATELLITE_MESSENGER.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private Things() {}

    static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
        ATTACHMENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
        BLOCKS.register(bus);
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        LOOT_MODIFIERS.register(bus);
        TABS.register(bus);
    }
}
