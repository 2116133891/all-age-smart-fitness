# backup/custom-guard-backup — 自制四层监控系统（已停用）

这是 2026-10-01 为「全龄智动」Android 项目**自制**的一套 Claude Code 守护/监控系统。
按用户要求，已整体归档到本目录并**停用**，不再作为活动监控运行。

## 为什么停用
- 后续改用「全局、低 Token、可复用」方案：
  - 现成 Claude Code 插件（Agent Monitor / Ralph Loop）
  - 用户级轻量 Supervisor agent（`%USERPROFILE%\.claude\agents\project-supervisor.md`）
  - 独立 PowerShell AGNES Resilience Watchdog（全局，不写死本项目路径）
- 自制系统绑定了本项目路径、Gradle 命令，无法跨项目复用，故归档保留、不删除。

## 目录说明
```
backup/custom-guard-backup/
├── .claude/
│   ├── agents/supervisor.md         # 自制只读监工 agent（项目级）
│   ├── hooks/heartbeat.ps1          # PostToolUse：记录 Agent 活动心跳
│   ├── hooks/auto-build.ps1         # PostToolUse：.kt/.xml 变更触发防抖后台 Gradle 编译
│   ├── hooks/error-monitor.ps1      # PostToolUseFailure：检测 AGNES 502/网关异常
│   ├── hooks/stop-guard.ps1         # Stop：3-strike 防循环的停止复核
│   ├── watchdog.ps1                 # 独立 60s 轮询 watchdog（心跳/进程/15723/AGNES/build）
│   ├── test-supervisor.ps1          # 模拟 502 / stale / buildfail 场景
│   ├── TASKS.md                     # P0/P1/P2 任务清单
│   ├── settings.json                # 激活 PostToolUse / PostToolUseFailure 的 hooks
│   ├── settings.stop-hook.md        # 可选 Stop hook（被安全分类器拒绝，需手动启用）
│   ├── state/                       # 运行态 JSON（heartbeat/build/agnes/agent/errors）
│   └── logs/                        # watchdog 日志
├── start-watchdog.ps1               # 启动独立 watchdog（写 watchdog.pid）
└── stop-watchdog.ps1                # 停止 watchdog
```

## 状态
- **停用中**。当前项目 `app/` 的 Android 业务代码**未被本归档影响**（未修改任何业务代码）。
- 若要重新启用：把 `backup/custom-guard-backup/.claude/` 拷回项目根目录的 `.claude/`，
  并运行 `start-watchdog.ps1`。
