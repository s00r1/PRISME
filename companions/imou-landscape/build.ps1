# Forward explicit build options to the shared Windows build script.
& (Join-Path $PSScriptRoot '../../build.ps1') -ProjectDirectory $PSScriptRoot -OutputFile 'IMOU_PAYSAGE_DM100.apk' @args
