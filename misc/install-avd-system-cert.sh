#!/usr/bin/env bash
set -euo pipefail

DEFAULT_CERT="/storage/emulated/0/Download/Reqable"
CERT_SOURCE="${1:-$DEFAULT_CERT}"
ADB_BIN="${ADB:-adb}"
SERIAL="${ANDROID_SERIAL:-}"
REMOTE_STAGING_DIR="/data/local/tmp/avd-system-certs"

die() {
  echo "error: $*" >&2
  exit 1
}

shell_quote() {
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

choose_emulator() {
  local devices
  mapfile -t devices < <("$ADB_BIN" devices | awk '$2 == "device" && $1 ~ /^emulator-/ { print $1 }')

  case "${#devices[@]}" in
    0)
      die "no running AVD found. Start an emulator or set ANDROID_SERIAL=emulator-xxxx"
      ;;
    1)
      printf '%s\n' "${devices[0]}"
      ;;
    *)
      die "multiple AVDs found: ${devices[*]}. Set ANDROID_SERIAL to the target emulator"
      ;;
  esac
}

if [[ -z "$SERIAL" ]]; then
  SERIAL="$(choose_emulator)"
fi

[[ "$SERIAL" == emulator-* ]] || die "refusing to install a system cert on non-AVD device: $SERIAL"

CERT_SOURCE_QUOTED="$(shell_quote "$CERT_SOURCE")"
REMOTE_STAGING_DIR_QUOTED="$(shell_quote "$REMOTE_STAGING_DIR")"

echo "Using AVD: $SERIAL"
echo "Using cert source: $CERT_SOURCE"

if [[ -f "$CERT_SOURCE" ]]; then
  [[ "$CERT_SOURCE" == *.0 ]] || die "local certificate file must end with .0: $CERT_SOURCE"
  "$ADB_BIN" -s "$SERIAL" shell "rm -rf $REMOTE_STAGING_DIR_QUOTED && mkdir -p $REMOTE_STAGING_DIR_QUOTED"
  "$ADB_BIN" -s "$SERIAL" push "$CERT_SOURCE" "$REMOTE_STAGING_DIR/" >/dev/null
elif [[ -d "$CERT_SOURCE" ]]; then
  mapfile -t local_certs < <(find "$CERT_SOURCE" -maxdepth 1 -type f -name "*.0" | sort)
  ((${#local_certs[@]} > 0)) || die "no .0 certificate files found in local directory: $CERT_SOURCE"

  "$ADB_BIN" -s "$SERIAL" shell "rm -rf $REMOTE_STAGING_DIR_QUOTED && mkdir -p $REMOTE_STAGING_DIR_QUOTED"
  for cert in "${local_certs[@]}"; do
    "$ADB_BIN" -s "$SERIAL" push "$cert" "$REMOTE_STAGING_DIR/" >/dev/null
  done
else
  if ! "$ADB_BIN" -s "$SERIAL" shell "test -e $CERT_SOURCE_QUOTED"; then
    echo "Certificate is not readable with current adbd state; restarting adbd without root for shared storage access."
    "$ADB_BIN" -s "$SERIAL" unroot >/dev/null 2>&1 || true
    "$ADB_BIN" -s "$SERIAL" wait-for-device
  fi

  "$ADB_BIN" -s "$SERIAL" shell "SOURCE=$CERT_SOURCE_QUOTED STAGING_DIR=$REMOTE_STAGING_DIR_QUOTED sh -s" <<'EOF'
set -eu

rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

copy_count=0
if [ -d "$SOURCE" ]; then
  for cert in "$SOURCE"/*.0; do
    [ -f "$cert" ] || continue
    cp "$cert" "$STAGING_DIR/$(basename "$cert")"
    copy_count=$((copy_count + 1))
  done
elif [ -f "$SOURCE" ]; then
  case "$SOURCE" in
    *.0)
      cp "$SOURCE" "$STAGING_DIR/$(basename "$SOURCE")"
      copy_count=1
      ;;
    *)
      echo "error: remote certificate file must end with .0: $SOURCE" >&2
      exit 1
      ;;
  esac
else
  echo "error: certificate source not found on AVD: $SOURCE" >&2
  exit 1
fi

if [ "$copy_count" -eq 0 ]; then
  echo "error: no .0 certificate files found in remote source: $SOURCE" >&2
  exit 1
fi

chmod 644 "$STAGING_DIR"/*.0
EOF
fi

root_output="$("$ADB_BIN" -s "$SERIAL" root 2>&1 || true)"
echo "$root_output"
[[ "$root_output" != *"cannot run as root"* ]] || die "target AVD does not allow adb root"

"$ADB_BIN" -s "$SERIAL" wait-for-device
uid="$("$ADB_BIN" -s "$SERIAL" shell id -u | tr -d '\r')"
[[ "$uid" == "0" ]] || die "adb shell is not root after adb root; got uid=$uid"

"$ADB_BIN" -s "$SERIAL" shell "STAGING_DIR=$REMOTE_STAGING_DIR_QUOTED sh -s" <<'EOF'
set -eu

staging_dir="$STAGING_DIR"
cert_dir="/system/etc/security/cacerts"
copy_dir="/data/local/tmp/avd-ca-copy"

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

rm -rf "$copy_dir"
mkdir -m 700 "$copy_dir"
cp "$cert_dir"/* "$copy_dir"/

if ! awk -v target="$cert_dir" '$2 == target { found = 1 } END { exit !found }' /proc/mounts; then
  mount -t tmpfs tmpfs "$cert_dir"
fi

cp "$copy_dir"/* "$cert_dir"/
for cert in "$staging_dir"/*.0; do
  [ -f "$cert" ] || continue
  cp "$cert" "$cert_dir/$(basename "$cert")"
done
chown root:root "$cert_dir"/*
chmod 644 "$cert_dir"/*
chcon u:object_r:system_file:s0 "$cert_dir"/* 2>/dev/null || true
rm -rf "$copy_dir"

if command -v ls >/dev/null 2>&1; then
  for cert in "$staging_dir"/*.0; do
    [ -f "$cert" ] || continue
    cert_name="$(basename "$cert")"
    ls -lZ "$cert_dir/$cert_name" 2>/dev/null || ls -l "$cert_dir/$cert_name"
  done
fi

rm -rf "$staging_dir"
EOF

echo "System certs installed from: $CERT_SOURCE"
