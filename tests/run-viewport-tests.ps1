param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$classes = Join-Path $PSScriptRoot 'viewport\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& (Join-Path $JavaHome 'bin\javac.exe') --release 8 -encoding UTF-8 -d $classes (Join-Path $project 'src\fr\dm100\prisme\ViewportLayout.java') (Join-Path $PSScriptRoot 'viewport\ViewportLayoutTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation du test de géométrie échouée.' }
& (Join-Path $JavaHome 'bin\java.exe') -cp $classes fr.dm100.prisme.ViewportLayoutTest
if ($LASTEXITCODE -ne 0) { throw 'Échec du test de géométrie.' }
