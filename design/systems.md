# Systems

Every mechanic of Backcountry, what it does for the player, and its edge cases. The Backcountry mod builds each system itself unless a mod is named. Numbers live in `balance.toml`; those quoted here are the first values to tune from.

## Settled

### S1 One body, counted in calories

The mod owns the survivor's whole body, so every number is in kcal, °C, kg and days, and the HUD and the Satellite Messenger read one model. No public survival mod goes in beside it.

- **Food** is counted in kcal, fat, protein, carbs and vitamin C, read from data in the mod for every food of every jar in the pack (`data/backcountry/data_maps/item/food.json`). An item with no entry is not food. Eating fills the stomach, which digests a few hundred kcal an hour; a full stomach refuses more ("Too full to eat").
- **A game day burns what a real day does:** a resting rate from body weight, plus work (chopping, running, carrying, building, mining), plus cold (shivering), plus illness (scurvy burns more). Building costs calories.
- **Weight follows the balance:** the day's surplus or deficit moves the fat reserve, 7,700 kcal to the kilogram. A survivor arrives at 77 kg and 1.80 m (BMI 23.8); at BMI 17 they are extracted (S10). Forge Labs went from 170 lb to 147.6 lb over his year.
- **Energy replaces hunger**: lightning bolts where the hunger bar was. Work drains it, sleep restores it, food, cold and the pack's load change it. Low energy slows chopping, stops sprinting ("Too tired to sprint"), makes the survivor stumble and fall, black out, and drown when swimming in deep water.
- **Strength** follows body weight and energy: it decides whether a heavy bow draws fully.
- **Water:** about 3 L a day, drunk from the Water Bottle (1 L) and the Camp Pot (2 L), which both fill at any water. The Camp Pot set on a lit campfire boils its water in about 4 minutes, and the bottle fills from it; unboiled water gives a gut infection 40% of the time. Dehydration knocks the survivor out, a heart lost each time until they drink.
- **Health** comes back a heart every 2 game hours while energy and water are above a quarter and no infected wound or burn is open, twice as fast asleep.
- **Conditions,** durations in game days:

  | Condition | From | Does | Ends |
  |---|---|---|---|
  | Infected wound | 15% of hits that hurt, as in Forge Labs' earlier mod | Fever: +10% kcal burn, no healing; after 2 days health drains | A bandage halts it and it heals 2 days later; the Med Kit at once |
  | Blisters | Walking with wet feet | No sprinting | 1 dry day, a bandage or the Med Kit |
  | Frostnip, frostbite, severe frostbite | Core below 35 °C, or a bare head at -15 °C and colder | Frostbite: slower, no sprinting; severe: health drains until death | Each stage falls back after 2 hours by a fire |
  | Burns | Fire, lava, a campfire | No healing | 2 days, halved by a bandage; the Med Kit at once |
  | Scurvy | 8 days without vitamin C | +20% kcal burn, wounds heal at half speed | Berries or raw fish; cooking destroys vitamin C |
  | Food poisoning | Foods the data marks, spoiled food | Vomiting: the stomach's food and 1 L of water are lost | 1 day; the Med Kit at once |
  | Gut infection | 40% of unboiled drinks | Water loss doubled, half the kcal absorbed | 2 days; the Med Kit at once |
  | Parasites | 60% of raw meat eaten | A quarter fewer kcal absorbed | Only the Med Kit |
  | Exhausted | 2 days awake | Energy can't rise above half | Sleep |

  Too warm (core above 37.5 °C) is no condition: the survivor sweats, losing water and getting wet. A bandage is two plant fibre, which grass and ferns drop; the Med Kit holds 4 uses, each curing every condition it cures at once.
- **Inventory:** the hotbar only. A Hide Pack worn in a back slot beside the armour opens one row of 9 above the hotbar, the rows it does not open stay dark ("Needs a hide pack"), and its items stay in it when it comes off or is dropped. What is carried weighs; above 16 kg energy drains faster and the action bar shows "Pack too full".
- **Edge cases:**
  - A player offline, in creative or in spectator: their body stands still until they return to survival.
  - A night slept through: every skipped hour counts as sleep, burning and drinking at rest.
  - Two players eating from one cache: each eats their own ration from it.
  - `/time set`: the calendar follows Ecliptic Seasons' day count, never the clock, and the body lives only the ticks that pass.

### S2 The year

- **48 days** of Ecliptic Seasons, 2 days to each of its 24 solar terms; a game day is 20 minutes, so a year is about 16 hours. Day 1 is 3 May, the last day of Grain Rain; one game day stands for about 7.6 calendar days, and dates are shown as dates (Day 9 · 2 Jul).
- Every duration (spoilage, seasoning, healing) counts game days.
- A date is 3 May plus (day - 1) × 365/48 days, rounded down: Day 9 · 2 Jul, Day 31 · 17 Dec. From Year 2 on, dates carry the year (Year 2 · Day 31 · 17 Dec).
- Daylight follows Anchorage, 61° N, through Ecliptic's day length per solar term: about 19.4 h of light at midsummer (16 minutes of a game day) and 5.5 h at midwinter (4.6 minutes).
- On 3 May the year's summary shows for the team, and play goes on into year 2; nothing ends with the year. It opens at each survivor's first quiet moment (on the ground, out of water, not hurt for 8 s, no screen open); someone offline sees it when they next join.

### S3 Weather and the forecast

