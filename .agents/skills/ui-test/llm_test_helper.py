#!/usr/bin/env python3
"""
Zhihu++ LLM 自动化测试辅助脚本

通过 UIAutomator dump + adb 精准交互 UI 元素，替代硬编码坐标。

用法:
    python3 .github/skills/ui-test/llm_test_helper.py dump
    python3 .github/skills/ui-test/llm_test_helper.py tap --tag nav_tab_home
    python3 .github/skills/ui-test/llm_test_helper.py tap --text "推荐"
    python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card --index 1
    python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "ChatGPT"
    python3 .github/skills/ui-test/llm_test_helper.py tap --tag feed_card_more_btn --within-text "AI" --index 1

多元素消歧规则（LLM 必读）:
    同一 tag（如 feed_card_more_btn）在屏幕上可能出现多个。消歧步骤：
    1. 先运行 dump，获取所有元素的序号和文字内容。
    2. 若知道目标卡片标题/关键词：用 --within-text TEXT 缩小范围到该卡片内。
    3. 若同一卡片内仍有多个同 tag 元素，再加 --index N 选第 N 个（0-based）。
    4. 若只知道位置不知道内容：只用 --index N（序号来自 dump 输出）。
    --within-text 和 --index 可以同时使用，--within-text 先过滤，--index 再选。
"""

import argparse
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from typing import Optional

PACKAGE_LITE = "com.github.zly2006.zhplus.lite"
PACKAGE_FULL = "com.github.zly2006.zhplus"
UI_DUMP_DEVICE = "/sdcard/ui_dump.xml"
UI_DUMP_LOCAL = "/tmp/zhihu_ui_dump.xml"


def adb(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["adb", *args], capture_output=True, text=True)


def get_package() -> str:
    result = adb("shell", "pm", "list", "packages")
    return PACKAGE_LITE if PACKAGE_LITE in result.stdout else PACKAGE_FULL


def dump_ui() -> ET.Element:
    dump_result = adb("shell", "uiautomator", "dump", UI_DUMP_DEVICE)
    if dump_result.returncode != 0:
        print(f"uiautomator dump 失败: {dump_result.stderr}", file=sys.stderr)
        sys.exit(1)

    pull_result = adb("pull", UI_DUMP_DEVICE, UI_DUMP_LOCAL)
    if pull_result.returncode != 0:
        print(f"adb pull dump 失败: {pull_result.stderr}", file=sys.stderr)
        sys.exit(1)
    if not os.path.exists(UI_DUMP_LOCAL):
        print(f"adb pull 后未找到 dump 文件: {UI_DUMP_LOCAL}", file=sys.stderr)
        sys.exit(1)
    return ET.parse(UI_DUMP_LOCAL).getroot()


def get_bounds(node: ET.Element) -> Optional[tuple[int, int, int, int]]:
    try:
        parts = node.get("bounds", "").replace("][", ",").strip("[]").split(",")
        return int(parts[0]), int(parts[1]), int(parts[2]), int(parts[3])
    except (ValueError, IndexError):
        return None


def center(bounds: tuple[int, int, int, int]) -> tuple[int, int]:
    x1, y1, x2, y2 = bounds
    return (x1 + x2) // 2, (y1 + y2) // 2


def node_contains_text(node: ET.Element, text: str) -> bool:
    """递归判断节点树中是否包含指定文字（text 或 content-desc）。"""
    for n in node.iter("node"):
        if text in n.get("text", "") or text in n.get("content-desc", ""):
            return True
    return False


def build_parent_map(root: ET.Element) -> dict[ET.Element, ET.Element]:
    """构建子节点 → 父节点的映射。"""
    parents: dict[ET.Element, ET.Element] = {}
    for parent in root.iter("node"):
        for child in parent:
            parents[child] = parent
    return parents


