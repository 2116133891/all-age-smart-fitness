$adb = "C:\Users\23163\.android-build\android-sdk\platform-tools\adb.exe"
# 进入 首页 → 青年体测 → 立定跳远（有 视频）
& $adb shell input keyevent 3
Start-Sleep -Seconds 2
& $adb shell input tap 540 410        # 重点 青年体测 卡
Start-Sleep -Seconds 4
& $adb shell input tap 400 1140       # 立定 跳远 项
Start-Sleep -Seconds 6
& $adb shell screencap -p /sdcard/v2_video.png
& $adb pull /sdcard/v2_video.png "C:\Users\23163\v2_video.png" 2>&1 | Out-Null
Write-Host "=== ExoPlayer / MediaCodec / 视频 日志 ==="
& $adb shell "logcat -d -t 200 | grep -iE 'ExoPlayer|MediaCodec|media3|cache|video_youth|AssetFile|DataScheme' | tail -30"
