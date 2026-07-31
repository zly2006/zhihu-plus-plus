#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 || ! $1 =~ ^[1-9][0-9]*$ || ! $2 =~ ^[0-9]+$ || $2 -ge $1 ]]; then
    echo "usage: $0 <shard-count> <zero-based-shard-index>" >&2
    exit 2
fi

readonly shard_count=$1
readonly shard_index=$2
readonly device_serial=${ANDROID_SERIAL:-emulator-5554}
readonly results_dir=app/build/outputs/androidTest-results
readonly app_apk=${INSTRUMENT_APP_APK:?INSTRUMENT_APP_APK must point to the app APK}
readonly test_apk=${INSTRUMENT_TEST_APK:?INSTRUMENT_TEST_APK must point to the test APK}
readonly instrumentation_component=com.chloemlla.zhplus.lite.test/com.chloemlla.zhplus.ZhihuInstrumentedTestRunner
readonly instrumentation_result="$results_dir/instrumentation-shard-$shard_index.txt"

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

adb -s "$device_serial" install -r -t "$app_apk"
adb -s "$device_serial" install -r -t "$test_apk"
adb -s "$device_serial" shell am instrument -w \
    -e zhpp_data_mode mock \
    -e numShards "$shard_count" \
    -e shardIndex "$shard_index" \
    "$instrumentation_component" | tee "$instrumentation_result"
grep -Eq '^OK \([0-9]+ tests?\)$' "$instrumentation_result"
