# 全龄智动 · 全龄 智能 运动 指导 平台（参赛 版 V2）

> 中国研究生 智慧城市 技术 与 创意 设计 大赛 · 参赛 版（企业级 MVP）
> 基于 **数字人 教学 + 真实 教学 视频 + 摄像头 实时 人体 姿态 识别 + 动作 评分 纠错 + 青年 体测 备考 闭环** 的 Android 应用。
> 定位：面向 **青年 体质 测试、中年 健康 管理、银龄 科学 运动** 的 全龄 智能 运动 指导 平台。
> 一句话：让 不同 年龄 段 的 人（青年 / 中年 / 银龄），在 镜头 前 练 得 标准、测 得 真实、看得 见 进步。

> **V2（2026-10-11）**：新增 青年 体测 完整 模块（七项：BMI/肺活量/50m/坐位 体前屈/立定 跳远/引体·仰卧/1000-800m）+ 7 条 真实 教学 视频（Media3 播放）+ 体测 标准 评分 + 备考 训练 计划 + 阶段 报告 + 中年/银龄 健康 管理。详见 `V2_IMPLEMENTATION_REPORT.md`。

---

## 一、这是什么

一个**全离线、无后端、可上架**的智能运动指导 App：

- **全离线、无后端、可上架**的智能运动指导 App：
- **会动的数字教练**：按动作逐帧演示（深蹲 / 肩颈 / 瑜伽 / 八段锦…），不再只是站着的 PNG。
- **摄像头实时跟练**：MediaPipe 本地姿态识别 → 关节角度 → 动作阶段 → 计分纠错 → 训练报告。
- **AI 动作诊断**：上传视频复盘 → 定位主要错误 → 数字教练针对性纠正 → 前后对比。
- **全龄三人群**：青年（体测备考 / 专项训练 / AI 动作指导）、中年（体重管理 / 基础体能 / 肩颈减压）、银龄（太极 / 八段锦 / 康养防跌）。
- **青年体测闭环**：七项体测 + 真实教学视频 + 标准评分 + 备考训练计划 + 阶段报告。
- **成长体系**：连续训练天数、成就徽章、7 天成长曲线。
- **AI 小助手**：离线规则问答，结合你的画像与最近训练给建议。
- **运动手环**（演示连接）：心率 / 步数 / 睡眠 联训（Phase 2 接真实 BLE）。

## 二、核心能力（已实现）

| 能力 | 说明 | 状态 |
|---|---|---|
| 数字人动作演示 | 三类数字教练（青/中/银龄）独立全身 PNG，静止站立 + 状态微反馈 | ✅ 已实现 |
| 摄像头实时跟练 | CameraX 出图 + MediaPipe 33 关键点 + 骨骼叠加 | ✅ 已实现 |
| 动作阶段/计数/评分 | 各动作专属分析器 + 四维加权（幅度/躯干/对称/完整） | ✅ 已实现 |
| 实时纠错 | 8 类错误 → 自然语言纠正建议（引用实时数据） | ✅ 已实现 |
| AI 视频诊断 | 抽帧 → 姿态 → 主错误定位 → 纠正 → 前后对比 | ✅ 已实现 |
| **青年体测完整模块** | **七项：BMI/肺活量/50m/坐位体前屈/立定跳远/引体·仰卧/1000-800m，教学视频 + 真实成绩录入 + 标准评分 + 趋势** | ✅ 已实现 |
| **真实教学视频** | **7 条 720P H.264 MP4（Agnes 生成，本地打包），Media3 ExoPlayer 播放（播放/暂停/进度/全屏/重播）** | ✅ 已实现 |
| **备考训练计划** | **薄弱项识别 → 周期/天数 → 每日任务（视频+步骤+安全边界）→ 完成/跳过 → 阶段报告** | ✅ 已实现 |
| **体测标准评分** | **《国家学生体质健康标准（2014 修订）》来源/版本标注，保守阈值，不适用不输出分数** | ✅ 已实现 |
| 中年/银龄健康管理 | 体重/BMI 历史 + 低冲击有氧 + 大字体安全提醒 + 教学与医疗区分 | ✅ 已实现 |
| 训练记录 / 成长曲线 | Room 本地持久化（v2 加体测表+计划表，保留旧记录）+ Canvas 折线 | ✅ 已实现 |
| 连续天数 / 成就徽章 | 按真实数据解锁 | ✅ 已实现 |
| AI 小助手 | 离线规则引擎（无 Key、无网络） | ✅ 已实现 |
| 运动手环 | 演示连接 + 数据维度（真实 BLE 为 Phase 2） | ⚠ 演示 |
| 隐私 / 设置 | 全本地、可清空、隐私政策 | ✅ 已实现 |

