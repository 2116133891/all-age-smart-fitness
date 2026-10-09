# NIGHT_RUN_HANDOVER_2026-10-04

> 全龄智动 · 竞赛版 Android MVP —— **夜间运行交接文件（新窗口唯一主入口）**
> 生成：2026-10-04 · 生成方：Agnes-3.0-flash（Claude Code）
> 项目根：`c:\Users\Admin\Desktop\城市大赛项目` · 包名：`com.quannian.zhidong`
>
> **本文件基于当前真实源码逐文件核对**，**不信任** `README.md` / 旧 `PROJECT_STATUS.md` 里的
> "已完成"描述。旧文档 10-01 版停留在 "P0 完成 / P1 未做 / 分析器全复用 ArmReach"，
> 而真实代码库**已远超该描述**（9 个专属分析器、数字人、Room、本地教练、TTS 全部已实现）。
> 一切以本文件 + 当前源码为准。

## 四级标记约定
- `IMPLEMENTED` = 代码已写完并编译通过，未真机验证
- `VERIFIED` = 编译 + 安装 + 真机运行 + 日志/截图确认
- `PARTIALLY VERIFIED` = 编译/安装/进程存活已确认，需真人操作才能确认最终画面
- `UNVERIFIED` = 未做或未确认

---

## 1. 当前技术栈
| 层 | 技术 | 版本 | 状态 |
|----|------|------|------|
| 语言 | Kotlin | 1.9.10 | VERIFIED(编译) |
| UI | Jetpack Compose + Material3 | BOM 2023.10.01 | VERIFIED(编译) |
| 相机 | CameraX (core/camera2/lifecycle/view) | 1.3.0 | IMPLEMENTED/PARTIALLY VERIFIED |
| 姿态 | MediaPipe Tasks-Vision Pose Landmarker | 0.10.14（本地 .task 模型） | IMPLEMENTED/PARTIALLY VERIFIED |
| 导航 | androidx Navigation-Compose | 2.7.5 | VERIFIED(链路编译) |
| 持久化 | Room (KSP) | 2.5.2 | VERIFIED(编译+单测逻辑) |
| 语音 | Android 框架 TextToSpeech（无三方依赖） | framework | IMPLEMENTED |
| 图片 | Coil | 2.5.0（仅预留，当前未用） | IMPLEMENTED |
| 图片格式 | 无（数字人为 Canvas 矢量） | — | VERIFIED |
| minSdk / target | 24 / 34 | — | VERIFIED |
| 网络 | **无**（全本地；RemoteLLM 无 Key 自动降级本地） | — | VERIFIED |

> 关键约束：Android 9 / arm64-v8a 真机必须可跑；MediaPipe 模型走本地 `assets/`，**不依赖联网**。

---

## 2. 当前目录结构
```
城市大赛项目/
├─ app/src/main/java/com/quannian/zhidong/
│  ├─ MainActivity.kt                  入口：TtsCoach.init/shutdown + 装载 NavigationHost
│  ├─ model/     PoseModels.kt  ScoreModels.kt          33点/相位/报告/错误枚举
│  ├─ camera/    CameraManager.kt                     CameraX 前后摄像头封装
│  ├─ pose/      PoseDetector.kt                      MediaPipe 本地封装
│  ├─ analyzer/  ExerciseAnalyzer.kt(工厂+接口) + 9 个专属分析器 + PoseAnalyzer.kt(AngleUtils/PoseIndex)
│  ├─ score/     ScoreCalculator.kt  CorrectionEngine.kt  AgeProfile.kt
│  ├─ coach/     CoachAIProvider.kt  LocalCoachProvider.kt  RemoteLLMProvider.kt
│  ├─ db/        QuanNingDatabase.kt  TrainingSession.kt  TrainingSessionDao.kt  TrainingRepository.kt
│  ├─ tts/       TtsCoach.kt
│  ├─ repository/ExerciseRepository.kt  ReportChannel.kt
│  └─ ui/
│     ├─ theme/     QuanNingTheme.kt  Theme.kt  (Palette)
│     ├─ components/Components.kt(数字人CoachAvatar+Card/Pill/ExerciseCard)  SkeletonOverlay.kt
│     └─ screens/   Splash/Home/Age/Exercise/Coach/FollowAlong(+ViewModel)/Report/MyTraining/NavigationHost
├─ app/src/main/assets/pose_landmarker.task   5.6MB 姿态模型（本地推理必需）
├─ app/src/test/java/com/quannian/zhidong/    ExerciseAnalyzerTest.kt(16)  ScoreEngineTest.kt(7)
├─ build.gradle / settings.gradle / gradle.properties / gradlew(.bat)
└─ 交接文档：NIGHT_RUN_HANDOVER_2026-10-04.md(本文件)  PROJECT_STATUS.md  README.md  FINAL_IMPLEMENTATION_REPORT.md
```

