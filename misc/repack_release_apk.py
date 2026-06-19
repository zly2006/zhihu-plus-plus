#!/usr/bin/env python3
"""Download a release APK, change its application id, and sign it locally.

Example:
    python3 misc/repack_release_apk.py \
        --tag 0.23.4 \
        --asset 'zhihu++-lite.apk' \
        --new-package com.github.zly2006.zhplus.lite.release
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path
from typing import Any


DEFAULT_REPO = "zly2006/zhihu-plus-plus"
DEFAULT_ASSET = "zhihu++-lite.apk"
DEFAULT_NEW_PACKAGE = "com.github.zly2006.zhplus.lite.release"
DEFAULT_APKTOOL_VERSION = "v3.0.2"
DEFAULT_APKTOOL_ASSET = "apktool_3.0.2.jar"


class RepackError(Exception):
    pass


def die(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(1)


def run(command: list[str], *, capture: bool = False) -> str:
    print("+", subprocess.list2cmdline(command), flush=True)
    proc = subprocess.run(
        command,
        check=False,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )
    if proc.returncode != 0:
        output = f"\n{proc.stdout}" if capture and proc.stdout else ""
        raise RepackError(f"command failed ({proc.returncode}): {subprocess.list2cmdline(command)}{output}")
    return proc.stdout or ""


def github_headers() -> dict[str, str]:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "zhihu-plus-plus-apk-repack",
    }
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def read_json_url(url: str) -> dict[str, Any]:
    request = urllib.request.Request(url, headers=github_headers())
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def download(url: str, path: Path, *, force: bool = False) -> None:
    if path.exists() and not force:
        print(f"reuse {path}")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers=github_headers())
    print(f"download {url}")
    with urllib.request.urlopen(request) as response, path.open("wb") as file:
        shutil.copyfileobj(response, file)


def resolve_release(repo: str, tag: str | None) -> dict[str, Any]:
    if tag:
        return read_json_url(f"https://api.github.com/repos/{repo}/releases/tags/{tag}")
    return read_json_url(f"https://api.github.com/repos/{repo}/releases/latest")


def resolve_asset(release: dict[str, Any], asset_name: str) -> tuple[str, str]:
    for asset in release.get("assets", []):
        if asset.get("name") == asset_name:
            return asset["name"], asset["browser_download_url"]
    available = ", ".join(asset.get("name", "") for asset in release.get("assets", []))
    raise RepackError(f"asset not found: {asset_name}. available assets: {available}")


def find_android_sdk(explicit: str | None) -> Path:
    candidates = [
        explicit,
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        str(Path.home() / "Library/Android/sdk"),
        str(Path.home() / "Android/Sdk"),
    ]
    for candidate in candidates:
        if not candidate:
            continue
        path = Path(candidate).expanduser()
        if path.exists():
            return path
    raise RepackError("Android SDK not found. Set ANDROID_HOME or pass --sdk-dir")


def version_key(path: Path) -> tuple[int, ...]:
    parts: list[int] = []
    for item in re.split(r"[.-]", path.name):
        if item.isdigit():
            parts.append(int(item))
        else:
            parts.append(0)
    return tuple(parts)


def find_build_tools(sdk_dir: Path, requested_version: str | None) -> tuple[Path, Path, Path]:
    build_tools_dir = sdk_dir / "build-tools"
    if requested_version:
        candidates = [build_tools_dir / requested_version]
    else:
        candidates = sorted((path for path in build_tools_dir.iterdir() if path.is_dir()), key=version_key, reverse=True)
    for path in candidates:
        aapt2 = path / "aapt2"
        zipalign = path / "zipalign"
        apksigner = path / "apksigner"
        if aapt2.exists() and zipalign.exists() and apksigner.exists():
            return aapt2, zipalign, apksigner
    raise RepackError(f"no complete build-tools installation found under {build_tools_dir}")


def find_apkanalyzer(sdk_dir: Path) -> Path | None:
    candidates = [
        sdk_dir / "cmdline-tools/latest/bin/apkanalyzer",
        sdk_dir / "tools/bin/apkanalyzer",
    ]
    return next((path for path in candidates if path.exists()), None)


def ensure_apktool(work_dir: Path, apktool_jar: str | None, force_download: bool) -> Path:
    if apktool_jar:
        path = Path(apktool_jar).expanduser()
        if not path.exists():
            raise RepackError(f"apktool jar not found: {path}")
        return path
    path = work_dir / DEFAULT_APKTOOL_ASSET
    url = f"https://github.com/iBotPeaches/Apktool/releases/download/{DEFAULT_APKTOOL_VERSION}/{DEFAULT_APKTOOL_ASSET}"
    download(url, path, force=force_download)
    return path


def manifest_package(manifest: Path) -> str:
    match = re.search(r'\bpackage="([^"]+)"', manifest.read_text(encoding="utf-8"))
    if not match:
        raise RepackError(f"manifest package not found in {manifest}")
    return match.group(1)


def replace_manifest_package(manifest: Path, old_package: str, new_package: str) -> None:
    text = manifest.read_text(encoding="utf-8")
    if old_package not in text:
        raise RepackError(f"old package {old_package} not found in decoded manifest")
    manifest.write_text(text.replace(old_package, new_package), encoding="utf-8")


def default_output_path(work_dir: Path, tag_name: str, asset_name: str, new_package: str) -> Path:
    safe_tag = re.sub(r"[^A-Za-z0-9_.-]+", "-", tag_name).strip("-") or "release"
    safe_asset = asset_name.replace("++", "-plus-plus").replace(".apk", "")
    suffix = new_package.rsplit(".", 1)[-1]
    return work_dir / f"{safe_asset}-{safe_tag}-{suffix}.apk"


def verify_output(apkanalyzer: Path | None, apksigner: Path, apk: Path, expected_package: str, old_package: str) -> None:
    if apkanalyzer:
        actual_package = run([str(apkanalyzer), "manifest", "application-id", str(apk)], capture=True).strip()
    else:
        actual_package = ""
    if actual_package and actual_package != expected_package:
        raise RepackError(f"rebuilt APK has package {actual_package}, expected {expected_package}")

    run([str(apksigner), "verify", "--verbose", str(apk)])

    data = apk.read_bytes()
    if old_package.encode() in data:
        print(f"warning: old package string still appears in APK bytes: {old_package}", file=sys.stderr)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=DEFAULT_REPO, help=f"GitHub repo, default: {DEFAULT_REPO}")
    parser.add_argument("--tag", help="Release tag. Omit to use GitHub latest stable release")
    parser.add_argument("--asset", default=DEFAULT_ASSET, help=f"Release asset name, default: {DEFAULT_ASSET}")
    parser.add_argument("--new-package", default=DEFAULT_NEW_PACKAGE, help=f"New application id, default: {DEFAULT_NEW_PACKAGE}")
    parser.add_argument("--old-package", help="Old application id. Omit to read it from decoded AndroidManifest.xml")
    parser.add_argument("--output", help="Signed APK output path")
    parser.add_argument("--work-dir", default="build/repack-release", help="Temporary/output directory")
    parser.add_argument("--sdk-dir", help="Android SDK directory")
    parser.add_argument("--build-tools-version", help="Android build-tools version to use")
    parser.add_argument("--apktool-jar", help="Existing apktool jar path. Omit to download apktool")
    parser.add_argument("--force-download", action="store_true", help="Re-download APK and apktool even if cached")
    parser.add_argument("--keep-work", action="store_true", help="Keep decoded APK directory")
    parser.add_argument("--keystore", default=str(Path.home() / ".android/debug.keystore"), help="Signing keystore")
    parser.add_argument("--ks-key-alias", default="androiddebugkey", help="Signing key alias")
    parser.add_argument("--ks-pass", default="android", help="Keystore password")
    parser.add_argument("--key-pass", default="android", help="Key password")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    work_dir = Path(args.work_dir).expanduser()
    work_dir.mkdir(parents=True, exist_ok=True)

    release = resolve_release(args.repo, args.tag)
    tag_name = release.get("tag_name") or args.tag or "latest"
    asset_name, asset_url = resolve_asset(release, args.asset)

    sdk_dir = find_android_sdk(args.sdk_dir)
    aapt2, zipalign, apksigner = find_build_tools(sdk_dir, args.build_tools_version)
    apkanalyzer = find_apkanalyzer(sdk_dir)
    apktool = ensure_apktool(work_dir, args.apktool_jar, args.force_download)

    input_apk = work_dir / asset_name
    decoded_dir = work_dir / f"{Path(asset_name).stem}-decoded"
    unsigned_apk = work_dir / f"{Path(asset_name).stem}-unsigned.apk"
    aligned_apk = work_dir / f"{Path(asset_name).stem}-aligned.apk"
    output_apk = Path(args.output).expanduser() if args.output else default_output_path(work_dir, tag_name, asset_name, args.new_package)

    download(asset_url, input_apk, force=args.force_download)
    if decoded_dir.exists():
        shutil.rmtree(decoded_dir)

    run(["java", "-jar", str(apktool), "d", "-f", str(input_apk), "-o", str(decoded_dir)])
    manifest = decoded_dir / "AndroidManifest.xml"
    old_package = args.old_package or manifest_package(manifest)
    replace_manifest_package(manifest, old_package, args.new_package)

    for path in (unsigned_apk, aligned_apk, output_apk):
        path.unlink(missing_ok=True)

    run(["java", "-jar", str(apktool), "b", "-f", "--aapt", str(aapt2), str(decoded_dir), "-o", str(unsigned_apk)])
    run([str(zipalign), "-p", "-f", "4", str(unsigned_apk), str(aligned_apk)])
    run(
        [
            str(apksigner),
            "sign",
            "--ks",
            str(Path(args.keystore).expanduser()),
            "--ks-key-alias",
            args.ks_key_alias,
            "--ks-pass",
            f"pass:{args.ks_pass}",
            "--key-pass",
            f"pass:{args.key_pass}",
            "--out",
            str(output_apk),
            str(aligned_apk),
        ],
    )
    verify_output(apkanalyzer, apksigner, output_apk, args.new_package, old_package)

    if not args.keep_work:
        shutil.rmtree(decoded_dir, ignore_errors=True)
        unsigned_apk.unlink(missing_ok=True)
        aligned_apk.unlink(missing_ok=True)

    print(f"output: {output_apk}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RepackError, subprocess.SubprocessError) as error:
        die(str(error))
