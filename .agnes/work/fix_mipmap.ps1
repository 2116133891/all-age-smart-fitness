$res = "C:\Users\23163\Desktop\城市大赛项目\app\src\main\res"
$dens = @("mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi")
foreach ($d in $dens) {
  $round = Join-Path $res "mipmap-$d-round"
  if (Test-Path $round) {
    $f = Join-Path $round "ic_launcher_round.png"
    if (Test-Path $f) {
      $target = Join-Path $res "mipmap-$d\ic_launcher_round.png"
      Move-Item -Force $f $target
      Write-Host "moved mipmap-$d-round -> mipmap-$d\ic_launcher_round.png"
    }
    Remove-Item -Recurse -Force $round
    Write-Host "removed empty dir mipmap-$d-round"
  }
}
Write-Host "=== final mipmap layout ==="
Get-ChildItem (Join-Path $res "mipmap-*") -Recurse -File | ForEach-Object { Write-Host $_.FullName }
