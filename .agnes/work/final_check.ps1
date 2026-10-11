$adb = "C:\Users\23163\.android-build\android-sdk\platform-tools\adb.exe"
# 清 logcat，杀 进 程，重新 启动
& $adb shell logcat -c
& $adb shell am force-stop com.quannian.zhidong
Start-Sleep -Seconds 1
& $adb shell am start -n com.quannian.zhidong/.MainActivity
Start-Sleep -Seconds 5
Write-Host "=== 过 程 / 崩溃 检查 ==="
& $adb shell "pidof com.quannian.zhidong"
& $adb shell "logcat -d | grep -iE 'FATAL|AndroidRuntime' | tail -10"
Write-Host "=== 媒体 解码 检查（若有 则 说明 视频 真实 播放） ==="
& $adb shell "logcat -d | grep -iE 'MediaCodec|ExoPlayer|media3|CCodec|quannian.*video' | tail -20"
Write-Host "=== 缓存 视频 文件（证明 资产 已 复制 到 缓存） ==="
& $adb shell "run-as com.quannian.zhidong ls -l /data/data/com.quannian.zhidong/cache/videos 2>/dev/null || echo '无 缓存 目录（尚未 触发 播放）'"