def find_ancestor_containing_text(
    node: ET.Element,
    text: str,
    parents: dict[ET.Element, ET.Element],
) -> Optional[ET.Element]:
    """向上遍历父节点，找到第一个包含指定文字的祖先。"""
    current = node
    while current in parents:
        current = parents[current]
        if node_contains_text(current, text):
            return current
    return None


def node_snippet(node: ET.Element, parents: dict[ET.Element, ET.Element], length: int = 30) -> str:
    """提取节点附近的文字片段，用于错误提示中标识元素。"""
    # 优先用自身文字
    text = node.get("text", "") or node.get("content-desc", "")
    if text:
        return text[:length]
    # 向上找父节点文字
    current = node
    while current in parents:
        current = parents[current]
        for n in current.iter("node"):
            t = n.get("text", "") or n.get("content-desc", "")
            if t:
                return t[:length]
    return "(无文字)"


def ambiguity_error(tag: str, candidates: list[ET.Element], within_text: Optional[str], parents: dict[ET.Element, ET.Element]) -> None:
    """多元素歧义错误：列出所有候选，告诉 LLM 如何消歧。"""
    scope = f" (已用 --within-text '{within_text}' 过滤后)" if within_text else ""
    print(f"[AMBIGUOUS] tag='{tag}'{scope} 匹配到 {len(candidates)} 个元素，必须消歧后重试：", file=sys.stderr)
    print(file=sys.stderr)
    for i, n in enumerate(candidates):
        b = get_bounds(n)
        cx, cy = center(b) if b else (0, 0)
        snippet = node_snippet(n, parents)
        print(f"  --index {i}  center=({cx:4d},{cy:4d})  附近文字: \"{snippet}\"", file=sys.stderr)
    print(file=sys.stderr)
    if not within_text:
        print("消歧方式（选其一）：", file=sys.stderr)
        print("  1. 用 --within-text TEXT  指定目标卡片的标题关键词", file=sys.stderr)
        print("  2. 用 --index N           按上方序号直接选取", file=sys.stderr)
    else:
        print("消歧方式：用 --index N 按上方序号选取", file=sys.stderr)
    sys.exit(2)


def find_all_by_text(root: ET.Element, text: str) -> list[ET.Element]:
    return [n for n in root.iter("node") if n.get("text") == text]


def find_all_by_content_desc(root: ET.Element, desc: str) -> list[ET.Element]:
    return [n for n in root.iter("node") if n.get("content-desc") == desc]


def find_all_by_tag(root: ET.Element, tag: str, package: str) -> list[ET.Element]:
    full_resource_id = f"{package}:id/{tag}"
    # 不同 Android/Compose 版本下，UI dump 里 resource-id 可能是完整格式或裸 tag。
    accepted = {full_resource_id, tag}
    return [n for n in root.iter("node") if n.get("resource-id", "") in accepted]


def format_resource_id(resource_id: str, package: str) -> str:
    if not resource_id:
        return ""
    if resource_id.startswith(package + ":id/"):
        return "tag:" + resource_id[len(package) + 4:]
    if ":" not in resource_id and "/" not in resource_id:
        return "tag:" + resource_id
    return resource_id


def node_has_useful_info(node: ET.Element) -> bool:
    rid = node.get("resource-id", "")
    if rid == "android:id/content":
        rid = ""
    return bool(rid or node.get("text") or node.get("content-desc") or node.get("clickable") == "true")


def node_order_key(node: ET.Element) -> tuple[int, int]:
    bounds = get_bounds(node)
    if not bounds:
        return (10**9, 10**9)
    x1, y1, _, _ = bounds
    return (y1, x1)


def shorten(text: str, max_len: int = 60) -> str:
    if len(text) <= max_len:
        return text
    return text[: max_len - 1] + "…"


