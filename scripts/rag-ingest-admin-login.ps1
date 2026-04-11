# Log in as admin (or any user) and run RAG doc ingest. No JWT copy/paste.
# Usage (recommended: avoids password in shell history):
#   $env:ROOMBAY_LOGIN_EMAIL = "you@gmail.com"
#   $env:ROOMBAY_LOGIN_PASSWORD = "your-password"
#   .\scripts\rag-ingest-admin-login.ps1
#
# Or interactive: .\scripts\rag-ingest-admin-login.ps1
param(
    [string] $BaseUrl = "http://localhost:8082",
    [string] $Email = $env:ROOMBAY_LOGIN_EMAIL
)
$passwordPlain = $env:ROOMBAY_LOGIN_PASSWORD
if (-not $Email) {
    $Email = Read-Host "Email or phone (emailOrPhone)"
}
if (-not $passwordPlain) {
    $sec = Read-Host "Password" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try {
        $passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}
$loginBody = @{ emailOrPhone = $Email; password = $passwordPlain } | ConvertTo-Json
try {
    $auth = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -Body $loginBody -ContentType "application/json"
} catch {
    Write-Host "Login failed: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Host "API:" $_.ErrorDetails.Message
    } elseif ($_.Exception.Response) {
        try {
            $r = $_.Exception.Response.GetResponseStream()
            if ($r) {
                $reader = New-Object System.IO.StreamReader($r)
                Write-Host "API:" $reader.ReadToEnd()
            }
        } catch { }
    }
    Write-Host "(Backend may return HTTP 404 for wrong password; check email/phone and DATABASE_URL matches the DB you promoted.)"
    exit 1
}
$token = $auth.accessToken
if (-not $token) {
    Write-Error "No accessToken in login response."
    exit 1
}
Write-Host "Logged in as $($auth.role). Ingesting docs..."
$r = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/ai/admin/ingest?force=true" -Headers @{
    Authorization = "Bearer $token"
} -ContentType "application/json"
Write-Host ($r | ConvertTo-Json -Depth 6)
