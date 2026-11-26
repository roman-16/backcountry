---
name: playthrough
description: Play Backcountry automatically to balance it - year-long body runs on the dev server, live play with the walking dev driver and its checkpoints, and the frame rate bench - and write what they show back into balance.toml or design/. Use when the user asks to balance the pack, play it through, measure a week, a winter or a year, or bench the frame rate, or when the user cannot play before a change is judged.
---

# Playthrough

The user's own play (`playtest` skill) judges how the pack feels; a playthrough measures what the numbers do over game days that no game test covers. Its results are numbers in `balance.toml`, written back the way the `playtest` skill writes a play's notes.

## Body runs

`/backcountry playthrough <name> <first_day> <days> <kcal_a_day> <work_kcal_a_day>` (dev runs) lives a survivor hour by hour through the island's calendar where the command runs: the body, the climate, the weather, shelter and fire there, in the hide clothing, eating half the kcal at 06:00 and half at 18:00 (pilot bread and three lingonberries a meal), working the work kcal over the waking hours and sleeping 22:00 to 06:00. It writes one line a day (weight, BMI, energy, core temperature, kcal eaten and burned, conditions) to `build/devservers/playthroughs/<name>.csv` and replies with the end weight.

Build the place first with commands, then run it there: a camp is a roof, a fire on netherrack two blocks off and solid ground, `forceload` around it.

```console
$ python3 scripts/server.py start --fresh
$ P='execute in minecraft:overworld positioned 0 64 0 run'
$ python3 scripts/server.py run 'forceload add -16 -16 16 16' "$P fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 stone" "$P fill ~-3 ~ ~-3 ~3 ~3 ~3 air" \
    "$P fill ~-3 ~4 ~-3 ~3 ~4 ~3 spruce_planks" "$P setblock ~2 ~-1 ~ netherrack" "$P setblock ~2 ~ ~ fire" \
    "$P backcountry playthrough year 1 48 3200 1024"
```

What the runs should show, from the reference year (77 kg to 67 kg); a camp day's work is 1,024 kcal (two game hours on foot, one chopping, one at other work), a hard day's 2,048:

| Run | Arguments | Should end near |
|---|---|---|
| The first week on a Normal ration | `1 7 3200 1024` | 76.9 kg |
| A winter in camp | `31 12 3200 1024`, and `2560` for Lean | 76.8 kg, 75.7 kg Lean |
| A steady year | `1 48 3200 1024` | 76 kg |
| A hard year, Lean rations and heavy work | `1 48 2560 2048` | 66 to 67 kg |

A run that lands far from its row, or a camp run that shivers, is a balance finding: find the number in `balance.toml` that drives it, change it within the pack's rules, and run every row again.

## Live play

The dev client walks and keeps checkpoints, so an agent can play for hours in steps of under a minute:

- `walk <x> <z>`: walks to the column, facing it, jumping when blocked or in water; returns on arrival with where it stands.
- `python3 scripts/client.py checkpoint <name>` saves the open world and keeps a copy; `python3 scripts/client.py restore <name>` opens that copy again, so a long play resumes after a failure.
- `/tick rate` speeds a stretch up; play one stretch at 20 ticks a second, since how long it feels is a finding too.

## The frame rate bench

```console
$ python3 scripts/client.py start --fresh --mode shipped
$ python3 scripts/client.py run '/time set noon' 'wait 200' 'bench 40'
```

`bench <seconds>` turns the camera once around over the time and replies with the average and lowest frames a second; `graphics <step> shaders on|off` picks the Preset's step first, and `far` waits until the far land is drawn. A drop below 30 lowest is a finding.

## Write it back

Numbers go to `balance.toml`; a rule that has to change goes to `design/` as an Open question for the user. End with each run's end weight against its row, the bench, and what changed.
