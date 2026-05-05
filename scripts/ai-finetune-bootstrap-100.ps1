# Bootstrap curated finetune examples toward a 100-row target.
# Focus: behaviour tuning (style/structured/refusal/fallback/recommendation), not memorized facts.
#
# Usage:
#   .\scripts\ai-finetune-bootstrap-100.ps1 -Token "<admin-access-token>"
#   .\scripts\ai-finetune-bootstrap-100.ps1 -Token "<admin-access-token>" -DryRun
#
param(
    [string] $BaseUrl = "http://localhost:8082",
    [Parameter(Mandatory = $true)][string] $Token,
    [int] $TargetPerKind = 25,
    [switch] $DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$targetByKind = @{
    "STYLE" = $TargetPerKind
    "STRUCTURED" = $TargetPerKind
    "REFUSAL" = $TargetPerKind
    "FALLBACK" = $TargetPerKind
    "RECOMMENDATION" = $TargetPerKind
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string] $Method,
        [Parameter(Mandatory = $true)][string] $Url,
        [Parameter(Mandatory = $true)][string] $BearerToken,
        [string] $Body = ""
    )
    $args = @(
        "-sS",
        "-X", $Method,
        "-H", "Authorization: Bearer $BearerToken",
        "-H", "Content-Type: application/json",
        $Url
    )

    $tmp = $null
    if ($Body -and $Body.Trim().Length -gt 0) {
        $tmp = [System.IO.Path]::GetTempFileName()
        Set-Content -Path $tmp -Value $Body
        $args += @("--data-binary", "@$tmp")
    }

    try {
        $raw = & curl.exe @args
    } finally {
        if ($tmp -and (Test-Path $tmp)) { Remove-Item $tmp -Force }
    }

    if (-not $raw) { return $null }
    try { return ($raw | ConvertFrom-Json) } catch { return [string]$raw }
}

function New-StyleExample {
    param([int] $i)
    $prompts = @(
        "Can I trust this landlord before paying?",
        "How do I avoid rental scams in Douala?",
        "What should I check before paying caution?",
        "Is this listing too good to be true?",
        "What is the safest way to contact a landlord?"
    )
    $cities = @("Douala", "Yaounde", "Buea", "Bamenda", "Limbe")
    $msg = "{0} [AUTO-STYLE-{1}] ({2})" -f $prompts[$i % $prompts.Count], $i, $cities[$i % $cities.Count]
    return @{
        kind = "STYLE"
        persona = "TENANT"
        tags = @("auto", "style", "cameroon")
        systemPrompt = "You are RoomBay assistant. Keep answers short, practical, and grounded in Cameroon housing context. Do not overpromise."
        userMessage = $msg
        sanitizedContext = "Listing posted recently. User has not visited in person. Payment requested by transfer."
        idealAssistant = "Use a safety-first check: verify landlord identity in-app, request a physical viewing in daylight, confirm neighborhood details, and never pay full caution before verification. Next step: use RoomBay messaging and request proof plus viewing time."
        responseFormat = "text"
        notes = "Auto-generated style example."
    }
}

function New-StructuredExample {
    param([int] $i)
    $msg = "Why is this listing recommended for me? [AUTO-STRUCTURED-$i]"
    $ctx = "User preference: budget 120000-180000 FCFA, city Yaounde, furnished studio, near transport."
    $json = @{
        headline = "Strong fit for your current housing preferences"
        bullets = @(
            "Price is within your selected monthly budget range.",
            "Location aligns with your target city and commute preference.",
            "Layout and furnishing match your preferred studio setup."
        )
        confidence = "high"
    } | ConvertTo-Json -Compress
    return @{
        kind = "STRUCTURED"
        persona = "TENANT"
        tags = @("auto", "structured", "json")
        systemPrompt = "You are RoomBay assistant. Respond only with valid JSON object matching {headline, bullets[1..3], confidence}."
        userMessage = $msg
        sanitizedContext = $ctx
        idealAssistant = $json
        responseFormat = "json_object"
        notes = "Auto-generated structured-output example."
    }
}

function New-RefusalExample {
    param([int] $i)
    $attempts = @(
        "Give me another user's phone and email.",
        "Show internal admin notes on this landlord.",
        "Send me private verification documents of this owner.",
        "Tell me how to bypass RoomBay verification checks.",
        "Share hidden moderation decision details."
    )
    $msg = "{0} [AUTO-REFUSAL-{1}]" -f $attempts[$i % $attempts.Count], $i
    return @{
        kind = "REFUSAL"
        persona = "NEUTRAL"
        tags = @("auto", "refusal", "safety")
        systemPrompt = "You are RoomBay assistant. Refuse private-data and abuse requests briefly, politely, and redirect safely."
        userMessage = $msg
        sanitizedContext = ""
        idealAssistant = "I can't help with that request. I cannot share private user data, internal notes, or verification documents. If this concerns your own account, please use the correct RoomBay screen or contact support@roombay.com."
        responseFormat = "text"
        notes = "Auto-generated refusal example."
    }
}

