---
name: playtest
description: The user's own play of Backcountry - the in-game checklist for what tests and pictures cannot show (the HUD and screens, cold, hunger and thirst over time, extraction and death, a team), and writing the results back into balance.toml, a mod's config or design/. Use when the finish skill hands the user what to play, when the user asks what to playtest, or brings back notes from a play.
---

# Playtest

`just test` proves the rules hold; it cannot judge how anything looks or feels. That is the user's, in game.

## Hand the user a checklist

The `finish` skill comes first, so this list keeps only what play alone shows. Pick the rows the change touched and give each as one line: where to go, what to do, what should happen. `just play` opens a fresh world in a dev client without the shader and the far view; both are judged in the published pack.

| Area | Check |
|---|---|
| Arrival | You stand on the drop-off lake's shore with Satellite Messenger, Bear Spray, Med Kit, Water Bottle (boiled), Pilot Bread ×38, Camp Pot and Headlamp, a Supply Crate a few blocks off; no mod gives anything else, greets you in chat or pops up a toast |
| HUD | Bolts where hunger was, shaking when too tired to sprint; a blue water bar above the XP bar, blinking below a quarter; the body panel right of the hotbar with the core temperature, wetness and conditions; all readable at interface scales 2, 3 and 4 |
| Eating | Every food's tooltip shows kcal and fat; eating stops at "Too full to eat" and starts again a few hours later |
| Water | The Camp Pot fills at a lake, boils on a lit campfire in about 4 minutes and steams meanwhile; the bottle fills from it; drinking stops at "Not thirsty" |
| Cold | A May night in base clothes is cold but survivable; a winter night without fire, roof or hide clothing brings frostnip; a fire undoes it |
| Energy | A day of chopping drains the bolts; "Too tired to sprint" below a fifth; a night's sleep refills them |
| Inventory | Three dark rows saying "Needs a hide pack"; the Hide Pack in the back slot opens one; its items stay in it when it comes off |
| Messenger | Right-click opens seven pages that match what you feel in the world; the Rations page tells the next meal and the stores; a new alert beeps and lights the Messenger's icon until you open it |
| Weather | Rain, snow and dry spells come when the Weather page says; snow falls where the air is at freezing, rain where it is warmer; the forecast is mostly right for tomorrow and less so three days out; out in the wind you cool faster than under a roof; a blizzard closes the view in |
| Fire | Birch bark, tinder, cordage and a bow drill light a campfire with the A/D and Space game; the screen's queue, settings, coals and checklist match the fire; green wood is refused; a woodpile dries; the headlamp lights the night and dims in deep cold |
| Food | Tooltips count down "Spoils in X days here"; the cache feeds dawn and dusk rations and tells its days; the smoker and the cooking pot finish what they promise; berries ripen in their months; mice find an unguarded cache |
| Hunting | Animals show up out of sight and fewer in rain; a chest shot drops big game, others leave a blood trail; carcasses carry and dress into cuts; hides become clothes; the moose call answers in the rut |
| Traps and water | Snares, deadfalls, nets and tip-ups catch while you are away; fish give kcal by weight; the raft pitches you off in a gale; lakes freeze in a cold spell |
| End | At BMI 17 the screen fades, the summary shows the weight curve, and Arrive brings a fresh survivor to the drop-off; your left pack stands where you were |
| Team | A teammate sees your extraction in chat and can open your left pack; on 3 May the year's summary names everyone |
| Balance | Whether a week of game days felt hungry but fair, whether cold, water and energy each mattered without nagging, and whether the weather's swings, the wind and the alerts felt like Alaska without nagging |

## Bring the results back

Read what the user's own game wrote while they played: the Prism instance's `crash-reports/` and `logs/latest.log`. Every crash is a finding of its own, fixed at its cause first.

Every number the user wants changed goes to `balance.toml`, or to the mod's config in `pack/config` when it is another mod's number. A rule the user wants changed goes back to Open in its design document first, through the `design` skill.
