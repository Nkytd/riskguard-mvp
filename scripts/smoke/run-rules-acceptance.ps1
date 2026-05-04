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

$page = Invoke-RestMethod -Uri "$BaseUrl/api/v1/rules?pageNo=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $page -Name 'rules/page'

if ($page.data.total -lt 1 -or @($page.data.records).Count -lt 1) {
    throw 'Expected at least one risk rule'
}

$firstRule = @($page.data.records)[0]

$detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/rules/$($firstRule.id)" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $detail -Name 'rules/detail'

$versions = Invoke-RestMethod -Uri "$BaseUrl/api/v1/rules/$($firstRule.id)/versions" -Method Get -Headers $headers -TimeoutSec 10
Assert-CodeZero -Response $versions -Name 'rules/versions'

$validateBody = @{
    expression = $detail.data.expression
} | ConvertTo-Json
$validation = Invoke-RestMethod -Uri "$BaseUrl/api/v1/rules/validate-expression" -Method Post -ContentType 'application/json' -Headers $headers -Body $validateBody -TimeoutSec 10
Assert-CodeZero -Response $validation -Name 'rules/validate-expression'

if (-not $validation.data.valid) {
    throw "Expected expression to be valid: $($validation.data.message)"
}

[PSCustomObject]@{
    status = 'PASS'
    baseUrl = $BaseUrl
    totalRules = $page.data.total
    sampledRule = $detail.data.ruleCode
    sampledStatus = $detail.data.status
    sampledVersion = $detail.data.version
    versionRecords = @($versions.data).Count
    validationMessage = $validation.data.message
} | Format-List
