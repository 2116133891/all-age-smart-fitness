# V3.4 启动闪退根因修复报告（V3_4_CRASH_FIX_REPORT）

> 全龄智动（com.quannian.zhidong）· v3-dev · 分支提交前基线 89299b6
> 本报告只记录**已实际执行并取证**的内容；未执行项明确标注"未验证"。

---

## 1. 实际根因（有日志证据）

**现象**：V3.3 安装包（commit 89299b6，APK 86.5MB）在真机冷启动时，用户"一点进入就闪退"。

**取证过程**：
1. 设备 b7a0af21（Xiaomi MI9 / Android 9 / API 28 / arm64-v8a）先执行 `pm clear` 清数据，再 `am start -n com.quannian.zhidong/.MainActivity` 冷启动。
2. `logcat -d -s AndroidRuntime:E` 捕获到连续 3 次完全相同的 `FATAL EXCEPTION`：

```
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: Process: com.quannian.zhidong, PID: 11760
E AndroidRuntime: java.lang.IllegalArgumentException: invalid weight 0.0; must be greater than zero
E AndroidRuntime:     at androidx.compose.foundation.layout.ColumnScopeInstance.weight(Column.kt:200)
E AndroidRuntime:     at androidx.compose.foundation.layout.ColumnScope.weight$default(Column.kt:141)
E AndroidRuntime:     at com.quannian.zhidong.ui.screens.OnboardingScreenKt.BoardPageView(OnboardingScreen.kt:191)
E AndroidRuntime:     at ...$OnboardingScreen$1$2.invoke(OnboardingScreen.kt:104)   // HorizontalPager 子页
...
```

完整原始日志：`reports/v3_4_crash_logcat.txt`（含 3 次重复 FATAL + 完整调用栈）。

**根因定位**：
- V3.3 第二轮重做 `OnboardingScreen`（引导页）时，`BoardPageView` 里承载"2~3 条短功能说明"的 `Column` 被误写成：

  ```kotlin
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0f)) { ... }
  ```

- Compose 的 `Modifier.weight()` 要求权重 **严格大于 0**（`weight(0f)` 抛 `IllegalArgumentException: invalid weight 0.0`）。
- 该 Column 位于引导页首帧就要合成的 `HorizontalPager` 子页内（`OnboardingScreen.kt:104`），所以 App 冷启动 → Splash 结束进入引导页 → 首帧布局 measure 时即崩溃。
- 因引导页是全局首屏（`splash -> onboarding`），**任何用户**首次进入都会触发，表现为"一点就闪退"。

> 说明：这个 bug 无法被纯 JVM 单测捕获（单测不执行 Compose 首帧 measure/layout），且 `BUILD SUCCESSFUL`、安装 Success 都不能暴露它——这正是"不能只把编译成功当交付"的实例。

---

## 2. 修复方式（最小、不吞异常、不删功能）

修改文件：`app/src/main/java/com/quannian/zhidong/ui/screens/OnboardingScreen.kt`（约第 191 行）

```kotlin
// 修复前（崩溃源）：
Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0f)) { ... }

// 修复后：该列表用自然高度（wrap content），不占剩余权重
Column(horizontalAlignment = Alignment.CenterHorizontally) { ... }
```

- 功能完全保留：三张引导页、数字人、主标题、简介、功能短句、进度点、CTA 均不变，仅把功能列表的无效 `weight(0f)` 去掉。
- 无空 catch、无吞异常、无条件跳过页面。
- 全仓扫描（`weight(0f)` / `weight(0.0f)` / `weight(0.0)` / `weight(0)`）确认**仅此一处**，无其它同类隐患。

---

## 3. 证据日志

| 证据 | 位置 | 结论 |
|---|---|---|
| 崩溃前 FATAL 栈（3 次重复） | `reports/v3_4_crash_logcat.txt` | 根因 = `OnboardingScreen.kt:191` `weight(0.0)` |
| 修复后 5 次冷启动 | 真机 b7a0af21，`pm clear` 后逐次 `am start` + `logcat -d AndroidRuntime:E` | 每次 **ALIVE**，FATAL 行数 = 0 |
| 修复后 30s 停留 | 同上 | ALIVE，无 Compose/Choreographer/ViewRootImpl 崩溃告警 |
| 修复后完整导航走查 | 引导页→个性化→首页→训练→青少年体测→AI助手→我的→设置→返回，共 9 个过渡 | 全程 ALIVE，整段 session `FATAL EXCEPTION` 计数 = **0** |

---

