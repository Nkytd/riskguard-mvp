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

    $loginBody = @{
        username = 'admin'
        password = 'RiskGuard@123456'
    } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
    $headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
    $me = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/me" -Method Get -Headers $headers -TimeoutSec 10

    $rules = Invoke-RestMethod -Uri "$baseUrl/api/v1/rules?pageNo=1&pageSize=5" -Method Get -Headers $headers -TimeoutSec 10
    $strategies = Invoke-RestMethod -Uri "$baseUrl/api/v1/strategies?pageNo=1&pageSize=5" -Method Get -Headers $headers -TimeoutSec 10
    $profile = Invoke-RestMethod -Uri "$baseUrl/api/v1/profiles/users/U10001" -Method Get -Headers $headers -TimeoutSec 10

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
    $decisionLog = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions/$($decision.data.decisionNo)" -Method Get -Headers $headers -TimeoutSec 10
    $hitRules = Invoke-RestMethod -Uri "$baseUrl/api/v1/risk/decisions/$($decision.data.decisionNo)/hit-rules" -Method Get -Headers $headers -TimeoutSec 10

    $reviewSuffix = Get-Date -Format 'yyyyMMddHHmmss'
    $reviewRequestNo = 'SMOKEREVIEW' + $reviewSuffix
    $reviewUserId = 'U-SMOKE-REVIEW-' + $reviewSuffix
    $reviewBody = @{
        requestNo = $reviewRequestNo
        eventType = 'PAYMENT'
        userId = $reviewUserId
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

    $asyncProfile = $null
    $profileDeadline = (Get-Date).AddSeconds(20)
    do {
        try {
            $profileResponse = Invoke-RestMethod -Uri "$baseUrl/api/v1/profiles/users/$reviewUserId" -Method Get -Headers $headers -TimeoutSec 5
            if ($profileResponse.code -eq 0 -and $null -ne $profileResponse.data) {
                $asyncProfile = $profileResponse
                break
            }
        } catch {
            Start-Sleep -Seconds 1
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $profileDeadline)

    if ($null -eq $asyncProfile -or $asyncProfile.data.payCount24h -lt 1) {
        throw "Expected async profile update for $reviewUserId"
    }

    $case = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo" -Method Get -Headers $headers -TimeoutSec 10
    $claimBody = @{ operatorId = 1; assigneeId = 1; remark = 'smoke claim' } | ConvertTo-Json
    $claimedCase = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/claim" -Method Post -Headers $headers -ContentType 'application/json' -Body $claimBody -TimeoutSec 10
    $approveBody = @{ operatorId = 1; auditOpinion = 'smoke approve' } | ConvertTo-Json
    $approvedCase = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/approve" -Method Post -Headers $headers -ContentType 'application/json' -Body $approveBody -TimeoutSec 10
    $caseOperations = Invoke-RestMethod -Uri "$baseUrl/api/v1/cases/$caseNo/operations" -Method Get -Headers $headers -TimeoutSec 10
    $dashboardOverview = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/overview" -Method Get -Headers $headers -TimeoutSec 10
    $dashboardDistribution = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/decision-distribution" -Method Get -Headers $headers -TimeoutSec 10
    $dashboardRuleRank = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/rule-hit-rank" -Method Get -Headers $headers -TimeoutSec 10
    $dashboardTrend = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/risk-trend" -Method Get -Headers $headers -TimeoutSec 10
    $dashboardCaseStatistics = Invoke-RestMethod -Uri "$baseUrl/api/v1/dashboard/case-statistics" -Method Get -Headers $headers -TimeoutSec 10

    if (@($dashboardTrend.data).Count -ne 24) {
        throw "Expected dashboard risk trend to return 24 hourly buckets"
    }

    [PSCustomObject]@{
        healthCode = $health.code
        authUser = $me.data.username
        authRole = $me.data.roleCode
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
        asyncProfileUser = $asyncProfile.data.userId
        asyncProfilePayCount24h = $asyncProfile.data.payCount24h
        reviewCaseInitialStatus = $case.data.status
        reviewCaseClaimedStatus = $claimedCase.data.status
        reviewCaseFinalStatus = $approvedCase.data.status
        reviewCaseOperationCount = @($caseOperations.data).Count
        dashboardTodayDecisionCount = $dashboardOverview.data.todayDecisionCount
        dashboardDistributionCount = @($dashboardDistribution.data).Count
        dashboardRuleHitRankCount = @($dashboardRuleRank.data).Count
        dashboardTrendBucketCount = @($dashboardTrend.data).Count
        dashboardCaseTotalCount = $dashboardCaseStatistics.data.totalCount
    } | ConvertTo-Json -Depth 5
} finally {
    Stop-Job -Job $job -ErrorAction SilentlyContinue
    Remove-Job -Job $job -Force -ErrorAction SilentlyContinue

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}
