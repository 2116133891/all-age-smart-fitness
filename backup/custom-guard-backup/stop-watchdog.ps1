# stop-watchdog.ps1
# Stop the watchdog started by start-watchdog.ps1.
$ErrorActionPreference = 'Continue'
$projectDir = $env:CLAUDE_PROJECT_DIR
if (-not $projectDir) { $projectDir = (Get-Item $PSScriptRoot).Parent.FullName }
$pidFile = Join-Path $projectDir '.claude/state/watchdog.pid'

if (-not (Test-Path $pidFile)) {
    Write-Host "No watchdog.pid found; nothing to stop."
    exit 0
}

$pids = (Get-Content $pidFile -Raw | ForEach-Object { $_.Trim() } | Where-Object { $_ })
foreach ($id in $pids) {
    try {
        # Kill the child process tree (watchdog + any spawned builds it owns).
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped watchdog pid $id."
    } catch {
        Write-Host "Could not stop pid $id : $_"
    }
}
Remove-Item $pidFile -Force
Write-Host "Watchdog stopped."
