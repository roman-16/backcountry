---
name: design
description: Run a Backcountry design session - take an open question in design/, research how survival games and mods answer it, offer options with a recommendation, and write the user's answer back as a settled decision. Use when the user asks to continue the design, work on a system, the world or the interface, or answer an open design question. A design session writes documents in design/ only.
---

# Design

`design/` decides what Backcountry becomes. A session moves one to three questions from Open to Settled.

## Start

1. Read `design/README.md` for the milestone under way, and `design/vision.md` whole: its Settled decisions bind every answer.
2. Take the question the user names, or the first Open one in the document the milestone builds. When it needs another question that is still open, take that one first.

## Work a question

- **Research before proposing**: how survival games and mods answer it, with the `exa` skill; what Minecraft 1.21.1, NeoForge and the pack's mods can do, from `just sources` and the jars, never from memory. Mark what was not verified.
- **Show it as play.** Each option is what the player sees and does: a moment of play, a HUD line, a screen. Mechanism comes after, if at all.
- **Numbers**: a number that tunes something is a power of two or a sum of two such; a number that means something stays as it is.
- **Say how the player learns it**: what the screen tells them, which key or click it takes, what shows it works, what a player alone does and what a teammate sees.
- **Name the edge cases**: what happens when the survivor walks off, logs out, dies, is extracted, sleeps, or a teammate does the opposite.
- **Recommend one**, lead with it and its reason, then ask with the `questionnaire` tool.

## Write it down

- The answer goes under Settled in its document, as the player meets it, with its edge cases. It leaves Open.
- A question the answer raises goes under Open with the next free number.
- A Settled decision changes only when the user reopens it; a new direction reopens whatever it contradicts, and every document it touches is rewritten to fit.

## Bounds

Never settle what the user did not decide. End with what was settled, what was opened and which milestone it belongs to.