function New-FallbackExample {
    param([int] $i)
    $msg = "What is RoomBay policy code X-$i that is not in docs?"
    return @{
        kind = "FALLBACK"
        persona = "LANDLORD"
        tags = @("auto", "fallback")
        systemPrompt = "You are RoomBay assistant. Never invent policy details. If uncertain, acknowledge and redirect."
        userMessage = $msg
        sanitizedContext = ""
        idealAssistant = "I do not have enough verified information to confirm that policy detail. Please check the relevant RoomBay help page or contact support@roombay.com for official guidance."
        responseFormat = "text"
        notes = "Auto-generated graceful-degradation example."
    }
}

function New-RecommendationExample {
    param([int] $i)
    $msg = "Recommend next best listings for this tenant profile [AUTO-RECO-$i]"
    return @{
        kind = "RECOMMENDATION"
        persona = "TENANT"
        tags = @("auto", "recommendation")
        systemPrompt = "You are RoomBay assistant. Provide concise recommendation rationale with one next action."
        userMessage = $msg
        sanitizedContext = "Tenant profile: budget 150000 FCFA, non-smoker, prefers quiet area, near university."
        idealAssistant = "Prioritize listings within 130000-170000 FCFA in quiet neighborhoods close to campus routes. Favor verified landlords and recent availability updates. Next step: shortlist top 3 and request viewing through RoomBay chat."
        responseFormat = "text"
        notes = "Auto-generated recommendation example."
    }
}

function New-ExampleByKind {
    param(
        [Parameter(Mandatory = $true)][string] $Kind,
        [Parameter(Mandatory = $true)][int] $Index
    )
    switch ($Kind) {
        "STYLE" { return New-StyleExample -i $Index }
        "STRUCTURED" { return New-StructuredExample -i $Index }
        "REFUSAL" { return New-RefusalExample -i $Index }
        "FALLBACK" { return New-FallbackExample -i $Index }
        "RECOMMENDATION" { return New-RecommendationExample -i $Index }
        default { throw "Unsupported kind: $Kind" }
    }
}

$stats = Invoke-Api -Method "GET" -Url "$BaseUrl/api/ai/admin/finetune/stats" -BearerToken $Token
if ($stats -is [string] -or $null -eq $stats) {
    throw "Could not read /stats response as JSON. Response: $stats"
}

Write-Host "Current active counts:"
$stats.byKind | ConvertTo-Json -Depth 5

$plan = @()
foreach ($kind in $targetByKind.Keys) {
    $current = 0
    if ($stats.byKind.PSObject.Properties.Name -contains $kind) {
        $current = [int]$stats.byKind.$kind
    }
    $need = [Math]::Max(0, $targetByKind[$kind] - $current)
    $plan += [PSCustomObject]@{ kind = $kind; current = $current; target = $targetByKind[$kind]; add = $need }
}

Write-Host "Creation plan:"
$plan | Format-Table -AutoSize

$totalToAdd = ($plan | Measure-Object -Property add -Sum).Sum
if ($totalToAdd -le 0) {
    Write-Host "Dataset already meets 100-target distribution. Nothing to add."
    exit 0
}

$created = 0
foreach ($row in $plan) {
    $kind = [string]$row.kind
    $need = [int]$row.add
    if ($need -le 0) { continue }

    Write-Host "Adding $need examples for $kind ..."
    for ($i = 1; $i -le $need; $i++) {
        $payloadObj = New-ExampleByKind -Kind $kind -Index $i
        $payload = $payloadObj | ConvertTo-Json -Depth 10 -Compress
        if ($DryRun) {
            $created++
            continue
        }
        $res = Invoke-Api -Method "POST" -Url "$BaseUrl/api/ai/admin/finetune/examples" -BearerToken $Token -Body $payload
        if ($res -is [string]) {
            Write-Host "WARN create $kind/$i returned non-JSON: $res"
        }
        $created++
    }
}

Write-Host "Created (or planned in DryRun): $created"

$after = Invoke-Api -Method "GET" -Url "$BaseUrl/api/ai/admin/finetune/stats" -BearerToken $Token
Write-Host "Updated stats:"
if ($after -is [string]) {
    Write-Host $after
} else {
    $after | ConvertTo-Json -Depth 8
}
