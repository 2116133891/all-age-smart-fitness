$jdk = "C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"
$jar = "C:\Users\23163\Desktop\城市大赛项目\.agnes\work\media3_ds.jar"
& "$jdk\bin\javap.exe" -classpath $jar "androidx.media3.datasource.AssetDataSource" 2>&1
Write-Host "=== Factory ==="
& "$jdk\bin\javap.exe" -classpath $jar "androidx.media3.datasource.AssetDataSource`$Factory" 2>&1
