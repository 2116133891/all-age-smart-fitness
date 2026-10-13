# 全龄智动 V3 参赛产品重构 — 实施计划 / 进度（可中断恢复）

> 分支 `v3-dev`（基于 V2 `main` @20a14ef）。基线备份见 `v2-backup-prechange`。
> 环境：JDK17 + Gradle 8.3 + SDK34（`C:\Users\23163\.android-build`）；`local.properties` 已指向该 SDK。
> 构建：`gradle.bat :app:assembleDebug --no-daemon`；单测：`bash run_jvm_tests_v2.sh`（中文路径 Gradle test 失败，手跑 java -cp 绕过）。

## 0. 现状检查结论（阶段 0 已完成）
- **分支/工作区**：`main` 干净、与 `origin/main` 同步；已建 `v3-dev`。
- **年龄体系**：`AgeGroup` 枚举已是 3 组（youth/middle/senior，10-10 已去 child）。但**儿童残留**仍在：
  `ProfileScreen` 仍列 `child` 选项、`coach_child.png` 资源、`LocalAssistant`/`LocalCoachProvider`/`CoachMotionEngine`/`CoachSpriteRenderer`/`Profile.ageLabel` 的 child 分支、`Palette` 的 child 配色。
- **性别**：`Profile.gender` 默认 `unknown`，`ProfileScreen` 有"不限"（unknown）选项，`genderLabel` 映射"不限"。需收敛为 男/女 两选项 + 明确默认。
- **首页**：`HomeScreen` 同时有 `YouthBodyTestFeatureCard`（"青年·体质测试"）和 `AgeCard(YOUTH)`（"青年 体测 教练·重点"）= **重复青年入口**，需合并为 1 个；并存在 AI 诊断/助手/手环/我的训练/个性化 多卡堆叠。
- **底部导航**：现为 `首页/功能/助手/我的` 4 tab。需改为 `首页/训练/AI 助手/我的`，统一一套导航。
- **体测详情**：`YouthBodyTestDetailScreen` 需收敛为"练/测"两大操作。
- **数字人**：`coach_{youth,middle,senior}.png` 已接入（CoachVisual 唯一映射点）；`coach_child.png` 需删除；`CoachSpriteRenderer`（旧 Canvas 火柴人，Age.CHILD）需逐页确认清理。
- **AI**：`LocalCoachProvider`（离线）+ `RemoteLLMProvider`（目前恒降级本地，未真发网络）。需统一 Provider 抽象 + API Key 安全配置（不硬编码、不进 Git、不进 APK 明文）。
- **视频**：`assets/videos/` 现有 7 条（全 youth）。需扩展到任务书 18 条基础集（youth 10 + middle 4 + senior 4），并统一卡通 3D 风格。
- **姿态识别**：`PoseDetector`（MediaPipe 33 点）+ `CameraManager`（CameraX）。任务 17 需诊断旋转/镜像/坐标映射。
- **资源**：`coach_*.png`、7 mp4、`pose_landmarker.task`(5.7MB) 均在库。`.gitignore` 已排除 `.agnes/`、`local.properties`、密钥文件建议补充。

