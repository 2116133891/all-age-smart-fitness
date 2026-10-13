# V3.2 紧急修复冲刺 · 最终报告

> 原则：对真实页面/源码重新核验并直接改源码、资源、页面；连续执行；不伪造结果；外部阻塞标记后继续。

## 结论速览
- **P0-1 视频动作分析**：定位 4 项根因并全部修复（不再"卡在等待分析/0帧"）。
- **P0-2 中年/银龄数字人**：教学舞台火柴人 → 匹配动作真视频（不 generic substitution）。
- **P0-3 备考训练计划**：修复"蓝上蓝不可见"、周期/天数选择、生成态、真实 planId、任务卡可操作、method-only 诚实指引。
- **P0-4 个性化 + AI 服务设置**：信息分层、真实 HTTP 连接测试、端点归一化、Key 加密存储 + 脱敏。
- **P1 回归**：4 底部导航 / finishReport / byId / PoseCoordinateMapper / ApiKeyStore / Room 均无回归。
- **验证**：`compileDebugKotlin` 绿；`compileDebugUnitTestKotlin` 绿；JVM 全量 **98 项绿**（80 基线 + 18 新增）；Debug APK **86.38MB**，资源入包校验通过。
- **真机**：设备 b7a0af21 曾离线 → 重连后已安装成功并启动无崩溃（2026-10-13）；逐页点击仍为人工项（MIUI rapid_charge 遮挡 uiautomator）。

---

## 修改文件清单

| 模块 | 文件 | 变更 |
|---|---|---|
| P0-1 | `video/VideoFrameReader.kt` | `extractFrames` 增 `isCancelled` + `onProgress(done,total,personFrames)`（识别人体帧=覆盖率） |
| P0-1 | `ui/screens/VideoAnalysisScreen.kt` | 重写：实时进度/已处理帧/覆盖率/阶段/评分；取消 + finally 释放 detector；NO_PERSON/ERROR 诚实分支；IO 线程防 ANR |
| P0-1 | `video/VideoAnalysisEngine.kt` | no-person（无有效人体帧）时 avgPosture/avgSym 默认 0（非 1f）→ 综合评分=0，不伪造 |
| P0-1 | `test/VideoAnalysisHonestTest.kt` | 新增 3 项（无人=0次诚实frameCount / 混合帧容忍 / 无人低分不伪造） |
| P0-2 | `repository/TeachingVideoRepository.kt` | 新增 `EXERCISE_TEACHING_VIDEO` 严格匹配表 + `teachingVideoFor(exerciseId)`（无专属视频返回 null） |
| P0-2 | `ui/screens/CoachScreen.kt` | 教学舞台 火柴人→`TeachingVideoPlayer` 匹配真视频（loop）；无匹配走诚实"暂无示范视频"卡片 |
| P0-2 | `ui/components/TeachingVideoPlayer.kt` | 增 `loop`（教学循环播放）+ 容忍空 title |
| P0-2 | `test/TeachingVideoMatchTest.kt` | 新增 4 项（严格匹配 / 无专属视频=null / 匹配资产非空） |
| P0-3 | `ui/screens/YouthTrainingPlanScreen.kt` | 重写：选中蓝底白字（修蓝上蓝）；周期/天数真实单选；生成真实状态+防双击；采用**服务真实 planId**（旧自建 pid→任务恒空）；任务卡可操作；method-only 显示"测量方法"不假放视频 |
| P0-3 | `test/TrainingPlanGenerateTest.kt` | 新增 6 项（一致 planId / 周×天数展开 / byId 视频解析 / method-only 空资产 / 薄弱项优先 / 参数校验） |
| P0-4 | `coach/AiFacade.kt` | 新增 `resolveChatEndpoint`（base/完整 URL 归一，避免重复 /chat/completions）+ `testConnection`（真实 HTTP，中文可行动结果，脱敏不回显 Key）；`remoteAnswer` 用归一端点 |
| P0-4 | `ui/screens/SettingsScreen.kt` | AI 面板信息分层 + "测试连接"按钮 + 诚实结果；端点 base/完整提示；Key 脱敏（`ApiKeyStore` 加密存储） |
| P0-4 | `test/AiServiceSettingsTest.kt` | 新增 5 项（端点归一化 / 缺 Key/端点诚实失败 / mask 不回显完整 Key） |
| 检查点 | `V3_2_EXECUTION_STATE.md` | 全程执行状态记录 |

---

## 根因 → 修复