---

## 3. 已真正实现的功能（全部 `IMPLEMENTED`+，非旧文档的 P1 未做）
- 全链路 8 屏导航：splash→home→age→exercise→coach→follow→report→mytraining `IMPLEMENTED/VERIFIED(链路编译)`
- 三年龄段（儿童/青年/银龄）× 3 项运动 = 9 项内容 `IMPLEMENTED/VERIFIED`
- **9 个专属分析器**（不再是全复用 ArmReach）`IMPLEMENTED/VERIFIED(单测)`
- 数字人三年龄段差异化 + `CoachState` 状态机 `IMPLEMENTED/VERIFIED(编译)`
- 四维评分 + 年龄段权重 + 规则纠错 `IMPLEMENTED/VERIFIED(单测)`
- 本地教练话术（引用实时分数/次数）`IMPLEMENTED`
- Android TTS 语音教练（默认关）`IMPLEMENTED`
- Room 训练记录 + 累计统计 + 7 天成长曲线 + 历史列表 `IMPLEMENTED/VERIFIED(编译+逻辑)`
- 前后摄像头切换、默认后置 `IMPLEMENTED/PARTIALLY VERIFIED`

## 4. 已通过 编译 / 单测 / 真机 验证的功能
| 项 | 结果 | 方式 |
|----|------|------|
| `:app:assembleDebug` | **BUILD SUCCESSFUL**（APK ~67MB） | gradlew |
| `ExerciseAnalyzerTest` | **OK (16 tests)** | `java -cp` 手跑（Gradle test 在中文路径下 `ClassNotFoundException`，按项目既有约定手跑） |
| `ScoreEngineTest` | **OK (7 tests)** | `java -cp` |
| 真机安装（MI 9 / Android 9 / arm64） | **Success** | `adb install -r` |
| 真机启动无崩溃 | **VERIFIED**（无 FATAL/AndroidRuntime，进程存活，MainActivity 前台） | `adb logcat` + `pidof` |

## 5. 只实现、尚未真机验证的功能（`PARTIALLY VERIFIED`）
- 摄像头**实际出画面**（需真人进跟练页并授权 CAMERA）
- MediaPipe **"Pose landmarker 初始化成功"日志**（只在进入跟练页、后台线程 `init()` 时打印）
- 真人做动作 → 实时计数 / 评分 / 纠错 / 数字人 CORRECT / 报告 / Room 落库 的**完整真人闭环**
- 前后摄像头**切换**在真机上的实际画面
- TTS 中文语音实际合成效果（MI 9 是否装中文 TTS 包未确认）

## 6. 当前已知 bug / 风险
| # | 内容 | 级别 |
|---|------|------|
| 1 | 本轮已修：`JumpingJackAnalyzer.legAngle` 原用 `atan2` 差值算分腿角，双踝在髋下方时给出反角（开 313°/合 356°），开合跳**永不计次**；已改为 `AngleUtils.angleOf(la, hipMid, ra)` 点积三点夹角（开 62°/合 4°），单测覆盖 | 已修 |
| 2 | 本轮已修：4 个单测夹具几何过简（线性插值位移≠真实三点夹角），已按真实关节几何重写 | 已修 |
| 3 | 分析器阈值（如深蹲 95°/110°/85°/150°、瑜伽 18°、八段锦 15°）**未经真人校准**，现场可能需微调灵敏度 | 风险 |
| 4 | `local.properties` 用绝对路径 `C:\Users\Admin\.android-build\...`，换机器需改 | 风险 |
| 5 | 中文路径（`城市大赛项目`）导致 Gradle 单测 `ClassNotFoundException`，只能手跑 `java -cp` | 已知 |
| 6 | 见 §7（CameraX 0 尺寸黑屏防护靠 `doOnPreDraw`，仍属 PARTIALLY VERIFIED） | 风险 |