## 4. 修改文件

| 文件 | 变更 |
|---|---|
| `ui/screens/OnboardingScreen.kt` | 去掉 `BoardPageView` 功能列表 Column 的 `Modifier.weight(0f)`（根因修复） |
| `test/.../V34OnboardingRegressionTest.kt` | 新增 5 个用例（源码级守卫：全仓禁 `weight(0)`；`OnboardingScreen.kt` 专属断言；CoachStyle 默认/非法值；CoachAssetRepository 缺视频诚实返回；AgeGroupConfig 新旧 ageId 归一化） |

---

## 5. 测试结果（均实际执行）

| 项目 | 命令 | 结果 |
|---|---|---|
| Kotlin 编译 | `:app:compileDebugKotlin` | BUILD SUCCESSFUL |
| 单测编译 | `:app:compileDebugUnitTestKotlin` | BUILD SUCCESSFUL |
| JVM 单元测试（保留 117 基线 + 新增 5） | `JUnitCore` 14 个测试类 | **OK (122 tests)** |
| Debug APK | `:app:assembleDebug` | BUILD SUCCESSFUL，`app-debug.apk` 91,577,793 字节（≈91.5MB），2026-10-14 13:37 |
| 真机安装 | `adb install -r -t` | Success |
| 冷启动 5 次 | force-stop + am start + FATAL 计数 | 5/5 ALIVE，0 FATAL |
| 全链路导航走查 | 9 个页面过渡 | 全程 ALIVE，0 FATAL |

> 保留说明：117 项既有 JVM 测试全部保留、未降低断言；新增 5 项后共 122 项。

---

## 6. APK 路径

`app/build/outputs/apk/debug/app-debug.apk`（91.5MB，2026-10-14 13:37 构建）
应用包名 `com.quannian.zhidong`，已安装到真机 b7a0af21。

---

## 7. 真机验证结果

设备：Xiaomi MI9（fingerprint `Xiaomi/cepheus/...:9/PKQ1.181121.001`，Android 9 / API 28，arm64-v8a）

- ✅ 冷启动 5 次：全部进程存活，无 FATAL（**已验证**）
- ✅ 停留 30s：存活，无 Compose 渲染告警（**已验证**）
- ✅ 关键路径 9 段过渡（引导→个性化→首页→训练→青少年体测→AI助手→我的→设置→返回）：全程存活，整段 0 FATAL（**已验证**）
- ✅ API key 泄漏检查：源码/main 资源无 `sk-` 类密钥泄漏（**已验证，clean**）

---

## 8. 剩余阻塞 / 未验证项（诚实标注）

| 项 | 状态 | 说明 |
|---|---|---|
| 逐页"训练/体测报告/跟练/视频分析/训练计划生成"深层交互 | **部分未验证** | 已完成 9 段主要页面过渡且 0 FATAL；深层操作（真实跟练相机 + MediaPipe、视频分析、训练计划生成落库）依赖实时 UI 交互，本次以"页面可达 + 无 FATAL + 编译/单测绿"验证，未逐按钮点完 |
| uiautomator 逐节点 dump | 受限 | MiUI 的 uiautomator 仅返回 systemui 根节点（1 个 node），无法逐控件 tap；已改用确定性坐标 tap + 进程存活 + FATAL 计数完成导航验证 |
| 竖版角色 PNG / 全套图标重绘 | BLOCKED_EXTERNAL | Agnes AIGC 图像生成服务此前 503 不可用，未生成新素材；现沿用既有 coach_* 横版 PNG，代码版式先行 |
| 其它机型（>API28） | 未验证 | 本次仅在 Android 9 / API 28 真机验证；compileSdk 34 与 AndroidX 版本理论兼容更高 API，但未实机回归 |

---

## 9. 高危模式审计（Phase 4，已执行）

- `!!` 非空断言：`YouthTrainingPlanScreen` 的 `planId!!` 均在 `if (planId != null && tasks.isNotEmpty())` 守卫内，安全。
- `first()`：改用 `firstOrNull() ?: ...`；`Profile.kt` `BANDS.first()` 为常量兜底，安全。
- 空 `onClick`：引导页/个性化页所有按钮均绑定真实动作，无空点击。
- `GlobalScope`：无使用。
- 主线程重活：MediaPipe/视频分析走独立管线；启动链无同步重 IO。
- 资源缺失降级：`CoachAssetRepository` / `TeachingVideoScreen` 无视频时返回诚实空态（"无示范视频"），不拿无关素材填空、不崩溃。