def detect_dialog_scope(root: ET.Element) -> Optional[ET.Element]:
    nodes = list(root.iter("node"))
    if not nodes:
        return None
    screen_bounds = get_bounds(nodes[0])
    if not screen_bounds:
        return None
    sx1, sy1, sx2, sy2 = screen_bounds
    # 某些系统版本在弹窗时只返回弹窗窗口树（根节点非全屏），此时根节点即弹窗作用域。
    if sx1 > 0 or sy1 > 0:
        return nodes[0]
    sw = sx2 - sx1
    sh = sy2 - sy1
    if sw <= 0 or sh <= 0:
        return None
    screen_area = sw * sh

    candidates: list[tuple[tuple[int, int, int], ET.Element]] = []
    for node in nodes:
        bounds = get_bounds(node)
        if not bounds:
            continue
        x1, y1, x2, y2 = bounds
        w = x2 - x1
        h = y2 - y1
        area = w * h
        if w <= 0 or h <= 0:
            continue
        # 典型居中弹窗：不占满屏、距离四周有边距。
        if x1 < sx1 + int(sw * 0.05) or x2 > sx2 - int(sw * 0.05):
            continue
        if y1 < sy1 + int(sh * 0.15) or y2 > sy2 - int(sh * 0.05):
            continue
        if area >= int(screen_area * 0.9) or area <= int(screen_area * 0.03):
            continue
        clickable_count = sum(1 for n in node.iter("node") if n.get("clickable") == "true")
        if clickable_count == 0:
            continue
        text_count = sum(
            1
            for n in node.iter("node")
            if n.get("text", "").strip() or n.get("content-desc", "").strip()
        )
        if text_count == 0:
            continue
        # 优先选择包含更多交互与文字信息的容器（通常是弹窗根），再比较面积。
        candidates.append(((clickable_count, text_count, area), node))

    if not candidates:
        return None
    candidates.sort(key=lambda item: item[0], reverse=True)
    return candidates[0][1]


def resolve_node(args, root: ET.Element, package: str) -> ET.Element:
    """根据参数解析出目标节点，歧义时输出结构化错误并退出（exit code 2）。"""
    parents = build_parent_map(root)
    index: Optional[int] = getattr(args, "index", None)  # None = 用户未指定
    within_text: Optional[str] = getattr(args, "within_text", None)

    if args.tag is not None:
        candidates = find_all_by_tag(root, args.tag, package)
        if not candidates:
            print(f"[NOT FOUND] tag='{args.tag}' 在当前界面不存在", file=sys.stderr)
            print("提示：运行 `dump` 查看当前界面所有可点击元素", file=sys.stderr)
            sys.exit(1)

        # --within-text 缩小范围
        if within_text:
            matched = [
                n for n in candidates
                if find_ancestor_containing_text(n, within_text, parents) is not None
                or node_contains_text(n, within_text)
            ]
            if not matched:
                print(f"[NOT FOUND] tag='{args.tag}' 在包含 '{within_text}' 的节点内不存在", file=sys.stderr)
                print(f"提示：当前界面共有 {len(candidates)} 个 tag='{args.tag}'，但无一位于含 '{within_text}' 的节点内", file=sys.stderr)
                print("请检查关键词拼写，或改用 --index N 按位置选取", file=sys.stderr)
                sys.exit(1)
            candidates = matched

        # 多个候选且未指定 --index → 歧义错误
        if len(candidates) > 1 and index is None:
            ambiguity_error(args.tag, candidates, within_text, parents)

        # 确定最终 index
        i = index if index is not None else 0
        if i >= len(candidates) or i < 0:
            print(
                f"[OUT OF RANGE] index={i} 超出范围，当前共 {len(candidates)} 个 tag='{args.tag}'（有效范围 0~{len(candidates)-1}）",
                file=sys.stderr,
            )
            sys.exit(1)
        return candidates[i]

    elif args.text is not None:
        matches = find_all_by_text(root, args.text)
        if not matches:
            print(f"[NOT FOUND] text='{args.text}' 在当前界面不存在", file=sys.stderr)
            sys.exit(1)
        if len(matches) > 1 and index is None:
            print(f"[AMBIGUOUS] text='{args.text}' 匹配到 {len(matches)} 个元素，必须消歧后重试：", file=sys.stderr)
            for i, n in enumerate(matches):
                b = get_bounds(n)
                cx, cy = center(b) if b else (0, 0)
                print(f"  --index {i}  center=({cx:4d},{cy:4d})  desc=\"{n.get('content-desc', '')}\"", file=sys.stderr)
            print("消歧方式：加 --index N 选取，或改用 --tag 精确定位", file=sys.stderr)
            sys.exit(2)
        i = index if index is not None else 0
        if i < 0 or i >= len(matches):
            print(
                f"[OUT OF RANGE] index={i} 超出范围，当前共 {len(matches)} 个（有效范围 0~{len(matches)-1}）",
                file=sys.stderr,
            )
            sys.exit(1)
        return matches[i]

    elif args.desc is not None:
        matches = find_all_by_content_desc(root, args.desc)
        if not matches:
            print(f"[NOT FOUND] desc='{args.desc}' 在当前界面不存在", file=sys.stderr)
            sys.exit(1)
        if len(matches) > 1:
            print(f"[AMBIGUOUS] desc='{args.desc}' 匹配到 {len(matches)} 个元素，请改用 --tag 消歧", file=sys.stderr)
            for i, n in enumerate(matches):
                b = get_bounds(n)
                cx, cy = center(b) if b else (0, 0)
                print(f"  [{i}] center=({cx:4d},{cy:4d})  text=\"{n.get('text', '')}\"", file=sys.stderr)
            sys.exit(2)
        return matches[0]

    print("未指定选择条件", file=sys.stderr)
    sys.exit(1)


