$ErrorActionPreference = "Stop"
$secureToken = $null
$token = $env:CLOUDFLARE_API_TOKEN

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "Cloudflare API Token'i yapistirip Enter'a bas." -ForegroundColor Cyan
    Write-Host "Guvenlik nedeniyle yapistirirken ekranda karakter veya yildiz gorunmez." -ForegroundColor DarkGray

    while ([string]::IsNullOrWhiteSpace($token)) {
        if ($null -ne $secureToken) {
            $secureToken.Dispose()
        }

        $secureToken = Read-Host "Cloudflare API Token" -AsSecureString
        $token = [System.Net.NetworkCredential]::new("", $secureToken).Password

        if ([string]::IsNullOrWhiteSpace($token)) {
            Write-Host "Token bos birakildi. Lutfen token'i yapistirip Enter'a bas." -ForegroundColor Yellow
        }
    }
}

$env:CLOUDFLARE_API_TOKEN = $token.Trim()
try {
    $deployOutput = & "$PSScriptRoot\cloudflare.cmd" deploy 2>&1
    $deployOutput | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        throw "Cloudflare deploy basarisiz oldu."
    }

    & "$PSScriptRoot\cloudflare.cmd" d1 migrations apply migo-users --remote
    if ($LASTEXITCODE -ne 0) {
        throw "Migo D1 veritabani migration islemi basarisiz oldu."
    }

    $secretOutput = & "$PSScriptRoot\cloudflare.cmd" secret list --format json 2>&1
    if ($LASTEXITCODE -ne 0) {
        $secretOutput | ForEach-Object { Write-Host $_ }
        throw "Cloudflare secret listesi okunamadi."
    }
    if (($secretOutput -join "`n") -notmatch '"name"\s*:\s*"NVIDIA_API_KEY"') {
        throw "Bu Worker icin NVIDIA_API_KEY secret'i eksik."
    }

    $propertiesPath = Join-Path $PSScriptRoot "..\local.properties"
    $backendUrl = [regex]::Match(
        ($deployOutput -join "`n"),
        "https://[A-Za-z0-9.-]+\.workers\.dev"
    ).Value.TrimEnd("/")
    if ([string]::IsNullOrWhiteSpace($backendUrl)) {
        throw "Deploy dogrulanamadi: Wrangler canli Worker adresi dondurmedi."
    }

    $propertyLines = @(Get-Content $propertiesPath)
    $updated = $false
    $propertyLines = $propertyLines | ForEach-Object {
        if ($_ -like "MIGO_BACKEND_URL=*") {
            $updated = $true
            "MIGO_BACKEND_URL=$backendUrl"
        }
        else { $_ }
    }
    if (-not $updated) { $propertyLines += "MIGO_BACKEND_URL=$backendUrl" }
    [System.IO.File]::WriteAllLines(
        $propertiesPath,
        $propertyLines,
        [System.Text.UTF8Encoding]::new($false)
    )

    Write-Host "Canli API dogrulaniyor: $backendUrl" -ForegroundColor Cyan
    $verified = $false
    $lastVerificationError = $null
    for ($attempt = 1; $attempt -le 8; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$backendUrl/health" -TimeoutSec 12
            $catalog = Invoke-RestMethod -Uri "$backendUrl/v1/education/universities?q=istanbul" -TimeoutSec 20
            $legal = Invoke-RestMethod -Uri "$backendUrl/v1/legal" -TimeoutSec 12
            $privacyPage = Invoke-WebRequest -UseBasicParsing -Uri "$backendUrl/privacy" -TimeoutSec 12
            $termsPage = Invoke-WebRequest -UseBasicParsing -Uri "$backendUrl/terms" -TimeoutSec 12
            $aiProbeStatus = 0
            $syncProbeStatus = 0
            try {
                Invoke-WebRequest `
                    -UseBasicParsing `
                    -Method Post `
                    -Uri "$backendUrl/v1/career-analysis" `
                    -Headers @{ "X-Migo-Installation" = "deploy_verification_1234567890" } `
                    -ContentType "application/json; charset=utf-8" `
                    -Body "{}" `
                    -TimeoutSec 20 | Out-Null
            }
            catch {
                if ($null -ne $_.Exception.Response) {
                    $aiProbeStatus = [int]$_.Exception.Response.StatusCode
                }
            }
            try {
                Invoke-WebRequest `
                    -UseBasicParsing `
                    -Method Get `
                    -Uri "$backendUrl/v1/sync/session" `
                    -Headers @{ `
                        "X-Migo-Installation" = "deploy_verification_1234567890"; `
                        "Authorization" = "Bearer abcdefghijklmnopqrstuvwxyzABCDEFGH1234567890_-" `
                    } `
                    -TimeoutSec 20 | Out-Null
            }
            catch {
                if ($null -ne $_.Exception.Response) {
                    $syncProbeStatus = [int]$_.Exception.Response.StatusCode
                }
            }
            $features = @($health.features)
            if ($health.apiVersion -ge 4 -and
                $features -contains "education_catalog" -and
                $features -contains "cv_analysis" -and
                $features -contains "cloud_sync" -and
                $features -contains "legal_pages" -and
                $features -contains "automatic_data_retention" -and
                @($catalog.items).Count -gt 0 -and
                $legal.policyVersion -ge 1 -and
                $privacyPage.StatusCode -eq 200 -and
                $termsPage.StatusCode -eq 200 -and
                $aiProbeStatus -eq 400 -and
                $syncProbeStatus -eq 404) {
                $verified = $true
                break
            }
            $lastVerificationError = "Canli Worker API surumu, D1, katalog, yasal sayfalar veya NVIDIA secret yapilandirmasi gecersiz."
        }
        catch {
            $lastVerificationError = $_.Exception.Message
        }
        Start-Sleep -Seconds 3
    }
    if (-not $verified) {
        throw "Deploy yuklendi ancak canli API dogrulanamadi: $lastVerificationError"
    }
    Write-Host "Deploy ve canli API kontrolu basarili: $backendUrl" -ForegroundColor Green
}
finally {
    Remove-Item Env:CLOUDFLARE_API_TOKEN -ErrorAction SilentlyContinue
    $token = $null
    if ($null -ne $secureToken) {
        $secureToken.Dispose()
    }
}
