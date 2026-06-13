#!/usr/bin/env python3
"""Scan Zhihu++ Kotlin main sources for low-call function candidates.

This script is a review queue generator, not a refactoring oracle.
"""

from __future__ import annotations

import argparse
import bisect
import csv
import re
import sys
from dataclasses import dataclass
from pathlib import Path


DEFAULT_SOURCE_ROOTS = (
    "app/src/main/java",
    "app/src/main/kotlin",
    "app/src/full/java",
    "app/src/full/kotlin",
    "app/src/lite/java",
    "app/src/lite/kotlin",
    "shared/src/commonMain/kotlin",
    "shared/src/androidMain/kotlin",
    "shared/src/jvmMain/kotlin",
    "shared/src/iosMain/kotlin",
    "shared/src/nativeMain/kotlin",
    "desktopApp/src/main/kotlin",
    "app/src/test/java",
    "app/src/test/kotlin",
    "app/src/androidTest/java",
    "app/src/androidTest/kotlin",
    "shared/src/commonTest/kotlin",
    "shared/src/androidUnitTest/kotlin",
    "shared/src/jvmTest/kotlin",
)

TEST_MARKERS = (
    "/test/",
    "/androidTest/",
    "/androidUnitTest/",
    "/commonTest/",
    "/jvmTest/",
    "/nativeTest/",
)
SKIP_DIRS = {".git", ".gradle", ".idea", ".worktrees", "build", ".kotlin"}

FUNCTION_RE = re.compile(
    r"""
    (?P<prefix>
        (?:^|[\s;{}])
        (?:@[\w.]+(?:\([^)]*\))?\s*)*
        (?:
            (?:public|private|protected|internal|actual|expect|override|open|abstract|
               final|external|inline|suspend|tailrec|operator|infix)
            \s+
        )*
    )
    fun\s+
    (?:<[^>{}()]*>\s*)?
    (?:(?P<receiver>[A-Za-z_][\w<>?,. ]*)\.)?
    (?P<name>`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)\s*\(
    """,
    re.VERBOSE,
)

MODIFIER_RE = re.compile(
    r"\b(public|private|protected|internal|actual|expect|override|open|abstract|"
    r"final|external|inline|suspend|tailrec|operator|infix)\b",
)

ANNOTATION_RE = re.compile(r"@(?:[A-Za-z_][\w]*:)?(?P<name>[A-Za-z_][\w.]*)")

ROOM_DAO_METHOD_ANNOTATIONS = {
    "Delete",
    "Insert",
    "Query",
    "RawQuery",
    "Transaction",
    "Update",
    "Upsert",
}

EXTERNAL_ENTRY_ANNOTATIONS = {
    "JavascriptInterface",
}

REASONABLE_SINGLE_CALL_ANNOTATIONS = {
    "OnLifecycleEvent",
    "Preview",
    "TestOnly",
    "TypeConverter",
    "VisibleForTesting",
}

COMPOSABLE_ANNOTATIONS = {
    "Composable",
}


@dataclass(frozen=True)
class FunctionDef:
    path: str
    line: int
    name: str
    arity: int
    modifiers: str
    annotations: tuple[str, ...]
    kind: str
    body_lines: int
    body_preview: str


def mask_comments_and_strings(text: str) -> str:
    out: list[str] = []
    i = 0
    state: str | None = None
    triple = False

    while i < len(text):
        char = text[i]
        pair = text[i : i + 2]

        if state is None:
            if pair == "//":
                end = text.find("\n", i)
                if end < 0:
                    out.extend(" " * (len(text) - i))
                    break
                out.extend(" " * (end - i))
                i = end
                continue
            if pair == "/*":
                end = text.find("*/", i + 2)
                end = len(text) if end < 0 else end + 2
                out.extend("\n" if c == "\n" else " " for c in text[i:end])
                i = end
                continue
            if text.startswith('"""', i):
                out.extend("   ")
                i += 3
                state = "string"
                triple = True
                continue
            if char == '"':
                out.append(" ")
                i += 1
                state = "string"
                triple = False
                continue
            if char == "'":
                out.append(" ")
                i += 1
                state = "char"
                continue
            out.append(char)
            i += 1
            continue

        if state == "string":
            if triple and text.startswith('"""', i):
                out.extend("   ")
                i += 3
                state = None
                continue
            if not triple and char == "\\":
                out.extend("  ")
                i += 2
                continue
            if not triple and char == '"':
                out.append(" ")
                i += 1
                state = None
                continue
            out.append("\n" if char == "\n" else " ")
            i += 1
            continue

        if state == "char":
            if char == "\\":
                out.extend("  ")
                i += 2
                continue
            if char == "'":
                out.append(" ")
                i += 1
                state = None
                continue
            out.append("\n" if char == "\n" else " ")
            i += 1

    return "".join(out)


