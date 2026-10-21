param(
    [string]$InputFile = 'datasets/DB_Utility.txt',
    [string]$OutputFile = 'results/local/sum-example.txt',
    [long]$MinUtility = 30,
    [int]$MaxR = 10,
    [string]$JdkHome = '',
    [switch]$Check,
    [switch]$Official,
    [switch]$Safe,
    [switch]$NoPrune
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    # Prefer an explicit JDK, then JAVA_HOME/PATH, then the VS Code bundled JDK.
    if ($JdkHome) {
        $compiler = Join-Path $JdkHome 'bin/javac.exe'
    } elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
        $compiler = Join-Path $env:JAVA_HOME 'bin/javac.exe'
    } elseif (Get-Command javac -ErrorAction SilentlyContinue) {
        $compiler = (Get-Command javac).Source
    } else {
        $extensionRoot = Join-Path $env:USERPROFILE '.vscode/extensions'
        $compiler = Get-ChildItem "$extensionRoot/redhat.java-*/jre/*/bin/javac.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    if (!$compiler -or !(Test-Path $compiler)) { throw 'JDK not found. Pass -JdkHome with a JDK directory.' }
    $java = Join-Path (Split-Path $compiler -Parent) 'java.exe'
    Write-Host "Compiler: $compiler"
    & $java -version
    if ($LASTEXITCODE -ne 0) { throw 'Java runtime failed' }
    if ($Official -and $Check) { throw 'Use -Official for a single run; -Check tests the local source.' }
    if ($Official -and $Safe) { throw 'Choose either -Official or -Safe' }
    if ($NoPrune -and !$Safe) { throw '-NoPrune requires -Safe' }
    $classDirectory = 'build/classes'
    # Compile the SUM target only. The unrelated UP-Growth checkout lacks dependencies.
    $sources = @('algorithms/sum/AlgoScentedUtilityMiner.java',
        'algorithms/sum/ResidueMap.java', 'algorithms/sum/MainTestAlgoScentedUtilityMiner.java',
        'tools/MemoryLogger.java', 'experiments/CheckSumCorrectness.java',
        'algorithms/sum/AlgoSUMSafe.java', 'experiments/RunSUMSafe.java', 'experiments/CheckSUMSafe.java')
    if ($Official) {
        $officialRoot = 'tmp/audit/official/ca/pfv/spmf'
        $sources = @("$officialRoot/algorithms/frequentpatterns/rminer_sminer/AlgoScentedUtilityMiner.java",
            "$officialRoot/algorithms/frequentpatterns/rminer_sminer/ResidueMap.java",
            "$officialRoot/tools/MemoryLogger.java", 'experiments/RunOfficialSum.java')
        foreach ($source in $sources) {
            if (!(Test-Path -LiteralPath $source)) { throw "Official audit source not found: $source" }
        }
        if ($MinUtility -le 0 -or $MaxR -lt 0) { throw 'Invalid minimum utility or maxR' }
        if (!(Test-Path -LiteralPath $InputFile -PathType Leaf)) { throw "Input missing: $InputFile" }
        $inputResolved = [IO.Path]::GetFullPath($InputFile)
        $outputResolved = [IO.Path]::GetFullPath($OutputFile)
        if ($inputResolved -eq $outputResolved) { throw 'Input and output must differ' }
        New-Item -ItemType Directory -Force (Split-Path $outputResolved -Parent) | Out-Null
        $classDirectory = 'build/official/classes'
    }
    New-Item -ItemType Directory -Force $classDirectory | Out-Null
    & $compiler -encoding UTF-8 -d $classDirectory @sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    if ($Official) {
        & $java -Xmx512m -cp $classDirectory experiments.RunOfficialSum $InputFile $OutputFile $MinUtility $MaxR
    } elseif ($Safe -and $Check) {
        & $java -Xmx512m -cp $classDirectory experiments.CheckSUMSafe
    } elseif ($Safe) {
        & $java -Xmx512m -cp $classDirectory experiments.RunSUMSafe $InputFile $OutputFile $MinUtility $MaxR (!$NoPrune).ToString().ToLowerInvariant()
    } elseif ($Check) {
        & $java -Xmx512m -cp build/classes experiments.CheckSumCorrectness
    } else {
        & $java -Xmx512m -cp build/classes algorithms.sum.MainTestAlgoScentedUtilityMiner $InputFile $OutputFile $MinUtility $MaxR
    }
    if ($LASTEXITCODE -ne 0) { throw "Java command failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}
