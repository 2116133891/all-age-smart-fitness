$src = "C:\Users\23163\Desktop\城市大赛项目\.agnes\artifacts\videos"
$dst = "C:\Users\23163\Desktop\城市大赛项目\app\src\main\assets\videos"
New-Item -ItemType Directory -Force -Path $dst | Out-Null
$map = [ordered]@{
  "agnes-video-20261011-041824-01a1292eee8e789096cbb6ba62e63f3b.mp4" = "video_youth_long_jump.mp4"
  "agnes-video-20261011-042026-01a12930cbf47ef0b714f449dc846b98.mp4" = "video_youth_pullup.mp4"
  "agnes-video-20261011-042126-01a12931b69979d3887d38b55c3f77f9.mp4" = "video_youth_situp.mp4"
  "agnes-video-20261011-042227-01a12932a69a79e2816f54039d32c75f.mp4" = "video_youth_forward_reach.mp4"
  "agnes-video-20261011-042330-01a129339b0377609699b12529a0d961.mp4" = "video_youth_50m.mp4"
  "agnes-video-20261011-042428-01a129347f5275019af3c1b46fc8908c.mp4" = "video_youth_endurance.mp4"
  "agnes-video-20261011-042547-01a12935b47376c198a19ca84a019232.mp4" = "video_youth_warmup.mp4"
}
foreach ($k in $map.Keys) {
  Copy-Item -Force (Join-Path $src $k) (Join-Path $dst $map[$k])
  Write-Host ("OK: " + $map[$k])
}
Write-Host "=== destination ==="
Get-ChildItem (Join-Path $dst "*.mp4") | ForEach-Object { Write-Host ($_.Name + "  " + [math]::Round($_.Length/1024) + "KB") }
