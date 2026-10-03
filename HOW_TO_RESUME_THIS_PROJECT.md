# HOW_TO_RESUME_THIS_PROJECT

> 给下一个会话的速查卡。项目：全龄智动 · 智能运动指导 Android MVP
> 更新时间：2026-10-02 · 主程：Agnes-3.0-flash（Claude Code）
> 项目根：`c:\Users\Admin\Desktop\城市大赛项目`

**先读什么**：
1. 本文件（速查卡）
2. `PROJECT_STATUS.md`（完整状态 + 已知问题 + 踩坑记录）
3. `~/.claude/watchdog/USAGE.md`（守护系统全局说明）

---

## 项目是什么

智能运动指导 Android App（Composable 界面 + 摄像头 + MediaPipe 姿态识别），面向三个年龄段（儿童/青年/银龄）提供 9 项运动的教学与跟练闭环：开摄像头 → 实时姿态骨骼 → 关节角度 → 动作阶段 → 实时评分（0–100）→ 纠错建议 → 完成次数 → 训练报告。P0 全完成，可编译可安装。

---

## 当前做到哪里

| 模块 | 状态 |
|------|------|
| 全链路 7 屏 UI（splash→home→age→exercise→coach→followalong→report） | ✅ P0 完成 |
| 数字人（Canvas 抽象人形，非 Lottie/3D） | ✅ P0 完成 |
| 三个年龄段页面 + 内容仓库（各 3 项运动） | ✅ P0 完成 |
| 摄像头 + 姿态识别 + 动作评分 + 纠错（SquatAnalyzer / ArmReachAnalyzer / JumpRopeAnalyzer） | ✅ P0 完成 |
| 训练报告（环形评分 + 建议） | ✅ P0 完成 |
| JVM 单测（评分/纠错 7/7） | ✅ 通过 |
| 全局低 Token 守护（watchdog + error hook + supervisor + Ralph Loop） | ✅ 完成 + 已验收 |
| 跳绳真实计数（当前是"起跳近似"，非真实绳体计数） | ⏳ P1 未做 |
| 八段锦/太极逐式识别 | ⏳ P1 未做 |
| 数字人 Lottie/3D/口型驱动 | ⏳ P1 未做 |
| 历史数据统计/成长曲线 | ⏳ P1 未做 |
| 大模型纠错生成（当前是预设文案） | ⏳ P2 未做 |
| 账号/云同步/后端 | ⏳ P2 未做 |

**当前不应继续开发业务代码**（用户已暂停），Guard 开发已完成验收。

---

## Android 怎么编译

```powershell
$env:JAVA_HOME   = "C:\Users\Admin\.android-build\jdk-17.0.10+7"
$env:ANDROID_HOME= "C:\Users\Admin\.android-build\android-sdk"
cd C:\Users\Admin\Desktop\城市大赛项目
.\gradlew.bat :app:assembleDebug --no-daemon
# 产物：app\build\outputs\apk\debug\app-debug.apk（~67MB）
```

**注意**：
- `--no-daemon` 是必须的（daemon 在中文路径/VS Code 下卡死）
- `gradle.properties` 已设 `android.overridePathCheck=true`（中文路径 AGP 警告）
- `local.properties` 已设 `sdk.dir=C:\Users\Admin\.android-build\android-sdk`
- 编译需 5–15 分钟（首次 Gradle 下载依赖后 ~5 min）

---

## 真机怎么安装

