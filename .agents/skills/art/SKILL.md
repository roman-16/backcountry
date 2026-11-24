---
name: art
description: Design how anything in Backcountry looks - item and block textures, models, worn clothing, the HUD and the screens - so it reads as if Mojang had made it. Use whenever something a player sees is added or changed.
---

# Art

Backcountry wears vanilla Minecraft's own look: the same pixel grid, light, palette, panels, buttons and font. The Satellite Messenger is the one screen with a skin of its own, the device's orange case and green-on-dark screen, as vanilla's book has its page.

## Before making anything

1. **Find its vanilla relative.** Every new thing has a vanilla cousin to measure against: a leather tunic beside the Hide Coat, a bundle beside the Hide Pack, a cauldron beside the Camp Pot. Vanilla's textures are in `mod/build/moddev/artifacts/neoforge-*-client-extra-aka-minecraft-resources.jar` under `assets/minecraft/`; read their pixels and palettes before drawing, and reuse a vanilla colour before inventing one.
2. **Start from vanilla where one fits.** A thing that is a vanilla thing in another material is that texture tinted (`[tinted]` in `generator/textures.toml`); only what has no vanilla relative is drawn from scratch.
3. **Show it to the user** before a look is final: its screenshots from the driver with the `questionnaire` tool, the recommended one first.

## Textures

- **One grid.** Items and blocks are 16 by 16 pixels and HUD icons 9 by 9, one texture pixel to one model pixel; never a higher resolution, never a stretched texture.
- **Few colours, in ramps.** Build each material from a midtone with one shadow and one highlight, then a few more shades; keep a texture to about ten colours.
- **Light from the top left**, and an item has a clearly darker outline and reads at a glance in a hotbar slot: the silhouette carries the idea, the inside only the material.
- **Avoid the classic faults:** banding, pillow shading, the darkest colour touching the lightest without a step between, lone pixels, staircase jaggies.
- **In this repository** every texture is data: rows of characters with a palette in `generator/textures.toml` (`[items]`, `[blocks]`, `[hud]`), or a vanilla texture with a colour (`[tinted]`, `[armour]`); `just generate` writes the PNGs, item models, block models and blockstates into `mod/src/generated/resources/`. Edit the source and generate, never the output.

## The HUD and the screens

- **Vanilla's parts.** Screens are built from vanilla's panels (`popup/background`), buttons, slots and font; the HUD's bolts, water bar and condition icons sit where vanilla's hunger bar and XP bar taught the player to look.
- **Every colour lives in `client/Look.java`.** A colour the interface needs is added there with its meaning, never written where it is used.
- **Judge it where the player meets it**, by screenshot from the driver (`shot`), at interface scales 2, 3 and 4, over snow at noon and at night.

## Checklist

Before calling a look done, beside its vanilla relative:

- Would a player believe Mojang made it?
- Does it read at a glance: an item in its slot, a block from a few blocks off, an icon above the hotbar?
- Could it be mistaken for another of the pack's things?
