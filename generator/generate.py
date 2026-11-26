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
BALANCE = ROOT / "balance.toml"
OUT = ROOT / "mod/src/generated/resources"
DEV = ROOT / "mod/src/generated/dev"
ASSETS = OUT / "assets/backcountry"
ARTIFACTS = ROOT / "mod/build/moddev/artifacts"
VANILLA = "assets/minecraft/textures/"
TRANSPARENT = "."
DATA_VERSION = 3955
TEST_ROOM = 8
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
BUSHES = ("baneberry_bush", "blueberry_bush", "highbush_cranberry_bush", "salmonberry_bush")
PIECE = 4
PIECES_A_ROW = 4
GAP = 0.5
BRIGHTEST = 15
FRAME = 16
CLOUD_LAYERS_APART = 64
IRIS = ROOT / "pack/config/iris.properties"
SHADER_OPTIONS = ROOT / "generator/shader.toml"
SHADERPACKS = ROOT / "pack/shaderpacks"
PATCHER = ROOT / "pack/mods/euphoria-patches.pw.toml"
RESOURCEPACKS = ROOT / "pack/resourcepacks"
DEFAULT_OPTIONS = ROOT / "pack/config/defaultoptions/options.txt"
BUILT_IN_PACKS = ("vanilla", "mod_resources")


def hex_colour(text):
    return [int(text[i : i + 2], 16) for i in (0, 2, 4)] + [255]


def grid(name, entry, width, height=None):
    height = height or width
    rows = entry["rows"]
    if len(rows) != height or any(len(row) != width for row in rows):
        sys.exit(f"textures.toml: {name} must be {height} rows of {width} characters")
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


def recolour(jar, base, colours, target, scratch, frame=None):
    source = scratch / (base.replace("/", "_") + ".png")
    source.write_bytes(jar.read(VANILLA + base + ".png"))
    command = ["magick", str(source)]
    if frame is not None:
        command += ["-crop", f"{FRAME}x{FRAME}+0+{FRAME * frame}", "+repage"]
    for old, new in colours.items():
        command += ["-fill", f"#{new}", "-opaque", f"#{old}"]
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
    camp_pot()
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
    for hanging in ("", "_hanging"):
        write_json(
            ASSETS / f"models/block/empty_lantern{hanging}.json",
            {
                "parent": f"minecraft:block/template{hanging}_lantern",
                "textures": {"lantern": "backcountry:block/empty_lantern_block"},
            },
        )
    write_json(
        ASSETS / "blockstates/empty_lantern.json",
        {
            "variants": {
                "hanging=false": {"model": "backcountry:block/empty_lantern"},
                "hanging=true": {"model": "backcountry:block/empty_lantern_hanging"},
            }
        },
    )
    for name in ("left_pack", "supply_crate"):
        write_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"backcountry:block/{name}"}}})
    write_json(
        OUT / "assets/fowlplay/blockstates/bird_feeder.json", {"variants": {"": {"model": "minecraft:block/air"}}}
    )
    write_json(OUT / "assets/fowlplay/models/item/bird_feeder.json", {"parent": "minecraft:item/barrier"})


def pot_faces(outer, inner, top):
    return {
        "down": {"texture": "#pot", "uv": [0, 5, 8, 6]},
        "up": {"texture": "#pot", "uv": top},
        **{face: {"texture": "#pot", "uv": uv} for face, uv in (*outer, *inner)},
    }


