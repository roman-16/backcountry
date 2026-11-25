# World

## Settled

### W1 One island, then deep ocean

- **The land** covers the area of a 6,144-block circle on every seed: a main island with a coast broken by bays, sounds and islets, and on some seeds big islands across open water. Past it, deep ocean to Minecraft's own world edge: no land lies more than 6,144 blocks from the centre.
- **How it is made:** the mod wraps Tectonic's raw continents (`tectonic:noise/raw_continents`, `IslandShape`) where the game wires the noise. Over the middle it lifts the value by `lift`; from `inner_share` to `outer_share` of the radius it falls to `-sink`, the distance bent by two noises (`island_warp`, `island_detail`) so the coast is never round; past `outer_share`, and past `edge_blocks` from the centre, it is held at the sea floor, so nothing grows there. The first time a world is used, its land is measured on a 96-block grid and the shape scaled so the land covers the target area; since rivers, lakes and flats at sea level take part of what the continents count as land, the measure aims at the target divided by `dry_share`. The numbers live in `balance.toml` `[world]`.
- **Tectonic** (`pack/config/tectonic.json`): `continents_scale` 0.5 (small continent features, so the coast breaks into bays, peninsulas and islets), islands off, the deep floor at `deep_ocean_depth` -0.75 (about 96 blocks down), his Amazon values for erosion (0.75), ridges (0.08, wider valleys) and flat ground (0.38), jungle pillars, river lanterns and lava tunnels off.
- **The shape is measured, not guessed:** `just shape <seeds>` draws each seed's land from the game's own generator into `build/shape/<seed>.png` and logs its scale, land area, farthest land, drop-off and land biomes. Measured on seeds 1 to 6: 31.2 to 32.8 km² of land (a circle 6,300 to 6,460 blocks across), the farthest land 5,384 to 6,076 blocks out.
- **Edge cases:** a world made before a change to `[world]` keeps its chunks, so the change needs a new world; Distant Horizons draws the far land from the same noise.

### W2 Cold land

- Tectonic's climate is pushed cold and wet (`temperature_multiplier` 0.5, `temperature_offset` -0.3, `vegetation_multiplier` 0.5, `vegetation_offset` 0.25): taiga, old-growth taiga, snowy taiga, grove and the peaks. Biome Replacer maps what warm or temperate biome is left onto taiga. WWOO dresses the vanilla biomes: aspens, spruces with branches, orange terracotta in water and cliffs. Dead spruces, bare grey poles, stand scattered in the taiga (`systems.md` S6).
- Measured on seeds 1 and 6: taiga 34 to 39%, snowy taiga 11 to 13%, old-growth pine taiga 7 to 14%, grove 7 to 8%, old-growth spruce taiga 6%, old-growth birch forest 3 to 6%, meadow up to 7%, and the peaks.

### W3 Wilderness only

- No structures: villages, ruins, outposts, portals and every mod's structures are cut. No monsters spawn (the spawn director, `systems.md` S8). The Nether stays unreachable.
- Surface iron lies in stream beds and cliff faces; digging into solid rock with no open space within 3 blocks behind runs at 0.05× speed, so iron is found by exploring, not by digging down.

### W4 The drop-off

- When the world is made, the mod searches outward from the centre on a 32-block grid for a lake: 16 or more cells of water whose surface is at y 128 or below, 512 or more blocks inland, and not joined to the sea. Its shore cell nearest the centre, no more than 8 blocks above the water, is the drop-off and the world's spawn; every survivor arrives there (`systems.md` S10). With no such lake within 3,072 blocks, the first bank of any water found is the drop-off. `just shape` marks it in red.

## Open

None.
