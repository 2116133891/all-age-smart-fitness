# V3.6 COACH / VIDEO MATRIX（教练视频矩阵 · 诚实）

生成时间：2026-10-16。所有 `videos/` 文件为本机实测存在（`Get-ChildItem` 逐一核验，均非空）。
权威来源：`TeachingVideoRepository.BY_ID`（18 条）＋ `EXERCISE_TEACHING_VIDEO`（8 条严格动作匹配）＋ `CoachAssetRepository`（ageGroup × style 解析）。

## 0. 风格 → 年龄段映射（代码事实）
`STYLE_AGE = { REALISTIC -> {youth}, ANIMATED -> {middle, senior} }`
- 青少年(youth) 用 **写实 REALISTIC** 教练立绘；中年/老年 用 **动画 ANIMATED** 立绘。
- **风格驱动的是"教练头像/立绘"视觉，不改变示范视频内容**：每个动作只有一套规范示范视频（canonical），写实/动画风格共用同一示范。不造假生成"动画版深蹲"之类重复文件。

## 1. 动作示范视频矩阵（itemKey × ageGroup × 视频文件）
✅ = 本机已有真实 MP4 并已接入 `BY_ID`；☑ = method-only（设计无视频，诚实卡片）

| ageGroup | 风格 | itemKey | 视频 id | 文件 | 状态 |
|---|---|---|---|---|---|
| youth | REALISTIC | RUN_50M | run_50m | video_youth_50m.mp4 (954,833 B) | ✅ |
| youth | REALISTIC | RUN_800_1000 | endurance | video_youth_endurance.mp4 (1,617,746 B) | ✅ |
| youth | REALISTIC | LONG_JUMP | long_jump | video_youth_long_jump.mp4 (515,868 B) | ✅ |
| youth | REALISTIC | SEATED_REACH | forward_reach | video_youth_forward_reach.mp4 (607,885 B) | ✅ |
| youth | REALISTIC | PULLUP_SITUP(男) | pullup | video_youth_pullup.mp4 (651,306 B) | ✅ |
| youth | REALISTIC | PULLUP_SITUP(女) | situp | video_youth_situp.mp4 (632,193 B) | ✅ |
| youth | REALISTIC | warmup | warmup | video_youth_warmup.mp4 (518,737 B) | ✅ |
| youth | REALISTIC | (plank 核心) | youth_plank | video_youth_plank.mp4 (505,074 B) | ✅ |
| youth | REALISTIC | BMI | bmi_method | —（方法说明，无动作视频） | ☑ method-only |
| youth | REALISTIC | VITAL_CAPACITY | vital_capacity_method | —（需专用肺活量仪） | ☑ method-only |
| middle | ANIMATED | middle-gym/cardio | middle_cardio | video_middle_cardio.mp4 (2,864,844 B) | ✅ |
| middle | ANIMATED | middle-stretch | middle_flexibility | video_middle_flexibility.mp4 (1,076,745 B) | ✅ |
| middle | ANIMATED | middle-neck | middle_neck_shoulder | video_middle_neck_shoulder.mp4 (662,569 B) | ✅ |
| middle | ANIMATED | (核心强化) | middle_core | video_middle_core.mp4 (897,131 B) | ✅ |
| senior | ANIMATED | senior-baduanjin | senior_baduanjin | video_senior_baduanjin.mp4 (1,247,004 B) | ✅ |
| senior | ANIMATED | senior-taichi | senior_taichi | video_senior_taichi.mp4 (826,283 B) | ✅ |
| senior | ANIMATED | senior-balance | senior_balance | video_senior_balance.mp4 (1,001,530 B) | ✅ |
| senior | ANIMATED | senior-easy | senior_gentle_stretch | video_senior_gentle_stretch.mp4 (691,246 B) | ✅ |

**合计：16 个真实 MP4（全部接入，全部 `assetPath` 非空）＋ 2 个 method-only（按设计无动作视频）。** 与 `assets/videos` 目录 16 个 MP4 一一对应，无缺文件。

## 2. 严格动作匹配（EXERCISE_TEACHING_VIDEO，8 条）
senior-baduanjin→八段锦、senior-taichi→太极、senior-easy→gentle_stretch、senior-balance→balance、
middle-neck→neck_shoulder、middle-stretch→flexibility、middle-gym→cardio、youth-yoga→forward_reach。
**不做泛化替换**：youth-squat / youth-shoulder 无专属动作视频 → 诚实"暂无该动作示范视频"卡（非塞无关视频）。

## 3. #5 修复对矩阵的影响（本轮）
`FollowAlongItemScreen` 现在按此矩阵的"canonical 同源"兜底：
`styleMissing && !methodOnly → TeachingVideoRepository.videoForItem(itemKey,gender) ?: byId(itemKey)`。
- 每个动作只要有"规范视频"，跟练页就必播放（真机 50米跑 已验证 PASS）；
- 仅当该 itemKey 连规范视频都没有（未知 key）才走诚实空态；
- BMI / 肺活量 始终 method-only，不播放视频（红线）。

## 4. 缺失 / 待办（诚实）
- **无"每动作 × 每风格"的多版本视频文件**：设计上示范视频按动作唯一，风格只换教练立绘。若要"写实版/动画版同一动作"双视频，属新增生成任务（需 agnes 视频工具 + 真机核验），本轮未做，记录为 **IN_PROGRESS / BLOCKED（设备离线 + 生成未启动）**。
- youth-squat、youth-shoulder 等缺专属动作视频 → 维持诚实卡片，不生成无关替代。
- 生成规范：仅对"确实缺失的动作"补齐，复用现有 16 个有效 MP4，**不重制已有有效视频**（用户红线：reuse existing valid videos）。
