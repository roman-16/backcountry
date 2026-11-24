# Design

What Backcountry is, decided before anything is built. Each document keeps its **Settled** decisions apart from its **Open** questions, numbered so an answer can name them.

## Now

**M1 Repo, tooling, world**, **M2 Body**, **M3 Satellite Messenger**, **M4 Fire and wood**, **M5 Food**, **M6 Hunting**, **M7 Traps, fishing, water** and **M8 The year** are built, balanced by automated playthroughs (`playthrough` skill). M2 to M8 wait for the user's first play (`just play`, the `playtest` skill's rows).

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

## Documents

| Document | Holds |
|---|---|
| `vision.md` | What the pack is and what the player does |
| `world.md` | The island, its climate and what stands on it |
| `systems.md` | Every mechanic with its edge cases |
| `interface.md` | The HUD and every screen |
| `mods.md` | Every mod in the pack and why, and those left out |
| `production.md` | How the pack reaches its players, and the performance budget |
