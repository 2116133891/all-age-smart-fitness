$ErrorActionPreference = 'Stop'
$GC = "C:\Users\23163\.gradle\caches\modules-2\files-2.1"
$JUNIT = "$GC\junit\junit\4.13.2\8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12\junit-4.13.2.jar"
$HAM   = "$GC\org.hamcrest\hamcrest-core\1.3\42a25dc3219429f0e5d060061f71acb49bf010a0\hamcrest-core-1.3.jar"
$KOT   = "$GC\org.jetbrains.kotlin\kotlin-stdlib\1.9.10\72812e8a368917ab5c0a5081b56915ffdfec93b7\kotlin-stdlib-1.9.10.jar"
$MAIN  = "C:\Users\23163\Desktop\城市大赛项目\app\build\tmp\kotlin-classes\debug"
$TEST  = "C:\Users\23163\Desktop\城市大赛项目\app\build\tmp\kotlin-classes\debugUnitTest"
$JAVA  = "C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1\bin\java.exe"
$aar   = "$GC\androidx.compose.runtime\runtime-android\1.5.4\15479bf9af6f414a9cd5a47c13a33d5e89886cc\runtime-release.aar"
$CR    = "C:\Users\23163\.android-build\compose_rt"
New-Item -ItemType Directory -Force -Path $CR | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (-not (Test-Path "$CR\classes.jar")) {
  [System.IO.Compression.ZipFile]::ExtractToDirectory($aar, $CR) | Out-Null
}
$CRT = "$CR\classes.jar"
Write-Output ("compose_rt_extracted=" + (Test-Path $CRT))

$tests = @(
  "ExerciseAnalyzerTest","ScoreEngineTest","VideoAnalysisEngineTest",
  "CoachMotionEngineTest","V2LogicTest","PoseAndVideoV3Test",
  "TeachingVideoMatchTest","TrainingPlanGenerateTest","AiServiceSettingsTest",
  "VideoAnalysisHonestTest","V33FollowAndBodyTestTest","V33NamingStyleTest",
  "V34OnboardingRegressionTest","V35CoachStyleAssetTest","V36RegressionTest"
)
$cp  = "$JUNIT;$HAM;$KOT;$CRT;$MAIN;$TEST"
$clsArr = $tests | ForEach-Object { "com.quannian.zhidong.$_" }
& $JAVA -cp $cp org.junit.runner.JUnitCore @clsArr 2>&1 | Select-Object -Last 60
