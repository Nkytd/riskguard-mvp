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

$health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/health" -Method Get -TimeoutSec 10
Assert-CodeZero -Response $health -Name 'health'

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$login = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
Assert-CodeZero -Response $login -Name 'login'

$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }

$me = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/me" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $me -Name 'auth/me'

$overview = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/overview" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $overview -Name 'dashboard/overview'

$decisionDistribution = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/decision-distribution" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $decisionDistribution -Name 'dashboard/decision-distribution'

$ruleHitRank = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/rule-hit-rank" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $ruleHitRank -Name 'dashboard/rule-hit-rank'

$riskTrend = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/risk-trend" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $riskTrend -Name 'dashboard/risk-trend'

$caseStatistics = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/case-statistics" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $caseStatistics -Name 'dashboard/case-statistics'

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    user = $me.data.username
    role = $me.data.roleCode
    todayDecisionCount = $overview.data.todayDecisionCount
    decisionDistributionItems = @($decisionDistribution.data).Count
    ruleHitRankItems = @($ruleHitRank.data).Count
    riskTrendItems = @($riskTrend.data).Count
    caseTotalCount = $caseStatistics.data.totalCount
} | Format-List
