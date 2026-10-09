# PROJECT_STATUS.md

> 全龄智动 · 智能运动指导 Android MVP（城市大赛项目）
> 更新时间：2026-10-09 · 主程：Agnes-3.0-flash（Claude Code）
> 项目根：`C:\Users\23163\Desktop\城市大赛项目` · 远程 `github.com/2116133891/all-age-smart-fitness`
> 包名：`com.quannian.zhidong` · applicationId 同（**本轮未改包名/applicationId/namespace**）

## ⚠️ 本轮（10-09）参赛版视觉升级与体验优化
**目标：首页稳定、数字人统一、动作示范直观、App 图标重做、核心能力不破坏。以下均为"代码已改、可编译预期"级别；构建/单测/真机在本环境未执行（见 §0）。**

### 0. 验证诚实分级（本环境约束）
- 本会话运行环境 **无 JDK 17、无 Android SDK、无 Gradle、无 adb**：`local.properties` 仍指向不存在的 `C:\Users\Admin\.android-build\android-sdk`，`JAVA_HOME`=JDK 8。
- 因此 **APK 生成、Gradle 单测、真机摄像头/视觉验收均"未在本环境执行"**；改动为精确代码修改，需在具备 JDK17+SDK 的机器上执行 `.\gradlew :app:assembleDebug` 复核。
- 静态核对（已完成）：① 数字人体系**不再存在任何 `infiniteRepeatable`/`rememberInfiniteTransition` 调用点**（grep 确认）；② 所有 `CoachAvatar` 调用点仅传 `ageId`+`size`（与新签名匹配）；③ 旧的 Canvas 火柴人 / 帧序列符号无悬空引用；④ App 图标引用链（`mipmap-anydpi-v26` → `@drawable/ic_launcher_{background,foreground}`）完好，且**无密度 mipmap 目录**，矢量替换无重复资源冲突。

### 1. 修改文件清单（本轮 7 处）
| 文件 | 变更 |
|---|---|
| `ui/components/CoachVisual.kt` | 重写为**唯一数字人视觉源**；删除 2s 无限呼吸循环（跳动根因）；`CoachAvatar` 改竖向容器 + `ContentScale.Fit`（修头脚被 Crop 裁切、透明背景不显黑块）；`CoachFigure`/`TeachingCoach` 默认静止站立，诚实标注 Phase 1（非 3D） |
| `ui/components/Components.kt` | 顶层 `CoachAvatar` 改为**转发**到 `CoachVisual.CoachAvatar`（消除双实现/双资源映射）；删除死代码 Canvas 火柴人与未用动画 import；**保留** `Card`/`Pill`/`ExerciseCard` |
| `ui/screens/HomeScreen.kt` | P0 稳定：卡片一次性 220ms 淡入（`graphicsLayer` alpha，按 `age.id` key，不叠加）+ 有界 ripple（克制按压反馈，不改布局）；P1 首页入口图标统一为 Material 图标（去 emoji 混搭） |
| `ui/components/CoachSpriteView.kt` | 新增 `coachState` 形参并透传给 `CoachFigure`；跟练页数字人按教练状态做**克制反馈**，与用户实时骨架明确区分 |
| `ui/screens/FollowAlongScreen.kt` | 跟练页 `CoachSpriteView` 调用处补 `coachState = coachState`（单点、编译安全） |
| `res/drawable/ic_launcher_background.{png→xml}` | 重做为**矢量**自适应图标背景：深蓝→青绿对角渐变 + 顶部高光（品牌科技配色） |
| `res/drawable/ic_launcher_foreground.{png→xml}` | 重做为**矢量**自适应图标前景：白色运动人形 + 姿态识别关键点（青点）+ 识别框角标，落在 108dp 安全区内 |

### 2. 核心能力保护（回归核对，未改动业务逻辑）
- 导航路由、`applicationId`/`namespace`、Room 结构、`FollowAlongViewModel` 会话清理、CameraX 生命周期、MediaPipe 33 关键点、9 个分析器与独立实例、深蹲状态机/计数/评分/纠错、训练结束即存本地记录 —— **本轮均未触碰**，仅改视觉层组件与图标资源。
- 未升级 Kotlin/Gradle/Compose/CameraX/MediaPipe 版本；未删除已稳定分析器；未改数据库/包名。

