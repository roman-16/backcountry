# Backcountry

A modpack about a year alone in the Alaskan wilderness, counted in calories, for Minecraft 1.21.1 on NeoForge, solo or as a team. Its layout and tooling follow Wayfarer (`~/Documents/wayfarer`), the first place to look for how a problem was solved before.

## The rules of the pack

- **The design decides.** `design/` holds what the pack is, and `design/README.md` says which milestone is under way. Build only what is Settled there; a gap found while building goes back to Open in its document, never decided in code.
- **The body is the mod's.** Every food, every hour of work and every degree of cold counts in the mod's one kcal model (`design/systems.md` S1), so no public survival mod goes in beside it: no temperature, thirst, nutrition, spoilage, stamina, weight or inventory mod.
- **The spawn director owns every animal.** Natural spawns of anything not on its list are cancelled, so a creature mod adds nothing until the list names its animals.
- **Wilderness only.** No structure generates (`StructuresMixin`) and the Nether stays unreachable: the island is the whole game.
- **The world's shape is measured, not guessed.** A change to `balance.toml` `[world]` or `pack/config/tectonic.json` is measured with `just shape 1 2 3 4 5 6` before it is kept, and its pictures judged.
- **Everyone in a world is one team.** Caches, rations and the director's rations are the team's; the body is each survivor's.
- **Every number lives once,** in `balance.toml`, which the mod reads from its jar. A number that tunes something is a power of two or a sum of two such; a number that means something stays as it is: a value quoted in `design/` or taken from Forge Labs' configs, a fact about vanilla or a mod, or time people read.
- **Every mechanic has its edge cases.** What a player may do while it runs is part of its design: walk off, log out, die, be extracted, or a teammate doing the opposite. A design entry, a plan and a build name each of them and what happens.
- **Singleplayer and server behave the same.** Rules live in the mod jar or in `pack/config`, never in a world save or a client-only setting.
- **Gear has plain names** (the Satellite Messenger, the Water Bottle), never brand names, so the pack can be published without renaming.

## Checking the work

The gates are `just lint`, then `just test`. `just shape <seeds>` draws worlds from the game's own generator into `build/shape/`; the dev server's watchdog is off (`max-tick-time` -1) so such long commands finish.

## Mods

- packwiz marks a mod for both sides unless Modrinth says otherwise; set `side = "client"` in its `.pw.toml` when the server never needs it, so the server pack stays lean.
- PandaLib is pinned at 0.6.0: Panda's Falling Trees 0.14.0 crashes at load with PandaLib 0.6.2.
