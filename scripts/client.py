import argparse
import json
import os
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

import drive
import hotswap
from drive import Failure, crashes, running

ROOT = Path(__file__).resolve().parent.parent
SEAT = drive.seat("client")
CLIENT = SEAT.folder
DRIVER = CLIENT / "driver"
GAME = DRIVER / "game.pid"
LAUNCHED = DRIVER / "launched.json"
SCREEN = DRIVER / "screen.pid"
SCREEN_DISPLAY = DRIVER / "screen-display"
SCREEN_LOG = DRIVER / "screen.log"
SNAPSHOT = DRIVER / "classes"
LAUNCH_LOG = CLIENT / "launch.log"
CRASHES = CLIENT / "crash-reports"
LOCK = ROOT / "build/launch.lock"
SCREEN_SIZE = ("1920", "1080")
SCREEN_SECONDS = 20
START_SECONDS = 600
RUN_SECONDS = 300
STOP_SECONDS = 120
FRESH = "play"
PANORAMA = ROOT / "mod/src/main/resources/assets/minecraft/textures/gui/title/background"
PANORAMA_FACES = 6
PANORAMA_SEED = "1"
PANORAMA_TIMES = {"dawn": 23400, "dusk": 12600, "night": 18000, "noon": 6000}
PANORAMA_SECONDS = 1800


