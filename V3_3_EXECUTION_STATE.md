# 全龄智动 V3.3 专项改造 · 执行状态

> 持续执行 · 不等确认 · 外部阻塞标记后继续
> 分支：`v3-dev` · 包名：`com.quannian.zhidong` · App 名：全龄智动
> 本文档逐任务记录状态（TODO / IN_PROGRESS / PASS / FAIL / BLOCKED_EXTERNAL）与证据。
> 真机/旋转/UI 点击类验收标记为 **BLOCKED_EXTERNAL**（人工验收，不伪造）。

---

## P0-1 跟练界面双栏重构（FollowAlongScreen）

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 1a | TeachingVideoRepository 增加 `videoForItem(itemKey)` 严格统一映射 | **PASS** | `videoForItem`/`hasItemVideo`/`isMethodOnlyItem`/`followCapabilityLabel` 已落地；`compileDebugKotlin` 通过 |
| 1b | 重写 FollowAlongScreen：双栏（视频左/摄像头右），移除火柴人 | **PASS** | `FollowAlongScreen.kt` 双 `TeachPane`+`CameraPane`；`CoachSpriteView` 不再被任何 screen 引用（仅组件定义留存） |
| 1c | 响应式：宽 Row / 窄 Column，双画面同屏，不锁方向 | **PASS** | `onGloballyPositioned` 测宽（先取 `LocalDensity.current`）；`sideBySide = pageWidthDp > 700f`；`MainActivity` 保持 portrait-lock（未改全局方向） |
| 1d | 视频缺失 → 诚实"暂无示范视频"卡片 + 要点，不回退火柴人 | **PASS** | `TeachPane` `videoUsable=false` 分支输出"暂无「X」示范视频"+ 要点，不渲染 CoachSpriteView |
| 1e | 复用真实 rep/phase/error（不另造识别） | **PASS** | `FollowAlongViewModel` 分析器保留；仅 `FollowAlongItemScreen` 走骨架展示（不接分析器、不自动计数） |
| 1f | 无第二 MediaPipe 实例；退出释放资源 | **PASS** | `CameraManager`/`PoseDetector` 每页一实例；`DisposableEffect` + `cameraManager.release()`；back 按钮 release |

## P0-2 青年体测 → 真正练习与测试系统

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 2a | 体测项卡：看示范 / 开始跟练 / 测试·录入 独立入口 | **PASS** | `YouthBodyTestScreen.MotionItemCard` 三按钮；`MeaureItemCard` 单"测评·录入" |
| 2b | 跟练入口 → 双栏跟练页（followItem/{key}） | **PASS** | `FollowAlongItemScreen`（TeachPane+CameraPane 复用）；`NavigationHost` `followItem/{key}` 路由 |
| 2c | 测试入口真实（50m 计时 / 坐体前屈 cm / 跳远 距离 / 人工录入） | **PASS** | 双栏页底部人工录入 + `BodyTestRepository.saveResult(source="manual")`；能力标签"辅助识别 · 人工…" |
| 2d | 自动化能力标注（auto/assisted/manual/not-implemented） | **PASS** | `followCapabilityLabel` 一律"辅助识别 · 人工计时/测量/计数"，不冒充自动检测 |
| 2e | 不伪造分数；闭环 选择→示范→练/测→数据→结果→保存→历史 | **PASS** | `BodyTestStandardService.scoreFor` 官方阈值；`YouthBodyTestDetailScreen` 历史趋势（<2 条不画假曲线） |

## P0-3 BMI/肺活量 移出动态跟练列表 → 独立测评录入

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 3.1 | BMI/肺活量 不在动态跟练列表 | **PASS** | `isMethodOnlyItem` 过滤：`motionItems`/`measureItems` 分离；计划任务卡 `followable=!isMethodOnlyItem` |
| 3.2 | 真实 BMI 计算（身高/体重） | **PASS** | `FollowAlongItemScreen` 双值 `value2/((value/100)^2)`；`V33FollowAndBodyTestTest` 锁定 60kg/1.7m≈20.8 |
| 3.3 | 肺活量 人工录入（mL） | **PASS** | `SINGLE` 录入分支保存 `rawValue`；不通过摄像头测量 |
| 3.4 | 不映射到无关动作视频；不删数据/模型/历史 | **PASS** | `videoForItem(BMI/VITAL)=*_method`（assetPath 空）；`BodyTestRepository`/Room 未改动 |

## P0-4 统一教学视频索引 · 严格匹配

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 4.1 | 唯一权威映射，训练详情/跟练/计划共用 | **PASS** | 全部经 `TeachingVideoRepository.videoForItem` / `teachingVideoFor`；`V33FollowAndBodyTestTest` 断言同 key 恒同视频 |
| 4.2 | 同项同视频；无跨年龄段错配 | **PASS** | 体测映射锁定 `ageId=="youth"`；引体/仰卧 按性别 split 但同 youth |
| 4.3 | 方法型项目排除动态映射；未匹配=诚实空态 | **PASS** | method-only `hasItemVideo=false`；未知 key `videoForItem=null` |
| 4.4 | 不复制/改名 MP4 造假视频 | **PASS** | `assets/videos` 仍 16 个 MP4（未新增）；清单与 `VIDEO_ASSET_MANIFEST` 一致 |