```powershell
& "C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

**前提**：
- 需要 **ARM 真机**（模拟器无摄像头，跟练功能无法验证）
- 手机需开启开发者选项 + USB 调试
- `adb devices` 确认设备已连接

---

## AGNES 怎么配置

AGNES 是一个本地 inference gateway，监听 `127.0.0.1:15723`。

当前 `~/.claude/settings.json` 中的 AGNES 配置：
```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "PROXY_MANAGED",
    "ANTHROPIC_BASE_URL": "http://127.0.0.1:15723",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "claude-opus-4-8[1M]",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "claude-sonnet-4-6[1M]",
    "ANTHROPIC_DEFAULT_FABLE_MODEL": "claude-fable-5[1M]",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "claude-haiku-4-5",
    "CLAUDE_CODE_SUBAGENT_MODEL": "agnes-3.0-flash[1M]"
  }
}
```

**已知问题**：AGNES 间歇性 502 / rate-limit（本轮开发已多次因此中断）。守护系统（watchdog + error hook）设计用于检测并记录这些错误，但**不能保证自动恢复 100% 成功**。

**如何确认 AGNES 在线**：
```powershell
# 检查端口是否在监听
Test-NetConnection 127.0.0.1 -Port 15723 -WarningAction SilentlyContinue | Select-Object TcpTestSucceeded
```

---

## Guard 怎么启动（全局守护系统）

### 组件清单
| 组件 | 位置 | 说明 |
|------|------|------|
| AGNES Resilience Watchdog | `~/.claude/watchdog/watchdog.ps1` | 60s 轮询，独立于 AGNES/Claude，纯 PowerShell |
| AGNES Error Hook | `~/.claude/watchdog/agnes-error-hook.ps1` | 全局 PostToolUseFailure Hook，纯文本扫描 AGNES 错误 |
| Ralph Loop | 官方插件 `ralph-loop@claude-plugins-official` | 长任务循环（需显式调用） |
| Project Supervisor | `~/.claude/agents/project-supervisor.md` | 只读 supervisor（Read/Grep/Glob），按需调用 |

### 启动 watchdog
```powershell
# 后台启动（60s 轮询，默认）
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\start.ps1"

# 指定间隔（如 30s）
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\start.ps1" 30

# 停止
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\stop.ps1"

# 查看当前状态
Get-Content "$env:USERPROFILE\.claude\watchdog\state\agnes-watchdog-state.json" -Raw

# 查看日志
Get-Content "$env:USERPROFILE\.claude\watchdog\logs\watchdog-$(Get-Date -Format 'yyyy-MM-dd').log" -Tail 30
```

### 单次自检（不启动后台，只跑一次）
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\watchdog.ps1" -Once
```

### 模拟一次 AGNES 502 给 hook
```powershell
echo '{"tool_name":"Bash","tool_error":"502 Bad Gateway from AGNES"}' |
  powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\agnes-error-hook.ps1"
# 然后查看 last-action.json 确认识别成功
Get-Content "$env:USERPROFILE\.claude\watchdog\state\last-action.json" -Raw
```

### Supervisor 复核
```
# 让主 Agent 显式调用（不是定时器触发）
# 用法：让 Claude 在任务完成/怀疑卡住时调用 project-supervisor
# 主 Agent 调用示例：
"让 project-supervisor 检查任务进度"
```

---

## Ralph Loop 怎么使用

Ralph Loop 是一个官方 Claude Code 插件，用于长时间持续开发某个明确任务。

```
# 基本用法（在 Claude Code 交互会话中）
/ralph-loop "<明确的下一段任务描述>" --max-iterations 10

# 取消
/cancel-ralph
```

**注意**：
- Ralph Loop **不会默认常驻**，必须显式调用
- 必须带 `--max-iterations`（建议 10~20，不建议超过 30）
- 在 AGNES 不稳定的情况下，设上限 10~15 轮就够
- 插件已安装：`claude plugin install ralph-loop@claude-plugins-official`

---

## 新会话应该先读什么

1. **本文件**（HOW_TO_RESUME_THIS_PROJECT.md）— 速查卡
2. **PROJECT_STATUS.md** — 完整状态 + 已知问题 + 踩坑记录
3. **README.md** — 项目说明（编译/运行/架构）
4. **~/.claude/watchdog/USAGE.md** — 守护系统全局说明
5. **~/.claude/settings.json** — AGNES 配置

**快速启动命令**（按 §4 的步骤复制粘贴即可）

---

## 当前已知问题

