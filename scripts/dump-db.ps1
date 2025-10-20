param(
    [string]$OutputDir = "backups"
)

$pgHost = if ($env:PGHOST) { $env:PGHOST } else { "localhost" }
$pgPort = if ($env:PGPORT) { $env:PGPORT } else { "5432" }
$pgDatabase = if ($env:PGDATABASE) { $env:PGDATABASE } else { "minurl" }
$pgUser = if ($env:PGUSER) { $env:PGUSER } else { "minurl" }
$pgPassword = if ($env:PGPASSWORD) { $env:PGPASSWORD } else { "minurl" }

$env:PGHOST = $pgHost
$env:PGPORT = $pgPort
$env:PGDATABASE = $pgDatabase
$env:PGUSER = $pgUser
$env:PGPASSWORD = $pgPassword

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss'Z'")
$dumpPath = Join-Path -Path $OutputDir -ChildPath ("{0}-{1}.dump" -f $pgDatabase, $timestamp)

& pg_dump --clean --if-exists --format=custom --file $dumpPath

Get-ChildItem -Path $OutputDir -Filter "*.dump" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } | Remove-Item -Force

Write-Host "Wrote $dumpPath"
