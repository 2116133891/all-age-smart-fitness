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
| 1 | 删儿童产品线 | ⏳ | ProfileScreen/LocalAssistant/LocalCoachProvider/CoachMotionEngine/CoachVisual/Profile/Palette + 删 coach_child.png | 全项目检索后清理 |
| 2 | 统一 3 成年年龄段 | ⏳ | 集中 AgeGroup/Profile/配置层 | 年龄范围写产品默认，体测标准另配 |
| 3 | 性别仅男/女 | ⏳ | Profile/ProfileScreen/Palette | 去"不限"，明确默认 |
| 4 | 首页重设计 | ⏳ | HomeScreen | 3 入口+主要任务+推荐+空态 |
| 5 | 合并青年入口 | ⏳ | HomeScreen/YouthBodyTestScreen | 单一青年体测入口 |
| 6 | 每项目仅练/测 | ⏳ | YouthBodyTestDetailScreen | 7 项目统一双操作 |
| 7 | 底部导航 4 tab | ⏳ | NavigationHost + 新建 Training tab | 首页/训练/AI/我的 |
| 8 | 数字人卡通 3D | ⏳ | 生成 3 独立 PNG + 中央配置 + 清理火柴人 | 见 §视频 |
| 9 | 教学视频扩展 | ⏳ | assets/videos + TeachingVideoRepository | 18 条基础集 |
| 10 | 姿态识别诊断 | ⏳ | PoseDetector/CameraManager | 旋转/镜像/坐标 |
| 11 | AI Provider + 安全 | ⏳ | coach/* + 设置页 | 统一抽象+Key 配置(不进 Git) |
| 12 | 统一数据模型 | ⏳ | db/ + model/ | 项目模型 + 体测/训练闭环 |
| 13 | 设计系统/图标/性能 | ⏳ | theme/ + mipmap | 青绿深蓝白 |
| 14 | 测试/构建/APK/真机/文档 | ⏳ | V3_REPORT + 各测试 | 诚实分级 |

## 2. 恢复说明（新会话）
1. `git checkout v3-dev`；读本文件 + `V3_IMPLEMENTATION_REPORT.md`。
2. 环境：`$env:JAVA_HOME="C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"`（或现有）；`$env:ANDROID_HOME="C:\Users\23163\.android-build\android-sdk"`。
3. 构建：`gradle.bat :app:assembleDebug --no-daemon`；产物 `app\build\outputs\apk\debug\app-debug.apk`。
4. 单测：`bash run_jvm_tests_v2.sh`。
5. 从 §1 表第一个 ⏳ 继续。

## 3. 风险 / 阻塞
- 视频真机解码画面：MIUI 浮层拦截编程点击，需人工手点确认（V2 已记录）。
- MoveNet 换模型：无真机实测条件，仅做评估记录，**默认保留 MediaPipe**（诚实，不盲换）。
- 姿态识别"识别人"验收：需真机多样本；无则明确记录未验证。
- API Key：开发期走本地未提交配置 + .gitignore；**不声称生产级密钥保护**。