| # | 问题 | 影响 | 状态 |
|---|------|------|------|
| 1 | AGNES 间歇 502 / rate-limit | 开发中断，需手动恢复 | 守护系统已部署，但自动恢复未 100% 验证 |
| 2 | 非 ASCII 项目路径 | AGP 警告 | 已用 `android.overridePathCheck=true` 缓解 |
| 3 | `gradle test` 在中文路径下 `ClassNotFoundException` | 无法用 Gradle 跑单测 | 改用手跑 `java -cp` 绕过（7/7 通过） |
| 4 | 跳绳是"起跳近似"，非真实计数 | 演示需诚实标注 | P1 升级接口已留 |
| 5 | 八段锦/太极未做逐式识别 | 通用 ArmReachAnalyzer 代替 | P1 未做 |

**详细记录**：见 `PROJECT_STATUS.md §5`（已知问题）和 `§8.6`（踩坑记录）。

---

## AGNES 自动 resume 哪些已验证、哪些没有验证

### 已验证（通过本次验收）
- [x] **502 检测**：watchdog 能正确识别 `last-action.json` 中 `agnes_upstream=true` 并分类为 `AGNES_UPSTREAM_ERROR`
- [x] **状态分类**：watchdog 能区分 `NORMAL` / `AGNES_UPSTREAM_ERROR` / `GATEWAY_OFFLINE` / `STALL_CONFIRMED` 等 11 种状态
- [x] **退避逻辑**：`consecutive_failures` 计数正确驱动 20s / 60s / 120s 退避（日志中 `Backoff 120 s` 确认触发）
- [x] **Hook 关键词识别**：`agnes-error-hook.ps1` 能识别 502 / 503 / upstream / timeout / connection failed / error sending request / ECONNREFUSED / Bad Gateway / rate.limit 等全部 14 个模式
- [x] **Hook 计数器**：连续 AGNES 错误时 `consecutive_failures` 正确递增；非 AGNES 错误时正确重置为 0
- [x] **Watchdog 启停**：`start.ps1` / `stop.ps1` 正常运作（注意：stop.ps1 只杀它自己派生的子进程，主进程需手动停）

### 未验证（需要真实场景）
- [ ] **自动 `claude --continue` 恢复**：watchdog 的 `Start-Process claude --continue` 在 Claude 真实因 502 退出后能否真的续上 session，**没有现场验证过**
  - 前置条件全满足（Claude 进程已退出 + 网关在线 + session 目录存在 + 5min 防抖窗口过了），但真实触发尚未发生
  - **如果新会话要依赖这个能力，第一次真实触发时务必手动确认**
  - 如果自动 resume 没触发，手动方案：`claude --continue` 或 `claude --resume <sessionId>`

- [ ] **PostToolUseFailure Hook 在真实 Claude Code 会话中触发**：本次验收是模拟喂入（`echo ... | agnes-error-hook.ps1`），不是真实工具失败触发的
  - 真实触发需要一次工具失败（如 Bash 返回 502），然后检查 `last-action.json` 是否被 hook 写入
  - 如果 Hook 没触发，检查 `~/.claude/settings.json` 中 `hooks.PostToolUseFailure` 配置是否正确

### 诚实声明
**自动 resume 功能逻辑上是正确的，但尚未在真实的 AGNES 中断场景下跑通**。本次验收验证了所有前置条件（检测、分类、退避、启停），但没有验证最后的 `Start-Process claude --continue` 执行路径。如果 AGNES 中断发生，第一次自动触发时请人工确认 resume 是否成功。

---

## 文件位置速查

| 用途 | 路径 |
|------|------|
| 项目根 | `c:\Users\Admin\Desktop\城市大赛项目` |
| APK | `app\build\outputs\apk\debug\app-debug.apk` |
| 姿态模型 | `app\src\main\assets\pose_landmarker.task`（5.7MB） |
| 全局守护 | `C:\Users\Admin\.claude\watchdog\` |
| 用户级 Supervisor | `C:\Users\Admin\.claude\agents\project-supervisor.md` |
| 自制监控归档 | `c:\Users\Admin\Desktop\城市大赛项目\backup\custom-guard-backup\` |
| AGNES 配置 | `C:\Users\Admin\.claude\settings.json` |
