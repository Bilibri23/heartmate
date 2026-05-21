# Post-deploy smoke checks (Day 3 / P8)
# Usage: .\scripts\smoke-test-production.ps1 -ApiBase https://api.roombay.com -WebBase https://www.roombay.com

param(
    [string]$ApiBase = "https://api.roombay.com",
    [string]$WebBase = "https://www.roombay.com"
)

$ErrorActionPreference = "Stop"
$failed = 0

function Test-Endpoint {
    param([string]$Name, [string]$Url, [int[]]$OkStatus = @(200))
    Write-Host "  $Name ... " -NoNewline
    try {
        $r = Invoke-WebRequest -Uri $Url -Method GET -UseBasicParsing -TimeoutSec 30
        if ($OkStatus -contains $r.StatusCode) {
            Write-Host "OK ($($r.StatusCode))"
            return $true
        }
        Write-Host "FAIL (status $($r.StatusCode))"
        return $false
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -and ($OkStatus -contains $code)) {
            Write-Host "OK ($code)"
            return $true
        }
        Write-Host "FAIL ($($_.Exception.Message))"
        return $false
    }
}

Write-Host "`nRoomBay production smoke tests`n"

if (-not (Test-Endpoint "API health" "$ApiBase/actuator/health")) { $failed++ }
if (-not (Test-Endpoint "Frontend home" "$WebBase/")) { $failed++ }

# OAuth routes are proxied on Vercel; expect redirect or 302 to Google when unauthenticated
Write-Host "  OAuth start (www proxy) ... " -NoNewline
try {
    $r = Invoke-WebRequest -Uri "$WebBase/oauth2/authorization/google" -Method GET -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue
    Write-Host "OK ($($r.StatusCode))"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -in 302, 303, 307, 308) {
        Write-Host "OK (redirect)"
    } else {
        Write-Host "FAIL ($($_.Exception.Message))"
        $failed++
    }
}

Write-Host "`nManual checks (not automated):"
Write-Host "  - Email register/login"
Write-Host "  - Google OAuth full flow"
Write-Host "  - Listing search, image upload, email"
Write-Host "  - Admin RAG ingest then AI chat"
Write-Host "  - Payment initiate/submit (manual MoMo; platform numbers set in Railway)"
Write-Host "  - DevTools Network: no localhost API calls"

if ($failed -gt 0) {
    Write-Host "`n$failed automated check(s) failed.`n" -ForegroundColor Red
    exit 1
}
Write-Host "`nAutomated checks passed. Complete manual items above.`n" -ForegroundColor Green
exit 0
