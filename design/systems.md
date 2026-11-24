# Systems

Every mechanic of Backcountry, what it does for the player, and its edge cases. The Backcountry mod builds each system itself unless a mod is named. Numbers live in `balance.toml`; those quoted here are the first values to tune from.

## Settled

### S1 One body, counted in calories

The mod owns the survivor's whole body, so every number is in kcal, °C, kg and days, and the HUD and the Satellite Messenger read one model. No public survival mod goes in beside it.

- **Food** is counted in kcal, fat, protein, carbs and vitamin C, read from data in the mod for every food of every jar in the pack (`data/backcountry/food/`). An item with no entry is not food.
- **A game day burns what a real day does:** a resting rate from body weight, plus work (chopping, running, carrying, building, mining), plus cold, plus illness (scurvy burns more). Building costs calories.
- **Weight follows the balance:** the day's surplus or deficit moves the fat reserve, 7,700 kcal to the kilogram. A survivor arrives at 77 kg and 1.80 m (BMI 23.8); at BMI 17 they are extracted (S10). Forge Labs went from 170 lb to 147.6 lb over his year.
- **Energy replaces hunger**: lightning bolts where the hunger bar was. Work drains it, sleep restores it, food, cold and the pack's load change it. Low energy slows chopping, stops sprinting, makes the survivor stumble and fall, black out, and drown when swimming in deep water.
- **Strength** follows body weight and energy: it decides whether a heavy bow draws fully.
- **Water:** drunk from the water bottle and the camp pot; dirty water has to be boiled. Dehydration knocks the survivor out.
- **Conditions and their cures:**
  - Wound infection, 15% a hit as in Forge Labs' earlier mod, and blisters from walking wet: bandages from plant fibre, the med kit.
  - Frostbite, which kills when severe: rest by the fire.
  - Burns.
  - Scurvy: berries or raw fish; cooking destroys vitamin C.
  - Food poisoning, gut infection (40% from dirty water) and parasites (60% from raw meat).
  - Too long without sleep.
- **Inventory:** the hotbar only, plus a hide pack that adds slots; what is carried weighs, and carrying too much drains energy and shows "Pack too full".
- **Edge cases:**
  - A player offline: their body stands still until they return.
  - Two players eating from one cache: each eats their own ration from it.
  - `/time set`: the calendar follows Ecliptic Seasons' day count, never the clock.

### S2 The year

- **48 days** of Ecliptic Seasons, 2 days to each of its 24 solar terms; a game day is 20 minutes, so a year is about 16 hours. Day 1 is 3 May, the last day of Grain Rain; one game day stands for about 7.6 calendar days, and dates are shown as dates (Day 9 · 2 Jul).
- Every duration (spoilage, seasoning, healing) counts game days.
- Daylight is long in summer (light at 9 pm) and short in winter, through Ecliptic's day length.
- On 3 May the year's summary shows for the team, and play goes on into year 2; nothing ends with the year.

### S3 Weather and the forecast

- The mod's own weather: an Alaska climate curve through the year, temperatures that drift day to day, rain, snow and blizzards, lows down to about -35 °C. It drives vanilla's weather, and Ecliptic's weather is held to it (spike: whether Ecliptic lets it).
- The forecast covers tomorrow, +2 and +3 days, less sure the further it looks.
- Animals react: fewer about in rain, and fishing changes with the weather.

### S4 The Satellite Messenger

- A hand-held device with seven pages:
  - Weather: temperature, feels like, precipitation, wind, cloud, date, high and low.
  - Forecast: tomorrow, +2 and +3 days.
  - Warmth: fire, shelter (open or roofed), wetness, clothing.
  - Body: core temperature, comfort, wetness, trend, fatigue.
  - Nutrition: protein, carbs, fat, energy.
  - Rations: fullness, the team's ration.
  - Vitals: weight, energy, BMI, the team's food stores in days, water.
- Alerts: thunderstorms or snowfall tomorrow, freezing overnight, bears feeding heavily, moose rut begun, body weight dropped, body wasting.
- **Edge cases:** a messenger lost or broken is crafted again from the supply crate at the drop-off.

### S5 Temperature and clothing

- Ambient temperature from S3, the biome and height; shelter (a roof within 4 blocks), wetness, fire heat and clothing insulation set the core temperature.
- Hide clothing with insulation: poncho, hide coat, hide pants, hat.
- Rawhide snowshoes for deep snow; walking deep snow without them frostbites the feet.
- A headlamp that dims in extreme cold, lit through LambDynamicLights (spike: a light worn on the head).

### S6 Fire and wood

