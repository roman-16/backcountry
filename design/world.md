# World

## Settled

### W1 One island, then deep ocean

- **The land** covers the area of a 6,144-block circle on every seed: a main island with a coast broken by bays, sounds and islets, and on some seeds big islands across open water. Past it, deep ocean to Minecraft's own world edge: no land lies more than 6,144 blocks from the centre.
- **The mountains** stand in two or three broad, snow-capped massifs with foothills on their flanks; between them the lowland rolls, cut by many narrow rivers and dotted with lakes.
- **How it is made:** the mod wraps Tectonic's raw continents (`tectonic:noise/raw_continents`, `IslandShape`) where the game wires the noise. Over the middle it lifts the value by `lift`; from `inner_share` to `outer_share` of the radius it falls to `-sink`, the distance bent by two noises (`island_warp`, `island_detail`) so the coast is never round; past `outer_share`, and past `edge_blocks` from the centre, it is held at the sea floor, so nothing grows there. The first time a world is used, its land is measured on a 96-block grid and the shape scaled so the land covers the target area; since rivers, lakes and flats at sea level take part of what the continents count as land, the measure aims at the target divided by `dry_share`. The numbers live in `balance.toml` `[world]`.
- **Tectonic** (`pack/config/tectonic.json`): `continents_scale` 0.5 (small continent features, so the coast breaks into bays, peninsulas and islets), islands off, the deep floor at `deep_ocean_depth` -0.75 (about 96 blocks down), Tectonic's own erosion (`erosion_scale` 0.25: the mountains gather in a few broad massifs, where a higher scale narrows them into thin ridges winding across the whole island with flat ground at their foot), ridges (`ridge_scale` 0.25: rolling lowland with many narrow rivers and lakes) and flat ground (`flat_terrain_skew` 0.1), jungle pillars, river lanterns and lava tunnels off, and every cave off: no carvers, cheese, noodle or spaghetti caves and no underground rivers (W3).
- **The shape is measured, not guessed:** `just shape <seeds>` draws each seed's land from the game's own generator into `build/shape/<seed>.png` and logs its scale, land area, farthest land, drop-off and land biomes. Measured on seeds 1 to 4: 28.8 to 30.7 km² of land (a circle 6,052 to 6,257 blocks across), the farthest land 4,568 to 5,968 blocks out.
- **Edge cases:** a world made before a change to `[world]` keeps its chunks, so the change needs a new world; Distant Horizons draws the far land from the same noise.

### W2 Cold land

- Tectonic's climate is pushed cold and wet (`temperature_multiplier` 0.5, `temperature_offset` -0.3, `vegetation_multiplier` 0.5, `vegetation_offset` 0.25): taiga, old-growth taiga, snowy taiga, grove and the peaks. Biome Replacer maps what warm or temperate biome is left onto taiga. WWOO dresses the vanilla biomes: aspens, spruces with branches, orange terracotta in water and cliffs. Dead spruces, bare grey poles, stand scattered in the taiga (`systems.md` S6).
- Measured on seeds 1 and 6: taiga 32 to 43%, snowy taiga 12 to 13%, old-growth spruce taiga 9 to 12%, grove 3 to 10%, old-growth pine taiga up to 9%, meadow 4 to 6%, birch forest up to 5%, and the peaks up to 15%.

### W3 Wilderness only