### 3. 数字人素材接入与资源映射说明（交付项 2）
- 资源命名：`coach_child` / `coach_youth` / `coach_senior`（480×960 高质量 3D 透明背景，风格统一，同品牌三角 logo）→ 位于 `res/drawable/`，**唯一**映射点在 `CoachVisual.coachResource(ageId)`。
- 动作示范（诚实 Phase 1）：教学/跟练 = **分年龄静态 PNG + 分运动文字示范 + 状态微反馈**；`CoachMotionEngine.teachingSequence` 按运动生成不同文案序列（深蹲/侧拉伸/开合跳/八段锦 8 式…），避免"所有运动同一图"。**不冒充真实连续 3D 动画**；分阶段动作素材 / 真 3D（Filament）为 Phase 2 预留。

### 4. 仍存在的问题与后续建议
- [ ] **P1 去 emoji（已部分完成）**：仅首页已换 Material 图标；`Age/Exercise/Coach/Report/MyTraining` 屏内仍有约 9 处 emoji（🎯📊💡🎤🔊🔇↻✓），受"无法编译验证"约束本轮**未全量替换**，建议在可构建机上统一为 Material 图标。
- [ ] 跟练页若需数字人**逐帧跟随动作**（而非状态微反馈），需 Phase 2 分阶段动作素材（切图/帧序列）。
- [ ] `local.properties` 仍指向不存在的 Admin 路径；构建前需按当前机器写 `sdk.dir`（或配 `ANDROID_HOME`）+ JDK17。
- [ ] 建议在可构建机上跑 `:app:assembleDebug` + JVM 单测，并真机验证：首页停留 ≥30s 教练静止、多次进出无动画叠加、图标显示、跟练联动、图标在桌面清晰度。
- [ ] App 图标矢量已接入，但 `mipmap-anydpi-v26` 仅覆盖 API 26+；**当前无密度 mipmap 兜底 PNG**，若需支持 API 24/25 需补 PNG（矢量 adaptive icon 在 <26 会显示为空/系统默认）。

## ⚠️ 本轮（10-03）状态修正
**本文件 10-01 版的"P0 全完成 / P1 未做"描述已过时。真实代码库已远超该描述。**
逐文件核对后的真实状态（见 `FINAL_IMPLEMENTATION_REPORT.md`）：

- **9 个专属分析器全部已实现**（squat/shoulder/yoga/jumprope/stretch/jack/baduanjin/taichi/easy），
  `ExerciseAnalyzerFactory` 已路由，**不再是"全部复用 ArmReach"**。
- **数字人三年龄段差异化**已实现（Canvas 矢量，头身比/发型/老花镜/呼吸节奏各异）+ CoachState 状态机。
- **Room 训练记录 + 成长曲线 + 我的训练页** 已实现（`db/`、`MyTrainingScreen`）。
- **本地教练（LocalCoachProvider）+ 可选 RemoteLLM（无 Key 降级）** 已实现。
- **Android TTS（TtsCoach，默认关）** 已实现。
- **本轮修复的真 bug**：开合跳分腿角反角（`JumpingJackAnalyzer.legAngle` 改点积三点夹角）；
  4 个单测夹具几何重写。
- **单测：ExerciseAnalyzerTest 16/16 + ScoreEngineTest 7/7 全过**（`java -cp` 手跑，
  因中文路径下 Gradle test 会 ClassNotFoundException）。
- **编译 + 安装 + 真机进程无崩溃 已确认**；摄像头出画面 / MediaPipe 初始化日志 / 真人计数
  属 PARTIALLY VERIFIED（需真人在跟练页配合）。

下方为 10-01 原始记录（保留历史），与当前真实代码可能有出入，**以代码 + `FINAL_IMPLEMENTATION_REPORT.md` 为准**。

---

## 1. 当前已完成的工作