## 1. 任务状态表（持续更新）
| # | 任务 | 状态 | 修改文件 | 备注 |
|---|------|------|---------|------|
| 1 | 删儿童产品线 | ✅ | Profile(LocalAssistant/LocalCoachProvider/CoachMotionEngine/CoachSpriteRenderer/CoachVisual/ProfileScreen/Theme) 已接入 AgeGroupConfig.normalizeAgeId() | child→youth 兜底；coach_child 分支已清 |
| 2 | 统一 3 成年年龄段 | ✅ | AgeGroupConfig(BANDS/GENDERS)+normalizeAgeId | 3 成年带；未知/child 兜底 |
| 3 | 性别仅男/女 | ✅ | AgeGroupConfig.GENDERS + ProfileScreen + Profile 默认 male | 去"unknown"，默认 male |
| 4 | 首页重设计 | ✅ | HomeScreen(MainTasksCard/RecommendedContentCard) | 入口+主要任务+推荐 |
| 5 | 合并青年入口 | ✅ | HomeScreen(AgeCard→onYouthBodyTest) | 单一青年体测入口 |
| 6 | 每项目仅练/测 | ✅ | YouthBodyTestDetailScreen(测=录入+评分 / 练=生成备考计划)+TrainingPlanService | 已逐页审计 7 项双操作；修 P1：备考计划"▶播教学视频"经 byId 解析（坐位体前屈/引体·仰卧/800-1000m 原静默失效）；JVM 单测 +3 固化 |
| 7 | 底部导航 4 tab | ✅ | NavigationHost(首页/训练/AI助手/我的)+TrainingHubScreen | 去 功能 tab，新建 训练 |
| 8 | 数字人卡通 3D | ✅ | CoachVisual 已收敛 3 段 + V3.1 容器比 0.7→1.5 | 3 张 PNG 实查终版（1248×832 高质量 3D，风格统一）；CoachAvatar 横版容器修复 + Onboarding 防溢出 |
| 9 | 教学视频扩展 | ✅ | TeachingVideoRepository(18 条)+PoseAndVideoV3Test+VIDEO_ASSET_MANIFEST | V3.1 补齐 9 条 AIGC 视频 → 可播 16 / 方法说明 2；middle/senior 不再空态；单测固化 |
| 10 | 姿态识别诊断 | ✅ | CameraManager(旋转修复)+PoseCoordinateMapper(新增)+SkeletonOverlay(委托) | 旋转→upright 帧；镜像/crop 诊断单一可信来源；JVM 单测覆盖映射 |
| 19 | MoveNet 评估 | ✅ | MOVENET_EVALUATION.md | 结论：保留 MediaPipe，MoveNet 记录为备选（无真机瓶颈证据，不盲换） |
| 11 | AI Provider + 安全 | ✅ | AiFacade/ApiKeyStore/CapabilityRegistry/SettingsScreen(AiServiceConfigPanel) | 远程优先+本地降级；Key 加密存；能力诚实分级 |
| 12 | 数据闭环（体测/训练/历史/报告） | ✅ | db/(Room v2)+FollowAlongViewModel.finishReport | 已审计：Room v2 迁移幂等（v1 旧表不动）；结束训练**即时落库**（不依赖报告页）；历史/趋势/阶段报告齐全；异常退出数据不丢 |
| 13 | 设计系统/图标/性能 | ✅ | theme/ + 报告/详情 CTA Button + CoachVisual 容器 | V3.1：CTA 统一 Material Button；图标审计（Material 一致，emoji 仅作内容字形，不盲换库）；配色已收敛；性能体积实测（APK 87.82MB<100MB） |
| 14 | 测试/构建/APK/真机/文档 | ✅ | V3_1_TASK_CHECKLIST + assembleDebug + 单测 + 报告 | V3.1 终版：编译 **BUILD SUCCESSFUL**；单测 **80 全绿**；APK **87.82MB**（19:38，含 P2+9 视频+3 PNG）；真机 MI9 安装 Success 启动无 crash；逐页点击待人工（MIUI 浮层） |

## 2. 恢复说明（新会话）
1. `git checkout v3-dev`；读本文件 + `V3_IMPLEMENTATION_REPORT.md`。
2. 环境：`$env:JAVA_HOME="C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"`（或现有）；`$env:ANDROID_HOME="C:\Users\23163\.android-build\android-sdk"`。
3. 构建：`gradle.bat :app:assembleDebug --no-daemon`；产物 `app\build\outputs\apk\debug\app-debug.apk`。
4. 单测：`bash run_jvm_tests_v2.sh`。
5. 从 §1 表第一个 ⏳ 继续。