def matching_paren(text: str, open_pos: int) -> int:
    depth = 0
    for index in range(open_pos, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return index
    return -1


def parameter_arity(params: str) -> int:
    if not params.strip():
        return 0
    depth = 0
    count = 1
    for char in params:
        if char in "(<[{":
            depth += 1
        elif char in ")>]}":
            depth = max(0, depth - 1)
        elif char == "," and depth == 0:
            count += 1
    return count


def body_span(masked: str, close_paren: int) -> tuple[str, int, int]:
    index = close_paren + 1
    while index < len(masked):
        char = masked[index]
        if char == "{":
            depth = 0
            for end in range(index, len(masked)):
                if masked[end] == "{":
                    depth += 1
                elif masked[end] == "}":
                    depth -= 1
                    if depth == 0:
                        return "block", index, end + 1
            return "block", index, len(masked)
        if char == "=":
            start = index + 1
            end = start
            paren = brace = square = 0
            while end < len(masked):
                c = masked[end]
                if c == "(":
                    paren += 1
                elif c == ")" and paren:
                    paren -= 1
                elif c == "{":
                    brace += 1
                elif c == "}" and brace:
                    brace -= 1
                elif c == "[":
                    square += 1
                elif c == "]" and square:
                    square -= 1
                elif c == "\n" and paren == brace == square == 0:
                    next_index = end + 1
                    while next_index < len(masked) and masked[next_index] in " \t":
                        next_index += 1
                    if next_index < len(masked) and masked[next_index] in ".?:,":
                        end += 1
                        continue
                    return "expr", start, end
                end += 1
            return "expr", start, len(masked)
        if char == "\n":
            return "abstract", index, index
        index += 1
    return "abstract", index, index


def line_starts(text: str) -> list[int]:
    return [0] + [match.end() for match in re.finditer("\n", text)]


def line_number(starts: list[int], position: int) -> int:
    return bisect.bisect_right(starts, position)


def annotation_names(prefix: str) -> tuple[str, ...]:
    return tuple(
        match.group("name").rsplit(".", 1)[-1]
        for match in ANNOTATION_RE.finditer(prefix)
    )


def is_test_path(relative_path: str) -> bool:
    return any(marker in relative_path for marker in TEST_MARKERS)


def source_files(root: Path, include_tests: bool, test_only: bool = False) -> list[Path]:
    files: list[Path] = []
    for source_root in DEFAULT_SOURCE_ROOTS:
        base = root / source_root
        if not base.exists():
            continue
        for path in sorted(base.rglob("*.kt")):
            rel = "/" + path.relative_to(root).as_posix()
            if any(part in SKIP_DIRS for part in path.relative_to(root).parts):
                continue
            is_test = is_test_path(rel)
            if test_only and not is_test:
                continue
            if not test_only and not include_tests and is_test:
                continue
            files.append(path)
    return files


def parse_functions(root: Path, files: list[Path]) -> tuple[list[FunctionDef], dict[Path, str]]:
    masked_by_file: dict[Path, str] = {}
    functions: list[FunctionDef] = []

    for path in files:
        text = path.read_text(errors="replace")
        masked = mask_comments_and_strings(text)
        masked_by_file[path] = masked
        starts = line_starts(text)

        for match in FUNCTION_RE.finditer(masked):
            prefix = match.group("prefix") or ""
            open_pos = masked.find("(", match.start())
            close_pos = matching_paren(masked, open_pos)
            if close_pos < 0:
                continue

            kind, body_start, body_end = body_span(masked, close_pos)
            body = text[body_start:body_end]
            body_lines = 0 if kind == "abstract" else max(1, body.count("\n") + 1)
            functions.append(
                FunctionDef(
                    path=path.relative_to(root).as_posix(),
                    line=line_number(starts, match.start()),
                    name=match.group("name").strip("`"),
                    arity=parameter_arity(masked[open_pos + 1 : close_pos]),
                    modifiers=" ".join(MODIFIER_RE.findall(prefix)),
                    annotations=annotation_names(prefix),
                    kind=kind,
                    body_lines=body_lines,
                    body_preview=" ".join(body.split())[:220],
                ),
            )

    return functions, masked_by_file


def grouped_key(function: FunctionDef) -> tuple[object, ...]:
    mods = set(function.modifiers.split())
    if "expect" in mods or "actual" in mods:
        return function.name, function.arity, "multiplatform-family"
    if "override" in mods:
        return function.name, function.arity, "override-family"
    return function.name, function.arity, function.path, function.line


def has_annotation(function: FunctionDef, names: set[str]) -> bool:
    return any(annotation in names for annotation in function.annotations)


def count_references(
    names: set[str],
    functions: list[FunctionDef],
    masked_by_file: dict[Path, str],
) -> dict[str, int]:
    combined = "\n".join(masked_by_file.values())
    def_counts: dict[str, int] = {}
    for function in functions:
        def_counts[function.name] = def_counts.get(function.name, 0) + 1

    return {
        name: max(
            0,
            len(re.findall(r"\b" + re.escape(name) + r"\b", combined)) - def_counts.get(name, 0),
        )
        for name in names
    }


def should_skip_group(
    group: list[FunctionDef],
    refs: int,
    test_ref_counts: dict[str, int],
) -> bool:
    if any(has_annotation(function, EXTERNAL_ENTRY_ANNOTATIONS) for function in group):
        return True

    if any(has_annotation(function, ROOM_DAO_METHOD_ANNOTATIONS) for function in group):
        return refs > 0

    if refs == 1 and any(has_annotation(function, COMPOSABLE_ANNOTATIONS) for function in group):
        return any(test_ref_counts.get(function.name, 0) > 0 for function in group)

    return refs == 1 and any(
        has_annotation(function, REASONABLE_SINGLE_CALL_ANNOTATIONS)
        for function in group
    )


def write_rows(
    functions: list[FunctionDef],
    masked_by_file: dict[Path, str],
    test_functions: list[FunctionDef],
    test_masked_by_file: dict[Path, str],
    max_calls: int,
    output: Path | None,
) -> None:
    names = {function.name for function in functions}
    ref_counts = count_references(names, functions, masked_by_file)
    test_ref_counts = count_references(names, test_functions, test_masked_by_file)

    groups: dict[tuple[object, ...], list[FunctionDef]] = {}
    for function in functions:
        groups.setdefault(grouped_key(function), []).append(function)

    rows: list[list[object]] = []
    for group in groups.values():
        representative = group[0]
        refs = max(0, ref_counts.get(representative.name, 0))
        if refs > max_calls:
            continue
        if should_skip_group(group, refs, test_ref_counts):
            continue
        rows.append(
            [
                refs,
                len(group),
                representative.body_lines,
                representative.kind,
                representative.modifiers,
                representative.name,
                representative.arity,
                representative.path,
                representative.line,
                representative.body_preview,
            ],
        )

    rows.sort(key=lambda row: (row[0], row[7], row[8], row[5]))
    header = [
        "calls_excluding_defs",
        "defs_grouped",
        "body_lines",
        "kind",
        "modifiers",
        "name",
        "arity",
        "path",
        "line",
        "body_preview",
    ]

    target = output.open("w", newline="") if output else sys.stdout
    try:
        writer = csv.writer(target, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)
    finally:
        if output:
            target.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="Repository root. Default: current directory.")
    parser.add_argument("--max-calls", type=int, default=2, help="Maximum non-definition references to include.")
    parser.add_argument("--include-tests", action="store_true", help="Include test source roots in the scan.")
    parser.add_argument("--output", type=Path, help="Write TSV output to this path instead of stdout.")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    files = source_files(root, include_tests=args.include_tests)
    test_files = source_files(root, include_tests=True, test_only=True)
    functions, masked_by_file = parse_functions(root, files)
    test_functions, test_masked_by_file = parse_functions(root, test_files)
    write_rows(
        functions,
        masked_by_file,
        test_functions,
        test_masked_by_file,
        args.max_calls,
        args.output,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
