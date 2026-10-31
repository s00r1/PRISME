param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
foreach ($name in @('run-tests.ps1','run-viewport-tests.ps1','run-gesture-tests.ps1','run-volume-tests.ps1','run-edge-tests.ps1')) {
    & (Join-Path $PSScriptRoot $name) -JavaHome $JavaHome
    if ($LASTEXITCODE -ne 0) { throw "Failed: $name" }
}
Write-Output 'All PRISME test suites passed.'