## 7. CameraX / MediaPipe 当前真实状态
- `CameraManager` `IMPLEMENTED / PARTIALLY VERIFIED`
  - **默认后置** `facingBack = true`（[CameraManager.kt:51](app/src/main/java/com/quannian/zhidong/camera/CameraManager.kt#L51)）；`switchCamera()` 切前后（unbound→rebind）。
  - 预览绑定用 `COMPATIBLE` 实现 + `doOnPreDraw` 等 PreviewView 拿到非 0 尺寸后再 `onSurfaceRequested`，防 0 尺寸黑屏（[CameraManager.kt:131-142](app/src/main/java/com/quannian/zhidong/camera/CameraManager.kt#L131)）。
  - 抽帧 `ImageAnalysis`（RGBA_8888，KEEP_ONLY_LATEST）→ `toBitmap` → 缩放 720p → 姿态推理 → 回调。
  - 模型加载在**独立后台线程**（`poseInitExecutor`），不阻塞预览；`onPoseReady` 先 false 后 true。
- `PoseDetector` `IMPLEMENTED / PARTIALLY VERIFIED`
  - MediaPipe 0.10.14：标准 `InputStream` 读模型 + `ByteBuffer.allocateDirect`（绕开 AndroidAssetUtil 的 UnsatisfiedLinkError 和堆 buffer 的 IllegalArgumentException）（[PoseDetector.kt:43-53](app/src/main/java/com/quannian/zhidong/pose/PoseDetector.kt#L43)）。
  - `RunningMode.VIDEO` + 单调递增时间戳；`BitmapImageBuilder(bitmap).build()` → `detectForVideo` → `landmarks().firstOrNull()`。
  - **33 点 + 可见度**经 `toLandmarks()` 校验（[PoseModels.kt:70-82](app/src/main/java/com/quannian/zhidong/model/PoseModels.kt#L70)）：数量异常只取前 33、低可见度归一不崩。

## 8. 9 个 Analyzer 当前状态（`ExerciseAnalyzerFactory` 路由）
`forKey(key)` 映射（[ExerciseAnalyzer.kt:60-71](app/src/main/java/com/quannian/zhidong/analyzer/ExerciseAnalyzer.kt#L60)）：
| key | 分析器 | 运动 | 单测 |
|-----|--------|------|------|
| squat | `SquatAnalyzer` | 青年深蹲 | VERIFIED(状态机+前倾) |
| shoulder | `NeckStretchAnalyzer` | 青年肩颈 | VERIFIED(侧倾/耸肩) |
| yoga | `YogaAnalyzer` | 青年瑜伽 | VERIFIED(侧倾保持) |
| jumprope | `JumpRopeAnalyzer` | 儿童跳绳 | VERIFIED(起跳+去抖) |
| stretch | `StretchAnalyzer` | 儿童拉伸 | VERIFIED(上举) |
| jack | `JumpingJackAnalyzer` | 儿童开合跳 | **VERIFIED(本轮修反角 bug 后)** |
| baduanjin | `BaduanjinAnalyzer` | 银龄八段锦 | VERIFIED(8式子标签) |
| taichi | `TaiChiAnalyzer` | 银龄太极 | VERIFIED(起势/推掌) |
| easy | `GentleStretchAnalyzer` | 银龄舒缓 | VERIFIED(缓慢保持) |

> ⚠️ **`ExerciseAnalyzerFactory` 共享状态风险（重要）**：工厂是 `object`，`instances` 是**启动时 new 一次的 9 个分析器单例**（[ExerciseAnalyzer.kt:60-71](app/src/main/java/com/quannian/zhidong/analyzer/ExerciseAnalyzer.kt#L60)），每个分析器内部有 `state/holdFrames/repCount/minKnee` 等**可变字段**。
> - `FollowAlongViewModel` 通过 `ExerciseAnalyzerFactory.forKey(...)` 直接拿到**那个共享单例**（[FollowAlongViewModel.kt:36](app/src/main/java/com/quannian/zhidong/ui/screens/FollowAlongViewModel.kt#L36)），**没有 clone、没有 reset**。
> - 后果：用户**切换运动/重新进跟练页/连续两次训练**时，同一分析器的 `state` 会从上次的中间态延续（例如跳进去时已在 `down`/`held`），计数/相位可能错位。
> - **待修（见 §14）**：进入跟练页时对 `analyzer` 调 `reset()`，或工厂改为每次返回新实例。当前 `reset()` 方法存在但**未被调用**。

## 9. 数字人当前真实实现
- `CoachAvatar(ageId, size, coachState)` Canvas 矢量（[Components.kt](app/src/main/java/com/quannian/zhidong/ui/components/Components.kt)），**非 Lottie/3D**，Android 9 兼容。
- 三年龄段形态差异化：儿童头身比 1:1.8（大头圆脸活泼，呼吸 900ms）、青年 1:3（运动型，1400ms）、银龄 1:2.8（白发+老花镜+慢动作，2400ms）。
- `CoachState`（IDLE/INTRO/PREPARE/DEMO/COUNTDOWN/GOOD/CORRECT/REST/FINISH）驱动不同手臂/表情。
- **跟练联动**：`FollowAlongViewModel` 根据 `phase.isRep` / `errors.isNotEmpty()` 把 `coachState` 置为 `GOOD`/`CORRECT`/`DEMO`（[FollowAlongViewModel.kt:120-125](app/src/main/java/com/quannian/zhidong/ui/screens/FollowAlongViewModel.kt#L120)）。
- 数字人**动作演示**与真实运动 phase 联动有限：`coachState` 是离散的 3 态映射，未做逐帧关节驱动（`motionPhase/animationProgress` 参数存在但主链路未充分驱动）——属 P1 可增强项。

## 10. Room / 训练记录
- `QuanNingDatabase`（v1，单例）+ `TrainingSession` 实体 + `TrainingSessionDao`（insert/recent/all/dailyStats）+ `TrainingRepository`（save/recent/all/dailyStats/summary）`IMPLEMENTED/VERIFIED(编译+逻辑)`。
- 全本地，无云/后端。

## 11. AI Coach / TTS
- `CoachAIProvider` 接口 + `LocalCoachProvider`（默认，规则+模板，**离线**，话术引用实时 `score`）`IMPLEMENTED`。
- `RemoteLLMProvider`（可选，无 Key 自动降级本地；**代码里没有真实网络请求**，仅占位，绝不强联网）`IMPLEMENTED`。
- **关键核查：`LocalCoachProvider` 是否真正收到实时 `ErrorType`** —— 部分：
  - `FollowAlongScreen` 调用 `LocalCoachProvider.correctionFor(ex.name, ageId, emptySet(), score)`（[FollowAlongScreen.kt:138](app/src/main/java/com/quannian/zhidong/ui/screens/FollowAlongScreen.kt#L138)）传的是 **`emptySet()`**，不是当前帧真实 `result.errors`。也就是说，**数字人"教练话术"目前只引用了实时分数，没有把当前命中的 ErrorType 喂给 LocalCoach**。
  - 而**纠错文字**其实来自 `CorrectionEngine.suggestions(result.errors...)`（[FollowAlongViewModel.kt:116](app/src/main/java/com/quannian/zhidong/ui/screens/FollowAlongViewModel.kt#L116)），是**真正**基于实时 errors 的。
  - **待修**：把 `vm` 暴露的实时 errors 接入 `LocalCoachProvider.correctionFor` 的 `errors` 参数，让教练话术真正"感知"当前错误类型。
- `TtsCoach`（framework TextToSpeech，`zh-CN`，默认关；`MainActivity` init/shutdown；跟练页 🔊 开关）`IMPLEMENTED/PARTIALLY VERIFIED`（MI 9 中文语音包未确认）。

## 12. UI 当前不足（P1）
- 数字人未做逐关节驱动 / 口型 / Lottie/3D。
- 各动作演示与"教"的可视化联动弱（`CoachScreen` 台词是固定 6 条轮播，非按实时纠错）。
- 大屏/小屏适配、暗色模式未做。
- 成长曲线图是 Canvas 自绘简易折线（无坐标轴标签/交互）。
- 报告页 "再次训练" `onAgain` 直接 `popBackStack` + `navigate("follow")`，未带上次运动 id（可能回到默认/空）——**疑似 bug，待核对** [NavigationHost.kt:73-76](app/src/main/java/com/quannian/zhidong/ui/screens/NavigationHost.kt#L73)。

## 13. 本轮（截至交接前）已修改过的文件
1. `app/src/main/java/.../analyzer/JumpingJackAnalyzer.kt` —— 修分腿角反角 bug（`legAngle` 改点积三点夹角）。
2. `app/src/test/java/.../ExerciseAnalyzerTest.kt` —— 重写 4 个夹具（squat/jack/yoga/shallow）几何。
3. `app/src/main/java/.../analyzer/PoseAnalyzer.kt` —— 一度加过 `hipAngle` 工具后**已回退并确认无残留**（`grep hipAngle` 全仓为空，`AngleUtils` 现仅有 angleOf/degOf/degOfCos/dist/mid/hipMid/angleOfSafe）。
4. 文档：`FINAL_IMPLEMENTATION_REPORT.md`（新建）、`PROJECT_STATUS.md`（顶部加 10-03 修正块）。
5. 仓库：`.gitignore`（新建，排除 build/.gradle/.claude/local.properties，保留模型 assets）。
> ⚠️ 请核对第 3 项 `PoseAnalyzer.kt` 是否有我临时加的 `hipAngle` 残留在源码里（若残留且无引用，属死代码）。

## 14. 尚未完成的任务（按优先级）
| 优先级 | 任务 | 说明 |
|--------|------|------|
| P0 | **分析器共享状态 reset** | §8：进跟练页对 `analyzer.reset()`，避免切换运动计数错位 |
| P0 | **LocalCoach 接实时 errors** | §11：`correctionFor` 传 `result.errors` 而非 `emptySet()` |
| P0 | **真人真机闭环走通** | 摄像头出画面→MediaPipe 初始化日志→计数/评分/纠错/报告/Room，确认 `VERIFIED` |
| P1 | "再次训练" 路由 bug | §12：`onAgain` 需带上次运动 id |
| P1 | 分析器阈值真人校准 | §6.3 |
| P1 | 数字人逐关节演示驱动 | §9 |
| P1 | TTS 中文包确认 | §11 |
| P2 | UI 暗色/大屏、图表坐标轴 | §12 |
| P2 | 清理 §13 第 3 项可能的死代码 | 若 `hipAngle` 残留 |

## 15. 推荐的夜间开发顺序
1. **P0-1 分析器 reset**（小改动，先消隐患）：`FollowAlongViewModel` 构造后 / 每次进页调用 `analyzer.reset()`。加单测：连续两次 analyze 之间 reset 后计数归零。
2. **P0-2 教练接实时 errors**：`FollowAlongViewModel` 暴露 `currentErrors: StateFlow<Set<ErrorType>>`（已有 `seenErrors`，但要用**当前帧**而非累计），`FollowAlongScreen` 改用该流喂 `LocalCoachProvider.correctionFor`。
3. **P0-3 真机闭环验证**：`adb` 驱动到跟练页（授权 CAMERA），`logcat -s CameraManager:V PoseDetector:V` 抓 "相机已启动" + "Pose landmarker 初始化成功" + 无 FATAL；截图确认骨骼与画面。
4. **P1 报告"再次训练"路由** + **P1 阈值校准**。
5. 每步后跑 `ExerciseAnalyzerTest`(16)+`ScoreEngineTest`(7) 手跑全绿再进下一步。
> 原则：一次只改一个小点，改动即单测/编译；不做大重构。

## 16. 最终 APK 路径
`app/build/outputs/apk/debug/app-debug.apk`（~67MB，含 MediaPipe 原生库 + `pose_landmarker.task`）。
重编：`gradlew.bat :app:assembleDebug --no-daemon`（需 `JAVA_HOME=C:\Users\Admin\.android-build\jdk-17.0.10+7`）。

## 17. 真机信息
- 设备：`b7a0af21` · Xiaomi MI 9 · Android 9 (API 28) · arm64-v8a
- ADB：`C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe`
- 包名：`com.quannian.zhidong` · 入口：`.MainActivity`

## 18. 哪些东西绝对不要再改（保护项）
- **不要**修改 `%USERPROFILE%\.claude\watchdog\`、`%USERPROFILE%\.claude\agents\project-supervisor.md`、`backup\custom-guard-backup\`（用户级守护/归档，与本项目收口无关）。
- **不要**继续开发 Guard / Watchdog / Supervisor 系统。
- **不要**把 `MediaPipe` 从本地 `.task` 推理改成必须联网的 LLM/云推理；`RemoteLLMProvider` 保持"无 Key 降级本地"。
- **不要**大规模更换 Kotlin / Compose / CameraX / MediaPipe 技术栈，不引入 Unity/Unreal 等大型运行时。
- **不要**把 `local.properties` 的绝对路径提交进仓库（已在 `.gitignore` 排除，保持）。
- **不要**动 `pose_landmarker.task`（5.6MB，必需模型；除非有明确升级理由）。

---

## 新窗口启动方式
新 Claude Code 窗口**只需先读**下面 3 个文件，然后继续夜间任务：

1. `NIGHT_RUN_HANDOVER_2026-10-04.md`（本文件 —— 主入口，先读它）
2. `PROJECT_STATUS.md`（历史 + 10-03 修正块，仅参考）
3. `README.md`（项目简介，仅参考；**不要据其判断完成度**）

读完按 §14 / §15 执行；改完务必：
- 手跑单测（`ExerciseAnalyzerTest` + `ScoreEngineTest`，`java -cp`，见 §4）
- 编译 `:app:assembleDebug`
- 真机 `adb install -r` + `logcat` 抓 CameraManager/PoseDetector 日志确认无 FATAL
- **每完成一段，把新增改动补写进本文件的 §13 / §14**（保持交接始终真实）

> 交接诚实声明：§4 的 4 项是"真实执行过并拿到结果"（编译/单测/安装/进程无崩溃）；
> §5 的 5 项**未**由 headless 兜底，需真人进跟练页配合，**不得**标成 VERIFIED。