### 1.1 Android 工程骨架（P0 全量完成，可编译可安装）
- 完整 Gradle 工程：`settings.gradle`、`build.gradle`、`app/build.gradle`、`gradle-wrapper.properties`、`gradle.properties`、`gradlew`/`gradlew.bat`
- 包结构：`ui/screens`、`ui/components`、`ui/theme`、`camera`、`pose`、`analyzer`、`score`、`model`、`repository`
- `MainActivity` + `NavigationHost`：splash → home → age → exercise → coach → followalong → report 全链路 7 页导航
- 资源：`values/strings.xml`、`values/colors.xml`、`values/themes.xml`、自适应启动图标（`mipmap-anydpi-v26/ic_launcher.xml` + `drawable/ic_launcher_foreground.xml` + `drawable/ic_launcher_background.xml`）
- `AndroidManifest.xml`：CAMERA 权限 + `MainActivity` 入口

### 1.2 数字人 + 视觉体系（P0 完成）
- `ui/theme/Palette.kt`：三年龄段配色（儿童珊瑚 / 青年科技蓝 / 银龄青绿）+ 全局浅色科技风
- `ui/components/Components.kt`：
  - `CoachAvatar(ageId, size)` — Canvas 绘制的抽象数字人（浮动动画，无外部素材）
  - `Card / Pill / ExerciseCard` 通用卡片组件
- `ui/components/SkeletonOverlay.kt`：在摄像头画面之上绘制 33 关键点 + 骨架连线（cyan 线 + yellow 点）

### 1.3 三个年龄段页面（P0 完成）
- `HomeScreen.kt`：品牌标题 + 「选择您的运动陪练」+ 三年龄入口卡片
- `AgeScreen.kt`：年龄标题 + 该年龄 3 项运动卡片
- `ExerciseScreen.kt`：动作名 / 步骤 / 注意 / 教练开场白 + 「开始教学」「开始跟练」按钮
- `CoachScreen.kt`：数字人舞台 + 轮播教练台词 + 动作要领速览 + 教学/跟练按钮
- 运动内容仓库 `repository/ExerciseRepository.kt`：儿童（跳绳/拉伸/开合跳）、青年（深蹲/肩颈拉伸/瑜伽）、银龄（八段锦/太极/舒缓拉伸）各 3 项，含数字人开场白、步骤、注意、目标次数

### 1.4 摄像头 + 姿态识别 + 动作评分 + 纠错（P0 核心 Demo 完成）
- `camera/CameraManager.kt`：CameraX `PreviewView` 预览 + `ImageAnalysis` 抽帧 → 缩放 → 回调
- `pose/PoseDetector.kt`：MediaPipe Pose Landmarker（本地 `pose_landmarker.task` 模型，5.7MB，已 `assets/pose_landmarker.task` 打包）
- `analyzer/` 三个分析器：
  - `SquatAnalyzer`（深蹲状态机：standing→down→bottom→up，完成次数判定）
  - `ArmReachAnalyzer`（抬臂幅度通用，用于肩颈/八段锦托天/儿童开合跳）
  - `JumpRopeAnalyzer`（跳绳起跳框架，诚实标注为"起跳近似"，留升级接口）
- `score/ScoreCalculator.kt`：四维权重（幅度 40 / 姿态 30 / 稳定 20 / 完整 10）
- `score/CorrectionEngine.kt`：规则引擎 → 8 类错误（armsTooLow / bodyTooForward / kneeAngleInvalid / unstablePose / rangeTooShallow / speedTooFast / noPerson / postureDrift）→ 自然语言建议
- `ui/screens/FollowAlongViewModel.kt`：实时状态流（landmarks / phase / score / stateText / corrections / repCount）
- `ui/screens/FollowAlongScreen.kt`：摄像头 + 骨骼 + 实时数据卡 + 纠错 + 完成按钮
- `ui/screens/ReportScreen.kt`：训练报告（环形评分 + 表现良好 + 建议改进 + 再次训练/返回首页）
- `repository/ReportChannel.kt`：跟练 → 报告的数据通道
- `model/PoseModels.kt`、`model/ScoreModels.kt`：数据结构