### P0-1 视频动作分析（"正在分析但卡在等待分析/0 帧"）
根因（实读 VideoAnalysisScreen/VideoFrameReader/PoseDetector/Channel/Pick/NavHost）：
1. `currentPhase` 全程保持默认"等待分析"，从未随抽帧/评分更新 → 卡片恒显示"阶段：等待分析 · 次数：0"。
2. `doneFrames/totalFrames` 已算出但**从不在 UI 显示**（"已处理"标签旁没有数字）→ 观感=0 帧。
3. 抽帧最多 720 帧 MediaPipe 同步推理，无实时进度读、无取消、无 finally 释放 detector → 长时间卡住。
4. 无论 0 帧/无人，一律走"分析完成"并跳报告页 → 伪造结果。

修复：
- 线程安全实时进度流：`onProgress(done,total,personFrames)` → UI 实时刷新"已处理 N/M 帧 + 覆盖率 + 人体帧 + 综合评分(仅DONE)"；`currentPhase` 随 提取→识别→评分→完成 真实切换。
- 取消/资源释放：`AtomicBoolean` 早退 + 协程 `cancel` + `DisposableEffect(onDispose)` + `finally detector.close()`。
- 诚实结果：无人 → `NO_PERSON` 指引"站到画面中央"，不跳伪造报告；视频不可读/空 → `ERROR` 可重试/重选。
- 引擎层：no-person（无有效人体帧）综合评分=0（原来默认 posture/symmetry=1f 会伪造 50 分）。
- 启动改到 `Dispatchers.IO`，避免主线程 ANR；content:// URI 经 `MediaMetadataRetriever.setDataSource` 读取（保留 OpenDocument 权限）；旋转说明：抽帧 720p 对称缩放 + MMR 解码帧。

### P0-2 中年/银龄数字人（"仍显示旧火柴人"）
根因：数字人教学页 `CoachScreen`（`coach/{id}`）教学舞台用 `CoachSpriteView`→`CoachMotionView`（Compose Canvas 关节火柴人）；报告说"教练 PNG 已接入"但教学主体仍是火柴人。
修复：
- `TeachingVideoRepository.teachingVideoFor(exerciseId)` 严格匹配：八段锦→`senior_baduanjin`、太极→`senior_taichi`、银龄舒缓→`senior_gentle_stretch`、平衡→`senior_balance`、肩颈→`middle_neck_shoulder`、久坐→`middle_flexibility`、下班力量→`middle_cardio`；**无专属视频的动作（深蹲/引体等）返回 null，绝不塞泛化视频冒充**。
- `CoachScreen` 教学舞台改为 `TeachingVideoPlayer`（匹配真视频，循环播放）；无匹配视频 → 诚实"暂无该动作示范视频"卡片（示要件 + 安全提示）；顶部保留小 `CoachAvatar` 做身份识别。
- 说明：`FollowAlongScreen` 的 `CoachSpriteView` 是"实时跟练"（livePose 驱动，非教学火柴人），保留；`ExerciseScreen`/`AgeHealthScreen` 仅用静止头像，非教学主体。

### P0-3 备考训练计划（"蓝色控件无可见文字 / 周期·天数选择不清 / 生成态不清"）
根因：
1. `OptionChip` 选中态与生成按钮均用 `Palette.accent`（蓝 0xFF2F7BE8）**既当背景又当文字** → 蓝上蓝，字不可见。
2. 生成按钮无加载/防双击态；参数选择无可视反馈。
3. UI 自建 `pid`（UUID），但 `TrainingPlanService.generate()` 生成**另一个** UUID planId → `repo.planTasks(pid)` 恒空 → 任务列表/报告永不显示（"生成无反应"）。

修复：
- 选中态 = 蓝底**白字**，未选 = 浅底深字 + 描边；所有蓝色控件文字可见可点。
- 周期 2/4/8、天数 1–7 真实单选高亮；生成按钮三态（生成中… / 生成 / 重新生成）+ 禁用防双击 + 参数非法中文提示。
- 生成改为采用**服务返回的真实 planId**（`newTasks.first().planId`）再 `repo.planTasks(newPlanId)` → 任务/阶段报告真实显示。
- 任务卡可操作（标记完成/跳过/重排，持久化 Room）；视频按 `byId` 解析，method-only（BMI/肺活量）显示"📋 测量方法"，不假放视频。

