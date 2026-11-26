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
| Arrival | You stand on the coast facing the sea, wearing a Canvas Pack and the Binoculars, with Satellite Messenger, Bear Spray, Water Bottle (boiled), Pilot Bread ×38 and Camp Pot in the hotbar and the Sleeping Bag, a Mono Line, 4 hooks, the Med Kit and the Headlamp in the pack, a Supply Crate a few blocks off; no mod gives anything else, greets you in chat or pops up a toast |
| HUD | Bolts where hunger was, shaking when too tired to sprint; a blue water bar where the XP bar was, blinking below a quarter, and no XP anywhere; the body panel right of the hotbar with the core temperature, wetness and conditions; all readable at interface scales 2, 3 and 4 |
| Eating | Every food's tooltip shows kcal and fat; eating stops at "Too full to eat" and starts again a few hours later |
| Water | The Camp Pot fills at a lake but not at the sea ("Sea water is salt: fill it at a lake or river"), boils on a lit campfire in about 4 minutes and steams meanwhile; the bottle fills from it; drinking stops at "Not thirsty" |
| Cold | A May night in base clothes is cold but survivable; a winter night without fire, roof or hide clothing brings frostnip; a fire undoes it |
| Energy | A day of chopping drains the bolts; "Too tired to sprint" below a fifth; a night's sleep refills them |
| Inventory | The Canvas Pack opens the row above the hotbar and the next reads "Needs a hide pack"; the Hide Pack in the back slot opens two; a pack's items stay in it when it comes off; logs stack to 4 and sticks to 32 while fibre and hooks stack to 64; a stone axe is a rock, a stick and a cordage, and planks come only from a stripped log and an axe |
| Messenger | Right-click fills the screen with five named pages that match what you feel in the world, the time in the header; a value in warning says what helps under it; the Food page tells the next meal and the stores; a new alert beeps and lights the Messenger's icon until you open it; you stand still while it is open |
| Weather | Rain, snow and dry spells come when the Weather page says; snow falls where the air is at freezing, rain where it is warmer; the forecast is mostly right for tomorrow and less so three days out; out in the wind you cool faster than under a roof; a blizzard closes the view in |
| Felling | Bare hands get nothing from a log; loose rocks and twigs make a stone axe that fells it |
| Lights | A torch burns about 30 minutes and a lantern an hour, in hand too with a bar on the slot; charcoal and Rendered Fat top them up; a spent lantern hangs dark until refilled; a winter night is spent by the fire or under the headlamp |
| Fire | Birch bark, tinder, cordage and a bow drill light a campfire with the A/D and Space game; the screen's queue, settings, coals and checklist match the fire; green wood is refused; a woodpile dries; the headlamp lights the night and dims in deep cold |
| Food | Tooltips count down "Spoils in X days here"; the cache feeds dawn and dusk rations and tells its days; the smoker and the cooking pot finish what they promise; berries ripen in their months; mice find an unguarded cache |
| Hunting | Animals show up out of sight and fewer in rain; a chest shot drops big game, others leave a blood trail; carcasses carry and dress into cuts; hides become clothes; the moose call answers in the rut |
| Binoculars | C brings the land 8 times closer through two circles, worn at the neck or from the hotbar, and holding use does with them in hand; from over the shoulder the look turns to first person and back; you walk slowly while looking; animals far off across a valley show through them; the empty neck and back slots show outlines |
| Traps and water | Snares, deadfalls, nets and tip-ups catch while you are away; fish give kcal by weight; the raft pitches you off in a gale; lakes freeze in a cold spell |
| Tasks | The top left shows the next step from arrival, with the items it needs and what you hold of them; each step ticks with a click as you do it, a task done shows a toast; the Messenger's Tasks page lists the seven chapters, a click on an item opens its recipe, Track follows another task; a task out of season reads "From June" and is passed over; a friend joining late starts on their own chapters and sees the team's goals |
| Body | The Food page's Rest, Move, Load, Work, Cold and Digest add up to the day's burn; a day hauling through deep snow burns far more than a day by the fire, snowshoes and a lighter load less; the Body page's skills rise with use, say so on the action bar and are gone for a new survivor |
| Forage | Salmonberries in June, highbush cranberries from September into winter, white baneberries that poison; spruce needles from leaves broken with the knife steep into tea in the Camp Pot; a birch tap fills the bottle with sap in May and the pot boils it down to syrup; fish left out in deep cold reads "Frozen through"; dead spruces split into dry wood; a red mushroom poisons the stew |
| Bears and the sled | Grizzlies leave you be in July unless surprised and come at you in spring and autumn; most charges stop short with a roar if you stand still; a sow charges near her cubs; black bears run off except at a scented cache; bears fish the rivers from July and leave half-eaten salmon; a sled on a lead glides on snow, drags on ground and takes a moose whole |
| The island | No cave opening anywhere, no ore in cliffs but surface iron, gold-flecked gravel in birch-forest gravel pits, no pumpkins, surface lava or bees; an obsidian frame lit with flint and steel opens nothing; JEI shows nothing from the Nether, the End or exotic animals; animals neither breed nor tame; the compass points north |
| Controls | Create New World offers Survival and Creative, Normal greyed out, only the seed on the World tab and no Experiments, and goes straight to loading; Shift sprints and Ctrl sneaks; Controls has a search box; mouse button 5 marks a spot your friend sees; the pause menu has Statistics across its row; F3 shows no coordinates; the published pack draws the shader from the first launch, and nights are dark with and without it |
| End | At BMI 17 the screen fades, the summary shows the weight curve, and Arrive brings a fresh survivor to the drop-off; your left pack stands where you were |
| Team | A teammate sees your extraction in chat and can open your left pack; on 3 May the year's summary names everyone |
| Voice and team | V opens the voice menu; a friend's voice fades past about 48 blocks and behind walls; their head shows when they type or have a screen open; sneaking at a bush names it and its mod in a small box at the top, and item tooltips end with their mod |
| Comfort | What you pick up lists at the top right; the hotbar bounces; an empty hand on a slab sits you down; F5 goes over the shoulder and U swaps sides; you walk with the inventory open but stand still in the bow-drill game; a middle click sorts a chest |
| Small things | Breath steams on a cold morning and not on a warm afternoon; the stomach growls when empty; footprints in snow and sand; a path wears between camp and water over days; wakes trail the raft; thunder comes seconds after its flash; the far land fills in smoothly where you arrive |
| Balance | Whether a week of game days felt hungry but fair, whether cold, water and energy each mattered without nagging, and whether the weather's swings, the wind and the alerts felt like Alaska without nagging |

## Bring the results back

Read what the user's own game wrote while they played: the Prism instance's `crash-reports/` and `logs/latest.log`. Every crash is a finding of its own, fixed at its cause first.

Every number the user wants changed goes to `balance.toml`, or to the mod's config in `pack/config` when it is another mod's number. A rule the user wants changed goes back to Open in its design document first, through the `design` skill.
