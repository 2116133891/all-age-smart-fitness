Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip2 = [System.IO.Compression.ZipFile]::OpenRead("C:\Users\23163\Desktop\城市大赛项目\.agnes\work\media3_ds.jar")
$zip2.Entries | Where-Object { $_.Name -like "*Asset*" -or $_.Name -like "*DataSource*" } | Select-Object -ExpandProperty FullName | Sort-Object
$zip2.Dispose()
