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

$page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk-lists?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $page -Name 'risk-lists/page'

if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
    throw 'Expected at least one risk list entry'
}

$firstEntry = @($page.data.records)[0]

$detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk-lists/$($firstEntry.id)" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $detail -Name 'risk-lists/detail'

$encodedKeyword = [System.Uri]::EscapeDataString($detail.data.objectValue)
$filtered = Invoke-RestMethod -Uri "$BaseUrl/api/v1/risk-lists?pageNo=1&pageSize=10&keyword=$encodedKeyword" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $filtered -Name 'risk-lists/search'

if ($filtered.data.total -lt 1) {
    throw "Expected keyword search to find $($detail.data.objectValue)"
}

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    totalEntries = $page.data.total
    sampledEntry = "$($detail.data.listType)/$($detail.data.objectType)/$($detail.data.objectValue)"
    sampledStatus = $detail.data.status
    sampledEffect = $detail.data.effectType
    sampledRiskLevel = $detail.data.riskLevel
    keywordMatches = $filtered.data.total
} | Format-List
