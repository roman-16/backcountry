# Production

## Settled

- **Release:** private to friends: the repository is public, a Prism instance updates itself from GitHub Pages (`https://roman-16.github.io/backcountry/1/Backcountry.zip`), and the mod jar comes from the GitHub release. A version is its dated section in `CHANGELOG.md`, released when it reaches main.
- **Performance:** the shipped pack draws with Complementary Unbound through Iris and Distant Horizons' far view. The frame rate bench (`bench <seconds>` in the dev client, the `playthrough` skill) turns the camera once around: on the user's desktop at 1920 by 1080 and 8 chunks it held 59 frames a second on average and 53 at its lowest; a lowest below 30 is a finding.

- **Tooling:** game tests run headless with the pack's server mods; the dev client and the dev server are driven by `scripts/client.py` and `scripts/server.py`, with pretend players for a team. The client walks (`walk <x> <z>`) and keeps checkpoints of its world, and `/backcountry playthrough` lives a survivor through game days on the dev server for balance; the `playthrough` skill runs them.
- **Balance** follows the playthroughs: in a camp with a roof, a fire and hide clothing, a Normal ration keeps a survivor near their weight through a year (77 to 76 kg), and a hard year of Lean rations and heavy work ends near the reference year's 67 kg.

## Open

None.
