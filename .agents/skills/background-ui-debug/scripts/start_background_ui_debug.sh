#!/bin/zsh
#
# Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
# Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation (version 3 only).
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

set -euo pipefail

skill_directory=${0:A:h}
project_directory=${skill_directory:h:h:h:h}
debug_bundle="$project_directory/macosUiDebug/build/bin/macosArm64/debugApp/ZhihuPlusPlusUiDebug.app"
debug_binary="$debug_bundle/Contents/MacOS/ZhihuPlusPlusUiDebug"

cd "$project_directory"
./gradlew :macosUiDebug:packageDebugMacosUiDebug

if [[ ! -x "$debug_binary" ]]; then
    print -u2 "后台 UI 调试二进制不存在：$debug_binary"
    exit 1
fi

if pgrep -x 'ZhihuPlusPlus|ZhihuPlusPlus.kexe' >/dev/null 2>&1; then
    print -u2 "正式应用仍在运行；拒绝启动后台 UI 调试器"
    exit 1
fi

if pgrep -x ZhihuPlusPlusUiDebug >/dev/null 2>&1; then
    print -u2 "已有后台 UI 调试器正在运行；拒绝启动重复实例"
    exit 1
fi

exec "$debug_binary" "$@"
