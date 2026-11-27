import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tomllib
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BALANCE = ROOT / "balance.toml"
PACK = ROOT / "pack"
DEFAULT_OPTIONS = PACK / "config/defaultoptions/options.txt"
DEFAULT_KEYS = PACK / "config/defaultoptions/keybindings.txt"
SHADER_OPTIONS = "*.txt"
DATA_VERSION = 3955
CACHE = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "backcountry"
IRIS = "iris.properties"
DEV_CONFIGS = {"dynamic_fps.json": {"enabled": False}}
TEST_CONFIGS = {IRIS: {"enableShaders": "false"}}
CLIENT_OPTIONS = {
    "fullscreen": "false",
    "onboardAccessibility": "false",
    "pauseOnLostFocus": "false",
    "renderDistance": "8",
    "simulationDistance": "8",
    "skipMultiplayerWarning": "true",
    "soundCategory_master": "0.5",
}
SILENT_OPTIONS = {"soundCategory_master": "0.0"}
SETTINGS = ("config",)
FAR_VIEW = {"mods/distant-horizons-seedgen.pw.toml", "mods/distanthorizons.pw.toml"}
DEV_CLASSPATH = {"mods/curios.pw.toml", "mods/ecliptic-seasons.pw.toml"}
SIDES = {"client": {"both", "client"}, "server": {"both", "server"}}
DEV_SERVER_PORT = 25566
RCON_PORT = 25567
RCON_PASSWORD = "backcountry"
SERVER_PROPERTIES = {
    "difficulty": "normal",
    "enable-command-block": "false",
    "level-type": "minecraft\\:normal",
    "motd": "Backcountry",
    "spawn-protection": "0",
    "view-distance": "12",
}


def download(url, digest_format, digest, name):
    target = CACHE / "files" / digest
    if not target.exists():
        target.parent.mkdir(parents=True, exist_ok=True)
        request = urllib.request.Request(url, headers={"User-Agent": "roman-16/backcountry"})
        with urllib.request.urlopen(request, timeout=120) as response:
            data = response.read()
        actual = hashlib.new(digest_format, data).hexdigest()
        if actual != digest:
            sys.exit(f"{name}: expected {digest_format} {digest}, got {actual}")
        target.write_bytes(data)
    return target


def source(meta):
    download_spec = meta["download"]
    if download_spec.get("mode") == "metadata:curseforge":
        file_id = meta["update"]["curseforge"]["file-id"]
        name = urllib.request.quote(meta["filename"])
        return f"https://mediafilez.forgecdn.net/files/{file_id // 1000}/{file_id % 1000}/{name}"
    return download_spec["url"]


def link(source, target):
    Path(target).unlink(missing_ok=True)
    try:
        os.link(source, target)
    except OSError:
        shutil.copyfile(source, target)


def files(target, side, folders, left_out=frozenset()):
    index = tomllib.loads((PACK / "index.toml").read_text())
    for entry in index["files"]:
        folder = entry["file"].split("/")[0]
        if not entry.get("metafile") or folder not in folders or entry["file"] in left_out:
            continue
        meta = tomllib.loads((PACK / entry["file"]).read_text())
        if meta["side"] not in SIDES[side]:
            continue
        spec = meta["download"]
        cached = download(source(meta), spec["hash-format"], spec["hash"], meta["name"])
        (target / folder).mkdir(parents=True, exist_ok=True)
        link(cached, target / folder / meta["filename"])


def neoforge(target):
    version = tomllib.loads((PACK / "pack.toml").read_text())["versions"]["neoforge"]
    installed = CACHE / f"neoforge-{version}-server"
    if not (installed / "server.jar").exists():
        base = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar"
        with urllib.request.urlopen(base + ".sha1", timeout=60) as response:
            sha1 = response.read().decode().split()[0]
        installer = download(base, "sha1", sha1, f"NeoForge {version} installer")
        partial = installed.with_name(installed.name + ".partial")
        shutil.rmtree(partial, ignore_errors=True)
        partial.mkdir(parents=True)
        subprocess.run(
            ["java", "-jar", str(installer), "--install-server", ".", "--server-starter"],
            cwd=partial,
            check=True,
            stdout=subprocess.DEVNULL,
        )
        for log in partial.glob("*.log"):
            log.unlink()
        shutil.rmtree(installed, ignore_errors=True)
        partial.rename(installed)
    shutil.copytree(installed, target, copy_function=link, dirs_exist_ok=True)


def options(target, silent):
    path = target / "options.txt"
    lines = path.read_text().splitlines() if path.exists() else []
    shipped = {
        key: value
        for source in (DEFAULT_OPTIONS, DEFAULT_KEYS)
        for key, value in (line.split(":", 1) for line in source.read_text().splitlines() if ":" in line)
    }
    overrides = shipped | CLIENT_OPTIONS | (SILENT_OPTIONS if silent else {})
    kept = [line for line in lines if line.split(":", 1)[0] not in overrides]
    if not any(line.startswith("version:") for line in kept):
        kept.insert(0, f"version:{DATA_VERSION}")
    forced = [f"{key}:{value}" for key, value in sorted(overrides.items())]
    path.write_text("\n".join(kept + forced) + "\n")


