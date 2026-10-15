# V3.5 资源资产清单（V3_5_ASSET_MANIFEST）

> 本清单追踪 V3.5 阶段所有**视觉/视频资源**的状态。每个资源逐条标注真实状态：
> `generated`=本次生成 / `exists`=既有文件 / `alpha+size OK`=透明与尺寸校验 /
> `wired`=已接入真实页面 / `packed`=已打包进 APK / `playback verified`=可播放校验 /
> `content-matched`=内容与动作匹配 / `device-accepted`=真机视觉验收。
> 未达成的状态诚实标注（如 AIGC 503 → `BLOCKED_EXTERNAL`），**绝不**把"文件存在"冒充"内容正确"。

## 1. 竖版透明全身教练立绘（P0-2，V3.5 本次生成）

| 资源 | 路径 | 尺寸 | 状态 |
|---|---|---|---|
| 青少年 | `res/drawable/coach_youth_portrait.png` | 736×1312 | generated / alpha OK（四角透明，透明占比≈70%，边缘羽化）/ 全身居中不裁切 / wired（CoachVisual.coachPortraitResource + OnboardingScreen + SplashScreen）/ 待打包+真机 |
| 中青年 | `res/drawable/coach_middle_portrait.png` | 736×1312 | generated / alpha OK / 全身居中 / wired / 待打包+真机 |
| 中老年 | `res/drawable/coach_senior_portrait.png` | 736×1312 | generated / alpha OK / 全身居中（银发、健朗不佝偻）/ wired / 待打包+真机 |

- 生成方式：AIGC image-to-image（参考"三代同堂教练团队"风格基准图），统一 3D 渲染、蓝青主色运动服、全身站立居中、纯白底；
  再用 JDK 工具做**边缘 flood-fill 抠透明**（保留人物内部白色运动鞋不被误切），落到 `res/drawable/`。
- 校验：三张四角均 0x00000000（真透明），透明占比 68–73%（人物居中、四周留白），符合"真透明 + 全身 + 居中"。
- 组件接入：`CoachVisual.coachPortraitResource(ageId)`（唯一映射点）+ `CoachVisual.CoachPortrait()`（736:1312 固定容器比例 + ContentScale.Fit，不裁切头脚）。

## 2. 横版教练图（既有，保留兼容）

| 资源 | 路径 | 尺寸 | 状态 |
|---|---|---|---|
| 青年（横版） | `res/drawable/coach_youth.png` | 1248×832 | exists（V3.3 既有）/ wired（CoachAvatar/CoachFigure 仍用，作为小头像回退）|
| 中年（横版） | `res/drawable/coach_middle.png` | 1248×832 | exists / wired |
| 银龄（横版） | `res/drawable/coach_senior.png` | 1248×832 | exists / wired |

> V3.5 大主视觉场景（引导页/启动页/个人中心）已切到竖版立绘；小头像场景保留横版图，避免大面积改动引入回归。

## 3. 教学/跟练视频资产矩阵（P0-6，18 条全量对照）

> 视频全部本地 `assets/videos/`，`assets/videos/*.mp4` 已打包。对照 `TeachingVideoRepository` 18 条索引
> （youth 10 / middle 4 / senior 4；其中 BMI/肺活量为 method-only 无动作视频）。

| # | 年龄段 | itemKey | 动作 | 视频文件 | 状态 |
|---|---|---|---|---|---|
| 1 | youth | long_jump | 立定跳远 | video_youth_long_jump.mp4 | exists / packed / 真人风格动作 |
| 2 | youth | pullup | 引体向上(男) | video_youth_pullup.mp4 | exists / packed |
| 3 | youth | situp | 仰卧起坐(女) | video_youth_situp.mp4 | exists / packed |
| 4 | youth | run_50m | 50 米跑 | video_youth_50m.mp4 | exists / packed |
| 5 | youth | forward_reach | 坐位体前屈 | video_youth_forward_reach.mp4 | exists / packed |
| 6 | youth | endurance | 800/1000 米 | video_youth_endurance.mp4 | exists / packed |
| 7 | youth | warmup | 热身&整理 | video_youth_warmup.mp4 | exists / packed |
| 8 | youth | youth_plank | 平板支撑 | video_youth_plank.mp4 | exists / packed |
| 9 | youth | bmi_method | BMI | —（method-only） | exists(方法说明) / 无动作视频（诚实） |
| 10 | youth | vital_capacity_method | 肺活量 | —（method-only） | exists(方法说明) / 无动作视频（诚实） |
| 11 | middle | middle_core | 核心强化 | video_middle_core.mp4 | exists / packed |
| 12 | middle | middle_flexibility | 柔韧恢复 | video_middle_flexibility.mp4 | exists / packed |
| 13 | middle | middle_cardio | 有氧减脂 | video_middle_cardio.mp4 | exists / packed |
| 14 | middle | middle_neck_shoulder | 肩颈放松 | video_middle_neck_shoulder.mp4 | exists / packed |
| 15 | senior | senior_baduanjin | 八段锦 | video_senior_baduanjin.mp4 | exists / packed |
| 16 | senior | senior_taichi | 太极 | video_senior_taichi.mp4 | exists / packed |
| 17 | senior | senior_balance | 平衡防跌 | video_senior_balance.mp4 | exists / packed |
| 18 | senior | senior_gentle_stretch | 银龄舒缓拉伸 | video_senior_gentle_stretch.mp4 | exists / packed |

