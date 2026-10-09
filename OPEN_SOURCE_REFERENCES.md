# 全龄智动 · 开源参考与复用说明（OPEN SOURCE REFERENCES）

> 原则（spec §二十八/§二十九）：**能合法复用成熟实现就复用，不要为了"自己写"而浪费时间。**
> 本文件对每个借鉴来源做许可分类，标注哪些可复用、哪些仅参考。
> 生成时间：2026-10-06 · 项目根：`C:\Users\Admin\Desktop\城市大赛项目`

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
