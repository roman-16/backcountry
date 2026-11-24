import argparse
import json
import os
import shutil
import signal
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

import drive
import hotswap
import instance
from drive import Failure, running

ROOT = Path(__file__).resolve().parent.parent
SEAT = drive.seat("devserver")
SERVER = SEAT.folder
DRIVER = SERVER / "driver"
PID = DRIVER / "server.pid"
SNAPSHOT = DRIVER / "classes"
LOG = SERVER / "logs/latest.log"
LAUNCH_LOG = SERVER / "launch.log"
LOCK = ROOT / "build/launch.lock"
READY = "]: Done ("
FAILED = ("Failed to start the minecraft server", "Encountered an unexpected exception")
START_SECONDS = 900
STOP_SECONDS = 120
LOGIN, COMMAND = 3, 2


def port():
    return instance.DEV_SERVER_PORT + SEAT.offset()


def rcon_port():
    return instance.RCON_PORT + SEAT.offset()


class Rcon:
    def __init__(self):
        self.socket = socket.create_connection(("127.0.0.1", rcon_port()), timeout=60)
        self.request = 0
        self.send(LOGIN, instance.RCON_PASSWORD)

    def send(self, kind, text):
        self.request += 1
        body = struct.pack("<ii", self.request, kind) + text.encode() + b"\x00\x00"
        self.socket.sendall(struct.pack("<i", len(body)) + body)
        size = struct.unpack("<i", self.read(4))[0]
        answer = self.read(size)
        if struct.unpack("<i", answer[:4])[0] == -1:
            raise Failure("rcon failed: the dev server refused the password")
        return answer[8:-2].decode(errors="replace")

    def read(self, size):
        data = b""
        while len(data) < size:
            chunk = self.socket.recv(size - len(data))
            if not chunk:
                raise Failure("rcon failed: the dev server closed the connection")
            data += chunk
        return data

    def close(self):
        self.socket.close()


def start(arguments):
    if running(PID):
        print(f"the dev server is running on port {port()} ({SERVER.relative_to(ROOT)})")
        return
    drive.room()
    DRIVER.mkdir(parents=True, exist_ok=True)
    PID.unlink(missing_ok=True)
    mode = "lean" if arguments.lean else "full"
    if arguments.fresh:
        shutil.rmtree(SERVER / ("world-lean" if arguments.lean else "world"), ignore_errors=True)
    started = time.monotonic()
    with drive.locked(LOCK):
        subprocess.run(
            ["python3", "scripts/instance.py", "devserver", str(SERVER), "--mode", mode]
            + ["--port", str(port()), "--rcon-port", str(rcon_port())],
            cwd=ROOT,
            check=True,
        )
        LOG.unlink(missing_ok=True)
        environment = dict(os.environ) | {
            "BACKCOUNTRY_GAME_DIRECTORY": str(SERVER),
            "BACKCOUNTRY_SWAP_PORT": str(hotswap.port("server_swap_port") + SEAT.offset()),
        }
        with LAUNCH_LOG.open("w") as log:
            subprocess.Popen(
                ["./gradlew", "--console=plain", "runServer"],
                cwd=ROOT / "mod",
                env=environment,
                stdin=subprocess.DEVNULL,
                stdout=log,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        hotswap.remember(SNAPSHOT)
        deadline = time.monotonic() + START_SECONDS
        while True:
            text = LOG.read_text(errors="replace") if LOG.exists() else ""
            if READY in text:
                break
            failure = next((line for line in text.splitlines() if any(sign in line for sign in FAILED)), None)
            if failure:
                raise Failure(f"start failed: {failure}")
            if time.monotonic() > deadline:
                raise Failure(f"start failed: the dev server did not start within {START_SECONDS} s; see {LAUNCH_LOG}")
            time.sleep(drive.CHECK_SECONDS)
    print(f"the dev server is running on port {port()} (started in {time.monotonic() - started:.0f} s)")


def run(arguments):
    if not running(PID):
        raise Failure("run failed: no dev server runs; start one with `python3 scripts/server.py start`")
    rcon = Rcon()
    try:
        for command in arguments.commands:
            print(json.dumps({"command": command, "reply": rcon.send(COMMAND, command.removeprefix("/"))}))
    finally:
        rcon.close()


def swap(arguments):
    if not running(PID):
        raise Failure("swap failed: no dev server runs")
    print(json.dumps(hotswap.swap(hotswap.port("server_swap_port") + SEAT.offset(), SNAPSHOT)))


def stop(arguments):
    if running(PID):
        try:
            rcon = Rcon()
            rcon.send(COMMAND, "stop")
            rcon.close()
        except (OSError, Failure):
            os.kill(int(PID.read_text()), signal.SIGTERM)
        deadline = time.monotonic() + STOP_SECONDS
        while running(PID) and time.monotonic() < deadline:
            time.sleep(drive.CHECK_SECONDS)
        if running(PID):
            os.kill(int(PID.read_text()), signal.SIGKILL)
    print("the dev server has stopped")


def main():
    parser = argparse.ArgumentParser(description="Drive the dev server: start it, run commands on it, stop it")
    commands = parser.add_subparsers(dest="command", required=True)
    start_parser = commands.add_parser("start", help="start the dev server; returns once it is up")
    start_parser.add_argument("--fresh", action="store_true", help="a new world")
    start_parser.add_argument("--lean", action="store_true", help="only the Backcountry mod and what it needs")
    run_parser = commands.add_parser("run", help="run commands on the dev server and print each reply")
    run_parser.add_argument("commands", nargs="+")
    commands.add_parser("swap", help="compile and swap changed code into the running dev server")
    commands.add_parser("stop", help="stop the dev server; returns once it has exited")
    arguments = parser.parse_args()
    try:
        {"run": run, "start": start, "stop": stop, "swap": swap}[arguments.command](arguments)
    except Failure as failure:
        sys.exit(str(failure))


main()
