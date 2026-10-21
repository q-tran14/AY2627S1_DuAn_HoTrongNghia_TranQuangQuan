# AY2627S1_DuAn_HoTrongNghia_TranQuangQuan

## Run SUM locally (PowerShell)

```powershell
./scripts/run-sum.ps1
./scripts/run-sum.ps1 -Check
```

The first command compiles the SUM target and runs the small example (8 HUIs).

Experimental Java correction (original mining class unchanged):

```powershell
./scripts/run-sum.ps1 -Safe -Check
./scripts/demo-counterexample.ps1 -Official -CompareSafe
```

See [SUM-Safe implementation and verified results](docs/SUM_SAFE_JAVA.md).
The official demo requires the previously downloaded audit source in `tmp/audit/official`;
omit `-Official` to compare with the local original miner.
The second compares outputs with an exhaustive oracle; it currently exits with
failure because three small cases expose mismatches in the existing miner.
See [Vietnamese source walkthrough](docs/CHAY_VA_DOC_SOURCE.md) for commands,
code flow, input format, and the reproducible findings. Other incomplete algorithm
packages are not included in this SUM build. Generated files go to `build/` and
`results/local/`.



# References:
- Sra, P., Chand, S. A Reinduction-Based Approach for Efficient High Utility Itemset Mining from Incremental Datasets. Data Sci. Eng. 9, 73–87 (2024). https://doi.org/10.1007/s41019-023-00229-4
- https://www.philippe-fournier-viger.com/spmf/ScentedMiner_utility.php