### 1.5 JVM 单测（P0 验证通过）
- `test/java/.../ScoreEngineTest.kt`：评分/纠错逻辑 7 项断言，全过（已用 `java -cp` 直跑验证）

### 1.6 监控 / 守护系统（本轮新增，全局低 Token 方案）
- `backup/custom-guard-backup/` — 早期自制四层监控（项目级）整体归档停用，**未删除**
- `~/.claude/agents/project-supervisor.md` — 用户级只读 Supervisor agent（Read/Grep/Glob only，按需调用）
- `~/.claude/watchdog/` — 全局 AGNES Resilience Watchdog（独立 PowerShell，不依赖 AGNES/模型）：
  - `watchdog.ps1` — 60s 轮询：15723 端口探测 + 心跳 + 进程 + AGNES 状态 + build 状态 → 11 种区分状态
  - `agnes-error-hook.ps1` — 全局 PostToolUseFailure Hook，纯文本扫描 502/503/upstream/timeout，写 `state/last-action.json`
  - `start.ps1` / `stop.ps1` — 一键启停 watchdog
  - `pscheck.ps1` — PowerShell 语法自检器
  - `USAGE.md` — 使用说明
- `~/.claude/settings.json` — 增加 `hooks.PostToolUseFailure` 全局激活 `agnes-error-hook.ps1`，`enabledPlugins` 启用 ralph-loop
- **Ralph Loop** 已安装：`claude plugin install ralph-loop@claude-plugins-official`（官方插件）

---

## 2. 创建 / 修改过的关键文件

### 2.1 业务（项目内）
| 路径 | 作用 |
|------|------|
| `settings.gradle` / `build.gradle` / `app/build.gradle` | 工程 / 依赖 |
| `gradle-wrapper.properties` / `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar` | Gradle 8.3 |
| `gradle.properties` | `android.overridePathCheck=true`（非 ASCII 路径） |
| `local.properties` | `sdk.dir=C:\Users\Admin\.android-build\android-sdk` |
| `app/src/main/AndroidManifest.xml` | 权限 + 入口 |
| `app/src/main/assets/pose_landmarker.task` | 5.7MB 姿态模型（本地推理） |
| `app/src/main/res/{values,mipmap-anydpi-v26,drawable}/*` | 资源 |
| `app/src/main/java/com/quannian/zhidong/MainActivity.kt` | 入口 |
| `app/src/main/java/com/quannian/zhidong/model/{PoseModels,ScoreModels}.kt` | 数据模型 |
| `app/src/main/java/com/quannian/zhidong/repository/{ExerciseRepository,ReportChannel}.kt` | 内容 + 通道 |
| `app/src/main/java/com/quannian/zhidong/analyzer/{PoseAnalyzer,SquatAnalyzer,ArmReachAnalyzer,JumpRopeAnalyzer}.kt` | 动作分析 |
| `app/src/main/java/com/quannian/zhidong/score/{ScoreCalculator,CorrectionEngine}.kt` | 评分 + 纠错 |
| `app/src/main/java/com/quannian/zhidong/pose/PoseDetector.kt` | MediaPipe 封装 |
| `app/src/main/java/com/quannian/zhidong/camera/CameraManager.kt` | CameraX 封装 |
| `app/src/main/java/com/quannian/zhidong/ui/theme/Palette.kt` | 设计系统 |
| `app/src/main/java/com/quannian/zhidong/ui/components/{Components,SkeletonOverlay}.kt` | 数字人/卡片/骨骼 |
| `app/src/main/java/com/quannian/zhidong/ui/screens/*.kt` | 7 屏 + `FollowAlongViewModel` + `NavigationHost` |
| `app/src/test/java/com/quannian/zhidong/ScoreEngineTest.kt` | JVM 单测 |
| `README.md` | 项目说明 |

