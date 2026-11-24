---
name: finish
description: Finish everything built since the last commit, so the user can commit it - the gates, a newcomer's walkthrough of what a survivor meets, and a report with what the user plays. Use when the user calls it, or asks to run the gates or walk the work through.
---

# Finish

Development checks each change in the running game as far as the change needs. Finish proves the whole of the uncommitted work once, the state the user is about to commit. It never commits; that is the user's, through the `commit` skill.

## The scope

- Everything uncommitted: `git status --short --untracked-files=all` and `git diff HEAD --stat`.
- From the files, list what a player meets that changed: the HUD, screens, items, blocks, the body's rules, mods and their configs. That list decides the walkthrough and the playtest rows.
- When nothing a player meets changed (tooling, tests, documents), only the gates run.

## The order

1. `just lint`, fixing every finding.
2. The walkthrough of the list.
3. `just test`.
4. The report.

A fix after a step means its earlier steps run again for what the fix touched.

## Walk it through

Play what changed as a newcomer who knows vanilla Minecraft and nothing of the pack, knowing only what the screen says:

- **Alone first**, in a fresh world: `python3 scripts/client.py start --fresh`, then steps such as `seen`, `use`, `key inventory`, `shot <name>` and commands (`/backcountry body weight 55`) to reach a state quickly. Read every screenshot.
- **Then with a teammate**: `python3 scripts/server.py start --fresh` (it prints its port), `python3 scripts/server.py run 'op Survivor' 'backcountry pretend join Mo'`, then the client joins it: `python3 scripts/client.py run 'join localhost:<port>'`. Team rules, such as a left pack anyone opens or the year's summary naming everyone, only show with two.
- **At the survivor's pace once**: a changed rule that runs over time is checked fast with `/tick rate`, then played once at 20 ticks a second, since how long it feels is a finding too.

At every step ask: does the screen say what happened and what to do next? Would a newcomer know which key or click? A no is a finding. Fix every finding in the code, the words or the look (`art` skill); a finding that needs a decision goes to the user as an Open question in its design document. Read the logs for what the work caused: the client's `log` step and the dev server's `logs/latest.log`.

## The report

- **Finished**, with what the gates said.
- **Found and fixed**, one line each.
- **Not checked**, and why.
- **For you to play**: the `playtest` skill's rows the scope touched, one line each.
