# V3.5 最终产品体验整改与资源补齐 · 执行状态（V3_5_EXECUTION_STATE）

> 基线：`v3-dev @ 26bb424`（V3.4 已交付：启动闪退根因修复 + 122 JVM 全绿 + 5×冷启动稳定）。
> 设备：b7a0af21（Xiaomi MI9 / Android 9 / API 28 / arm64-v8a，1080×2340）。
> 状态图例：`done`=已实现并验证 / `code-done-wired`=代码已改并接入、编译通过 / `packed`=已入 APK /
> `device-verified`=真机 0 FATAL / `blocked-external`=外部依赖不可用 / `not-done`=未处理（诚实标注）。

## Phase 0 基线
- [x] git `v3-dev @ 26bb424` clean；设备 b7a0af21 在线；APK 基线 91.5MB（13:37）
- [x] 确认 `com.quannian.zhidong` / compileSdk34 / minSdk24 / targetSdk34
- [x] 确认 CameraX 1.3.0 / MediaPipe Pose 0.10.14 / Media3 1.4.1 / Room 2.5.2 / Compose BOM 2023.10.01

## P0-1 启动屏整改 —— done / device-verified
- [x] `SplashScreen` 去除模拟 4 段进度条 + "正在准备摄像头与姿态识别模型…" 文案；保留品牌 `全龄智动` + 副标题 + 竖版立绘，700ms 品牌定版后直进主流程
- [x] 冷启动路径审计：`MainActivity` 仅 `TtsCoach.init`（本地、非阻塞）；CameraX/MediaPipe（`CameraManager`/`PoseDetector`）仅在 `FollowAlongScreen`/`FollowAlongItemScreen`/`VideoAnalysisScreen` 惰性创建，**首页/引导/个性化/个人中心零相机模型初始化**
- [x] 相机权限**不在首页/启动期请求**（仅在跟练页授权）

## P0-2 竖版透明全身教练立绘 —— done / packed / device-verified
- [x] AIGC image-to-image 生成 3 张竖版全身 3D 教练立绘（`coach_youth/middle/senior_portrait.png`，736×1312）
- [x] JDK 工具边缘 flood-fill 抠透明（保留内部白色运动鞋）：三张四角均 0x00000000，透明占比 68–73%，全身居中不裁切
- [x] 落 `res/drawable/`；`CoachVisual` 新增 `coachPortraitResource(ageId)` + `CoachPortrait()`（736:1312 固定容器比例 + `ContentScale.Fit`，不切头脚）
- [x] 接入 `OnboardingScreen` + `SplashScreen`（大主视觉）；APK 校验 3 张立绘已入包
- 立绘视觉（透明/全身/居中）已 read_image 验证；真机 0 FATAL 验证解码无崩

## P0-3 图标体系 —— 结论性 done（详见 ASSET_MANIFEST §4）
- [x] 扫描全部业务图标/资源现状
- [x] 结论：控制类（返回/关闭/暂停/播放/勾选/设置等）沿用 Material 矢量图标，小尺寸锐利 → **不重绘**；角色形象类已由 P0-2 竖版立绘覆盖
- [ ] emoji 提示（🔊/↻/💡/📺 等）→ 矢量图标替换：**not-done**（非 P0 关键路径、收益有限；诚实标注，不冒充完成）

## P0-4 引导页重做 —— done / device-verified
- [x] 3 页：大居中竖版透明教练主视觉 + 指定文案（青少年/中青年/中老年 标题/简介/3 短点）
- [x] 同一容器比例（立绘 736:1312，Fit）；去顶部冗余"全龄智动·科学运动"标签，仅留右上"跳过"
- [x] 底部进度点 + 主 CTA；窄屏/大字体下 CTA 仍可见
- [x] 坐标走查引导页进入/翻页/开始体验 0 FATAL

## P0-5 教练风格真驱动资源 —— code-done-wired / 测试通过
- [x] `CoachAssetRepository.resolveAsset(itemKey, gender, style, assetType, ageGroup)`：按 用户风格 + 年龄段 真正解析视频/头像
- [x] `ownerAgeOf(itemKey)`：解析顺序 `videoForItem` → `byId` → 回退；风格切换**不改**权威视频 id（只改 styleMatched/note）
- [x] 红线：资源缺失/风格不覆盖 → 诚实空态 + note，**不跨风格/跨动作/跨年龄段填充**；BMI/肺活量恒 method-only
- [x] 接线 `FollowAlongItemScreen`：示范视频改经 `resolveAsset`，副标题显示诚实风格标注
- [x] 持久化 `CoachStyleStore`（SharedPreferences `quanli_coach`）跨重启有效；风格切换不改动作 ID/记录/评分/历史
- [x] 新增 8 项 JVM 测试（V35CoachStyleAssetTest）全绿