- A fire is lit by a bow-drill minigame from a tinder bundle (birch bark), kindling (sticks) and a bow drill (cordage); flint and steel later.
- The campfire screen: a 4-slot fuel queue, heat, burning and queued time, Low, Steady and Hot, banked coals for a relight, a checklist (firewood queued, coals banked, out of the rain, cook raw food on it); with an empty queue it settles to coals.
- A fire packed too full sets the shelter alight, and the fire spreads. A shovel puts a fire out.
- Firewood is softwood or hardwood (hardwood from stripping birch), with a seasoning percentage; green wood is "too green to catch", and logs laid out on the ground dry.
- Trees fall whole (Panda's Falling Trees) and are split into logs.
- **Edge cases:** rain on an open fire burns it faster and puts it out once its queue is empty, leaving banked coals; a fire in an unloaded chunk waits.

### S7 Food, cooking and stores

- Every food shows kcal, fat and "spoils in X days here", which follows the temperature where it lies: 1 to 2 days in summer, 60 and more in winter.
- The camp pot to fill and boil, the cooking pot and stand (iron), a hanging pot over the fire; rendering fat in the cooking pot.
- Mushrooms (12 kcal), mushroom stew (180 kcal, hydrates), seasonal berries that regrow (blueberries, lingonberries).
- **The smoker**: crafted from cordage, logs, a campfire and cobblestone; hanging-food slots and firewood slots, 12-hour batches, a smoke level from cold to rising, dry firewood only; smoked meat keeps 75 to 1,350 days.
- **The food cache**: the team's ration (Lean, Normal or Feast), usable food in kcal, days at that ration, fat reserve, a spoilage multiplier from the temperature, and a scent that draws bears, wolves and wolverines ("Scent attracts scavengers").
- **Rations** are eaten at dawn and dusk from the team's caches within 64 blocks; out in the field, from food carried.
- Mice raid caches, eat their calories and leave droppings that contaminate them, fought with deadfall and mouse traps.
- Farming in real seasons: wheat planted May to June is ready in late August, germination is not certain, soil has to be warm, frost kills young plants, the number of plantings is limited.
- **Edge cases:** food in a cache keeps spoiling by its own chunk's temperature while nobody is near; a ration with no cache in reach eats carried food; a cache in an unloaded chunk counts as it was left.

### S8 Hunting

- **The spawn director** spawns every animal: it rations "animal opportunities" per species, area and season for the team (after a beaver, no new beaver comes for a while), puts them out of sight in their habitat, and keeps game trails. Natural spawns of anything not on its list are cancelled, which keeps out monsters and the mods' exotic animals. Traps draw on the same rations.
- **Seasonal events:** moose rut, bears feeding heavily before winter, salmon running up waterfalls, spawned-out salmon unfit to eat.
- **Animals:** moose and grizzly (Alex's Mobs), wolves in packs that attack a door, wolverine, beaver with dams that grow, territorial and diving, caribou, mouse, black bear (Critters n' Crawlers), squirrels that climb trees (Wilder Nature), snowshoe hares (vanilla rabbits, white in winter), raven, chickadee, Canada goose, gulls, mallard, hawk (Fowl Play), bald eagle, seal, orca (Alex's Mobs), and the mod's own grouse, ptarmigan, loon (feathers, its call at dusk) and summer mosquito swarms near water that bite and cost sleep.
- **Weapons:** thrown rocks, axe, knife, spear, bear spray.
- **Bows**: the Light and the Heavy Hunting Bow (60 lb draw); weakness reduces draw power; built from bow staves seasoned over the campfire (300 s), a tillered bow and a bowstring.
- **Arrows,** one per craft: blunt arrows for small game, made with game feathers; broadheads for wolf, moose and bear (recovered 78% from blocks, 58% from animals); bone arrows (70% and 48%). Arrows that pass through drop nearby; embedded arrows stay in the animal.
- **A hit has to reach a major organ;** otherwise the animal flees wounded, leaves a blood trail and has to be tracked.
- Moose call from birch bark, rawhide strips, cordage and a wooden dowel; a trail camera.
- **Carcasses**: "Sneak + empty-hand click to carry. Set it down and use your knife to dress it." Dressing has a progress bar; without a knife: "Skinning knife required".
- **Cuts** per animal, each with its own kcal, fat and spoil time: meat, heart, liver, kidney, tongue, tail (beaver), animal fat, game guts, pelt, head (a moose head trophy). Raw moose meat: 1,600 kcal, 10 g fat, spoils in 2.3 days in September.
- **Hides:** small pelt → scraped hide → cured hide (with rendered fat), rawhide strips.
- Every animal type, the mods' and vanilla's, is a data file: size, organ zones, cuts, scent and how it flees.
- **Edge cases:** two players hunting one area share one ration; a wounded animal that leaves loaded chunks dies out of sight and leaves its carcass at the trail's end.

### S9 Traps, fishing and water travel

- **Materials:** plant fibre → cordage → rope, snare wire ("bends into snares and hooks"), wire mesh, trap stakes, crossbeams.
- **Traps:** small snare, large snare (beaver), deadfall, mouse trap, minnow trap, gill net, ice-fishing tip-up; snares baited with a cricket.
- **Bait:** worms dug after rain, crickets, live minnows, bait from fish guts and a hook.
- **Fishing** is River Fishing's: its Alaskan species (grayling, northern pike, lake trout, Arctic char, burbot, whitefish, sockeye, chinook, coho, chum and pink salmon), with the mod giving each catch its kcal by weight (a large raw pike: 3.9 kg, 1,879 kcal) and answering its season from Ecliptic (spike: River Fishing reads only Serene Seasons).
- A raft, from which you can fall.

### S10 The team, arrival and the end of a survivor

- Everyone in a world is one team; in singleplayer it is a team of one. Caches, rations, food stores in days and the spawn director's rations are the team's; the body, the messenger and the kit are each survivor's.
- Every survivor arrives at the drop-off: a lake on the main island picked when the world is made, with the starting kit: Satellite Messenger, Bear Spray, Med Kit, Water Bottle, Pilot Bread ×38 (100 kcal, 2.5 g fat each), Camp Pot.
- At BMI 17 the survivor is extracted; at death they die. Either way the player sees the run's summary (days, weight curve, kcal eaten, animals taken) and arrives again at the drop-off as a new survivor, in the same world with its seasons and camps.
- **Edge cases:** an extracted or dead survivor's pack stays where they were, a container the team can open; a world with no fitting lake puts the drop-off on the bank of the first water found from the centre (`world.md` W4).

## Open

None.
