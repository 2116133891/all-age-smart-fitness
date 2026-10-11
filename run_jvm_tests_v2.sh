#!/usr/bin/env bash
# 全龄智动 V2 纯 JVM 单测跑法（本机 23163 环境，绕开中文路径 Gradle test 的 ClassNotFoundException）。
# 用法：从项目根目录  bash run_jvm_tests_v2.sh
set -e
cd "$(dirname "$0")"

JAVA="C:/Users/23163/.android-build/jdk17/jdk-17.0.20.1+1/bin/java.exe"
GC="/c/Users/23163/.gradle/caches/modules-2/files-2.1"
JUNIT="$GC/junit/junit/4.13.2/8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12/junit-4.13.2.jar"
HAMCREST="$GC/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
KOTLIN="$GC/org.jetbrains.kotlin/kotlin-stdlib/1.9.10/72812e8a368917ab5c0a5081b56915ffdfec93b7/kotlin-stdlib-1.9.10.jar"
MAIN="app/build/tmp/kotlin-classes/debug"
TEST="app/build/tmp/kotlin-classes/debugUnitTest"

# Compose runtime（LiveLiterals 合成类在 <clinit> 引用 androidx.compose.runtime，纯 JVM 单测 需 classpath）
CR="/tmp/quannian_compose_rt_v2"
mkdir -p "$CR"
AAR="/c/Users/23163/.gradle/caches/modules-2/files-2.1/androidx.compose.runtime/runtime-android/1.5.4/15479bf9af6f414a9cd5a47c13a33d5e89886cc/runtime-release.aar"
unzip -o -q "$AAR" classes.jar -d "$CR" 2>/dev/null || true
COMPOSE_RT="$CR/classes.jar"

# 若 junit/hamcrest 未 缓存，先 让 Gradle 下载
if [ ! -f "$JUNIT" ] || [ ! -f "$HAMCREST" ]; then
  echo "== 下载 junit/hamcrest =="
  cmd //c "cd /d C:\Users\23163\Desktop\城市大赛项目 && set JAVA_HOME=C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1&& set ANDROID_HOME=C:\Users\23163\.android-build\android-sdk&& \"C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat\" :app:compileDebugUnitTestKotlin --no-daemon -q" || { echo "依赖 下载 失败"; exit 1; }
fi

echo "== 编译 主 类 + 测试 类 =="
cmd //c "cd /d C:\Users\23163\Desktop\城市大赛项目 && set JAVA_HOME=C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1&& set ANDROID_HOME=C:\Users\23163\.android-build\android-sdk&& \"C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat\" :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-daemon -q" || { echo "编译 失败"; exit 1; }

echo "== 运行 纯 JVM 单测（含 V2LogicTest）=="
"$JAVA" -cp "$JUNIT:$HAMCREST:$KOTLIN:$COMPOSE_RT:$MAIN:$TEST" org.junit.runner.JUnitCore \
  com.quannian.zhidong.ExerciseAnalyzerTest \
  com.quannian.zhidong.ScoreEngineTest \
  com.quannian.zhidong.OneEuroFilterTest \
  com.quannian.zhidong.VideoAnalysisEngineTest \
  com.quannian.zhidong.CoachMotionEngineTest \
  com.quannian.zhidong.V2LogicTest
