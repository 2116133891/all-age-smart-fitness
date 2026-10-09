# 全龄智动 · 竞赛版 数字人/视频分析 升级交接

> 生成：2026-10-07 · 项目：`C:\Users\Admin\Desktop\城市大赛项目` · 包名 `com.quannian.zhidong`
> 用途：记录本轮"数字人 Pose Sprite + 离线视频分析闭环"的实现状态与真机验证边界。

## 一、本轮已完成（编译 + JVM 单测 + APK 安装）

### 1. 数字人 Pose Sprite 帧序列（spec §八 方案 B）
- `ui/components/CoachSpriteRenderer.kt`（纯 JVM）：把 `CoachPose` 画成**透明位图帧**（离屏 `Bitmap`），按年龄差异化（儿童大头/青年修长/银龄白发老花镜）。`renderSequence` 把 `CoachMotionEngine.teachingSequence` 的每一步渲染成一张帧。
- `ui/components/CoachSpriteView.kt`（Compose）：
  - **序列模式**（教学页）：按每步 `durationMs` 循环播放帧序列，`DisposableEffect` 释放位图。
  - **实时模式**（跟练页 `livePose` 非空）：`remember(livePose, ...)` 直接渲染实时 pose，事件驱动不循环。
  - 配色用 `Palette.ageColor(ageId).rgbToInt()`（新增 `Color.rgbToInt` 扩展）。
- 接线：`CoachScreen` 教学舞台 `440.dp` 高、`figureSizePx=560`；`FollowAlongScreen` 教练面板 `300.dp`、`figureSizePx=440` + `livePose = coachPose`（随 phase 实时驱动）。
- 保留旧 `CoachMotionView`/`CoachTeachingPlayer` 未删（可后续清理）。

### 2. 离线视频分析闭环（spec §十一~§二十）
全部新增于 `com.quannian.zhidong.video`：
- `OneEuroFilter.kt`：一维 + `Pair`(x,y) 一欧滤波（纯 JVM，可单测）。抑制 MediaPipe 视频抖动。
- `VideoFrameReader.kt`：`MediaMetadataRetriever` 读宽高/时长，按 `DEFAULT_FPS=12` 抽帧（`getFrameAtTime` → `Bitmap`），`extractFrames(uri, detector, onProgress)` 逐帧 MediaPipe 出 33 点（**严格单调时间戳 `i*1000+1`**），`thumbnail(uri)` 取封面。限 `MAX_ANALYZE_SECONDS=60`。**全程本地**。
- `VideoAnalysisEngine.kt`（纯 JVM）：输入抽帧 `List<VideoFrame>` + 全新 `ExerciseAnalyzer` + `AgeProfile`，逐帧 OneEuro 平滑 → 复用 `analyzer.analyze` + `ScoreCalculator`；产出 `VideoAnalysisResult`（次数/四维/每 rep 质量 `RepSample`/时间轴 `TimelineEvent`/主错误/做对需改）。`CorrectionWording.of(ErrorType)` 给纠正文案。
- `VideoAnalysisResult.kt` / `VideoAnalysisChannel.kt`：数据模型 + 单例通道（`pending` 选运动+视频、`put` 报告、`peekCorrection/markCorrection/consumeCorrection` 纠正标记、`beforeScore` 前后对比基准）。

UI 屏（`ui/screens/`）：
- `VideoPickScreen.kt`：选动作（9 动作按年龄分组）+ `OpenDocument("video/*")` 选本地视频 → `VideoAnalysisChannel.pending(...)` → `videoAnalysis`。
- `VideoAnalysisScreen.kt`：视频缩略 + "开始分析" → 后台 `VideoFrameReader.extractFrames` + 进度条 + 本地分析提示（🔒 视频仅在本机分析）→ `videoReport`。
- `VideoReportScreen.kt`：AI 体检报告（次数/综合分/规范度 + 做得好✓/需改进⚠ + **Canvas 自绘质量时间轴** + **查看问题动作**（点片段 `MediaPlayer` seek 回放）+ **数字教练针对纠正**（主错误 → `markCorrection` → `coach/{id}` 纠错模式）+ 返回。
- `HomeScreen`：新增「🎯 AI 动作诊断」入口卡（`onAiDiagnose` → `videoPick`）。
- `NavigationHost`：新增 `videoPick` / `videoAnalysis` / `videoReport` 路由。
- `ReportScreen`：新增 `BeforeAfterCard`（改善前后对比：读 `VideoAnalysisChannel.result?.beforeScore` vs 本次 live 分，`提升 +N`）。
- `CoachScreen`：纠错模式读 `peekCorrection` → 显示"数字教练纠正示范"横幅 + 纠正文案 + "开始重新练习"。