def camp_pot():
    outer = [0, 0, 8, 6]
    inner = [8, 0, 16, 6]
    end = [0, 0, 1, 6]
    walls = [
        {
            "from": [4, 0, 4],
            "to": [12, 6, 5],
            "faces": pot_faces([("north", outer), ("east", end), ("west", end)], [("south", inner)], [0, 0, 8, 1]),
        },
        {
            "from": [4, 0, 11],
            "to": [12, 6, 12],
            "faces": pot_faces([("south", outer), ("east", end), ("west", end)], [("north", inner)], [0, 0, 8, 1]),
        },
        {
            "from": [4, 0, 5],
            "to": [5, 6, 11],
            "faces": pot_faces([("west", [1, 0, 7, 6])], [("east", [9, 0, 15, 6])], [1, 0, 7, 1]),
        },
        {
            "from": [11, 0, 5],
            "to": [12, 6, 11],
            "faces": pot_faces([("east", [1, 0, 7, 6])], [("west", [9, 0, 15, 6])], [1, 0, 7, 1]),
        },
        {
            "from": [5, 0, 5],
            "to": [11, 1, 11],
            "faces": {"down": {"texture": "#pot", "uv": [0, 8, 6, 14]}, "up": {"texture": "#pot", "uv": [0, 8, 6, 14]}},
        },
    ]
    rod = ("down", "east", "north", "south", "up", "west")
    bail = [
        {"from": start, "to": stop, "faces": {face: {"texture": "#pot", "uv": uv} for face in rod}}
        for start, stop, uv in (
            ([3.5, 4, 7.75], [4, 9, 8.25], [8, 8, 9, 13]),
            ([12, 4, 7.75], [12.5, 9, 8.25], [8, 8, 9, 13]),
            ([3.5, 9, 7.75], [12.5, 9.5, 8.25], [0, 0, 8, 1]),
            ([7.75, 9.5, 7.75], [8.25, 10, 8.25], [8, 8, 9, 9]),
        )
    ]
    crane = [
        {"from": start, "to": stop, "faces": {face: {"texture": "#stake", "uv": uv} for face in rod}}
        for start, stop, uv in (
            ([7, -16, -1.5], [9, 0, 0], [7, 0, 9, 16]),
            ([7, 0, -1.5], [9, 11, 0], [7, 5, 9, 16]),
            ([7, -16, 16], [9, 0, 17.5], [7, 0, 9, 16]),
            ([7, 0, 16], [9, 11, 17.5], [7, 5, 9, 16]),
            ([7.25, 10, -1.5], [8.75, 11.5, 8], [0, 7, 9.5, 8.5]),
            ([7.25, 10, 8], [8.75, 11.5, 17.5], [6.5, 7, 16, 8.5]),
        )
    ]
    water = {
        "from": [5, 5, 5],
        "to": [11, 5, 11],
        "faces": {"up": {"texture": "#water", "uv": [5, 5, 11, 11], "tintindex": 0}},
    }
    textures = {
        "particle": "backcountry:block/camp_pot",
        "pot": "backcountry:block/camp_pot",
        "stake": "minecraft:block/stripped_spruce_log",
        "water": "minecraft:block/water_still",
    }
    for name, elements in (("camp_pot_empty", walls + bail + crane), ("camp_pot", walls + bail + crane + [water])):
        write_json(
            ASSETS / f"models/block/{name}.json",
            {
                "ambientocclusion": False,
                "render_type": "minecraft:translucent",
                "textures": textures,
                "elements": elements,
            },
        )
    write_json(
        ASSETS / "blockstates/camp_pot.json",
        {
            "variants": {
                f"content={content}": {"model": "backcountry:block/camp_pot" + ("_empty" if content == "empty" else "")}
                for content in ("boiling", "empty", "heating", "water")
            }
        },
    )


def campfire(jar):
    template = json.loads(jar.read("assets/minecraft/models/block/template_campfire.json"))
    logs = [
        element
        for element in template["elements"]
        if all(face["texture"] != "#fire" for face in element["faces"].values())
    ]
    write_json(
        ASSETS / "models/block/campfire_coals.json",
        {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {
                "lit_log": "minecraft:block/campfire_log_lit",
                "log": "minecraft:block/campfire_log",
                "particle": "minecraft:block/campfire_log",
            },
            "elements": logs,
        },
    )
    for name in ("campfire", "campfire_off"):
        write_json(
            ASSETS / f"models/block/{name}.json",
            {"parent": f"minecraft:block/{name}", "render_type": "minecraft:cutout"},
        )
    variants = {}
    for facing, turn in FACINGS.items():
        for state, model in (
            ("lit=true", "campfire"),
            ("coals=true,lit=false", "campfire_coals"),
            ("coals=false,lit=false", "campfire_off"),
        ):
            variants[f"{state},facing={facing}"] = (
                {"model": f"backcountry:block/{model}", "y": turn} if turn else {"model": f"backcountry:block/{model}"}
            )
    write_json(ASSETS / "blockstates/campfire.json", {"variants": variants})
    write_json(ASSETS / "models/item/campfire.json", {"parent": "minecraft:item/campfire"})


