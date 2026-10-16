# V3.6 ASSET MANIFEST（素材清单 · 诚实）

生成时间：2026-10-16。本轮**未凭空生成任何同名资源**；以下只记录本机上确实存在的素材与接入状态。

## 1. 输入素材来源
| 项 | 状态 | 说明 |
|----|------|------|
| 素材 ZIP（app_brand_mark / coach_* / nav_* / category_* / ASSET_MAPPING.md） | **MISSING** | 项目根、`.agnes/uploads`、桌面/下载均未找到该 ZIP；无法按 ASSET_MAPPING 做 1:1 替换。 |
| 4×4 联系表（蓝绿色3D健身图标合集.png） | 存在 | `C:\Users\23163\Desktop\城市大赛项目\.agnes\uploads\20261015_4\upload_eced13f7-b7bf-4bb0-b2d1-192a7561a5d4.png`（1,559,196 B）。可裁剪"可识别"的 tile，但缺 ASSET_MAPPING，映射需人工确认。 |
| 10 张旧教练上传 PNG | 存在 | 位于 res/drawable（见下）。 |

## 2. 已接入源码的图标（res/drawable）
| 资源 | 用途 | 接入点 | 状态 |
|------|------|--------|------|
| coach_youth.png / coach_youth_portrait.png | 青少年教练头像/立绘 | CoachAvatar(ageId="youth") | PASS |
| coach_middle.png / coach_middle_portrait.png | 中年教练 | CoachAvatar(ageId="middle") | PASS |
| coach_senior.png / coach_senior_portrait.png | 老年教练 | CoachAvatar(ageId="senior") | PASS |
| coach_realistic_youth/middle/senior_portrait.png | 写实风格立绘（P0-C） | CoachVisual 风格驱动 | PASS |
| coach_realistic_row.png | 写实立绘合集条 | 展示 | 存在 |

> 说明：底部导航（首页/训练/AI 助手/我的）与业务卡片图标当前使用 Compose **Vector Icon**（`Icons.Filled.*`），非位图；本轮 spec 的 nav_*/category_* 位图因 ZIP 缺失**未替换**，保持矢量图标现状（渲染正常，真机已验证）。品牌 app_brand_mark 位图 ZIP 缺失，沿用现有 launcher 图标。

## 3. 接入原则
- 图标/立绘统一经 `CoachVisual` / `CoachAssetRepository` 按 ageGroup × style 解析，**非**直接拷贝进 res 目录了事。
- 新资源到位（ZIP / ASSET_MAPPING）后，按映射替换 CoachAvatar/导航/业务图标即可，无需改动解析框架。
- 本轮未生成、未覆盖任何已有位图，保证既有素材不回归。

## 4. 本轮变更文件（真接入）
- `run_v36_tests.ps1`（新增，直跑 JUnitCore 测试脚本，规避中文路径 classloader 问题）
- 其余图标/视频资源**未改动**（诚实：素材缺失 + 未启动 agnes 生成）。
