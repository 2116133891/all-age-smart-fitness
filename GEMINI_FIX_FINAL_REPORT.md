# GEMINI_FIX_FINAL_REPORT — 全龄智动 V3.6 修复最终报告

> 分支 `gemini-v3-6-fixes`（HEAD `e9855b1`，基于 `v3-dev` @ `f7e9bd9`；`v3-dev` 未改动，保留 `56434d3`）。
> 构建：2026-10-15 19:57。设备：**`adb devices` 为空（无真机/模拟器）** —— 真机走查与 Logcat 采集本轮**无法执行**，下文如实标注。

---

## 0. 环境硬限制（必须先声明）
- **无真机**：`adb devices -l` 输出空。因此 P0-A **没有新崩溃 Logcat**，P1-G 16 步真机走查**未执行**，**不能声称真机通过**。
- **无 ffprobe/Python**：16 个视频的**精确**源宽高比**未能测量**；下方 P0-B 用"稳健的 FIT letterbox"方案，不依赖确切比例。
- 因此：以下"已修复"均指**源码/构建/资源层面**已落地并通过编译与 JVM 单测；凡标注"未真机验证"的项目，**不得视为已真机确认**。

---

## 1. 实际修复了什么（按 P 级）

### P0-A 跟练页间歇性闪退 —— 源码定位，已修
**根因（代码路径，非日志证实）**：
1. `FollowAlongScreen` 缺少 `DisposableEffect` 相机释放：走**系统返回 / tab 切换**退出时，`CameraManager` 不释放，CameraX 用例与下一相机页竞争。
2. 更关键：`CameraManager.release()` 会 `shutdown()` 两个 `ExecutorService`；但相机窗 `onGloballyPositioned` 仍会调用 `start()`，而 `start()` 里 `poseInitExecutor.execute{}` 在 try/catch **之外**、且无 released 守卫 → 向已关闭的 executor 提交任务抛 `RejectedExecutionException`，发生在**主线程、未被捕获** → 整应用闪退。这正是"进入跟练后几秒间歇闪退"最吻合的机制。

**改动文件**：
- `camera/CameraManager.kt`：新增 `released` 标志；`start()/restart()/switchCamera()` 加 `if (released) return`；新增 `submitPoseInit()` 捕获 `RejectedExecutionException` 并降级；`release()` 置 `released=true` 且幂等。
- `ui/screens/FollowAlongScreen.kt`：新增 `DisposableEffect(cameraManager){ onDispose { cameraManager.release() } }`，覆盖所有退出路径。
- 回归：`V36RegressionTest`（源码守卫）锁定以上不变量。
- **未用空 catch / 未吞异常 / 未禁摄像头 / 未删功能**。

### P0-B 教学视频与摄像头比例 —— 已修
- 证据：`TeachingVideoPlayer` 旧容器固定 `aspectRatio(16/9)`；而示范视频源比例**并不统一**（横/竖/近方形混杂，精确值未测）→ 竖版/近方形视频头脚被裁。
- `ui/components/TeachingVideoPlayer.kt`：新增 `fixed16By9` 参数；`false` 时容器随父级剩余高度（`fillMaxWidth().weight(1f)`）、`PlayerView` 用默认 FIT 对**源真实比例**做中性黑边 letterbox（不拉伸、不裁切头脚）。
- `ui/screens/FollowAlongScreen.kt`：`TeachPane` 传 `fixed16By9 = false`，实现"上示范视频（FIT 完整）+ 下实时摄像头+骨架"上下双画面、同屏可见、不硬编码固定尺寸。
- 相机侧未重造坐标系：沿用 `CameraManager.visibleCrop`（`FILL_START`）+ `SkeletonOverlay`/`PoseCoordinateMapper` 单一映射源（已复核）。
- **未强制横屏、未 fillBounds、未隐藏任一块区域。**

### P0-C 真正接入用户新图标 —— 已接入（诚实边界）
- 复制进 `res/drawable`：`coach_realistic_youth_portrait.png`、`coach_realistic_middle_portrait.png`、`coach_realistic_senior_portrait.png`（3 张竖版真人立绘）、`coach_realistic_row.png`（横版真人三人合集）。
- 重复文件已用 SHA1 去重确认（`09234f92`=`e68beddc` 12 格拼图；`73f11407`=`a1bfc173` 横版合集）——**未**把拼图/合集当单个 App 图标使用。
- `ic_launcher`（mipmap）未改动（用户素材里无独立单图 App 图标，属"需用户补充素材"边界，见 §4）。
- 资源真实性：4 张新 PNG 已确认**打包进 APK**（ZIP 清单核实）；`V36RegressionTest` 断言文件非空存在。

