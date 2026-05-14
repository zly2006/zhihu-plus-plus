#!/usr/bin/env python3
from __future__ import annotations

import argparse
import fcntl
import hashlib
import json
import os
import tempfile
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterator

ROOT = Path(__file__).resolve().parents[3]
MEMORY_ROOT = ROOT / ".memory"
LOCK_ROOT = MEMORY_ROOT / ".locks"
AGENT_ALIASES = {
    "picky-user": "picky-user",
    "ui-voyager": "ui-volayor",
    "ui-volayor": "ui-volayor",
}
ISSUE_PREFIX = {
    "picky-user": "PU",
    "ui-volayor": "UV",
}
OPEN_STATUSES = {"open", "accepted", "reopened"}
CLOSED_STATUSES = {"fixed", "rejected", "invalid"}


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def canonical_agent(name: str) -> str:
    try:
        return AGENT_ALIASES[name]
    except KeyError as exc:
        raise SystemExit(f"Unsupported agent: {name}") from exc


def memory_dir(agent: str, date: str) -> Path:
    return MEMORY_ROOT / date / canonical_agent(agent)


def memory_path(agent: str, date: str) -> Path:
    return memory_dir(agent, date) / "issues.json"


def agent_lock_path(agent: str) -> Path:
    return LOCK_ROOT / f"{canonical_agent(agent)}.lock"


@contextmanager
def agent_lock(agent: str, *, exclusive: bool) -> Iterator[None]:
    LOCK_ROOT.mkdir(parents=True, exist_ok=True)
    lock_path = agent_lock_path(agent)
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open("a+", encoding="utf-8") as handle:
        fcntl.flock(handle.fileno(), fcntl.LOCK_EX if exclusive else fcntl.LOCK_SH)
        try:
            yield
        finally:
            fcntl.flock(handle.fileno(), fcntl.LOCK_UN)


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_data(path: Path, data: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        "w",
        delete=False,
        dir=path.parent,
        encoding="utf-8",
    ) as handle:
        handle.write(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=False) + "\n")
        handle.flush()
        os.fsync(handle.fileno())
        temp_path = Path(handle.name)
    os.replace(temp_path, path)


def ensure_day_file(agent: str, date: str) -> Path:
    path = memory_path(agent, date)
    if path.exists():
        return path
    data = {
        "agent": canonical_agent(agent),
        "date": date,
        "issues": [],
    }
    save_data(path, data)
    return path


def iter_issue_files(agent: str) -> list[Path]:
    agent_name = canonical_agent(agent)
    if not MEMORY_ROOT.exists():
        return []
    return sorted(MEMORY_ROOT.glob(f"*/{agent_name}/issues.json"))


def collect_issues(agent: str) -> list[tuple[Path, dict[str, Any], dict[str, Any]]]:
    issues: list[tuple[Path, dict[str, Any], dict[str, Any]]] = []
    for path in iter_issue_files(agent):
        data = load_json(path)
        for issue in data.get("issues", []):
            issues.append((path, data, issue))
    issues.sort(key=lambda item: (item[2].get("updatedAt", ""), item[2]["id"]))
    return issues


def find_issue_by_id(agent: str, issue_id: str) -> tuple[Path, dict[str, Any], dict[str, Any]]:
    for path, data, issue in collect_issues(agent):
        if issue["id"] == issue_id:
            return path, data, issue
    raise SystemExit(f"Issue not found: {issue_id}")


def find_issue_by_fingerprint(
    agent: str,
    fingerprint: str,
    statuses: set[str],
) -> tuple[Path, dict[str, Any], dict[str, Any]] | None:
    for path, data, issue in collect_issues(agent):
        if issue.get("fingerprint") == fingerprint and issue.get("status") in statuses:
            return path, data, issue
    return None


def make_fingerprint(args: argparse.Namespace) -> str:
    raw = "\n".join(
        [
            canonical_agent(args.agent),
            args.title.strip().lower(),
            args.why.strip().lower(),
            args.repro.strip().lower(),
            (args.expected or "").strip().lower(),
        ]
    )
    return hashlib.sha1(raw.encode("utf-8")).hexdigest()


def next_issue_id(agent: str, date: str, issues: list[dict[str, Any]]) -> str:
    prefix = ISSUE_PREFIX[canonical_agent(agent)]
    return f"{prefix}-{date.replace('-', '')}-{len(issues) + 1:03d}"


def cmd_path(args: argparse.Namespace) -> int:
    print(memory_path(args.agent, args.date))
    return 0


def cmd_show_pending(args: argparse.Namespace) -> int:
    with agent_lock(args.agent, exclusive=False):
        pending = [
            issue
            for _, _, issue in collect_issues(args.agent)
            if issue.get("status") in OPEN_STATUSES
        ]
    if not pending:
        print("No pending issues.")
        return 0
    for issue in pending:
        print(f"{issue['id']} [{issue['severity']}] [{issue['persona']}] {issue['title']}")
        print(f"  kind: {issue['kind']}")
        print(f"  openedOn: {issue.get('openedOnDate', 'unknown')}")
        print(f"  lastSeenOn: {issue.get('lastSeenOnDate', issue.get('openedOnDate', 'unknown'))}")
        print(f"  why: {issue['why']}")
        print(f"  repro: {issue['repro']}")
        if issue.get("expected"):
            print(f"  expected: {issue['expected']}")
        print(f"  seenCount: {issue['seenCount']}")
        print(f"  status: {issue['status']}")
        print()
    return 0