def get_content_recursive(node: ET.Element) -> str:
    """递归获取节点及其子节点的 text 和 content-desc，用于 dump 输出中展示元素信息。"""
    parts = []
    text = node.get("text", "").strip()
    desc = node.get("content-desc", "").strip()
    if text:
        parts.append(text)
    if desc:
        parts.append(desc)
    for child in node:
        child_text = get_content_recursive(child)
        if child_text:
            parts.append(child_text)
    final = " | ".join(parts)
    if len(final) > 60:
        return final[:57] + "..."
    return final


def cmd_dump(args):
    root = dump_ui()
    print(f"已保存 UI dump → {UI_DUMP_LOCAL}")
    scope = detect_dialog_scope(root)
    if scope is not None:
        print("\n检测到弹窗：仅显示弹窗内元素（已过滤无用节点）")
        dump_nodes = list(scope.iter("node"))
    else:
        dump_nodes = list(root.iter("node"))

    visible_nodes = [n for n in dump_nodes if node_has_useful_info(n)]
    visible_nodes.sort(key=node_order_key)

    print(f"\n关键信息元素（{len(visible_nodes)} 个，按屏幕顺序；含可点击与不可点击）:\n")
    for i, node in enumerate(visible_nodes):
        clickable = "C" if node.get("clickable") == "true" else "N"
        label = get_content_recursive(node)
        bounds = node.get("bounds", "")
        print(f"  [{i:2d}] [{clickable}] {label:70s} {bounds}")


def cmd_find(args):
    package = get_package()
    root = dump_ui()

    if args.tag:
        candidates = find_all_by_tag(root, args.tag, package)
        if not candidates:
            print(f"未找到 tag='{args.tag}'", file=sys.stderr)
            sys.exit(1)

        within_text: Optional[str] = getattr(args, "within_text", None)
        if within_text:
            parents = build_parent_map(root)
            candidates = [
                n for n in candidates
                if find_ancestor_containing_text(n, within_text, parents) is not None
                or node_contains_text(n, within_text)
            ]
            if not candidates:
                print(f"[NOT FOUND] tag='{args.tag}' 在包含 '{within_text}' 的节点内不存在", file=sys.stderr)
                sys.exit(1)

        index: Optional[int] = getattr(args, "index", None)
        if index is not None:
            if index < 0 or index >= len(candidates):
                print(
                    f"[OUT OF RANGE] index={index} 超出范围，当前共 {len(candidates)} 个（有效范围 0~{len(candidates)-1}）",
                    file=sys.stderr,
                )
                sys.exit(1)
            candidates = [candidates[index]]

        print(f"找到 {len(candidates)} 个 tag='{args.tag}'（index 0 = 最顶部）:")
        for i, n in enumerate(candidates):
            b = get_bounds(n)
            cx, cy = center(b) if b else (0, 0)
            snippet = n.get("text", "") or n.get("content-desc", "")
            print(f"  [{i}] center=({cx:4d},{cy:4d})  bounds={n.get('bounds')}  {snippet}")
        return

    node = resolve_node(args, root, package)
    b = get_bounds(node)
    if b:
        cx, cy = center(b)
        print(f"center=({cx},{cy})  bounds={node.get('bounds')}")


