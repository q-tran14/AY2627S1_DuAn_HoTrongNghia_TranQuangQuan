"""Summarize preserved pilot measurements into CSV, table, and PGFPlots files.

Python standard library only. The current LaTeX report embeds its own charts;
this script writes separate analysis artifacts and does not edit that report.
"""
from pathlib import Path
import argparse
import csv
import statistics

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "evidence/2026-10-21/safe-pilot/runs.csv"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--output-dir", type=Path, default=ROOT / "results/local/analysis",
                    help="Output folder; relative paths are resolved from the midterm folder")
options = parser.parse_args()
OUT = options.output_dir
if not OUT.is_absolute():
    OUT = ROOT / OUT
OUT.mkdir(parents=True, exist_ok=True)
with SOURCE.open(newline="", encoding="utf-8") as stream:
    rows = list(csv.DictReader(stream))
groups = {(d, p): [r for r in rows if r["dataset"] == d and r["pruning"] == p]
          for d in ["foodmart", "mushroom"] for p in ["false", "true"]}
assert len(rows) == 12 and all(len(v) == 3 for v in groups.values())
summary = []
for (dataset, pruning), records in groups.items():
    assert len({int(r["HUI"]) for r in records}) == 1
    assert len({int(r["joins"]) for r in records}) == 1
    times = [float(r["mine_ms"]) for r in records]
    summary.append(dict(dataset=dataset, pruning=pruning, HUI=records[0]["HUI"],
                        joins=records[0]["joins"], median_ms=statistics.median(times),
                        min_ms=min(times), max_ms=max(times)))
with (OUT / "pilot_summary.csv").open("w", newline="", encoding="utf-8") as stream:
    writer = csv.DictWriter(stream, fieldnames=summary[0].keys())
    writer.writeheader()
    writer.writerows(summary)

table = [r"\begin{table}[htbp]\centering\small",
         r"\caption{Historical pilot: three independent JVM runs per configuration}",
         r"\begin{tabular}{llrrr}",
         r"\hline Projection & RU & HUI & Joins & Median ms\\\hline"]
for row in summary:
    label = "on" if row["pruning"] == "true" else "off"
    table.append(f"{row['dataset'].capitalize()} & {label} & {row['HUI']} & "
                 f"{row['joins']} & {row['median_ms']:.3f}" + r"\\")
table.extend([r"\hline\end{tabular}\label{tab:pilot}", r"\end{table}"])
(OUT / "pilot_table.tex").write_text("\n".join(table) + "\n", encoding="utf-8")

join_plots, time_plots = [], []
for dataset in ["foodmart", "mushroom"]:
    title = dataset.capitalize() + " projection"
    common = (r"width=.46\linewidth,height=5.2cm,scale only axis=false,"
              r"font=\footnotesize,tick label style={font=\scriptsize},"
              r"title={" + title + r"},ymin=0,xtick={0,1},xticklabels={RU off,RU on},"
              r"xmin=-.5,xmax=1.5,enlarge x limits=false,axis lines=left,")
    joins = [int(groups[dataset, p][0]["joins"]) for p in ["false", "true"]]
    join_plots.extend([r"\begin{tikzpicture}\begin{axis}[" + common
                       + f"ymax={max(joins)*1.25:.2f}," + r"ylabel={Attempted joins},ybar,bar width=12pt,nodes near coords]",
                       r"\addplot[draw=blue!55!black,fill=blue!40] coordinates {(0," + str(joins[0]) + r")};",
                       r"\addplot[draw=green!45!black,fill=green!40] coordinates {(1," + str(joins[1]) + r")};",
                       r"\end{axis}\end{tikzpicture}"])
    time_plots.append(r"\begin{tikzpicture}\begin{axis}[" + common + r"ylabel={Mining time (ms)}]")
    for x, pruning in enumerate(["false", "true"]):
        values = [float(r["mine_ms"]) for r in groups[dataset, pruning]]
        color = "blue!55!black" if x == 0 else "green!45!black"
        coords = " ".join(f"({x+offset:.2f},{value:.3f})" for offset, value in zip([-.05, 0, .05], values))
        median = statistics.median(values)
        time_plots.append(r"\addplot[only marks,mark=*,color=" + color + r"] coordinates {" + coords + r"};")
        time_plots.append(r"\addplot[no marks,very thick,color=" + color + r"] coordinates {"
                          + f"({x-.16:.2f},{median:.3f}) ({x+.16:.2f},{median:.3f})" + r"};")
    time_plots.append(r"\end{axis}\end{tikzpicture}")
    if dataset == "foodmart":
        join_plots.append(r"\hfill")
        time_plots.append(r"\hfill")
(OUT / "pilot_joins.tex").write_text("\n".join(join_plots) + "\n", encoding="utf-8")
(OUT / "pilot_times.tex").write_text("\n".join(time_plots) + "\n", encoding="utf-8")
print("Generated pilot_summary.csv, pilot_table.tex, pilot_joins.tex and pilot_times.tex from 12 recorded rows.")

print("Analysis output:", OUT)
