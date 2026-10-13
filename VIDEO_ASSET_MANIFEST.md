# VIDEO_ASSET_MANIFEST — 全龄智动 V3.1 教学视频资源清单

> 分支 `v3-dev`。生成日期 2026-10-12。
> 数据来源：`app/src/main/java/com/quannian/zhidong/repository/TeachingVideoRepository.kt`
> （`allItems()` 18 条；`allVideoItems()` 可播 **16** 条；方法说明型 **2** 条）。
> 诚实声明：所有视频由 Agnes AIGC `text_to_video`（agnes-video-2.5-flash，720P H.264 MP4）
> 本地生成，非真实摄影师拍摄；仅作动作示范教学辅助，**不冒充**官方场地测试。
> V3.1 轮已生成 9 条 pending 视频并落地 `assets/videos/`，可播数 7 → 16。

## 1. 总览

| 指标 | 数值 |
|------|------|
| 条目总数（`allItems`） | **18** |
| 可播（assetPath 非空 + mp4 已落地） | **16** |
| 方法说明型（BMI / 肺活量，设计不生成动作视频） | **2** |
| 分组 | youth **10** / middle **4** / senior **4** |
| 分段可播 | youth **8**（10 条中 2 条方法说明）/ middle **4** / senior **4** |

## 2. 逐条清单

图例：✅ = mp4 已落地可播；⏳ = pending（AIGC 生成后填入 `assets/videos/`）；📝 = 方法说明型（无动作视频，按设计不生成 mp4）。

| # | id | 年龄段 | 标题 | 状态 | assetPath |
|---|----|--------|------|------|-----------|
| 1 | long_jump | youth | 立定跳远 · 标准动作示范 | ✅ | videos/video_youth_long_jump.mp4 |
| 2 | pullup | youth | 引体向上（男）· 标准动作示范 | ✅ | videos/video_youth_pullup.mp4 |
| 3 | situp | youth | 1 分钟仰卧起坐（女）· 标准动作示范 | ✅ | videos/video_youth_situp.mp4 |
| 4 | run_50m | youth | 50 米跑 · 起跑 / 加速 / 冲刺要点 | ✅ | videos/video_youth_50m.mp4 |
| 5 | forward_reach | youth | 坐位体前屈 · 动作要点与常见错误 | ✅ | videos/video_youth_forward_reach.mp4 |
| 6 | endurance | youth | 800/1000 米 · 配速与分段训练方法 | ✅ | videos/video_youth_endurance.mp4 |
| 7 | warmup | youth | 体测前热身 & 运动后整理 | ✅ | videos/video_youth_warmup.mp4 |
| 8 | vital_capacity_method | youth | 肺活量 · 正确测量方法（无动作视频） | 📝 | （空，设计如此） |
| 9 | bmi_method | youth | BMI · 测量方法与计算（无动作视频） | 📝 | （空，设计如此） |
| 10 | youth_plank | youth | 平板支撑 · 核心稳定示范（青年） | ✅ | videos/video_youth_plank.mp4 |
| 11 | middle_core | middle | 中年核心强化 · 下班后 15 分钟 | ✅ | videos/video_middle_core.mp4 |
| 12 | middle_flexibility | middle | 中年柔韧性恢复 · 坐姿拉伸 | ✅ | videos/video_middle_flexibility.mp4 |
| 13 | middle_cardio | middle | 中年有氧减脂 · 间歇走跑 | ✅ | videos/video_middle_cardio.mp4 |
| 14 | middle_neck_shoulder | middle | 肩颈放松 · 工位 5 分钟版（中年） | ✅ | videos/video_middle_neck_shoulder.mp4 |
| 15 | senior_baduanjin | senior | 八段锦 · 第一至三式示范（银龄） | ✅ | videos/video_senior_baduanjin.mp4 |
| 16 | senior_taichi | senior | 太极 · 起势与云手示范（银龄） | ✅ | videos/video_senior_taichi.mp4 |
| 17 | senior_balance | senior | 平衡防跌 · 串联走与单腿站（银龄） | ✅ | videos/video_senior_balance.mp4 |
| 18 | senior_gentle_stretch | senior | 银龄舒缓拉伸 · 4 分钟全身活动 | ✅ | videos/video_senior_gentle_stretch.mp4 |

## 3. 已落地 mp4 校验（`assets/videos/`，2026-10-12）

V3.1 共 **16** 个 mp4 已落地（原 7 + 新生成 9），与 `allVideoItems()` 返回的 16 条一一对应。

