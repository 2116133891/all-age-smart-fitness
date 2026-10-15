# GEMINI_HANDOFF — 全龄智动 Android 项目交接文档（V3.5 状态）

> 交接日期：2026-10-15 ｜ 分支 `v3-dev` ｜ HEAD `f7e9bd9`（与 origin/v3-dev 0 ahead / 0 behind）
> 本轮只做了事实核查与交接文档，**没有修改任何功能代码或资源**。
> 所有"已验证/未验证"状态均基于本轮对源码、git、资源目录、APK 的直接检查；
> 此前报告（v3_3/v3_4/v3_5 reports、测试记录）中的"done/verified"声明一律降级为
> "先前报告声称，未独立核实"，除非本文档另行标注。

---

## 1. 项目概述

- **项目**：全龄智动（包名 `com.quannian.zhidong`），面向青少年/中年/老年三组的智能健身 App。
- **形态**：单 Activity（`MainActivity`）+ Jetpack Compose（Material 3）+ NavigationHost，4 个底部标签：首页 / 训练 / AI助手 / 我的。
- **核心能力**：
  - CameraX 1.3.0（PreviewView + ImageAnalysis + ProcessCameraProvider），后摄实时姿态识别；
  - MediaPipe Pose Landmarker 0.10.14，on-device 模型 `pose_landmarker.task`（5.7MB，在 assets）；
  - Media3/ExoPlayer 1.4.1 示范视频播放（16 个 MP4 在 assets/videos）；
  - Room 2.5.2（`QuanNingDatabase`，体测/训练记录）；ksp room-compiler。
- **版本基线**：compileSdk 34 / minSdk 24 / targetSdk 34；Compose BOM 2023.10.01；navigation-compose 2.7.5；Kotlin 1.9.x。
- **git 真实状态（本轮核实）**：
  - 仓库 `github.com/2116133891/all-age-smart-fitness`，分支 `v3-dev`；
  - HEAD = `f7e9bd9`（docs v3.5）← 前一提交 `c87f3d4`（feat v3.5）← `26bb424`（docs v3.4）；
  - 工作区干净（本轮新增本交接文档前）；`v3-dev...origin/v3-dev` = 0/0，推送为安全快进。
  - `local.properties` 未被 git 跟踪（内含 `sdk.dir=C:\Users\23163\.android-build\android-sdk`）。
- **APK（本轮核实存在）**：`app\build\outputs\apk\debug\app-debug.apk`，92,705,981 字节（88.4MB），构建于 2026/10/15 12:20；APK 内容含 6 张教练 PNG + 16 个 MP4 + pose_landmarker.task。
- **测试基线（先前报告声称）**：130 个 JVM 单测通过（PowerShell + `java -cp` + JUnitCore 直接跑）；Gradle `:app:testDebugUnitTest` 因中文路径 classloader 问题报 ClassNotFoundException，不可直接用作测试通道。**此 130 绿为先前报告声称，未独立复测。**

## 2. 真实项目架构（实际文件与职责）