### 3.1 教练风格（真人 REALISTIC / 动画 ANIMATED）视频矩阵（P0-5/P0-6）

当前策略（诚实，不跨风格/跨年龄段填充）：
- **REALISTIC** 默认对应 **youth 组**（真人风格示范视频）；
- **ANIMATED** 默认对应 **middle / senior 组**（动画风格 3D 形象 + 对应视频）。
- 用户风格若与该动作权威视频所属年龄段不一致，`CoachAssetRepository.resolveAsset` 返回
  `styleMatched=false` + 诚实 note（"该动作暂时只有 X 风格示范"），**不**用无关视频填空。
- BMI/肺活量恒为 method-only，`resolveAsset` 返回 `methodOnly=true`，UI 走录入/说明、不冒充自动测量。

| 风格 | 年龄段 | 已有动作视频 | 缺失（需补齐/标注） |
|---|---|---|---|
| REALISTIC | youth | long_jump/pullup/situp/50m/forward_reach/endurance/warmup/plank（8 条真人）| 无（youth 真人已齐）|
| ANIMATED | middle | middle_core/flexibility/cardio/neck_shoulder（4 条动画）| 无 |
| ANIMATED | senior | senior_baduanjin/taichi/balance/gentle_stretch（4 条动画）| 无 |

> 结论：**18 条索引全部有对应本地 MP4 或 method-only 诚实空态，无"缺失且需 AIGC 补齐"的动作视频。**
> 风格切换只影响"哪组视频/哪张立绘被选中"，不改动作 ID / 训练记录 / 评分 / 历史（见 P0-5 修复）。
> 若后续要"每动作都有真人 + 动画双份视频"，则需为 middle/senior 补真人版、youth 补动画版；
> 本次未做，标记为**未验证/可选项**，绝不伪造已补齐。

## 4. 业务图标体系（P0-3）

### 4.1 现状
- **控制/功能类图标**：使用 Material `Icons.Filled.*` 矢量图标（Accessibility/Check/Delete/History/
  Pause/PlayArrow/Replay/Schedule/Settings/Videocam/…），小尺寸下**锐利清晰**，符合"控制类用矢量图标"。
- **角色/形象类**：3 张竖版立绘 PNG（见 §1）+ 3 张横版 PNG（见 §2）。
- **业务"标签"**：部分用 emoji 文字（🔊/↻/💡/📺 等）表达语音/切换/提示/画面；**非**独立图标资源。

### 4.2 V3.5 结论
- 控制类图标：沿用 Material 矢量图标（不重绘，保证小尺寸清晰度）—— 判定为**无需重绘**。
- 角色形象类：竖版立绘已重绘并抠透明（§1）—— **已生成并接入**。
- emoji 提示：本次**未**替换为独立矢量资源（替换需逐页改动且收益有限），标记为**未处理**；
  不影响主流程，无低糊/不可读文本问题（emoji 为系统渲染，非放大模糊图）。

> 说明：若竞赛验收要求"全部业务图标为统一风格独立资源"，emoji 提示应替换为矢量图标；
> 本次按"控制类矢量 + 角色类立绘"交付，emoji 明确标注为未处理项，不冒充已完成。

## 5. 待办 / 未达成项（诚实标注）
- [ ] 竖版立绘 3 张：`wired` 已接入，**APK 打包 + 真机视觉验收**待 11.4 执行。
- [ ] emoji → 矢量图标替换：**未处理**（非 P0 关键路径，收益有限）。
- [ ] 每动作"真人+动画"双份视频：**未补齐**（当前矩阵已覆盖默认风格，双份为可选增强）。
