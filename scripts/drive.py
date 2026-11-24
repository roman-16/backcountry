import contextlib
import fcntl
import os
import re
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHECK_SECONDS = 0.1
CLAIM = "session"
GAME_OVERHEAD_GIGABYTES = 4
SEAT_PORTS = 10
ANSI = re.compile(r"\x1b\[[0-9;]*m")
REPLY = re.compile(r"\[minecraft/MinecraftServer\]: (.*)$")


class Failure(Exception):
    pass


class Seat:
    def __init__(self, kind, number, folder):
        self.kind = kind
        self.number = number
        self.folder = folder

    def offset(self):
        return SEAT_PORTS * self.number

    def owner(self):
        claim = self.folder / CLAIM
        return claim.read_text().strip() if claim.exists() else None


def seat(kind):
    session = os.environ.get("PI_SESSION_ID")
    if not session:
        return Seat(kind, 0, ROOT / "build" / kind)
    base = ROOT / "build" / f"{kind}s"
    base.mkdir(parents=True, exist_ok=True)
    with locked(base / ".lock"):
        numbered = sorted(
            (int(folder.name), folder) for folder in base.iterdir() if folder.is_dir() and folder.name.isdigit()
        )
        for number, folder in numbered:
            if (folder / CLAIM).exists() and (folder / CLAIM).read_text().strip() == session:
                return Seat(kind, number, folder)
        for number, folder in numbered:
            if not playing(folder):
                (folder / CLAIM).write_text(session)
                return Seat(kind, number, folder)
        number = numbered[-1][0] + 1 if numbered else 1
        folder = base / str(number)
        folder.mkdir()
        (folder / CLAIM).write_text(session)
        return Seat(kind, number, folder)


def seats(kind):
    found = [Seat(kind, 0, ROOT / "build" / kind)]
    base = ROOT / "build" / f"{kind}s"
    if base.exists():
        found += [
            Seat(kind, int(folder.name), folder)
            for folder in sorted(base.iterdir())
            if folder.is_dir() and folder.name.isdigit()
        ]
    return found


def playing(folder):
    return any(running(folder / "driver" / name) for name in ("game.pid", "server.pid"))


@contextlib.contextmanager
def locked(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a") as handle:
        fcntl.flock(handle, fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(handle, fcntl.LOCK_UN)


def room():
    available = next(
        int(line.split()[1])
        for line in Path("/proc/meminfo").read_text().splitlines()
        if line.startswith("MemAvailable:")
    )
    heap = tomllib.loads((ROOT / "balance.toml").read_text())["heap"]["gigabytes"]
    needed = (heap + GAME_OVERHEAD_GIGABYTES) * 1024 * 1024
    if available >= needed:
        return
    others = [
        f"session {other.owner() or 'of your own'}'s {other.kind}"
        for other in seats("client") + seats("devserver")
        if playing(other.folder)
    ]
    raise Failure(
        f"{available // (1024 * 1024)} GB of memory free, a game needs {needed // (1024 * 1024)} GB; "
        f"running: {', '.join(others) or 'nothing of the pack'}"
    )


def running(pid_file):
    if not pid_file.exists():
        return False
    try:
        pid = int(pid_file.read_text())
    except ValueError:
        return False
    try:
        if os.waitpid(pid, os.WNOHANG)[0] == pid:
            return False
    except ChildProcessError:
        pass
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    return True


def crashes(folder):
    return set(folder.glob("*.txt")) if folder.exists() else set()


def reply(line):
    found = REPLY.search(ANSI.sub("", line))
    return found.group(1) if found else None
