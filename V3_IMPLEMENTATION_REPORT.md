# V3_IMPLEMENTATION_REPORT — 全龄智动 V3 参赛产品重构实施报告

> 分支 `v3-dev`（基于 V2 `main` @20a14ef）。报告日期 2026-10-12。
> 环境：JDK 17（`C:\Users\23163\.android-build\jdk17`）+ Gradle 8.3 + SDK 34。
> 配套：`V3_IMPLEMENTATION_PLAN.md`（计划/状态表）、`VIDEO_ASSET_MANIFEST.md`（视频清单）、
> `MOVENET_EVALUATION.md`（MoveNet 评估）。

---

## 1. 目标与范围

V3 参赛产品重构核心诉求（见 `V3_IMPLEMENTATION_PLAN.md`）：
- 移除儿童产品线，统一三大成年年龄段（青年 / 中年 / 银龄）；
- 性别仅保留男 / 女；
- 首页重设计（三入口 + 主要任务 + 推荐）、合并重复青年入口、每项目仅"练/测"两操作；
- 底部导航改造为 4 tab（首页 / 训练 / AI 助手 / 我的）；
- 数字人卡通 3D 三类独立资源 + 清理火柴人；
- 教学视频扩展至 18 条基础集；
- 视觉识别与动作分析重做（旋转/镜像/坐标诊断）；
- AI 助手 Provider 抽象 + API Key 安全存储；
- 数据架构统一 + 青年体测流程闭环；
- 设计系统 / 图标 / 性能；
- 测试 / 构建 / APK / 真机 / 文档 / 交付。

本报告记录 V3 全量实施结果、本轮（2026-10-12）余待办推进、验证证据与遗留项。

## 2. 交付物总览

| 类别 | 交付物 | 状态 |
|------|--------|------|
| 代码 | 年龄段/性别统一（删儿童线） | ✅ |
| 代码 | 首页重设计 + 合并青年入口 + 4-tab 导航 + 训练中心 | ✅ |
| 代码 | 数字人卡通 3D（youth/middle/senior 3 段收敛，清 child 残留） | ✅（PNG 实查终版，容器比修复） |
| 代码 | AI Provider 抽象（远程优先 + 本地降级）+ Key 加密存储 + 能力诚实分级 | ✅ |
| 代码 | 教学视频 18 条基础集（可播 16 / 方法说明 2） | ✅（V3.1 补齐 9 条 AIGC 视频） |
| 代码 | 姿态识别旋转修复 + 坐标映射单一可信来源 | ✅ |
| 文档 | 实施计划 / 视频清单 / MoveNet 评估 / 本报告 | ✅ |
| 验证 | 编译 BUILD SUCCESSFUL + 80 单测全绿 + APK 87.82MB | ✅ |
| 验证 | 真机安装/启动（Xiaomi MI9/Android9） | ✅（无 crash；逐页点击待人工） |
| 验证 | 真机多样本回归（跟练帧率/骨架贴合） | ⏳（需人工设备操作） |

## 3. 代码变更明细（本轮 2026-10-12）

### 3.1 教学视频 18 条扩展（任务 9）
- 文件：`app/src/main/java/com/quannian/zhidong/repository/TeachingVideoRepository.kt`
- 变更：
  - `BY_ID` 由 7 条扩到 18 条：youth 10（7 有 mp4 + 1 pending 平板支撑 + BMI/肺活量方法说明 2）
    + middle 4（核心强化 / 柔韧性 / 有氧减脂 / 肩颈放松）+ senior 4（八段锦 / 太极 / 平衡防跌 / 舒缓拉伸）。
  - 新增 `allItems()`（全量 18）、`byAgeId(ageId)`（按段归组）、`byId(id)`（直接取）；
    保留 `allVideoItems()`（仅 assetPath 非空，当前 7）、`hasVideo`、`assetExists`。
- 诚实原则：pending 条目 `assetPath=""`，自动被 `allVideoItems()` 过滤，播放入口不会出现
  "点了没得播"；middle/senior 补齐 mp4 前其可播列表为空，UI 走设计内空态（非缺陷）。

### 3.2 姿态识别旋转 / 镜像 / 坐标诊断（任务 10）
- 文件：`app/src/main/java/com/quannian/zhidong/camera/CameraManager.kt`
  - `analyzeFrame` 新增读取 `image.imageInfo.rotationDegrees`（0/90/180/270），
    用 `Matrix.postRotate` 旋转为 upright 帧再 `scaleDown` 推理。
  - 根因：CameraX `toBitmap()` 出的是**传感器方向**帧；横置传感器 + 竖拿手机时画面横躺，
    不旋转会让 MediaPipe 拿到"横躺"人体 → 左右肩/髋互换、整体旋错、深度角失真。
  - 回收链重写（analysis → resized → upright），避免 double-recycle。