def screen(visible):
    environment = dict(os.environ)
    if visible:
        return environment
    if shutil.which("gamescope") is None:
        raise Failure(
            "the hidden screen needs gamescope from the system (programs.gamescope.enable); run with --visible"
        )
    SCREEN_DISPLAY.unlink(missing_ok=True)
    width, height = SCREEN_SIZE
    announce = (
        f'printf %s "$DISPLAY" > {SCREEN_DISPLAY}.tmp && mv {SCREEN_DISPLAY}.tmp {SCREEN_DISPLAY}; exec sleep infinity'
    )
    alone = {key: value for key, value in environment.items() if key not in ("DISPLAY", "WAYLAND_DISPLAY")}
    with SCREEN_LOG.open("w") as log:
        process = subprocess.Popen(
            ["gamescope", "--backend", "headless", "-W", width, "-H", height, "-w", width, "-h", height]
            + ["--", "sh", "-c", announce],
            env=alone,
            stdin=subprocess.DEVNULL,
            stdout=log,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    SCREEN.write_text(str(process.pid))
    deadline = time.monotonic() + SCREEN_SECONDS
    while not SCREEN_DISPLAY.exists() and process.poll() is None and time.monotonic() < deadline:
        time.sleep(drive.CHECK_SECONDS)
    if not SCREEN_DISPLAY.exists():
        release_screen()
        raise Failure("the hidden screen (gamescope) did not start; run with --visible to use the user's screen")
    environment.pop("WAYLAND_DISPLAY", None)
    return environment | {"DISPLAY": SCREEN_DISPLAY.read_text(), "XDG_SESSION_TYPE": "x11"}


def release_screen():
    if running(SCREEN):
        # gamescope segfaults and dumps core when asked to stop, so the hidden screen is killed outright.
        os.kill(int(SCREEN.read_text()), signal.SIGKILL)
        deadline = time.monotonic() + STOP_SECONDS
        while running(SCREEN) and time.monotonic() < deadline:
            time.sleep(drive.CHECK_SECONDS)
    for screen_file in (SCREEN, SCREEN_DISPLAY):
        screen_file.unlink(missing_ok=True)


def wanted(arguments):
    world = FRESH if arguments.fresh or arguments.seed else arguments.world or ""
    return {"mode": arguments.mode, "seed": arguments.seed or "", "visible": arguments.visible, "world": world}


def start(arguments):
    wish = wanted(arguments)
    if running(GAME) and LAUNCHED.exists() and json.loads(LAUNCHED.read_text()) == wish and not arguments.fresh:
        print(f"the client is running in {wish['world'] or 'the title screen'} ({CLIENT.relative_to(ROOT)})")
        return
    if running(GAME):
        stop(None)
    drive.room()
    DRIVER.mkdir(parents=True, exist_ok=True)
    for stale in ("ready.json", "failed.txt", "done.txt", "commands.txt", "quit", "game.pid", "screen.txt"):
        (DRIVER / stale).unlink(missing_ok=True)
    if wish["world"] == FRESH:
        shutil.rmtree(CLIENT / "saves" / FRESH, ignore_errors=True)
    before = crashes(CRASHES)
    started = time.monotonic()
    with drive.locked(LOCK):
        subprocess.run(
            ["python3", "scripts/instance.py", "client", str(CLIENT), "--mode", arguments.mode], cwd=ROOT, check=True
        )
        environment = screen(arguments.visible) | {
            "BACKCOUNTRY_GAME_DIRECTORY": str(CLIENT),
            "BACKCOUNTRY_SEED": wish["seed"],
            "BACKCOUNTRY_SWAP_PORT": str(hotswap.port("client_swap_port") + SEAT.offset()),
            "BACKCOUNTRY_WORLD": wish["world"],
            "LD_LIBRARY_PATH": f"{os.environ.get('DEVBOX_PACKAGES_DIR', '')}/lib:/run/opengl-driver/lib",
        }
        with LAUNCH_LOG.open("w") as log:
            subprocess.Popen(
                ["./gradlew", "--console=plain", "runPlay"],
                cwd=ROOT / "mod",
                env=environment,
                stdin=subprocess.DEVNULL,
                stdout=log,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        hotswap.remember(SNAPSHOT)
        LAUNCHED.write_text(json.dumps(wish))
        deadline = time.monotonic() + START_SECONDS
        while not (DRIVER / "ready.json").exists():
            if (DRIVER / "failed.txt").exists():
                raise Failure((DRIVER / "failed.txt").read_text())
            if crashes(CRASHES) - before:
                raise Failure(f"start failed: the game crashed; see {sorted(crashes(CRASHES) - before)[-1]}")
            if time.monotonic() > deadline:
                standing = DRIVER / "screen.txt"
                shown = f", standing at {standing.read_text()}" if standing.exists() else ""
                raise Failure(f"start failed: the game did not open within {START_SECONDS} s{shown}; see {LAUNCH_LOG}")
            if GAME.exists() and not running(GAME):
                raise Failure(f"start failed: the game exited; see {LAUNCH_LOG}")
            time.sleep(drive.CHECK_SECONDS)
    ready = json.loads((DRIVER / "ready.json").read_text())
    where = "the title screen" if ready["world"] == "title" else ready["world"]
    hidden = "the user's screen" if arguments.visible else "a hidden screen"
    print(f"the client is running in {where} on {hidden} (opened in {time.monotonic() - started:.0f} s)")


def run(arguments):
    if not running(GAME):
        raise Failure("run failed: no client runs; start one with `python3 scripts/client.py start`")
    steps = arguments.steps or [line for line in sys.stdin.read().splitlines() if line.strip()]
    (DRIVER / "done.txt").unlink(missing_ok=True)
    (DRIVER / "commands.txt").write_text("\n".join(steps) + "\n")
    deadline = time.monotonic() + arguments.timeout
    while not (DRIVER / "done.txt").exists():
        if not running(GAME):
            raise Failure(f"run failed: the game exited; see {LAUNCH_LOG}")
        if time.monotonic() > deadline:
            raise Failure(f"run failed: the batch did not finish within {arguments.timeout} s")
        time.sleep(drive.CHECK_SECONDS)
    failed = False
    for line in (DRIVER / "done.txt").read_text().splitlines():
        print(line)
        failed |= '"error"' in line
    if failed:
        sys.exit(1)


def swap(arguments):
    if not running(GAME):
        raise Failure("swap failed: no client runs")
    print(json.dumps(hotswap.swap(hotswap.port("client_swap_port") + SEAT.offset(), SNAPSHOT)))


def stop(arguments):
    if running(GAME):
        (DRIVER / "quit").write_text("")
        deadline = time.monotonic() + STOP_SECONDS
        while running(GAME) and time.monotonic() < deadline:
            time.sleep(drive.CHECK_SECONDS)
        if running(GAME):
            os.kill(int(GAME.read_text()), signal.SIGKILL)
    release_screen()
    LAUNCHED.unlink(missing_ok=True)
    if arguments is not None:
        print("the client has stopped")


def checkpoint(arguments):
    world = json.loads(LAUNCHED.read_text())["world"] if running(GAME) and LAUNCHED.exists() else ""
    if not world:
        raise Failure("checkpoint failed: no world is open")
    arguments.steps = ["/save-all flush"]
    arguments.timeout = RUN_SECONDS
    run(arguments)
    target = CLIENT / "checkpoints" / arguments.name
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(CLIENT / "saves" / world, target, ignore=shutil.ignore_patterns("session.lock"))
    print(f"checkpoint {arguments.name} keeps {world} ({target.relative_to(ROOT)})")


def restore(arguments):
    source = CLIENT / "checkpoints" / arguments.name
    if not source.exists():
        raise Failure(f"restore failed: no checkpoint {arguments.name}")
    if running(GAME):
        stop(None)
    target = CLIENT / "saves" / arguments.name
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(source, target)
    arguments.fresh = False
    arguments.seed = None
    arguments.world = arguments.name
    start(arguments)


def panorama(arguments):
    arguments.fresh = False
    arguments.mode = "shipped"
    arguments.seed = PANORAMA_SEED
    arguments.world = None
    start(arguments)
    steps = ["graphics ultra shaders on", "gui off", "/gamemode spectator", "/backcountry weather clear", "far"]
    for name, daytime in sorted(PANORAMA_TIMES.items()):
        steps += [
            "/tick unfreeze",
            f"/time set {daytime}",
            "/backcountry weather clear",
            "/tick freeze",
            f"panorama {name}",
        ]
    steps += ["/tick unfreeze", "/gamemode survival", "gui on"]
    arguments.steps = steps
    arguments.timeout = PANORAMA_SECONDS
    run(arguments)
    shutil.rmtree(PANORAMA, ignore_errors=True)
    for name in sorted(PANORAMA_TIMES):
        (PANORAMA / name).mkdir(parents=True)
        for face in range(PANORAMA_FACES):
            shot = CLIENT / "screenshots" / "panorama" / name / f"panorama_{face}.png"
            subprocess.run(
                [
                    "magick",
                    str(shot),
                    "-dither",
                    "FloydSteinberg",
                    "-colors",
                    "256",
                    f"PNG8:{PANORAMA / name / f'panorama_{face}.png'}",
                ],
                check=True,
            )
    print(f"the title's panoramas ({', '.join(sorted(PANORAMA_TIMES))}) are in {PANORAMA.relative_to(ROOT)}")


def main():
    parser = argparse.ArgumentParser(description="Drive the dev client: start it, run steps in it, stop it")
    commands = parser.add_subparsers(dest="command", required=True)
    start_parser = commands.add_parser("start", help="open the client in a world; returns once the world is open")
    start_parser.add_argument("--fresh", action="store_true", help="a fresh natural world, played in survival")
    start_parser.add_argument("--seed", help="a fresh natural world grown from this seed")
    start_parser.add_argument("--world", help="a save by its folder name")
    start_parser.add_argument("--mode", choices=["full", "lean", "shipped"], default="full")
    start_parser.add_argument(
        "--visible", action="store_true", help="draw on the user's screen instead of a hidden one"
    )
    run_parser = commands.add_parser("run", help="run steps in the running client, one line of JSON each")
    run_parser.add_argument("steps", nargs="*")
    run_parser.add_argument("--timeout", type=int, default=RUN_SECONDS)
    commands.add_parser("swap", help="compile and swap changed code into the running client")
    checkpoint_parser = commands.add_parser("checkpoint", help="save the open world and keep a copy of it under a name")
    checkpoint_parser.add_argument("name")
    restore_parser = commands.add_parser("restore", help="open a kept copy of a world as the save of the same name")
    restore_parser.add_argument("name")
    restore_parser.add_argument("--mode", choices=["full", "lean", "shipped"], default="full")
    restore_parser.add_argument(
        "--visible", action="store_true", help="draw on the user's screen instead of a hidden one"
    )
    panorama_parser = commands.add_parser(
        "panorama",
        help="grow the panorama's world from its seed and photograph the title's panoramas at the drop-off at each time of day, "
        "with the shader at Ultra and the far view drawn",
    )
    panorama_parser.add_argument(
        "--visible", action="store_true", help="draw on the user's screen instead of a hidden one"
    )
    commands.add_parser("stop", help="quit the client; returns once it has exited")
    arguments = parser.parse_args()
    try:
        {
            "checkpoint": checkpoint,
            "panorama": panorama,
            "restore": restore,
            "run": run,
            "start": start,
            "stop": stop,
            "swap": swap,
        }[arguments.command](arguments)
    except Failure as failure:
        sys.exit(str(failure))


main()