- No structures: villages, ruins, outposts, portals and every mod's structures are cut. No monsters spawn (the spawn director, `systems.md` S8), and no dungeon with its spawner and chest generates. The Nether stays unreachable: an obsidian frame lit with flint and steel burns and opens no portal. The End has no portal frame to reach, since its strongholds are cut.
- **No caves.** A survivalist in Alaska does not go underground: the rock under the island is solid, with no cave openings, ravines, lush or dripstone caves or underground rivers.
- **No ore anywhere.** Coal, iron, copper, gold, redstone, lapis, diamond, emerald, amethyst geodes and infested stone never generate, in the rock or on the surface. **Iron is dug, not mined:** iron-flecked gravel lies in the birch forests' gravel pits (Project: Vibrant Journeys) and in patches on the beds of streams and lakes, and a shovel in it turns up Raw Iron one dig in 4, which the furnace smelts; the gold-flecked gravel never generates (`systems.md` S12). Solid rock breaks at 0.05× speed with every tool, wherever it sits (stone about 12 s with a stone pickaxe), so loose rocks are the way to cobblestone.
- **Nothing that Alaska lacks:** no wild pumpkins, no lava lakes or lava falls on the surface (lava lies only deep in the rock), no hazelnut bushes by the rivers (Wilder Nature's), no honeybees: the nests in the birches stand empty.
- **Edge cases:** land a world generated before keeps its caves, ores and dungeons; new land comes without.

### W4 The drop-off

- Every survivor arrives on the main island's ocean coast, facing the sea, at a low stretch of shore drawn from the world's seed when the world is made: every world of that seed gets the same one. It is the world's spawn (`systems.md` S10), and `just shape` marks it in red.
- **How it is found:** the continents are mapped on a 32-block grid out to `edge_blocks`. The open ocean is the sea joined to the water past the island; the main island is the land joined to the centre, rivers counting as land there so they never split it. The main island's cells beside the open ocean are drawn in an order shuffled by the seed, and the first whose ground is dry and whose last dry block before the water stands no more than 8 blocks above the sea is the drop-off, facing the water. The numbers live in `balance.toml` `[drop_off]`.
- **Edge cases:** a coast with no such shore takes the first cell drawn as it stands; islets, islands across open water and bays narrower than the grid never hold the drop-off; a world made before keeps its drop-off and its Supply Crate.

### W5 Tracks and paths

- Anything that walks presses footprints into snow layers and sand, which fade (Soft Imprints, drawn on each player's own screen).
- Grass, dirt, sand, short plants and snow wear into a path where survivors and animals walk often, and grow back when left (The Roads More Travelled): a year at one camp wears a trail to the water and the woodpile. Leaves and berry bushes never wear, and a path makes no one faster, since a survivor's speed is the body's (energy, snow, load).
- **Edge cases:** footprints show in snow layers, not in the snow Ecliptic Seasons draws over the ground's own blocks; a path left alone grows back whether or not anyone is near.

### W6 The coast and the streams

- **Driftwood** washes up on sandy beaches beside the sea: a log lying on the sand, grey with salt, already seasoned through (`systems.md` S6). About 2 pieces a day wash up near each survivor on the coast, 8 at most within 64 blocks, and four times as many on a gale's day and the day after (wind of 48 km/h or more).
- **Clams** live in beach sand: a shovel in it turns up Clams one dig in 4. **Mussels** grow in beds on stony shores at the waterline, where stone or gravel lies a block or two under the sea; a right-click picks 2 to 4 off a bed. New beds settle about twice a day, 8 at most within 64 blocks. Shellfish poison 1 in 32, 1 in 8 from June to August when the blooms come, cooked or not: shellfish poisoning, 4 hours of slowness and hearts draining with no cure (`systems.md` S1). The JEI pages say where each comes from.
- **Goose nests** appear in May on the grass and dirt beside lakes and rivers, away from the sea, about one a day near a survivor, each with 2 to 4 eggs; a right-click takes an egg, and the last takes the nest. A Goose Egg (266 kcal, 19 g fat) is eaten raw, with a small chance of food poisoning, or cooked on a campfire.
- **Streams** turn the Fish Wheel (`systems.md` S9) where they run two blocks deep; still water never does.
- **Edge cases:** driftwood, beds and nests come only near a living survivor, never within 24 blocks of one, so they seem to have been there; land nobody visits gains none; broken driftwood and picked beds do not come back in place, new ones settling elsewhere.

## Open

None.
