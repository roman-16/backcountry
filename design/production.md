# Production

## Settled

- **Release:** private to friends: the repository is public, a Prism instance updates itself from GitHub Pages (`https://roman-16.github.io/backcountry/1/Backcountry.zip`), and the mod jar comes from the GitHub release. A version is its dated section in `CHANGELOG.md`, released when it reaches main.
- **Performance:** the shipped pack draws with Complementary Unbound through Iris and Distant Horizons' far view, 1,024 chunks out, which a server keeps up to date as far (`pack/config/DistantHorizons.toml`). The frame rate bench (`bench <seconds>` in the dev client, the `playthrough` skill) turns the camera once around: on the user's desktop at 1920 by 1080 and 8 chunks it held 60 frames a second on average and 59 at its lowest once the far land had loaded, and 41 at its lowest in a fresh world's first minute; a lowest below 30 is a finding.
- **Night and the shader, as in Wayfarer:** Iris draws Complementary Unbound from the first launch (`pack/config/iris.properties`). Without the shader More Darkness darkens the night: a moonless night is black beyond the fire, a full moon shows the land in pale grey, and the eyes adapt over a few seconds; the mod has it read the screen's brightness a few frames late instead of waiting on the graphics card every frame. With the shader on, the mod switches More Darkness off and the shader's own night settings (Wayfarer's, `generator/shader.toml`) give the night. One moon serves both: the new, partial and full moons' light are `balance.toml` `[sky]`, read by the shader's options and More Darkness alike, and the clouds float at `[sky]` `cloud_height`, 384, above the build limit with and without the shader.
- **Server:** the team plays on an exaroton server. `just exaroton <server>` puts the tree's server pack on it while it is offline: NeoForge, launched by its server starter jar, the mods, the configs and the pack's server properties, uploading only what changed and removing what the pack no longer holds. The world, the whitelist and the operators stay; a running server is refused.

- **Tooling:** game tests run headless with the pack's server mods; the dev client and the dev server are driven by `scripts/client.py` and `scripts/server.py`, with pretend players for a team. The client walks (`walk <x> <z>`) and keeps checkpoints of its world, and `/backcountry playthrough` lives a survivor through game days on the dev server for balance; the `playthrough` skill runs them.
- **Balance** follows the playthroughs: in a camp with a roof, a fire and hide clothing, a Normal ration with 1,024 kcal of work a day keeps a survivor near their weight through a year (77 to 76.1 kg), and a hard year of Lean rations and 2,048 kcal of work a day ends at 66.1 kg, near the reference year's 67 kg.

## Open

None.