def woodpile():
    pieces = tomllib.loads(BALANCE.read_text())["firewood"]["pile_pieces"]
    variants = {}
    for count in range(1, pieces + 1):
        elements = []
        for piece in range(count):
            row, column = divmod(piece, PIECES_A_ROW)
            low = column * PIECE
            side = "#bark" if (piece + row) % 2 == 0 else "#split"
            elements.append(
                {
                    "from": [0, row * PIECE, low + GAP],
                    "to": [16, row * PIECE + PIECE - GAP, low + PIECE - GAP],
                    "faces": {
                        "down": {"uv": [0, low, 16, low + PIECE], "texture": side},
                        "east": {"uv": [6, 6, 10, 10], "texture": "#end"},
                        "north": {"uv": [0, low, 16, low + PIECE], "texture": side},
                        "south": {"uv": [0, low, 16, low + PIECE], "texture": side},
                        "up": {"uv": [0, low, 16, low + PIECE], "texture": side},
                        "west": {"uv": [6, 6, 10, 10], "texture": "#end"},
                    },
                }
            )
        write_json(
            ASSETS / f"models/block/woodpile_{count}.json",
            {
                "parent": "minecraft:block/block",
                "textures": {
                    "bark": "minecraft:block/spruce_log",
                    "end": "minecraft:block/spruce_log_top",
                    "particle": "minecraft:block/stripped_spruce_log",
                    "split": "minecraft:block/stripped_spruce_log",
                },
                "elements": elements,
            },
        )
        for axis, turn in (("x", 0), ("z", 90)):
            model = {"model": f"backcountry:block/woodpile_{count}"}
            variants[f"axis={axis},pieces={count}"] = {**model, "y": turn} if turn else model
    write_json(ASSETS / "blockstates/woodpile.json", {"variants": variants})