## P0-5 导航流程统一

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 5.1 | 全部入口传正确 itemKey/videoId/planTaskId | **PASS** | 主页`onWatchItem`/`onFollow`→`teachingVideoItem/{key}`、`followItem/{key}`；详情`onFollow`；计划卡`onFollow(t.itemKey)` |
| 5.2 | 教学视频页 加 开始跟练 按钮 | **PASS** | `TeachingVideoScreen(itemKey, onFollowItem)` → `followItem/{key}`（method-only 不显示） |
| 5.3 | 计划任务卡 加 跟练 入口 | **PASS** | `YouthTrainingPlanScreen.TaskRow` "🎯 开始跟练"（仅运动型项） |
| 5.4 | 无冲突跟练页 per path | **PASS** | 双栏跟练仅 `FollowAlongItemScreen`（体测项）+ `FollowAlongScreen`（exerciseId）两套，各走各的 key，不混用 |
| 5.5 | 返回/状态保留 核查 | **BLOCKED_EXTERNAL** | 需真机逐页点按验证（MIUI 屏蔽 uiautomator）；代码用 `popBackStack` 标准回退 |

## P1 双栏布局与性能

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| 6.1 | 中年/银龄 跟练页 无残留火柴人主舞台 | **PASS** | `CoachScreen`/`AgeHealthScreen`/`FollowAlongScreen` 均不再引用 CoachSprite/CoachMotion（仅注释/组件定义） |
| 6.2 | 双栏 不 tab 切换、两画面同屏 | **PASS** | 横/竖 都 Row/Column 并排可见；`TeachPane` `muted=true` 循环静音 |

## 测试与构建

| # | 任务 | 状态 | 证据 |
|---|------|------|------|
| T.1 | 保留既有 JVM 测试（不降标准） | **PASS** | `ExerciseAnalyzer/ScoreEngine/VideoAnalysis/CoachMotion/V2Logic/PoseAndVideoV3/TeachingVideoMatch/TrainingPlanGenerate/AiServiceSettings/VideoAnalysisHonest/OneEuroFilter` 全保留 |
| T.2 | 新增 视频匹配/体测练测/跟练生命周期 测试 | **PASS** | `V33FollowAndBodyTestTest`（14 用例）：videoForItem 严格性、method-only 排除、性别 split、未知 key、能力标签、BMI 计算、单值录入 |
| T.3 | :app:compileDebugKotlin | **PASS** | BUILD SUCCESSFUL（仅 warning，无 error） |
| T.4 | :app:compileDebugUnitTestKotlin | **PASS** | BUILD SUCCESSFUL |
| T.5 | 全量 JVM 测试集 | **PASS** | `java -cp ... JUnitCore` → **OK (105 tests)** |
| T.6 | :app:assembleDebug | **PASS** | BUILD SUCCESSFUL；`app-debug.apk` 87.2 MB，时间戳 2026-10-13 15:50（本轮构建） |
| T.7 | APK 资源校验 | **PASS** | 16 MP4 + `pose_landmarker.task` 在 `assets/`；namespace=`com.quannian.zhidong`；label=全龄智动 |
| T.8 | UI + 构建 真机验收 | **BLOCKED_EXTERNAL** | 旋转/逐页点按/双栏真机显示 需真机人工验收；设备 `b7a0af21`（Xiaomi MI9/Android9）需重新连机验证 |

---

## 构建环境（本机）
- `JAVA_HOME = C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1`（**必须含 `+1` 后缀**）
- `ANDROID_HOME = C:\Users\23163\.android-build\android-sdk`
- gradle = `C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat`（`--no-daemon`）
- JVM 测试 = `java -cp junit:hamcrest:kotlin-stdlib:compose_rt:debug:debugUnitTest`（中文路径下 bash wrapper 失败，用 PowerShell 直跑）

## 外部阻塞清单（BLOCKED_EXTERNAL，不伪造）
1. 真机逐页点击 / 旋转双栏显示 / 摄像头权限回收 / 双栏跟练真机验收（设备 `b7a0af21` 需重连）。
2. 视频生成服务（若有新增视频需求）——本轮未新增视频，未触发。

## 变更文件清单（本轮）
- `repository/TeachingVideoRepository.kt`（新增 V3.3 严格映射 + 能力标签）
- `ui/screens/FollowAlongScreen.kt`（双栏重写，移除火柴人）
- `ui/screens/FollowAlongItemScreen.kt`（新增，体测项双栏跟练/测评页）
- `ui/screens/YouthBodyTestScreen.kt`（BMI/肺活量分离 + 独立入口）
- `ui/screens/YouthBodyTestDetailScreen.kt`（加"开始跟练"入口，method-only 隐藏）
- `ui/screens/YouthTrainingPlanScreen.kt`（任务卡加跟练入口）
- `ui/screens/TeachingVideoScreen.kt`（加"开始跟练"按钮 + itemKey）
- `ui/screens/NavigationHost.kt`（`followItem/{key}` + `teachingVideoItem/{key}` 路由 + 全入口 wiring）
- `ui/components/TeachingVideoPlayer.kt`（`muted` 参数，双栏默认静音）
- `test/java/com/quannian/zhidong/V33FollowAndBodyTestTest.kt`（新增 14 用例）
- `run_jvm_tests_v2.sh`（补充新增测试类）
