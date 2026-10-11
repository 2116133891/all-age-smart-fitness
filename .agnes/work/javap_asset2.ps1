Add-Type -AssemblyName System.IO.Compression.FileSystem
$jdk = "C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"
$aar = "C:\Users\23163\.gradle\caches\modules-2\files-2.1\androidx.media3\media3-datasource\1.4.1\e1da53904b9e567685638063798076e3e1bebacf\media3-datasource-1.4.1.aar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($aar)
$entry = $zip.GetEntry("classes.jar")
$tmp = "C:\Users\23163\Desktop\城市大赛项目\.agnes\work\media3_ds_clean.jar"
$fs = [System.IO.File]::Create($tmp)
$s = $entry.Open()
$s.CopyTo($fs)
$s.Close(); $fs.Close()
$zip.Dispose()
Write-Host "extracted $tmp"
& "$jdk\bin\javap.exe" -classpath $tmp "androidx.media3.datasource.AssetDataSource" 2>&1
Write-Host "=== Factory ==="
& "$jdk\bin\javap.exe" -classpath $tmp "androidx.media3.datasource.AssetDataSource`$Factory" 2>&1
