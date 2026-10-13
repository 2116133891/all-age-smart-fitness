# V3.2 紧急修复冲刺 · 执行状态

> 原则：对"真实运行页面/源码"重新核验并直接改源码、资源、页面；连续执行，不一模块一停；不伪造结果；外部阻塞标记后继续。

## 复核基线（本回合开始）
- 分支：`v3-dev`。改动 8 + 未跟踪（AiFacade/ApiKeyStore/CapabilityRegistry/PoseCoordinateMapper/PoseAndVideoV3Test + 9 个新视频 mp4 + 3 个报告/清单 md）。
- V3.1 基线：80/80 JVM 测试绿；APK 87.82MB；Xiaomi MI9/Android9 无崩溃（MIUI rapid_charge 遮挡 uiautomator，真机逐页点击为人工项）。
- 教学视频：TeachingVideoRepository 18 条（youth 10 / middle 4 / senior 4），可播 mp4 16 + method-only 2；byId() 为正确解析。

## P0-1 视频动作分析 ✅
根因定位（实读 VideoAnalysisScreen / VideoFrameReader / PoseDetector / Channel / Pick / NavHost）：
- ① `currentPhase` 全程保持默认"等待分析"，从未在抽帧/评分时实时更新 → 卡片恒显示"阶段：等待分析 · 次数：0"。
- ② `doneFrames/totalFrames` 已算出但**从不在 UI 显示**（"已处理"标签旁没有数字）→ 观感=0 帧。
- ③ 抽帧=最多 720 帧 MediaPipe 同步推理，无实时进度读、无取消、无 finally 释放 detector → 长时间卡住。
- ④ 无论 0 帧/无人，一律走"分析完成"并跳报告页 → 伪造结果。
已改：
- video/VideoFrameReader.kt：extractFrames 增 `isCancelled` + `onProgress(done,total,personFrames)`（识别人体帧计数=覆盖率）。
- ui/screens/VideoAnalysisScreen.kt：重写。Stage 增 NO_PERSON/CANCELED；currentPhase 随 提取→识别→评分→完成 真实切换；实时显示 已处理 N/M 帧 + 覆盖率% + 人体帧 + 综合评分(仅DONE)；RUNNING 遮罩显示已处理帧；取消 AtomicBoolean 早退 + 协程 cancel + DisposableEffect(onDispose)；finally detector.close()；诚实：无人→NO_PERSON 指引、视频不可读→ERROR 重试/重选；启动在 Dispatchers.IO 防 ANR。
- video/VideoAnalysisEngine.kt：no-person（validFrameCount=0）时 avgPosture/avgSym 默认 0（非1f）→ 综合评分=0，不伪造高分。
- test/VideoAnalysisHonestTest.kt：新增 3 项。
验证：:app:clean :app:compileDebugKotlin 绿（4m26s）；:app:compileDebugUnitTestKotlin 绿；JVM 全量 8 类 83 项全绿（含新增3）。

## P0-2 中年/银龄数字人（火柴人→真视频）✅
根因：数字人教学页 CoachScreen（coach/{id}）教学舞台用 CoachSpriteView→CoachMotionView（Compose Canvas 关节火柴人）。
已改：
- repository/TeachingVideoRepository.kt：新增 `EXERCISE_TEACHING_VIDEO` 严格匹配表 + `teachingVideoFor(exerciseId)`（八段锦→八段锦/太极→太极/肩颈→肩颈/久坐→柔韧/力量→有氧；无专属视频动作返回 null，不 generic substitution）。
- ui/screens/CoachScreen.kt：教学舞台 火柴人 → TeachingVideoPlayer 匹配真视频（loop）；无匹配视频走诚实"暂无该动作示范视频"卡片（示要件/安全提示）；顶部保留小 CoachAvatar 身份识别。
- ui/components/TeachingVideoPlayer.kt：加 `loop` 参数（教学循环播放）+ 容忍空 title。
说明：FollowAlongScreen 的 CoachSpriteView 是"实时跟练"（livePose 驱动），非教学火柴人，保留；ExerciseScreen/AgeHealthScreen 仅用静止 CoachAvatar 头像，非教学主体。
验证：:app:compileDebugKotlin 绿；新增 test/TeachingVideoMatchTest.kt(4)。

## P0-3 备考训练计划交互重做（进行中）
- [ ] 读 YouthTrainingPlanScreen 现状（蓝色控件无文字/周期/天数/生成态）
- [ ] 控件可见文字 + 周期2/4/8 + 天数1–7 单选 + 生成按钮真实状态/加载/防双击
- [ ] 真实调用 TrainingPlanService(weeks,daysPerWeek) + 可操作任务卡 + byId 视频 + method-only 显示测量指引
- [ ] 单测

## P0-4 个性化设置 + AI 服务设置
- [ ] 信息结构/中文标签/年龄(青年/中年/银龄) 性别(男/女) 选项统一 + 真实保存持久化 + 同步依赖页
- [ ] AI 配置分组(服务/endpoint/model/Key) + 正确 endpoint 构造(不重复 /v1/chat/completions) + 真实 HTTP 连接测试 + Key 安全（加密存储/脱敏日志）

## P1 回归
- [ ] 训练/数据闭环、中年银龄视频播放、4 底部导航(首页/训练/AI助手/我的)
- [ ] 无回归：TrainingPlanService / TeachingVideoRepository.byId / PoseCoordinateMapper / FollowAlongViewModel.finishReport / ApiKeyStore / Room

## 构建与报告
- [ ] compile + 单测编译 + JVM 全量测试(80+新增) + Debug APK + APK 资源校验
- [ ] V3_2_FINAL_REPORT.md（修改文件/根因/修复/测试/APK路径/真机状态/遗留阻塞）

## 当前动作
✅ V3.2 全部可执行项完成。仅剩真机逐页点击（设备离线 BLOCKED_EXTERNAL，人工项）。

## 最终验证
- :app:compileDebugKotlin / :app:compileDebugUnitTestKotlin 均 BUILD SUCCESSFUL。
- JVM 全量 11 类 **98 项 0 失败**（80 基线 + 18 新增：VideoAnalysisHonest 3 / TeachingVideoMatch 4 / TrainingPlanGenerate 6 / AiServiceSettings 5）。
- Debug APK **86.38MB**（2026-10-13 13:31）：16 教学视频 + pose_landmarker.task(5.51MB) + 3 教练 PNG 全部入包。
- 真机：设备 b7a0af21 离线（adb no devices）→ BLOCKED_EXTERNAL，安装/逐页点击为人工项（MIUI rapid_charge 遮挡 uiautomator）。
- 回归：4 底部导航 / NavigationHost / finishReport / TrainingPlanService.byId / PoseCoordinateMapper / ApiKeyStore / Room 均未改动，无回归。
- 交付报告：V3_2_FINAL_REPORT.md。
