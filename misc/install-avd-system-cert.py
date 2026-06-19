#!/usr/bin/env python3
import os
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Optional


DEFAULT_CERT = "/storage/emulated/0/Download/Reqable"
REMOTE_STAGING_DIR = "/data/local/tmp/avd-system-certs"
CERT_DIR = "/system/etc/security/cacerts"


class InstallError(Exception):
    pass


def die(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(1)


def adb_prefix(adb_bin: str, serial: Optional[str] = None) -> list[str]:
    command = [adb_bin]
    if serial:
        command.extend(["-s", serial])
    return command


def run(
    command: list[str],
    *,
    check: bool = True,
    text: str | None = None,
    quiet: bool = False,
) -> subprocess.CompletedProcess[str]:
    proc = subprocess.run(
        command,
        input=text,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if proc.stdout and not quiet:
        print(proc.stdout, end="" if proc.stdout.endswith("\n") else "\n")
    if check and proc.returncode != 0:
        raise InstallError(
            f"command failed ({proc.returncode}): {shlex.join(command)}\n{proc.stdout}".rstrip(),
        )
    return proc


def adb(
    adb_bin: str,
    serial: str,
    args: list[str],
    *,
    check: bool = True,
    text: str | None = None,
    quiet: bool = False,
) -> subprocess.CompletedProcess[str]:
    return run(adb_prefix(adb_bin, serial) + args, check=check, text=text, quiet=quiet)


def choose_emulator(adb_bin: str) -> str:
    proc = run(adb_prefix(adb_bin) + ["devices"], quiet=True)
    devices: list[str] = []
    for line in proc.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device" and parts[0].startswith("emulator-"):
            devices.append(parts[0])

    if not devices:
        die("no running AVD found. Start an emulator or set ANDROID_SERIAL=emulator-xxxx")
    if len(devices) > 1:
        die(f"multiple AVDs found: {' '.join(devices)}. Set ANDROID_SERIAL to the target emulator")
    return devices[0]


def shell(adb_bin: str, serial: str, script: str, *, args: Optional[list[str]] = None) -> subprocess.CompletedProcess[str]:
    shell_args = ["shell", "sh", "-s"]
    if args:
        shell_args.extend(args)
    return adb(adb_bin, serial, shell_args, text=script)


def reset_staging(adb_bin: str, serial: str) -> None:
    shell(
        adb_bin,
        serial,
        """
set -eu
rm -rf "$1"
mkdir -p "$1"
""",
        args=[REMOTE_STAGING_DIR],
    )


def stage_local_cert(adb_bin: str, serial: str, cert: Path) -> None:
    if cert.suffix != ".0":
        die(f"local certificate file must end with .0: {cert}")
    reset_staging(adb_bin, serial)
    adb(adb_bin, serial, ["push", str(cert), f"{REMOTE_STAGING_DIR}/"], quiet=True)


def stage_local_dir(adb_bin: str, serial: str, cert_dir: Path) -> None:
    certs = sorted(path for path in cert_dir.iterdir() if path.is_file() and path.name.endswith(".0"))
    if not certs:
        die(f"no .0 certificate files found in local directory: {cert_dir}")

    reset_staging(adb_bin, serial)
    for cert in certs:
        adb(adb_bin, serial, ["push", str(cert), f"{REMOTE_STAGING_DIR}/"], quiet=True)


def stage_remote_source(adb_bin: str, serial: str, source: str) -> None:
    exists = adb(adb_bin, serial, ["shell", "test", "-e", source], check=False, quiet=True).returncode == 0
    if not exists:
        print("Certificate is not readable with current adbd state; restarting adbd without root for shared storage access.")
        adb(adb_bin, serial, ["unroot"], check=False, quiet=True)
        adb(adb_bin, serial, ["wait-for-device"], quiet=True)

    shell(
        adb_bin,
        serial,
        """
set -eu

source="$1"
staging_dir="$2"

rm -rf "$staging_dir"
mkdir -p "$staging_dir"

copy_count=0
if [ -d "$source" ]; then
  for cert in "$source"/*.0; do
    [ -f "$cert" ] || continue
    cp "$cert" "$staging_dir/$(basename "$cert")"
    copy_count=$((copy_count + 1))
  done
elif [ -f "$source" ]; then
  case "$source" in
    *.0)
      cp "$source" "$staging_dir/$(basename "$source")"
      copy_count=1
      ;;
    *)
      echo "error: remote certificate file must end with .0: $source" >&2
      exit 1
      ;;
  esac
else
  echo "error: certificate source not found on AVD: $source" >&2
  exit 1
fi

if [ "$copy_count" -eq 0 ]; then
  echo "error: no .0 certificate files found in remote source: $source" >&2
  exit 1
fi

chmod 644 "$staging_dir"/*.0
""",
        args=[source, REMOTE_STAGING_DIR],
    )


def stage_certs(adb_bin: str, serial: str, source: str) -> None:
    local_source = Path(source)
    if local_source.is_file():
        stage_local_cert(adb_bin, serial, local_source)
    elif local_source.is_dir():
        stage_local_dir(adb_bin, serial, local_source)
    else:
        stage_remote_source(adb_bin, serial, source)


def ensure_root(adb_bin: str, serial: str) -> None:
    root = adb(adb_bin, serial, ["root"], check=False)
    if "cannot run as root" in root.stdout:
        die("target AVD does not allow adb root")
    adb(adb_bin, serial, ["wait-for-device"], quiet=True)

    uid = adb(adb_bin, serial, ["shell", "id", "-u"], quiet=True).stdout.strip().replace("\r", "")
    if uid != "0":
        die(f"adb shell is not root after adb root; got uid={uid}")


def try_remount(adb_bin: str, serial: str) -> str:
    remount = adb(adb_bin, serial, ["remount"], check=False)
    adb(adb_bin, serial, ["wait-for-device"], quiet=True)
    return remount.stdout


def install_staged_certs(adb_bin: str, serial: str) -> str:
    proc = shell(
        adb_bin,
        serial,
        """
set -eu

staging_dir="$1"
cert_dir="$2"
copy_dir="/data/local/tmp/avd-ca-copy"
install_mode="temporary"
persistent_error=""

if [ "$(id -u)" != "0" ]; then
  echo "error: adb shell is not root" >&2
  exit 1
fi

test -d "$staging_dir"
test -d "$cert_dir"

install_count=0
for cert in "$staging_dir"/*.0; do
  [ -f "$cert" ] || continue
  install_count=$((install_count + 1))
done

if [ "$install_count" -eq 0 ]; then
  echo "error: no staged .0 certificate files found in $staging_dir" >&2
  exit 1
fi

if awk -v target="$cert_dir" '$2 == target && $3 == "tmpfs" { found = 1 } END { exit !found }' /proc/mounts; then
  umount "$cert_dir" || true
fi

probe="$cert_dir/.avd-cert-write-test"
if awk -v target="$cert_dir" '$2 == target && $3 == "tmpfs" { found = 1 } END { exit !found }' /proc/mounts; then
  persistent_error="previous tmpfs mount on $cert_dir could not be removed"
elif (: > "$probe" && rm -f "$probe") 2>/data/local/tmp/avd-cert-write-error; then
  for cert in "$staging_dir"/*.0; do
    [ -f "$cert" ] || continue
    cp "$cert" "$cert_dir/$(basename "$cert")"
  done
  install_mode="persistent"
else
  persistent_error="$(cat /data/local/tmp/avd-cert-write-error 2>/dev/null || true)"

  rm -rf "$copy_dir"
  mkdir -m 700 "$copy_dir"
  for cert in "$cert_dir"/*; do
    [ -f "$cert" ] || continue
    cp "$cert" "$copy_dir/"
  done

  mount -t tmpfs tmpfs "$cert_dir"

  for cert in "$copy_dir"/*; do
    [ -f "$cert" ] || continue
    cp "$cert" "$cert_dir/"
  done
  for cert in "$staging_dir"/*.0; do
    [ -f "$cert" ] || continue
    cp "$cert" "$cert_dir/$(basename "$cert")"
  done
  rm -rf "$copy_dir"
fi

chown root:root "$cert_dir"/*
chmod 644 "$cert_dir"/*
chcon u:object_r:system_file:s0 "$cert_dir"/* 2>/dev/null || true

for cert in "$staging_dir"/*.0; do
  [ -f "$cert" ] || continue
  cert_name="$(basename "$cert")"
  ls -lZ "$cert_dir/$cert_name" 2>/dev/null || ls -l "$cert_dir/$cert_name"
done

rm -rf "$staging_dir" /data/local/tmp/avd-cert-write-error

echo "INSTALL_MODE=$install_mode"
if [ -n "$persistent_error" ]; then
  echo "PERSISTENT_ERROR=$persistent_error"
fi
""",
        args=[REMOTE_STAGING_DIR, CERT_DIR],
    )

    mode = "temporary"
    for line in proc.stdout.splitlines():
        if line.startswith("INSTALL_MODE="):
            mode = line.split("=", 1)[1]
    return mode


def main() -> None:
    cert_source = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CERT
    adb_bin = os.environ.get("ADB", "adb")
    serial = os.environ.get("ANDROID_SERIAL") or choose_emulator(adb_bin)

    if not serial.startswith("emulator-"):
        die(f"refusing to install a system cert on non-AVD device: {serial}")

    print(f"Using AVD: {serial}")
    print(f"Using cert source: {cert_source}")

    try:
        stage_certs(adb_bin, serial, cert_source)
        ensure_root(adb_bin, serial)
        remount_output = try_remount(adb_bin, serial)
        if remount_output.strip():
            print("adb remount output:")
            print(remount_output, end="" if remount_output.endswith("\n") else "\n")

        mode = install_staged_certs(adb_bin, serial)
    except InstallError as exc:
        die(str(exc))

    print(f"System certs installed from: {cert_source}")
    if mode == "persistent":
        print("Install mode: persistent. The certificates were written to the AVD system certificate directory.")
    else:
        print("Install mode: temporary. The certificates are mounted with tmpfs and will be lost after reboot.")
        print("For persistent install, start the AVD with a writable system image, then run this script again.")


if __name__ == "__main__":
    main()