### P0-D 青少年页面 —— 已对齐（结构性，非仅改标题）
- 源码核实：`YouthBodyTestScreen` 已"练/测"分离，`BMI`/`肺活量` 经 `isMethodOnlyItem` **排除出动作示范清单**、走独立测评录入（`MeasureItemCard`）；动态项目卡三入口（看示范/开始跟练/测试·录入）真实接 `onWatchItem`/`onFollow`/`onPickItem`。
- 改动：新增"青少年 · 数字教练"介绍头块（对齐中/老年模板的统一头部结构）。
- 真实导航已走查（源码）：青少年所有项目 → 正确页面；`youthBodyTestItem`/`followItem`/`teachingVideoItem` 路由正确；返回保留数据（Room `latestResult`）。

### P1-E 真人/动画风格资源映射 —— 已补齐图像侧
- 视频侧 `CoachAssetRepository.resolveAsset/resolveVideo` 已风格驱动（V35，8 用例）；本轮补齐**图像侧**：
  - `CoachVisual.coachResource` / `coachPortraitResource` 加 `style` 参数：`REALISTIC→用户真人立绘`、`ANIMATED→原有 3D 卡通`。
  - `Components.CoachAvatar` 默认读取 `CoachStyleStore` 并透传 → 首页 / Age / Exercise / 青少年页等自动风格感知。
  - 启动页 / 引导页读取用户风格。
- 资源矩阵（真实状态）：
  | 年龄段 | REALISTIC 图像 | ANIMATED 图像 | REALISTIC 视频 | ANIMATED 视频 |
  |---|---|---|---|---|
  | youth | ✅ 真人立绘+横版 | ✅ 3D 卡通 | 部分动作有真人视频 | 无（诚实 styleMatched=false + note） |
  | middle | 仅横版合集(共享) | ✅ 3D 卡通 | 无专属 | ✅ 动画视频 |
  | senior | 仅横版合集(共享) | ✅ 3D 卡通 | 无专属 | ✅ 动画视频 |
  - **真人示范缺失处不以动画视频替代**（`resolveVideo` 诚实返回）。视频生成工具本环境不可用 → **未生成、未假装生成**。

### P1-F 引导页人物 + 启动页
- 三张竖版角色图 = `coach_{youth,middle,senior}_portrait.png`（736:1312，`ContentScale.Fit`，引导三页统一走 `CoachPortrait`，不裁切头脚）。
- 启动页 `SplashScreen` 仅品牌定版 + `CoachPortrait`，**不初始化 CameraX/MediaPipe**（惰性到跟练页才创建相机）。

### P1-G 全链路回归
- **133 项 JVM 单测通过**（旧 125 + V34 + V35 + 新增 `V36RegressionTest` 8 项）；`:app:assembleDebug` **BUILD SUCCESSFUL**。
- 真机 16 步走查（冷启动/风格切换/青少年入口/同屏跟练/摄像头坐标/结束报告等）**未执行**（无设备）。

---

## 2. 修改了哪些文件（本轮，`git diff` 核对）
功能/资源（已提交 `e9855b1`，共 13 文件）：
1. `camera/CameraManager.kt` — released 守卫 + `submitPoseInit` 异常降级（P0-A）
2. `ui/screens/FollowAlongScreen.kt` — `DisposableEffect` 相机释放（P0-A）+ `TeachPane` 传 `fixed16By9=false`（P0-B）
3. `ui/components/TeachingVideoPlayer.kt` — `fixed16By9` 参数 + 动态高度容器 + PlayerView FIT（P0-B）
4. `ui/components/CoachVisual.kt` — `coachResource`/`coachPortraitResource` 风格感知（P0-C/P1-E）
5. `ui/components/Components.kt` — 顶层 `CoachAvatar` 默认读用户风格（P1-E）
6. `ui/screens/OnboardingScreen.kt` — 引导页立绘跟随用户风格（P1-E/P1-F）
7. `ui/screens/SplashScreen.kt` — 启动页立绘跟随用户风格（P1-F）
8. `ui/screens/YouthBodyTestScreen.kt` — 青少年"教练介绍"头块（P0-D）
9. 新增 4 资源 `res/drawable/coach_realistic_{youth,middle,senior}_portrait.png` + `coach_realistic_row.png`（P0-C）
10. 新增 `test/.../V36RegressionTest.kt`（P0-A/B/C/D/E 回归守卫）

**未改动**：未动 `gradle-wrapper.properties` 之外的无关模块；未删任何已有测试；未动 Room/历史记录；未动 MediaPipe 方案。

---

## 3. 闪退根因及证据
- 机制（代码路径）：`FollowAlongScreen` 退出未释放相机 + `CameraManager.release()` 关闭 executor 后，`onGloballyPositioned` 仍触发 `start()` → `poseInitExecutor.execute` 抛未捕获的 `RejectedExecutionException`（主线程）。
- 证据类型：**源码定位 + 修复后回归守卫**，**非** 真机 Logcat 证实（无设备）。
- 诚实声明：在拿到新 AndroidRuntime FATAL 栈之前，这是**最吻合候选根因**而非唯一已被日志证实的根因。