def food():
    write_json(
        ASSETS / "models/block/food_cache.json",
        {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "bottom": "minecraft:block/spruce_planks",
                "side": "minecraft:block/spruce_planks",
                "top": "minecraft:block/barrel_top",
            },
        },
    )
    write_json(
        ASSETS / "models/block/smoker.json",
        {
            "parent": "minecraft:block/orientable",
            "render_type": "minecraft:cutout",
            "textures": {
                "front": "minecraft:block/spruce_door_bottom",
                "side": "minecraft:block/spruce_planks",
                "top": "minecraft:block/spruce_trapdoor",
            },
        },
    )
    faces = ("down", "east", "north", "south", "up", "west")
    legs = [
        {"from": [x, 0, z], "to": [x + 1, 2, z + 1], "faces": {face: {"texture": "#side"} for face in faces}}
        for x in (3, 12)
        for z in (3, 12)
    ]
    write_json(
        ASSETS / "models/block/cooking_pot.json",
        {
            "parent": "minecraft:block/block",
            "textures": {
                "inside": "minecraft:block/cauldron_inner",
                "particle": "minecraft:block/cauldron_side",
                "side": "minecraft:block/cauldron_side",
                "top": "minecraft:block/cauldron_top",
            },
            "elements": [
                *legs,
                {
                    "from": [3, 2, 3],
                    "to": [13, 9, 13],
                    "faces": {
                        face: {"texture": "#inside" if face == "down" else "#top" if face == "up" else "#side"}
                        for face in faces
                    },
                },
            ],
        },
    )
    for name in ("cooking_pot", "food_cache", "smoker"):
        write_json(ASSETS / f"models/item/{name}.json", {"parent": f"backcountry:block/{name}"})
    write_json(ASSETS / "blockstates/cooking_pot.json", {"variants": {"": {"model": "backcountry:block/cooking_pot"}}})
    for name in ("food_cache", "smoker"):
        write_json(
            ASSETS / f"blockstates/{name}.json",
            {
                "variants": {
                    f"facing={facing}": {"model": f"backcountry:block/{name}", "y": turn}
                    if turn
                    else {"model": f"backcountry:block/{name}"}
                    for facing, turn in FACINGS.items()
                }
            },
        )
    for bush in BUSHES:
        for stage in range(4):
            write_json(
                ASSETS / f"models/block/{bush}_stage{stage}.json",
                {
                    "parent": "minecraft:block/cross",
                    "render_type": "minecraft:cutout",
                    "textures": {"cross": f"backcountry:block/{bush}_stage{stage}"},
                },
            )
        write_json(
            ASSETS / f"blockstates/{bush}.json",
            {"variants": {f"age={stage}": {"model": f"backcountry:block/{bush}_stage{stage}"} for stage in range(4)}},
        )
    write_json(
        ASSETS / "models/block/birch_tap.json",
        {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": "minecraft:block/stripped_birch_log",
                "wood": "minecraft:block/stripped_birch_log",
            },
            "elements": [
                {
                    "from": [6, 6, 10],
                    "to": [10, 10, 16],
                    "faces": {
                        face: {
                            "texture": "#wood",
                            "uv": [6, 6, 10, 10] if face in ("north", "south") else [10, 6, 16, 10],
                        }
                        for face in ("north", "south", "east", "west", "up", "down")
                    },
                },
                {
                    "from": [7, 5, 9],
                    "to": [9, 6, 11],
                    "faces": {face: {"texture": "#wood"} for face in ("north", "south", "east", "west", "down")},
                },
            ],
        },
    )
    write_json(
        ASSETS / "blockstates/birch_tap.json",
        {
            "variants": {
                f"facing={facing},sap={sap}": {"model": "backcountry:block/birch_tap", "y": turn}
                if turn
                else {"model": "backcountry:block/birch_tap"}
                for facing, turn in FACINGS.items()
                for sap in range(9)
            }
        },
    )
    write_json(
        ASSETS / "models/block/dead_spruce_log.json",
        {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": "backcountry:block/dead_spruce_log_top", "side": "backcountry:block/dead_spruce_log"},
        },
    )
    write_json(
        ASSETS / "models/block/dead_spruce_log_horizontal.json",
        {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": "backcountry:block/dead_spruce_log_top", "side": "backcountry:block/dead_spruce_log"},
        },
    )
    write_json(
        ASSETS / "blockstates/dead_spruce_log.json",
        {
            "variants": {
                "axis=x": {"model": "backcountry:block/dead_spruce_log_horizontal", "x": 90, "y": 90},
                "axis=y": {"model": "backcountry:block/dead_spruce_log"},
                "axis=z": {"model": "backcountry:block/dead_spruce_log_horizontal", "x": 90},
            }
        },
    )
    write_json(ASSETS / "models/item/dead_spruce_log.json", {"parent": "backcountry:block/dead_spruce_log"})


def hunting():
    for bow in ("light_hunting_bow", "heavy_hunting_bow"):
        write_json(
            ASSETS / f"models/item/{bow}.json",
            {
                "parent": "minecraft:item/bow",
                "textures": {"layer0": f"backcountry:item/{bow}"},
                "overrides": [
                    {"predicate": {"pulling": 1}, "model": f"backcountry:item/{bow}_pulling_0"},
                    {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"backcountry:item/{bow}_pulling_1"},
                    {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"backcountry:item/{bow}_pulling_2"},
                ],
            },
        )
        for stage in range(3):
            write_json(
                ASSETS / f"models/item/{bow}_pulling_{stage}.json",
                {"parent": "minecraft:item/bow", "textures": {"layer0": f"backcountry:item/{bow}_pulling_{stage}"}},
            )
    write_json(
        ASSETS / "models/block/blood.json",
        {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"blood": "backcountry:block/blood", "particle": "backcountry:block/blood"},
            "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "faces": {"up": {"texture": "#blood"}}}],
        },
    )
    faces = ("down", "east", "north", "south", "up", "west")
    write_json(
        ASSETS / "models/block/carcass.json",
        {
            "parent": "minecraft:block/block",
            "textures": {"fur": "minecraft:block/brown_wool", "particle": "minecraft:block/brown_wool"},
            "elements": [
                {"from": [2, 0, 4], "to": [13, 5, 12], "faces": {face: {"texture": "#fur"} for face in faces}},
                {"from": [13, 1, 6], "to": [16, 4, 10], "faces": {face: {"texture": "#fur"} for face in faces}},
            ],
        },
    )
    for name in ("blood", "carcass"):
        write_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"backcountry:block/{name}"}}})


