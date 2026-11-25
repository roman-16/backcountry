import argparse
import contextlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SERVER = ROOT / "build/server"
API = "https://api.exaroton.com/v1/"
AGENT = "roman-16/backcountry"
FOLDERS = ("config", "libraries", "mods")
FILES = ("run.bat", "run.sh", "server.jar", "user_jvm_args.txt")
MOTD = "motd"
STATUSES = {
    0: "offline",
    1: "online",
    2: "starting",
    3: "stopping",
    4: "restarting",
    5: "saving",
    6: "loading",
    7: "crashed",
    8: "pending",
    9: "transferring",
    10: "preparing",
}
OFFLINE = 0
READ_BACK = 1 << 20
REQUEST_SECONDS = 30
TRANSFER_SECONDS = 600
RATE_TRIES = 8
TOO_MANY_REQUESTS = 429
WORKERS = 4
SHOWN = 8


class ApiError(Exception):
    pass


class Api:
    def __init__(self, token):
        self.token = token

    def open(self, method, path, body=None, kind=None, timeout=REQUEST_SECONDS):
        headers = {"Authorization": f"Bearer {self.token}", "User-Agent": AGENT}
        if kind:
            headers["Content-Type"] = kind
        request = urllib.request.Request(API + urllib.parse.quote(path), data=body, method=method, headers=headers)
        for attempt in range(RATE_TRIES):
            try:
                with urllib.request.urlopen(request, timeout=timeout) as response:
                    return response.read()
            except urllib.error.HTTPError as error:
                if error.code == TOO_MANY_REQUESTS and attempt < RATE_TRIES - 1:
                    time.sleep(float(error.headers.get("Retry-After") or 2**attempt))
                    continue
                said = error.read().decode("utf-8", "replace")
                with contextlib.suppress(ValueError):
                    said = json.loads(said).get("error") or said
                raise ApiError(f"{method} {path}: HTTP {error.code}: {said}") from None
        raise ApiError(f"{method} {path}: no attempt left")

    def get(self, path):
        answer = json.loads(self.open("GET", path))
        if not answer.get("success"):
            raise ApiError(f"GET {path}: {answer.get('error')}")
        return answer["data"]

    def data(self, path):
        return self.open("GET", path, timeout=TRANSFER_SECONDS)

    def change(self, method, path, body=None, kind=None):
        answer = json.loads(self.open(method, path, body, kind, TRANSFER_SECONDS))
        if not answer.get("success"):
            raise ApiError(f"{method} {path}: {answer.get('error')}")


def say(text):
    print(text, flush=True)


def fail(why):
    sys.exit(f"exaroton failed: {why}")


def megabytes(size):
    return f"{size / 1_000_000:.0f} MB"


def listed(paths):
    shown = ", ".join(paths[:SHOWN])
    return shown + (f" and {len(paths) - SHOWN} more" if len(paths) > SHOWN else "")


def find_server(api, wanted):
    servers = api.get("servers/")
    chosen = [server for server in servers if wanted in (server["id"], server["name"])] if wanted else servers
    if len(chosen) != 1:
        fail(f"name the server, one of: {', '.join(server['name'] for server in servers)}")
    return chosen[0]


def offline(api, server):
    state = api.get(f"servers/{server['id']}")
    if state["status"] != OFFLINE:
        players = (state.get("players") or {}).get("list") or []
        online = f" with {', '.join(players)} online" if players else ""
        fail(
            f"{server['name']} is {STATUSES.get(state['status'], state['status'])}{online}; "
            "the pack goes on only while it is offline"
        )


def remote(api, server, folder, files, folders):
    try:
        children = api.get(f"servers/{server['id']}/files/info/{folder}/").get("children") or []
    except ApiError:
        return
    folders.add(folder)
    for child in children:
        path = child["path"].strip("/")
        if child["isDirectory"]:
            remote(api, server, path, files, folders)
        else:
            files[path] = child["size"]


def stale(api, server, path, size):
    local = SERVER / path
    if size != local.stat().st_size:
        return True
    return size <= READ_BACK and api.data(f"servers/{server['id']}/files/data/{path}") != local.read_bytes()


