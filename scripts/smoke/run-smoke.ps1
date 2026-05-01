param(
    [int] $Port = 28080,
    [int] $MysqlPort = 23306,
    [int] $RedisPort = 6380,
    [int] $RabbitPort = 5673
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $repoRoot

$logDir = Join-Path $repoRoot 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir 'smoke-app.log'
if (Test-Path $logFile) {
    Remove-Item -LiteralPath $logFile -Force
}

$job = Start-Job -ScriptBlock {
    param($repoRoot, $port, $mysqlPort, $redisPort, $rabbitPort)
    Set-Location $repoRoot
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File '.\scripts\smoke\start-riskguard-smoke.ps1' `
        -Port $port `
        -MysqlPort $mysqlPort `
        -RedisPort $redisPort `
        -RabbitPort $rabbitPort
} -ArgumentList $repoRoot, $Port, $MysqlPort, $RedisPort, $RabbitPort

try {
    $baseUrl = "http://localhost:$Port"
    $deadline = (Get-Date).AddMinutes(2)
    $health = $null
    do {
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/api/v1/health" -Method Get -TimeoutSec 3
            break
        } catch {
            Start-Sleep -Seconds 3
        }
    } while ((Get-Date) -lt $deadline)

    if ($null -eq $health) {
        $tail = if (Test-Path $logFile) { Get-Content $logFile -Tail 120 | Out-String } else { 'NO_LOG' }
        throw "Health check failed. App log tail:`n$tail"
    }

    $rules = Invoke-RestMethod -Uri "$baseUrl/api/v1/rules?pageNo=1&pageSize=5" -Method Get -TimeoutSec 10
    $strategies = Invoke-RestMethod -Uri "$baseUrl/api/v1/strategies?pageNo=1&pageSize=5" -Method Get -TimeoutSec 10
    $profile = Invoke-RestMethod -Uri "$baseUrl/api/v1/profiles/users/U10001" -Method Get -TimeoutSec 10

    $requestNo = 'SMOKE' + (Get-Date -Format 'yyyyMMddHHmmss')
    $decisionBody = @{
        requestNo = $requestNo
        eventType = 'PAYMENT'
        userId = 'U10001'
        deviceId = 'D90001'
        ip = '192.168.1.10'
        amount = 1299.00
        bizId = 'ORDER-SMOKE'
        scene = 'APP'
        eventTime = '2026-05-01 10:30:00'
        extra = @{
            payMethod = 'BANK_CARD'
        }
    } | ConvertTo-Json -Depth 5

    $decision = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions" -Method Post -ContentType 'application/json' -Body $decisionBody -TimeoutSec 10
    $decisionAgain = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions" -Method Post -ContentType 'application/json' -Body $decisionBody -TimeoutSec 10
    $decisionLog = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions/$($decision.data.decisionNo)" -Method Get -TimeoutSec 10
    $hitRules = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions/$($decision.data.decisionNo)/hit-rules" -Method Get -TimeoutSec 10

    $reviewRequestNo = 'SMOKEREVIEW' + (Get-Date -Format 'yyyyMMddHHmmss')
    $reviewBody = @{
        requestNo = $reviewRequestNo
        eventType = 'PAYMENT'
        userId = 'U-SMOKE-REVIEW'
        deviceId = 'D90001'
        ip = '192.168.1.10'
        amount = 1299.00
        bizId = 'ORDER-SMOKE-REVIEW'
        scene = 'APP'
        eventTime = '2026-05-01 10:35:00'
        extra = @{
            payMethod = 'BANK_CARD'
        }
    } | ConvertTo-Json -Depth 5

    $reviewDecision = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions" -Method Post -ContentType 'application/json' -Body $reviewBody -TimeoutSec 10
    $caseNo = $reviewDecision.data.caseNo
    if ([string]::IsNullOrWhiteSpace($caseNo)) {
        throw "Expected REVIEW decision to create a case"
    }

    $case = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo" -Method Get -TimeoutSec 10
    $claimBody = @{ operatorId = 1; assigneeId = 1; remark = 'smoke claim' } | ConvertTo-Json
    $claimedCase = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/claim" -Method Post -ContentType 'application/json' -Body $claimBody -TimeoutSec 10
    $approveBody = @{ operatorId = 1; auditOpinion = 'smoke approve' } | ConvertTo-Json
    $approvedCase = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/approve" -Method Post -ContentType 'application/json' -Body $approveBody -TimeoutSec 10
    $caseOperations = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/operations" -Method Get -TimeoutSec 10

    [PSCustomObject]@{
        healthCode = $health.code
        ruleCount = $rules.data.total
        strategyCount = $strategies.data.total
        userProfile = $profile.data.userId
        decision = $decision.data.decision
        riskScore = $decision.data.riskScore
        riskLevel = $decision.data.riskLevel
        hitRuleCount = @($decision.data.hitRules).Count
        idempotentSecondCall = $decisionAgain.data.idempotent
        decisionLogFound = $decisionLog.data.decisionNo -eq $decision.data.decisionNo
        hitRuleLogCount = @($hitRules.data).Count
        reviewDecision = $reviewDecision.data.decision
        reviewCaseNo = $caseNo
        reviewCaseInitialStatus = $case.data.status
        reviewCaseClaimedStatus = $claimedCase.data.status
        reviewCaseFinalStatus = $approvedCase.data.status
        reviewCaseOperationCount = @($caseOperations.data).Count
    } | ConvertTo-Json -Depth 5
} finally {
    Stop-Job -Job $job -ErrorAction SilentlyContinue
    Remove-Job -Job $job -Force -ErrorAction SilentlyContinue

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}
