# Production

## Settled

- **Release:** private to friends: the repository is public, a Prism instance updates itself from GitHub Pages to the newest release, majors included (`https://roman-16.github.io/backcountry/Backcountry.zip`), and the mod jar comes from the GitHub release. A version is its dated section in `CHANGELOG.md`, released when it reaches main.
- **Performance:** the shipped pack draws with Complementary Reimagined through Iris and Distant Horizons' far view, as far as the Preset sets it. Distant Horizons SeedGen draws the far land from the world's seed, in singleplayer and on a server, which shares the seed with its players; its sampler matches the island the game generates (`/seedrenderer validate 2000 6000` on seed 1: 83% of columns to the block, the middle error 0, 95% within 19 blocks), its chat lines go to the log only and it keeps Distant Horizons' own share of each frame for loading. Lithium, FerriteCore, ModernFix, ImmediatelyFast, ScalableLux, C2ME, Noisiumed, Entity Culling and More Culling speed up the game, memory, drawing, light and chunks (C2ME and ScalableLux are alpha builds, and `just shape` measures the same island with them); Dynamic FPS draws 10 frames a second while the game is in the background, Packet Fixer gives slow connections longer time-outs, and spark (`/spark`) profiles a lagging server. The frame rate bench (`bench <seconds>` in the dev client, the `playthrough` skill) turns the camera once around; a lowest below 30 after the first minute is a finding.
- **Night and the shader:** Iris draws Complementary Reimagined from the first launch (`pack/config/iris.properties`). Without the shader More Darkness darkens the night: a moonless night is black beyond the fire, a full moon shows the land in pale grey, and the eyes adapt over a few seconds; the mod has it read the screen's brightness a few frames late instead of waiting on the graphics card every frame. With the shader on, the mod switches More Darkness off and the shader's own night settings (`generator/shader.toml`) give the night. The shader is Complementary Reimagined patched at launch by Euphoria Patches, whose added options keep their defaults; its pop-up, in-game messages and update check are off. One moon serves both: the new, partial and full moons' light are `balance.toml` `[sky]`, read by the shader's options and More Darkness alike, and the clouds float at `[sky]` `cloud_height`, 384, above the build limit with and without the shader.
- **The graphics Preset:** Video Settings, Sodium's screen, opens on a Backcountry page, first in its list, with a Graphics group: a Preset that steps through four settings, High on the first launch, and Shaders on or off. Each step sets the shader's profile, the draw distance, the simulation distance and Distant Horizons' far view with its detail and its share of the processor, and LambDynamicLights' quality, together (`balance.toml` `[graphics]`); changing any of them by hand makes the Preset read Custom, and pressing it again picks a step and sets them all. Shaders off draws plain Minecraft with More Darkness's nights. LambDynamicLights lights what is held and the worn Headlamp in both modes, and the shader's own held light is off (`generator/shader.toml`), so the Headlamp shines under the shader and a torch never lights twice.

  | | Low | Medium | High | Ultra |
  |---|---|---|---|---|
  | Shader profile | Low | Medium | High | Ultra |
  | Draw distance | 6 chunks | 10 | 12 | 16 |
  | Simulation distance | 6 chunks | 10 | 12 | 12 |
  | Far view | 128 chunks, about 2 km | 128, about 2 km | 256, about 4 km | 512, about 8 km |
  | Far view's detail | Low | Medium | Medium | High |
  | Far view's share of the processor | Least | Low | Balanced | Balanced |
  | Held light | Fastest | Fast | Fancy | Fancy |

- **Server:** the team plays on an exaroton server. `just exaroton <server>` puts the tree's server pack on it while it is offline: NeoForge, launched by its server starter jar, the mods, the configs and the pack's server properties, uploading only what changed and removing what the pack no longer holds. The world, the whitelist and the operators stay; a running server is refused.

- **Tooling:** game tests run headless with the pack's server mods; the dev client and the dev server are driven by `scripts/client.py` and `scripts/server.py`, with pretend players for a team. The client walks (`walk <x> <z>`) and keeps checkpoints of its world, and `/backcountry playthrough` lives a survivor through game days on the dev server for balance; the `playthrough` skill runs them.
- **Balance** follows the playthroughs: in a camp with a roof, a fire and hide clothing, a Normal ration with 1,024 kcal of work a day keeps a survivor near their weight through a year (77 to 76.1 kg), and a hard year of Lean rations and 2,048 kcal of work a day ends at 66.1 kg, near the reference year's 67 kg.

## Open

None.
