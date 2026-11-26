# Backcountry

A modpack about a year alone in the Alaskan wilderness, counted in calories, for Minecraft 1.21.1 on NeoForge, solo or as a team.

## The rules of the pack

- **The design decides.** `design/` holds what the pack is, and `design/README.md` says which milestone is under way. Build only what is Settled there; a gap found while building goes back to Open in its document, never decided in code. Design work runs through the `design` skill.
- **The body is the mod's.** Every food, every hour of work and every degree of cold counts in the mod's one kcal model (`design/systems.md` S1), so no public survival mod goes in beside it: no temperature, thirst, nutrition, spoilage, stamina, weight or inventory mod. Ecliptic Seasons' heat stroke stays off for the same reason.
- **The weather is the mod's.** One sky drawn from the world seed and the day (`design/systems.md` S3) is what the forecast reads, so Ecliptic's own rain stays off (`UseSolarWeather`), mixins answer its rain-or-snow choice and its weather effects from the mod's air, `/weather` is refused, and no weather mod goes in beside it.
- **The fire is the mod's.** The pack's campfire (`backcountry:campfire`, `design/systems.md` S6) burns only the wood queued in it, so vanilla's campfire, which burns forever, is cut; torches and lanterns burn down and are refilled (`fire/Lights`), and the furnace cooks no food.
- **Vanilla's hunger is never read.** The body holds the food level at 17 (no vanilla healing, no starving) and drops it to 6 while the survivor is too tired, blistered or frostbitten, so vanilla's own check stops sprinting; the difficulty is locked at Normal, since Peaceful heals and feeds.
- **Every food has its numbers.** Every food of every jar has an entry in `mod/src/main/resources/data/backcountry/data_maps/item/food.json`, and an item without one cannot be eaten; `just test` fails on a food without an entry, so a mod added to the pack gets its foods' entries with it.
- **Every item has its bulk.** How many of an item fit in one slot is `balance.toml` `[bulk]`, applied as the item's stack size when the game starts; `just test` fails on an item no entry covers, so a mod added to the pack gets its items' sizes with it.
- **The tasks are the mod's.** The Messenger's tasks (`design/systems.md` S11) tick from the mod's own state, what a survivor holds, placed and did, so no quest mod (FTB Quests and the like) goes in beside them.
- **The spawn director owns every animal.** Natural spawns of anything not on its list are cancelled, so a creature mod adds nothing until the list names its animals; no animal breeds or is tamed.
- **A survivalist's year.** The pack is what a survivalist alone in Alaska meets and does: a mechanic, item or place that one would not is cut, in `cuts.toml` or at its source (`design/systems.md` S12). No structure generates (`StructuresMixin`), no cave is carved (`pack/config/tectonic.json`), no ore lies in the rock but the surface iron and the birch forests' gold-flecked gravel, and no Nether portal lights: the island's surface is the whole game.
- **What goes is in `cuts.toml`.** A cut item is hidden from JEI, every recipe that makes or needs it is dropped and every loot drops it no more; a cut creature never joins the world. A mod added to the pack has its items weighed there, a whole mod of exotic things under `[mods]` with the few it keeps; `just test` fails on a cut that names nothing, on a recipe that makes or needs a cut item, and on an item whose every recipe needed one, which is then cut or listed as `found` when the island gives it as it lies.
- **Every key is the pack's.** Every key of every mod is entered in `pack/config/defaultoptions/keybindings.txt`, unbound unless a player needs it (River Fishing's fight keys, Ping Wheel's mark, JEI's keys inside its own screens, the voice menu, the shoulder swap, the sort, the binoculars); a key's name is copied from a dev client's `options.txt`, since a name guessed from the mod's code binds nothing.
- **The world's shape is measured, not guessed.** A change to `balance.toml` `[world]` or `pack/config/tectonic.json` is measured with `just shape 1 2 3 4 5 6` before it is kept, and its pictures judged.
- **Everyone in a world is one team.** Caches, rations and the director's rations are the team's; the body is each survivor's.
- **Every number lives once,** in `balance.toml`, which the mod reads from its jar. A number that tunes something is a power of two or a sum of two such; a number that means something stays as it is: a value quoted in `design/` or taken from Forge Labs' configs, a fact about vanilla or a mod, or time people read.
- **Every mechanic has its edge cases.** What a player may do while it runs is part of its design: walk off, log out, die, be extracted, or a teammate doing the opposite. A design entry, a plan and a build name each of them and what happens.
- **Singleplayer and server behave the same.** Rules live in the mod jar or in `pack/config`, never in a world save or a client-only setting.
- **Gear has plain names** (the Satellite Messenger, the Water Bottle), never brand names, so the pack can be published without renaming.
- **Everything a player sees wears vanilla's look**, made with the `art` skill; the Satellite Messenger alone is drawn as the device it is.

## Checking the work

The gates are `just lint`, then `just test` (the game tests beside the headless boot). `just shape <seeds>` draws worlds from the game's own generator into `build/shape/`; the dev server's watchdog is off (`max-tick-time` -1) so such long commands finish.

After every push, watch the GitHub Actions runs it starts to their end (`gh run list`, then `gh run watch <id> --exit-status`) and report how they ended, the failing step's log with a failure: a push to `main` with a dated version in `CHANGELOG.md` publishes that release and the pack's page, and a failed run leaves players on the previous one.

## Where things go

