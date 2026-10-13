# 全龄智动 V3.3 · 资源清单（V3_3_ASSET_MANIFEST）

> 分支 `v3-dev` · 本轮记录 V3.3 第二轮（视觉统一 / 教练风格 / 引导页 / 练测结构 / 跟练双栏）的资源映射与复用状态。
> **诚实声明：本轮 Agnes AIGC 图像生成服务不可用（HTTP 503 no available server），无法生成新的竖版角色 PNG 与重绘图标。以下资源全部为复用/已有，未用占位图或纯色块冒充新资源。**

## 教练角色（coach）

| 年龄段 | 资源 | 位置 | 规格 | 状态 | 说明 |
|---|---|---|---|---|---|
| 青少年 | `coach_youth.png` | `res/drawable/coach_youth.png` | 1248×832 横版 · 浅灰底 | 复用（已有） | 真人风格主视觉；`CoachVisual.coachResource("youth")` 唯一映射 |
| 中青年 | `coach_middle.png` | `res/drawable/coach_middle.png` | 同上 | 复用（已有） | 动画风格主视觉；映射 `coachResource("middle")` |
| 中老年 | `coach_senior.png` | `res/drawable/coach_senior.png` | 同上 | 复用（已有） | 动画风格主视觉；映射 `coachResource("senior")` |

- 展示：`CoachVisual.CoachAvatar` / `CoachFigure` 用 `ContentScale.Fit`（保持真实比例，**不 fillBounds 拉伸**）。
- 引导页（§2.3）计划资源 `coach_youth_portrait` 等竖版素材：因生图 503 **未生成**，暂用现有 `coach_*` 横版素材以 Fit 展示（不拉伸）。**待生图服务恢复后补充竖版全身素材并替换。**

## 教学 / 示范视频

| 组 | 已有真实 MP4 | 数量 | 状态 |
|---|---|---|---|
| youth（真人风格） | long_jump / pullup / situp / run_50m / forward_reach / endurance / warmup / youth_plank | 8 | 复用 |
| middle（动画风格） | middle_core / middle_flexibility / middle_cardio / middle_neck_shoulder | 4 | 复用 |
| senior（动画风格） | senior_baduanjin / senior_taichi / senior_balance / senior_gentle_stretch | 4 | 复用 |

- 权威映射单一来源：`TeachingVideoRepository.videoForItem(itemKey, gender)` / `byId(id)`。
- BMI / 肺活量 = method-only（`assetPath=""`），**不映射**到任何动作视频；UI 走"测评·录入"。
- 跟练页按 `exerciseId + ageGroup + coachStyle` 经 `CoachAssetRepository.resolveVideo` 取视频；缺失返回诚实空态，**不跨年龄段填空**，**不拿火柴人冒充真人视频**。

## 教练风格（V3.3 新增）

| 项 | 值 |
|---|---|
| 配置 | `CoachStyle.REALISTIC` / `CoachStyle.ANIMATED`（`model/CoachStyle.kt`） |
| 持久化 | `CoachStyleStore`（本地 SharedPreferences，重启仍有效） |
| 默认 | 青少年=REALISTIC；中青年/中老年=ANIMATED（`CoachStyle.defaultForAgeGroup`） |
| 资源层 | `CoachAssetRepository.resolveVideo(ageGroup, style, videoId)`（纯函数，JVM 可测） |
| 影响 | 头像 / 示范视频资源选择；**不改**动作 ID / 训练记录 / 评分 / 历史成绩 |

## 本轮生成/替换的资源
- 无新生成图像或视频（生图服务 503）。全部为复用 + 代码接入（个性化风格选择、引导页版式、青少年练/测、跟练双栏）。
- 新代码文件：`model/CoachStyle.kt`、`repository/CoachAssetRepository.kt`、`test/V33NamingStyleTest.kt`。

## 外部阻塞（BLOCKED_EXTERNAL）
1. **竖版角色 PNG / 全套图标重绘**：Agnes AIGC 图像生成服务 503 不可用 → 无法产出新素材。已用现有素材 + 代码版式先行落地，避免阻断主流程；待服务恢复后补齐并替换。
2. 真机逐页点击 / 旋转 / 摄像头权限验收：设备 `b7a0af21` 在线，本轮 APK 已安装（Success），逐页人工点按仍建议人工确认（MIUI 屏蔽 uiautomator）。
