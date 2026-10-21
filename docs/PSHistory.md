# 20/10/2026 - 00:57
```powershell
PS W:\Study\AY2627S1_DuAn_HoTrongNghia_TranQuangQuan\scripts> Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
PS W:\Study\AY2627S1_DuAn_HoTrongNghia_TranQuangQuan\scripts> .\demo-counterexample.ps1 -Official
ONE TRANSACTION: items 1, 2, 3 (A, B, C), each utility 4.
Input file: datasets/counterexamples/triple-only-high.txt
Input text: 1 2 3:12:4 4 4
Threshold: 10 | maxR: 10 | source: official
Each item just appeared. Recency cannot eliminate any of these items.
All seven nonempty subsets (computed from input):
  {1}: utility=4, HUI=False
  {2}: utility=4, HUI=False
  {1 2}: utility=8, HUI=False
  {3}: utility=4, HUI=False
  {1 3}: utility=8, HUI=False
  {2 3}: utility=8, HUI=False
  {1 2 3}: utility=12, HUI=True
Compiler: C:\Users\ADMIN\.vscode\extensions\redhat.java-1.57.0-win32-x64\jre\25.0.4.1-win32-x86_64\bin\javac.exe
openjdk version "25.0.4.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS, mixed mode, sharing)
=============  Scented Utility Miner ALGORITHM 2.63 - STATS =============
High utility itemsets: 0
Join Count: 0
Execution Time: 2ms
Memory ~ 2.6360321044921875 MB
===================================================
EXPECTED:
  1 2 3 #UTIL: 12
ACTUAL SUM OUTPUT (metadata excluded):
  (empty)
MISMATCH: SUM output differs from exhaustive enumeration.
  Missing: 1 2 3 #UTIL: 12
Raw output: results/local/demo/official-threshold-10.txt
The demo completes normally even on a mismatch; -Check remains the failing regression check.
PS W:\Study\AY2627S1_DuAn_HoTrongNghia_TranQuangQuan\scripts> .\demo-counterexample.ps1 -Official -MinUtility 8
ONE TRANSACTION: items 1, 2, 3 (A, B, C), each utility 4.
Input file: datasets/counterexamples/triple-only-high.txt
Input text: 1 2 3:12:4 4 4
Threshold: 8 | maxR: 10 | source: official
Each item just appeared. Recency cannot eliminate any of these items.
All seven nonempty subsets (computed from input):
  {1}: utility=4, HUI=False
  {2}: utility=4, HUI=False
  {1 2}: utility=8, HUI=True
  {3}: utility=4, HUI=False
  {1 3}: utility=8, HUI=True
  {2 3}: utility=8, HUI=True
  {1 2 3}: utility=12, HUI=True
Compiler: C:\Users\ADMIN\.vscode\extensions\redhat.java-1.57.0-win32-x64\jre\25.0.4.1-win32-x86_64\bin\javac.exe
openjdk version "25.0.4.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS, mixed mode, sharing)
=============  Scented Utility Miner ALGORITHM 2.63 - STATS =============
High utility itemsets: 4
Join Count: 4
Execution Time: 7ms
Memory ~ 2.6360321044921875 MB
===================================================
EXPECTED:
  1 2 #UTIL: 8
  1 3 #UTIL: 8
  2 3 #UTIL: 8
  1 2 3 #UTIL: 12
ACTUAL SUM OUTPUT (metadata excluded):
  1 3 #UTIL: 8
  1 2 #UTIL: 8
  2 3 #UTIL: 8
  1 2 3 #UTIL: 12
MATCH for this input and threshold.
Raw output: results/local/demo/official-threshold-8.txt
The demo completes normally even on a mismatch; -Check remains the failing regression check.
```