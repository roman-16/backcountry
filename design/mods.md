# Mods

Every mod in the pack and why, and what was left out. "His" marks a mod in one of Forge Labs' public packs.

## In

| Mod | Role |
|---|---|
| Tectonic (his) | Terrain shape; the island is cut from its continents (`world.md` W1) |
| William Wythers' Overhauled Overworld (his) | The biomes' look: aspens, branched spruces, orange terracotta |
| Streams Reflowing | Streams that step down the land, spray at falls, lakes at every height |
| Biome Replacer (his) | Maps every warm or temperate biome onto a cold one (`pack/config/biome_replacer.properties`) |
| Lithostitched (his) | Tectonic's library |
| Project: Vibrant Journeys (his) | Shelf fungus, twigs, rocks, fallen logs, pinecones |
| Better Foliage Renewed (his) | Bushy leaves, reeds; its last 1.21.1 build is from 22.08.2024, so it leaves if it breaks with Sodium or Iris |
| Ecliptic Seasons | The year's look: day length, gradual snow on the ground, falling leaves, lake ice (`systems.md` S2); the sky and whether it rains or snows are the mod's weather (S3); nothing it adds can be held (`systems.md` S12) |
| Distant Horizons (his) | The far view |
| Sodium, Iris, Complementary Reimagined | Drawing and the shader; the mod's Backcountry page in Video Settings sets them with the far view through one Preset (`production.md`) |
| Euphoria Patches | Reimagined's added options, kept at their defaults; no pop-up, messages or update check |
| Distant Horizons SeedGen | The far land from the seed; its chat lines go to the log (`SeedGenGreetingMixin`, and `SeedGenSharingMixin` on the client alone, since its class names the client's) and it loads within Distant Horizons' share of a frame (`SeedGenBudgetMixin`); a worldgen namespace with no mod of its own (Tectonic's `lithosphere`, WWOO's `wythers`) reads as an empty version on both sides, so it is not taken for a mod the player lacks (`SeedGenVersionMixin`) |
| Lithium, FerriteCore, ModernFix, ImmediatelyFast, ScalableLux, C2ME, Noisiumed, Entity Culling, More Culling | Speed (`production.md`); Lithium's campfire sleeping is off, since a campfire it puts to sleep stops the pack's fire burning (`pack/config/lithium.properties`) |
| Dynamic FPS | 10 frames a second in the background; off in every dev instance, whose driver waits on drawn frames |
| Packet Fixer, spark | Longer time-outs between friends and the server; `/spark` profiles it |
| Panda's Falling Trees (his), with PandaLib 0.6.0 | Trees fall whole; its print of every felled tree's blocks is dropped (`FallingTreesPrintMixin`), and a dedicated server reads the players' unsynced Falling Trees options as their defaults without a warning each time (`PandaLibSyncMixin`) |
| Particular Reforged | Falling leaves, waterfall spray; no fireflies, which Alaska has none of |
| Sound Physics Remastered (his), AmbientSounds | Sound, voices muffled through walls |
| Sounds | Screen, item and block sounds |
| Reactive Music | Music by place, time and weather, its own tracks |
| Distant Thunders | Thunder after the flash by the strike's distance |
| Subtle Effects (no fireflies, waterfall spray or rarity sparkles) | Sparks, dust and smoke, the heartbeat at 3 hearts; breath where the air is 7 °C or colder (`SubtleEffectsBreathMixin`) and a growl from an empty stomach (`SubtleEffectsGrowlMixin`) in place of its biome and hunger checks |
| Soft Imprints | Footprints in snow layers and sand (`world.md` W5) |
| The Roads More Travelled | Paths that wear and grow back; leaves never wear and paths give no speed (`world.md` W5) |
| Wakes Reforged | Wakes behind the Raft and swimmers |
| LambDynamicLights | Light from what is held, and the headlamp, with the shader on and off; its quality follows the Preset, and the shader's own held light is off so a torch never lights twice |
| JEI | Recipes, and the pack's own pages for every way the island gives things: gathering, dressing, the pots, the smoker, the rack, salting, the birch tap, the woodpile, traps and the fish wheel (`JeiLink`); it lists only what the island gives (`cuts.toml`) |
| Controlling, with Searchables | A search box in Controls |
| Mouse Tweaks (his), Inventory Sorter (his) | Dragging and scrolling items; a middle click sorts, never into a dark row |
| InvMove | Walking with a screen open, except the bow-drill game, the summary and JEI's search (`InvMoveLink`) |
| Pick Up Notifier | What comes in, at the top right |
| Immersive Hotbar | The hotbar's animations, with its XP bar effects off |
| Jade | The name of what a sneaking survivor looks at and the mod it comes from, in vanilla's tooltip look, with the Camp Pot's, the campfire's and a carcass's state, and sign and water named for what they are (`JadeLink`); every item's tooltip ends with its mod |
| Cut Through | A swing passes through grass and other blocks without collision to hit what stands behind; client only |
| Chat Animation | Chat lines slide in |
| Default Options, with Balm | The first launch's settings and every key of the pack (`pack/config/defaultoptions/`) |
| More Darkness | Dark nights without the shader, stepping aside while it draws (`production.md`) |
| Ping Wheel | Team marks on mouse button 5 |
| Simple Voice Chat | Voice within 48 blocks, open from the first join; V opens its menu; on exaroton it shares the game's port (`port=-1`, `scripts/exaroton/`), the one port exaroton opens |
| What Are They Up To | A teammate typing, in a screen or idle |
| Shoulder Surfing Reloaded | The over-the-shoulder view on F5; U swaps the shoulder |
| Sit | Seats on slabs and stairs |
| 3D Skin Layers, Not Enough Animations, Detailed Animations, Fresh Animations | Survivors and rabbits that move as they look; the two packs through Entity Model Features and Entity Texture Features |
| Leggings Fix | Leggings closed at the ankle |
| Alex's Mobs (Unofficial Port) and Citadel (Unofficial Port) | Moose, grizzly, bald eagle, seal, orca, sperm whale; of their items only moose ribs stay; its moose keeps the pack's temper (`hunt/Moose`) |
| Critters n' Crawlers (his) | Wolverine, beaver, caribou, mouse, black bear, timber wolf, coyote, hoary marmot, goose; of its items only the meats, stews and pemmican stay; its timber wolf and coyote hunt no survivor and keep their distance (`hunt/Wolves`) |
| [Let's Do] Wilder Nature (his) | Squirrel; none of its items or hazelnut bushes |
| Fowl Play | Raven, chickadee, Canada goose, gulls, mallard, hawk, robin, sparrow, crow; its bird feeder, registered with no recipe, name or look, is cut |
| Animal Garden - Porcupine | The porcupine, whose quills punish a bare hand (`systems.md` S8); never tamed |
| Curios API | The worn slots, back, neck and hands, in its own panel on G, drawn on the survivor; their drop rule keeps them on the survivor (`ALWAYS_KEEP`), since the left pack takes them at death or extraction (`systems.md` S1, S10) |
| River Fishing | Fishing with Alaska's species and the tackle the island makes (`systems.md` S9); Game Guts are its meat bait, so the mod compiles against it |
| Sleeping Bags (his) | The Sleeping Bag, sewn from hides; its other colours are cut (`systems.md` S1, S10) |

Libraries they bring: Architectury, Cloth Config, CoroUtil, CreativeCore, Fzzy Config, GeckoLib, Kotlin for Forge, [Let's Do] Foundation, MezzConfig, MRU, Puzzles Lib, SmartBrainLib, YetAnotherConfigLib.

## Left out

| Mod | Why |
|---|---|
| Legendary Survival Overhaul, Tough As Nails, Dynamic Nutrition, FFFoodSpoilageAndPreservation, Rot Timer, Stamina For Tweakers, FFItemWeight, TinyInv | The mod's body owns all of it (`systems.md` S1) |
| No Tree Punching (Remastered), Butchery, Simply Traps, Carry On, Starter Kit | The mod builds them itself: logs need an axe (S6), carcasses and cuts (S8), traps (S9), the kit (S10) |
| In Control!, Bad Mobs | The spawn director owns spawning (S8) |
| KubeJS | Recipes, loot and rules live in the mod's data |
| Snow! Real Magic!, Snow Under Trees | Ecliptic Seasons draws the snow |
| Weather, Storms & Tornadoes | The mod's weather (S3); tornadoes are not Alaska |
| Regions Unexplored | WWOO dresses the vanilla biomes instead |
| Variety Birds | Version 0.1.0, two weeks old when weighed (11.2026) |
| Just Enough Resources, Advanced Loot Info | They list loot tables, full of creatures that never come; the pack's own JEI pages say how the island gives things |
| Field Guide | The Messenger's Notes page keeps each player's field notes (`systems.md` S13) |
| Fish 'N' Ships | The Fish Wheel and the canoe are the pack's own (S9) |
| Advanced Chimneys | The Stone Chimney vents the pack's rooms (S6) |
| Rack It Up | The Drying Rack is the pack's own (S7) |
| Salt: Renewed | Sea salt boils out of the Camp Pot (S7) |
| Animal Garden - Sea Otter | Weighed beside the porcupine and left for later |
| Insanity, sanity and mental-health mods | Spirits are the mod's (S1), and nothing ends at the bottom |
| Chunky | Distant Horizons 3.3.3 binds to it while the server prepares its spawn, before Chunky is loaded, and the server crashes |
| Immersive UI | Its trails mark rarity, which the island has none of, and the inventory with its dark rows is the pack's own |
| JourneyMap, Antique Atlas 4, Mapwright | The Birch-Bark Map draws only what a survivor surveys (`systems.md` S13), and coordinates are hidden (S12) |
| Locator Bar | The Messenger shares the team's positions on a bar of the pack's own in vanilla's look (`systems.md` S4); the mod's vanilla-style bar takes the XP bar's row, where the hearts and energy sit, and its other bar has a look of its own |
| Auroras | Complementary Reimagined draws its own aurora |
| Particle Rain, Windy | They draw rain, snow and wind of their own, where the mod's weather decides them (`systems.md` S3) |
| Simple Grass Flowers | Flowers painted on the grass all year, through autumn |
| Motschen's Better Leaves | Better Foliage Renewed already draws bushy leaves |
| Bushy pink petals | The cherry grove's plants are cut |
| Campsite Structures, ChoiceTheorem's Overhauled Village, Dungeons and Taverns with its overhauls, Explorify, Luki's Grand Capitals, Moog's Voyager Structures, Structory, Towns and Towers, Trek, Snowy Tents, YUNG's structure mods, Sparse Structures, Structure Layout Optimizer, Async Locator Refined, Lootr | No structure generates (`world.md` W3) |
| End Remastered, Ender Dragon Fight Remastered, Nullscape, Spelunkery | The island's surface is the whole game |
| Spice of Life Onion, AppleSkin, Healing Campfire | The body is the mod's, and vanilla's hunger is never read |
| Serene Seasons, Terralith, Geophilic, Terraphilic | Ecliptic Seasons keeps the year; WWOO dresses the land |
| Naturalist, Friends & Foes, Respawning Animals, Improved Mobs, The Hordes, Spawn Animations, No Hostiles Around Campfire, Attract to Sound | The spawn director owns every animal and no monster spawns; a hearing AI would fight the bears' seasonal temper (`systems.md` S8) |
| Artifacts, Better Combat, Combat Roll, Simply Swords, Silent Gear, Hang Glider, Yori3o's Grappling Hooks, Small Ships, Waystones, Horseman, Enhanced Celestials, RarityCore, Enchantment Descriptions, Enchant Icons, Supplementaries, Amendments, Easy NPC, Simply Tooltips, Traveler's Titles | No exotic gear, magic, XP or labels on the land (`systems.md` S12); Amendments' wall lanterns would never burn down |
| Sophisticated Backpacks, Spyglass Improvements, Nomadic Tents, GraveStone, HT's TreeChop, Farmer's Delight | The mod builds them: the hotbar and the Hide Pack, the binoculars' zoom (Spyglass Improvements zooms only the vanilla spyglass, which the island cannot make), the camp, the fallen survivor's pack, felling, the pots and every food's numbers |

## Open

None.
