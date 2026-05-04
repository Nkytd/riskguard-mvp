param(
    [string] $BaseUrl = 'http://localhost:8080',
    [string] $Username = 'admin',
    [string] $Password = 'RiskGuard@123456'
)

$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $repoRoot

$acceptanceScripts = @(
    @{ Name = 'dashboard'; Path = Join-Path $PSScriptRoot 'run-dashboard-acceptance.ps1' },
    @{ Name = 'rules'; Path = Join-Path $PSScriptRoot 'run-rules-acceptance.ps1' },
    @{ Name = 'strategies'; Path = Join-Path $PSScriptRoot 'run-strategies-acceptance.ps1' },
    @{ Name = 'lists'; Path = Join-Path $PSScriptRoot 'run-lists-acceptance.ps1' },
    @{ Name = 'decisions'; Path = Join-Path $PSScriptRoot 'run-decisions-acceptance.ps1' },
    @{ Name = 'cases'; Path = Join-Path $PSScriptRoot 'run-cases-acceptance.ps1' }
)

$results = New-Object System.Collections.Generic.List[object]
$overallStartedAt = Get-Date

Write-Host "RiskGuard frontend acceptance started"
Write-Host "BaseUrl: $BaseUrl"
Write-Host ''

foreach ($script in $acceptanceScripts) {
    $startedAt = Get-Date
    Write-Host "[$($script.Name)] running $($script.Path)"

    try {
        $output = & $script.Path -BaseUrl $BaseUrl -Username $Username -Password $Password 2>&1 | Out-String
        $duration = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 2)

        if (-not [string]::IsNullOrWhiteSpace($output)) {
            Write-Host $output.TrimEnd()
        }

        $results.Add([PSCustomObject]@{
            Name = $script.Name
            Status = 'PASS'
            DurationSeconds = $duration
            Message = ''
        })
        Write-Host "[$($script.Name)] PASS in ${duration}s"
    } catch {
        $duration = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 2)
        $message = $_.Exception.Message

        $results.Add([PSCustomObject]@{
            Name = $script.Name
            Status = 'FAIL'
            DurationSeconds = $duration
            Message = $message
        })
        Write-Host "[$($script.Name)] FAIL in ${duration}s"
        Write-Host $message
    }

    Write-Host ''
}

$overallDuration = [math]::Round(((Get-Date) - $overallStartedAt).TotalSeconds, 2)
$failed = @($results | Where-Object { $_.Status -ne 'PASS' })

Write-Host 'RiskGuard frontend acceptance summary'
$results | Format-Table -AutoSize
Write-Host "Total duration: ${overallDuration}s"

if ($failed.Count -gt 0) {
    Write-Host ''
    Write-Host 'Failed suites:'
    $failed | Select-Object Name, Message | Format-List
    exit 1
}

exit 0
