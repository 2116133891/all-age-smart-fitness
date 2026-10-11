# 全龄智动 · 参赛版 V2 实施计划（V2_IMPLEMENTATION_PLAN）

> 生成：2026-10-11 · 主程：Agnes-3.0-flash · 项目根：`C:\Users\23163\Desktop\城市大赛项目`
> 目标：面向「青年体质测试 / 中年健康管理 / 银龄科学运动」的全龄智能运动指导平台。
> 比赛：第十二届中国研究生智慧城市技术与创意设计大赛。

## 状态图例
- `TODO` 未开始 · `IN_PROGRESS` 进行中 · `DONE` 完成 · `BLOCKED` 被阻塞（记录原因）

## 构建环境（本机已验证可用）
- JDK17：`C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1`
- Gradle 8.3：`C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat`
- Android SDK：`C:\Users\23163\.android-build\android-sdk`（platform 34 + build-tools 34）
- `local.properties` 已指向 23163 机器 SDK。
- 构建：`$env:JAVA_HOME=...jdk17; gradle.bat :app:assembleDebug --no-daemon`
- 单测：`bash run_jvm_tests.sh`（中文路径下 Gradle test 有 ClassNotFoundException，手跑绕过）
- 设备：MI 9 / Android 9 / arm64（`b7a0af21`，可能未连接）

## 备份
- `v2-backup-prechange` 分支 = 任务开始前基线（含用户 versionCode bump 已提交）。
- 开发在 `v2-dev` 分支。

## 任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 0 | 项目接管、备份、代码审查 | DONE | 代码审查完成；构建环境验证 |
| 0a | Git 备份分支 | DONE | v2-backup-prechange（含用户 versionCode bump） |
| 1 | 重新定位产品与首页（三大人群入口 + 重点青年） | DONE | AgeGroup 加 focus 标签；首页青年体测重点卡；hero 文案重定位；AgeScreen 青年→新模块 |
| 2 | 三类数字人教练（识别/裁切/处理提供的合并图） | DONE | 用 Agnes i2i 从用户合并图分离 3 张独立全身 PNG（coach_youth/middle/senior，864x1152，统一 3D 风+品牌色）；CoachVisual 补 middle 映射 |
| 3 | 可扩展教学视频系统（播放器 + 真实视频 + 索引） | DONE | TeachingVideoPlayer（Media3 ExoPlayer）+ TeachingVideoRepository 索引 + 缺失资源处理 + 全屏路由 |
| 3a | 第一批 7 条青年体测教学视频（或记录阻塞） | DONE | Agnes text_to_video 生成 7 条 720P H.264 MP4（~5.4MB）落地 assets/videos/ |
| 3b | Media3 ExoPlayer 播放器 | DONE | 播放/暂停/进度/全屏/错误提示/释放/本地 assets/重播 全满足 |
| 4 | 青年体测完整模块（七项） | DONE | BodyTestCatalog + YouthBodyTest 主页/详情（视频+录入+标准评分+趋势）+ 性别分支 |
| 5 | 体测标准与评分体系（可维护/可追溯/版本） | DONE | BodyTestStandardService（来源/版本/适用范围 + 保守阈值 + 不适用则不输出分数） |
| 6 | 训练计划与智能推荐（数据模型+校验+安全边界） | DONE | TrainingPlanService（数据模型+规则，非 LLM 自由文本；安全边界文案；周渐进） |
| 7 | 训练报告与成绩趋势（真实数据，不足则提示） | DONE | YouthBodyTestReportScreen（完成/未录入/最近/变化/完成率/弱项；不足提示不画假曲线） |
| 8 | 中年与银龄功能完善 | IN_PROGRESS | 中年 BMI/低冲击 + 银龄 太极/八段锦 大字体 复用现有；待补 |
| 9 | 训练历史与个人档案（Room migration 不破坏旧库） | DONE | QuanNingDatabase v1→v2（+BodyTestResult/TrainingPlanTask 表 + 手工 migration；旧 training_sessions 保留） |
| 10 | 开源项目研究与选择性借鉴 | TODO | SmartForm/AI Sport/pose correction/MediaPipe（记录到 OPEN_SOURCE_REFERENCES） |
| 11 | 统一 UI 与视觉资源 | IN_PROGRESS | 三教练图 + 青绿/深蓝/白 色系统一（Palette）；待 全页 走查 |
| 12 | 自动化测试与真机验证 | DONE(单测)/TODO(真机) | V2LogicTest 19 项 + 全套 63 项 全过；真机 MI 9 待安装验证 |
| 13 | APK、文档与最终交付 | IN_PROGRESS | 待 构建 APK + 更新 README/PROJECT_STATUS/OPEN_SOURCE + 生成 V2_IMPLEMENTATION_REPORT |

## 代码审查关键结论（任务 0）
- **技术栈**：Kotlin 1.9.10 · AGP 8.1.1 · Compose BOM 2023.10.01 + material3 1.1.2 · CameraX 1.3.0 · MediaPipe tasks-vision 0.10.14（本地 pose_landmarker.task 5.7MB）· Room 2.5.2（KSP）· minSdk24/targetSdk34。
- **导航**：4-tab（首页/功能/助手/我的）+ 子路由（splash→onboarding→profile→home；age/{id}→bodyTest/exercise/{id}→coach/{id}→follow/{id}→report；videoPick→videoAnalysis→videoReport；training/wearable/settings）。
- **数字人**：`CoachVisual.coachResource(ageId)` 是唯一资源映射点；`coach_child/coach_youth/coach_senior` PNG 已在 `res/drawable/`。**缺 `middle` 映射**（`coachResource` 未处理 "middle"，会回退 youth）。
- **首页动画**：`HomeScreen` 已用一次性 220ms 淡入 + `CoachAvatar` 静止站立（无 infinite 循环），符合"默认静止"。
- **体测现状**：`BodyTestScreen` 是 5 项星级自评（青年/中年/银龄各一套文案），**不是**七项真实体测（BMI/肺活量/50m/坐位体前屈/立定跳远/引体·仰卧起坐/1000-800m）。需改造。
- **Room**：`QuanNingDatabase` version=1，仅 `TrainingSession`。需为体测成绩新增实体 + migration 到 v2（不破坏旧表）。
- **README 与代码一致性**：README 仍写"儿童/青年/银龄"三人群、"三个运动"，但代码已是 青年/中年/银龄 且含 9 分析器 + 视频分析 + 手环 + 助手。**README 需更新**（任务 13）。
- **视频**：目前无任何真实视频；只有"上传本地视频→离线分析"链路（VideoPick/Analysis/Report）。教学页无播放视频。需新建播放器 + 真实视频资源（任务 3）。
- **未提交改动**：用户已 bump versionCode 1→2 / versionName 1.0→1.1.0（已提交到 main 并作为基线）。mipmap PNG 目录为未跟踪（新增 app 图标兜底）。

## 资源 / 媒体
- 用户提供合并数字人图：`.agnes/uploads/20261011_1/upload_73f11407...png`（1536x1024，青年男/中年女/银龄男三合一，青绿白深蓝运动服，浅灰棚拍背景）。
- 视频生成：Agnes AIGC 内置 `agnes_aigc__text_to_video`（文生视频），可生成真实 MP4 并落地到工作目录；用于第一批 7 条教学视频。
- 若视频生成不可用/体积过大：先落地播放器 + 资源索引 + 数据模型 + 教学页，并如实记录"未生成/占位"，**不伪称已生成**。