- 文件：`app/src/main/java/com/quannian/zhidong/pose/PoseCoordinateMapper.kt`（新增，纯逻辑）
  - `Crop`（纯数据，替代 `RectF` 以便 JVM 测试）+ `fromRectF` 桥接；
  - `toCanvasPoint` / `toCanvasPoints`：源帧归一化 → 可见裁切 → 前置镜像 → 画布像素；
  - `cropMatters`：诊断"是否必须按 crop 映射"（全帧 vs 裁切差异超阈值）。
- 文件：`app/src/main/java/com/quannian/zhidong/ui/components/SkeletonOverlay.kt`
  - 骨架映射改为委托 `PoseCoordinateMapper`（单一可信来源，行为不变，且可单测）。

### 3.3 编译错误修复（2026-10-11，承接上一轮）
- `AiFacade.kt`：远程 OpenAI 兼容响应 `choices` 是 JSON 数组，`optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")`。
- `ApiKeyStore.kt`：security-crypto **1.0.0** API（与 1.1.0 不同）——类是 `MasterKeys`（复数），
  `MasterKeys.getOrCreate(AES256_GCM_SPEC)` 返回**别名 String**；
  `EncryptedSharedPreferences.create(alias, fileName, context, ...)` 以 String 别名传入。
- `SettingsScreen.kt`：保存按钮 `Row` 误用 `contentAlignment`，改 `horizontalArrangement = Arrangement.Center`。

### 3.4 AI Provider + 安全（任务 11，此前已完成，本轮复核）
- `CoachAIProvider`（接口）+ `LocalCoachProvider`（离线默认）+ `RemoteLLMProvider`（远程，无 Key 自动降级本地）。
- `AiFacade` 统一入口：远程优先、本地兜底；`CapabilityRegistry` 按 `AnalysisCapability`
  诚实分级（ASSIST_POSE / MANUAL_ENTRY / NOT_IMPLEMENTED），不伪造自动计数。
- `ApiKeyStore` 用加密 SharedPreferences 存 Key（开发期本地机制，**非生产级密钥保护**，不硬编码、不进 Git、不进 APK 明文）。

## 4. 验证证据

| 项 | 命令 / 证据 | 结果 |
|----|-------------|------|
| 编译（主 + 测试类） | `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-daemon` | **BUILD SUCCESSFUL** |
| 单测 | 纯 JVM `java -cp … org.junit.runner.JUnitCore …`（含 `PoseAndVideoV3Test`） | **OK (80 tests)** |
| 打包（V3.1 终版） | `gradle :app:assembleDebug --no-daemon` | **BUILD SUCCESSFUL**，`app-debug.apk` **87.82MB**（2026-10-12 19:38，含 P2 修复 + 9 新视频 + 3 coach PNG） |
| 视频清单 | `PoseAndVideoV3Test`：总数 18 / 分组 10-4-4 / 可播 **16** / 方法说明 2 / id 唯一 / assetPath 规范 | 全绿 |
| 坐标映射 | `PoseAndVideoV3Test`：全帧映射 / 裁切映射 / 前置镜像翻转 / `cropMatters` 诊断 / 批量映射 | 全绿 |
| 备考计划视频解析（P1 修复） | `PoseAndVideoV3Test`：5 动作项经 `byId` 解析到真实 mp4 / BMI·肺活量保持空串 / 薄弱项优先排布仍解析正确 | 全绿（+3 条） |
| 真机安装/启动 | `adb install -r` + `pidof`/`dumpsys`（Xiaomi MI9/Android9） | **Success + 前台运行无 crash** |

测试类清单（`run_jvm_tests_v2.sh`，含 `PoseAndVideoV3Test`）：
`ExerciseAnalyzerTest`、`ScoreEngineTest`、`OneEuroFilterTest`、
`VideoAnalysisEngineTest`、`CoachMotionEngineTest`、`V2LogicTest`、`PoseAndVideoV3Test`。
合计 **80 条**（77 基线 + P1 修复 3 条；V3.1 更新视频数期望值，总数不变仍 80 全绿）。

### 3.6 V3.1 最终成品化冲刺（2026-10-12 19:xx）
- **P2 UI**：`YouthBodyTestReportScreen` "去生成备考训练计划" 与 `YouthBodyTestDetailScreen`
  "生成备考训练计划" 两处 CTA `Text+clickable` → Material 3 `Button`（防重复 + 正确导航 + 返回）。
  `CoachAvatar` 容器 0.7:1→1.5:1（匹配 1248×832 横版 PNG）；Onboarding 头像 260dp→200dp 防溢出。
