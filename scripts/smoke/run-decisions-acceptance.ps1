param(
    [string] $BaseUrl = 'http://localhost:8080',
    [string] $Username = 'admin',
    [string] $Password = 'RiskGuard@123456'
)

$ErrorActionPreference = 'Stop'

function Assert-CodeZero {
    param(
        [Parameter(Mandatory = $true)] $Response,
        [Parameter(Mandatory = $true)] [string] $Name
    )

    if ($Response.code -ne 0) {
        throw "$Name failed with code=$($Response.code), message=$($Response.message)"
    }
}

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$login = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
Assert-CodeZero -Response $login -Name 'login'

$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }

$page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $page -Name 'decisions/page'

if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
    $suffix = Get-Date -Format 'yyyyMMddHHmmss'
    $seedBody = @{
        requestNo = "DECISIONACCEPT$suffix"
        eventType = 'LOGIN'
        userId = "U-DECISION-ACCEPT-$suffix"
        deviceId = "D-DECISION-ACCEPT-$suffix"
        ip = '203.0.113.10'
        amount = 0
        bizId = "LOGIN-ACCEPT-$suffix"
        scene = 'WEB'
        eventTime = '2026-05-01 10:30:00'
        extra = @{}
    } | ConvertTo-Json -Depth 5

    $seed = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions" -Method Post -ContentType 'application/json' -Body $seedBody -TimeoutSec 10
    Assert-CodeZero -Response $seed -Name 'decisions/seed'

    $page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
    Assert-CodeZero -Response $page -Name 'decisions/page-after-seed'

    if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
        throw 'Expected at least one decision log after seed'
    }
}

$firstDecision = @($page.data.records)[0]

$detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions/$($firstDecision.decisionNo)" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $detail -Name 'decisions/detail'

$hitRules = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions/$($firstDecision.decisionNo)/hit-rules" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $hitRules -Name 'decisions/hit-rules'

$encodedUserId = [System.Uri]::EscapeDataString($detail.data.userId)
$filtered = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions?pageNo=1&pageSize=10&userId=$encodedUserId" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $filtered -Name 'decisions/search-user'

if ($filtered.data.total -lt 1) {
    throw "Expected user search to find $($detail.data.userId)"
}

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    totalDecisions = $page.data.total
    sampledDecision = $detail.data.decisionNo
    sampledUser = $detail.data.userId
    sampledOutcome = $detail.data.decision
    sampledRiskLevel = $detail.data.riskLevel
    sampledScore = $detail.data.riskScore
    hitRuleRecords = @($hitRules.data).Count
    userMatches = $filtered.data.total
} | Format-List
