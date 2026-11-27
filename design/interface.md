# Interface

Everything read off the screen outside the world. Units are metric: °C, kg, km/h.

## Settled

### I1 The HUD

The hearts stay and the energy bolts, drawn like vanilla's hunger icons over empty slots, take the hunger bar's place; they shake while the survivor is too tired to sprint. Water is a row of droplets drawn the same way above the bolts, and they shake below a quarter. There is no XP (`systems.md` S12), so neither bar nor level shows, and the hearts and bolts sit down in the XP bar's row; air bubbles stack above the droplets. Right of the hotbar, each line on a dark plate with its icon, the body panel shows the core temperature and how the air feels through the clothes ("Body 35.6 °C · feels very cold", blue when the core is cold, orange when hot), the wetness while wet (a wet shirt, so a drop means only water), spirits only while high or low ("High spirits", "Low spirits", and at the bottom "Low spirits: you keep thinking of home"), and above them the conditions by name.

```text
                           💧💧💧💧💧💧💧💧░░
 ♥♥♥♥♥♥♥♥♥♥               ⚡⚡⚡⚡⚡⚡░░░░    ❄ Frostnip
 [ ][ ][ ][ ][ ][ ][ ][ ][ ]                ✚ Infected wound
                                            👕 Wet 37%
                                            🌡 Body 35.6 °C · feels very cold
```

With the inventory or a container open, the HUD shows under it, and hovering over the hearts, the bolts, the droplets or a line of the body panel shows a tooltip in vanilla's look with its name, its value and what fills it: "Energy 62%" with "Work, cold and a heavy pack drain it; sleep and food restore it." and "Below 20% you are too tired to sprint.", the water with how much to drink, the body line with what the two values are and, when cold, what helps, the wetness with what dries it, each condition with its cure. Nothing shows while an item is held on the cursor.

As the core falls below 36.5 °C, vanilla's own freezing frost creeps in from the screen's edges, full at 35 °C, as deep in powder snow, and it fades as the survivor warms; in powder snow the thicker of the two shows. Only that survivor sees it, never while dead, extracted or in creative, and F1 and third person treat it as they treat vanilla's frost.

No vanilla tutorial hint shows ("Punch a tree", "Craft wooden planks", "Move with W, A, S and D", "Open your inventory", "Social Interactions"), and no screen has the recipe book's button: recipes are JEI's and the tasks'.

Everything the pack draws wears vanilla's own look: its panels, buttons, slots and font.

