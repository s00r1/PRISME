param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$testRoot = $PSScriptRoot
$project = Split-Path $testRoot
$classes = Join-Path $testRoot 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$json = Join-Path $testRoot 'libs\android-json.jar'
if (!(Test-Path -LiteralPath $json)) { throw 'Dépendance de test JSON Android absente ; voir README.md.' }
$sources = @(Get-ChildItem (Join-Path $testRoot 'stubs') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem (Join-Path $testRoot 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += Join-Path $project 'src\fr\dm100\prisme\SearchIndex.java'
$sources += Join-Path $project 'src\fr\dm100\prisme\ChecklistStore.java'
$sources += Join-Path $project 'src\fr\dm100\prisme\DashboardPrefs.java'
$sources += Join-Path $project 'src\fr\dm100\prisme\ThemeSpec.java'
$sources += Join-Path $project 'src\fr\dm100\prisme\ControlCenterPrefs.java'
$sources += Join-Path $project 'src\fr\dm100\prisme\FloatingVolumePrefs.java'
& (Join-Path $JavaHome 'bin\javac.exe') --release 8 -encoding UTF-8 -classpath $json -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation des tests de préférences échouée.' }
& (Join-Path $JavaHome 'bin\java.exe') -classpath ($classes + ';' + $json) fr.dm100.prisme.DashboardPrefsTest
if ($LASTEXITCODE -ne 0) { throw 'Échec des tests de préférences.' }

& (Join-Path $JavaHome 'bin\java.exe') -classpath ($classes + ';' + $json) fr.dm100.prisme.ControlCenterPrefsTest
if ($LASTEXITCODE -ne 0) { throw 'Échec des tests du centre de contrôle.' }

& (Join-Path $JavaHome 'bin\java.exe') -classpath ($classes + ';' + $json) fr.dm100.prisme.FloatingVolumePrefsTest
if ($LASTEXITCODE -ne 0) { throw 'Échec des tests de la pastille intégrée.' }
& (Join-Path $JavaHome 'bin\java.exe') -classpath ($classes + ';' + $json) fr.dm100.prisme.DisplayProfileTest
if ($LASTEXITCODE -ne 0) { throw "Échec des tests des nouveaux profils d’écran." }

& (Join-Path $JavaHome 'bin\java.exe') -classpath ($classes + ';' + $json) fr.dm100.prisme.ExperienceTest
if ($LASTEXITCODE -ne 0) { throw 'Échec des tests des nouveaux outils et des sauvegardes.' }