def properties():
    pairs = {}
    for line in (SERVER / "server.properties").read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            pairs[key] = value.replace("\\:", ":")
    return pairs


def typed(value):
    if value in ("true", "false"):
        return value == "true"
    return int(value) if value.lstrip("-").isdigit() else value


def configure(api, server):
    wanted = properties()
    motd = wanted.pop(MOTD)
    base = f"servers/{server['id']}"
    api.change(
        "POST",
        f"{base}/files/config/server.properties/",
        json.dumps({key: typed(value) for key, value in wanted.items()}).encode(),
        "application/json",
    )
    api.change("POST", f"{base}/options/motd/", json.dumps({MOTD: motd}).encode(), "application/json")
    there = {option["key"]: option["value"] for option in api.get(f"{base}/files/config/server.properties/")}
    kept = [key for key, value in wanted.items() if str(there.get(key)).lower() != value.lower()]
    if kept:
        fail(
            f"{server['name']} kept its own {', '.join(f'{key}={there.get(key)}' for key in kept)}; set them in its options"
        )
    return len(wanted) + 1


def put(api, server):
    if not (SERVER / "server.jar").exists():
        fail("build/server holds no server pack; run just pack")
    offline(api, server)
    started = time.monotonic()
    files, folders = {}, set()
    for folder in FOLDERS:
        remote(api, server, folder, files, folders)
    for child in api.get(f"servers/{server['id']}/files/info/").get("children") or []:
        if child["name"] in FILES and not child["isDirectory"]:
            files[child["name"]] = child["size"]
    local = [
        path.relative_to(SERVER).as_posix()
        for folder in FOLDERS
        for path in (SERVER / folder).rglob("*")
        if path.is_file()
    ]
    local += list(FILES)
    wanted = set(local)
    needed = {parent.as_posix() for path in local for parent in Path(path).parents if parent != Path(".")}
    gone = sorted(folder for folder in folders - needed if Path(folder).parent.as_posix() in needed)
    dropped = gone + sorted(
        path for path in files if path not in wanted and not any(path.startswith(folder + "/") for folder in gone)
    )
    sent = [path for path in local if path not in files or stale(api, server, path, files[path])]
    for folder in sorted(needed - folders, key=lambda name: len(Path(name).parts)):
        api.change("PUT", f"servers/{server['id']}/files/data/{folder}", b"", "inode/directory")

    def send(path):
        api.change(
            "PUT", f"servers/{server['id']}/files/data/{path}", (SERVER / path).read_bytes(), "application/octet-stream"
        )

    def tried(path):
        try:
            send(path)
        except (ApiError, OSError):
            return path
        return None

    with ThreadPoolExecutor(WORKERS) as pool:
        refused = [path for path in pool.map(tried, sent) if path is not None]
    for path in refused:
        send(path)
    for path in dropped:
        api.change("DELETE", f"servers/{server['id']}/files/data/{path}")
    set_count = configure(api, server)
    say(
        f"put the server pack on {server['name']}: {len(sent)} files up ({megabytes(sum((SERVER / path).stat().st_size for path in sent))})"
        + (f": {listed(sent)}" if sent else "")
        + f", {len(dropped)} removed"
        + (f": {listed(dropped)}" if dropped else "")
        + f", {set_count} server properties set, in {time.monotonic() - started:.0f} s"
    )


def main():
    parser = argparse.ArgumentParser(
        description="Put the server pack in build/server on an exaroton server while it is offline: NeoForge, the mods, "
        "the configs and the pack's server properties, uploading only what changed and removing what the pack no "
        "longer holds; the world stays"
    )
    parser.add_argument("server", nargs="?", help="server name or id; the account's only server when omitted")
    arguments = parser.parse_args()
    token = os.environ.get("EXAROTON_API_TOKEN")
    if not token:
        fail("EXAROTON_API_TOKEN is not set; put it in .env, which devbox loads")
    api = Api(token)
    try:
        put(api, find_server(api, arguments.server))
    except ApiError as error:
        fail(str(error))


if __name__ == "__main__":
    main()
