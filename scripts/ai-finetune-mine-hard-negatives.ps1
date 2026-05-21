# Extract draft hard negatives from recent AI chat logs via admin API.
#
# Usage:
#   .\scripts\ai-finetune-mine-hard-negatives.ps1 -Token "<admin-access-token>"
#   .\scripts\ai-finetune-mine-hard-negatives.ps1 -Token "<token>" -RecentLimit 400 -MaxDrafts 80
#
param(
    [string] $BaseUrl = "http://localhost:8082",
    [Parameter(Mandatory = $true)][string] $Token,
    [int] $RecentLimit = 200,
    [int] $MaxDrafts = 50
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($RecentLimit -lt 1) { throw "RecentLimit must be >= 1" }
if ($MaxDrafts -lt 1) { throw "MaxDrafts must be >= 1" }

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path ".\tmp" "ai-hard-negatives-$timestamp"
New-Item -ItemType Directory -Path $outDir | Out-Null

$url = "$BaseUrl/api/ai/admin/finetune/hard-negatives?recentLimit=$RecentLimit&maxDrafts=$MaxDrafts"
$raw = & curl.exe -sS -X GET `
    -H "Authorization: Bearer $Token" `
    -H "Content-Type: application/json" `
    $url

if (-not $raw) {
    throw "Empty response from hard-negatives endpoint."
}

$rows = $raw | ConvertFrom-Json
if ($rows -isnot [System.Collections.IEnumerable]) {
    throw "Unexpected response shape from hard-negatives endpoint."
}

$jsonPath = Join-Path $outDir "hard-negatives.json"
$rows | ConvertTo-Json -Depth 12 | Set-Content -Path $jsonPath

$jsonlPath = Join-Path $outDir "hard-negatives-draft.jsonl"
if (Test-Path $jsonlPath) { Remove-Item $jsonlPath -Force }

foreach ($r in $rows) {
    $line = [ordered]@{
        chatLogId = $r.chatLogId
        reason = $r.reason
        suggestedKind = $r.suggestedKind
        userMessage = $r.userMessage
        assistantAnswer = $r.assistantAnswer
        draftIdealAssistant = $r.draftIdealAssistant
        suggestedResponseFormat = $r.suggestedResponseFormat
    } | ConvertTo-Json -Compress
    Add-Content -Path $jsonlPath -Value $line
}

$count = @($rows).Count
Write-Host "Drafts saved:"
Write-Host " - $jsonPath"
Write-Host " - $jsonlPath"
Write-Host "Total drafts: $count"