- **数字人终版**：3 张 coach PNG 实查确认（高质量 3D、风格统一、年龄区分、银龄非病态）→ 无需重生成。
- **教学视频**：9 条可生成 pending 用 Agnes AIGC `text_to_video`（agnes-video-2.5-flash, 720P）
  生成并落地 `assets/videos/`（youth_plank + middle×4 + senior×4）；`TeachingVideoRepository`
  回填 `assetPath`；可播 7→16；middle/senior 不再空态；`PoseAndVideoV3Test` 期望值同步更新。
- **青年体测 7 项闭环**：练（教学视频/要点 + 生成备考计划）/测（人工录入，`source="manual"`）
  双操作逐项核验；识别能力诚实分级（自动/辅助/人工/未实现），不伪造自动计数。
- **相机/姿态/AI/Key/性能**：旋转/镜像/recycle 链、权限/`release()`、诚实本地降级、
  无硬编码 key、30s 超时 + 401 处理、数据最小化；APK 体积构成实测（87.82MB < 100MB）。
- **真机**：Xiaomi MI9/Android9 安装 Success、启动无 crash；MIUI 浮层拦截 UI 自动点击（已记录，需人工）。

### 3.5 交付验收轮：P1 修复（备考计划教学视频解析）
- 文件：`app/src/main/java/com/quannian/zhidong/service/TrainingPlanService.kt`
- 缺陷：`generate()` 里 `videoAsset = TeachingVideoRepository.videoFor(tpl.videoId)`，
  但 `tpl.videoId` 存的是**视频 id**（`BY_ID` 键，如 `forward_reach`/`pullup`/`endurance`），
  而 `videoFor(itemKey,…)` 期望**体测 item key**（`ITEM_TO_PRIMARY` 键，如 `seated_reach`/`pullup_situp`/`run_800_1000`）。
  命名空间错位 → 这 3 项查不到 → 返回 `null` → `videoAsset=""`，
  导致备考计划里"坐位体前屈 / 引体·仰卧 / 800-1000m"三项的"▶ 播教学视频"按钮**静默失效**。
- 修复：改为 `TeachingVideoRepository.byId(tpl.videoId)`（7 个 `tpl.videoId` 均为合法 `BY_ID` 键；
  方法说明型 `bmi_method`/`vital_capacity_method` 的 `assetPath` 为空 → `videoAsset=""` 保持设计内）。
- 固化：`PoseAndVideoV3Test` 新增 3 条用例锁定"备考计划 7 项 videoAsset 解析"行为。

## 5. 数字人 / 图标 / 设计系统（任务 8 / 13）状态

- 数字人卡通 3D：`coach_{youth,middle,senior}.png` 已接入（`CoachVisual` 唯一映射点），
  `coach_child.png` 及 child 分支已清；`CoachSpriteRenderer`/`CoachMotionEngine` 收敛到 3 段。
  **V3.1 实查确认**：3 张 PNG 均为 **1248×832 横版**、高质量风格化 3D、统一 teal/white/navy
  色彩体系、明确年龄差异（青年活力 / 中年稳重 / 银龄饱满非病态），风格一致 → 终版无需重生成。
- **V3.1 修复**：`CoachAvatar` 容器由旧假设的 480×960 竖版（0.7:1）改为匹配实际 **横版 1.5:1**，
  修复大尺寸容器（Onboarding 等）上下大留白 / 拉伸；Onboarding 头像 260dp→200dp 防窄屏溢出。
- 设计系统：`Palette` 配色已按 3 段收敛。
- **V3.1 P2 收敛**：报告页"去生成备考训练计划"与详情页"生成备考训练计划"两处 CTA 由
  `Text+clickable` 统一为 Material 3 `Button`（防重复点击 + 正确导航 + 返回），
  与全局 Material 组件语言一致；图标系统审计结论为"主导航/首页/卡片已用 Material 图标一致，
  emoji 仅作数据驱动内容字形，未盲换图标库"。

## 6. 真机验证（设备已接入；逐页操作待人工）

