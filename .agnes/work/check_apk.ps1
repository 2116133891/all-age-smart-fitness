Add-Type -AssemblyName System.IO.Compression
$apk = "C:\Users\23163\Desktop\城市大赛项目\app\build\outputs\apk\debug\app-debug.apk"
$z = [System.IO.Compression.ZipFile]::OpenRead($apk)
Write-Host "=== videos in APK ==="
$z.Entries | Where-Object { $_.FullName -like "assets/videos/*" } | ForEach-Object {
  Write-Host ($_.FullName + "  " + [math]::Round($_.Length/1024) + " KB")
}
Write-Host "=== coach pngs in APK ==="
$z.Entries | Where-Object { $_.FullName -like "*coach_*" } | ForEach-Object {
  Write-Host ($_.FullName + "  " + [math]::Round($_.Length/1024) + " KB")
}
$z.Dispose()
Write-Host "APK size: " + [math]::Round((Get-Item $apk).Length/1MB,1) + " MB"
