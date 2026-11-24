# Design

What Backcountry is, decided before anything is built. Each document keeps its **Settled** decisions apart from its **Open** questions, numbered so an answer can name them.

## Now

**M1 Repo, tooling, world** is built: the pack, the dev client and server, the boot test, the release workflow, the island and its shape maps, cold biomes, no structures, surface iron, the mining rule and the drop-off. It waits for the user's first look in the game (`just play`). **M2 Body** is next.

## Milestones

Each milestone ends playable, with `just lint` and `just test` green.

| Milestone | Builds |
|---|---|
| M1 Repo, tooling, world | The pack and its tooling, the island (`world.md`), cold biomes, no structures, surface iron and the mining rule, the drop-off |
| M2 Body | The kcal body, energy, weight and BMI, water, temperature, wetness, clothing, sleep, conditions, the hotbar and pack, the HUD, extraction, death, the summary, the team (`systems.md` S1, S2, S5, S10) |
| M3 Satellite Messenger | The device's pages and alerts, the mod's weather and forecast (S3, S4) |
| M4 Fire and wood | The bow drill, the campfire screen, firewood and seasoning, fire spread (S6) |
| M5 Food | Kcal data for every food, spoilage, pots, the smoker, the food cache and rations, mice, berries, farming (S7) |
| M6 Hunting | The spawn director, seasonal events, bows and arrows, organ hits, blood trails, carcasses, cuts, hides, the mod's birds and mosquitoes (S8) |
| M7 Traps, fishing, water | The material chain, traps, bait, River Fishing's kcal and season, the raft (S9) |
| M8 The year | Balance from playtests: the first week, a winter, a whole year; the frame rate bench |

## Documents

| Document | Holds |
|---|---|
| `vision.md` | What the pack is and what the player does |
| `world.md` | The island, its climate and what stands on it |
| `systems.md` | Every mechanic with its edge cases |
| `interface.md` | The HUD and every screen |
| `mods.md` | Every mod in the pack and why, and those left out |
| `production.md` | How the pack reaches its players, and the performance budget |
