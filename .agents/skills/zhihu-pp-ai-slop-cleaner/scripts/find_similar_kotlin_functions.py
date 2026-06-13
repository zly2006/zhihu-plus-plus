#!/usr/bin/env python3
"""Find structurally similar Kotlin function bodies in Zhihu++.

The output is a review queue. Similarity is based on normalized token shingles,
so every reported pair still needs source inspection.
"""

from __future__ import annotations

import argparse
import bisect
import csv
import re
import sys
from collections import defaultdict
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

TEST_MARKERS = ("/test/", "/androidTest/", "/commonTest/", "/jvmTest/", "/nativeTest/")
SKIP_DIRS = {".git", ".gradle", ".idea", ".worktrees", "build", ".kotlin"}

KOTLIN_KEYWORDS = {
    "as",
    "as?",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "!in",
    "interface",
    "is",
    "!is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",
}

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

TOKEN_RE = re.compile(
    r"""
    [A-Za-z_][A-Za-z0-9_]* |
    \d+(?:\.\d+)? |
    ==|!=|<=|>=|&&|\|\||->|=>|::|\?:|[{}()\[\].,;:+\-*/%<>=!?]
    """,
    re.VERBOSE,
)


@dataclass(frozen=True)
class FunctionBody:
    path: str
    line: int
    name: str
    arity: int
    modifiers: str
    kind: str
    body_lines: int
    body_preview: str
    tokens: tuple[str, ...]
    shingles: frozenset[tuple[str, ...]]


def mask_comments_and_literals(text: str, keep_literal_tokens: bool = True) -> str:
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
                if keep_literal_tokens:
                    out.append(" STR ")
                out.extend("   ")
                i += 3
                state = "string"
                triple = True
                continue
            if char == '"':
                if keep_literal_tokens:
                    out.append(" STR ")
                out.append(" ")
                i += 1
                state = "string"
                triple = False
                continue
            if char == "'":
                if keep_literal_tokens:
                    out.append(" CHAR ")
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


def source_files(root: Path, include_tests: bool) -> list[Path]:
    files: list[Path] = []
    for source_root in DEFAULT_SOURCE_ROOTS:
        base = root / source_root
        if not base.exists():
            continue
        for path in sorted(base.rglob("*.kt")):
            rel = "/" + path.relative_to(root).as_posix()
            if any(part in SKIP_DIRS for part in path.relative_to(root).parts):
                continue
            if not include_tests and any(marker in rel for marker in TEST_MARKERS):
                continue
            files.append(path)
    return files


def normalize_tokens(body: str) -> tuple[str, ...]:
    masked = mask_comments_and_literals(body, keep_literal_tokens=True)
    tokens: list[str] = []
    for token in TOKEN_RE.findall(masked):
        if re.fullmatch(r"\d+(?:\.\d+)?", token):
            tokens.append("NUM")
        elif token in {"STR", "CHAR"}:
            tokens.append(token)
        elif re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", token):
            tokens.append(token if token in KOTLIN_KEYWORDS else "ID")
        else:
            tokens.append(token)
    return tuple(tokens)


def make_shingles(tokens: tuple[str, ...], shingle_size: int) -> frozenset[tuple[str, ...]]:
    if len(tokens) < shingle_size:
        return frozenset()
    return frozenset(tuple(tokens[i : i + shingle_size]) for i in range(len(tokens) - shingle_size + 1))


def parse_functions(
    root: Path,
    files: list[Path],
    min_lines: int,
    min_tokens: int,
    shingle_size: int,
    include_abstract: bool,
) -> list[FunctionBody]:
    functions: list[FunctionBody] = []

    for path in files:
        text = path.read_text(errors="replace")
        masked = mask_comments_and_literals(text, keep_literal_tokens=False)
        starts = line_starts(text)

        for match in FUNCTION_RE.finditer(masked):
            open_pos = masked.find("(", match.start())
            close_pos = matching_paren(masked, open_pos)
            if close_pos < 0:
                continue

            kind, body_start, body_end = body_span(masked, close_pos)
            if kind == "abstract" and not include_abstract:
                continue

            body = text[body_start:body_end]
            body_lines = 0 if kind == "abstract" else max(1, body.count("\n") + 1)
            tokens = normalize_tokens(body)
            if body_lines < min_lines or len(tokens) < min_tokens:
                continue

            shingles = make_shingles(tokens, shingle_size)
            if not shingles:
                continue

            functions.append(
                FunctionBody(
                    path=path.relative_to(root).as_posix(),
                    line=line_number(starts, match.start()),
                    name=match.group("name").strip("`"),
                    arity=parameter_arity(masked[open_pos + 1 : close_pos]),
                    modifiers=" ".join(MODIFIER_RE.findall(match.group("prefix") or "")),
                    kind=kind,
                    body_lines=body_lines,
                    body_preview=" ".join(body.split())[:220],
                    tokens=tokens,
                    shingles=shingles,
                ),
            )

    return functions


