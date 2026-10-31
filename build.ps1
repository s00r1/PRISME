param(
    [string]$SdkRoot = $(if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }),
    [string]$SdkPlatform = '',
    [string]$BuildTools = '',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$ProjectDirectory = $PSScriptRoot,
    [string]$BuildDirectory = '',
    [string]$OutputFile = 'PRISME_DM100.apk',
    [string]$Keystore = $env:PRISME_KEYSTORE,
    [string]$KeyAlias = 'prisme',
    [switch]$Unsigned
)
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path -LiteralPath $ProjectDirectory).Path
if (!$SdkPlatform) {
    if (!$SdkRoot) { throw 'Set ANDROID_SDK_ROOT, ANDROID_HOME or provide -SdkPlatform and -BuildTools.' }
    $SdkPlatform = Get-ChildItem -LiteralPath (Join-Path $SdkRoot 'platforms') -Directory | Where-Object { $_.Name -match '^android-\d+$' } | Sort-Object { [int]($_.Name -replace '^android-', '') } -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (!$BuildTools) {
    $BuildTools = Get-ChildItem -LiteralPath (Join-Path $SdkRoot 'build-tools') -Directory | Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (!$BuildDirectory) { $BuildDirectory = Join-Path $project '.build' }
if (!$JavaHome) { throw 'Set JAVA_HOME or provide -JavaHome (JDK 17+).' }
$java = Join-Path $JavaHome 'bin/java.exe'
$javac = Join-Path $JavaHome 'bin/javac.exe'
$jar = Join-Path $JavaHome 'bin/jar.exe'
$platformJar = Join-Path $SdkPlatform 'android.jar'
foreach ($tool in @($java, $javac, $jar, $platformJar, (Join-Path $BuildTools 'aapt.exe'), (Join-Path $BuildTools 'zipalign.exe'), (Join-Path $BuildTools 'lib/d8.jar'), (Join-Path $BuildTools 'lib/apksigner.jar'))) {
    if (!(Test-Path -LiteralPath $tool)) { throw "Required build tool missing: $tool" }
}
function Checked([scriptblock]$Action) { & $Action; if ($LASTEXITCODE -ne 0) { throw "Build command failed with exit code $LASTEXITCODE" } }
New-Item -ItemType Directory -Force -Path $BuildDirectory | Out-Null
$build = (Resolve-Path -LiteralPath $BuildDirectory).Path
foreach ($name in @('classes','dex','generated','tmp')) {
    $target = Join-Path $build $name
    if (Test-Path -LiteralPath $target) {
        $resolved = (Resolve-Path -LiteralPath $target).Path
        if (!$resolved.StartsWith($build + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing to clean outside the build directory.' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
    New-Item -ItemType Directory -Path $target | Out-Null
}
$classes = Join-Path $build 'classes'; $dex = Join-Path $build 'dex'; $generated = Join-Path $build 'generated'; $javaTemp = Join-Path $build 'tmp'
Checked { & (Join-Path $BuildTools 'aapt.exe') package -f -M (Join-Path $project 'AndroidManifest.xml') -S (Join-Path $project 'res') -I $platformJar -F (Join-Path $build 'resources.apk') -J $generated }
$sources = @(Get-ChildItem -LiteralPath (Join-Path $project 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem -LiteralPath $generated -Recurse -Filter '*.java' | ForEach-Object FullName)
Checked { & $javac --release 8 -encoding UTF-8 -classpath $platformJar -d $classes @sources }
Checked { & $jar "-J-Djava.io.tmpdir=$javaTemp" --create --file (Join-Path $build 'classes.jar') -C $classes . }
Checked { & $java "-Djava.io.tmpdir=$javaTemp" -cp (Join-Path $BuildTools 'lib/d8.jar') com.android.tools.r8.D8 --min-api 25 --lib $platformJar --output $dex (Join-Path $build 'classes.jar') }
Copy-Item -LiteralPath (Join-Path $build 'resources.apk') -Destination (Join-Path $build 'unsigned.apk') -Force
Push-Location $dex
try { Checked { & (Join-Path $BuildTools 'aapt.exe') add (Join-Path $build 'unsigned.apk') 'classes.dex' } } finally { Pop-Location }
Checked { & (Join-Path $BuildTools 'zipalign.exe') -f -p 4 (Join-Path $build 'unsigned.apk') (Join-Path $build 'aligned.apk') }
if ($Unsigned) { $OutputFile = [IO.Path]::GetFileNameWithoutExtension($OutputFile) + '-unsigned.apk' }
$apk = Join-Path $project $OutputFile
if ($Unsigned) {
    Copy-Item -LiteralPath (Join-Path $build 'aligned.apk') -Destination $apk -Force
    Write-Output 'Unsigned developer build. Sign it before installation; public users should download the signed GitHub Release APK.'
} else {
    if (!$Keystore -or !(Test-Path -LiteralPath $Keystore)) { throw 'Provide your own -Keystore or use -Unsigned. Release signing keys are not in this repository.' }
    if (!$env:PRISME_STORE_PASSWORD -or !$env:PRISME_KEY_PASSWORD) { throw 'Set PRISME_STORE_PASSWORD and PRISME_KEY_PASSWORD in your local environment.' }
    Checked { & $java "-Djava.io.tmpdir=$javaTemp" -jar (Join-Path $BuildTools 'lib/apksigner.jar') sign --ks $Keystore --ks-key-alias $KeyAlias --ks-pass env:PRISME_STORE_PASSWORD --key-pass env:PRISME_KEY_PASSWORD --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled false --v4-signing-enabled false --out $apk (Join-Path $build 'aligned.apk') }
    Checked { & $java -jar (Join-Path $BuildTools 'lib/apksigner.jar') verify --verbose --min-sdk-version 25 $apk }
}
Checked { & (Join-Path $BuildTools 'zipalign.exe') -c 4 $apk }
Write-Output ('SHA-256: ' + (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash)
Write-Output "Output: $apk"
