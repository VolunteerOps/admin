param([string]$DeviceSerial, [switch]$Rebuild)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$config = Get-Content -LiteralPath (Join-Path $projectRoot 'androidApp\build.gradle.kts') -Raw
$appId = [regex]::Match($config, 'applicationId\s*=\s*"([^"]+)"').Groups[1].Value
$version = [regex]::Match($config, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $appId -or -not $version) { throw 'Cannot read the Android application ID and version.' }
$sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
$adb = $null
foreach ($candidate in $sdkCandidates) {
    if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'platform-tools\adb.exe'))) {
        $adb = Join-Path $candidate 'platform-tools\adb.exe'; break
    }
}
if (-not $adb) { throw 'Set ANDROID_HOME to your Android SDK, including platform-tools.' }
$deviceLines = & $adb devices
if ($LASTEXITCODE -ne 0) { throw 'ADB could not enumerate connected devices.' }
$devices = @($deviceLines | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
if ($DeviceSerial) {
    if ($DeviceSerial -notin $devices) { throw "Device '$DeviceSerial' is not connected and authorized. Check adb devices." }
} elseif ($devices.Count -eq 1) { $DeviceSerial = $devices[0] }
else { throw 'Connect one authorized Android device or pass -DeviceSerial from adb devices.' }

Write-Output "Building $appId $version from $projectRoot"
$buildArgs = @(':androidApp:assembleDebug', '--console=plain')
if ($Rebuild) { $buildArgs += @('--rerun-tasks', '--no-build-cache') }
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @buildArgs
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. No APK was installed.' }
} finally { Pop-Location }
$apk = Join-Path $projectRoot 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'
if (-not (Test-Path -LiteralPath $apk)) { throw "Missing build output: $apk" }
Write-Output "APK SHA-256: $((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash)"
& $adb -s $DeviceSerial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Install failed. The existing app and its data have been retained.' }
$installed = & $adb -s $DeviceSerial shell dumpsys package $appId
if ($LASTEXITCODE -ne 0 -or -not ($installed -match ('versionName=' + [regex]::Escape($version) + '\s*$'))) {
    throw 'Installed version did not match the APK that was built.'
}
& $adb -s $DeviceSerial shell am force-stop $appId
& $adb -s $DeviceSerial shell am start -n "$appId/.MainActivity"
if ($LASTEXITCODE -ne 0) { throw 'The updated APK installed, but its activity did not launch.' }
Write-Output "Launched $appId $version on $DeviceSerial. Application data was preserved."
