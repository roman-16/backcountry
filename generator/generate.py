import json
import shutil
import subprocess
import sys
import tempfile
import tomllib
import zipfile
from pathlib import Path

import nbt
import png

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "generator/textures.toml"
OUT = ROOT / "mod/src/generated/resources"
DEV = ROOT / "mod/src/generated/dev"
ASSETS = OUT / "assets/backcountry"
ARTIFACTS = ROOT / "mod/build/moddev/artifacts"
VANILLA = "assets/minecraft/textures/"
TRANSPARENT = "."
DATA_VERSION = 3955
TEST_ROOM = 8


def hex_colour(text):
    return [int(text[i : i + 2], 16) for i in (0, 2, 4)] + [255]


def grid(name, entry, size):
    rows = entry["rows"]
    if len(rows) != size or any(len(row) != size for row in rows):
        sys.exit(f"textures.toml: {name} must be {size} rows of {size} characters")
    palette = {letter: hex_colour(colour) for letter, colour in entry["palette"].items()}
    missing = {letter for row in rows for letter in row if letter != TRANSPARENT and letter not in palette}
    if missing:
        sys.exit(f"textures.toml: {name} uses {''.join(sorted(missing))} without a colour")
    return [[[0, 0, 0, 0] if letter == TRANSPARENT else palette[letter] for letter in row] for row in rows]


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def vanilla_jar():
    found = sorted(ARTIFACTS.glob("*client-extra*.jar"))
    if not found:
        sys.exit("generate needs Minecraft's resources: run just compile first")
    return zipfile.ZipFile(found[-1])


def tint(jar, base, colour, target, scratch):
    source = scratch / (base.replace("/", "_") + ".png")
    source.write_bytes(jar.read(VANILLA + base + ".png"))
    dark, light = colour.split(",") if "," in colour else ("000000", colour)
    command = [
        "magick",
        str(source),
        "-modulate",
        "100,0",
        "-channel",
        "RGB",
        "+level-colors",
        f"#{dark},#{light}",
        "+channel",
    ]
    overlay = VANILLA + base + "_overlay.png"
    if overlay in jar.namelist():
        layer = scratch / (base.replace("/", "_") + "_overlay.png")
        layer.write_bytes(jar.read(overlay))
        command += [str(layer), "-composite"]
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([*command, "-strip", f"PNG32:{target}"], check=True)


def item_model(name):
    write_json(
        ASSETS / f"models/item/{name}.json",
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"backcountry:item/{name}"}},
    )


def blocks():
    write_json(
        ASSETS / "models/block/supply_crate.json",
        {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": "backcountry:block/supply_crate_top", "side": "backcountry:block/supply_crate_side"},
        },
    )
    write_json(ASSETS / "models/item/supply_crate.json", {"parent": "backcountry:block/supply_crate"})
    write_json(
        ASSETS / "models/block/camp_pot.json",
        {
            "parent": "minecraft:block/block",
            "textures": {
                "inside": "minecraft:block/cauldron_inner",
                "particle": "minecraft:block/cauldron_side",
                "side": "minecraft:block/cauldron_side",
                "top": "minecraft:block/cauldron_top",
            },
            "elements": [
                {
                    "from": [4, 0, 4],
                    "to": [12, 6, 12],
                    "faces": {
                        "down": {"texture": "#inside"},
                        "east": {"texture": "#side"},
                        "north": {"texture": "#side"},
                        "south": {"texture": "#side"},
                        "up": {"texture": "#top"},
                        "west": {"texture": "#side"},
                    },
                },
                {
                    "from": [7.5, 6, 4],
                    "to": [8.5, 9, 12],
                    "faces": {
                        "east": {"texture": "#side"},
                        "north": {"texture": "#side"},
                        "south": {"texture": "#side"},
                        "up": {"texture": "#side"},
                        "west": {"texture": "#side"},
                    },
                },
            ],
        },
    )
    write_json(
        ASSETS / "models/block/left_pack.json",
        {
            "parent": "minecraft:block/block",
            "textures": {
                "hide": "minecraft:block/brown_wool",
                "particle": "minecraft:block/brown_wool",
                "strap": "minecraft:block/stripped_spruce_log",
            },
            "elements": [
                {
                    "from": [3, 0, 4],
                    "to": [13, 10, 12],
                    "faces": {face: {"texture": "#hide"} for face in ("down", "east", "north", "south", "up", "west")},
                },
                {
                    "from": [4, 6, 3],
                    "to": [12, 9, 4],
                    "faces": {face: {"texture": "#strap"} for face in ("down", "east", "north", "south", "up", "west")},
                },
            ],
        },
    )
    for name in ("camp_pot", "left_pack", "supply_crate"):
        write_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"backcountry:block/{name}"}}})


def test_room():
    blocks = [
        {"pos": [x, y, z], "state": 0} for x in range(TEST_ROOM) for y in range(TEST_ROOM) for z in range(TEST_ROOM)
    ]
    nbt.write(
        DEV / "data/backcountry/structure/empty.nbt",
        {
            "DataVersion": DATA_VERSION,
            "blocks": blocks,
            "entities": [],
            "palette": [{"Name": "minecraft:air"}],
            "size": [TEST_ROOM, TEST_ROOM, TEST_ROOM],
        },
    )


def main():
    source = tomllib.loads(SOURCE.read_text())
    shutil.rmtree(OUT, ignore_errors=True)
    shutil.rmtree(DEV, ignore_errors=True)
    test_room()
    for name, entry in source["items"].items():
        png.write(ASSETS / f"textures/item/{name}.png", grid(name, entry, 16))
        item_model(name)
    for name, entry in source["blocks"].items():
        png.write(ASSETS / f"textures/block/{name}.png", grid(name, entry, 16))
    for name, entry in source["hud"].items():
        png.write(ASSETS / f"textures/gui/sprites/hud/{name}.png", grid(name, entry, 9))
    with vanilla_jar() as jar, tempfile.TemporaryDirectory() as folder:
        scratch = Path(folder)
        for name, entry in source["tinted"].items():
            tint(jar, entry["base"], entry["colour"], ASSETS / f"textures/item/{name}.png", scratch)
            item_model(name)
        for name, entry in source["sprites"].items():
            tint(jar, entry["base"], entry["colour"], ASSETS / f"textures/gui/sprites/hud/{name}.png", scratch)
        for name, entry in source["armour"].items():
            for layer in (1, 2):
                tint(
                    jar,
                    f"models/armor/leather_layer_{layer}",
                    entry["colour"],
                    ASSETS / f"textures/models/armor/{name}_layer_{layer}.png",
                    scratch,
                )
    blocks()


main()
