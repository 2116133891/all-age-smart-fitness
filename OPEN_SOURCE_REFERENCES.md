# 全龄智动 · 开源参考与复用说明（OPEN SOURCE REFERENCES）

> 原则（spec §二十八/§二十九）：**能合法复用成熟实现就复用，不要为了"自己写"而浪费时间。**
> 本文件对每个借鉴来源做许可分类，标注哪些可复用、哪些仅参考。
> 生成时间：2026-10-06 · 项目根：`C:\Users\Admin\Desktop\城市大赛项目`

> **V2 更新（2026-10-11）**：本轮任务 10 研究的 4 个目标仓库 + 新增依赖（Media3）+ 视频/数字人素材生成工具，许可分类与处置见文末「V2 开源研究记录」。

---

## 分类图例

| 分类 | 含义 |
|------|------|
| **SAFE_TO_REUSE** | 许可证允许（MIT / Apache-2.0 / BSD 等），可按许可证要求复制相关代码并保留版权/许可声明。 |
| **REFERENCE_ONLY** | 许可证不明确 / 仅参考思路，**不复制源码**，在本工程内自行实现。 |
| **DO_NOT_COPY** | 专有 / All Rights Reserved，禁止复制。 |

---

## 1. 本次实际借鉴（已落地到代码）

### 1.1 One Euro Filter — **REFERENCE_ONLY**（自行实现，未复制源码）
- **来源**：Casiez & Roullier, *"1€ Filter: A Simple Efficient Noise Filter for Real-Time Tracked Signals"*（UIST 2012）；开源实现如 `nolano/OneEuroFilter`（MIT）。
- **借鉴点**：低通 + 自适应截止（`minCutoff` + `beta`）的联合滤波，抑制 MediaPipe 关键点视频抖动，静止时平滑、运动时跟手。
- **落地**：本工程**自行实现**于 `app/src/main/java/com/quannian/zhidong/video/OneEuroFilter.kt`（纯 JVM，可单测）。
- **许可结论**：算法思想本身不受版权保护；未复制任何具体源码 → **无许可义务**，无需保留原作者代码版权头。标注来源仅为可追溯性。
- **单测**：`OneEuroFilterTest`（4 用例：恒定收敛 / 抖动抑制 / reset / 双通道独立）。

### 1.2 视频抽帧（MediaMetadataRetriever）— **Android 原生 API**
- **参考**：MediaPipe 官方 Android `Pose Landmarker` Gallery 的 VIDEO running mode + 视频帧处理；IO_motion 按 15fps 抽帧思路。
- **落地**：`app/src/main/java/com/quannian/zhidong/video/VideoFrameReader.kt`（`MediaMetadataRetriever.getFrameAtTime` ~12fps 抽帧，限 60s，本地处理）。
- **许可**：Android 框架 API（Apache-2.0），无额外许可义务。

### 1.3 复用工程内既有成熟模块（非外部，自研）
- `ExerciseAnalyzer`（9 个）/ `ScoreCalculator` / `CorrectionEngine` / `AgeProfile` — 视频分析直接复用（spec §二十六"只在真正需要的地方加"）。
- `OneEuroFilter` 平滑 + 置信度门（各分析器 `visOk`/`MIN_VIS` 已具备）→ 不重造。

---

## 2. 候选参考（研究记录，本轮未整项目引入）

> 均为 **REFERENCE_ONLY**：思路参考，若后续需要可评估移植其成熟小模块（保留各自 LICENSE 要求），**不整项目搬进**（保持全龄智动自有架构，spec §二十八）。

| 项目 | 借鉴点 | 许可 | 本轮处置 |
|------|--------|------|----------|
| `bardia1122/IO_motion` | 视频输入 / 统一 PoseFrame / per-rep quality / FSM / One-Euro / Room | 需查 LICENSE（无明确声明则 REFERENCE_ONLY） | 思路参考：抽帧+统一帧管线+per-rep 质量已参考；**未复制源码** |
| `giaongo/RepDetect` | Camera Flip / Exercise Plan / Room / Voice / per-rep 评分（**Apache-2.0**） | **SAFE_TO_REUSE**（Apache-2.0） | 借鉴 per-rep 评分思路（已在 `VideoAnalysisEngine` 每 rep 用 `ScoreCalculator` 推 0-100）；未复制源码，如需复制须保留 Apache-2.0 声明 |
| `vmalikov/pose-detection-android` | CameraX + Compose / skeleton crop mapping / squat FSM / HUD | 需查 LICENSE | 参考 CameraX 骨架映射思路（本工程 `CameraManager.visibleCrop` 已实现） |
| `google-ai-edge/mediapipe-samples` | Pose Landmarker / VIDEO mode / 视频帧处理 | Apache-2.0 | 参考官方用法（本工程 `PoseDetector` 已按 VIDEO 模式 + 单调时间戳实现） |
| `airbnb/lottie-android` | 动画运行时（**Apache-2.0**） | SAFE_TO_REUSE | **本轮未引入**；数字人用"分层 2D 骨骼 + 帧序列"（最稳、Android 9 兼容），保留为后续备选 |
| `rive-app/rive-android` | 状态机动画（MIT runtime） | 需查 | **本轮未引入**，避免 native/包体/Android 9 兼容风险 |

