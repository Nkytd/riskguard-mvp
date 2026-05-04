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

$page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cases?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $page -Name 'cases/page'

if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
    $suffix = Get-Date -Format 'yyyyMMddHHmmss'
    $seedBody = @{
        requestNo = "CASEACCEPT$suffix"
        eventType = 'PAYMENT'
        userId = "U-CASE-ACCEPT-$suffix"
        deviceId = 'D90001'
        ip = '192.168.1.10'
        amount = 1299.00
        bizId = "ORDER-CASE-ACCEPT-$suffix"
        scene = 'APP'
        eventTime = '2026-05-01 10:35:00'
        extra = @{
            payMethod = 'BANK_CARD'
        }
    } | ConvertTo-Json -Depth 5

    $seed = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk/decisions" -Method Post -ContentType 'application/json' -Body $seedBody -TimeoutSec 10
    Assert-CodeZero -Response $seed -Name 'cases/seed-decision'

    if ([string]::IsNullOrWhiteSpace($seed.data.caseNo)) {
        throw 'Expected seed REVIEW decision to create a case'
    }

    $page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cases?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
    Assert-CodeZero -Response $page -Name 'cases/page-after-seed'

    if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
        throw 'Expected at least one case after seed'
    }
}

$firstCase = @($page.data.records)[0]

$detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cases/$($firstCase.caseNo)" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $detail -Name 'cases/detail'

$operations = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cases/$($firstCase.caseNo)/operations" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $operations -Name 'cases/operations'

$encodedUserId = [System.Uri]::EscapeDataString($detail.data.userId)
$filtered = Invoke-RestMethod -Uri "$BaseUrl/api/v1/cases?pageNo=1&pageSize=10&userId=$encodedUserId" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $filtered -Name 'cases/search-user'

if ($filtered.data.total -lt 1) {
    throw "Expected user search to find $($detail.data.userId)"
}

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    totalCases = $page.data.total
    sampledCase = $detail.data.caseNo
    sampledUser = $detail.data.userId
    sampledStatus = $detail.data.status
    sampledRiskLevel = $detail.data.riskLevel
    sampledScore = $detail.data.riskScore
    operationRecords = @($operations.data).Count
    userMatches = $filtered.data.total
} | Format-List
