package com.quannian.zhidong.service

import com.quannian.zhidong.db.TrainingPlanTask
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.repository.TeachingVideoRepository
import java.util.UUID

/**
 * V2 体测备考训练计划生成器（任务 6）。
 *
 *  原则：
 *   - 计划 由 **数据 模型 + 规则** 生成（**不 用 大模型 自由 文本**，任务 6 明确 要求）。
 *   - 输入：已 录入 的 薄弱 项 + 备考 周期（2/4/8 周）+ 每周 训练 天数。
 *   - 输出：每日 训练 任务 列表（每个 任务 关联 教学 视频 / 训练 步骤 / 注意 事项）。
 *   - 安全 边界：每个 任务 固定 携带 "不适 即 停" 提示（[SAFETY_BOUNDARY]）；
 *     训练 建议 保守 循序渐进，**不 保证 一定 达到 某 体测 分数**。
 *
 *  生成 策略（保守 渐进）：
 *   - 按 周期 周数 × 每周 天数 展开 为 每日 任务 槽位。
 *   - 薄弱 项 优先 排 前；无 薄弱 项 时 用 七 项 轮换。
 *   - 每 项 任务 分配 对应 教学 视频（有 动作 的 项）或 方法 说明（BMI/肺活量）。
 *   - 训练 强度 按 周 渐进（第 1 周 低，末 周 高），体现 循序渐进。
 */
object TrainingPlanService {

    /** 固定 安全 边界 文案（每个 任务 都 携带，满足 任务 6 "身体不适/疼痛/异常 呼吸 即 停"）。 */
    const val SAFETY_BOUNDARY = "训练中 如 出现 明显 疼痛、异常 呼吸 困难 或 头晕，立即 停止 训练 并 寻求 医护 帮助；本 计划 不 保证 一定 达到 某 体测 分数。"

    data class PlanParams(
        val weakItems: List<String>,      // 已 识别 的 薄弱 项 key
        val weeks: Int,                   // 备考 周期（2/4/8）
        val daysPerWeek: Int,             // 每周 训练 天数
        val profileId: String = "local"
    )

    /** 薄弱 项 为 空 时 的 默认 轮换 顺序（七 项）。 */
    private val DEFAULT_ORDER = listOf(
        BodyTestItemKey.RUN_50M, BodyTestItemKey.LONG_JUMP,
        BodyTestItemKey.SEATED_REACH, BodyTestItemKey.PULLUP_SITUP,
        BodyTestItemKey.RUN_800_1000, BodyTestItemKey.BMI, BodyTestItemKey.VITAL_CAPACITY
    )

    /** 每个 训练 项目 的 保守 每日 训练 步骤 + 对应 视频 id。 */
    private data class ItemTemplate(
        val itemKey: String,
        val videoId: String,
        val title: String,
        val steps: String,
        val baseMinutes: Int
    )

    private val TEMPLATES: Map<String, ItemTemplate> = mapOf(
        BodyTestItemKey.RUN_50M to ItemTemplate(
            "run_50m", "run_50m", "50 米 跑 专项",
            "热身 慢跑 5min → 起跑 练习 6×30m → 加速 段 4×20m → 冲刺 4×10m → 整理 拉伸",
            30),
        BodyTestItemKey.LONG_JUMP to ItemTemplate(
            "long_jump", "long_jump", "立定 跳远 专项",
            "原地 跳 10× → 纵跳 触 高 10× → 预摆 跳远 6 次 → 连续 跳远 3 组 → 整理",
            25),
        BodyTestItemKey.SEATED_REACH to ItemTemplate(
            "seated_reach", "forward_reach", "坐位 体前屈 柔韧",
            "动态 热身 → 腘绳 肌 静 拉伸 4×30s → 坐位 前伸 6 次 → 髋 屈 拉伸 4×30s",
            20),
        BodyTestItemKey.PULLUP_SITUP to ItemTemplate(
            "pullup_situp", "pullup", "力量 专项（引体/仰卧）",
            "引体 辅助 组 4× 或 仰卧起坐 3×30 → 核心 平板 支撑 3×30s → 背部 激活",
            25),
        BodyTestItemKey.RUN_800_1000 to ItemTemplate(
            "run_800_1000", "endurance", "耐力 配速 跑",
            "热身 8min → 匀速 有氧 跑 15min → 间歇 4×200m → 慢跑 整理 5min",
            40),
        BodyTestItemKey.BMI to ItemTemplate(
            "bmi", "bmi_method", "体重 管理",
            "记录 身高 体重 → 有氧 30min（快走/慢跑）→ 控制 碳水 摄入；每周 复测 体重",
            35),
        BodyTestItemKey.VITAL_CAPACITY to ItemTemplate(
            "vital_capacity", "vital_capacity_method", "肺活量 提升",
            "腹式 呼吸 练习 5min → 有氧 跑 20min（提升 心肺）→ 腹 部 肌 训练",
            30)
    )

