# Ingest docs/*.md into pgvector and rebuild GraphRAG entities/edges (backend must be running).
# After ingest, GET /api/ai/admin/graph-stats (admin JWT) shows entity/edge counts.
#
# Option A — no admin user: set ROOMBAY_AI_INGEST_DEV_KEY in backend/.env, then:
#   .\scripts\rag-ingest-local.ps1 -DevKey $env:ROOMBAY_AI_INGEST_DEV_KEY
#   $env:ROOMBAY_AI_INGEST_DEV_KEY="your-secret" ; .\scripts\rag-ingest-local.ps1
#
# Option B — admin JWT:
#   .\scripts\rag-ingest-local.ps1 -Token "eyJhbG..."
param(
    [string] $BaseUrl = "http://localhost:8082",
    [string] $Token = $env:ROOMBAY_ADMIN_TOKEN,
    [string] $DevKey = $env:ROOMBAY_AI_INGEST_DEV_KEY
)
if ($DevKey) {
    $uri = "$BaseUrl/api/ai/ingest-dev?force=true"
    $r = Invoke-RestMethod -Method Post -Uri $uri -Headers @{
        "X-RoomBay-Ingest-Key" = $DevKey
    } -ContentType "application/json"
    Write-Host "Ingest result:"
    Write-Host ($r | ConvertTo-Json -Depth 5)
    $statsUri = "$BaseUrl/api/ai/graph-stats-dev"
    try {
        $gs = Invoke-RestMethod -Method Get -Uri $statsUri -Headers @{
            "X-RoomBay-Ingest-Key" = $DevKey
        }
        Write-Host "Graph stats:"
        Write-Host ($gs | ConvertTo-Json -Depth 5)
    } catch {
        Write-Host "Graph stats (optional): $($_.Exception.Message)"
    }
    exit 0
}
if (-not $Token) {
    Write-Error "Set ROOMBAY_AI_INGEST_DEV_KEY (see backend/.env.example) or pass -Token (admin JWT)."
    exit 1
}
$uri = "$BaseUrl/api/ai/admin/ingest?force=true"
$r = Invoke-RestMethod -Method Post -Uri $uri -Headers @{ Authorization = "Bearer $Token" } -ContentType "application/json"
Write-Host ($r | ConvertTo-Json -Depth 5)
