$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$keystoreProperties = Join-Path $root "keystore.properties"
$keystoreFile = Join-Path $root "keystore\migo-upload.jks"
$localProperties = Join-Path $root "local.properties"
$androidStudioJbr = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"

if (-not (Test-Path $keystoreProperties) -or -not (Test-Path $keystoreFile)) {
    throw "Release imzasi hazir degil. Once setup-release-signing.cmd calistir."
}
if (-not (Test-Path $localProperties)) {
    throw "local.properties bulunamadi. Android Studio ile projeyi bir kez ac."
}

$backendLine = Get-Content $localProperties |
    Where-Object { $_ -match '^MIGO_BACKEND_URL=' } |
    Select-Object -First 1
$backendUrl = if ($backendLine) { ($backendLine -split '=', 2)[1].Trim() } else { "" }
if ($backendUrl -notmatch '^https://[^/]+') {
    throw "Gecerli HTTPS MIGO_BACKEND_URL bulunamadi. local.properties dosyasini kontrol et."
}

if (-not $env:JAVA_HOME -and (Test-Path $androidStudioJbr)) {
    $env:JAVA_HOME = $androidStudioJbr
}
if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME bulunamadi. Android Studio kurulumunu kontrol et."
}

$gradle = Join-Path $root "gradlew.bat"
$bundlePath = if ($env:LOCALAPPDATA) {
    Join-Path $env:LOCALAPPDATA "MigoAndroidBuild\app\outputs\bundle\release\app-release.aab"
} else {
    Join-Path $root "app\build\outputs\bundle\release\app-release.aab"
}
$outputDirectory = Join-Path $root "release-output"
$outputPath = Join-Path $outputDirectory "migo-release.aab"

Push-Location $root
try {
    & $gradle :app:lintRelease :app:bundleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Release AAB derlemesi basarisiz." }
}
finally {
    Pop-Location
}

if (-not (Test-Path $bundlePath)) {
    throw "Derleme tamamlandi ancak AAB bulunamadi: $bundlePath"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Copy-Item -LiteralPath $bundlePath -Destination $outputPath -Force

$jarsigner = Join-Path $env:JAVA_HOME "bin\jarsigner.exe"
if (-not (Test-Path $jarsigner)) {
    throw "jarsigner bulunamadi: $jarsigner"
}
& $jarsigner -verify $outputPath
if ($LASTEXITCODE -ne 0) { throw "AAB imza dogrulamasi basarisiz." }

$hash = (Get-FileHash -LiteralPath $outputPath -Algorithm SHA256).Hash
Write-Host "Migo Play Store paketi hazir:" -ForegroundColor Green
Write-Host $outputPath
Write-Host "SHA-256: $hash"
