# Design

What Backcountry is, decided before anything is built. Each document keeps its **Settled** decisions apart from its **Open** questions, numbered so an answer can name them.

## Now

**M1 Repo, tooling, world** to **M17 Binoculars** are built, M1 to M12 balanced by automated playthroughs (`playthrough` skill). M2 to M17 wait for the user's first play (`just play`, the `playtest` skill's rows). M16's title panorama waits until the island's terrain is settled.

## Milestones

Each milestone ends playable, with `just lint` and `just test` green.

| Milestone | Builds |
|---|---|
| M1 Repo, tooling, world | The pack and its tooling, the island (`world.md`), cold biomes, no structures, surface iron and the mining rule, the drop-off |
| M2 Body | The kcal body with data for every food of every jar, energy, weight and BMI, water with the Water Bottle and the Camp Pot boiled on a campfire, the climate curve, temperature, wetness, clothing and the Hide Pack (from vanilla leather and rabbit hide until M6), plant fibre and the bandage, sleep, conditions, the hotbar, the HUD, the Satellite Messenger with its Warmth, Body, Nutrition and Vitals pages, the Supply Crate, the Bear Spray, the year's calendar and daylight, extraction, death, the summary, the team; game tests and the dev drivers (`systems.md` S1, S2, S5, S10) |
| M3 Satellite Messenger | The Weather and Forecast pages and the alerts, the mod's weather and forecast around the climate curve (S3, S4) |
| M4 Fire and wood | The bow drill, the campfire screen, firewood and seasoning, fire spread (S6) |
| M5 Food | Spoilage, the cooking pot and stand, the hanging pot, the smoker, the food cache, rations and the Messenger's Rations page, mice, berries, farming (S7) |
| M6 Hunting | The spawn director with fewer animals about in rain, seasonal events and their alerts (bears feeding heavily, moose rut begun), strength, bows and arrows, organ hits, blood trails, carcasses, cuts, hides with the hide clothing and the Hide Pack made from them, rawhide snowshoes, the mod's birds and mosquitoes (S8) |
| M7 Traps, fishing, water | The material chain from cordage on, traps, bait, River Fishing's kcal, season, weather and catches counted in the summary, the raft (S9) |
| M8 The year | Balance from playtests: the first week, a winter, a whole year; the frame rate bench; the walking dev driver and the `playthrough` skill |
| M9 Tasks | The Messenger's Tasks page: the first days and the year's goals (S11, I2) |
| M10 The body measured | Every activity's kcal by the game time it fills at real rates, with load, terrain and snow, slope, wind, cold, clothing, digestion and sleep; skills; the rations and the energy bar rescaled; the playthroughs run again (S1) |
| M11 Forage | Spruce tea, salmonberries, highbush cranberries and baneberries, the mushroom stew, dead spruces, birch sap and syrup, frozen fish (S6, S7) |
| M12 Bears and the sled | The grizzly's temper by season and surprise with its bluff charge, sows with cubs, bears at the salmon run, the timid black bear; the sled (S8) |
| M13 A survivalist's island | What a survivalist in Alaska would not meet or do cut from vanilla and every mod: no caves or ore in the rock, no Nether, XP, magic, machines or exotic gear, Alaska's fish only, logs that need an axe, torches and lanterns that burn down, a locked Normal difficulty (S6, S12, `world.md` W3); the pack's keys, first-launch settings, Ping Wheel's marks and dark nights (`interface.md` I8, `production.md`) |
| M14 Company and comfort | Voice within earshot, teammates' typing and screens, sitting, animated players, the pick-up list, the animated hotbar, Jade's name while sneaking, dragging, sorting and walking with screens open, footprints and worn paths, wakes, thunder by distance, new sounds and music, breath by the air and a growl by the stomach, the far land from the seed and the speed mods (`systems.md` S1, S5, S10, `world.md` W5, `interface.md` I1, I8, `production.md`) |
| M15 The Messenger and the tasks | The Messenger full screen with five named pages and what helps under each warning, the clock; the year's tasks in seven chapters of steps with items, the tracker and the toasts; the Canvas Pack, the Sleeping Bag, line and hooks in the kit; lashed stone tools, hewn planks and bulk by stack size; Create New World as the pack plays (`systems.md` S1, S4, S6, S9, S10, S11, `interface.md` I1, I2, I8) |
| M16 Clearer screens | Water as droplets above the bolts, the body panel's words and every HUD element named on hover, frost at the screen's edges when cold, no vanilla tutorial or recipe book, the Messenger by the movement keys and SOS to tap out, the Backcountry Preset in Video Settings with Complementary Reimagined, the island on the title screen (`interface.md` I1, I2, I7, I8, `systems.md` S10, `production.md`) |
| M17 Binoculars | The Binoculars in the kit, worn in the neck slot above the pack and looked through with C or in hand at 8× (`systems.md` S1, S8, S10, `interface.md` I8) |

## Documents

| Document | Holds |
|---|---|
| `vision.md` | What the pack is and what the player does |
| `world.md` | The island, its climate and what stands on it |
| `systems.md` | Every mechanic with its edge cases |
| `interface.md` | The HUD and every screen |
| `mods.md` | Every mod in the pack and why, and those left out |
| `production.md` | How the pack reaches its players, and the performance budget |
