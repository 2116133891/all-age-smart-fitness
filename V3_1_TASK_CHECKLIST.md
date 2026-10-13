# V3.1 任务清单（可恢复检查点）

> 状态图例：`TODO` / `IN_PROGRESS` / `PASS`(有证据) / `FAIL` / `BLOCKED_EXTERNAL`(需外部)。
> 每项 PASS 须填：改了什么文件 + 验证命令 + 结果。

## A. P2 UI 修复
- [x] A1 报告页"去生成计划" → Material Button（防重复 + 导航 + 返回正常）
  - 修改：`YouthBodyTestReportScreen.kt`（Button + planNavigated + FitnessCenter 图标）
  - 验证：`:app:compileDebugKotlin` BUILD SUCCESSFUL
- [x] A2 图标系统收敛
  - 审计：主导航/首页/卡片已 Material 图标一致；emoji 仅作数据驱动内容字形
  - 结论：未盲换图标库（符合约束），记录为设计决策
- [x] A3 设计系统一致性
  - 报告/详情 CTA 统一 Material Button；CoachAvatar 容器比 0.7→1.5；Onboarding 260→200dp
  - 验证：编译绿，80 测试绿
- [x] A4 CoachVisual 容器比例修正（1248×832 横版 1.5:1）+ 各调用点布局
  - 修改：`CoachVisual.kt`（width=size*1.5f）+ `OnboardingScreen.kt`（size=200dp）
  - 验证：编译绿

## B. 数字人终版
- [x] B1 审核 3 PNG 实图（PASS：1248×832，高质量 3D，浅灰底，风格统一，年龄区分）
- [x] B2 角色统一性结论（3 图光照/材质/色彩/比例一致 → 无需重新生成）
- [x] B3 页面集成验证（CoachAvatar 1.5:1 横版容器匹配 PNG；Onboarding 防溢出；编译绿）

## C. 教学视频
- [x] C1 校验 18 条清单（youth10/middle4/senior4；7 可播 + 2 方法说明 + 9 可生成 pending）
- [x] C2 9 pending 视频 AIGC 生成 → assets/videos/（16 mp4）；BLOCKED_EXTERNAL 解除
  - 修改：`TeachingVideoRepository.kt` 回填 9 条 assetPath
  - 验证：ftyp 校验通过；APK 入包 16 mp4 确认
- [x] C3 资源接入 + 播放器（byId 解析/缺资源提示/退出释放）
  - 验证：80 测试绿（PoseAndVideoV3Test 更新 16 可播 / 2 方法说明 / youth 可播 8）
- [ ] C4 质量分级：文件存在✅ / ftyp 格式✅ / 资源路径✅ / 播放(需真机)⏳ / 内容抽帧(需真机)⏳

## D. 青年体测闭环
- [x] D1 逐 7 项 练/测 闭环核验（代码审计）
  - BMI=身高体重双值/肺活量=人工录入/50m=人工计时/坐体前屈+立定跳远=人工距离/
    引体仰卧=按性别人工次数；识别能力诚实分级；备考计划 byId 解析；80 测试固化

## E. 相机/AI/性能
- [x] E1 相机/骨架/坐标映射排查（旋转/upright/mirror/recycle 链；权限/release；PoseAndVideoV3Test 绿）
- [x] E2 AI/Key 安全 + 诚实本地降级（无硬编码 key / 加密存储 / mask / 30s 超时 / 401 / 数据最小化）
- [x] E3 性能/APK 体积分析（视频 14.56MB + model 5.78MB + PNG 1.78MB → APK 87.82MB < 100MB）

## F. 真机 + 交付
- [x] F1 真机安装/启动（Xiaomi MI9/Android9，`install -r` Success，MainActivity 前台无 crash）
  - 局限：MIUI rapid_charge 浮层拦截 uiautomator dump → 逐页点击需人工
- [x] F2 完整 80 JVM 测试 + 重打 APK 87.82MB(19:38) + 资源入包校验（16 mp4 + 3 PNG）
- [x] F3 更新全部文档 + 最终报告（VIDEO_ASSET_MANIFEST / REPORT / PLAN / 两份 V3_1 检查点）

## 遗留（需人工/外部，无法在当前环境执行）
- [ ] C4 视频"动作内容"真机抽帧验收（需人工在 MI9 上播放 16 条视频确认动作与名称匹配）
- [ ] F1-逐页 MIUI 真机操作（跟练计数/相机骨架/前后摄/4 底栏/视频播放逐页点击）
- [ ] API Key 生产级方案（服务端代理，区分开发 vs 部署）
- [ ] MoveNet 切换（不盲换；帧率瓶颈时按 MOVENET_EVALUATION §6 立项）