def cmd_tap(args):
    package = get_package()
    root = dump_ui()
    node = resolve_node(args, root, package)

    b = get_bounds(node)
    if b is None:
        print("无法解析元素边界", file=sys.stderr)
        sys.exit(1)

    cx, cy = center(b)
    if args.tag is not None:
        tag_info = f"tag={args.tag}"
    elif args.text is not None:
        tag_info = f"text={args.text!r}"
    else:
        tag_info = f"desc={args.desc!r}"
    print(f"点击 {tag_info} → ({cx}, {cy})  bounds={node.get('bounds')}")
    # uiautomator dump 会短暂冻结 UI，立刻 tap 会被系统丢弃，需等待 UI 恢复
    time.sleep(0.8)
    result = adb("shell", "input", "tap", str(cx), str(cy))
    if result.returncode != 0:
        print(f"tap 失败: {result.stderr}", file=sys.stderr)
        sys.exit(1)


def cmd_screenshot(args):
    remote = "/sdcard/llm_test_screenshot.png"
    capture = adb("shell", "screencap", "-p", remote)
    if capture.returncode != 0:
        print(f"screencap 失败: {capture.stderr}", file=sys.stderr)
        sys.exit(1)
    local = args.output or "/tmp/zhihu_screenshot.png"
    pull = adb("pull", remote, local)
    if pull.returncode != 0:
        print(f"截图 pull 失败: {pull.stderr}", file=sys.stderr)
        sys.exit(1)
    print(f"截图 → {local}")


def add_selector_args(p: argparse.ArgumentParser):
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument("--tag", help="testTag 名称")
    g.add_argument("--text", help="元素显示文字（精确匹配）")
    g.add_argument("--desc", help="contentDescription（精确匹配）")


def main():
    parser = argparse.ArgumentParser(
        description="Zhihu++ LLM 自动化测试辅助工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("dump", help="Dump UI 树，列出关键信息元素（含可点击与不可点击，弹窗优先）")

    find_p = sub.add_parser("find", help="查找元素，打印坐标（不点击）")
    add_selector_args(find_p)
    find_p.add_argument("--within-text", metavar="TEXT", help="只在包含此文字的父节点内查找")
    find_p.add_argument("--index", type=int, default=None, help="第 N 个同名 tag（未指定时若有歧义则报错）")

    tap_p = sub.add_parser("tap", help="点击指定元素")
    add_selector_args(tap_p)
    tap_p.add_argument("--within-text", metavar="TEXT", help="只点击包含此文字的父节点内的 tag")
    tap_p.add_argument("--index", type=int, default=None, help="第 N 个同名 tag（未指定时若有歧义则报错）")

    ss_p = sub.add_parser("screenshot", help="截图并拉取到本地")
    ss_p.add_argument("output", nargs="?", help="本地路径（默认 /tmp/zhihu_screenshot.png）")

    args = parser.parse_args()
    {"dump": cmd_dump, "find": cmd_find, "tap": cmd_tap, "screenshot": cmd_screenshot}[
        args.command
    ](args)


if __name__ == "__main__":
    main()
