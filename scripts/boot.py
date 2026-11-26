import argparse
import io
import json
import queue
import re
import subprocess
import sys
import threading
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIXINS = ROOT / "mod/src/main/resources/backcountry.mixins.json"
MIXIN_SOURCES = ROOT / "mod/src/main/java/io/github/roman16/backcountry/mixin"
TARGETS = re.compile(r'@Mixin\((?:[^)]*?targets\s*=\s*"([^"]+)"|(?:value\s*=\s*)?([\w.]+)\.class)')
CLIENT = b"net/minecraft/client/"

START_SECONDS = 900
STOP_SECONDS = 300
DONE = re.compile(r"\]: Done \([0-9.]+s\)!")
FAILED = re.compile(r"Failed to start the minecraft server|Mod loading has failed|Encountered an unexpected exception")
BROKEN = re.compile(
    r"\[(?:main|Server thread|modloading-worker-\d+)/(?:ERROR|FATAL)\].*(?:Failed|failed|Exception|Couldn't)"
)


def classes_of(jar, into):
    for name in jar.namelist():
        if name.endswith(".class"):
            into.setdefault(name[: -len(".class")].replace("/", "."), lambda jar=jar, name=name: jar.read(name))
        elif name.endswith(".jar"):
            classes_of(zipfile.ZipFile(io.BytesIO(jar.read(name))), into)


def target_of(mixin):
    source = (MIXIN_SOURCES / f"{mixin}.java").read_text()
    found = TARGETS.search(source)
    if not found:
        return None
    if found.group(1):
        return found.group(1).replace("$", ".")
    simple = found.group(2)
    imported = re.search(rf"^import ([\w.]+\.{re.escape(simple.split('.')[0])});", source, re.MULTILINE)
    return imported.group(1) + simple[len(simple.split(".")[0]) :] if imported else None


def client_mixins(folder):
    classes = {}
    for jar in sorted((folder / "mods").glob("*.jar")):
        classes_of(zipfile.ZipFile(jar), classes)
    named = {name.replace("$", "."): read for name, read in classes.items()}
    wrong = []
    for mixin in json.loads(MIXINS.read_text())["mixins"]:
        target = target_of(mixin)
        if target and not target.startswith("net.minecraft.") and target in named and CLIENT in named[target]():
            wrong.append(f"{mixin} targets {target}, which names client classes: move it to the client mixins")
    if wrong:
        sys.exit("a common mixin would crash the dedicated server:\n" + "\n".join(wrong))


def lines_of(process, into):
    for line in process.stdout:
        into.put(line.rstrip("\n"))
    into.put(None)


def boot(folder):
    client_mixins(folder)
    (folder / "eula.txt").write_text("eula=true\n")
    process = subprocess.Popen(
        ["bash", "run.sh", "--nogui"],
        cwd=folder,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    said = queue.Queue()
    threading.Thread(target=lines_of, args=(process, said), daemon=True).start()
    problems = []
    started = time.monotonic()
    while True:
        try:
            line = said.get(timeout=max(1, START_SECONDS - (time.monotonic() - started)))
        except queue.Empty:
            process.kill()
            sys.exit(f"the server did not start within {START_SECONDS} s")
        if line is None:
            sys.exit("the server exited before it started:\n" + "\n".join(problems[-32:]))
        if BROKEN.search(line):
            problems.append(line)
        if FAILED.search(line):
            process.kill()
            sys.exit("the server failed to start:\n" + "\n".join(problems[-32:] + [line]))
        if DONE.search(line):
            break
    seconds = time.monotonic() - started
    process.stdin.write("stop\n")
    process.stdin.flush()
    try:
        process.wait(timeout=STOP_SECONDS)
    except subprocess.TimeoutExpired:
        process.kill()
        sys.exit(f"the server did not stop within {STOP_SECONDS} s")
    if problems:
        sys.exit(f"the server started in {seconds:.0f} s, but logged {len(problems)} problems:\n" + "\n".join(problems))
    print(f"the server started in {seconds:.0f} s and stopped cleanly")


def main():
    parser = argparse.ArgumentParser(description="Boot a server pack headless and stop it once it is up")
    parser.add_argument("folder", type=Path)
    boot(parser.parse_args().folder.resolve())


if __name__ == "__main__":
    main()
