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
| Distant Horizons SeedGen | The far land from the seed; its chat lines go to the log (`SeedGenGreetingMixin`, `SeedGenSharingMixin`) and it loads within Distant Horizons' share of a frame (`SeedGenBudgetMixin`) |
| Lithium, FerriteCore, ModernFix, ImmediatelyFast, ScalableLux, C2ME, Noisiumed, Entity Culling, More Culling | Speed (`production.md`) |
| Dynamic FPS | 10 frames a second in the background; off in every dev instance, whose driver waits on drawn frames |
| Packet Fixer, spark | Longer time-outs between friends and the server; `/spark` profiles it |
| Panda's Falling Trees (his), with PandaLib 0.6.0 | Trees fall whole |
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
| JEI | Recipes; it lists only what the island gives (`cuts.toml`) |
| Controlling, with Searchables | A search box in Controls |
| Mouse Tweaks (his), Inventory Sorter (his) | Dragging and scrolling items; a middle click sorts, never into a dark row |
| InvMove | Walking with a screen open, except the bow-drill game, the summary and JEI's search (`InvMoveLink`) |
| Pick Up Notifier | What comes in, at the top right |
| Immersive Hotbar | The hotbar's animations, with its XP bar effects off |
| Jade | The name of what a sneaking survivor looks at and the mod it comes from, in vanilla's tooltip look (`JadeLink`); every item's tooltip ends with its mod |
| Chat Animation | Chat lines slide in |
| Default Options, with Balm | The first launch's settings and every key of the pack (`pack/config/defaultoptions/`) |
| More Darkness | Dark nights without the shader, stepping aside while it draws (`production.md`) |
| Ping Wheel | Team marks on mouse button 5 |
| Simple Voice Chat | Voice within 48 blocks, open from the first join; V opens its menu |
| What Are They Up To | A teammate typing, in a screen or idle |
| Shoulder Surfing Reloaded | The over-the-shoulder view on F5; U swaps the shoulder |
| Sit | Seats on slabs and stairs |
| 3D Skin Layers, Not Enough Animations, Detailed Animations, Fresh Animations | Survivors and rabbits that move as they look; the two packs through Entity Model Features and Entity Texture Features |
| Leggings Fix | Leggings closed at the ankle |
| Alex's Mobs (Unofficial Port) and Citadel (Unofficial Port) | Moose, grizzly, bald eagle, seal, orca; of their items only moose ribs stay |
| Critters n' Crawlers (his) | Wolverine, beaver, caribou, mouse, black bear, timber wolf, goose; of its items only the meats, stews and pemmican stay |
| [Let's Do] Wilder Nature (his) | Squirrel; none of its items or hazelnut bushes |
| Fowl Play | Raven, chickadee, Canada goose, gulls, mallard, hawk |
| River Fishing | Fishing with Alaska's species and the tackle the island makes (`systems.md` S9) |
| Sleeping Bags (his) | Sleeping out |

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
| Chunky | Distant Horizons 3.3.3 binds to it while the server prepares its spawn, before Chunky is loaded, and the server crashes |
| Immersive UI | Its trails mark rarity, which the island has none of, and the inventory with its dark rows is the pack's own |
| JourneyMap | Maps are made on the island from paper and the compass, and coordinates are hidden (`systems.md` S12) |
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
| Sophisticated Backpacks, Curios API, Spyglass Improvements, Nomadic Tents, GraveStone, HT's TreeChop, Farmer's Delight | The mod builds them: the hotbar and the Hide Pack, the back and neck slots, the binoculars' zoom (Spyglass Improvements zooms only the vanilla spyglass, which the island cannot make), the camp, the fallen survivor's pack, felling, the pots and every food's numbers |

## Open

None.