---

## 3. 许可合规声明

- 本次**仅**自行实现算法（OneEuro），**未复制**任何外部项目的专有源码 → 无许可风险。
- 若后续引入 Apache-2.0 / MIT 代码（如 RepDetect 片段），须在该文件头部保留对应版权与许可声明（Apache-2.0 还须保留 `LICENSE` 与 `NOTICE`）。
- 绝不复制 proprietary / all-rights-reserved 代码（spec §九/§二十九）。

---

## 4. 隐私声明（spec §三十三）

- 视频动作分析**全程本机**（MediaPipe 本地推理 + `MediaMetadataRetriever` 读本地视频），**不上传任何用户视频到服务器**。
- UI 明示「视频仅在本机分析」。
- 评分标注为「动作规范度」（运动教学辅助），**非医学/伤病诊断**（spec §三十四）。

---

## 5. V2 开源研究记录（任务 10，2026-10-11）

### 5.1 用户指定研究的 4 个仓库

| 仓库 | 目标功能 | 许可证（核查结果） | 架构 | 兼容性 | 处置 |
|------|---------|------------------|------|--------|------|
| `atikulmunna/SmartForm` | 运动姿态识别 + 动作计数 | 未能在线确认（仓库可达但 本次 网络 超时，无法 抓取 LICENSE） | 多 平台 姿态 模型 | 未 整 项目 引入 | **REFERENCE_ONLY**：仅 参考 姿态 计数 思路；**未 复制 源码**，无 许可 风险。若 后续 复制 须 先 确认 其 LICENSE。 |
| `Gakiwoo/ai-sport-mobile` | MediaPipe 实时 运动 计数 移动端 | 未能在线 确认（同上 超时） | 移动 运动 追踪 | 未 整 项目 引入 | **REFERENCE_ONLY**：参考 实时 计数 架构；**未 复制 源码**。 |
| `shahmir2004/exercise-form-correction` | 动作 纠错（姿态 + 分类） | 未能在线 确认（同上 超时） | 姿态 纠错 模型 | 未 整 项目 引入 | **REFERENCE_ONLY**：参考 纠错 分类 思路；**未 复制 源码**。 |
| `google-ai-edge/mediapipe-samples`（pose_landmarker android） | Pose Landmarker 官方 示例 | **Apache-2.0**（已 在线 确认） | 官方 示例 | 兼容（本项目 已 用 0.10.14 tasks-vision） | **SAFE_TO_REUSE**：官方 用法 参考（本项目 `PoseDetector` 已 按 VIDEO 模式 + 单调 时间戳 实现，无需 复制 示例 代码）。 |

> 诚实 声明：3 个 非 官方 仓库 的 LICENSE 因 本次 网络 超时 **未 能 在线 确认**，一律 标 **REFERENCE_ONLY** 且 **未 复制 任何 源码**（仅 思路 参考），故 **无 许可 合规 风险**。MediaPipe 官方 示例 为 Apache-2.0，可 放心 参考。

### 5.2 V2 新增 依赖 许可

| 依赖 | 版本 | 许可 | 用途 |
|------|------|------|------|
| `androidx.media3:media3-exoplayer/ui/common/datasource` | 1.4.1 | **Apache-2.0**（AndroidX） | 教学 视频 播放（本地 assets 视频） |
| `com.google.mediapipe:tasks-vision` | 0.10.14 | **Apache-2.0** | 姿态 识别（既有） |
| `androidx.camera:*` | 1.3.0 | Apache-2.0 | 摄像头（既有） |
| `androidx.room:*` | 2.5.2 | Apache-2.0 | 持久化（既有，V2 升 v2 加 表） |

### 5.3 V2 视频 / 数字人 素材 生成 工具（非 开源 库，记录 来源 可 追溯）

- **数字 教练 PNG**：由 Agnes AIGC 图 生 图（image-to-image）从 用户 提供 的 三 合一 参考 图 分离 生成 3 张 独立 全身 人物（青/中/银 龄），落地 `res/drawable/coach_{youth,middle,senior}.png`。**非 真实 3D 模型**，仅作 UI 视觉 形象（诚实 标注）。
- **教学 视频**：由 Agnes AIGC 文 生 视频（text-to-video，`agnes-video-2.5`）生成 7 条 720P H.264 MP4，落地 `assets/videos/video_youth_*.mp4`。**AI 生成 动作 示范**，非 摄影师 实拍，**不 冒充** 官方 场地 测试。
- 所有 素材 **均 本地 打包** 进 APK，**不 依赖 临时 外部 链接**（满足 任务 2 约束）。

### 5.4 许可 合规 总 声明（V2）

- V2 **未 复制** 任何 外部 专有 源码；3 个 非 官方 仓库 仅 思路 参考（REFERENCE_ONLY）。
- 新增 依赖 全部 为 **Apache-2.0**（AndroidX / MediaPipe / Media3），无 许可 冲突。
- 若 后续 需 复制 SmartForm / ai-sport-mobile / exercise-form-correction 的 具体 代码，**必须 先 在线 确认 其 LICENSE**，并 在 该 文件 头部 保留 版权 + 许可 声明（Apache-2.0 须 保留 LICENSE + NOTICE）。
