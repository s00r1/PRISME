param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$classes = Join-Path $PSScriptRoot 'gesture\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& (Join-Path $JavaHome 'bin\javac.exe') --release 8 -encoding UTF-8 -d $classes (Join-Path $project 'src\fr\dm100\prisme\ControlCenterGesture.java') (Join-Path $PSScriptRoot 'fr\dm100\prisme\ControlCenterGestureTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation du test de gestes échouée.' }
& (Join-Path $JavaHome 'bin\java.exe') -cp $classes fr.dm100.prisme.ControlCenterGestureTest
if ($LASTEXITCODE -ne 0) { throw 'Échec du test de gestes.' }
