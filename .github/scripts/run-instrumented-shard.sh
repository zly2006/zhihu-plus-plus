#!/usr/bin/env bash

set -uo pipefail

if [[ $# -ne 2 || ! $1 =~ ^[1-9][0-9]*$ || ! $2 =~ ^[0-9]+$ || $2 -ge $1 ]]; then
    echo "usage: $0 <shard-count> <zero-based-shard-index>" >&2
    exit 2
fi

readonly shard_count=$1
readonly shard_index=$2
readonly device_serial=${ANDROID_SERIAL:-emulator-5554}
readonly results_dir=app/build/outputs/androidTest-results

mkdir -p "$results_dir"
adb -s "$device_serial" logcat -c
adb -s "$device_serial" logcat -v time -s ZHPP_TEST:I &
logcat_pid=$!

capture_diagnostics() {
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    adb -s "$device_serial" logcat -d > "$results_dir/logcat-shard-$shard_index.txt" || true
}
trap capture_diagnostics EXIT

ANDROID_SERIAL="$device_serial" ./gradlew connectedLiteDebugAndroidTest \
    --console=plain \
    -Dorg.gradle.configuration-cache=false \
    -Pandroid.testInstrumentationRunnerArguments.numShards="$shard_count" \
    -Pandroid.testInstrumentationRunnerArguments.shardIndex="$shard_index"
