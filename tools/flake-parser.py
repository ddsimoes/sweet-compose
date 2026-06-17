#!/usr/bin/env python3
"""Aggregate JUnit XML test results across N runs into a frequency table.

Usage: python3 tools/flake-parser.py build/flake-report
Reads run-{i}-{module}/ directories containing TEST-*.xml files.
Outputs a markdown frequency table to stdout.
"""
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

def main():
    report_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("build/flake-report")
    if not report_dir.exists():
        print(f"Error: {report_dir} not found", file=sys.stderr)
        sys.exit(1)

    # scan for run dirs grouped by module
    module_runs = defaultdict(list)  # module -> [run_dir, ...]
    for d in sorted(report_dir.iterdir()):
        if not d.is_dir() or not d.name.startswith("run-"):
            continue
        # e.g., run-1-sweet -> parts = ["run", "1", "sweet"]
        parts = d.name.split("-", 2)
        if len(parts) == 3:
            module_runs[parts[2]].append(d)

    if not module_runs:
        print("Error: no run-*-module directories found", file=sys.stderr)
        sys.exit(1)

    # total_runs = max number of runs for any module
    total_runs = max(len(v) for v in module_runs.values())

    # test_key -> set of run_nums where test was seen
    test_seen = defaultdict(set)
    # test_key -> set of run_nums where test failed
    test_failures = defaultdict(set)
    # test_key -> first failure message
    test_fail_msg = {}

    for module, run_dirs in sorted(module_runs.items()):
        for run_dir in sorted(run_dirs):
            run_num = int(run_dir.name.split("-")[1])
            for xml_file in sorted(run_dir.glob("TEST-*.xml")):
                try:
                    tree = ET.parse(xml_file)
                    root = tree.getroot()
                    classname = root.get("name", xml_file.stem.replace("TEST-", ""))
                    for tc in root.findall("testcase"):
                        test_name = f"{classname}.{tc.get('name')}"
                        test_key = f"{module}:{test_name}"
                        test_seen[test_key].add(run_num)
                        failure = tc.find("failure")
                        error = tc.find("error")
                        if failure is not None:
                            test_failures[test_key].add(run_num)
                            if test_key not in test_fail_msg:
                                test_fail_msg[test_key] = failure.get("message", "")[:200]
                        elif error is not None:
                            test_failures[test_key].add(run_num)
                            if test_key not in test_fail_msg:
                                test_fail_msg[test_key] = f"ERROR: {error.get('message', '')[:200]}"
                except ET.ParseError as e:
                    print(f"Warning: could not parse {xml_file}: {e}", file=sys.stderr)

    # Categorize
    stable_pass = []
    stable_fail = []
    flaky = []

    for test_key in sorted(test_seen.keys()):
        seen_runs = test_seen[test_key]
        fail_runs = test_failures.get(test_key, set())
        num_pass = len(seen_runs - fail_runs)
        num_fail = len(fail_runs)

        if num_fail == 0:
            stable_pass.append(test_key)
        elif num_pass == 0:
            stable_fail.append((test_key, num_fail, test_fail_msg.get(test_key, "")))
        else:
            flaky.append((test_key, num_pass, num_fail, total_runs, test_fail_msg.get(test_key, "")))

    # Output
    print(f"# Flake Frequency Report")
    print(f"\n- **Total runs:** {total_runs}")
    print(f"- **Unique tests observed:** {len(test_seen)}")
    print(f"- **Always pass:** {len(stable_pass)}")
    print(f"- **Always fail:** {len(stable_fail)}")
    print(f"- **Flaky (pass some, fail some):** {len(flaky)}")
    print()

    if flaky:
        print("## Flaky Tests")
        print()
        print("| Test | Pass | Fail | Failure Message |")
        print("|------|------|------|-----------------|")
        for test_key, npass, nfail, tr, msg in sorted(flaky, key=lambda x: -x[2]):
            print(f"| `{test_key}` | {npass} | {nfail} | {msg[:120]} |")
        print()

    if stable_fail:
        print("## Always Failing")
        print()
        for test_key, nfail, msg in stable_fail:
            print(f"- `{test_key}` ({nfail} runs): {msg[:120]}")
        print()

    print("## Per-Module Summary")
    print()
    for module in sorted(module_runs.keys()):
        mod_tests = [t for t in test_seen if t.startswith(f"{module}:")]
        mod_flaky = [t for t, _, _, _, _ in flaky if t.startswith(f"{module}:")]
        mod_fail = [t for t, _, _ in stable_fail if t.startswith(f"{module}:")]
        print(f"- **{module}**: {len(mod_tests)} tests, {len(mod_flaky)} flaky, {len(mod_fail)} always-failing")


if __name__ == "__main__":
    main()
