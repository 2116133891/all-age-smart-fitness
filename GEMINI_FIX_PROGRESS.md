# GEMINI_FIX_PROGRESS — 全龄智动 V3.6 修复进度

> 分支 `gemini-v3-6-fixes`（基于 `v3-dev` @ `f7e9bd9`，保留原分支未覆盖）｜ 构建 2026-10-15
> 环境限制：**adb devices 为空（无真机/模拟器连接）**，无法采集新 Logcat、无法真机走查。
> 因此 P0-A 闪退根因**从源码定位**（证据 = 代码路径），未用日志证实；下文如实标注。

## 当前真实起点（独立核实，非旧报告）
- `git`：v3-dev @ `f7e9bd9`（与 origin 0/0），工作区原有 4 个未跟踪 md（保留未动）。
- 旧 APK `app-debug.apk` 92,705,981 B（88.4MB，10/15 12:20）→ 本轮新 APK 98,884,628 B（94.3MB，10/15 19:57）。
- 图标素材 10 张在 `.agnes/uploads/20261011_1/`（不在 git、未被旧源码引用），逐张查看确认。
- 重复文件（SHA1）：`09234f92`=`e68beddc`（12 格产品拼图）；`73f11407`=`a1bfc173`（横版三人合集）。

## 逐项进度

### P0-A 跟练闪退（源码定位，未日志证实）
- [x] 设备检查：`adb devices` 空 → **无真机复现证据**（已如实标注）。
- [x] 源码根因 1：`FollowAlongScreen` 缺 `DisposableEffect` 相机释放 → 系统返回/tab 切换退出时 CameraX 用例不释放，与下一相机页竞争。
- [x] 源码根因 2：`CameraManager.release()` 关闭 `poseInitExecutor`，但 `onGloballyPositioned` 仍会 `start()`，其中 `poseInitExecutor.execute{}` 在 try/catch 外且无 released 守卫 → `RejectedExecutionException` 在主线程未捕获 → 闪退。
- [x] 修复：`CameraManager` 加 `released` 守卫 + `submitPoseInit()` 捕获 `RejectedExecutionException`；`FollowAlongScreen` 加 `DisposableEffect(cameraManager){ onDispose { release() } }`。
- [x] 回归：`V36RegressionTest`（源码守卫）锁定"release 后 start 幂等 + 捕获 RejectedExecutionException + FollowAlong 释放相机"。
- [ ] 真机复现/走查：**未执行**（无设备）。

### P0-B 视频/摄像头比例
- [x] 证据：16 个 MP4 源宽高比高度不一致（横/竖/近方形混杂）；`TeachingVideoPlayer` 旧容器固定 `aspectRatio(16/9)` → 竖版/近方形视频被裁切头脚。
- [x] 修复：`TeachingVideoPlayer` 新增 `fixed16By9` 参数；`false` 时容器随父级剩余高度、`PlayerView` FIT 对源比例 letterbox（中性黑边，不拉伸不裁切）；跟练页传 `fixed16By9=false`。
- [x] 相机侧沿用 `FILL_START` + `SkeletonOverlay`/`PoseCoordinateMapper` 单一映射源（已复核，未再造第二套坐标系）。
- [ ] 逐视频源比例精确值（需 ffprobe，本环境不可用）与真机渲染：**未执行**。

### P0-C 图标真正接入
- [x] 复制 3 张竖版真人立绘 + 1 横版合集进 `res/drawable`（`coach_realistic_{youth,middle,senior}_portrait.png`、`coach_realistic_row.png`）。
- [x] `CoachVisual.coachResource / coachPortraitResource` 加 `style` 参数：`REALISTIC→真人`、`ANIMATED→3D 卡通`。
- [x] `Components.CoachAvatar` 默认读取 `CoachStyleStore` 并透传 → 首页/Age/Exercise/Report 等自动风格感知。
- [x] 验证：4 张新资源确认打包进 APK；`V36RegressionTest` 断言文件真实存在。
- [x] 诚实判断：用户上传的真人立绘带 studio 渐变背景（非透明），横版为三人合集；**不拿拼图/合集当单个 App 图标**（`ic_launcher` 未改）。

### P0-D 青少年页面
- [x] 源码核实：`YouthBodyTestScreen` 已"练/测"分离，BMI/肺活量经 `isMethodOnlyItem` 排除出动作清单、走独立测评录入；卡片三入口（看示范/开始跟练/测试·录入）真实接 `onWatchItem/onFollow/onPickItem`。
- [x] 对齐改动：新增"青少年 · 数字教练"介绍头块（与中/老年模板一致的头部结构）。
- [ ] 真机 UI 与中/老年页像素级对比走查：**未执行**（无设备）。

### P1-E 真人/动画风格资源映射
- [x] `CoachStyle`（REALISTIC/ANIMATED）+ `CoachAssetRepository.resolveAsset`（视频侧已风格驱动，V35 有 8 用例）。
- [x] 本轮补齐**图像侧**：`coachResource`/`coachPortraitResource`/`CoachAvatar` 均风格感知；启动页/引导页读取用户风格。
- [x] 资源矩阵：REALISTIC→真人立绘(3)+横版合集(1)，ANIMATED→3D 卡通(原有 6 PNG)。**真人示范视频缺失的不以动画替代**（`resolveVideo` 已诚实 styleMatched=false + note）。

### P1-F 引导页人物 + 启动页
- [x] 三张竖版角色图 = `coach_{youth,middle,senior}_portrait.png`（736:1312，ContentScale.Fit，不裁切头脚）；引导页三页统一走 `CoachPortrait`。
- [x] 启动页 `SplashScreen` 仅品牌定版 + `CoachPortrait`，**不初始化 CameraX/MediaPipe**（惰性到跟练页才创建）。
- [ ] 真机"人物足够大/头手脚不裁切"目检：**未执行**。

### P1-G 全链路回归
- [x] 133 项 JVM 单测通过（含新增 `V36RegressionTest`；旧 125 + V34 + V35）。
- [x] `:app:assembleDebug` BUILD SUCCESSFUL；APK 94.3MB；16 MP4 + pose_landmarker.task + 4 张新真人资源确认在包。
- [ ] 冷启动→各入口→结束 的真机 16 步走查：**未执行**（无设备，不能声称通过）。

## 构建与产物
- 分支 `gemini-v3-6-fixes`，最新提交 `e9855b1`。
- 最新 APK：`app\build\outputs\apk\debug\app-debug.apk`，98,884,628 B（94.3MB），2026-10-15 19:57:50。
- 保留 `v3-dev`（`56434d3`）未改动。
