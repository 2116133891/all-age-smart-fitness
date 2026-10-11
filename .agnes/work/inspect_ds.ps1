Add-Type -AssemblyName System.IO.Compression.FileSystem
$aar = "C:\Users\23163\.gradle\caches\modules-2\files-2.1\androidx.media3\media3-datasource\1.4.1\e1da53904b9e567685638063798076e3e1bebacf\media3-datasource-1.4.1.aar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($aar)
$entry = $zip.GetEntry("classes.jar")
$fs = [System.IO.File]::Create("C:\Users\23163\Desktop\城市大赛项目\.agnes\work\media3_ds.jar")
$s = $entry.Open(); $s.CopyTo($fs); $s.Close(); $fs.Close()
$zip.Dispose()
$zip2 = [System.IO.Compression.ZipFile]::OpenRead("C:\Users\23163\Desktop\城市大赛项目\.agnes\work\media3_ds.jar")
$zip2.Entries | Where-Object { $_.Name -like "*AssetFileDataSource*" -or $_.Name -like "*DefaultDataSource*" } | Select-Object -ExpandProperty FullName
$zip2.Dispose()
