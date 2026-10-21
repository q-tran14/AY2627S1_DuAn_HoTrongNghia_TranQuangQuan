param(
    [ValidateRange(1, 2147483647)][int]$MinUtility = 10,
    [switch]$Official,
    [string]$JdkHome = '',
    [switch]$CompareSafe
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $inputFile = 'datasets/counterexamples/triple-only-high.txt'
    $variant = if ($Official) { 'official' } else { 'local' }
    $outputFile = "results/local/demo/$variant-threshold-$MinUtility.txt"
    $raw = (Get-Content -LiteralPath $inputFile -Raw).Trim()
    Write-Host 'ONE TRANSACTION: items 1, 2, 3 (A, B, C), each utility 4.'
    Write-Host "Input file: $inputFile"
    Write-Host "Input text: $raw"
    Write-Host "Threshold: $MinUtility | maxR: 10 | source: $variant"
    Write-Host 'Each item just appeared. Recency cannot eliminate any of these items.'

    # Independent enumeration, using only the input file, not SUM internals.
    $fields = $raw.Split(':')
    if ($fields.Count -ne 3) { throw 'Expected one SPMF transaction' }
    $items = @($fields[0].Split(' ') | ForEach-Object { [int]$_ })
    $utilities = @($fields[2].Split(' ') | ForEach-Object { [long]$_ })
    if ($items.Count -ne 3 -or $utilities.Count -ne 3 -or
        ($utilities | Measure-Object -Sum).Sum -ne [long]$fields[1]) {
        throw 'This demo expects one valid transaction with three distinct items'
    }
    $expected = @()
    Write-Host 'All seven nonempty subsets (computed from input):'
    for ($mask = 1; $mask -lt 8; $mask++) {
        $selected = @()
        [long]$utility = 0
        for ($j = 0; $j -lt 3; $j++) {
            if (($mask -band (1 -shl $j)) -ne 0) {
                $selected += $items[$j]
                $utility += $utilities[$j]
            }
        }
        $key = ($selected | Sort-Object) -join ' '
        $qualifies = $utility -ge $MinUtility
        Write-Host ("  {{{0}}}: utility={1}, HUI={2}" -f $key, $utility, $qualifies)
        if ($qualifies) { $expected += "$key #UTIL: $utility" }
    }
    & "$PSScriptRoot/run-sum.ps1" -InputFile $inputFile -OutputFile $outputFile `
        -MinUtility $MinUtility -MaxR 10 -JdkHome $JdkHome -Official:$Official
    $actual = @(Get-Content -LiteralPath $outputFile | Where-Object { $_ -match '#UTIL:' } |
        ForEach-Object {
            $parts = $_ -split '#UTIL:'
            $key = (($parts[0].Trim() -split '\s+' | ForEach-Object { [int]$_ }) | Sort-Object) -join ' '
            "$key #UTIL: $([long]$parts[1].Trim())"
        })
    $missing = @($expected | Where-Object { $_ -notin $actual })
    $unexpected = @($actual | Where-Object { $_ -notin $expected })
    Write-Host 'EXPECTED:'
    if ($expected.Count) { $expected | ForEach-Object { Write-Host "  $_" } } else { Write-Host '  (empty)' }
    Write-Host 'ACTUAL SUM OUTPUT (metadata excluded):'
    if ($actual.Count) { $actual | ForEach-Object { Write-Host "  $_" } } else { Write-Host '  (empty)' }
    if ($missing.Count -or $unexpected.Count) {
        Write-Host 'MISMATCH: SUM output differs from exhaustive enumeration.' -ForegroundColor Yellow
        $missing | ForEach-Object { Write-Host "  Missing: $_" }
        $unexpected | ForEach-Object { Write-Host "  Unexpected: $_" }
    } else {
        Write-Host 'MATCH for this input and threshold.' -ForegroundColor Green
    }
    Write-Host "Raw output: $outputFile"
    if ($CompareSafe) {
        $safeOutput = "results/local/demo/safe-threshold-$MinUtility.txt"
        & "$PSScriptRoot/run-sum.ps1" -Safe -InputFile $inputFile -OutputFile $safeOutput `
            -MinUtility $MinUtility -MaxR 10 -JdkHome $JdkHome
        $safeActual = @(Get-Content -LiteralPath $safeOutput | Where-Object { $_ -match '#UTIL:' } |
            ForEach-Object {
                $parts = $_ -split '#UTIL:'
                $key = (($parts[0].Trim() -split '\s+' | ForEach-Object { [int]$_ }) | Sort-Object) -join ' '
                "$key #UTIL: $([long]$parts[1].Trim())"
            })
        Write-Host 'SUM-SAFE OUTPUT:'
        if ($safeActual.Count) { $safeActual | ForEach-Object { Write-Host "  $_" } }
        else { Write-Host '  (empty)' }
        if (@($expected | Where-Object { $_ -notin $safeActual }).Count -or
            @($safeActual | Where-Object { $_ -notin $expected }).Count -or
            $safeActual.Count -ne $expected.Count) { throw 'SUM-Safe does not match oracle' }
        Write-Host 'PASS: SUM-Safe matches exhaustive enumeration.' -ForegroundColor Green
    }
    Write-Host 'The demo completes normally even on a mismatch; -Check remains the failing regression check.'
} finally {
    Pop-Location
}