## 三、架构

```
城市大赛项目/
├── app/
│   ├── build.gradle                # Kotlin / Compose / CameraX / MediaPipe / Room
│   └── src/main/
│       ├── AndroidManifest.xml     # 摄像头权限 + 入口
│       ├── assets/pose_landmarker.task   # MediaPipe 姿态模型（本地推理，无后端）
│       ├── java/com/quannian/zhidong/
│       │   ├── MainActivity.kt
│       │   ├── model/              # Landmark / PoseFrame / ErrorType / TrainingReport / Profile
│       │   ├── pose/PoseDetector.kt       # MediaPipe 封装（readBytes 完整读模型，防闪退）
│       │   ├── camera/CameraManager.kt    # CameraX 出图 + 关键点回调
│       │   ├── analyzer/           # 各动作专属分析器（squat/shoulder/yoga/baduanjin/taichi/easy…）
│       │   ├── score/             # ScoreCalculator + CorrectionEngine + AgeProfile（年龄权重）
│       │   ├── repository/        # 运动内容仓库 + 报告通道
│       │   ├── assistant/LocalAssistant.kt  # 离线 AI 问答
│       │   ├── db/                # Room：训练记录 + 成长统计 + clearAll
│       │   ├── video/             # 视频诊断引擎（抽帧/滤波/结果）
│       │   ├── tts/TtsCoach.kt    # 本地语音教练（默认关）
│       │   └── ui/
│       │       ├── theme/         # 设计系统（浅色科技风 + 四年龄段配色 + 卡片阴影）
│       │       ├── components/    # 数字人（CoachMotionView 关节人形）/ 卡片 / 骨骼覆盖层
│       │       └── screens/       # 15+ 页面：引导/画像/首页/功能/助手/我的/设置/体测/手环/跟练/报告/视频诊断…
│       └── res/                   # 矢量自适应图标 / 主题
└── PROJECT_STATUS.md              # 逐轮验证诚实分级
```

## 四、技术栈

- **语言 / UI**：Kotlin + Jetpack Compose（Material3）
- **姿态识别**：MediaPipe Pose Landmarker（本地，33 关键点）
- **相机**：CameraX（Preview + ImageAnalysis）
- **教学视频**：Media3 ExoPlayer（本地 assets 视频，无网络依赖）
- **持久化**：Room v2（体测成绩 + 训练计划 + 训练记录，本地，无云）
- **架构**：MVVM + StateFlow
- **导航**：Navigation Compose（底部 4 tab + 子页）
- **minSdk 24 / targetSdk 34**

## 五、离线与隐私（上架关键）

- 所有姿态推理、评分、纠错、AI 问答**均在设备本地**完成，无网络请求、无账号体系。
- 教学视频全部**本地打包进 APK**（`assets/videos/`），离线可播放；不依赖外部链接。
- 训练 / 体测数据仅存本机 Room；摄像头画面不上传。
- 提供"清空训练记录"与隐私政策。

## 六、构建

```powershell
# 环境：JDK 17 + Android SDK + Gradle 8.3
$env:JAVA_HOME = "C:\Users\23163\.android-build\jdk17\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "C:\Users\23163\.android-build\android-sdk"
& "C:\Users\23163\.android-build\gradle-8.3-dist\gradle-8.3\bin\gradle.bat" :app:assembleDebug --no-daemon
# 产物：app\build\outputs\apk\debug\app-debug.apk（~81MB，含 7 条教学视频 + 3 数字人 + MediaPipe）

# 纯 JVM 单测（中文路径下 Gradle test 会 ClassNotFoundException，手跑绕过）
bash run_jvm_tests_v2.sh   # 63 项（含 V2LogicTest 19 项）
```

> 中文路径下 Gradle wrapper 下载 10s 易超时，本项目用**预下载 Gradle 8.3 分发包**直接 `gradle.bat` 构建绕开。

## 七、Phase 路线

- **Phase 1（竞赛 MVP）**：全龄三人群数字人 + 跟练闭环 + 视频诊断 + 成长体系 + 离线助手。
- **Phase 2（本版 V2，2026-10-11）**：青年体测七项完整模块 + 真实教学视频 + 标准评分 + 备考训练计划 + 阶段报告 + 中年/银龄健康管理 + Room v2 迁移。
- **Phase 3（后续）**：真实 BLE 手环接入、分阶段动作帧序列素材、真 3D（Filament）数字人、官方标准完整分档复核、引体/仰卧姿态计数（可靠标定后）。