### P0-4 个性化设置 + AI 服务设置（"结构不清 / 仅 URL 字符串校验 / 无真实连接测试 / Key 安全"）
根因/现状：
- `ProfileScreen` 已统一 年龄(青年/中年/银龄) 性别(男/女) 与 `ProfileStore` 真实持久化（无缺陷，保留）。
- AI 面板原**无任何连接测试**，端点 `URL(endpoint)` 原样使用 → 只填 base（`https://api.openai.com/v1`）时 404，只有用户填完整 `.../chat/completions` 才通（"重复路径"隐患）。
- Key 已用 `ApiKeyStore`（`EncryptedSharedPreferences` + Android Keystore）加密存储 + `mask` 脱敏（无缺陷，保留）。

修复：
- `AiFacade.resolveChatEndpoint`：base 补全 `/chat/completions`，已带则不重复 → `remoteAnswer` 与 `testConnection` 同口径。
- `AiFacade.testConnection`：真实发一次最小 chat 请求（`max_tokens=1`），返回中文可行动结果（成功 / Key 无效 / 超时 / 网络不可用 / 路径 404 / 模型 400）；缺失配置直接诚实失败，**不回显完整 Key**。
- `SettingsScreen` AI 面板信息分层（服务商→端点→模型→Key 脱敏→保存+测试连接）；"测试连接"真实 HTTP + 诚实结果卡；端点 base/完整提示。
- 同步：`AssistantScreen` 经 `ApiKeyStore.loadKey/loadConfig` + `AiFacade.ask`，端点归一化对实时问答同样生效。

---

## 测试
- 编译：`:app:compileDebugKotlin`、`:app:compileDebugUnitTestKotlin` 均 **BUILD SUCCESSFUL**。
- JVM 全量（`java -cp` 直跑，含 Compose runtime classes.jar）：**11 类 98 项，0 失败**
  - 基线 80：ExerciseAnalyzer(18) / ScoreEngine(7) / OneEuroFilter(4) / VideoAnalysisEngine(6) / CoachMotionEngine(9) / V2Logic(19) / PoseAndVideoV3(17)。
  - 新增 18：VideoAnalysisHonest(3) / TeachingVideoMatch(4) / TrainingPlanGenerate(6) / AiServiceSettings(5)。
- 资源入包校验（APK 内 zip 列表）：16 条教学视频 mp4 + `pose_landmarker.task`(5.51MB) + 3 教练 PNG 全部在包内。

## APK
- 路径：`C:\Users\23163\Desktop\城市大赛项目\app\build\outputs\apk\debug\app-debug.apk`
- 体积：**86.38 MB**（2026-10-13 13:31 构建）。

## 真机状态（已安装 ✅，逐页点击仍为人工项）
- 目标设备：Xiaomi MI 9 / Android 9（adb serial `b7a0af21`）。
- 设备曾离线（`adb no devices` / `offline`）→ 经 `adb restart-server` + `adb reconnect offline` 触发重握手后转在线。
- 安装：`adb -s b7a0af21 install -r -t app-debug.apk` → **Success**（流式安装首次 Broken pipe(32)，非流式重试成功）。
- 启动核验：`am start -n com.quannian.zhidong/.MainActivity` → `ps` 确认进程存活（PID 6570）；logcat 60s 内无 `AndroidRuntime` FATAL / quannian 崩溃记录 → **无崩溃**。
- 已知限制（不变）：MIUI rapid_charge 充电浮层遮挡 uiautomator 页面 dump，逐页点击需人工：首页/训练/AI助手/我的 → 教学页看真视频 → 视频分析页看真实进度 → 备考计划看可点选与生成 → 设置页测试连接。
- 回归风险低：4 底部导航与 `NavigationHost` 未改；`finishReport`/`byId`/`PoseCoordinateMapper`/`ApiKeyStore`/Room 均未改动；JVM 全绿。

## GitHub 推送（✅）
- remote 已预配置：`origin → https://github.com/2116133891/all-age-smart-fitness.git`；分支 `v3-dev` 推送成功（45 files changed, +2850/−255），GitHub 提示可建 PR：`/pull/new/v3-dev`。
- .gitignore 已补 `cp_p01.txt`；APK 与 build 产物排除在外，未入仓库。

## 遗留阻塞 / 后续
- 真机逐页点击验收（设备在线后）——人工。
- AI 连接测试的"成功"分支需**有真实可用的 API Key + 可路由网络**才能端到端验证；当前单测覆盖"缺 Key/缺端点诚实失败"与"脱敏不回显"，真实 2xx 路径为运行期外部依赖。
- 若后续需要 3D 连续数字人动画（非火柴人、非视频），可再引入帧序列/Sprite 视频源；本回合已用"匹配动作真视频"满足 P0-2 且不做泛化替代。
