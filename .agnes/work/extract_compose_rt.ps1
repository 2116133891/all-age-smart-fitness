Add-Type -AssemblyName System.IO.Compression.FileSystem
$aar = "C:\Users\23163\.gradle\caches\modules-2\files-2.1\androidx.compose.runtime\runtime-android\1.5.4\15479bf9af6f414a9cd5a47c13a33d5e89886cc\runtime-release.aar"
$out = "C:\Users\23163\Desktop\城市大赛项目\.agnes\work\compose_rt\classes.jar"
$dir = Split-Path $out
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$zip = [System.IO.Compression.ZipFile]::OpenRead($aar)
try {
  $entry = $zip.GetEntry("classes.jar")
  if ($entry) {
    $fs = [System.IO.File]::Create($out)
    $s = $entry.Open()
    $s.CopyTo($fs)
    $s.Close(); $fs.Close()
    Write-Host ("extracted " + $out + " " + (Get-Item $out).Length + " bytes")
  } else {
    Write-Host "classes.jar entry not found"
  }
} finally {
  $zip.Dispose()
}