源码根：`app\src\main\java\com\quannian\zhidong\`

**入口与导航**
- `MainActivity.kt`：唯一 Activity，Compose setContent，挂 NavigationHost。
- `ui\screens\NavigationHost.kt`：全局导航。4 标签（首页/训练/AI助手/我的）+ 子路由；青少年入口 `"youthBodyTest"` → `YouthBodyTestScreen`；`"followItem/{key}"` → `FollowAlongItemScreen`；`"teachingVideoItem/{key}"` → `TeachingVideoScreen`。
- `ui\screens\OnboardingScreen.kt`：V3.4 曾修过此文件的 `weight(0f)` 闪退（见 §4-A 历史证据）。

**跟练 / 体测 / 视频页**
- `ui\screens\FollowAlongScreen.kt`：通用跟练（V3.5 重写为严格竖排堆叠：`BoxWithConstraints`，视频窗 weight 0.42~0.46、相机窗 weight 1f；`CameraPane` 用 `onGloballyPositioned` 延迟 start，非零测量后再启动；相机黑屏重试 `onRetry` → `cameraManager.restart()`）。
- `ui\screens\FollowAlongItemScreen.kt`：单项跟练/测评双页签。V3.5 起通过 `CoachAssetRepository.resolveAsset(itemKey, gender, style, AssetType.DEMO_VIDEO, assetAge)` 解析素材；`videoUsable = teachVideo != null && assetPath.isNotBlank()`；页底有手动脉分录入 + Room `saveResult`。
- `ui\screens\YouthBodyTestScreen.kt`：青少年体测主页。`BodyTestCatalog.ITEMS` 拆为 `motionItems`（非 method-only，MotionItemCard 三按钮：看示范/开始跟练/测试·录入）与 `measureItems`（BMI/肺活量，MeasureItemCard）。
- `ui\screens\TeachingVideoScreen.kt` + `ui\components\TeachingVideoPlayer.kt`：示范视频页与播放器（`aspectRatio(16f/9f)` 容器，asset 拷到 cache 后 `ProgressiveMediaSource`，循环/静音支持）。
- `ui\screens\TrainingHomeScreen.kt`、`BodyTestScreens.kt`（中年/老年体测与青少年同模板）等：各年龄段训练/体测页。

**相机与姿态**
- `camera\CameraManager.kt`：CameraX 预览 + MediaPipe 分析。V3.5 新增 `restart()`（unbindAll+rebind 同一实例，`start()` 幂等）；`visibleCrop(PreviewView)` 计算源帧归一化裁剪 RectF。
- `camera\PoseDetector.kt`：PoseDetector 封装，含旋转/镜像处理。
- `camera\SkeletonOverlay.kt` + `camera\PoseCoordinateMapper.kt`：骨架叠加；`toCanvasPoint(lm, crop, canvasW, canvasH, mirror)` 用 visibleCrop + 镜像做坐标映射。

**仓库 / 数据**
- `repository\CoachAssetRepository.kt`：V3.5 核心——`AssetResolution(resolved, styleMatched, note, methodOnly)`；`resolveAsset(itemKey, gender, style, assetType, ageGroup)`；`ownerAgeOf(itemKey)`。风格（CoachStyle）真正驱动视频解析；method-only 项诚实返回。
- `repository\TeachingVideoRepository.kt`：18 条目视频索引（youth 10 / middle 4 / senior 4）；`byId`、`videoForItem(itemKey, gender)`（pullup/situp 按性别）、`isMethodOnlyItem`（BMI、肺活量）、`allVideoItems()` 只列 assetPath 非空者。
- `data\BodyTestRepository.kt` / `data\TrainingRepository.kt` / Room `QuanNingDatabase`：体测/训练记录持久化。
- `coach\CoachStyle.kt` + `coach\CoachStyleStore.kt`：`CoachStyle` 枚举（REALISTIC 青少年默认，ANIMATED 中年/老年默认），SharedPreferences 键 `quanli_coach`。

**UI 组件 / 资源映射**
- `ui\components\CoachVisual.kt`：教练资源**唯一映射点**。`coachPortraitResource` → `R.drawable.coach_{youth,middle,senior}_portrait`（竖版 736:1312，ContentScale.Fit）；`coachResource` → 横版 `coach_{ageId}.png`（CoachAvatar 用 1248x832）。
- `ui\components\CoachAvatar.kt`、各卡片/列表组件。

**AI 助手**：`ai\*`（AI 对话/建议，接 LLM 或本地规则；本轮未逐行核实其实现细节，标记 待检查）。

**资源**
- `app\src\main\assets\videos\`：16 个 MP4（§5 清单）。
- `app\src\main\assets\pose_landmarker.task`：5.7MB 姿态模型。
- `app\src\main\res\drawable\`：仅 6 张 `coach_*.png` / `coach_*_portrait.png` + ic_launcher 相关 xml。**没有用户提供的 10 张素材。**

## 3. 最近的关键改动（V3.3 → V3.5 提交链）

| 提交 | 内容 | 兼容性风险 |
|---|---|---|
| `c87f3d4` feat(v3.5) | 启动页去假进度；3 张竖版透明教练肖像；跟练严格竖排堆叠；黑相机修复（start 延迟到视图测量后）；`CameraManager.restart()`；`resolveAsset` 让教练风格真正驱动素材解析；onboarding 三页竖版肖像；BMI/肺活量移出运动清单；新增 V35 8 用例 | 相机生命周期、布局权重、资源解析全链路 |
| `26bb424` docs(v3.4) | 标记 deep-interaction 路径（跟练相机+MediaPipe、青少年体测、训练计划 Room 写）"已走查验证，0 FATAL" | 无（纯文档） |
| （更早）V3.4 修复 | `OnboardingScreen.kt:191` `weight(0f)` IllegalArgumentException 启动闪退，按 logcat 修掉 | 无 |

- 测试记录：V3.5 提交声称 130 JVM 用例通过（JUnitCore 直跑）。**未独立复测。**
- 兼容性注意：APK 88.4MB（16 个 MP4 + task 模型 + 6 PNG 全打进 assets）；minSdk 24；MediaPipe 0.10.14 与 Android 9（API 28）设备兼容性依赖设备实测。

## 4. 未解决问题（4 项，全部未解决）

### A. 跟练偶现闪退（最优先）
- **真实状态**：本轮**没有采集到任何新的崩溃 Logcat**。仓库中仅有旧日志 `reports\v3_4_crash_logcat.txt`，内容是 **V3.4 时**的 `OnboardingScreen.kt:191` `invalid weight 0.0` 崩溃（已修），**与现在跟练偶现闪退无关**。
- **根因：未确认。** 候选路径（均读源码核实存在，未定位到崩溃点）：
  - `FollowAlongScreen.kt` / `FollowAlongItemScreen.kt`：V3.5 布局重写（BoxWithConstraints 权重）后相机窗 start 时机变化；
  - `camera\CameraManager.kt`：`restart()` 的 unbind/rebind 与回调线程（`runOnUiThread`）、`onFrame(emptyList())` 空帧路径；
  - `camera\PoseDetector.kt`：`poseInitExecutor` 线程内 init 与 UI 回调竞争；
  - `camera\SkeletonOverlay.kt`：visibleCrop 为 null 时的坐标映射；
  - 对应 ViewModel：状态流收集与相机生命周期耦合。
- **下一步取证**：复现前执行 `adb -s b7a0af21 logcat -c && adb -s b7a0af21 logcat -b crash -v time > crash_YYYYMMDD.txt`（并挂 `-b main` 过滤 `com.quannian.zhidong` + FATAL），复现"进跟练页→几秒后闪退"，拿到 `AndroidRuntime` FATAL 栈再定位。**在拿到新栈之前，任何"修闪退"都是猜测。**

### B. 用户提供的图标素材未接入
- **真实状态**：10 个用户素材（PNG/JPG）位于 `.agnes\uploads\20261011_1\`，**不在 git 仓库里、不在 res 目录、没有任何源码引用它们**（源码引用扫描本轮核实）。
- 页面当前实际使用的仍是 6 张生成教练 PNG（`coach_{youth,middle,senior}{,_portrait}.png`，见 §5）。
- **要做**：素材需重新提供/导入（当前目录内容可能已随环境丢失，10 个文件仅存在于本地 `.agnes/uploads`），再按 `CoachVisual.kt` 单一映射点替换/新增资源，涉及 res + 构建 + 逐页走查。

### C. 示范视频比例/裁剪显示问题（设备级未验证）
- 源码事实（已核实）：`TeachingVideoPlayer` 用固定 `aspectRatio(16f/9f)` 容器；`CameraManager` 预览 `ScaleType.FILL_START`；`SkeletonOverlay` 叠加依赖 `visibleCrop` + mirror。
- 用户反馈"教练视频显示不完整/比例不对"，但**尚未在设备 b7a0af21（1080x2340，API 28）上逐项验证**：
  1. 各年龄段 16 个 MP4 的源比例与 16:9 容器是否一致；
  2. 相机窗（FILL_START + 权重 1f）与骨架 overlay 裁剪是否对齐；
  3. 竖屏堆叠下 0.42/0.46 权重切换阈值（`avail < 520.dp` tight 分支）在真机上的实际渲染。
- **待检查项**均需实机目检/截图，当前**无结论**。不要把"构建成功"当成"显示正确"。

### D. 青少年页面与规范不符（用户反馈，UI 未验证）
- 源码事实（已核实）：`YouthBodyTestScreen` 结构为 motionItems（3 按钮卡）+ measureItems（BMI/肺活量卡），BMI/肺活量确实被正确排除出运动清单。
- 用户说"与规范/中年老年模板不符"。**源码层面结构成立，但真机 UI 未验证**：卡按钮顺序、文案、布局是否与其他年龄段页面视觉一致，需设备走查后才能定论。
- 下一步：在 b7a0af21 上截图对比青少年页 vs 中年/老年页同构卡片，差异点列成清单再改。

## 5. 真实素材清单（本轮逐文件核实）

### 5.1 视频（`app\src\main\assets\videos\`，16 个 MP4，全部存在于仓库）

| 文件 | 组 | 仓库内? | 被 TeachingVideoRepository 引用? | 在 APK? | 播放验证? |
|---|---|---|---|---|---|
| video_youth_warmup.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_50m.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_long_jump.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_endurance.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_pullup.mp4 | youth(男) | ✅ | ✅（pullup/situp 按性别） | ✅ | 未独立验证 |
| video_youth_situp.mp4 | youth(女) | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_plank.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_youth_forward_reach.mp4 | youth | ✅ | ✅ | ✅ | 未独立验证 |
| video_middle_cardio.mp4 | middle | ✅ | ✅ | ✅ | 未独立验证 |
| video_middle_core.mp4 | middle | ✅ | ✅ | ✅ | 未独立验证 |
| video_middle_flexibility.mp4 | middle | ✅ | ✅ | ✅ | 未独立验证 |
| video_middle_neck_shoulder.mp4 | middle | ✅ | ✅ | ✅ | 未独立验证 |
| video_senior_taichi.mp4 | senior | ✅ | ✅ | ✅ | 未独立验证 |
| video_senior_baduanjin.mp4 | senior | ✅ | ✅ | ✅ | 未独立验证 |
| video_senior_balance.mp4 | senior | ✅ | ✅ | ✅ | 未独立验证 |
| video_senior_gentle_stretch.mp4 | senior | ✅ | ✅ | ✅ | 未独立验证 |

- `TeachingVideoRepository` 共 18 条索引（youth 10 / middle 4 / senior 4），其中 **BMI、肺活量为 method-only**（`assetPath` 为空，无视频），16 条有真实 MP4，与文件数吻合。

### 5.2 教练图像（`app\src\main\res\drawable\`，6 张 PNG，全部存在）

| 文件 | 用途 | 引用点 | 在 APK? | 显示验证? |
|---|---|---|---|---|
| coach_youth.png | 横版头像（CoachAvatar） | `CoachVisual.coachResource` | ✅ | 未独立验证 |
| coach_middle.png | 同上 | 同上 | ✅ | 未独立验证 |
| coach_senior.png | 同上 | 同上 | ✅ | 未独立验证 |
| coach_youth_portrait.png | 竖版肖像（onboarding/首页） | `CoachVisual.coachPortraitResource` | ✅ | 未独立验证 |
| coach_middle_portrait.png | 同上 | 同上 | ✅ | 未独立验证 |
| coach_senior_portrait.png | 同上 | 同上 | ✅ | 未独立验证 |

### 5.3 模型（1 个）
| 文件 | 用途 | 大小 | 在 APK? | 验证? |
|---|---|---|---|---|
| assets/pose_landmarker.task | MediaPipe Pose on-device 模型 | 5.7MB | ✅ | 识别效果未在本轮独立验证 |

### 5.4 缺失素材（必须补）
`.agnes\uploads\20261011_1\` 下 10 个用户提供的文件（不在 git、不被源码引用）：

```
upload_09234f92.png (2073879 B)  upload_17a1e63d.png (1897261 B)
upload_31d1c36f.png (1569283 B)  upload_73f11407.png (1857676 B)
upload_7d155f35.png (1577228 B)  upload_a1bfc173.png (1857676 B)
upload_b3788c9b.jpg (62701 B)    upload_c4c3ac97.png (2590633 B)
upload_e41d28ba.png (1480959 B)  upload_e68beddc.png (2073879 B)
```
（其中 09234f92 与 e68beddc 大小相同 2073879 B；73f11407 与 a1bfc173 相同 1857676 B，疑似重复文件，导入前先比对哈希。）
**交接给 Gemini：这 10 个文件需重新提供或从该目录取回；若环境里没有，必须先向用户要，不要编造资源。**

## 6. 运行 / 测试 / 构建说明（绝对路径是环境相关的，换环境必须改）

**本机（2026/10/15 使用）环境路径**
- JDK 17：`C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1`（`JAVA_HOME`）
- Android SDK：`C:\Users\23163\.android-build\android-sdk`（`ANDROID_HOME`；`local.properties` 的 `sdk.dir` 也指向它）
- Gradle 8.3：`C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat`
- 设备：`b7a0af21`（Xiaomi MI9，Android 9 / API 28，arm64-v8a，1080x2340）

**构建**
```bat
cd /d C:\Users\23163\Desktop\城市大赛项目
set JAVA_HOME=C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1
C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat :app:assembleDebug
```
产物：`app\build\outputs\apk\debug\app-debug.apk`。

**安装与日志**
```bat
adb -s b7a0af21 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s b7a0af21 logcat -c
adb -s b7a0af21 logcat -b crash -v time
rem 或主日志过滤: adb -s b7a0af21 logcat -v time | findstr /i "com.quannian.zhidong FATAL"
```

**JVM 单测（Gradle 直接跑会因中文路径 ClassNotFoundException，用 JUnitCore 直跑）**
```powershell
# 依赖 jar：junit-4.13.2 + hamcrest-core-1.3 + kotlin-stdlib + compose_rt classes.jar
java -cp <testclasses>;<主类>;<上述 jar> org.junit.runner.JUnitCore <测试类全名...>
```
（PowerShell 脚本里 `$pid` 是保留变量，脚本变量请避开；javac 编译需 `-encoding UTF-8`，否则 GBK/UTF-8 混编报错。）

**MiUI uiautomator 限制**：`uiautomator dump` 只能导出 systemui 根（416 字节 XML），交互走查只能用**坐标 tap + 进程存活 + logcat FATAL 计数**。

**环境适配（Gemini 必读）**：§6 所有 `C:\Users\23163\...` 路径、`local.properties` 的 sdk.dir、gradle 绝对路径都是**本机专属**；换机器/换环境先定位 JDK17 + SDK + Gradle 8.3 再改，不要盲跑。
**安全**：仓库内无密钥；不要把任何 token 写进文件或提交。

## 7. 下一步开发优先级（固定顺序）

**P0-1 跟练闪退（问题 A）**
- 文件：`FollowAlongScreen.kt`、`FollowAlongItemScreen.kt`、`camera\CameraManager.kt`、`camera\PoseDetector.kt`、`camera\SkeletonOverlay.kt`、相关 ViewModel。
- 目标：先按 §4-A 抓到**新的** AndroidRuntime FATAL 栈（crash buffer），确认根因后再改；改动保持最小。
- 验证：复现脚本（进跟练页→停留 N 秒→进出 3 次）× 3 台/3 轮 0 FATAL；crash buffer 为空；再跑跟练走查坐标 tap 全通。

**P0-2 视频/相机比例（问题 C）**
- 文件：`ui\components\TeachingVideoPlayer.kt`、`camera\CameraManager.kt`（ScaleType）、`camera\PoseCoordinateMapper.kt`、`camera\SkeletonOverlay.kt`、`FollowAlongScreen.kt`（权重切换阈值）。
- 目标：16 个 MP4 逐一确认源比例，玩家容器策略与相机裁剪/骨架对齐方式**分开验证**（先视频后相机，各出一页检查表）。**禁止无证据地强制横屏/强制 fill。**
- 验证：b7a0af21 逐页截图对比 + 骨架与人体对齐抽查。

**P0-3 用户素材导入（问题 B）**
- 文件：`res\drawable`（新增）、`CoachVisual.kt`（唯一映射点）、引用 6 张旧 PNG 的各页。
- 目标：先确认 10 个 upload 文件存在（§5.4），去重（哈希比对 2 组同尺寸文件），按用途映射导入 res，改映射点，不改散落引用。
- 验证：构建 + 逐页截图，确认新素材实际显示，无九宫格拉伸。

**P0-4 青少年页规范对齐（问题 D）**
- 文件：`ui\screens\YouthBodyTestScreen.kt`（及中年/老年同构页）。
- 目标：真机截图出"青少年 vs 中年/老年"差异清单，按清单改，不改数据层。
- 验证：走查坐标 tap + 截图。

**P1 教练风格资产解析**：`CoachAssetRepository.resolveAsset` 全路径（REALISTIC/ANIMATED × 3 组）回归；**P1 onboarding 三页**走查；**P1 全量回归**（130 JVM + 关键路径走查）。

**P2 视觉打磨**：卡片、空态、动效；最后做，且每项附截图证据。

## 8. 安全操作要求（Gemini 必读 10 条）

1. **先读本文档**（本文件）再动手；文档里的绝对路径按你的环境重定位后再执行。
2. **一切结论以源码为准**：git log/status、源码、资源目录直接证据 > 旧报告 > 记忆。旧报告（v3_3/4/5、reports/）里"已验证/done"的表述一律视为待核实。
3. **改前先查**：动任何文件前先 `git status` + 读目标源文件；**本轮及后续交接回合若只要求文档，不碰功能代码**。
4. **不要大重写**：修复走最小 diff；布局类修复（跟练/比例）禁止整页推翻重写，保留 V3.5 竖排堆叠结构做增量。
5. **不强制横屏**：跟练页严格竖屏堆叠是 V3.5 决定；除非有明确设计依据，不要引入 landscape 模式或全局 orientation 修改。
6. **不盲改多模块**：一次只动一个子系统（相机 / 视频 / 资源 / 页面），改完单独验证再下一个；不要跨模块"顺手"重构。
7. **证据驱动修复**：闪退必须先有**新** logcat FATAL 栈（§4-A 方法）再定位；显示类问题先出截图检查表。没有证据不下结论。
8. **"构建成功 ≠ 交互成功"**：assembleDebug 通过只证明能编译；每个 P0 项必须附设备走查证据（坐标 tap + 进程存活 + FATAL 计数 + 截图）。
9. **诚实标注未验证**：无法验证的一律写"未验证/待检查"并说明缺什么（设备、素材、日志），不要声称完成。
10. **素材来源诚实**：§5.4 的 10 个 upload 文件缺失/存疑时，向用户要原始文件，**不得伪造资源或跳过引用**；git 只提交有依据的改动，提交前 `git status` 自查无意外文件。

---

### 附：本轮交接操作记录
- 事实核查：git（v3-dev @ f7e9bd9，0/0 vs origin/v3-dev，工作区干净）、资源（16 MP4 / 6 PNG / 1 task）、源码（§2 全部文件实读）、APK（88.4MB 存在）。
- 本轮**未修改任何功能代码或资源**；新增文件仅 `GEMINI_HANDOFF.md`（独立提交）。
- 推送：快进、无冲突、远端 `origin/v3-dev`，安全。
