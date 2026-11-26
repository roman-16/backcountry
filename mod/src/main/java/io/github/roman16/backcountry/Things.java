package io.github.roman16.backcountry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Nutrition;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.fire.FireMenu;
import io.github.roman16.backcountry.fire.FirewoodItem;
import io.github.roman16.backcountry.fire.Lights;
import io.github.roman16.backcountry.fire.WoodpileBlock;
import io.github.roman16.backcountry.fire.WoodpileEntity;
import io.github.roman16.backcountry.food.Berry;
import io.github.roman16.backcountry.food.Age;
import io.github.roman16.backcountry.food.BerryBushBlock;
import io.github.roman16.backcountry.food.BirchTapBlock;
import io.github.roman16.backcountry.food.CacheBlock;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.CacheMenu;
import io.github.roman16.backcountry.food.CookingPotBlock;
import io.github.roman16.backcountry.food.CookingPotEntity;
import io.github.roman16.backcountry.food.SmokerBlock;
import io.github.roman16.backcountry.food.SmokerEntity;
import io.github.roman16.backcountry.food.SmokerMenu;
import io.github.roman16.backcountry.food.StewRecipe;
import io.github.roman16.backcountry.gear.BandageItem;
import io.github.roman16.backcountry.gear.BearSprayItem;
import io.github.roman16.backcountry.gear.BinocularsItem;
import io.github.roman16.backcountry.gear.CampPotBlock;
import io.github.roman16.backcountry.gear.CampPotEntity;
import io.github.roman16.backcountry.gear.CampPotItem;
import io.github.roman16.backcountry.gear.HeadlampItem;
import io.github.roman16.backcountry.gear.LeftPackBlock;
import io.github.roman16.backcountry.gear.LeftPackEntity;
import io.github.roman16.backcountry.gear.MedKitItem;
import io.github.roman16.backcountry.gear.PackItem;
import io.github.roman16.backcountry.gear.PlantFibreModifier;
import io.github.roman16.backcountry.gear.SpruceNeedlesModifier;
import io.github.roman16.backcountry.gear.SupplyCrateBlock;
import io.github.roman16.backcountry.gear.ToolRecipe;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import io.github.roman16.backcountry.haul.Sled;
import io.github.roman16.backcountry.haul.SledItem;
import io.github.roman16.backcountry.hunt.Animal;
import io.github.roman16.backcountry.hunt.Bird;
import io.github.roman16.backcountry.hunt.BloodBlock;
import io.github.roman16.backcountry.hunt.CarcassBlock;
import io.github.roman16.backcountry.hunt.CarcassEntity;
import io.github.roman16.backcountry.hunt.CarcassItem;
import io.github.roman16.backcountry.hunt.HuntArrowItem;
import io.github.roman16.backcountry.hunt.HuntingBowItem;
import io.github.roman16.backcountry.hunt.MooseCallItem;
import io.github.roman16.backcountry.hunt.Wound;
import io.github.roman16.backcountry.trap.Kind;
import io.github.roman16.backcountry.trap.TrapBlock;
import io.github.roman16.backcountry.trap.TrapEntity;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Backcountry.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Backcountry.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Backcountry.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Backcountry.MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Backcountry.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Backcountry.MODID);

    public static final DataMapType<EntityType<?>, Animal> ANIMAL = DataMapType.builder(Backcountry.id("animal"), Registries.ENTITY_TYPE, Animal.CODEC).build();
    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, Backcountry.id("knives"));
    public static final TagKey<Biome> SALT_WATER = TagKey.create(Registries.BIOME, Backcountry.id("salt_water"));
    public static final TagKey<Fluid> MOVING_WATER = TagKey.create(Registries.FLUID, Backcountry.id("moving_water"));
    public static final DataMapType<Item, Nutrition> FOOD = DataMapType.builder(Backcountry.id("food"), Registries.ITEM, Nutrition.CODEC).synced(Nutrition.CODEC, true).build();

    public static final Supplier<AttachmentType<Body>> BODY = ATTACHMENTS.register("body", () -> AttachmentType.serializable(Body::new).build());
    public static final Supplier<AttachmentType<Fire>> FIRE = ATTACHMENTS.register("fire", () -> AttachmentType.serializable(Fire::new).build());
    public static final Supplier<AttachmentType<Long>> FOOD_CLOCK = ATTACHMENTS.register("food_clock",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE).serialize(Codec.LONG).build());
    public static final Supplier<AttachmentType<Wound>> WOUND = ATTACHMENTS.register("wound", () -> AttachmentType.builder(() -> Wound.NONE).serialize(Wound.CODEC).build());
    public static final Supplier<AttachmentType<Long>> FISHING_AT = ATTACHMENTS.register("fishing_at",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE).serialize(Codec.LONG).build());
    public static final Supplier<AttachmentType<Long>> SPRAYED_UNTIL = ATTACHMENTS.register("sprayed_until", () -> AttachmentType.builder(() -> 0L).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Age>> AGE = COMPONENTS.registerComponentType("age",
            builder -> builder.persistent(Age.CODEC).networkSynchronized(Age.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> BURN = COMPONENTS.registerComponentType("burn",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> CONTAMINATED = COMPONENTS.registerComponentType("contaminated",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SMOKED = COMPONENTS.registerComponentType("smoked",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ANIMAL_TYPE = COMPONENTS.registerComponentType("animal",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> ARROWS = COMPONENTS.registerComponentType("arrows",
            builder -> builder.persistent(ItemStack.CODEC.listOf()).networkSynchronized(ItemStack.LIST_STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LANDED = COMPONENTS.registerComponentType("landed",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SPAWNED_OUT = COMPONENTS.registerComponentType("spawned_out",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> POISONED = COMPONENTS.registerComponentType("poisoned",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> FROZEN = COMPONENTS.registerComponentType("frozen",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> CHARGE = COMPONENTS.registerComponentType("charge",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LIGHT = COMPONENTS.registerComponentType("light",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SEASONING = COMPONENTS.registerComponentType("seasoning",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Water>> WATER = COMPONENTS.registerComponentType("water",
            builder -> builder.persistent(Water.CODEC).networkSynchronized(Water.STREAM_CODEC));

    public static final Holder<ArmorMaterial> HIDE = ARMOR_MATERIALS.register("hide", () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.LEGGINGS, 1);
        defense.put(ArmorItem.Type.CHESTPLATE, 2);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.BODY, 2);
    }), 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER), List.of(new ArmorMaterial.Layer(Backcountry.id("hide"))), 0, 0));
    public static final Holder<ArmorMaterial> RAWHIDE = ARMOR_MATERIALS.register("rawhide", () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }
    }), 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(Backcountry.id("snowshoes"))), 0, 0));
    public static final Holder<ArmorMaterial> HEADLAMP_STRAP = ARMOR_MATERIALS.register("headlamp", () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }
    }), 0, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(Backcountry.id("headlamp"))), 0, 0));

    public static final DeferredBlock<FireBlock> CAMPFIRE = BLOCKS.registerBlock("campfire", FireBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).lightLevel(FireBlock::light)
                    .noOcclusion().ignitedByLava());
    public static final DeferredBlock<BloodBlock> BLOOD = BLOCKS.registerBlock("blood", BloodBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak().replaceable().randomTicks().noLootTable().sound(SoundType.SLIME_BLOCK)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CarcassBlock> CARCASS = BLOCKS.registerBlock("carcass", CarcassBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0F).sound(SoundType.WOOL).noOcclusion().noLootTable());
    public static final DeferredBlock<TrapBlock> DEADFALL = trap("deadfall", Kind.DEADFALL);
    public static final DeferredBlock<TrapBlock> GILL_NET = trap("gill_net", Kind.GILL_NET);
    public static final DeferredBlock<TrapBlock> LARGE_SNARE = trap("large_snare", Kind.LARGE_SNARE);
    public static final DeferredBlock<TrapBlock> MOUSE_TRAP = trap("mouse_trap", Kind.MOUSE_TRAP);
    public static final DeferredBlock<TrapBlock> SMALL_SNARE = trap("small_snare", Kind.SMALL_SNARE);
    public static final DeferredBlock<TrapBlock> TIP_UP = trap("tip_up", Kind.TIP_UP);
    public static final DeferredBlock<BerryBushBlock> BANEBERRY_BUSH = bush("baneberry_bush", Berry.BANEBERRY);
    public static final DeferredBlock<BerryBushBlock> BLUEBERRY_BUSH = bush("blueberry_bush", Berry.BLUEBERRY);
    public static final DeferredBlock<BerryBushBlock> HIGHBUSH_CRANBERRY_BUSH = bush("highbush_cranberry_bush", Berry.HIGHBUSH_CRANBERRY);
    public static final DeferredBlock<BerryBushBlock> SALMONBERRY_BUSH = bush("salmonberry_bush", Berry.SALMONBERRY);
    public static final DeferredBlock<BirchTapBlock> BIRCH_TAP_BLOCK = BLOCKS.registerBlock("birch_tap", BirchTapBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noOcclusion().randomTicks().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<RotatedPillarBlock> DEAD_SPRUCE_LOG = BLOCKS.registerBlock("dead_spruce_log", RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<LanternBlock> EMPTY_LANTERN = BLOCKS.registerBlock("empty_lantern", LanternBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).lightLevel(state -> 0));
    public static final DeferredBlock<CacheBlock> FOOD_CACHE = BLOCKS.registerBlock("food_cache", CacheBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<CookingPotBlock> COOKING_POT_BLOCK = BLOCKS.registerBlock("cooking_pot", CookingPotBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.LANTERN).noOcclusion());
    public static final DeferredBlock<SmokerBlock> SMOKER = BLOCKS.registerBlock("smoker", SmokerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).lightLevel(SmokerBlock::light));
    public static final DeferredBlock<CampPotBlock> CAMP_POT_BLOCK = BLOCKS.registerBlock("camp_pot", CampPotBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.LANTERN).noOcclusion());
    public static final DeferredBlock<LeftPackBlock> LEFT_PACK = BLOCKS.registerBlock("left_pack", LeftPackBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<WoodpileBlock> WOODPILE = BLOCKS.registerBlock("woodpile", WoodpileBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noOcclusion().noLootTable().ignitedByLava());
    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE = BLOCKS.registerBlock("supply_crate", SupplyCrateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0F, 3600000.0F).sound(SoundType.WOOD).noLootTable());

    public static final Supplier<BlockEntityType<CampPotEntity>> CAMP_POT_ENTITY = BLOCK_ENTITIES.register("camp_pot",
            () -> BlockEntityType.Builder.of(CampPotEntity::new, CAMP_POT_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<CarcassEntity>> CARCASS_ENTITY = BLOCK_ENTITIES.register("carcass",
            () -> BlockEntityType.Builder.of(CarcassEntity::new, CARCASS.get()).build(null));
    public static final Supplier<BlockEntityType<TrapEntity>> TRAP_ENTITY = BLOCK_ENTITIES.register("trap",
            () -> BlockEntityType.Builder.of(TrapEntity::new, DEADFALL.get(), GILL_NET.get(), LARGE_SNARE.get(), MOUSE_TRAP.get(), SMALL_SNARE.get(), TIP_UP.get()).build(null));
    public static final Supplier<BlockEntityType<CacheEntity>> FOOD_CACHE_ENTITY = BLOCK_ENTITIES.register("food_cache",
            () -> BlockEntityType.Builder.of(CacheEntity::new, FOOD_CACHE.get()).build(null));
    public static final Supplier<BlockEntityType<CookingPotEntity>> COOKING_POT_ENTITY = BLOCK_ENTITIES.register("cooking_pot",
            () -> BlockEntityType.Builder.of(CookingPotEntity::new, COOKING_POT_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<SmokerEntity>> SMOKER_ENTITY = BLOCK_ENTITIES.register("smoker",
            () -> BlockEntityType.Builder.of(SmokerEntity::new, SMOKER.get()).build(null));
    public static final Supplier<BlockEntityType<WoodpileEntity>> WOODPILE_ENTITY = BLOCK_ENTITIES.register("woodpile",
            () -> BlockEntityType.Builder.of(WoodpileEntity::new, WOODPILE.get()).build(null));
    public static final Supplier<BlockEntityType<LeftPackEntity>> LEFT_PACK_ENTITY = BLOCK_ENTITIES.register("left_pack",
            () -> BlockEntityType.Builder.of(LeftPackEntity::new, LEFT_PACK.get()).build(null));

    public static final DeferredItem<Item> ANIMAL_FAT = food("animal_fat");
    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage", BandageItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BEAR_SPRAY = ITEMS.registerItem("bear_spray", BearSprayItem::new, new Item.Properties().durability(Balance.get().bearSpray().bursts()));
    public static final DeferredItem<Item> BEAVER_TAIL = food("beaver_tail");
    public static final DeferredItem<Item> BINOCULARS = ITEMS.registerItem("binoculars", BinocularsItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BIRCH_BARK = ITEMS.registerSimpleItem("birch_bark", new Item.Properties());
    public static final DeferredItem<Item> BANEBERRIES = berries("baneberries", BANEBERRY_BUSH);
    public static final DeferredItem<Item> BIRCH_SYRUP = food("birch_syrup");
    public static final DeferredItem<BlockItem> BIRCH_TAP = ITEMS.registerSimpleBlockItem("birch_tap", BIRCH_TAP_BLOCK);
    public static final DeferredItem<Item> BLUEBERRIES = berries("blueberries", BLUEBERRY_BUSH);
    public static final DeferredItem<BlockItem> DEAD_SPRUCE_LOG_ITEM = ITEMS.registerSimpleBlockItem(DEAD_SPRUCE_LOG);
    public static final DeferredItem<Item> HIGHBUSH_CRANBERRIES = berries("highbush_cranberries", HIGHBUSH_CRANBERRY_BUSH);
    public static final DeferredItem<Item> SALMONBERRIES = berries("salmonberries", SALMONBERRY_BUSH);
    public static final DeferredItem<Item> SLED = ITEMS.registerItem("sled", SledItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SPRUCE_NEEDLES = ITEMS.registerSimpleItem("spruce_needles", new Item.Properties());
    public static final DeferredItem<Item> BLUNT_ARROW = ITEMS.registerItem("blunt_arrow", properties -> new HuntArrowItem(properties, HuntArrowItem.Head.BLUNT));
    public static final DeferredItem<Item> BONE_ARROW = ITEMS.registerItem("bone_arrow", properties -> new HuntArrowItem(properties, HuntArrowItem.Head.BONE));
    public static final DeferredItem<Item> BOW_DRILL = ITEMS.registerSimpleItem("bow_drill", new Item.Properties().durability(Balance.get().bowDrill().uses()));
    public static final DeferredItem<Item> BOW_STAVE = ITEMS.registerSimpleItem("bow_stave", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BOWSTRING = ITEMS.registerSimpleItem("bowstring", new Item.Properties());
    public static final DeferredItem<Item> BROADHEAD_ARROW = ITEMS.registerItem("broadhead_arrow", properties -> new HuntArrowItem(properties, HuntArrowItem.Head.BROADHEAD));
    public static final DeferredItem<Item> CAMP_POT = ITEMS.registerItem("camp_pot", CampPotItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> CAMPFIRE_ITEM = ITEMS.registerSimpleBlockItem(CAMPFIRE);
    public static final DeferredItem<Item> CANVAS_PACK = ITEMS.registerItem("canvas_pack", properties -> new PackItem(properties, Balance.get().load().canvasPackRows()),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CARCASS_ITEM = ITEMS.registerItem("carcass", CarcassItem::new, new Item.Properties());
    public static final DeferredItem<BlockItem> COOKING_POT = ITEMS.registerSimpleBlockItem(COOKING_POT_BLOCK);
    public static final DeferredItem<Item> CORDAGE = ITEMS.registerSimpleItem("cordage", new Item.Properties());
    public static final DeferredItem<Item> CRICKET = ITEMS.registerSimpleItem("cricket", new Item.Properties());
    public static final DeferredItem<Item> CROSSBEAM = ITEMS.registerSimpleItem("crossbeam", new Item.Properties());
    public static final DeferredItem<Item> CURED_HIDE = ITEMS.registerSimpleItem("cured_hide", new Item.Properties());
    public static final DeferredItem<BlockItem> DEADFALL_ITEM = ITEMS.registerSimpleBlockItem(DEADFALL);
    public static final DeferredItem<BlockItem> EMPTY_LANTERN_ITEM = ITEMS.registerSimpleBlockItem(EMPTY_LANTERN);
    public static final DeferredItem<BlockItem> FOOD_CACHE_ITEM = ITEMS.registerSimpleBlockItem(FOOD_CACHE);
    public static final DeferredItem<Item> GAME_GUTS = food("game_guts");
    public static final DeferredItem<BlockItem> GILL_NET_ITEM = ITEMS.registerSimpleBlockItem(GILL_NET);
    public static final DeferredItem<Item> HARDWOOD = ITEMS.registerItem("hardwood", properties -> new FirewoodItem(properties, true), new Item.Properties());
    public static final DeferredItem<Item> HEADLAMP = ITEMS.registerItem("headlamp", properties -> new HeadlampItem(HEADLAMP_STRAP, properties), new Item.Properties());
    public static final DeferredItem<Item> HEART = food("heart");
    public static final DeferredItem<Item> HEAVY_HUNTING_BOW = ITEMS.registerItem("heavy_hunting_bow", properties -> new HuntingBowItem(properties, true),
            new Item.Properties().durability(384));
    public static final DeferredItem<Item> HIDE_COAT = ITEMS.registerItem("hide_coat", properties -> new ArmorItem(HIDE, ArmorItem.Type.CHESTPLATE, properties),
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(8)));
    public static final DeferredItem<Item> HIDE_HAT = ITEMS.registerItem("hide_hat", properties -> new ArmorItem(HIDE, ArmorItem.Type.HELMET, properties),
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(8)));
    public static final DeferredItem<Item> HIDE_PACK = ITEMS.registerItem("hide_pack", properties -> new PackItem(properties, Balance.get().load().hidePackRows()),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> HIDE_PANTS = ITEMS.registerItem("hide_pants", properties -> new ArmorItem(HIDE, ArmorItem.Type.LEGGINGS, properties),
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(8)));
    public static final DeferredItem<Item> HIDE_PONCHO = ITEMS.registerItem("hide_poncho", properties -> new ArmorItem(HIDE, ArmorItem.Type.CHESTPLATE, properties),
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(4)));
    public static final DeferredItem<Item> KIDNEY = food("kidney");
    public static final DeferredItem<Item> KNIFE = ITEMS.registerItem("knife", properties -> new SwordItem(Tiers.STONE, properties),
            new Item.Properties().attributes(SwordItem.createAttributes(Tiers.STONE, 1, -2.0F)));
    public static final DeferredItem<BlockItem> LARGE_SNARE_ITEM = ITEMS.registerSimpleBlockItem(LARGE_SNARE);
    public static final DeferredItem<Item> LIGHT_HUNTING_BOW = ITEMS.registerItem("light_hunting_bow", properties -> new HuntingBowItem(properties, false),
            new Item.Properties().durability(256));
    public static final DeferredItem<Item> LIVER = food("liver");
    public static final DeferredItem<Item> MED_KIT = ITEMS.registerItem("med_kit", MedKitItem::new, new Item.Properties().durability(4));
    public static final DeferredItem<Item> MOOSE_CALL = ITEMS.registerItem("moose_call", MooseCallItem::new, new Item.Properties());
    public static final DeferredItem<Item> MOOSE_HEAD = ITEMS.registerSimpleItem("moose_head", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MOUSE_DROPPINGS = ITEMS.registerSimpleItem("mouse_droppings", new Item.Properties());
    public static final DeferredItem<BlockItem> MOUSE_TRAP_ITEM = ITEMS.registerSimpleBlockItem(MOUSE_TRAP);
    public static final DeferredItem<Item> PELT = ITEMS.registerSimpleItem("pelt", new Item.Properties());
    public static final DeferredItem<Item> PILOT_BREAD = ITEMS.registerSimpleItem("pilot_bread",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre", new Item.Properties());
    public static final DeferredItem<Item> RAWHIDE_STRIPS = ITEMS.registerSimpleItem("rawhide_strips", new Item.Properties());
    public static final DeferredItem<Item> RENDERED_FAT = food("rendered_fat");
    public static final DeferredItem<Item> ROPE = ITEMS.registerSimpleItem("rope", new Item.Properties());
    public static final DeferredItem<Item> SATELLITE_MESSENGER = ITEMS.registerSimpleItem("satellite_messenger", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SCRAPED_HIDE = ITEMS.registerSimpleItem("scraped_hide", new Item.Properties());
    public static final DeferredItem<Item> SEASONED_BOW_STAVE = ITEMS.registerSimpleItem("seasoned_bow_stave", new Item.Properties().stacksTo(16));
    public static final DeferredItem<BlockItem> SMALL_SNARE_ITEM = ITEMS.registerSimpleBlockItem(SMALL_SNARE);
    public static final DeferredItem<BlockItem> SMOKER_ITEM = ITEMS.registerSimpleBlockItem(SMOKER);
    public static final DeferredItem<Item> SNARE_WIRE = ITEMS.registerSimpleItem("snare_wire", new Item.Properties());
    public static final DeferredItem<Item> SNOWSHOES = ITEMS.registerItem("snowshoes", properties -> new ArmorItem(RAWHIDE, ArmorItem.Type.BOOTS, properties),
            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(8)));
    public static final DeferredItem<Item> SOFTWOOD = ITEMS.registerItem("softwood", properties -> new FirewoodItem(properties, false), new Item.Properties());
    public static final DeferredItem<BlockItem> SUPPLY_CRATE_ITEM = ITEMS.registerSimpleBlockItem(SUPPLY_CRATE);
    public static final DeferredItem<Item> TINDER_BUNDLE = ITEMS.registerSimpleItem("tinder_bundle", new Item.Properties());
    public static final DeferredItem<BlockItem> TIP_UP_ITEM = ITEMS.registerSimpleBlockItem(TIP_UP);
    public static final DeferredItem<Item> TONGUE = food("tongue");
    public static final DeferredItem<Item> TRAP_STAKES = ITEMS.registerSimpleItem("trap_stakes", new Item.Properties());
    public static final DeferredItem<Item> WATER_BOTTLE = ITEMS.registerItem("water_bottle",
            properties -> new WaterVesselItem(properties, Balance.get().water().bottleL()), new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> WIRE_MESH = ITEMS.registerSimpleItem("wire_mesh", new Item.Properties());

    public static final Supplier<EntityType<Sled>> SLED_ENTITY = ENTITY_TYPES.register("sled",
            () -> EntityType.Builder.<Sled>of(Sled::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build("sled"));
    public static final Supplier<EntityType<Bird>> GROUSE = ENTITY_TYPES.register("grouse",
            () -> EntityType.Builder.of(Bird::new, MobCategory.CREATURE).sized(0.5F, 0.6F).clientTrackingRange(10).build("grouse"));
    public static final Supplier<EntityType<Bird>> LOON = ENTITY_TYPES.register("loon",
            () -> EntityType.Builder.of(Bird::new, MobCategory.CREATURE).sized(0.5F, 0.6F).clientTrackingRange(10).build("loon"));
    public static final Supplier<EntityType<Bird>> PTARMIGAN = ENTITY_TYPES.register("ptarmigan",
            () -> EntityType.Builder.of(Bird::new, MobCategory.CREATURE).sized(0.5F, 0.6F).clientTrackingRange(10).build("ptarmigan"));
    public static final Supplier<SoundEvent> LOON_CALL = SOUNDS.register("loon", () -> SoundEvent.createVariableRangeEvent(Backcountry.id("loon")));

    public static final Supplier<MenuType<FireMenu>> FIRE_MENU = MENUS.register("campfire", () -> IMenuTypeExtension.create(FireMenu::new));
    public static final Supplier<MenuType<CacheMenu>> FOOD_CACHE_MENU = MENUS.register("food_cache", () -> IMenuTypeExtension.create(CacheMenu::new));
    public static final Supplier<MenuType<SmokerMenu>> SMOKER_MENU = MENUS.register("smoker", () -> IMenuTypeExtension.create(SmokerMenu::new));
    public static final Supplier<RecipeType<StewRecipe>> STEWING = RECIPE_TYPES.register("stewing", () -> RecipeType.simple(Backcountry.id("stewing")));
    public static final Supplier<RecipeSerializer<StewRecipe>> STEW = RECIPE_SERIALIZERS.register("stewing", StewRecipe.Serializer::new);
    public static final Supplier<RecipeSerializer<ToolRecipe>> TOOL_RECIPE = RECIPE_SERIALIZERS.register("tool", ToolRecipe.Serializer::new);

    public static final Supplier<MapCodec<Cuts.Loot>> CUT_MODIFIER = LOOT_MODIFIERS.register("cut", () -> Cuts.Loot.CODEC);
    public static final Supplier<MapCodec<Lights.Drop>> LIGHT_MODIFIER = LOOT_MODIFIERS.register("light", () -> Lights.Drop.CODEC);
    public static final Supplier<MapCodec<PlantFibreModifier>> PLANT_FIBRE_MODIFIER = LOOT_MODIFIERS.register("plant_fibre", () -> PlantFibreModifier.CODEC);
    public static final Supplier<MapCodec<SpruceNeedlesModifier>> SPRUCE_NEEDLES_MODIFIER = LOOT_MODIFIERS.register("spruce_needles", () -> SpruceNeedlesModifier.CODEC);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("backcountry", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.backcountry"))
            .icon(() -> new ItemStack(SATELLITE_MESSENGER.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private Things() {}

    private static DeferredBlock<TrapBlock> trap(String name, Kind kind) {
        return BLOCKS.registerBlock(name, properties -> new TrapBlock(properties, kind),
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noOcclusion());
    }

    private static DeferredBlock<BerryBushBlock> bush(String name, Berry berry) {
        return BLOCKS.registerBlock(name, properties -> new BerryBushBlock(berry, properties),
                BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));
    }

    private static DeferredItem<Item> berries(String name, DeferredBlock<BerryBushBlock> bush) {
        return ITEMS.registerItem(name, properties -> new ItemNameBlockItem(bush.get(), properties),
                new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    }

    private static DeferredItem<Item> food(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    }

    static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
        ATTACHMENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
        BLOCKS.register(bus);
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        LOOT_MODIFIERS.register(bus);
        MENUS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
        ENTITY_TYPES.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
    }
}
