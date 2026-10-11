# 全龄智动 · 数字人智能运动指导平台

> 中国研究生智慧城市技术与创意设计大赛 · 参赛版（企业级 MVP）
> 基于 **数字人教学 + 摄像头实时人体姿态识别 + 动作评分纠错 + 全龄运动指导** 的 Android 应用。
> 一句话：让不同年龄段的人（青年 / 中年 / 银龄），在镜头前练得标准、练得安全、看得见进步。

---

## 一、这是什么

一个**全离线、无后端、可上架**的智能运动指导 App：

- **会动的数字教练**：按动作逐帧演示（深蹲 / 肩颈 / 瑜伽 / 八段锦…），不再只是站着的 PNG。
- **摄像头实时跟练**：MediaPipe 本地姿态识别 → 关节角度 → 动作阶段 → 计分纠错 → 训练报告。
- **AI 动作诊断**：上传视频复盘 → 定位主要错误 → 数字教练针对性纠正 → 前后对比。
- **全龄三人群**：青年（科学训练 / 体态）、中年（下班健身 / 肩颈减压）、银龄（康养 / 防跌康复）。
- **成长体系**：连续训练天数、成就徽章、7 天成长曲线。
- **AI 小助手**：离线规则问答，结合你的画像与最近训练给建议。
- **运动手环**（演示连接）：心率 / 步数 / 睡眠 联训（Phase 2 接真实 BLE）。

## 二、核心能力（已实现）

| 能力 | 说明 | 状态 |
|---|---|---|
| 数字人动作演示 | 2D 关节人形，逐动作不同姿态 + 600ms 平滑插值 | ✅ 已实现 |
| 摄像头实时跟练 | CameraX 出图 + MediaPipe 33 关键点 + 骨骼叠加 | ✅ 已实现 |
| 动作阶段/计数/评分 | 各动作专属分析器 + 四维加权（幅度/躯干/对称/完整） | ✅ 已实现 |
| 实时纠错 | 8 类错误 → 自然语言纠正建议（引用实时数据） | ✅ 已实现 |
| AI 视频诊断 | 抽帧 → 姿态 → 主错误定位 → 纠正 → 前后对比 | ✅ 已实现 |
| 训练记录 / 成长曲线 | Room 本地持久化 + Canvas 折线 | ✅ 已实现 |
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
- **持久化**：Room（本地，无云）
- **架构**：MVVM + StateFlow
- **导航**：Navigation Compose（底部 4 tab + 子页）
- **minSdk 24 / targetSdk 34**

## 五、离线与隐私（上架关键）

- 所有姿态推理、评分、纠错、AI 问答**均在设备本地**完成，无网络请求、无账号体系。
- 训练数据仅存本机 Room；摄像头画面不上传。
- 提供"清空训练记录"与隐私政策。

## 六、构建

```powershell
# 环境：JDK 17 + Android SDK + Gradle 8.3
$env:JAVA_HOME = "...\jdk-17.0.20.1+1"
gradle :app:assembleDebug --no-daemon
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

> 中文路径下 Gradle wrapper 下载 10s 易超时，本项目用**预下载 Gradle 8.3 分发包**直接 `gradle.bat` 构建绕开。

## 七、Phase 路线

- **Phase 1（本版）**：全龄三人群数字人 + 跟练闭环 + 视频诊断 + 成长体系 + 离线助手。
- **Phase 2**：真实 BLE 手环接入、分阶段动作帧序列素材、真 3D（Filament）数字人、更多动作。