### 3. 测试
- `run_jvm_tests.sh`（项目根，已加）：绕开中文路径下 Gradle `testDebugUnitTest` 的 `ClassNotFoundException`，用 `java -cp` 直跑。需要把 `androidx.compose.runtime` 的 `classes.jar`（从 aar 解出）加进 classpath（`LiveLiterals` 合成类依赖）。
- 结果：**44/44 全过**（`ExerciseAnalyzerTest` 16 / `ScoreEngineTest` 7 / `OneEuroFilterTest` 4 / `VideoAnalysisEngineTest` 6 / `CoachMotionEngineTest` 9 = 部分数，实际汇总 44）。
- `VideoAnalysisEngineTest` 的 `squatFrame` 夹具用**真实三点夹角**驱动 SquatAnalyzer FSM（站立 180° → 深蹲 <93°），深蹲时髋大幅下沉到接近踝使膝角变锐。

### 4. 文档
- 新增 `OPEN_SOURCE_REFERENCES.md`：OneEuro(自实现/参考 Casiez)、MediaMetadataRetriever(原生)、各 GitHub 项目许可分类(SAFE/REFERENCE/DO_NOT_COPY) + 隐私声明。
- 新增 memory：`quannian-zhidong-build-env` / `quannian-zhidong-video-loop` / `quannian-zhidong-jvm-tests`。

## 二、已验证（VERIFIED）
- `:app:compileDebugKotlin` / `:app:assembleDebug` **BUILD SUCCESSFUL**（APK ~73MB，`app\build\outputs\apk\debug\app-debug.apk`）。
- 44 项 JVM 单测全过（`bash run_jvm_tests.sh`）。
- APK 已 `adb install -r` 到 `b7a0af21`（MI 9 / Android 9）→ **安装成功 + 进程启动无崩溃**（`pidof` 存活，`logcat` 无 FATAL/AndroidRuntime）。
- 首页真机截图确认「🎯 AI 动作诊断」入口卡已渲染。

## 三、待真机真人验证（PARTIALLY / UNVERIFIED，需真人配合，勿冒充 VERIFIED）
- **视频分析端到端**：Home→AI诊断→选动作→选视频→分析→报告（时间轴/回放/纠正）→再跟练→改善前后。需真人选一段做动作的视频。
- **数字人 Sprite 逐动作差异**：教学页 9 动作逐帧真机截图回归（深蹲屈膝/开合跳分腿/八段锦 8 式/太极…），确认"一眼看懂"。
- **摄像头出画面 / MediaPipe 初始化日志 / 真人计数评分**：需真人进跟练页授权 CAMERA。
- **App Icon**：新 foreground/background PNG 已生成，launcher/recent task 真机显示待确认。

## 四、关键文件速查
| 用途 | 路径 |
|------|------|
| 数字人 Sprite 渲染 | `ui/components/CoachSpriteRenderer.kt` / `CoachSpriteView.kt` |
| 视频分析引擎 | `video/VideoAnalysisEngine.kt` / `VideoFrameReader.kt` / `OneEuroFilter.kt` |
| 视频 UI | `ui/screens/Video{Pick,Analysis,Report}Screen.kt` |
| 前后对比 | `ui/screens/ReportScreen.kt` 的 `BeforeAfterCard` |
| 导航 | `ui/screens/NavigationHost.kt`（videoPick/videoAnalysis/videoReport） |
| 单测 | `app/src/test/.../VideoAnalysisEngineTest.kt` + `run_jvm_tests.sh` |
| 开源/许可 | `OPEN_SOURCE_REFERENCES.md` |

## 五、构建 / 验证命令
```
# APK
cmd //c "cd /d C:\Users\Admin\Desktop\城市大赛项目 && set JAVA_HOME=C:\Users\Admin\.android-build\jdk-17.0.10+7&& set ANDROID_HOME=C:\Users\Admin\.android-build\android-sdk&& .\gradlew.bat :app:assembleDebug --no-daemon"
# JVM 单测
bash run_jvm_tests.sh
# 安装 + 启动
C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe shell am start -n com.quannian.zhidong/.MainActivity
```

## 六、诚实声明
- "VERIFIED"仅指**编译 + JVM 单测 + APK 安装无崩溃**；视频分析/数字人逐动作差异/摄像头真人闭环均需**真机真人**走一遍，保留 PARTIALLY/UNVERIFIED。
- 视频全程本地，无服务器上传；评分为"动作规范度"（运动教学辅助），**非医学诊断**。
