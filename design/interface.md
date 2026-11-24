# Interface

Everything read off the screen outside the world. Units are metric: °C, kg, km/h.

## Settled

### I1 The HUD

The hearts stay and the energy bolts, drawn like vanilla's hunger icons over empty slots, take the hunger bar's place; they shake while the survivor is too tired to sprint. Water is a blue bar drawn like vanilla's XP bar, across the hotbar's width just above it, and it blinks below a quarter; the hearts, the bolts and the XP level lift to make room. Right of the hotbar, each line on a dark plate with its icon, the body panel shows the core temperature and how it feels (blue when cold, orange when hot), the wetness while wet, and above them the conditions by name.

```text
 ♥♥♥♥♥♥♥♥♥♥       12       ⚡⚡⚡⚡⚡⚡░░░░    ❄ Frostnip
 ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬░░░░░░░░░░░░░   ✚ Infected wound
 ══════════════════════════════════════════   💧 Wet 37%
 [ ][ ][ ][ ][ ][ ][ ][ ][ ]                   🌡 35.6 °C · very cold
```

Everything the pack draws wears vanilla's own look: its panels, buttons, slots and font.

### I2 The Satellite Messenger

Right-click opens it, drawn as the device itself: an orange handheld with a green-on-dark screen, the only screen with a skin of its own, as vanilla's book has its page. Its pages are tabs: the shown page's tab carries its icon and name, the others their icon alone (hovering names them), switched by a click, the arrow keys, 1 to 6 or the ◀ ▶ keys on the case. Every value has its icon and a meter; the air sits on a gauge from -40 to +30 °C banded at freezing and at -15 °C, the wind on a meter with the blizzard mark at 56 km/h, the core temperature on a gauge banded freezing, cold, comfortable and hot, and the weight on a BMI gauge with extraction marked at 17; warnings turn amber or red. A strip of alerts sits under every page, two lines at most, "and N more" past them, "No alerts" when there are none.

```text
┌ SATELLITE MESSENGER ────────────────────────────┐
│ Day 31 · 17 Dec                         SAT ▮▮▮ │
│ [☁ Weather]  📅   🔥   🌡   🍖   ♥              │
│ 🌡 Air            -6 °C · feels like -13 °C     │
│ ❄ Precipitation       Light snow until 18:00    │
│ ≋ Wind                            18 km/h NW    │
│ ☁ Cloud                                  80%    │
│ 📅 Today              High -3 °C · low -11 °C   │
├─────────────────────────────────────────────────┤
│ ⚠ Snow tomorrow  ⚠ Frost tonight                │
│ ⚠ Weight dropped                                │
└─────────────────────────────────────────────────┘
```

The Forecast page gives each of the next three days two lines: its sky (the icon of the rain or snow when it is likely, else of the cloud), the day and date, the chance ("Snow 80%"), and below it the high, the low and the wind. A new alert beeps once and lights a small amber light on the Messenger's icon in the hotbar until the Messenger is opened.

### I3 Food

```text
Raw Moose Meat
1,600 kcal · 10 g fat
Spoils in 2.3 days here (9 °C)
```

### I4 The campfire

```text
┌ Campfire ─────────────────────────────────────────┐
│ Fuel queue  [Spruce 64%] [Birch 81%] [   ] [   ]  │
│ Heat ███████░░░   Low · [Steady] · Hot           │
│ Burning 1 h 20 min · queued 3 h 10 min           │
│ ✓ Firewood queued  ✓ Coals banked                 │
│ ✗ Out of the rain  · Cook raw food on it          │
└───────────────────────────────────────────────────┘
```

### I5 The smoker and the food cache

The smoker: hanging slots and firewood slots, a 12-hour batch, a smoke level from cold to rising. The food cache: usable kcal, days at the team's ration, the ration (Lean, Normal, Feast), fat reserve, the spoilage multiplier, and its scent ("Scent attracts scavengers").

### I6 Hunting

A hit to a major organ drops the animal; any other hit: "Wounded, it fled", and a blood trail. "Sneak + empty-hand click to carry. Set it down and use your knife to dress it."; dressing shows a progress bar; without a knife: "Skinning knife required".

### I7 The end of a survivor and of a year

```text
Extracted · Day 31 · 17 Dec
BMI 17.0 · 55.0 kg (arrived at 77.0 kg)
 77 ┤▀▀▄▄▄
    │     ▀▀▀▄▄▄
 55 ┤           ▀▀▄
    └ Day 1 ──── Day 31
31 days · 61,200 kcal eaten · 1 moose · 3 beaver · 14 hare · 22 fish
[ Arrive as a new survivor ]
[ Title Screen ]
```

It stands on vanilla's dark panel: the weight curve is a chart with 5 kg grid lines, the first and the last day, and a dashed red line where extraction comes; beside it the days, the kcal and each animal taken, each with its icon (the animal's spawn egg), three at most and "and N more". A death reads "Died" over vanilla's red. On 3 May the team's year shows, each survivor with their weight from start to end and when they arrived, died or were extracted, and "Year 2" begins:

```text
Year 1 · 3 May
Roman 77.0 → 68.4 kg
Mo 77.0 → 71.2 kg (arrived Day 12)
48 days · 412,000 kcal eaten · 2 moose · 5 beaver · 31 hare
[ Go on into Year 2 ]
```

The inventory shows its locked rows dark ("Needs a hide pack") and the back slot beside the armour.

## Open

None.