---

## 4. 图标资源哪些已真正替换
- **已真实接入**：3 张竖版真人教练立绘（youth/middle/senior）+ 1 横版真人合集，进 `res/drawable` 并被 `CoachVisual` 风格化引用、打包进 APK。
- **未接入/需补充**：
  - App 启动图标（`mipmap/ic_launcher`）：用户上传素材里**没有**独立单图 App 图标（`09234f92/e68beddc` 是 12 格产品拼图、`73f11407/a1bfc173` 是三人合集、`c4c3ac97` 是 12 格图标+界面合集），**不能**拿合集当单个 App 图标使用 → 需用户单独提供一张 App 图标素材。
  - 横版"单人"真人头像（与 `coach_youth/middle/senior` 一一对应的横版单人）：用户素材是**三人合集**横版，未切分成三人独立横版 → 未强行拆分，REALISTIC 头像暂用横版合集占位，需用户确认是否接受或补充单人横版。
- 操作类图标（返回/播放/暂停/设置）按需求**保留 Material 矢量图标**，未替换。

---

## 5. 青少年页面哪些实际变化
- 结构：确认"练（动态跟练·测试）+ 测（独立测评录入）"分离，`BMI`/`肺活量` 已排除出动作示范清单、走独立录入（`isMethodOnlyItem`）。
- 新增：页头"青少年 · 数字教练"介绍块（与中/老年统一模板对齐）。
- 交互：卡片三入口（看示范/开始跟练/测试·录入）走真实路由；`V36RegressionTest` 守卫"BMI/肺活量 不在动作清单"。
- 未做：真机像素级 UI 对比（无设备）。

---

## 6. 视频与摄像头比例如何修复
- 视频：`TeachingVideoPlayer.fixed16By9=false` → 容器随剩余高度、PlayerView FIT 源比例 letterbox（中性黑边、不拉伸、不切头脚）；跟练页已启用。
- 摄像头：保持 `FILL_START` + `visibleCrop`/`PoseCoordinateMapper` 单一坐标映射（未造第二套坐标系），前置镜像与分析帧镜像一致。
- 精确源比例：本环境无 ffprobe，**未测**；修复采用稳健 FIT 方案，不依赖确切比例。

---

## 7. 真人/动画资源矩阵（完成/缺失）
- 图像：REALISTIC=3 竖版真人立绘 + 1 横版合集（真实存在、已打包）；ANIMATED=原 6 张 3D 卡通。
- 视频：youth 部分真人示范视频已随包；middle/senior 为动画风格；**真人视频缺失的 middle/senior 项诚实标注**，未用动画替代。
- 未生成任何新视频（本环境无视频生成工具，不假装生成）。

---

## 8. 执行过的测试
- `:app:compileDebugKotlin` 通过；`:app:compileDebugUnitTestKotlin` 通过。
- 全量 JVM 单测（JUnitCore 直跑，15 测试类）：**OK (133 tests)**，含 `V36RegressionTest`（8 项，锁定 P0-A/B/C/D/E 修复不变量）。

## 9. 构建是否成功
- `:app:assembleDebug` → **BUILD SUCCESSFUL in 2m 21s**。

## 10. 最新 APK
- 路径：`C:\Users\23163\Desktop\城市大赛项目\app\build\outputs\apk\debug\app-debug.apk`
- 体积：**98,884,628 B（94.3 MB）**
- 构建时间：**2026-10-15 19:57:50**
- 包内已核实：16 个 MP4 + `pose_landmarker.task` + 4 张新真人教练 PNG。

## 11. 真机测试状态
- **经过真机测试的项：无**（本环境 `adb devices` 为空，未连任何设备/模拟器）。
- 未执行：进跟练复现闪退、16 步全链路走查、逐页截图、骨架人体对齐目检、小屏竖屏布局实机验证。
- 诚实声明：本报告**不声称任何真机通过**；上述修复在**源码 + 构建 + JVM 单测 + APK 资源清单**层面验证，真机验证需在有设备的环境补做。

---

### 给下一位接手者的下一步（需真机）
1. `adb -s <b7a0af21> logcat -b crash -v time` 清缓冲 → 冷启动 → 进跟练页停留数秒 → 进出 3 次 → 确认 crash buffer 是否仍出现 FATAL；若有，用 `CameraManager`/`FollowAlongScreen` 栈精确定位。
2. 逐页截图验证 P0-B 视频/相机双画面在 1080x2340 上的实际渲染与骨架对齐。
3. 补一张独立 App 图标素材与 3 张单人横版真人头像，替换 `mipmap/ic_launcher` 与横版占位。
4. 若需精确视频比例，装 ffprobe 跑 `ffprobe -select_streams v -show_entries stream=width,height`。