    /** 生成 计划。返回 全部 每日 任务（扁平 列表，按 dayIndex 升序）。 */
    fun generate(params: PlanParams): List<TrainingPlanTask> {
        val order = if (params.weakItems.isNotEmpty()) {
            // 薄弱 项 优先，其余 补 满 轮换
            val rest = DEFAULT_ORDER.filter { it !in params.weakItems }
            params.weakItems + rest
        } else DEFAULT_ORDER

        val planId = UUID.randomUUID().toString()
        val totalDays = params.weeks * 7
        val tasks = mutableListOf<TrainingPlanTask>()

        for (day in 0 until totalDays) {
            val week = day / 7
            // 该 周 内 已 排 的 训练 日 数（决定 今天 是否 训练 日）
            val daysInThisWeekSoFar = day % 7
            if (daysInThisWeekSoFar >= params.daysPerWeek.coerceIn(1, 7)) {
                continue // 该 天 不 训练（休息 日 不 生成 任务）
            }
            // 渐进 强度：第 1 周 ×0.6，中期 ×0.8，末 周 ×1.0
            val loadFactor = when {
                week == 0 -> 0.6f
                week >= params.weeks - 1 -> 1.0f
                else -> 0.8f
            }
            val item = order[day % order.size]
            val tpl = TEMPLATES[item] ?: TEMPLATES[DEFAULT_ORDER[day % DEFAULT_ORDER.size]]!!
            val minutes = (tpl.baseMinutes * loadFactor).toInt().coerceAtLeast(10)
            // P1 修复（2026-10-12）：tpl.videoId 是**视频 id**（BY_ID 键），而 videoFor() 期望
            // **体测 item key**（ITEM_TO_PRIMARY 键），二者命名空间不同。旧代码用 videoFor(tpl.videoId)
            // 会把 forward_reach/pullup/endurance 等视频 id 误当 item key 查不到 → 返回 null →
            // 坐位体前屈 / 引体·仰卧 / 800-1000m 三项的"▶ 播教学视频"按钮静默失效。
            // 改为按视频 id 直接取 byId()（7 个 tpl.videoId 均为合法 BY_ID 键；方法说明型
            // bmi_method / vital_capacity_method 的 assetPath 为空 → videoAsset="" 保持设计内）。
            val videoAsset = TeachingVideoRepository.byId(tpl.videoId)?.assetPath ?: ""
            tasks.add(
                TrainingPlanTask(
                    planId = planId,
                    dayIndex = day,
                    itemKey = item,
                    weakItemKey = if (item in params.weakItems) item else "",
                    title = tpl.title,
                    steps = tpl.steps,
                    videoAsset = videoAsset,
                    durationMin = minutes,
                    caution = SAFETY_BOUNDARY,
                    status = "pending"
                )
            )
        }
        return tasks
    }

    /** 校验 参数（任务 6 校验 逻辑）。 */
    fun validate(weeks: Int, daysPerWeek: Int): String? {
        if (weeks !in intArrayOf(2, 4, 8)) return "备考 周期 仅 支持 2/4/8 周"
        if (daysPerWeek !in 1..7) return "每周 训练 天数 需 在 1-7 天"
        return null
    }

    /** 阶段 报告：任务 完成率（done/total）。 */
    fun completionReport(tasks: List<TrainingPlanTask>): Map<String, Any> {
        val total = tasks.size
        val done = tasks.count { it.status == "done" }
        val skipped = tasks.count { it.status == "skipped" }
        return mapOf(
            "total" to total,
            "done" to done,
            "skipped" to skipped,
            "rate" to (if (total > 0) done.toDouble() / total else 0.0)
        )
    }
}
