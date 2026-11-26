# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.1] - 2026-11-26

### Highlights

- **One address to import** - Import `https://roman-16.github.io/backcountry/Backcountry.zip` in Prism once more; from then on the instance follows every release by itself.

### Changed

- Prism imports the pack from `https://roman-16.github.io/backcountry/Backcountry.zip`, and the instance follows every release, majors included. An instance imported from `/1/` or `/2/` no longer updates: import the new address once.

## [2.0.0] - 2026-11-26

### Highlights

- **Alaska's island** - No caves, ore in the rock, Nether, XP, magic or machines; instead bears that act their season and mostly bluff, sows with cubs, the salmon run, new berries, spruce tea, birch syrup and a sled.
- **Tasks** - The Satellite Messenger opens on Tasks, walking each survivor through their first days, then setting the team's goals for winter and the year.
- **Real work, real calories** - Every activity burns what it would burn a real person, skills make work cheaper with practice, and the Nutrition page shows where the day's kcal went.
- **Company** - Voice chat between teammates within 48 blocks (V opens its menu), and teammates seen typing, sitting and moving with animations.

### Added

- Torches and lanterns burn down: a torch lasts 30 minutes and a lantern an hour, in hand too, with a bar on the slot; charcoal tops up a torch, Rendered Fat a lantern, and a spent lantern hangs dark until refilled.
- A search box in Controls, team marks on mouse button 5, and dark nights without the shader that step aside when it draws.
- The compass is made from iron and a glass pane and points north; the clock is gold and a glass pane; the lead is made from rope, and black dye from charcoal.
- Voice chat between teammates within 48 blocks, open from the first join; V opens its menu.
- Teammates show when they are typing, in a screen or idle, and survivors move, eat and drink with animations, their skins in 3D.
- Sit on slabs and stairs; F5 steps through an over-the-shoulder view, and U swaps the shoulder.
- What you pick up is listed at the top right, the hotbar bounces as items come and go, and sneaking shows the name of what you look at.
- Drag and scroll items between slots, sort a chest, a cache or your pack with a middle click, and keep walking with a screen open.
- Footprints in snow and sand, paths that wear where you walk and grow back, wakes behind the raft, and thunder that arrives after its flash.
- New sounds for screens, items and blocks, music that follows place, time and weather, sparks from fires and lamps, breath in cold air and a growling empty stomach.
- The far land fills in faster, and the game loads, draws and generates faster.
- The Satellite Messenger opens on a Tasks page: each survivor's first days, then the team's goals for winter and the year.
- Fire-making, Woodcutting, Butchering and Archery rise with use, each level making that work quicker and cheaper; the Vitals page shows them, and a new survivor starts over.
- Salmonberries in June and July, highbush cranberries from September into March, and white baneberries that poison.
- Spruce Tea against scurvy, from needles cut off spruce leaves with the knife and boiled in the Camp Pot.
- A Birch Tap gathers sap in April and May that drinks like water and boils down to Birch Syrup.
- Dead spruces in the taiga split into Softwood dry enough to burn from the first day.
- Fish frozen through at -16 °C or colder for a day carries no parasites raw.
- Sows with one or two cubs that charge anyone near them, and bears fishing the rivers from July that leave half-eaten salmon on the bank and defend it.
- The Sled, pulled on a lead, carries 27 stacks or a whole moose, gliding on snow and dragging on bare ground.

### Changed

- The island is a survivalist's: no caves, no ore in the rock but surface iron and gold-flecked gravel in the birch forests, no wild pumpkins, surface lava, honeybees or hazelnuts.
- River Fishing catches only Alaska's fish, with the stick rod in open water and the winter rod through the ice.
- Logs need an axe; the first is the stone axe from loose rocks and twigs.
- The water bar sits where the XP bar was.
- The pack's items and blocks are drawn in vanilla's style: outlines in their own darker colour, light from the top left, and bows, arrows and bottles that keep vanilla's string, feathers and glass.
- The shader is on from the first launch, with the pack's keys and settings: Shift sprints, Ctrl sneaks, FOV 80, GUI scale 3, Bright, full screen.
- The difficulty stays Normal, and F3 shows no coordinates.
- Every activity burns what the same time of it burns a real person, with load, snow, slope, wind and cold counted; snowshoes and a lighter pack save kcal, and the Nutrition page splits the day's burn by cause.
- Rations are larger to match, Lean 1,280, Normal 1,600 and Feast 2,048 kcal a meal, and the stomach holds twice as much.
- Grizzlies come at survivors in spring and autumn and otherwise only when surprised, most charges stop short with a roar unless you run or strike, and black bears flee except at food.
- Mushroom stew takes two brown mushrooms; with a red one it comes out Poisoned.
- Distant Horizons draws 1,024 chunks out, with no warnings in chat.

### Removed

