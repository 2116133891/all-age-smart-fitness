# V3.6 EXECUTION STATE（第二轮 · 诚实状态）

生成时间：2026-10-16 14:1x（本地）
分支：`gemini-v3-6-fixes`，本轮提交 `7a102f2`，已 fast-forward 推送 `origin/main`（`f2b13da..7a102f2`）。
设备：Xiaomi MI 9（b7a0af21，cephseus），USB PnP 节点状态不稳定（多次 Error/Disabled）。

## 状态图例
PASS=已验证通过 · DONE=已完成（编译/提交/推送） · IN_PROGRESS=进行中 · FAIL=失败 · BLOCKED_EXTERNAL=受外部条件阻塞 · MISSING=素材/资源缺失。

## 逐项状态
| # | 事项 | 状态 | 证据 / 说明 |
|---|------|------|-------------|
| 1 | 素材 ZIP（app_brand_mark / coach_* / nav_* / category_* / ASSET_MAPPING.md）接入 | **MISSING → 部分 BLOCKED** | 本机未找到该 ZIP；仅存 4×4 联系表（蓝绿色3D健身图标合集.png）+ 10 张旧上传。按交接规则**未凭空生成同名资源**；只能从联系表裁剪"可识别"的 tile，映射关系需以 ASSET_MAPPING 为准（缺失）。 |
| 2 | P0 图标替换（品牌/年龄段教练/底部导航/业务图标）并**接入源码**（非仅拷贝 res） | **IN_PROGRESS** | 因素材 ZIP 缺失，本轮未做整套替换；现有 CoachAvatar / 导航图标沿用前轮资源，风格由 CoachVisual/CoachAssetRepository 驱动，可接受新资源后按映射替换。 |
| 3 | P0 #4 青少年体测教练卡"异常拉长蓝竖条" | **BLOCKED_EXTERNAL** | 静态源码排查：YouthBodyTestScreen `SectionLabel` 蓝条为固定 `width(4.dp).height(16.dp)`、教练介绍 Card 为 Row 无 fillMaxHeight；AgeScreen 教练卡 Box 56dp + CoachAvatar 44dp，全仓无 `fillMaxHeight`/`matchParentSize` 于该卡片。真机复现需设备在线，本轮设备反复离线，**未能在真机定位/复现**该竖条的确切 Composable，不臆造修复。 |
| 4 | P0 #5 跟练"开始跟练"后教学视频消失 | **DONE + PASS** | 根因：独立教学页 TeachingVideoScreen 直接播放导航传入 assetPath（可用）；而 `FollowAlongItemScreen`（route `followItem/{key}`）改用 `CoachAssetRepository.resolveAsset(style)` 重新解析，风格组合缺专属视频时 `resolved=null` → 诚实空态卡 → 观感"消失"。修复：风格解析为 null（且非 methodOnly）时回退到权威同源 `TeachingVideoRepository.videoForItem(itemKey,gender) ?: byId(itemKey)`，并显示"风格专属示范暂缺，先播放该动作标准示范"，不做跨动作/跨风格替换。 |
| 4b | #5 真机验证 | **PASS** | b7a0af21 安装新 APK 后：青少年体测 → 50米跑「开始跟练」→ 跟练页"动作示范·50米跑"视频正常（00:00/00:04），竖屏上下双画面（示范在上、摄像头在下，"正在启动智能识别"），**视频不再消失**。截图 `.agnes/work/v36_follow.png`。 |
| 5 | P0 #13-16 竖屏跟练比例（示范上/摄像头下、双画面、禁横屏/禁拉伸） | **PASS（跟练双栏）+ IN_PROGRESS（独立复核）** | 跟练双栏已真机确认竖屏、上下双画面、无横屏、无明显拉伸（letterbox 保留）；独立比例专项复核待设备在线补齐。 |
| 6 | P0 视频矩阵（REAL + ANIMATED，逐动作；仅补缺失组合、复用已有） | **IN_PROGRESS** | 需先核实 assets/videos 已有 16 个 MP4 真实内容，仅对缺失的 ageGroup×style 组合用 agnes 视频工具生成；BMI / 肺活量保持 methodOnly（无动作视频）。本轮未调用生成（设备离线 + 未启动 agnes 视频工具）。 |
| 7 | P1 青少年页与中/老年统一（练·运动训练 / 测·体测评估） | **PASS（结构已统一）** | YouthBodyTestScreen 已有「练 · 运动训练 + 测 · 体测评估」统一头块与 SectionLabel 分组（动态跟练·测试 / 独立测评录入），与中/老年模板一致。 |
| 8 | 构建 + JVM 全量测试 + 打包 | **DONE** | 编译 `:app:compileDebugKotlin`、`:app:assembleDebug` BUILD SUCCESSFUL（JDK17）。APK `app\build\outputs\apk\debug\app-debug.apk` = **99,797,493 B**（≈95.2 MB）。JVM 全量经 `run_v36_tests.ps1` 直跑 JUnitCore：**OK (133 tests)**。 |
| 8a | Gradle `:app:testDebugUnitTest` | **FAIL（环境非回归）** | 报 "16 tests completed, 16 failed"。查 `app/build/test-results/testDebugUnitTest/*.xml`：16 个测试类 `initializationError` 均为 `ClassNotFoundException`（中文路径 + Gradle 测试 worker classloader 问题），**非 #5 代码回归**。直跑 JUnitCore 全绿即证。 |
| 9 | 交付 4 文件 + 推送 | **DONE** | 本文件 + ASSET_MANIFEST + COACH_VIDEO_MATRIX + FINAL_REPORT；提交 `7a102f2` 已推送 `origin/main`（fast-forward，无 force）。 |

## 本轮结论
- #5（P0 视频消失）：已根因定位、源码修复、编译打包、真机 PASS。
- 构建/测试：编译打包绿；JVM 133 项全绿；Gradle 16/16 失败为环境 classloader，已用直跑 JUnitCore 佐证。
- #4 蓝条：源码层面未找到会"异常拉长"的固定尺寸竖条；确切根因需真机复现（BLOCKED_EXTERNAL）。
- 素材（#1/#2）与视频矩阵（#6）：因素材 ZIP 缺失 / 生成未启动，标 MISSING / IN_PROGRESS，未凭空生成。