**V3.1 已用 Xiaomi MI9 / Android 9 完成安装 + 启动无 crash 验证。** 旋转/镜像/坐标修复在
逻辑层已单测，"骨架与真人严格重合"的最终验收仍需真机多样本逐页操作（MIUI 浮层拦截
`uiautomator` 自动点击，需人工手点）：
1. 安装 `app\build\outputs\apk\debug\app-debug.apk`（arm64-v8a）。
2. 跟练页（深蹲）竖拿手机，确认 upright 帧、左右肩/髋不互换、骨架贴合身体。
3. 切到前置摄像头，确认预览镜像画面与骨架严格重合（`mirror=true`）。
4. 视频分析页抽帧，确认 90/270° 源视频人体不横躺。
5. MIUI 等 ROM 编程点击被浮层拦截时，改人工手点确认（V2 已记录）。

## 7. 遗留 / 风险（诚实记录）

> **V3.1 最终成品化冲刺（2026-10-12 19:xx）已完成全部当前可执行任务**：
> P2 UI 修复、3 coach PNG 终版确认、9 条 AIGC 视频补齐（可播 7→16）、青年体测 7 项闭环核验、
> 相机/姿态/AI/Key 安全/性能代码审计、完整测试 + 重打 APK + 真机安装验证。下表为剩余项。

| 项 | 状态 | 说明 |
|----|------|------|
| P1 备考计划视频解析 | ✅ | `byId` 修复 + 3 测试固化（§3.5） |
| P2 报告/详情 CTA 按钮 | ✅ | 统一 Material `Button`（§5），编译绿 |
| 数字人 coach PNG 终版 | ✅ | 3 张实查（1248×832 高质量 3D，风格统一）；`CoachAvatar` 容器比 0.7→1.5 修复（§5） |
| 教学视频可播 16/18 | ✅ | 9 条 AIGC 已生成落地 + `assetPath` 回填；middle/senior 不再空态；80 测试绿 |
| 青年体测 7 项 练/测 闭环 | ✅（代码审计） | 入口/采集/计算/保存/历史/识别限制逐项核（§见下） |
| 相机/姿态/AI/Key/性能 | ✅（代码审计） | 旋转/镜像/回收链、权限/release、诚实降级、无硬编码 key、APK 87.82MB<100MB |
| API Key 安全 | ⚠️ | 开发期本地加密存储（Keystore），**不声称生产级密钥保护**；生产应服务端代理 |
| 真机逐页操作验收 | ⏳（人工） | MI9/Android9 已安装并启动无 crash；MIUI 浮层拦截 UI 自动点击，跟练/相机/逐页点击需人工（§6 清单） |
| 视频"动作内容"真机抽帧验收 | ⏳（人工） | 文件存在/ftyp 格式/资源路径/入包 已验；"动作内容是否与名称匹配"需真机播放抽帧确认 |
| MoveNet 切换 | ⏳（不盲换） | 结论保留 MediaPipe，见 `MOVENET_EVALUATION.md` |

## 青年体测 7 项 练/测 闭环（任务 7 核验结论，代码审计）

每项含：练习入口（教学视频/要点 + "生成备考训练计划"）与测试入口（人工录入成绩，`source="manual"`）双操作，
保存进 Room `body_test_results`，历史页查看，报告页趋势。识别能力诚实分级：
- **BMI**：身高/体重双值录入，`BMI=kg/m²` 计算，合法数据校验（`h>0`）。
- **肺活量**：方法说明 + 人工录入实测 mL（**非**摄像头测量，诚实标注）。
- **50m / 1000·800m**：人工录入计时（秒），未做自动计时，诚实标注。
- **坐位体前屈 / 立定跳远**：人工录入距离（cm），自动测距未开放（诚实）。
- **引体向上 / 仰卧起坐**：按性别展示，人工录入次数；识别未验证前**不**提供自动计数（诚实）。
- 备考计划 7 项 `videoId` 经 `byId` 解析到真实 mp4（P1 已修，3 测试固化）。
- 跟练自动计数（深蹲/开合跳/八段锦/太极/肩颈）为"辅助识别"，`MIN_VIS` 可见性阈值 + 迟滞去抖 + 空帧/不完整帧兜底，非正式计数。

## 8. 下一步（剩余项，需人工/外部）

1. **人工真机逐页操作**：按 §6 清单在 MI9 上点击验证（视频播放 / 跟练计数 / 相机骨架 / 前后摄 / 返回重进 / 4 底栏）。
2. **视频动作内容抽帧**：真机播放 16 条视频，确认动作与名称匹配（当前文件/格式/路径/入包已验，内容级需视觉确认）。
3. **生产级 AI Key**：正式上架前接服务端代理保管密钥（区分开发功能 vs 部署需求）。
4. **MoveNet**：若真机跟练出现帧率瓶颈，按 `MOVENET_EVALUATION.md §6` 立项切换（当前不盲换）。