### 2.2 守护（用户级 + 项目级）
| 路径 | 作用 |
|------|------|
| `backup/custom-guard-backup/` | 早期自制四层监控归档（**停用**，含 `README.md` 说明） |
| `~/.claude/agents/project-supervisor.md` | 用户级只读 Supervisor |
| `~/.claude/watchdog/{watchdog,start,stop,agnes-error-hook,pscheck}.ps1` + `USAGE.md` | 全局 AGNES 守护 |
| `~/.claude/watchdog/state/{last-action.json,heartbeat.json,agnes-watchdog-state.json}` | 运行时状态 |
| `~/.claude/settings.json` | 全局 Hook + ralph-loop 启用 |

### 2.3 环境（非 git 管理，新会话需重建）
| 路径 | 作用 |
|------|------|
| `C:\Users\Admin\.android-build\jdk-17.0.10+7\` | Temurin JDK 17 |
| `C:\Users\Admin\.android-build\android-sdk\` | Android SDK（platform 34 + build-tools 33/34 + platform-tools） |

---

## 3. 当前可直接使用的功能

- ✅ **编译 / 打包 / 安装**：`.\gradlew :app:assembleDebug` 产出 `app\build\outputs\apk\debug\app-debug.apk`（67MB，含 MediaPipe 4-ABI 原生库 + 姿态模型）
- ✅ **全链路交互**：启动 → 首页 → 三年龄 → 运动 → 教学 → 跟练 → 报告（每页可点击跳转）
- ✅ **真机跟练闭环**（需 ARM 真机，模拟器无摄像头）：开摄像头 → MediaPipe 实时姿态识别 → 骨骼绘制 → 关节角度 → 动作阶段 → 实时得分（0–100）→ ≥3 类纠错建议 → 完成次数 → 训练报告
- ✅ **JVM 单测**：评分/纠错逻辑 7/7 通过
- ✅ **全局 AGNES 守护**：
  - `powershell -File "%USERPROFILE%\.claude\watchdog\start.ps1"` 启动 60s 轮询 watchdog
  - 检测到 Claude 进程已退出 + 15723 在监听 + 有 session 时，自动 `claude --continue` resume（5min 防抖）
  - AGNES 连续 502 按 20s/60s/120s 退避
- ✅ **Ralph Loop 长任务**：`/ralph-loop "任务描述" --max-iterations 10`（不会默认常驻）
- ✅ **Supervisor 复核**：`claude agents` 可发现 `project-supervisor`；主 Agent 任务完成时 / 怀疑卡住时显式调用

---

## 4. 当前未完成的功能

- **跳绳真实计数**：当前 `JumpRopeAnalyzer` 是"起跳状态近似"（膝角阈值），非真实绳体计数。升级接口已留（手腕轨迹 + 周期信号 + MediaPipe z 高度）
- **八段锦 / 太极逐式识别**：目前八段锦用 `ArmReachAnalyzer` 通用抬臂，未做逐式（两手托天/左右开弓/体转双关…）
- **数字人升级**：当前是 Canvas 抽象人形 + 轮播台词；未做 Lottie / 3D / 口型驱动
- **历史数据统计 / 成长曲线 / 个人档案**：无 Room / 图表
- **大模型纠错生成**：当前纠错是预设文案，未接 LLM
- **账号 / 云同步 / 后端**：P2 未做
- **UI 动画 / 大屏视觉细节**：P1 未做
- **更多动作（瑜伽 / 开合跳 / 基础体能）**：内容仓库已有，但每个都复用 `ArmReachAnalyzer`，未做动作专属分析

---

## 5. 当前已知问题

1. **AGNES 间歇 502 / rate-limit**：本地 inference gateway `127.0.0.1:15723` 会间歇 `API Error: 502` 与 `claude-sonnet-4-6[1M] is temporarily unavailable (rate-limited)`。本轮开发已数次因此中断。
   - 守护方案（全局 watchdog + error hook）已部署，可在 Claude 退出后自动 `--continue` 恢复，但**不保证**自动重连 100% 成功
2. **非 ASCII 项目路径**：`城市大赛项目` 中文路径触发 AGP 警告，已 `gradle.properties` 加 `android.overridePathCheck=true`
3. **JVM 单测 classpath 限制**：`gradle test` 在 Windows + 非 ASCII 路径下会 `ClassNotFoundException`；改用手跑 `java -cp` 验证（评分逻辑 7/7 过）。后续要跑 Gradle 单测需先把项目挪到 ASCII 路径
4. **Reikor-Arg/claude-agent-monitor 是 VS Code 扩展，非 Claude Code 插件**：`claude plugin install agent-monitor` 找不到（无 `marketplace.json`）。当前方案**不装**这个，改用 Claude Code 原生 task-notification + 可选 VS Code 扩展
5. **自动编译 Hook 的 JAVA_HOME 继承 bug**（已修但留痕）：`auto-build.ps1` 早期版本把 `$env:JAVA_HOME` 取到字面 `'True'`，导致子进程 Gradle 报 `JAVA_HOME is not set`。现已用 `[string]::IsNullOrWhiteSpace` 守卫 + 默认路径，但**自制系统已停用归档**，此 bug 不再影响

---

## 6. 当前正在进行但尚未完成的任务

**本轮"全局低 Token 守护方案"已基本完成**，剩余收尾项：

- [ ] 跑一次 watchdog 完整自测（`start.ps1` 后台起来 → 模拟 502 → 看 `agnes-watchdog-state.json` 状态变化 → `stop.ps1`）
- [ ] 验证 `claude agents` 能否列出 `project-supervisor`（之前 `claude agents --json` 返回 `[]`，怀疑用户级 agent 在 subagent 上下文未加载，需在交互式会话里确认）
- [ ] 确认 `~/.claude/settings.json` 的 PostToolUseFailure Hook 在实际 Claude Code 会话里真的触发（需要一次工具失败才能验证）
- [ ] 写一份 `HOW_TO_RESUME_THIS_PROJECT.md`（给下一个会话的速查卡，见 §8）

**没有**其他未完成任务。

---

## 7. 下一步最推荐执行的任务

按用户"暂停业务开发"的指示，**现在不该再写业务代码**。最合理的下一步：

1. **（推荐，最小）** 跑一遍 §6 里的 4 项守护自测，确认全局方案真的生效
2. **（如果用户明确要恢复开发）** 优先把 **跳绳从"起跳近似"升级为真实计数**（手腕轨迹 + 周期信号），这是 P0 里唯一"留了接口但没做真"的动作
3. **（如果用户要演示）** 装 `adb` 连一台 ARM 真机跑通"深蹲跟练"闭环，这是评委最关心的 Demo
4. **不建议** 在 AGNES 不稳定的情况下再拉长时间任务；Ralph Loop 上限设 10~20 轮就够

---

## 8. 如需继续当前任务，新会话需要知道的全部上下文

### 8.1 快速启动（环境）
```powershell
# JDK / SDK（都在用户目录，非 git 管理）
$env:JAVA_HOME   = "C:\Users\Admin\.android-build\jdk-17.0.10+7"
$env:ANDROID_HOME= "C:\Users\Admin\.android-build\android-sdk"
cd C:\Users\Admin\Desktop\城市大赛项目