- Textures, item and block models, blockstates and the game tests' room are written by `just generate` from `generator/textures.toml` into `mod/src/generated/`, and `pack/config/iris.properties` and the shader's options in `pack/shaderpacks/` from `generator/shader.toml` and `balance.toml` `[sky]`; change the source, never the output.
- Ecliptic Seasons is on the dev classpath, fetched from Modrinth at the version `pack/mods/ecliptic-seasons.pw.toml` names, so `scripts/instance.py` leaves its jar out of the dev client's, dev server's and game tests' mods; the server pack and the shipped pack carry it as usual.
- A mod's config in `pack/config` holds only the keys the pack changes; the mod fills in the rest itself.

## Seeing the game

Never wait by guessing: every driver step returns the moment its effect is done (the server's reply to a command, a freshly drawn frame, the world opened), and a step that does not finish within 60 s fails the batch and says where it stuck (`far` and each face of `panorama` count the 60 s from their last progress). `wait <ticks>` is only for watching something that has no end, such as a fade. Code that knows something failed says so in one line, `<what> failed: <why>`, and the drivers stop on it.

```console
$ python3 scripts/client.py start --fresh        # a fresh world in survival; --seed <n>, --world <save>, nothing for the title; --visible to use the user's screen
$ python3 scripts/client.py run seen '/backcountry body weight 55' 'shot extracted'
$ python3 scripts/client.py swap                 # compile and swap changed code into the running client
$ python3 scripts/client.py checkpoint <name>       # save the open world and keep a copy; restore <name> opens it again
$ python3 scripts/client.py stop
$ python3 scripts/client.py panorama             # photographs the title's panoramas at the drop-off of seed 1, shader at Ultra, into the title's textures
$ python3 scripts/server.py start --fresh        # the dev server, which prints its port; run '<command>' answers through RCON
```

Each π session drives games of its own in `build/clients/<n>/` and `build/devservers/<n>/`, with its own ports; a shell outside π, `just play` and `just devserver` use `build/client` and `build/devserver`. The client runs on a hidden screen (gamescope from the system) unless `--visible`.

| Step | Does |
|---|---|
| `/<command>` | Runs a command as the player and returns once the server has run it, with its replies under `chat` |
| `seen` | The body, the weather, the forecast and the alerts as the HUD and the Messenger show them, whether rain or snow falls where the player stands, the date, the action bar, the hotbar and the screen |
| `screen` | The open screen's class, title and widgets with their centres |
| `shot <name>` | A screenshot of the next drawn frame into the game folder's `screenshots/` |
| `press <text>` | Clicks the active button whose label holds the text |
| `click <x> <y> [right]` | Clicks on the open screen at GUI coordinates |
| `key <name>` | `inventory`, `perspective` (F5) or `escape` (the pause menu) in the world; `left`, `right`, `enter`, `escape`, `tab` or a letter on a screen |
| `hold <key> <ticks>` | Holds `forward`, `back`, `left`, `right`, `jump`, `sneak`, `sprint`, `use`, `attack` or `binoculars`; `on` instead of the ticks keeps it down through the steps after it until `off` |
| `use`, `slot <n>`, `face <yaw> <pitch>`, `close` | A right-click, a hotbar slot, a facing, back to the world |
| `join <address>` | Leaves the world and joins a server |
| `pos`, `log`, `gui on\|off`, `wait <ticks>`, `quit` | Where the player stands, warnings and errors since the last `log`, the HUD, a watch, quitting |
| `scale <n>` | Sets the interface scale (2, 3 or 4) for the steps after it, the window staying as it is |
| `walk <x> <z>` | Walks to the column, jumping when blocked, and returns on arrival |
| `bench <seconds>` | Turns the camera once around and returns the average and lowest frames a second |
| `graphics <step> shaders on\|off` | Picks a step of the Backcountry page's Preset (`low`, `medium`, `high`, `ultra`) and the shader, as a player does in Video Settings |
| `far` | Waits until Distant Horizons has generated and drawn the far land around the player (`--mode shipped`) |
| `panorama <name>` | Photographs the six faces of a panorama where the player stands into `screenshots/panorama/<name>/` |

Dev runs have `/backcountry body <field> <value>` (core, energy, stomach, water, weight, wetness), `/backcountry condition <condition>`, `/backcountry day <day>` (moves Ecliptic's calendar), `/backcountry weather clear|rain|snow|thunderstorm|blizzard` (today's weather from now to 06:00, warm or cold enough where the command runs), `/backcountry pretend join|leave <name>`, `/backcountry registries <folder>` (`just registries`) and `/backcountry playthrough <name> <first_day> <days> <kcal_a_day> <work_kcal_a_day>`, which lives a survivor through game days where it runs (the `playthrough` skill).

## Mods

- packwiz marks a mod for both sides unless Modrinth says otherwise; set `side = "client"` in its `.pw.toml` when the server never needs it, so the server pack stays lean.
- PandaLib is pinned at 0.6.0: Panda's Falling Trees 0.14.0 crashes at load with PandaLib 0.6.2.
- A mod's starting item, chat greeting or pop-up is switched off, in its config where it has one, else by a mixin (`DailyOrderMixin`, `ToastComponentMixin`): the arrival kit is the design's alone (`design/systems.md` S10) and the screen tells only what the pack tells. Advancements are not announced in chat (`announceAdvancements` off in every new world).
- A mod that reads vanilla's state where the pack has its own is hooked to the pack's or switched off: Subtle Effects' breath follows the mod's air and its growl the body's stomach, and its fireflies and Particular's stay off, since Alaska has none.
- LambDynamicLights lights what is held and worn with the shader on too, and the shader's own held light is off (`generator/shader.toml`), since the Headlamp is worn and the shader lights only the hand.
- Dynamic FPS is off in every dev instance (`scripts/instance.py`), since the drivers wait on drawn frames.