def traps():
    faces = ("down", "east", "north", "south", "up", "west")

    def cube(start, end, texture):
        return {"from": start, "to": end, "faces": {face: {"texture": texture} for face in faces}}

    posts = [
        cube([3, 0, 7], [4, 7, 8], "#wood"),
        cube([12, 0, 7], [13, 7, 8], "#wood"),
        cube([3, 7, 7], [13, 8, 8], "#wood"),
    ]
    loop = [cube([6, 1, 7.5], [10, 5, 7.5], "#wire")]
    lump = [cube([4, 0, 4], [12, 4, 12], "#fur")]
    shapes = {
        "small_snare": (posts + loop, posts + lump),
        "large_snare": (
            [
                cube([2, 0, 7], [3, 10, 8], "#wood"),
                cube([13, 0, 7], [14, 10, 8], "#wood"),
                cube([2, 10, 7], [14, 11, 8], "#wood"),
                *loop,
            ],
            [cube([2, 0, 7], [3, 10, 8], "#wood"), cube([13, 0, 7], [14, 10, 8], "#wood"), *lump],
        ),
        "deadfall": (
            [
                cube([7, 0, 3], [8, 6, 4], "#wood"),
                {
                    **cube([1, 6, 1], [15, 8, 15], "#stone"),
                    "rotation": {"angle": 22.5, "axis": "x", "origin": [8, 6, 2]},
                },
            ],
            [cube([1, 0, 1], [15, 2, 15], "#stone"), cube([5, 0, 14], [11, 2, 16], "#fur")],
        ),
        "mouse_trap": (
            [cube([5, 0, 3], [11, 1, 13], "#plank"), cube([5, 1, 5], [11, 2, 6], "#wire")],
            [cube([5, 0, 3], [11, 1, 13], "#plank"), cube([5, 1, 10], [11, 2, 11], "#wire")],
        ),
        "gill_net": (
            [cube([0, 0, 7.5], [16, 14, 8.5], "#net")],
            [cube([0, 0, 7.5], [16, 14, 8.5], "#net"), cube([5, 4, 6], [11, 8, 10], "#scales")],
        ),
        "tip_up": (
            [cube([7, 0, 7], [9, 2, 9], "#wood"), cube([7.5, 2, 7.5], [8.5, 6, 8.5], "#wood")],
            [
                cube([7, 0, 7], [9, 2, 9], "#wood"),
                cube([7.5, 2, 7.5], [8.5, 12, 8.5], "#wood"),
                cube([8.5, 9, 7.5], [12, 12, 8.5], "#flag"),
            ],
        ),
    }
    textures = {
        "wood": "minecraft:block/stripped_spruce_log",
        "wire": "minecraft:block/iron_bars",
        "fur": "minecraft:block/brown_wool",
        "stone": "minecraft:block/cobblestone",
        "plank": "minecraft:block/oak_planks",
        "net": "minecraft:block/cobweb",
        "scales": "minecraft:block/light_gray_wool",
        "flag": "minecraft:block/red_wool",
        "particle": "minecraft:block/stripped_spruce_log",
    }
    for name, (set_shape, sprung_shape) in shapes.items():
        for state, elements in (("", set_shape), ("_sprung", sprung_shape)):
            write_json(
                ASSETS / f"models/block/{name}{state}.json",
                {
                    "parent": "minecraft:block/block",
                    "render_type": "minecraft:cutout",
                    "textures": textures,
                    "elements": elements,
                },
            )
        write_json(
            ASSETS / f"blockstates/{name}.json",
            {
                "variants": {
                    "sprung=false": {"model": f"backcountry:block/{name}"},
                    "sprung=true": {"model": f"backcountry:block/{name}_sprung"},
                }
            },
        )
        write_json(ASSETS / f"models/item/{name}.json", {"parent": f"backcountry:block/{name}"})


def sounds():
    target = ASSETS / "sounds/loon.ogg"
    target.parent.mkdir(parents=True, exist_ok=True)
    wail = "0.5*sin(2*PI*(620*t+70*t*t+6*sin(2*PI*5*t)))*min(1,t*4)*min(1,(2.4-t)*2)"
    subprocess.run(
        [
            "ffmpeg",
            "-loglevel",
            "error",
            "-y",
            "-f",
            "lavfi",
            "-i",
            f"aevalsrc='{wail}':s=22050:d=2.4",
            "-ac",
            "1",
            "-c:a",
            "libvorbis",
            "-fflags",
            "+bitexact",
            "-flags:a",
            "+bitexact",
            str(target),
        ],
        check=True,
    )


