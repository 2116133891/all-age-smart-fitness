Add-Type -AssemblyName System.IO.Compression.FileSystem
$aar = "C:\Users\23163\.gradle\caches\modules-2\files-2.1\androidx.media3\media3-datasource\1.4.1\e1da53904b9e567685638063798076e3e1bebacf\media3-datasource-1.4.1.aar"
$zip = [System.IO.Compression.ZipFile]::OpenRead($aar)
$entry = $zip.GetEntry("classes.jar")
$tmp = [System.IO.Path]::GetTempFileName()
$fs = [System.IO.File]::Create($tmp)
$s = $entry.Open()
$s.CopyTo($fs)
$s.Close(); $fs.Close()
$zip.Dispose()
$z2 = [System.IO.Compression.ZipFile]::OpenRead($tmp)
Write-Host "=== Asset/DataSource classes in media3-datasource ==="
$z2.Entries | Where-Object { $_.FullName -like "androidx/media3/datasource/*" } | Select-Object -ExpandProperty FullName | Sort-Object | ForEach-Object { Write-Host $_ }
$z2.Dispose()