# 编译
.\gradlew.bat :app:assembleDebug --no-daemon
# 产物：app\build\outputs\apk\debug\app-debug.apk

# 真机安装
& "C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

### 8.2 守护系统（全局，跨项目）
```powershell
# 启动 AGNES watchdog（60s 轮询，后台）
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\start.ps1"

# 停止
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\stop.ps1"

# 看当前状态
Get-Content "$env:USERPROFILE\.claude\watchdog\state\agnes-watchdog-state.json" -Raw

# 模拟一次 AGNES 502 给 hook
echo '{"tool_name":"Bash","tool_error":"502 Bad Gateway from upstream"}' |
  powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\agnes-error-hook.ps1"
```

### 8.3 长任务 / 复核
```
/ralph-loop "<明确的下一段任务描述>" --max-iterations 10
/cancel-ralph
```
- 怀疑主 Agent 偏离目标 / 卡住 / 连续编译失败时，让主 Agent 显式调用 `project-supervisor`（Read/Grep/Glob 只读，按需消耗 token）

### 8.4 项目状态
- **P0 全完成**（含编译、安装、跟练闭环、评分、纠错、报告）
- **P1 未做**（更多动作、数字人 Lottie/3D、UI 动画、数据统计）
- **P2 未做**（云、账号、大模型）
- 详见 `README.md`