def cmd_list(args: argparse.Namespace) -> int:
    with agent_lock(args.agent, exclusive=False):
        issues = [issue for _, _, issue in collect_issues(args.agent)]
    if not issues:
        print("No issues recorded.")
        return 0
    for issue in issues:
        print(f"{issue['id']} [{issue['status']}] {issue['title']}")
    return 0


def cmd_record_issue(args: argparse.Namespace) -> int:
    fingerprint = make_fingerprint(args)
    now = utc_now()
    with agent_lock(args.agent, exclusive=True):
        open_match = find_issue_by_fingerprint(args.agent, fingerprint, OPEN_STATUSES)
        if open_match is not None:
            path, data, issue = open_match
            issue["seenCount"] += 1
            issue["lastSeenAt"] = now
            issue["lastSeenOnDate"] = args.date
            issue["updatedAt"] = now
            issue.setdefault("history", []).append(
                {
                    "at": now,
                    "action": "seen-again",
                    "note": args.note or f"Observed again during the {args.date} UI review.",
                }
            )
            save_data(path, data)
            print(f"{issue['id']} {path}")
            return 0

        if args.reopen:
            closed_match = find_issue_by_fingerprint(args.agent, fingerprint, CLOSED_STATUSES)
            if closed_match is not None:
                path, data, issue = closed_match
                issue["status"] = "reopened"
                issue["seenCount"] += 1
                issue["lastSeenAt"] = now
                issue["lastSeenOnDate"] = args.date
                issue["updatedAt"] = now
                issue.pop("closedAt", None)
                issue.setdefault("history", []).append(
                    {
                        "at": now,
                        "action": "reopened",
                        "note": args.note or f"Reopened during the {args.date} UI review.",
                    }
                )
                save_data(path, data)
                print(f"{issue['id']} {path}")
                return 0

        path = ensure_day_file(args.agent, args.date)
        data = load_json(path)
        issue_id = next_issue_id(args.agent, args.date, data["issues"])
        issue = {
            "id": issue_id,
            "status": "open",
            "fingerprint": fingerprint,
            "title": args.title,
            "kind": args.kind,
            "severity": args.severity,
            "persona": args.persona,
            "why": args.why,
            "repro": args.repro,
            "expected": args.expected or "",
            "openedOnDate": args.date,
            "createdAt": now,
            "updatedAt": now,
            "lastSeenAt": now,
            "lastSeenOnDate": args.date,
            "seenCount": 1,
            "history": [
                {
                    "at": now,
                    "action": "recorded",
                    "note": args.note or "Recorded by UI review agent.",
                }
            ],
        }
        data["issues"].append(issue)
        save_data(path, data)
        print(f"{issue_id} {path}")
        return 0


def cmd_update_status(args: argparse.Namespace) -> int:
    if args.status in {"rejected", "invalid"} and not args.note:
        raise SystemExit(f"--note is required when setting status to {args.status}")
    now = utc_now()
    with agent_lock(args.agent, exclusive=True):
        path, data, issue = find_issue_by_id(args.agent, args.id)
        issue["status"] = args.status
        issue["updatedAt"] = now
        if args.status in CLOSED_STATUSES:
            issue["closedAt"] = now
        else:
            issue.pop("closedAt", None)
            issue["lastSeenAt"] = now
            issue["lastSeenOnDate"] = args.date
        issue.setdefault("history", []).append(
            {
                "at": now,
                "action": f"status:{args.status}",
                "note": args.note or "",
            }
        )
        save_data(path, data)
        print(f"{issue['id']} -> {args.status}")
        return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Persistent memory store for picky-user and UI漫游者 reviews."
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    def add_common(subparser: argparse.ArgumentParser) -> None:
        subparser.add_argument("--agent", required=True, choices=sorted(AGENT_ALIASES))
        subparser.add_argument("--date", required=True, help="Date in YYYY-MM-DD format.")

    path_parser = subparsers.add_parser("path")
    add_common(path_parser)
    path_parser.set_defaults(func=cmd_path)

    show_parser = subparsers.add_parser("show-pending")
    add_common(show_parser)
    show_parser.set_defaults(func=cmd_show_pending)

    list_parser = subparsers.add_parser("list")
    add_common(list_parser)
    list_parser.set_defaults(func=cmd_list)

    record_parser = subparsers.add_parser("record-issue")
    add_common(record_parser)
    record_parser.add_argument("--title", required=True)
    record_parser.add_argument("--kind", required=True)
    record_parser.add_argument("--severity", required=True, choices=["low", "medium", "high"])
    record_parser.add_argument("--persona", required=True)
    record_parser.add_argument("--why", required=True)
    record_parser.add_argument("--repro", required=True)
    record_parser.add_argument("--expected")
    record_parser.add_argument("--note")
    record_parser.add_argument("--reopen", action="store_true")
    record_parser.set_defaults(func=cmd_record_issue)

    update_parser = subparsers.add_parser("update-status")
    add_common(update_parser)
    update_parser.add_argument("--id", required=True)
    update_parser.add_argument(
        "--status",
        required=True,
        choices=["open", "accepted", "reopened", "fixed", "rejected", "invalid"],
    )
    update_parser.add_argument("--note")
    update_parser.set_defaults(func=cmd_update_status)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
