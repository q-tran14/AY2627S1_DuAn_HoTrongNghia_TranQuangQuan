"""Reproduce small SUM cases against unmodified official source downloaded separately.
Python standard library only. This is a correctness audit, not a runtime benchmark.
"""
from pathlib import Path
from itertools import combinations
import subprocess
import json
import hashlib
import difflib
import random
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'results/local/official-audit'
OUT.mkdir(parents=True, exist_ok=True)
archive = ROOT / 'tmp/audit/spmf.zip'
official = ROOT / 'tmp/audit/official'
package = 'ca/pfv/spmf/algorithms/frequentpatterns/rminer_sminer/'
with zipfile.ZipFile(archive) as z:
    for name in [package+'AlgoScentedUtilityMiner.java', package+'ResidueMap.java',
                 'ca/pfv/spmf/tools/MemoryLogger.java']:
        z.extract(name, official)
original = (official/package/'AlgoScentedUtilityMiner.java').read_text(encoding='utf-8-sig')
local = (ROOT/'algorithms/sum/AlgoScentedUtilityMiner.java').read_text(encoding='utf-8-sig')
(OUT/'official-vs-local.diff').write_text(''.join(difflib.unified_diff(
    original.splitlines(True), local.splitlines(True), fromfile='official', tofile='local')), encoding='utf-8')
jdk = sorted((Path.home()/'.vscode/extensions').glob('redhat.java-*/jre/*/bin/javac.exe'))[-1].parent
runner = OUT/'AuditOfficial.java'
runner.write_text('''import ca.pfv.spmf.algorithms.frequentpatterns.rminer_sminer.AlgoScentedUtilityMiner;
public class AuditOfficial {
 public static void main(String[] args) throws Exception {
  AlgoScentedUtilityMiner a = new AlgoScentedUtilityMiner();
  long value = a.runAlgorithm(args[0], args[1], Long.parseLong(args[2]), Integer.parseInt(args[3]));
  if (a.endTimestamp == 0) throw new IllegalStateException("Incomplete run");
  System.out.println(value);
 }
}
''', encoding='utf-8')
classes = OUT/'classes'
classes.mkdir(exist_ok=True)
subprocess.run([str(jdk/'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes),
               str(official/package/'AlgoScentedUtilityMiner.java'),
               str(official/package/'ResidueMap.java'),
               str(official/'ca/pfv/spmf/tools/MemoryLogger.java'), str(runner)], check=True)

def parse(lines):
    result = []
    for line in lines:
        if not line.strip() or line[0] in '#%@': continue
        items, total, values = line.split(':')
        row = dict(zip(map(int, items.split()), map(int, values.split())))
        assert sum(row.values()) == int(total)
        result.append(row)
    return result

def active_items(rows, window):
    last = {i: t for t, row in enumerate(rows, 1) for i in row}
    return sorted(i for i in last if len(rows)-last[i] <= window)

def oracle(rows, threshold, window):
    items = active_items(rows, window)
    result = {}
    for size in range(1, len(items)+1):
        for itemset in combinations(items, size):
            u = sum(sum(row[i] for i in itemset) for row in rows if set(itemset) <= row.keys())
            if u >= threshold: result[itemset] = u
    return result

def bounded_prototype(rows, threshold, window):
    """Independent solution sketch: canonical DFS with a nonnegative remaining-utility bound.
    Not a Java SUM replacement and not used for performance comparison.
    """
    items = active_items(rows, window)
    result = {}
    def visit(prefix, start, supporting):
        for j in range(start, len(items)):
            item = items[j]
            projected = [(row, value+row[item]) for row, value in supporting if item in row]
            if not projected: continue
            child = prefix+(item,)
            u = sum(value for row, value in projected)
            if u >= threshold: result[child] = u
            bound = sum(value+sum(row.get(k, 0) for k in items[j+1:]) for row, value in projected)
            if bound >= threshold: visit(child, j+1, projected)
    visit((), 0, [(row, 0) for row in rows])
    return result

cases = [
 ('spmf-example', (ROOT/'datasets/DB_Utility.txt').read_text().splitlines(), 30, 10),
 ('singleton-final', ['1:10:10', '1:5:5'], 10, 10),
 ('expired-singleton', ['1:10:10', '2:1:1', '2:1:1'], 10, 1),
 ('triple-only-high', ['1 2 3:12:4 4 4'], 10, 10),
 ('counter-zero', ['1:10:10', '2:1:1'], 10, 1),
 ('reinduction', ['1:5:5', '2:1:1', '2:1:1', '1:7:7'], 10, 1),
 # Fig.5: q=1,r=2,t=3,w=4,y=5; utilities transcribed exactly from figure.
 ('paper-fig5', ['1 2 3 4:10:4 2 1 3', '3:2:2', '3 5:8:3 5',
                 '1 3 5:11:2 4 5', '4:6:6', '1:1:1'], 11, 3),
 ('min-final-singleton', ['1:1:1', '1:1:1', '1:98:98', '2:5:5'], 1000, 10),
 ('min-never-repeated', ['1:5:5', '2:7:7'], 1000, 10),
]
def printable(result):
    return {' '.join(map(str,k)): v for k,v in sorted(result.items())}
records = []
for name, lines, threshold, window in cases:
    inp, out = OUT/(name+'.txt'), OUT/(name+'.out')
    inp.write_text('\n'.join(lines)+'\n', encoding='utf-8')
    run = subprocess.run([str(jdk/'java.exe'), '-Xmx512m', '-cp', str(classes), 'AuditOfficial',
                          str(inp), str(out), str(threshold), str(window)],
                         capture_output=True, text=True, timeout=20, check=True)
    actual = {}
    for line in out.read_text().splitlines():
        if line.startswith('@') or not line.strip(): continue
        items, u = line.split('#UTIL:')
        key = tuple(sorted(map(int, items.split())))
        assert key not in actual
        actual[key] = int(u)
    rows = parse(lines)
    expected = oracle(rows, threshold, window)
    proposed = bounded_prototype(rows, threshold, window)
    assert proposed == expected
    records.append(dict(name=name, minUtility=threshold, maxR=window,
                        expected=printable(expected), actual=printable(actual),
                        matches=actual==expected, returnedDbMinUtility=int(run.stdout.strip())))

rng = random.Random(20261020)
for _ in range(500):
    rows = []
    for t in range(rng.randint(1, 10)):
        selected = rng.sample(range(1, 8), rng.randint(1, 7))
        rows.append({i:rng.randint(1, 15) for i in selected})
    threshold, window = rng.randint(1, 100), rng.randint(0, 12)
    assert bounded_prototype(rows, threshold, window) == oracle(rows, threshold, window)
summary = dict(source_url='https://www.philippe-fournier-viger.com/spmf/spmf.zip',
               zip_sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),
               semantics='full-history nonnegative utility, each item RC>=0 at final transaction',
               cases=records, prototype_random_cases=500, seed=20261020,
               limitation='Prototype random checks are not official SUM random checks or performance measurements.')
(OUT/'report.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
print(json.dumps(summary, indent=2))