### 8.5 关键设计决策（新会话别推翻）
1. **MediaPipe 0.10.14 API**：`PoseLandmarker.createFromOptions` + `BaseOptions.setModelAssetBuffer(ByteBuffer)` + `RunningMode.VIDEO`；`BitmapImageBuilder(bitmap).build()` → `detectForVideo(mpImage, ts)` → `result.landmarks()` 返回 `List<List<NormalizedLandmark>>`。模型 `pose_landmarker.task` 在 `assets/`（5.7MB）
2. **CameraX 1.3.0**：`ImageProxy.toBitmap()`；`ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888`；`PreviewView` 绑定用 `preview.setSurfaceProvider { req -> view.getSurfaceProvider().onSurfaceRequested(req) }`
3. **评分**：`ScoreCalculator` 四维（幅度 40 / 姿态 30 / 稳定 20 / 完整 10），独立模块，加新动作只加 `analyzer/` 规则不推翻架构
4. **纠错**：`CorrectionEngine` 预设 8 类错误 → 自然语言；未来可换 LLM
5. **跳绳诚实标注**：当前是起跳近似，不是真计数，别在演示里冒充
6. **守护系统刻意低 Token**：watchdog 是纯 PowerShell 本地脚本（0 token），Supervisor 按需调用，**不要**装多个同类 monitor / 不要给 Supervisor 加 Write/Edit 权限

### 8.6 本轮踩过的坑（别再踩）
- **PowerShell 读 stdin**：Claude Code Hook 用 `[Console]::In.ReadToEnd()`，不能用 `$input`（`param(ValueFromRemainingArguments)` 会吞掉管道输入）
- **PowerShell 5.x 默认 GBK**：任何带中文的 `.ps1` 必须存 **UTF-8 BOM**，否则解析器把中文当乱码导致语法错误
- **非 ASCII 路径 + classpath**：`gradle test` 在中文路径下会 `ClassNotFoundException`，手跑 `java -cp` 绕过
- **AGNES 502 时 Bash 被拒**：auto-mode classifier 在 AGNES 挂时会拒绝 Bash / Edit，此时只做 Read 类操作，等 AGNES 恢复再继续
- **`gradlew.bat` 必须 `--no-daemon`**：daemon 在 VS Code / 中文路径下经常卡住

### 8.7 文件位置速查
- 项目根：`c:\Users\Admin\Desktop\城市大赛项目`
- APK：`app\build\outputs\apk\debug\app-debug.apk`
- 姿态模型：`app\src\main\assets\pose_landmarker.task`
- 全局守护：`C:\Users\Admin\.claude\watchdog\`
- 用户级 Supervisor：`C:\Users\Admin\.claude\agents\project-supervisor.md`
- 自制监控归档：`c:\Users\Admin\Desktop\城市大赛项目\backup\custom-guard-backup\`

### 8.8 一个明确的"未验证"声明
**AGNES 自动 `claude --continue` 恢复 尚未在真实的 AGNES 中断场景下跑通**。本轮 watchdog 只验证了：
- 502 检测（模拟注入 last-action.json）✅
- 状态分类（AGNES_UPSTREAM_ERROR / NORMAL / GATEWAY_OFFLINE）✅
- 防抖退避逻辑（20s/60s/120s）✅
但 `Start-Process claude --continue` 在 Claude 真实因 502 退出后能否真的续上 session，**没有现场验证过**。如果新会话要依赖这个能力，**第一次真实触发时务必手动确认**。

---

## 9. 一句话总结

**P0 全完成、可编译、可安装、可演示**（需 ARM 真机）；**全局低 Token 守护已部署**（watchdog + AGNES 错误 Hook + 用户级 Supervisor + Ralph Loop）；**业务代码本轮未再改动**。下一步最推荐：跑完守护自测后，要么连真机演深蹲闭环，要么把跳绳从近似升级为真实计数。