| 文件 | 大小 | 来源 |
|------|------|------|
| video_youth_50m.mp4 | 954,833 B | 原有 |
| video_youth_endurance.mp4 | 1,617,746 B | 原有 |
| video_youth_forward_reach.mp4 | 607,885 B | 原有 |
| video_youth_long_jump.mp4 | 515,868 B | 原有 |
| video_youth_pullup.mp4 | 651,306 B | 原有 |
| video_youth_situp.mp4 | 632,193 B | 原有 |
| video_youth_warmup.mp4 | 518,737 B | 原有 |
| video_youth_plank.mp4 | 505,074 B | V3.1 AIGC |
| video_middle_core.mp4 | 897,131 B | V3.1 AIGC |
| video_middle_flexibility.mp4 | 1,076,745 B | V3.1 AIGC |
| video_middle_cardio.mp4 | 2,864,844 B | V3.1 AIGC |
| video_middle_neck_shoulder.mp4 | 662,569 B | V3.1 AIGC |
| video_senior_baduanjin.mp4 | 1,247,004 B | V3.1 AIGC |
| video_senior_taichi.mp4 | 826,283 B | V3.1 AIGC |
| video_senior_balance.mp4 | 1,001,530 B | V3.1 AIGC |
| video_senior_gentle_stretch.mp4 | 691,246 B | V3.1 AIGC |

16 个文件全部存在且通过 `ftyp` 容器校验；均已打包进最新 APK。

## 4. 缺失资源处理（任务 12）

- `TeachingVideoRepository.assetExists(context, assetPath)` 通过 `assets.open()` 判断；
  文件缺失返回 `false`，UI 显示明确提示（不用假封面 / 不静默失败）。
- `allVideoItems()` 仅返回 assetPath 非空条目；pending 条目（assetPath=""）**不会**
  进入播放入口，避免"点了没得播"的空态崩溃。
- JVM 单测 `PoseAndVideoV3Test` 已固化以上约束（总数 18 / 分组 10-4-4 / 可播 16 /
  方法说明型 2 / id 唯一 / assetPath 规范前缀），任何回归都会红。

## 5. 视频生成状态（V3.1 已完成）

原 11 条 pending 中，**9 条可生成运动示范已全部落地**（youth_plank + middle 4 + senior 4）；
**2 条方法说明型（#8 肺活量 / #9 BMI）按设计不生成动作视频**，保留图文 + 人工录入流程。

已完成的补齐流程（供追溯 / 再生成时复用）：

1. 用 Agnes AIGC `text_to_video` 逐条生成，风格与既有 7 条一致：
   **卡通 3D 教练示范，720P H.264 MP4，单条 4–8s，聚焦动作要点**。
   每条正文提示词直接用该条 `title` + `summary` + `keyPoints` 拼成运动描述。
2. 生成后重命名并落地到 `app/src/main/assets/videos/`，命名规则：
   `video_{ageId}_{id 去前缀}.mp4`。例：
   - `youth_plank` → `video_youth_plank.mp4`
   - `middle_core` → `video_middle_core.mp4`
   - `senior_baduanjin` → `video_senior_baduanjin.mp4`
3. 回填 `TeachingVideoRepository` 对应条目的 `assetPath`（去掉 `""`），
   并更新本清单把 ⏳ 改为 ✅、更新 §3 落地表。
4. 重跑 `PoseAndVideoV3Test`：可播数会从 7 上升到 7 + 新落地数；
   同步修改该测试里 `only real videos are surfaced for playback` 的期望值（= 已落地 mp4 数）。
5. 重新 `:app:assembleDebug`，确认 APK 体积随 mp4 增大后仍 < 100MB（单条 ~0.5MB 起）。

> 注意：V3.1 已补齐 middle / senior 各 4 条可播 mp4，推荐/列表 UI（`HomeScreen.RecommendedContentCard`
> 与 `TrainingHubScreen`）现在三段人群均有可播内容，不再出现"当前还没有可播放的教学视频"空态
> （若某段人为删光 mp4，仍会走设计内空态兜底，不崩）。

## 6. 与 UI 的消费关系

- `HomeScreen.RecommendedContentCard`：`allVideoItems().take(2)`，按当前人群展示，空则空态。
- `TrainingHubScreen`：`allVideoItems()` 全量列"教学视频"。
- `YouthBodyTestDetailScreen`：`videoFor(itemKey, gender)` 单项视频 + 要点。
- `TeachingVideoPlayer` / `TeachingVideoScreen`：ExoPlayer 经 `AssetFileDataSource` 播本地 mp4。
- 方法说明型（BMI / 肺活量）：`assetPath` 为空，UI 只展示"方法说明"文案，不进播放器
  （任务 4：不生成假装摄像头测量的动作视频）。