### 本轮完成（2026-10-12）：余待办 5 项推进
- **教学视频 18 条扩展（任务 9 ✅）**：`TeachingVideoRepository` 由 7 条扩到 18 条基础集（youth 10 + middle 4 + senior 4）；新增 `allItems()`/`byAgeId()`/`byId()` 全量索引，`allVideoItems()` 仍只返回可播 7 条；方法说明型（bmi/vital_capacity）保留。可播 7 / pending 11。新增 `PoseAndVideoV3Test` 固化总数 18、分组 10-4-4、可播 7、pending 11、id 唯一、assetPath 前缀规范。
- **姿态识别旋转/镜像/坐标诊断（任务 10 ✅）**：`CameraManager.analyzeFrame` 增加 `image.imageInfo.rotationDegrees` → 旋转为 upright 帧再推理（修复横拿手机 90/270° 人体横躺导致的左右肩/髋互换与深度角失真）；新增纯逻辑 `PoseCoordinateMapper`（crop 裁切 + 前置镜像 + `cropMatters` 诊断，JVM 可测）；`SkeletonOverlay` 改为委托 mapper（单一可信来源，行为不变）。回收链重写避免 double-recycle。
- **MoveNet 评估（任务 19 ✅）**：新增 `MOVENET_EVALUATION.md`，结论**保留 MediaPipe**，MoveNet 记录为备选（无真机帧率瓶颈证据，不盲换；切换需 `MoveNetDetector` 同接口 + 真机多样本回归）。
- **文档三件套**：`VIDEO_ASSET_MANIFEST.md`（18 条清单 + 7 mp4 落地校验 + 11 pending 生成指引）、`MOVENET_EVALUATION.md`、`V3_IMPLEMENTATION_REPORT.md`。
- **验证**：`:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` **BUILD SUCCESSFUL**；纯 JVM 单测 **77 全绿**（含 14 条 `PoseAndVideoV3Test`）；`:app:assembleDebug` 产出 **app-debug.apk 78.22MB**（2026-10-12）。
- **真机验证**：仍待人工（见 §3 / 报告 §6）；旋转/镜像修复在**逻辑层**已通过单测验证，真机多样本回归为下一步。

### 本轮已修复的编译错误（2026-10-11）
- `AiFacade.kt` 解析远程响应：`choices` 是 JSON 数组，原写 `optJSONObject("choices")?.optJSONArray(0)`（类型不匹配 188 行报错）。改为 `optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")`。
- `ApiKeyStore.kt` 未解析 `MasterKey`：根因是 **security-crypto 1.0.0 的 API 与 1.1.0 不同**——该类叫 `MasterKeys`（复数），`MasterKeys.getOrCreate(AES256_GCM_SPEC)` 返回的是**别名 String**，`EncryptedSharedPreferences.create(alias, fileName, context, ...)` 以 String 别名传入。已按 1.0.0 签名改写。
- `SettingsScreen.kt` 保存按钮 `Row` 误用 `contentAlignment`（Row 无此参），改为 `horizontalArrangement = Arrangement.Center`。
- 以上三者后 `:app:compileDebugKotlin` 与 `:app:assembleDebug` 均 **BUILD SUCCESSFUL**，APK 78.2MB。

### 交付验收轮（2026-10-12 17:xx）：只读审计 + 修复 + 复验
> 本轮目标：不推倒重写、不扩大功能，逐项消除剩余问题，交付可演示/可操作/稳定的 APK。

**A. 只读审计结论（A–H 全项）**
- 无 P0 阻断（未发现崩溃、假数据、空按钮、导航死链）。
- **P1（已修）**：`TrainingPlanService.generate()` 用 `videoFor(tpl.videoId)`，但 `tpl.videoId` 是**视频 id**（`BY_ID` 键）而 `videoFor()` 期望 **item key**（`ITEM_TO_PRIMARY` 键），命名空间错位 → 坐位体前屈 / 引体·仰卧 / 800-1000m 三项在"备考计划"里的"▶ 播教学视频"按钮**静默失效**（按钮在，点了无视频）。修法：改 `TeachingVideoRepository.byId(tpl.videoId)`。
- **P2（外部依赖，本轮不盲改）**：① 11 条 pending 视频 mp4（AIGC 生成）；② 数字人卡通 PNG 终版（现用 coach_{youth,middle,senior}.png 各 ~600KB 已可渲染，终版视觉待确认）；③ 真机多样本回归（旋转/镜像/骨架贴合）；④ 报告页"去生成计划"按钮用 `Text+clickable` 略不醒目（可后续换 Button，不影响功能）。
- 已核证据：7 条 youth mp4 磁盘存在且 `ftyp/isom` 头有效；3 张 coach PNG 存在；child 残留仅数据归一化（无 UI 入口）；4-tab 导航 saveState 保留各子栈；`FollowAlongViewModel.finishReport` 结束即落库 Room（异常退出不丢数据）。