- The Nether can no longer be reached, and dungeons, infested stone and every way to XP, enchanting, potions, redstone machines, swords, shields, metal armour, boats, breeding, taming and bone meal's growth are gone.
- JEI lists only what the island gives: no Nether, End, structure or exotic mod items, no spawn eggs, no Ecliptic Seasons gadgets, no River Fishing electronics, ponds or contracts.
- Advancements are hidden: L is unbound and the pause menu has no Advancements button.

## [1.0.0] - 2026-11-24

### Highlights

- **A year counted in calories** - Every survivor's body burns what a real day burns, shown by the energy bolts, the water bar and the conditions beside the hotbar, until extraction at BMI 17 ends the run with its summary.
- **Alaska's own weather** - Days, rain, snow and blizzards follow Anchorage's year, and the Satellite Messenger forecasts them three days ahead and alerts what is coming.
- **Living off the land** - Fires lit with a bow drill, food that spoils unless it is cached or smoked, bows, carcasses and hides, and traps and nets that work while you are away.

### Added

- The body counts every kcal: energy bolts take the hunger bar's place, a water bar sits above the XP bar, and conditions show their names right of the hotbar.
- Every food shows its kcal and fat, and eating stops when the stomach is full.
- The Water Bottle and the Camp Pot fill at any water, and the Camp Pot boils it on a lit campfire.
- Air temperature follows the season, the weather and the hour; shelter, wind, fire, wetness and the Hide Hat, Poncho, Coat and Pants set how cold the survivor gets.
- The weather is the pack's own: days warmer or colder than the season, rain where the air is above freezing and snow where it is not, thunderstorms in summer whose lightning starts no fires, and blizzards that close the view in, as often as Anchorage has them; `/weather` is refused.
- Infected wounds, blisters, frostbite, burns, scurvy, food poisoning, gut infection, parasites and exhaustion, cured with bandages from plant fibre and the Med Kit.
- The inventory is the hotbar alone; a Hide Pack worn on the back opens a row.
- The Satellite Messenger shows the Weather, a three-day Forecast, Warmth, Body, Nutrition and Vitals, and beeps and lights up with alerts for snow, thunderstorms and frost ahead, lost weight and a wasting body.
- At BMI 17 the survivor is extracted; extraction or death shows the run's summary with its weight curve, and a new survivor arrives at the drop-off with the kit, where the Supply Crate replaces a lost Satellite Messenger.
- The year runs 48 days from 3 May with Anchorage's daylight, and each 3 May shows the team's year.
- The Bear Spray drives off what attacks.
- Fire is made and kept: birch bark, tinder, cordage and a bow drill light a campfire that burns a queue of firewood on Low, Steady or Hot, banks coals for a relight, burns faster in the rain and can set a shelter alight when packed too full; a shovel or water puts it out.
- Logs split into softwood and hardwood with an axe; green wood is too green to catch until it has dried on a woodpile.
- The Headlamp arrives in the kit: wound by hand, worn on the head, dimming in deep cold.
- Food spoils by the temperature where it lies, telling how many days it has left here; spoiled food poisons.
- The Food Cache keeps the team's food, shows its kcal, days and scent, and feeds every survivor a Lean, Normal or Feast ration at dawn and dusk; mice raid it at night.
- The Smoker smokes meat and fish over dry firewood for 12 hours to keep them for months, and the Cooking Pot on a campfire cooks stews that also quench thirst.
- Mushrooms are food, blueberries and lingonberries ripen in their season, and crops grow only in the short summer, killed by frost.
- The Satellite Messenger has a Rations page with the next meal and the team's food stores in days.
- The spawn director places every animal out of sight in its habitat and season, fewer in the rain; no monster spawns.
- Hunting with the Knife, the Light and Heavy Hunting Bow and blunt, bone and broadhead arrows: only a hit to the chest kills big game outright, others wound it into a blood trail; the Moose Call works in the rut, and the Messenger alerts when the rut begins and the bears feed.
- Every animal leaves a carcass to carry or dress with the knife into meat, organs, fat, pelts and the moose's head; pelts become scraped and cured hides and rawhide strips for the hide clothing, the Hide Pack and snowshoes for deep snow.
- Grouse, ptarmigan white in winter, loons calling at dusk, and summer mosquitoes by the water that cost sleep.
- Rope, snare wire, wire mesh, stakes and crossbeams build snares, deadfalls, mouse traps that keep mice off caches, gill nets and tip-ups that catch while the survivor is away; crickets and worms for bait.
- River Fishing's catches give kcal by their weight, follow the island's seasons and sky, count in the summary, and late salmon are spawned out.
- A raft of logs and rope that can pitch its rider off in the wind, and lakes that freeze in the cold and thaw in spring.
- Under the shader, a blizzard whites out the view as it closes in without it.
