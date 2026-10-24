# CLI and packaging recheck - 24 October 2026

- JDK: Temurin 25.0.4.1; compilation uses --release 25.
- Batch checks: 9 fixed cases and 2,759 prefixes; retained-state checks: 7,036 snapshots.
- 32 separate CLI invocations cover help aliases, demos, custom paths, pruning on/off, fixed/paper thresholds, and invalid arguments.
- Single-run and incremental mappings were independently checked by Python exhaustive enumeration on the small example inputs.
- A separate project copy with spaces in its path compiles and runs from another working directory.
- Packaging was executed in the copy with and without report/: evidence .log/.out contents survive, while LaTeX caches are excluded only from report/.
- Analysis runs with default and custom output folders; its four medians match preserved CSV values.
- The actual dist/SUM_midterm.zip was not replaced. These checks do not establish a speedup or full-dataset benchmark.

Current source SHA-256:
- src/algorithms/sum/AlgoSUMSafe.java: 3d114704a92ccadeb5fff7e1f67d83ba44823651d55ddf4144969ee3473ed28f
- src/experiments/StudyCLI.java: 62ebf305b358d22961a3da4de2518425ebae332b117dc431251a81fefb478e71
- src/experiments/RunIncrementalSUMSafe.java: ae1b646776a282c95d570fcf06c1acfae5c1a5be9a2ae03076fdf1232e492052
- src/experiments/PackageSubmission.java: a0463370d86f350b5f8f48bddfbc9e5c505af46388b7730216af7bf9133ade7e

Additional worked example: original SUM and SUM-Safe were run on datasets/examples/two-transactions.txt at minUtility=8, maxR=10; both returned exactly {1,3}:9.
