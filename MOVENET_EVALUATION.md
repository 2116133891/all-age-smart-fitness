# MoveNet 评估 — 全龄智动 V3（姿态识别模型选型）

> 分支 `v3-dev`。日期 2026-10-12。对应 `V3_IMPLEMENTATION_PLAN.md` 任务 19。
> 结论先行：**本阶段保留 MediaPipe Pose Landmarker 为默认姿态后端；MoveNet 仅作
> 评估记录，不盲换。** 理由：缺少真机多样本实测条件，换模型收益未经验证、
> 风险更高（见 §5）。MediaPipe 在"本地推理 + 33 点 + 视频/图像双模式 + 与
> CameraX 已打通"的点上已满足产品需求。

## 1. 候选对象

| 项 | 现有（MediaPipe） | 评估（MoveNet） |
|----|-------------------|------------------|
| 厂商/来源 | Google MediaPipe tasks-vision `0.10.14` | Google TFLite MoveNet（单姿态/多姿态） |
| 运行方式 | 本地推理（`pose_landmarker.task` 5.7MB，Bundle 文件） | 本地推理（`.tflite`，需自行管图） |
| 输出 | 33 点 归一化（含 visibility/Presence） | 33/17/33 关键点，输出为归一化坐标 |
| 集成层 | `PoseLandmarker`（VIDEO/IMAGE 模式，官方 Android 封装） | 需接 TFLite Interpreter + 预处理 + 后处理（自管） |
| 模型大小 | ~5.7MB（Bundle） | MoveNet Lightning ~3.7MB / SinglePose ~6MB（量级相近） |

## 2. 需求对齐（本产品要什么）

产品对姿态后端的硬性要求：
1. **本地推理**（不上云、离线可用）—— 两者都满足。
2. **33 点全身**（含手/脚，供 SkeletonOverlay 画骨架、analyzer 算关节角）—— 两者都满足。
3. **视频模式 + 严格单调时间戳**（跟练逐帧 + 离线视频抽帧）—— MediaPipe 原生 VIDEO 模式；MoveNet 需自己逐帧喂图 + 时序管理。
4. **可见度/置信度**（低置信度点过滤，避免坏点拖坏整帧）—— MediaPipe 自带 `visibility`；MoveNet 输出需自行算置信度或用单姿态版无 presence。
5. **与 CameraX 出帧解耦**（本仓 `CameraManager` 已按 `ImageProxy→Bitmap→MPImage` 打通）—— MediaPipe 已打通；换 MoveNet 要重写这一段。

## 3. 逐项对比

| 维度 | MediaPipe（现有） | MoveNet（候选） | 备注 |
|------|-------------------|-----------------|------|
| Android 封装成熟度 | 高（官方 tasks API，`createFromOptions`） | 中（TFLite 原始 Interpreter，需自写预处理/矩阵） | 换 MoveNet = 重写 `PoseDetector` |
| 视频时序 | 内置 VIDEO 模式 + 时间戳单调约束 | 无内置，逐帧调用 | 跟练/离线抽帧要自己保证单调 |
| 关键点可见度 | 33 点带 visibility | 单姿态版通常无 presence/visibility | 过滤坏点能力弱 |
| 模型体积 | 5.7MB | 3.7MB（Lightning）~ 6MB | 差异小，不足以成为换模型理由 |
| 帧率/延迟（量级） | 中等，720p 缩放后可跑 | Lightning 更快（专为速度优化） | **仅在真机高帧率需求下才是优势** |
| 稳定性（当前） | 已 `init`/`detect`/native crash 兜底 + 77 单测固化 | 未经本仓验证 | 换 = 引入未验证路径 |
| 维护成本 | 一条官方依赖 | 需长期自维 TFLite 推理栈 | 长期成本更高 |

## 4. MoveNet 的潜在优势（何时才值得换）

- **追求更高跟练帧率**：MoveNet Lightning 面向速度优化，若在真机上 MediaPipe 帧率
  成为瓶颈（掉帧、跟练卡顿），MoveNet 有性能收益。
- **降低单条 APK 模型体积**：Lightning ~3.7MB，比 5.7MB 省 ~2MB——收益有限。
- **多姿态**：MoveNet 有多姿态版；但本产品当前 `numPoses=1`（单人），无此需求。

> 关键：以上都是"假设真机存在瓶颈"下的收益。在**没有真机多样本实测**的前提下，
> 无法证明 MediaPipe 已是瓶颈，因此不构成换模型的理由。

## 5. 结论与决策（诚实，不盲换）

1. **保留 MediaPipe 为默认后端**。它已满足 §2 全部硬性需求，且本仓已打通
   （`PoseDetector` + `CameraManager` 旋转/镜像修复 + `SkeletonOverlay` 经
   `PoseCoordinateMapper` 单测固化），77 单测全绿，APK 正常产出。
2. **MoveNet 记录为"备选/评估通过但暂不切换"**。若后续真机出现明确的
   帧率瓶颈（跟练卡顿、掉帧 > 可接受阈值）或需要多姿态，再立项切换，并：
   - 新建 `MoveNetDetector`（与 `PoseDetector` 同接口），TFLite 自管；
   - 补齐 33 点 + 可见度 + 单调时序；
   - 真机多样本（不同身高/姿态/光照/前后摄像头）回归后才灰度替换，不直接删 MediaPipe。
3. **本阶段不改动任何姿态后端代码**，仅新增 `PoseCoordinateMapper`（坐标映射单一
   可信来源）与 `CameraManager` 旋转修复，降低骨架/真人错位风险——这是任务 10/17
   的正向收益，与模型选型解耦。

## 6. 后续切换 checklist（若真机触发）

- [ ] 真机录 3–5 组视频样本人体动作，MediaPipe 实测帧率/延迟基线
- [ ] 实现 `MoveNetDetector`（同 `detect(bitmap, ts)` 接口），33 点 + visibility
- [ ] 离线抽帧（`VideoFrameReader`）接入 MoveNet，验证单调时间戳
- [ ] 与 `PoseCoordinateMapper` 对齐坐标系（归一化 0..1，源帧 upright）
- [ ] 真机多样本回归（含旋转 90/270、前置镜像）后灰度
- [ ] 更新 `V3_IMPLEMENTATION_PLAN.md` 任务 19 为"已切换"并记录实测数据