- **The mod's own weather**, one sky for the whole world. Each game day is drawn from the world seed and the day: how far it drifts from S5's climate curve (by about 8 °C in winter and 2 °C in summer, half of it carried into the next day, so cold snaps bring lows to about -35 °C in the coldest places), its cloud, its wind and its direction, and at most one spell of rain or snow, 2 to 16 hours long, light, moderate or heavy. Wet days come as often as Anchorage's days with precipitation (NOAA 1991-2020: 5 in 30 in April, 15 in September). A thunderstorm is a rain day from June to August, one in four.
- **Rain or snow follows the air** where it falls: snow at +1 °C and colder, so it can rain at the beach while it snows in the grove above. Ecliptic's snow on the ground builds up while snow falls near sea level and melts on days above freezing.
- **A blizzard** is snow with wind of 56 km/h or more (the US weather service's threshold): outdoors the fog closes in to about 32 blocks and the light turns grey, and Distant Horizons' far view is fogged with it. Under the Complementary shader the blizzard wears the shader's own snow fog.
- **Wind chills the body** in the open (the wind-chill formula of Environment Canada and the US weather service, above 4.8 km/h and at 10 °C and colder); a roof within 4 blocks, dense canopy included, takes the wind off (S5).
- **Lightning** flashes, thunders and hurts what it hits, but sets no fire and brings no skeleton riders.
- **The forecast** covers tomorrow, +2 and +3 days, issued each morning at 06:00 when the day turns: the real coming day with an error that grows each day further (2 °C more per day), a chance of rain or snow that drifts towards the season's usual chance, and the high and low where the survivor stands.
- **How it is held:** rain and thunder are vanilla's two world switches, which Ecliptic only reads; its own seasonal rain is off (`UseSolarWeather`), and the mod sets the switches each tick to its day. Ecliptic's rain-or-snow choice and its weather effects are answered by mixins from the mod's air.
- **Edge cases:** sleeping through a spell, `/time set` and `/backcountry day` give whatever that day and hour has; an empty server holds nothing; `doWeatherCycle` off leaves the mod's weather going and stops Ecliptic's snow on the ground changing; `/weather` is refused ("The weather follows the forecast") and a command block's the same; every survivor sees one sky, but the temperature, rain or snow and the alerts are each one's own spot.
- Animals react (fewer about in rain) with the spawn director in M6, and fishing changes with the weather in M7.

### S4 The Satellite Messenger

- A hand-held device with seven pages:
  - Weather: the air and how it feels in the wind, precipitation now or later today with its hours, wind and direction, cloud, today's high and low here.
  - Forecast: tomorrow, +2 and +3 days, each with its chance of rain or snow, high, low and wind.
  - Warmth: fire, shelter (open, open in the wind, or roofed), wetness, clothing.
  - Body: core temperature, comfort, wetness, trend, fatigue.
  - Nutrition: protein, carbs, fat, energy.
  - Rations: fullness, the team's ration.
  - Vitals: weight, energy, BMI, the team's food stores in days, water.
- **Alerts**, each survivor's own: snow, a blizzard or a thunderstorm tomorrow (a chance of 50% or more here), frost tonight (tonight freezes here after a night above 0 °C), weight dropped (2 kg below where it last alerted; weight regained raises the mark), body wasting (BMI below 18.5, the WHO's underweight line). The weather alerts last until 06:00, weight dropped a day, body wasting until the BMI is back above it at a new day. Bears feeding heavily and moose rut begun come with M6's seasonal events.
- **A new alert** beeps once while the survivor carries a Messenger, and an amber light shows on its icon until it is opened.
- **Edge cases:** a messenger lost or broken comes again from the Supply Crate at the drop-off, which stands there from the world's first day and cannot be broken: a survivor carrying no messenger right-clicks it and takes a new one ("You already carry a Satellite Messenger" otherwise). Unread alerts wait for a messenger carried again; alerts missed while offline do not beep; an extracted or dead survivor's alerts go with them; a teammate's messenger shows its carrier's alerts.

### S5 Temperature and clothing

- Ambient temperature follows a climate curve, Anchorage's monthly normal highs and lows through the year and the day, shifted by the biome and by height; S3's weather drifts from it and cloud narrows the swing between day and night. Shelter (a roof within 4 blocks), wind in the open, wetness, fire heat and clothing insulation set the core temperature; rain and snow wet the survivor by their intensity.
- The survivor arrives in clothes of their own; hide clothing adds insulation: the Hide Hat, the Hide Poncho (the lighter chest piece, worn instead of the coat), the Hide Coat and the Hide Pants.
- Rawhide snowshoes for deep snow; walking deep snow without them frostbites the feet.

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
- **Weapons:** thrown rocks, axe, knife, spear, bear spray. Held right-click sprays the Bear Spray in a cone 4 blocks long, 8 bursts a can; a creature caught in it can't attack and flees for 30 s, and a survivor spraying point-blank is blinded for 4 s.
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
- At BMI 17 the survivor is extracted: their screen fades, they leave the world, and everyone reads "Roman was extracted at BMI 17.0". At death they die. Either way the player sees the run's summary (days, weight curve, kcal eaten, animals taken) and arrives again at the drop-off as a new survivor, in the same world with its seasons and camps. Beds and sleeping bags never move where a survivor arrives.
- **Edge cases:** an extracted or dead survivor's pack stays where they were, a container the team can open ("Roman's pack"), on the nearest ground within 8 blocks or the sea floor beneath; a survivor ending with nothing carried leaves none; leaving the game on the summary brings it back on the next join; a world with no fitting lake puts the drop-off on the bank of the first water found from the centre (`world.md` W4).

## Open

- **O1 The headlamp.** S5 wants a headlamp that dims in extreme cold, lit through LambDynamicLights (spike: a light worn on the head), but not where it comes from or what powers it.
