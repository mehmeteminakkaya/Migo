$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$keystoreDirectory = Join-Path $root "keystore"
$keystorePath = Join-Path $keystoreDirectory "migo-upload.jks"
$propertiesPath = Join-Path $root "keystore.properties"
$keytool = Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe"

if (-not (Test-Path $keytool)) {
    throw "Android Studio keytool bulunamadi. Android Studio kurulumunu kontrol et."
}
if ((Test-Path $keystorePath) -or (Test-Path $propertiesPath)) {
    throw "Release anahtari zaten var. Var olan dosyalari silmeden yeni anahtar olusturulmaz."
}

Write-Host "Migo Play Store upload anahtari hazirlaniyor." -ForegroundColor Cyan
$securePassword = Read-Host "En az 12 karakterli guclu bir sifre gir" -AsSecureString
$password = [System.Net.NetworkCredential]::new("", $securePassword).Password
if ($password.Length -lt 12) {
    throw "Sifre en az 12 karakter olmali."
}

New-Item -ItemType Directory -Path $keystoreDirectory -Force | Out-Null
try {
    & $keytool -genkeypair -v `
        -keystore $keystorePath `
        -storepass $password `
        -keypass $password `
        -alias "migo-upload" `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -dname "CN=Migo, OU=Mobile, O=Migo, L=Istanbul, C=TR"
    if ($LASTEXITCODE -ne 0) { throw "Upload anahtari olusturulamadi." }

    $lines = @(
        "storeFile=keystore/migo-upload.jks",
        "storePassword=$password",
        "keyAlias=migo-upload",
        "keyPassword=$password"
    )
    [System.IO.File]::WriteAllLines(
        $propertiesPath,
        $lines,
        [System.Text.UTF8Encoding]::new($false)
    )
    Write-Host "Release imzasi hazir. keystore klasorunu ve keystore.properties dosyasini guvenli yedekle." -ForegroundColor Green
}
finally {
    $password = $null
    $securePassword.Dispose()
}
