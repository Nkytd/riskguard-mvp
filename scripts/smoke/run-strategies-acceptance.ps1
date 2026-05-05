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

$page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/strategies?pageNo=1&pageSize=10&status=ENABLED" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $page -Name 'strategies/page'

if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
    throw 'Expected at least one risk strategy'
}

$firstStrategy = @($page.data.records)[0]

$detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/strategies/$($firstStrategy.id)" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $detail -Name 'strategies/detail'

$rules = Invoke-RestMethod -Uri "$BaseUrl/api/v1/strategies/$($firstStrategy.id)/rules" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $rules -Name 'strategies/rules'

$versions = Invoke-RestMethod -Uri "$BaseUrl/api/v1/strategies/$($firstStrategy.id)/versions" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $versions -Name 'strategies/versions'

$snapshotRuleCount = 0
if (@($versions.data).Count -gt 0) {
    $snapshot = @($versions.data)[0].ruleSnapshot | ConvertFrom-Json
    $snapshotRuleCount = @($snapshot).Count
}

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    totalStrategies = $page.data.total
    sampledStrategy = $detail.data.strategyCode
    sampledStatus = $detail.data.status
    sampledVersion = $detail.data.version
    boundRuleRecords = @($rules.data).Count
    versionRecords = @($versions.data).Count
    snapshotRuleCount = $snapshotRuleCount
} | Format-List
