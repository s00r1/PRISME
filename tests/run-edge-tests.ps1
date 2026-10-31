param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot
$classes=Join-Path $PSScriptRoot 'edge/classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') --release 8 -encoding UTF-8 -d $classes (Join-Path $project 'src/fr/dm100/prisme/ViewportLayout.java') (Join-Path $PSScriptRoot 'viewport/EdgeToEdgeTest.java')
if($LASTEXITCODE -ne 0){throw 'Compilation bord à bord échouée'}
& (Join-Path $JavaHome 'bin/java.exe') -cp $classes fr.dm100.prisme.EdgeToEdgeTest
if($LASTEXITCODE -ne 0){throw 'Test bord à bord échoué'}