def lights():
    for light in range(1, BRIGHTEST + 1):
        write_json(
            ASSETS / f"dynamiclights/item/headlamp_{light}.json",
            {
                "match": {"items": "backcountry:headlamp", "components": {"backcountry:light": light}},
                "luminance": light,
            },
        )


def shaders():
    shader = tomllib.loads((SHADERPACKS / "complementary-reimagined.pw.toml").read_text())["filename"]
    patches = tomllib.loads(PATCHER.read_text())["filename"].split("-")[1]
    pack = f"{shader.removesuffix('.zip')} + EuphoriaPatches_{patches}"
    IRIS.write_text(f"disableUpdateMessage=true\nenableShaders=true\nshaderPack={pack}\n")
    for stale in SHADERPACKS.glob("*.txt"):
        stale.unlink()
    sky = tomllib.loads(BALANCE.read_text())["sky"]
    options = tomllib.loads(SHADER_OPTIONS.read_text()) | {
        "CLOUD_ALT1": str(sky["cloud_height"]),
        "CLOUD_ALT2": str(sky["cloud_height"] + CLOUD_LAYERS_APART),
        "MOON_PHASE_DARK": f"{sky['moon_new']:.2f}",
        "MOON_PHASE_FULL": f"{sky['moon_full']:.2f}",
        "MOON_PHASE_PARTIAL": f"{sky['moon_partial']:.2f}",
    }
    (SHADERPACKS / f"{pack}.txt").write_text("".join(f"{key}={value}\n" for key, value in sorted(options.items())))


def resource_packs():
    files = [f"file/{tomllib.loads(meta.read_text())['filename']}" for meta in sorted(RESOURCEPACKS.glob("*.pw.toml"))]
    line = "resourcePacks:" + json.dumps([*BUILT_IN_PACKS, *files], separators=(",", ":"))
    options = [entry for entry in DEFAULT_OPTIONS.read_text().splitlines() if not entry.startswith("resourcePacks:")]
    DEFAULT_OPTIONS.write_text("\n".join(sorted([*options, line], key=lambda entry: entry.split(":", 1)[0])) + "\n")


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
    for name, entry in source["slots"].items():
        png.write(ASSETS / f"textures/item/empty_slot_{name}.png", grid(name, entry, 16))
    for name, entry in source["views"].items():
        rows = entry["rows"]
        png.write(ASSETS / f"textures/misc/{name}.png", grid(name, entry, len(rows[0]), len(rows)))
    for name, entry in source["worn"].items():
        png.write(ASSETS / f"textures/models/armor/{name}_layer_1.png", grid(name, entry, 64, 32))
    with vanilla_jar() as jar, tempfile.TemporaryDirectory() as folder:
        scratch = Path(folder)
        for name, entry in source["tinted"].items():
            kind = entry.get("kind", "item")
            tint(jar, entry["base"], entry["colour"], ASSETS / f"textures/{kind}/{name}.png", scratch)
            if kind == "item":
                item_model(name)
        for entry in source["overrides"].values():
            tint(jar, entry["base"], entry["colour"], OUT / "assets/minecraft" / entry["path"], scratch)
        for name, entry in source["entities"].items():
            tint(jar, entry["base"], entry["colour"], ASSETS / f"textures/entity/{name}.png", scratch)
        for name, entry in source["armour"].items():
            for layer in (1, 2):
                tint(
                    jar,
                    f"models/armor/leather_layer_{layer}",
                    entry["colour"],
                    ASSETS / f"textures/models/armor/{name}_layer_{layer}.png",
                    scratch,
                )
        for name, entry in source["recoloured"].items():
            recolour(
                jar,
                entry["base"],
                entry["colours"],
                ASSETS / f"textures/{entry['kind']}/{name}.png",
                scratch,
                entry.get("frame"),
            )
            if entry["kind"] == "item":
                item_model(name)
        campfire(jar)
    blocks()
    food()
    hunting()
    traps()
    sounds()
    woodpile()
    lights()
    shaders()
    resource_packs()


main()
