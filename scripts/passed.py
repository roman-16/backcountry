import argparse
import datetime
import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECORD = ROOT / "build/passed.json"
CLIENT_ONLY = ("assets/", "io/github/roman16/backcountry/client/")
INPUTS = (
    "balance.toml",
    "devbox.json",
    "devbox.lock",
    "justfile",
    "mod/src/dev",
    "mod/src/generated/dev",
    "mod/src/main/templates",
    "pack/config",
    "pack/index.toml",
    "pack/mods",
    "scripts/boot.py",
    "scripts/instance.py",
    "scripts/passed.py",
)


def digest(jar):
    found = hashlib.sha256()
    with zipfile.ZipFile(jar) as archive:
        for entry in sorted(archive.infolist(), key=lambda entry: entry.filename):
            if not entry.filename.startswith(CLIENT_ONLY) and not entry.is_dir():
                found.update(f"{entry.filename}\0{entry.CRC}\0".encode())
    for top in INPUTS:
        path = ROOT / top
        for file in sorted(path.rglob("*")) if path.is_dir() else [path] * path.exists():
            if file.is_file():
                found.update(str(file.relative_to(ROOT)).encode() + b"\0" + file.read_bytes())
    return found.hexdigest()


def when(stamp):
    passed = datetime.datetime.fromisoformat(stamp)
    return passed.strftime("%H:%M" if passed.date() == datetime.date.today() else "%d.%m. %H:%M")


def main():
    parser = argparse.ArgumentParser(
        description="Remember what the last passing just test ran on, and say whether anything it loads has changed since"
    )
    parser.add_argument("action", choices=["check", "record"])
    parser.add_argument("jar", type=Path)
    arguments = parser.parse_args()
    current = digest(arguments.jar)
    if arguments.action == "record":
        RECORD.write_text(json.dumps({"at": datetime.datetime.now().isoformat(timespec="seconds"), "digest": current}))
        return
    record = json.loads(RECORD.read_text()) if RECORD.exists() else {}
    if record.get("digest") != current:
        sys.exit(1)
    print(
        f"nothing the server or the game tests load changed since the pass at {when(record['at'])} "
        "(game tests and boot test)"
    )


main()
