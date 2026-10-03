# start-watchdog.ps1
# Launch the independent watchdog as a detached background process.
# Writes the watchdog PID to .claude/state/watchdog.pid for stop-watchdog.ps1.
$ErrorActionPreference = 'Continue'

$projectDir = $env:CLAUDE_PROJECT_DIR
if (-not $projectDir) { $projectDir = (Get-Item $PSScriptRoot).Parent.FullName }
$stateDir = Join-Path $projectDir '.claude/state'
if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force | Out-Null }
$pidFile = Join-Path $stateDir 'watchdog.pid'

# Refuse to double-launch.
if (Test-Path $pidFile) {
    $oldPid = (Get-Content $pidFile -Raw | ForEach-Object { $_.Trim() } | Where-Object { $_ }) | Select-Object -First 1
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
        Write-Host "Watchdog already running (pid $oldPid). Use stop-watchdog.ps1 first."
        exit 0
    }
}

$interval = 60
if ($args.Count -gt 0 -and $args[0] -match '^\d+$') { $interval = [int]$args[0] }

$watchdog = Join-Path $projectDir '.claude/watchdog.ps1'
$p = Start-Process -FilePath 'powershell.exe' `
    -ArgumentList "-NoProfile","-ExecutionPolicy","Bypass","-File","`"$watchdog`"","-CheckIntervalSec","$interval" `
    -WindowStyle Hidden -PassThru

Set-Content -Path $pidFile -Value "$($p.Id)" -Encoding UTF8
Write-Host "Watchdog started (pid $($p.Id)), checking every ${interval}s."
Write-Host "Logs: .claude/logs/watchdog.log"