**B. 修复 + 复验**
- 修 P1：`TrainingPlanService` 1 行（`videoFor`→`byId`，含注释说明命名空间）。
- 新增 JVM 单测 3 条（`PoseAndVideoV3Test`）：固化"备考计划 7 项 videoAsset 解析"——5 项动作项经 byId 解析到真实 mp4、BMI/肺活量方法型保持空串、薄弱项优先排布下仍解析正确。
- 复验：`:app:compileDebugKotlin :app:compileDebugUnitTestKotlin` **BUILD SUCCESSFUL**；纯 JVM 单测 **OK (80 tests)**（77 基线 + 3）；`:app:assembleDebug` 产出 **app-debug.apk 78.22MB**（2026-10-12 17:41）。
- 文档同步：本计划表（任务 6/12/14 状态）+ `V3_IMPLEMENTATION_REPORT.md`（§7 遗留项 / §4 验证证据）。

### V3.1 最终成品化冲刺（2026-10-12 19:xx，本轮）
- **P2 UI**：`YouthBodyTestReportScreen` + `YouthBodyTestDetailScreen` 两处 CTA `Text+clickable` → Material 3 `Button`（防重复 + 正确导航 + 返回）；`CoachAvatar` 容器 0.7:1→1.5:1（匹配 1248×832 横版 PNG）+ Onboarding 260dp→200dp 防溢出。
- **数字人终版**：3 张 coach PNG 实查确认（高质量 3D、风格统一、年龄区分、银龄非病态）→ 无需重生成。
- **教学视频**：9 条可生成 pending 用 Agnes AIGC `text_to_video`（720P）生成并落地 `assets/videos/`；`TeachingVideoRepository` 回填 `assetPath`；可播 7→16；`PoseAndVideoV3Test` 期望值同步（16 可播 / 2 方法说明 / youth 可播 8）。
- **青年体测 7 项 练/测 闭环**：逐项核验入口/采集/计算/保存/历史/识别限制；识别能力诚实分级（自动/辅助/人工/未实现）。
- **相机/姿态/AI/Key/性能代码审计**：旋转/mirror/recycle 链、权限/release、诚实本地降级、无硬编码 key、APK 体积构成实测。
- **验证**：`:app:compileDebugKotlin :app:compileDebugUnitTestKotlin` **BUILD SUCCESSFUL**；纯 JVM **OK (80 tests)**；`:app:assembleDebug` → **app-debug.apk 87.82MB**（19:38）；真机 Xiaomi MI9/Android9 **install Success + 启动无 crash**（逐页点击因 MIUI 浮层需人工）。
- 详见 `V3_1_EXECUTION_STATE.md` / `V3_1_TASK_CHECKLIST.md`。

## 3. 风险 / 阻塞
- **真机逐页操作**（V3.1 已有 MI9/Android9 设备）：安装/启动无 crash 已程序化确认；
  跟练计数 / 相机骨架 / 前后摄 / 4 底栏 / 16 条视频播放的**逐页点击**因 MIUI `rapid_charge`
  浮层拦截 `uiautomator`，需人工手点确认（清单见 `V3_1_TASK_CHECKLIST.md §F`）。
- **视频"动作内容"抽帧**：文件存在 / ftyp 格式 / 资源路径 / 入包已验；"动作与名称匹配"
  需真机播放视觉确认（ffmpeg 不在本机，无法离线抽帧，如实记录，不编造）。
- MoveNet 换模型：仅做评估记录，**默认保留 MediaPipe**（诚实，不盲换；帧率瓶颈时立项）。
- API Key：开发期走本地加密存储（Keystore）+ 诚实内存降级；**不声称生产级密钥保护**，
  正式上架应由服务端代理保管密钥。