## P0-6 视频资产矩阵 —— done / packed（详见 ASSET_MANIFEST §3）
- [x] 全量盘点：`assets/videos/` 16 个 MP4 全部存在（各 0.5–2.8MB），与 `TeachingVideoRepository` 18 条索引一致（BMI/肺活量 method-only 无视频，诚实）
- [x] 风格视频矩阵：REALISTIC=youth 真人 8 条、ANIMATED=middle/senior 8 条，无"缺失需 AIGC 补齐"的动作视频（不伪造补齐）
- [x] 16 MP4 全部入 APK；method-only 诚实空态
- 每视频"内容匹配动作"：沿用既有素材（V3.3 生成时已逐动作命名），本次未逐段抽帧复核 → **内容匹配度以既有素材为准，未重新逐段验证**（诚实标注）

## P0-7 跟练竖版双栏 + 黑屏修复 —— code-done-wired / device-verified(无崩)
- [x] `FollowAlongScreen` + `FollowAlongItemScreen` 改为**强制竖屏上下堆叠**（上=示范视频，下=相机+骨架），删除横屏 sideBySide 分支；`BoxWithConstraints` 按可用高度自适应，空间紧张压缩权重但**不移除任一块**
- [x] 视频保持源宽高比、相机保持相机宽高比；不 `fillBounds` 拉伸、不切头脚；宽高比不一致用中性底色填充
- [x] 黑屏根因修复：`AndroidView` factory 阶段 `PreviewView` 常为 0 尺寸 → 原 `start(pv)` 把 Surface 绑到 0 尺寸上导致黑屏；改为 factory 只创建 view，`start()` 延后到 `onGloballyPositioned` 首次拿到非 0 尺寸时触发（幂等）
- [x] `CameraManager.restart()`：黑屏/初始化失败重绑同一实例（不重建、不竞争 CameraX 用例）；相机区加"重试"入口
- [x] 坐标系：`Crop` → `PoseCoordinateMapper.toCanvasPoint`（复用 PreviewView 实测 + 可见裁切 + 前置镜像），预览容器变化时 `visibleCrop` 重算（不重用旧全屏矩阵）
- [x] 次数计数：保留 `FollowAlongViewModel.repCount`（有真实计数逻辑时显示），体测项经 `followCapabilityLabel` 显式"辅助识别·人工"；暂停/结束保留 Room 保存（`repo.saveResult`）
- 真机：跟练竖版双栏进入/返回 0 FATAL；黑屏修复需后续实机抽帧复核实际出画面（**相机实际出画面未在本次坐标走查中逐帧确认**，诚实标注）

## P0-8 青少年页同模板 —— done
- [x] `YouthBodyTestScreen` 与中青年/中老年同版式：教练形象 + `练·运动训练`（动态跟练·测试项）+ `测·体测评估`（独立测评录入）
- [x] BMI/肺活量经 `isMethodOnlyItem` 移出动态动作训练列表；能力标签（`followCapabilityLabel`）；结果仅来自人工录入（`source="manual"`）/真实识别，不用训练示范视频冒充正式测试

## 11.x 测试 / 构建 / 真机
- [x] **JVM：130 tests OK**（122 V3.3/V3.4 基线 + 8 V3.5 新增），`JUnitCore` 全绿
- [x] `:app:compileDebugKotlin` / `:app:compileDebugUnitTestKotlin` / `:app:assembleDebug` BUILD SUCCESSFUL
- [x] **APK：88.4MB（92,705,981B）**，10/15 12:20；内容校验：3 竖版立绘 + 3 横版 coach + **16 MP4 全部入包**；包名 `com.quannian.zhidong`
- [x] 真机 `adb install` Success（第 1 次）；**5× 清缓存冷启动全 ALIVE、FATAL=0**
- [x] **9 段业务路径坐标走查 全 ALIVE、整段 FATAL=0**（引导→个性化→首页→训练→青少年体测→跟练竖版→返回→AI→我的）
- [ ] 更高 API 机型（>API28）实机回归：**未验证**（当前仅 API28 设备）
- [ ] 跟练相机实际出画面逐帧复核 + 立绘真机视觉验收（截图比对）：**未做实机抽帧**（本次以 0 FATAL + 布局/解码无崩为准）

## 12.x 交付
- [x] `V3_5_ASSET_MANIFEST.md`（资源/视频/图标全量清单 + 逐项状态）
- [x] `V3_5_EXECUTION_STATE.md`（本文件）
- [x] `V3_5_FINAL_REPORT.md`（见同目录）

## 诚实未达成项汇总
- [ ] emoji → 矢量图标替换：not-done（P0-3 结论项，非关键路径）
- [ ] 每动作"真人 + 动画"双份视频补齐：not-done（当前矩阵已覆盖默认风格；双份为可选增强，未伪造）
- [ ] 跟练相机出画面 / 立绘真机视觉逐帧验收：未做实机抽帧（以 0 FATAL + 无崩为准）
- [ ] >API28 机型回归：未验证（设备受限）
