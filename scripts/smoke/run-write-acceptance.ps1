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

function Invoke-Json {
    param(
        [Parameter(Mandatory = $true)] [string] $Uri,
        [Parameter(Mandatory = $true)] [string] $Method,
        [hashtable] $Headers = @{},
        $Body = $null,
        [string] $Name = $Uri
    )

    $parameters = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
        TimeoutSec = 10
    }

    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    $response = Invoke-RestMethod @parameters
    Assert-CodeZero -Response $response -Name $Name
    return $response
}

$health = Invoke-Json -Uri "$BaseUrl/api/v1/health" -Method Get -Name 'health'

$login = Invoke-Json -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Name 'login' -Body @{
    username = $Username
    password = $Password
}
$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }

$suffix = Get-Date -Format 'yyyyMMddHHmmss'
$prefix = "ACCEPT_$suffix"
$ruleId = $null
$strategyId = $null
$listId = $null
$caseNo = $null
$disabledRuleStatus = ''
$disabledStrategyStatus = ''
$cleanupNotes = New-Object System.Collections.Generic.List[string]

try {
    $rule = Invoke-Json -Uri "$BaseUrl/api/v1/rules" -Method Post -Headers $headers -Name 'rules/create' -Body @{
        ruleCode = "${prefix}_RULE_PAYMENT_AMOUNT"
        ruleName = "${prefix} payment amount rule"
        eventType = 'PAYMENT'
        expression = 'event.amount >= 999999'
        score = 5
        action = 'SCORE'
        priority = 900
        description = 'controlled write acceptance rule'
    }
    $ruleId = $rule.data.id

    $updatedRule = Invoke-Json -Uri "$BaseUrl/api/v1/rules/$ruleId" -Method Put -Headers $headers -Name 'rules/update' -Body @{
        ruleName = "${prefix} updated payment amount rule"
        eventType = 'PAYMENT'
        expression = 'event.amount >= 888888'
        score = 10
        action = 'SCORE'
        priority = 850
        description = 'controlled write acceptance rule updated'
    }

    $validation = Invoke-Json -Uri "$BaseUrl/api/v1/rules/validate-expression" -Method Post -Headers $headers -Name 'rules/validate-expression' -Body @{
        expression = $updatedRule.data.expression
    }
    if (-not $validation.data.valid) {
        throw "Expected ACCEPT rule expression to be valid: $($validation.data.message)"
    }

    $publishedRule = Invoke-Json -Uri "$BaseUrl/api/v1/rules/$ruleId/publish" -Method Post -Headers $headers -Name 'rules/publish' -Body @{
        publishNote = "$prefix write acceptance publish"
    }
    $ruleVersions = Invoke-Json -Uri "$BaseUrl/api/v1/rules/$ruleId/versions" -Method Get -Headers $headers -Name 'rules/versions'

    $strategy = Invoke-Json -Uri "$BaseUrl/api/v1/strategies" -Method Post -Headers $headers -Name 'strategies/create' -Body @{
        strategyCode = "${prefix}_STRATEGY_PAYMENT"
        strategyName = "${prefix} payment strategy"
        eventType = 'PAYMENT'
        grayRatio = 100
        description = 'controlled write acceptance strategy'
    }
    $strategyId = $strategy.data.id

    $updatedStrategy = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId" -Method Put -Headers $headers -Name 'strategies/update' -Body @{
        strategyName = "${prefix} updated payment strategy"
        eventType = 'PAYMENT'
        grayRatio = 50
        description = 'controlled write acceptance strategy updated'
    }

    $binding = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/rules" -Method Post -Headers $headers -Name 'strategies/bind-rule' -Body @{
        ruleId = $ruleId
        ruleVersion = $publishedRule.data.version
        executeOrder = 850
        enabled = $true
    }

    $orderedBindings = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/rules/order" -Method Put -Headers $headers -Name 'strategies/rules/order' -Body @{
        items = @(
            @{
                ruleId = $ruleId
                executeOrder = 840
            }
        )
    }

    $publishedStrategyV1 = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/publish" -Method Post -Headers $headers -Name 'strategies/publish-v1' -Body @{
        publishNote = "$prefix write acceptance publish v1"
    }

    $updatedStrategyV2Draft = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId" -Method Put -Headers $headers -Name 'strategies/update-v2-draft' -Body @{
        strategyName = "${prefix} rollback target strategy"
        eventType = 'PAYMENT'
        grayRatio = 75
        description = 'controlled write acceptance strategy rollback target'
    }
    $publishedStrategyV2 = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/publish" -Method Post -Headers $headers -Name 'strategies/publish-v2' -Body @{
        publishNote = "$prefix write acceptance publish v2"
    }
    $rolledBackStrategy = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/rollback" -Method Post -Headers $headers -Name 'strategies/rollback-v1' -Body @{
        version = $publishedStrategyV1.data.version
    }

    $strategyVersions = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/versions" -Method Get -Headers $headers -Name 'strategies/versions'
    Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/rules/$ruleId" -Method Delete -Headers $headers -Name 'strategies/remove-rule' | Out-Null
    $disabledStrategy = Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/disable" -Method Post -Headers $headers -Name 'strategies/disable-before-case'
    $disabledStrategyStatus = $disabledStrategy.data.status
    $disabledRule = Invoke-Json -Uri "$BaseUrl/api/v1/rules/$ruleId/disable" -Method Post -Headers $headers -Name 'rules/disable-before-case'
    $disabledRuleStatus = $disabledRule.data.status

    $list = Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists" -Method Post -Headers $headers -Name 'risk-lists/create' -Body @{
        listType = 'BLACK'
        objectType = 'USER'
        objectValue = "${prefix}_USER"
        riskLevel = 'HIGH'
        effectType = 'SCORE_UP'
        scoreDelta = 12
        reason = "$prefix controlled write acceptance list"
        startTime = '2026-05-04T00:00:00'
        endTime = '2026-05-05T00:00:00'
    }
    $listId = $list.data.id

    $updatedList = Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists/$listId" -Method Put -Headers $headers -Name 'risk-lists/update' -Body @{
        riskLevel = 'MEDIUM'
        effectType = 'SCORE_UP'
        scoreDelta = 8
        reason = "$prefix controlled write acceptance list updated"
        startTime = '2026-05-04T00:00:00'
        endTime = '2026-05-06T00:00:00'
    }
    $disabledList = Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists/$listId/disable" -Method Post -Headers $headers -Name 'risk-lists/disable'
    $enabledList = Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists/$listId/enable" -Method Post -Headers $headers -Name 'risk-lists/enable'
    $filteredList = Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists?pageNo=1&pageSize=10&keyword=$([System.Uri]::EscapeDataString($prefix))" -Method Get -Headers $headers -Name 'risk-lists/search'

    Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists/$listId" -Method Delete -Headers $headers -Name 'risk-lists/delete' | Out-Null
    $listId = $null

    $reviewRequestNo = "${prefix}_REVIEW"
    $reviewDecision = Invoke-Json -Uri "$BaseUrl/api/v1/risk/decisions" -Method Post -Name 'decisions/create-review-case' -Body @{
        requestNo = $reviewRequestNo
        eventType = 'PAYMENT'
        userId = "${prefix}_CASE_USER"
        deviceId = 'D90001'
        ip = '192.168.1.10'
        amount = 1299.00
        bizId = "${prefix}_ORDER"
        scene = 'APP'
        eventTime = '2026-05-04 10:35:00'
        extra = @{
            payMethod = 'BANK_CARD'
        }
    }
    $caseNo = $reviewDecision.data.caseNo
    if ([string]::IsNullOrWhiteSpace($caseNo)) {
        throw 'Expected controlled REVIEW decision to create a case'
    }

    $case = Invoke-Json -Uri "$BaseUrl/api/v1/cases/$caseNo" -Method Get -Headers $headers -Name 'cases/detail'
    $claimedCase = Invoke-Json -Uri "$BaseUrl/api/v1/cases/$caseNo/claim" -Method Post -Headers $headers -Name 'cases/claim' -Body @{
        assigneeId = 1
        remark = "$prefix claim"
    }
    $rejectedCase = Invoke-Json -Uri "$BaseUrl/api/v1/cases/$caseNo/reject" -Method Post -Headers $headers -Name 'cases/reject' -Body @{
        auditOpinion = "$prefix reject"
        remark = "$prefix reject"
    }
    $caseOperations = Invoke-Json -Uri "$BaseUrl/api/v1/cases/$caseNo/operations" -Method Get -Headers $headers -Name 'cases/operations'

    [PSCustomObject]@{
        status = 'PASS'
        baseUrl = $BaseUrl
        prefix = $prefix
        ruleId = $ruleId
        ruleCode = $updatedRule.data.ruleCode
        ruleFinalStatus = $disabledRuleStatus
        rulePublishedVersion = $publishedRule.data.version
        ruleVersionRecords = @($ruleVersions.data).Count
        strategyId = $strategyId
        strategyCode = $updatedStrategy.data.strategyCode
        strategyPublishedVersions = @($strategyVersions.data).Count
        strategyRollbackVersion = $rolledBackStrategy.data.version
        strategyFinalStatus = $disabledStrategyStatus
        bindingOrder = @($orderedBindings.data)[0].executeOrder
        listId = $list.data.id
        listDisabledStatus = $disabledList.data.status
        listEnabledStatus = $enabledList.data.status
        listKeywordMatches = $filteredList.data.total
        listDeleted = $true
        reviewCaseNo = $caseNo
        caseInitialStatus = $case.data.status
        caseClaimedStatus = $claimedCase.data.status
        caseFinalStatus = $rejectedCase.data.status
        caseOperationCount = @($caseOperations.data).Count
    } | Format-List
} finally {
    if ($null -ne $strategyId) {
        try {
            Invoke-Json -Uri "$BaseUrl/api/v1/strategies/$strategyId/disable" -Method Post -Headers $headers -Name 'cleanup/strategy-disable' | Out-Null
            $cleanupNotes.Add("strategy:$strategyId disabled")
        } catch {
            $cleanupNotes.Add("strategy:$strategyId disable failed: $($_.Exception.Message)")
        }
    }

    if ($null -ne $ruleId) {
        try {
            Invoke-Json -Uri "$BaseUrl/api/v1/rules/$ruleId/disable" -Method Post -Headers $headers -Name 'cleanup/rule-disable' | Out-Null
            $cleanupNotes.Add("rule:$ruleId disabled")
        } catch {
            $cleanupNotes.Add("rule:$ruleId disable failed: $($_.Exception.Message)")
        }
    }

    if ($null -ne $listId) {
        try {
            Invoke-Json -Uri "$BaseUrl/api/v1/risk-lists/$listId" -Method Delete -Headers $headers -Name 'cleanup/list-delete' | Out-Null
            $cleanupNotes.Add("list:$listId deleted")
        } catch {
            $cleanupNotes.Add("list:$listId delete failed: $($_.Exception.Message)")
        }
    }

    if ($cleanupNotes.Count -gt 0) {
        Write-Host 'Cleanup:'
        $cleanupNotes | ForEach-Object { Write-Host "  $_" }
    }
}
