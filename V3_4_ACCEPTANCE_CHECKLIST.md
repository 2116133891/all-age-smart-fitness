# V3_4_ACCEPTANCE_CHECKLIST

> 只列"已实际执行"的结果；未执行的显式标注 **未验证**，不冒充通过。
> 设备：b7a0af21 · Xiaomi MI9 · Android 9 / API 28 · arm64-v8a

## A. 启动稳定性（核心验收）
| # | 检查项 | 方法 | 结果 |
|---|---|---|---|
| A1 | 冷启动不闪退 | `pm clear` + `am start` + logcat FATAL 计数 × 5 | ✅ 5/5 ALIVE，FATAL=0 |
| A2 | 无 `invalid weight` 崩溃 | logcat `AndroidRuntime:E` 全段 grep | ✅ 0 条 |
| A3 | 引导页正常渲染 | 观察"跳过/科学训练/下一步·1/3"文案 | ✅ 正常 |
| A4 | 停留 30s 无渲染告警 | Compose/Choreographer/ViewRootImpl 日志 | ✅ 无 |

## B. 全链路可达（页面过渡）
| # | 路径 | 结果 |
|---|---|---|
| B1 | 引导页 → 个性化（跳过） | ✅ ALIVE 0 FATAL |
| B2 | 个性化 → 首页（保存并进入） | ✅ |
| B3 | 首页 → 训练 tab | ✅ |
| B4 | 训练 → 青少年体测 | ✅ |
| B5 | 青少年体测 → 返回 | ✅ |
| B6 | 首页 → AI 助手 tab | ✅ |
| B7 | 首页 → 我的 tab | ✅ |
| B8 | 我的 → 设置 | ✅ |
| B9 | 设置 → 返回 | ✅ |
| B10 | 整段 session FATAL 计数 | ✅ = 0 |

## C. 构建 / 测试
| # | 项 | 结果 |
|---|---|---|
| C1 | `:app:compileDebugKotlin` | ✅ BUILD SUCCESSFUL |
| C2 | `:app:compileDebugUnitTestKotlin` | ✅ BUILD SUCCESSFUL |
| C3 | JVM 单元测试 | ✅ OK (122 tests)（保留 117 基线 + 新增 5） |
| C4 | `:app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| C5 | APK | ✅ 91,577,793 字节（≈91.5MB） |
| C6 | 真机安装 | ✅ Success |
| C7 | API key 泄漏检查 | ✅ clean（无 sk- 密钥） |

## D. 高危模式审计
| # | 项 | 结果 |
|---|---|---|
| D1 | `weight(0)` 全仓扫描 | ✅ 已清除，仅 1 处已修复 |
| D2 | `!!` 裸奔 | ✅ 均受 if 守卫 |
| D3 | `first()` 空集合 | ✅ 用 firstOrNull 兜底 |
| D4 | GlobalScope / 空 catch / 空按钮 | ✅ 无 |
| D5 | 资源缺失诚实降级 | ✅ 无视频→空态提示，不崩溃 |

## E. 未验证 / 外部阻塞（诚实标注）
| # | 项 | 状态 |
|---|---|---|
| E1 | 深层交互（跟练相机+MediaPipe、青少年体测进入、训练计划生成落库） | ✅ 已验证（坐标 tap 逐段触发，全程 ALIVE，整段 session FATAL=0，crash buffer 空） |
| E2 | 其它机型 / 更高 API（>28） | **未验证**（仅 API 28 实机回归） |
| E3 | 竖版角色 PNG / 全套图标重绘 | BLOCKED_EXTERNAL（AIGC 服务 503，沿用既有素材） |
