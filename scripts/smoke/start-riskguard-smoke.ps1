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

$java = $env:JAVA_EXE
if ([string]::IsNullOrWhiteSpace($java)) {
    $java = (Get-Command java -ErrorAction Stop).Source
}

$datasourceUrl = "jdbc:mysql://localhost:$MysqlPort/riskguard?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

& $java `
    -jar 'target\riskguard-mvp-0.0.1-SNAPSHOT.jar' `
    "--server.port=$Port" `
    "--spring.datasource.url=$datasourceUrl" `
    "--spring.data.redis.port=$RedisPort" `
    "--spring.rabbitmq.port=$RabbitPort" *> $logFile
