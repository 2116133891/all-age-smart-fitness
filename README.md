# 全龄智动 — 数字人智能运动指导平台

> 中国研究生智慧城市技术与创意设计大赛 · 项目原型 MVP
> 基于 **数字人教学 + 摄像头实时人体姿态识别 + 动作评分 + 纠错 + 全年龄运动指导** 的 Android 应用。

## 一、项目目录结构

```
城市大赛项目/
├── app/
│   ├── build.gradle              # 模块构建配置（Kotlin / Compose / CameraX / MediaPipe）
│   ├── proguard-rules.pro        # 混淆规则
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # 摄像头权限 + MainActivity
│       │   ├── assets/
│       │   │   └── pose_landmarker.task   # MediaPipe 全身姿态模型（本地推理，无后端）
│       │   ├── java/com/quannian/zhidong/
│       │   │   ├── MainActivity.kt        # Compose 入口
│       │   │   ├── model/                 # 数据模型（Landmark / PoseFrame / 错误类型 / 报告）
│       │   │   ├── pose/PoseDetector.kt   # MediaPipe 姿态检测封装
│       │   │   ├── camera/CameraManager.kt# CameraX 出图 + 关键点回调
│       │   │   ├── analyzer/              # 动作分析器（深蹲 / 抬臂 / 跳绳框架）
│       │   │   ├── score/               # ScoreCalculator + CorrectionEngine
│       │   │   ├── repository/            # 运动内容仓库 + 报告通道
│       │   │   └── ui/
│       │   │       ├── theme/             # 设计系统（配色 / 浅色科技风）
│       │   │       ├── components/        # 数字人头像 / 卡片 / 骨骼覆盖层
│       │   │       └── screens/         # 7 个页面 + 导航 + 跟练 ViewModel
│       │   └── res/                        # 图标 / 字符串 / 主题
│       └── test/java/.../ScoreEngineTest.kt # 评分/纠错逻辑单测（7 项，全部通过）
├── build.gradle / settings.gradle / gradle.properties
└── local.properties                 # sdk.dir（本机 Android SDK 路径）
```

## 二、核心技术栈

| 能力 | 选型 | 说明 |
|------|------|------|
| 语言 | **Kotlin 1.9.10** | 100% Kotlin |
| UI | **Jetpack Compose** (BOM 2023.10.01) | Material 3 组件 + Canvas 数字人 |
| 相机 | **CameraX 1.3.0** | `PreviewView` + `ImageAnalysis` 出图 |
| 姿态识别 | **MediaPipe Pose Landmarker 0.10.14** | 本地 33 关键点，纯端侧推理，**无后端/无网络** |
| 架构 | MVVM + 单向数据流 | 页面用 `StateFlow` 收集实时状态 |
| 导航 | Navigation-Compose | 7 页闭环 |

## 三、已实现功能（P0 全量）

- **启动页**：品牌 + 数字人 + 加载动效
- **首页**：标题「全龄智动」+ 副标题 + 「选择您的运动陪练」+ 三个年龄段入口（儿童/青年/银龄，各专属配色数字人）
- **年龄段页**：数字人教练 + 该年龄运动项目卡片（儿童 3 项 / 青年 3 项 / 银龄 3 项）
- **数字人教学页**：动作名称 / 步骤 / 注意事项 / 教练开场白 / 「开始教学」「开始跟练」按钮
- **跟练页（核心 Demo）**：
  - 申请摄像头权限 → 实时摄像头画面（`PreviewView`）
  - **MediaPipe 实时人体姿态识别** → **骨骼关键点 + 连线实时绘制**（Canvas 覆盖层）
  - 实时计算关节角度 / 动作阶段 / 动作得分（0-100）/ 动作状态
  - **≥3 类纠错建议**（幅度不足 / 躯干前倾 / 重心不稳 / 抬臂幅度）
  - 完成次数统计
  - 「完成并查看训练报告」
- **训练报告页**：训练项目 / 完成次数 / 动作规范度 / 综合评分环形图 / 表现良好 / 建议改进 / 「再次训练」「返回首页」
- **评分系统**：`ScoreCalculator` 四维权重（幅度 40% / 姿态 30% / 稳定 20% / 完整度 10%），独立模块，可扩展
- **纠错系统**：`CorrectionEngine` 规则引擎（姿态信号 → 错误类型 → 自然语言建议），未来可接大模型
- **深蹲动作真正做通**：膝关节 / 髋关节 / 躯干倾角 → 下蹲 / 最低点 / 起身状态机 → 完成次数判定
- **跳绳**：起跳状态检测框架（`JumpRopeAnalyzer`），**诚实标注**为起跳近似，留 `升级路径` 注释（手腕轨迹 / 绳体检测）
- **抬臂 / 八段锦托天**：`ArmReachAnalyzer` 通用抬臂分析器

## 四、当前真正可以运行的功能

1. **Gradle 编译 ✅**、**APK 安装 ✅**（含 MediaPipe 原生库 4 ABI + 姿态模型）
2. 首页 → 年龄段 → 运动 → 教学 → 跟练 → 报告 **全链路可点击**
3. 跟练页：**开摄像头 + 实时骨骼 + 实时评分 + 纠错 + 计数**（需真机，模拟器无摄像头）
4. 7 项 JVM 单测全部通过（评分 / 纠错逻辑）

> 说明：跟练页的相机 / 姿态闭环必须**在真机上验证**（模拟器无摄像头）。桌面端已验证编译、打包、单测。

## 五、尚未实现 / 下一阶段建议（P1 / P2）

- 跳绳真实计数（当前为起跳近似，已留扩展接口）
- 八段锦 / 太极 多式动作的逐式识别
- 数字人升级：从 Canvas 抽象形象 → 真实 3D / Lottie 人物 + 口型
- 历史数据统计 / 个人档案 / 成长曲线
- 接入大模型做自然语言纠错生成（当前为预设文案）
- 账号 / 云同步（当前全本地，符合 MVP 要求）

## 六、如何运行

### 1. 环境（本机已就绪）
- **JDK 17**：`C:\Users\Admin\.android-build\jdk-17.0.10+7`
- **Android SDK**：`C:\Users\Admin\.android-build\android-sdk`（platform 34 + build-tools）
- 已写入 `local.properties` 指向该 SDK

### 2. 编译 & 安装
```bash
cd 城市大赛项目
# Windows
set JAVA_HOME=C:\Users\Admin\.android-build\jdk-17.0.10+7
set ANDROID_HOME=C:\Users\Admin\.android-build\android-sdk
gradlew.bat :app:assembleDebug
# 安装到已连接的真机
C:\Users\Admin\.android-build\android-sdk\platform-tools\adb install -r app\build\outputs\apk\debug\app-debug.apk
```
> 若用 Android Studio：打开项目，SDK 路径已自动由 `local.properties` 指定。

### 3. 连接 Android 手机
1. 手机开启「开发者选项 → USB 调试」
2. USB 连接电脑，`adb devices` 确认设备在线
3. 安装 APK → 首次运行授予**摄像头权限** → 进入任一运动 → 「开始跟练」
4. 站到摄像头前（全身入镜），即可看到实时骨骼、评分与纠错

## 七、重要诚实声明

- 姿态识别使用 **MediaPipe 官方开源模型**，**真实视觉检测**，非假算法。
- 评分标注为「**动作规范度评分**」，属运动教学辅助，**非医学/专业运动医学评估**。
- 跳绳计数当前为起跳状态近似，已留清晰升级接口，**未冒充完整跳绳识别**。