def configure(path, overrides):
    text = path.read_text() if path.exists() else ""
    if path.suffix == ".json":
        path.write_text(json.dumps(json.loads(text or "{}") | overrides, indent=2) + "\n")
    else:
        pairs = dict(line.split("=", 1) for line in text.splitlines() if "=" in line and not line.startswith("#"))
        path.write_text("".join(f"{key}={value}\n" for key, value in sorted((pairs | overrides).items())))


def settings(target):
    for folder in SETTINGS:
        if (PACK / folder).exists():
            shutil.copytree(PACK / folder, target / folder, dirs_exist_ok=True)


def client(target, mode, silent):
    shutil.rmtree(target / "mods", ignore_errors=True)
    (target / "mods").mkdir(parents=True)
    files(
        target,
        "client",
        {"resourcepacks", "shaderpacks"} if mode == "lean" else {"mods", "resourcepacks", "shaderpacks"},
        DEV_CLASSPATH | (frozenset() if mode == "shipped" else FAR_VIEW),
    )
    settings(target)
    for options_file in (PACK / "shaderpacks").glob(SHADER_OPTIONS):
        (target / "shaderpacks").mkdir(parents=True, exist_ok=True)
        shutil.copyfile(options_file, target / "shaderpacks" / options_file.name)
    for name, overrides in (DEV_CONFIGS if mode == "shipped" else DEV_CONFIGS | TEST_CONFIGS).items():
        (target / "config").mkdir(parents=True, exist_ok=True)
        configure(target / "config" / name, overrides)
    options(target, silent)


def properties(target, overrides):
    lines = [f"{key}={value}" for key, value in sorted((SERVER_PROPERTIES | overrides).items())]
    (target / "server.properties").write_text("\n".join(lines) + "\n")


def server(target, jar):
    shutil.rmtree(target, ignore_errors=True)
    (target / "mods").mkdir(parents=True)
    neoforge(target)
    files(target, "server", {"mods"})
    shutil.copyfile(jar, target / "mods" / jar.name)
    settings(target)
    properties(target, {})
    (target / "user_jvm_args.txt").unlink()
    (target / "user_jvm_args.txt").write_text(f"-Xmx{tomllib.loads(BALANCE.read_text())['heap']['gigabytes']}G\n")


def dev_server(target, mode, port=DEV_SERVER_PORT, rcon_port=RCON_PORT):
    shutil.rmtree(target / "mods", ignore_errors=True)
    (target / "mods").mkdir(parents=True)
    if mode != "lean":
        files(target, "server", {"mods"}, FAR_VIEW | DEV_CLASSPATH)
    settings(target)
    world = {"level-name": "world-lean"} if mode == "lean" else {}
    if os.environ.get("BACKCOUNTRY_SEED"):
        world["level-seed"] = os.environ["BACKCOUNTRY_SEED"]
    properties(
        target,
        {
            "enable-rcon": "true",
            "max-tick-time": "-1",
            "online-mode": "false",
            "rcon.password": RCON_PASSWORD,
            "rcon.port": rcon_port,
            "server-port": port,
        }
        | world,
    )
    (target / "eula.txt").write_text("eula=true\n")


def game_test(target):
    shutil.rmtree(target / "mods", ignore_errors=True)
    (target / "mods").mkdir(parents=True)
    files(target, "server", {"mods"}, FAR_VIEW | DEV_CLASSPATH)
    settings(target)
    properties(target, {})


def main():
    parser = argparse.ArgumentParser(description="Assemble the pack as a dev client instance or a dedicated server")
    sides = parser.add_subparsers(dest="side", required=True)
    client_parser = sides.add_parser(
        "client",
        help="mods, packs and configs for the dev client; keeps saves and options",
    )
    client_parser.add_argument("target", type=Path)
    client_parser.add_argument(
        "--mode",
        choices=["full", "lean", "shipped"],
        default="full",
        help="lean loads only the Backcountry mod; shipped keeps the far view and the shader as the pack ships them",
    )
    client_parser.add_argument("--silent", action="store_true", help="mute the game, for a client nobody listens to")
    devserver_parser = sides.add_parser(
        "devserver",
        help="mods, configs and properties for the dev server, which brings the Backcountry mod itself",
    )
    devserver_parser.add_argument("target", type=Path)
    devserver_parser.add_argument("--mode", choices=["full", "lean"], default="full")
    devserver_parser.add_argument("--port", type=int, default=DEV_SERVER_PORT)
    devserver_parser.add_argument("--rcon-port", type=int, default=RCON_PORT)
    gametest_parser = sides.add_parser("gametest", help="the pack's server mods and configs for the game test server")
    gametest_parser.add_argument("target", type=Path)
    server_parser = sides.add_parser("server", help="a complete dedicated server, rebuilt from nothing")
    server_parser.add_argument("target", type=Path)
    server_parser.add_argument("jar", type=Path)
    arguments = parser.parse_args()
    if arguments.side == "client":
        client(arguments.target.resolve(), arguments.mode, arguments.silent)
    elif arguments.side == "devserver":
        dev_server(arguments.target.resolve(), arguments.mode, arguments.port, arguments.rcon_port)
    elif arguments.side == "gametest":
        game_test(arguments.target.resolve())
    else:
        server(arguments.target.resolve(), arguments.jar.resolve())


if __name__ == "__main__":
    main()
