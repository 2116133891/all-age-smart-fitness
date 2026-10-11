@echo off
rem V2 build helper for this machine (JDK17 + Gradle 8.3 + SDK under C:\Users\23163\.android-build)
set "JAVA_HOME=C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"
set "ANDROID_HOME=C:\Users\23163\.android-build\android-sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Users\23163\Desktop\城市大赛项目"
set "GRADLE=C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat"
if "%1"=="" goto help
set "TASK=%1"
echo === gradle %TASK% ===
call "%GRADLE%" %TASK% --no-daemon
exit /b %ERRORLEVEL%
:help
echo usage: v2_build.bat "task"   e.g. v2_build.bat ":app:assembleDebug"
echo          v2_build.bat ":app:compileDebugKotlin"
echo          v2_build.bat "test"
