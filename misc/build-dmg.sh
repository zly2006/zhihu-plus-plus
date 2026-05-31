#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "This script builds a DMG and must be run on macOS." >&2
    exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_TASK="${GRADLE_TASK:-:desktopApp:packageDmg}"
JAVA_VERSION="${DMG_JAVA_VERSION:-25}"

is_homebrew_path() {
    case "$1" in
        /opt/homebrew/* | /usr/local/Cellar/* | /usr/local/Homebrew/* | /usr/local/opt/*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

validate_java_home() {
    local java_home="$1"

    [[ -n "$java_home" ]] || return 1
    [[ -x "$java_home/bin/java" ]] || return 1
    ! is_homebrew_path "$java_home" || return 1
}

java_major_version() {
    local version

    version="$("$1/bin/java" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
    if [[ "$version" == 1.* ]]; then
        echo "${version#1.}" | cut -d. -f1
    else
        echo "$version" | cut -d. -f1
    fi
}

resolve_java_home() {
    local candidate

    if [[ -n "${DMG_JAVA_HOME:-}" ]]; then
        validate_java_home "$DMG_JAVA_HOME" || {
            echo "DMG_JAVA_HOME must point to a non-Homebrew JDK." >&2
            return 1
        }
        echo "$DMG_JAVA_HOME"
        return 0
    fi

    if validate_java_home "${JAVA_HOME:-}"; then
        echo "$JAVA_HOME"
        return 0
    fi

    if [[ -x /usr/libexec/java_home ]]; then
        if candidate="$(/usr/libexec/java_home -v "$JAVA_VERSION" 2>/dev/null)" && validate_java_home "$candidate"; then
            echo "$candidate"
            return 0
        fi

        for version in 25 21 17; do
            if candidate="$(/usr/libexec/java_home -v "$version" 2>/dev/null)" && validate_java_home "$candidate"; then
                echo "$candidate"
                return 0
            fi
        done

        if candidate="$(/usr/libexec/java_home 2>/dev/null)" && validate_java_home "$candidate"; then
            echo "$candidate"
            return 0
        fi
    fi

    echo "No non-Homebrew JDK found. Install one or set DMG_JAVA_HOME." >&2
    return 1
}

read_app_version() {
    awk -F= '/^app\.versionName=/ { print $2; exit }' "$ROOT_DIR/gradle.properties"
}

normalize_jpackage_version() {
    local source_version="$1"
    local major minor patch

    IFS=. read -r major minor patch _ <<< "$source_version"
    if [[ ! "$major" =~ ^[0-9]+$ ]] || [[ ! "${minor:-0}" =~ ^[0-9]+$ ]] || [[ ! "${patch:-0}" =~ ^[0-9]+$ ]]; then
        echo "app.versionName '$source_version' is not a jpackage version. Set DMG_PACKAGE_VERSION." >&2
        return 1
    fi

    if (( major <= 0 )); then
        major=1
    fi

    echo "$major.${minor:-0}.${patch:-0}"
}

JAVA_HOME="$(resolve_java_home)"
JAVA_MAJOR="$(java_major_version "$JAVA_HOME")"

if (( JAVA_MAJOR < 17 )); then
    echo "JDK 17 or newer is required, but selected JAVA_HOME is Java $JAVA_MAJOR: $JAVA_HOME" >&2
    exit 1
fi

APP_VERSION="$(read_app_version)"
PACKAGE_VERSION="${DMG_PACKAGE_VERSION:-$(normalize_jpackage_version "$APP_VERSION")}"

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Running Gradle task: $GRADLE_TASK"
if [[ "$PACKAGE_VERSION" != "$APP_VERSION" ]]; then
    echo "Using DMG package version: $PACKAGE_VERSION (from app.versionName=$APP_VERSION)"
fi

cd "$ROOT_DIR"
PATH="$JAVA_HOME/bin:$PATH" JAVA_HOME="$JAVA_HOME" ./gradlew --no-daemon "-Papp.versionName=$PACKAGE_VERSION" "$GRADLE_TASK"

dmg_files=()
while IFS= read -r dmg_file; do
    dmg_files+=("$dmg_file")
done < <(find "$ROOT_DIR/desktopApp/build/compose/binaries" -type f -name "*.dmg" -print 2>/dev/null | sort)

if (( ${#dmg_files[@]} == 0 )); then
    echo "Gradle completed, but no DMG file was found under desktopApp/build/compose/binaries." >&2
    exit 1
fi

echo "DMG output:"
printf '%s\n' "${dmg_files[@]}"
