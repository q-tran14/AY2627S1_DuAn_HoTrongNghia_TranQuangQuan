"""Bounded pilot on explicitly labelled projections, not a full-dataset benchmark.
Run run.cmd help once before this script to build Java classes.
"""
from pathlib import Path
from collections import Counter
from itertools import combinations
import subprocess
import random
import re
import csv
import json
import hashlib
import os
import shutil

root = Path(__file__).resolve().parents[1]
out = root / "results/local/safe-pilot"
out.mkdir(parents=True, exist_ok=True)
java = None
for variable in ["SUM_JDK", "JAVA_HOME"]:
    candidate = Path(os.environ[variable])/"bin/java.exe" if os.environ.get(variable) else None
    if candidate and candidate.is_file():
        java = candidate
        break
if java is None:
    java = shutil.which("java")
if java is None:
    candidates = sorted((Path.home()/".vscode/extensions").glob("redhat.java-*/jre/*/bin/java.exe"))
    if not candidates:
        raise SystemExit("Java not found: set JAVA_HOME or SUM_JDK")
    java = candidates[-1]

def read_result(path):
    result = {}
    for line in path.read_text().splitlines():
        if not line.strip() or line.startswith("@"): continue
        items, utility = line.split("#UTIL:")
        key = tuple(sorted(map(int, items.split())))
        assert key not in result
        result[key] = int(utility)
    return result

records, manifests = [], []
for dataset in ["foodmart", "mushroom"]:
    source = root / "datasets" / (dataset+".txt")
    original = []
    for line in source.read_text().splitlines():
        if not line.strip() or line[0] in "#%@": continue
        items, total, values = line.split(":")
        row = dict(zip(map(int, items.split()), map(int, values.split())))
        assert len(row) == len(items.split()) and sum(row.values()) == int(total)
        original.append(row)
        if len(original) == 200: break
    counts = Counter(i for row in original for i in row)
    selected = sorted(sorted(counts, key=lambda i: (-counts[i], i))[:12])
    projected = [{i:row[i] for i in selected if i in row} for row in original]
    rows = [row for row in projected if row]
    total = sum(sum(row.values()) for row in rows)
    threshold = max(1, (total*5+99)//100)
    input_path = out / (dataset+"-prefix200-top12.txt")
    input_path.write_text("\n".join(
        " ".join(map(str,row))+":"+str(sum(row.values()))+":"+" ".join(map(str,row.values()))
        for row in rows)+"\n")
    expected = {}
    for size in range(1, len(selected)+1):
        for key in combinations(selected, size):
            utility = sum(sum(row[i] for i in key) for row in rows if set(key)<=row.keys())
            if utility >= threshold: expected[key] = utility
    manifests.append(dict(dataset=dataset, source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                          first_transactions=len(original), retained_transactions=len(rows),
                          selected_items=selected, minUtility=threshold, maxR=100000,
                          expected_huis=len(expected), input_sha256=hashlib.sha256(input_path.read_bytes()).hexdigest()))
    jobs = [(mode, repeat) for mode in ["false", "true"] for repeat in range(1, 4)]
    random.Random(20261021).shuffle(jobs)
    for mode, repeat in jobs:
        output = out / f"{dataset}-ru-{mode}-{repeat}.out"
        command = [str(java), "-Xmx512m", "-cp", str(root/"build/classes"),
                   "experiments.RunSUMSafe", str(input_path), str(output),
                   str(threshold), "100000", mode]
        run = subprocess.run(command, capture_output=True, text=True, timeout=30, check=True)
        (out/f"{dataset}-ru-{mode}-{repeat}.log").write_text(run.stdout+run.stderr)
        actual = read_result(output)
        assert actual == expected, (dataset, mode, repeat)
        stats = dict(re.findall(r"(\w+)=([0-9.]+)", run.stdout))
        records.append(dict(dataset=dataset, pruning=mode, repeat=repeat, **stats))
with (out/"runs.csv").open("w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=records[0].keys())
    writer.writeheader()
    writer.writerows(records)
(out/"manifest.json").write_text(json.dumps(manifests, indent=2))
print("All 12 independent JVM runs match exhaustive oracle.")
print(json.dumps(manifests, indent=2))
for row in records:
    print(row)