def find_similar_pairs(
    functions: list[FunctionBody],
    threshold: float,
    max_bucket_size: int,
    limit: int,
) -> list[tuple[float, int, int, FunctionBody, FunctionBody]]:
    index: dict[tuple[str, ...], list[int]] = defaultdict(list)
    for idx, function in enumerate(functions):
        for shingle in function.shingles:
            index[shingle].append(idx)

    shared_counts: dict[tuple[int, int], int] = defaultdict(int)
    for ids in index.values():
        if len(ids) < 2 or len(ids) > max_bucket_size:
            continue
        for left_pos, left in enumerate(ids):
            for right in ids[left_pos + 1 :]:
                shared_counts[(left, right)] += 1

    pairs: list[tuple[float, int, int, FunctionBody, FunctionBody]] = []
    for (left, right), shared in shared_counts.items():
        left_function = functions[left]
        right_function = functions[right]
        union = len(left_function.shingles) + len(right_function.shingles) - shared
        if union <= 0:
            continue
        score = shared / union
        if score < threshold:
            continue
        pairs.append((score, shared, union, left_function, right_function))

    pairs.sort(
        key=lambda item: (
            -item[0],
            -(item[3].body_lines + item[4].body_lines),
            item[3].path,
            item[3].line,
            item[4].path,
            item[4].line,
        ),
    )
    return pairs[:limit]


def write_pairs(
    pairs: list[tuple[float, int, int, FunctionBody, FunctionBody]],
    output: Path | None,
) -> None:
    header = [
        "score",
        "shared_shingles",
        "union_shingles",
        "tokens_a",
        "tokens_b",
        "body_lines_a",
        "body_lines_b",
        "modifiers_a",
        "name_a",
        "arity_a",
        "path_a",
        "line_a",
        "modifiers_b",
        "name_b",
        "arity_b",
        "path_b",
        "line_b",
        "body_preview_a",
        "body_preview_b",
    ]
    rows = []
    for score, shared, union, left, right in pairs:
        rows.append(
            [
                f"{score:.4f}",
                shared,
                union,
                len(left.tokens),
                len(right.tokens),
                left.body_lines,
                right.body_lines,
                left.modifiers,
                left.name,
                left.arity,
                left.path,
                left.line,
                right.modifiers,
                right.name,
                right.arity,
                right.path,
                right.line,
                left.body_preview,
                right.body_preview,
            ],
        )

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
    parser.add_argument("--include-tests", action="store_true", help="Include test source roots in the scan.")
    parser.add_argument("--threshold", type=float, default=0.82, help="Minimum Jaccard similarity over normalized token shingles.")
    parser.add_argument("--shingle-size", type=int, default=5, help="Number of normalized tokens per shingle.")
    parser.add_argument("--min-lines", type=int, default=4, help="Minimum function body lines to compare.")
    parser.add_argument("--min-tokens", type=int, default=28, help="Minimum normalized token count to compare.")
    parser.add_argument("--max-bucket-size", type=int, default=200, help="Ignore overly common shingles above this bucket size.")
    parser.add_argument("--limit", type=int, default=500, help="Maximum pair rows to output.")
    parser.add_argument("--include-abstract", action="store_true", help="Include abstract/interface declarations.")
    parser.add_argument("--output", type=Path, help="Write TSV output to this path instead of stdout.")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    files = source_files(root, include_tests=args.include_tests)
    functions = parse_functions(
        root=root,
        files=files,
        min_lines=args.min_lines,
        min_tokens=args.min_tokens,
        shingle_size=args.shingle_size,
        include_abstract=args.include_abstract,
    )
    pairs = find_similar_pairs(
        functions=functions,
        threshold=args.threshold,
        max_bucket_size=args.max_bucket_size,
        limit=args.limit,
    )
    write_pairs(pairs, args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