The tracked task's next step shows at the top left on a dark plate (`systems.md` S11): the task and its count, the step, and the items it needs with what the survivor holds of them. A step done ticks with a soft click and the next takes its place; F1 hides it, and it is gone once every task is done. A task done shows a toast in vanilla's look at the top right for a few seconds ("Task done", the task's name and icon): a survivor's own task to them, a team task to everyone online.

```text
Light a fire · 2 of 7
▸ Make a Bow Drill
  [Stick] 2/2  [Cordage] 0/1  [Softwood] 2/1
```

The locator bar (`systems.md` S4) runs across the top centre in vanilla's locator-bar look, 182 pixels wide, spanning 120° of what lies ahead: a dot for each teammate in their own colour, ▲ or ▼ beside it when they are well above or below, fading with distance; a teammate behind the survivor sits at the nearer end of the bar as a half dot. The dot nearest where the survivor faces, within 16°, carries the teammate's name and distance in a line under the bar. It shows only while the survivor carries a Messenger and a teammate is in the world; F1 hides it, and Jade's box (below) opens under it.

```text
Make a stone axe · 1 of 4        ━━━━●━━━━━━━━▲━━━━        4x Stick
▸ Pick up a Rock                      Paul · 340 m
```

What comes into the inventory is listed at the top right for a few seconds, a line an item, counting up while more of it arrives ("4x Stick", Pick Up Notifier). The hotbar's items bounce as they land, are used, eaten or placed, the selected slot grows, a tool near breaking glows and the selected item's name pops in (Immersive Hotbar). While a survivor sneaks, a box at the top centre names what they look at and, under the name, the mod it comes from, in vanilla's tooltip look (Jade), with no health, state or contents; an animal, berries or a mushroom the survivor has not yet put in their field notes is named only by its kind ("Unknown animal", "Unknown berries", "Unknown mushroom", `systems.md` S13), so a forager crouching at a bush still has to tell a baneberry by its look. A few things also tell their state under the name: the Camp Pot ("Boiling · 1 min 40 s left", "Fire out · 1 min 40 s left", "Boiled water · safe to drink · 2.00 L", "Lake water · 2.00 L" off the fire, "Spruce tea · steeping · 1 min left", "Birch sap · syrup in 12 min", "Empty") the campfire (its time line as on its screen, I4, and a line for each piece cooking on it: "Raw Grouse · 20 s left") and a carcass ("Scavengers take it in 1 day 6 h"). Sign and water say what they are in place of the name: "Moose tracks · fresh", "Caribou droppings · half a day old", "Sea water · salt", "Lake water", "Stream water". Every item's tooltip ends with its mod in blue italics. Chat lines slide in (Chat Animation). Simple Voice Chat draws its own icon at the bottom left: a speaker struck through while voice is off or no device is found.

```text
                    ┌─────────────┐               [■] Spruce Log x4
                    │ Baneberry   │               [■] Blueberries x6
                    │ Backcountry │
                    └─────────────┘
                          +
```

### I2 The Satellite Messenger

Right-click opens it, drawn as the device itself: an orange case with a green-on-dark screen, the only screen with a skin of its own, as vanilla's book has its page. It fills the screen up to 640 by 360 GUI pixels (GUI scale 3 at 1080p) and stands centred on anything larger; it still fits vanilla's smallest screen, 320 by 240. While it is open the survivor stands still, since it covers the world (I8).

The case carries the device's name at the top left and a red SOS button at the top right. The header shows the day and date on the left and the time of day on the right. Under it eight tabs, each with its icon and name: Tasks, Weather, Year, Warmth, Food, Body, Notes, Guide, switched by a click, 1 to 8, A and D or the ← → keys; where the names don't fit across, the tabs show their icons alone and name themselves on hover; E or Esc closes it. The letters are the survivor's own movement keys, so a rebound layout moves through it the same way. A strip of alerts sits under every page, two lines at most, "and N more" past them, "No alerts" when there are none. Every value has its icon and a meter or gauge; warnings turn amber or red, and a value in warning carries one dim line under it saying what helps.

```text
┌ SATELLITE MESSENGER ───────────────────────────────────────────────────────── [SOS] ┐
│ Day 1 · 3 May                                                                 14:20 │
│ [▤ Tasks]    ☁ Weather    🔥 Warmth    🍖 Food    ♥ Body                            │
├─────────────────────────────┬───────────────────────────────────────────────────────┤
│ First day            1 of 5 │ Light a fire                         ◆ Tracked 2 of 7 │
│  ■ Make a stone axe         │ A fire warms and dries you, cooks and boils water.    │
│ ◆□ Light a fire             │ ■ Fell a spruce                                       │
│  □ Boil water               │ ■ Split it into Softwood                              │
│  □ Refill the Water Bottle  │ ▸ Make a Bow Drill                                    │
│  □ Sleep under a roof       │   [Stick] 2/2   [Cordage] 0/1   [Softwood] 2/1        │
│ Camp                 0 of 5 │   Grass and ferns drop Plant Fibre; 4 twist into      │
│ Food                 0 of 6 │   a Cordage.                                          │
│ Hunting              0 of 5 │ □ Make a Tinder Bundle                                │
│ Before snow · team   0 of 3 │ □ Build a Campfire and set it down                    │
│ Winter               0 of 2 │ □ Light it with the Bow Drill                         │
│ The year · team      0 of 1 │ □ Feed it Softwood                                    │
├─────────────────────────────┴───────────────────────────────────────────────────────┤
│ ⚠ Frost tonight                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

- **Tasks** (`systems.md` S11): on the left the chapters, each with its count ("2 of 5", "· team" beside the team's); the chapter of the chosen task is open with its tasks, the others folded to their heading, and a click on a heading opens it. On the right the chosen task: its name, its count, one line on why it matters, and its steps, each with a box filled once done; the first open step shows the items it needs with what the survivor holds of them ("2/2", met in the screen's text colour, unmet dim) and how to get them. Hovering an item names it with "Click for its recipe", and a click opens its recipe in JEI. A task carrying a season reads "From June" while out of it. The tracked task wears ◆ in the list and "◆ Tracked" beside its name; another task shows a Track button there. W and S or ↑ ↓ choose a task, Space or Enter tracks it.
- **Weather:** the air and how it feels on a gauge from -40 to +30 °C banded at freezing and at -15 °C, the rain or snow now or later today with its hours, the wind on a meter with the blizzard mark at 56 km/h, the cloud, today's high and low; then the next three days a line each: its sky (the icon of the rain or snow when it is likely, else of the cloud), the day and date, the chance ("Snow 80%"), the high and low and the wind.
- **Year** (`systems.md` S4): twelve months with today's in the middle, lit, five before it and six after. Under "Usual here", a grid of numbers, a column a month: High °C and Low °C (blue at freezing, red at -15 °C), Daylight h and Wet days. Under it a row for each thing of the season, its name on the left and a bar across its months, bright where it happens and dim for its quiet side; hovering a month of a bar says what it means.

```text
│ Usual here   Dec Jan Feb Mar Apr [May] Jun Jul Aug Sep Oct Nov │
│ High °C       -3  -4  -2   1   7 [ 14]  18  19  18  13   6  -1 │
│ Low °C       -10 -10  -9  -7  -1 [  5]  10  12  11   6   0  -7 │
│ Daylight h     6   7   9  12  15 [ 18]  19  18  16  13  10   7 │
│ Wet days      11   8   8   6   5 [  7]   9  12  14  15  12  10 │
│ Berries      ▒▒▒▒▒▒▒▒▒▒▒▒             ███████████████████▒▒▒▒ │
│ Salmon run                                ███████████          │
│ Bears        ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒████████████████████████████▒▒▒▒ │
│ Moose rut                                        ███████       │
│ Cows with calves             [████]████                        │
│ Lake ice     ████████████████▒▒▒▒                       ████ │
```

- **Warmth:** the core temperature with how it feels and its trend on a gauge banded freezing, cold, comfortable and hot, then what sets it: fire, shelter or the room with its air ("Cabin · 9 °C inside", amber with what helps when smoky), clothing and wetness.
- **Food:** the day's kcal eaten and burned, the burn by cause in a dim line ("Rest 2,170 · Move 190 (5.1 km) · Load 40 · Work 620 · Cold 250 · Digest 320") and protein, carbs and fat; the stomach; the ration, the next meal and the team's food stores in days.
- **Body:** the weight on a BMI gauge with extraction marked at 17, energy, water and the load carried, spirits on a meter (High, Even, Low) with a dim line of what moves them now ("a fire, sun, the same food again"), hours awake, the conditions each with what cures it, and the four skills, each with its name and five steps lit to its level.
- **Notes** (`systems.md` S13): "Field notes" and the count ("27 of 60"); under it three columns, Animals, Fish and Plants, each with its own count and a line for each thing noted, its name and the date. W and S, ↑ ↓ or the mouse wheel scroll them. With nothing noted yet a line says how notes are made.
- **Guide** (`systems.md` S4): on the left the topics, the chosen one lit, the list scrolling to keep it in sight on a small screen; on the right its title and its text. A click or W and S or ↑ ↓ choose a topic.

```text
│  Rations              │ Caches and scent                       │
│ ▸Caches and scent     │ A cache holding 1,024 kcal of meat or  │
│  Wolves and bears     │ fish smells: bears, wolves and         │
│  Water                │ wolverines come twice as often within  │
│  Fire and firewood    │ 64 blocks. A bear eats from a ground   │
│  Cold                 │ cache and a wolverine eats and fouls a │
│  Health               │ stack, unless you are within 16 blocks.│
│  Food and cooking     │ Set the cache on 3 stripped logs and   │
│  Fat                  │ bears, wolverines and mice can't reach │
│  Spirits              │ it; a ladder beside the poles lets     │
│  Hunting and tracking │ them up.                               │
```

```text
Warmth    Core                          35.6 °C · very cold · cooling
            Get to a fire or under a roof, and dry off.
          Shelter                       Open sky · in the wind
            A roof within 4 blocks overhead takes the wind off.
Food      Food stores                   0.0 days
            Put food in a Food Cache: the rations eat from caches within 64 blocks.
```

A new alert beeps once and lights a small amber light on the Messenger's icon in the hotbar until the Messenger is opened.

**SOS** taps a survivor out, as on *Alone* (`systems.md` S10). At the bottom of a survivor's spirits it glows, blinking slowly, and nothing more. A click on it asks on the screen, the tabs unlit:

```text
│ Send SOS and tap out?                                                               │
│ A boat picks you up, and your run ends here.                                        │
│ Your pack stays where you stand.                                                    │
│ A new survivor arrives at the drop-off.                                             │
│ [Tap out] [Cancel]                                                                  │
```

Only a click on Tap out confirms; a tab, Cancel, E or Esc drop the question, and no other key answers it. The button is dim and does nothing for a player who is not a living survivor in survival.

### I3 Food

```text
Raw Moose Meat
1,600 kcal · 10 g fat
Spoils in 2.3 days here (9 °C)
```

A food holding water names it on the first line ("30 kcal · 0.2 g fat · 0.04 L water"); a raw one adds "Raw: parasites 3 times in 5. Cook it on a fire.", and Animal Fat "Render 2 in the Cooking Pot.". A spoiled food reads "Spoiled" in red instead, a smoked one adds "Smoked", and one the mice reached adds "Contaminated by mice" in red.

### I4 The campfire

```text
┌ Campfire ─────────────────────────────────────────┐
│ Fuel queue  [Softwood] [Hardwood] [   ] [   ]     │
│               64%        81%                      │
│ Heat ███████░░░   Low · [Steady] · Hot           │
│ Burning 1 h 20 min · queued 3 h                  │
│ Coals 2 h 30 min                                  │
│ ✓ Firewood queued  ✓ Coals banked                 │
│ ✗ Out of the rain  · Cook raw food on it          │
└───────────────────────────────────────────────────┘
```

It stands on vanilla's panel above the survivor's inventory, its locked rows dark. Each queued piece shows its seasoning under its slot, and the shown setting is the pressed button. Settled to coals, the time line reads "Coals 2 h 30 min: add firewood and it catches"; cold, "Cold: light it with a bow drill or a ferro rod"; packed too full, a red line "Packed too full: sparks may catch" joins the checklist. Out of the rain is ticked under a roof. Under snowy spruce branches "✗ Under snowy branches" joins it, and in a sealed room without a way out for the smoke "✗ Smoke hole" (`systems.md` S6). The Stone Fireplace opens the same screen.

The bow-drill game is a small panel over the world, the friction bar first and then the ember's glow with its band and the flame growing beside it:

```text
┌ Bow Drill ──────────────────────┐   ┌ Bow Drill ──────────────────────┐
│ Friction ██████░░░░░░            │   │ Glow  ░░░░[██░]░░   Flame ███░░ │
│ Saw with ◀ and ▶ in turn         │   │ Blow with Space: keep it in the band │
└──────────────────────────────────┘   └──────────────────────────────────┘
```

### I5 The smoker and the food cache

Both stand on vanilla's panel above the survivor's inventory.

```text
┌ Smoker ───────────────────────────┐
│ Hanging   [  ][  ][  ][  ]        │
│ Firewood      [  ][  ]            │
│ Smoke: Rising · 3 h 10 min of wood│
│ Batch ██████░░░░░░░░░░            │
│ 5 h of 12 h                       │
└───────────────────────────────────┘
┌ Food Cache ────────────────────────────────────┐
│ Food 12,400 kcal · 6.1 days on Normal          │
│ Fat reserve 169,400 kcal                       │
│ Spoils ×0.5 here (3 °C)                        │
│ Scent attracts scavengers: keep it out of camp │
│ [ Lean ] [Normal] [ Feast ]                    │
│ [ 18 slots ]                                   │
└────────────────────────────────────────────────┘
```

The shown ration is the pressed button; the scent line is red and shows only with 1,024 kcal of meat or fish. On 3 stripped poles a line reads "Tall cache: out of reach of bears, wolverines and mice", or "A ladder against the poles lets climbers in" in amber while a ladder stands beside them. The Drying Rack shows its pieces hanging on the frame in the world, with no screen: a right-click hangs a piece or, with an empty hand, takes one back.

### I6 Hunting

A hit to a major organ drops the animal; any other hit: "Wounded, it fled" on the action bar, and a trail of blood on the ground. A carcass on the ground: "Sneak + empty-hand click to carry. Set it down and use your knife to dress it."; dressing fills a bar on the action bar, "Dressing the Moose ▮▮▮▯▯▯▯▯▯▯"; without a knife: "Skinning knife required". A carried carcass is an item named for its animal ("Moose Carcass"). The bows draw as vanilla's bow does, in their own wood colours.

### I7 The end of a survivor and of a year

```text
Extracted · Day 31 · 17 Dec
BMI 17.0 · 55.0 kg (arrived at 77.0 kg)
 77 ┤▀▀▄▄▄
    │     ▀▀▀▄▄▄
 55 ┤           ▀▀▄
    └ Day 1 ──── Day 31
31 days · 61,200 kcal eaten · 1 moose · 3 beaver · 14 hare · 22 fish
[ Arrive as a new survivor ]
[ Title Screen ]
```

It stands on vanilla's dark panel: the weight curve is a chart with 5 kg grid lines, the first and the last day, and a dashed red line where extraction comes; beside it the days, the kcal and each animal taken, each with its icon (the animal's spawn egg), three at most and "and N more". A death reads "Died" over vanilla's red, a survivor who sent SOS "Tapped out · Day 31 · 17 Dec", and everyone reads "Roman tapped out on Day 31" in chat. On 3 May the team's year shows, each survivor with their weight from start to end and when they arrived, died or were extracted, and "Year 2" begins:

```text
Year 1 · 3 May
Roman 77.0 → 68.4 kg
Mo 77.0 → 71.2 kg (arrived Day 12)
Ana 77.0 → 69.8 kg (tapped out Day 40)
48 days · 412,000 kcal eaten · 2 moose · 5 beaver · 31 hare
[ Go on into Year 2 ]
```

The inventory shows the rows no pack opens dark ("Needs a pack", and over the second row "Needs a hide pack"). The worn slots are Curios' own panel, opened with G or the button beside the survivor: Back, Neck and Hands, each showing the outline of what it takes while empty.

### I8 Controls and the first launch

- **Keys:** Left Shift sprints and Left Ctrl sneaks; every mod's key is unbound unless a player needs it: River Fishing's fight keys on the arrows, Ping Wheel's mark on mouse button 5, JEI's keys inside its own screens (R recipes, U uses, A bookmark, Ctrl+F search), V for the voice menu, U to swap the shoulder in third person, the middle mouse button to sort the inventory under the cursor, C to look through the binoculars, G for the worn slots (Curios). F5 steps from first person to over the shoulder to facing the survivor (Shoulder Surfing Reloaded); the game starts in first person. Iris's, Ecliptic Seasons' debug and Critters n' Crawlers' keys, JEI's cheat-to-inventory keys, Save Hotbar Activator and L for advancements are unbound. Controls has a search box (Controlling). Controls' Reset Keys brings back the pack's keys.
- **Screens open:** a survivor keeps walking with the inventory, a chest or a cache open, the world showing behind it (InvMove); the Messenger, which covers the world, the bow-drill game, whose A, D and Space saw and blow, the summary and typing in JEI's search hold them still.
- **Team marks:** mouse button 5 marks a spot for the whole team for a few seconds, with who set it and how far it is (Ping Wheel).
- **The first launch** (Default Options): FOV 80, GUI scale 3, full screen, VSync on, an unlimited frame rate, sensitivity 67%, Brightness Bright, no splash texts, no accessibility onboarding, no multiplayer warning; the graphics Preset reads High. Each player's own settings stay theirs after that.
- **Video Settings** opens on a Backcountry page, first in Sodium's list, in Sodium's own look: a Preset that steps Low, Medium, High and Ultra, and Shaders on or off (`production.md`). A player who launched before reads Custom until they pick a step.

```text
Options › Video Settings › Backcountry › Graphics
  Preset    High
  Shaders   [x]
```
- **Nights** are dark: without the shader More Darkness makes a moonless night black beyond the fire and a full moon pale grey, the eyes adjusting over a few seconds; with Complementary Reimagined on, as it is from the first launch, More Darkness steps aside and the shader's night settings give the same night (`production.md`).
- **Create New World** offers what the pack plays: Game Mode holds Survival and Creative (a Hardcore death would end the world, where the pack's brings a new survivor), Difficulty stands greyed at Normal, the World tab holds only the seed, with no world type, Generate Structures or Bonus Chest, and the More tab holds Game Rules and Data Packs without Experiments. Creating the world goes straight to loading, with no warning about experimental settings, since every world of the pack has them; a Hardcore world made before keeps its mode.
- **The pause menu** has Statistics across the whole row where Advancements and Statistics stood.
- **F3** shows no coordinates, facing or biome.
- **Sodium's donation prompt and Iris's update notice** never show.
- **The title screen** shows the island, photographed in the game at the drop-off at dawn, noon, dusk and night, one set at random each start (`python3 scripts/client.py panorama`); until the island's terrain is settled it shows vanilla's panorama.
- **Edge cases:** a player who launched the pack before keeps their own settings and keys, Reset Keys giving them the pack's; a key a player rebinds stays theirs through updates.

### I9 The ten items

A survivor arriving at the drop-off first chooses their kit, on a screen over the world in vanilla's look, before anything else shows:

```text
                      Choose your ten items
Issued: Satellite Messenger, Bear Spray, Med Kit, Water Bottle, Binoculars, Canvas Pack, Headlamp
┌ Your picks ───────────────────────────────── 7 of 10 ┐
│ [Axe] [Bow Saw] [Belt Knife] [Ferro Rod] [Camp Pot] [Gill Net] │
│ [Snare Wire] [Fishing Kit] [Hunting Bow] [Tarp] [Paracord] [Sleeping Bag] │
│ [Sleeping Pad] [Ice Auger] [Shovel] [Pilot Bread] [Pemmican] [Sharpening Stone] │
└──────────────────────────────────────────────────────┘
                          [ Arrive ]
```

Each pick is a slot with its item; hovering names it and lists what it holds ("Fishing Kit: 2 Mono Line, 25 Hook"), and a click picks it, filling the slot and outlining it, or puts it back. Past ten, a click picks nothing until one goes back. Arrive takes what is picked, fewer than ten included; Esc does nothing, since a survivor has to arrive. A narrator reads each pick with "picked" when it is.

### I10 The Birch-Bark Map

Use opens it over the world: the sheet of bark drawn in charcoal, blank where nothing was surveyed, the land's colours toned into the bark, a × at each survey and N at the top right; under it "3 surveys · 8 blocks a pixel". A blank map reads "Blank. Climb high, then sneak and hold use to survey: it takes a quarter hour of daylight and a piece of charcoal." There is no arrow and no coordinate. E or Esc closes it, and the survivor stands still while it is open.

## Open

None.
