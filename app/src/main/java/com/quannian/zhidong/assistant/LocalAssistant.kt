package com.quannian.zhidong.assistant

import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.model.Profile
import com.quannian.zhidong.model.ProfileStore

/**
 * 离线 AI 小助手（本地规则 + 知识库，可问答，不上网，符合离线原则）。
 *
 *  设计（对标主流运动 App 的"AI 教练/助手"）：
 *   - 关键词命中 → 返回针对性建议（引用用户画像/最近训练数据，比固定文案"更像 AI"）。
 *   - 覆盖：运动入门、肩颈缓解、体测、康复、睡眠、饮食、减脂增肌、心率、手环、纠错等高频问题。
 *   - 无法命中 → 给通用运动建议 + 提示可换种问法。
 *
 *  全程本地、无网络、无 API Key（比赛可离线完整运行）。
 */
object LocalAssistant {

    data class Reply(
        val text: String,
        val quickReplies: List<String> = emptyList()
    )

    /** 常用快捷提问（按年龄段定制）。 */
    fun quickQuestions(ageId: String): List<String> = when (ageId) {
        "child" -> listOf("儿童每天运动多少合适？", "跳绳怎么入门？", "运动前后要注意什么？")
        "middle" -> listOf("下班后健身怎么安排？", "肩颈酸痛怎么缓解？", "久坐族怎么恢复？")
        "senior" -> listOf("老人做什么运动安全？", "怎么预防跌倒？", "膝盖不好怎么练？")
        else -> listOf("体测结果怎么看？", "日常训练计划建议？", "动作不标准怎么纠正？")
    }

    /** 主入口：根据问题 + 用户画像 + 最近训练返回一条 AI 回复。 */
    fun ask(
        question: String,
        profile: Profile,
        recentSessions: List<com.quannian.zhidong.db.TrainingSession> = emptyList()
    ): Reply {
        val q = question.trim()
        val tone = if (profile.gender == "male") "兄弟" else "你"
        val age = ProfileStore.ageLabel(profile.ageId)

        // 个性化开场（引用画像 + 数据，体现"懂你"）
        val head = buildString {
            append("好的，$tone 问到「$q」。")
            if (recentSessions.isNotEmpty()) {
                val last = recentSessions.first()
                append("（你上次「${last.exerciseName}」得了 ${last.score} 分，")
                append("我结合你的${age}画像给建议）")
            } else {
                append("（结合你的${age}画像给建议）")
            }
        }

        val body = matchBody(q, profile) ?: genericAdvice(q, profile)
        val quicks = quickQuestions(profile.ageId)
        return Reply(head + "  " + body, quicks)
    }

    private fun genericAdvice(q: String, profile: Profile): String {
        val base = when (ProfileStore.ageLabel(profile.ageId)) {
            "中年" -> "上班族运动建议每周 ≥150 分钟中等强度有氧，配合 2 次力量训练；"
            "银龄" -> "长者运动重在稳与柔，先低强度热身，避免急停与高冲击；"
            "儿童" -> "儿童每天至少 60 分钟中等~高强度活动，游戏化更持久；"
            else -> "建议每周 3~5 次、每次 30 分钟左右的全身性运动；"
        }
        return "$base 先从基础动作练起、注意呼吸均匀，有不适随时停下。"
    }

    private fun matchBody(q: String, profile: Profile): String? {
        fun has(vararg kws: String) = kws.any { it.isNotEmpty() && q.contains(it) }

        return when {
            has("肩颈", "颈椎", "脖子", "脖颈", "落枕") ->
                "久坐肩颈疲劳，建议每天做 3 组：① 头部缓慢侧倾 + 保持 15 秒换边；② 耸肩放松 10 次；③ 后收下巴对位 10 次。工位每 40 分钟起身活动 2 分钟，比一次猛练更护颈。"

            has("下班", "上班", "久坐", "办公室", "通勤") ->
                "下班健身黄金窗口（18-20 点，皮质醇自然下降、体温偏高更利于发力）：先 5 分钟动态热身 → 30 分钟「力量+有氧」交替（深蹲/划船/开合跳为主）→ 10 分钟肩颈拉伸收尾。控制在 50 分钟内，既不挤占休息又高效。"

            has("体测", "体能测试", "测一下", "评估", "评分") ->
                "体测建议走「练→测」里的体测模块：会评估动作幅度、躯干稳定、左右对称、完成度四维并给 0-100 分。想提升就针对最低那一维做专项。"

            has("康复", "膝盖", "关节", "跌", "平衡") ->
                "康复向优先「稳」：开合/侧移类小幅度起步，重心低、动作慢；深蹲时膝盖务必朝脚尖、别内扣；单腿站 30 秒练平衡防跌倒。基础病先咨询医生再练。"

            has("睡", "睡眠", "失眠") ->
                "运动助眠：下午 4-6 点做一次中等强度运动效果最好；睡前 2 小时避免剧烈训练，可改做 5 分钟舒缓拉伸 + 深呼吸。"

            has("吃", "饮食", "蛋白质", "减脂", "增肌", "瘦") ->
                "减脂看热量缺口（吃<动）、增肌看蛋白质+力量训练双在线。运动后 30 分钟内补蛋白质更利于恢复；水别等渴了才喝。"

            has("心率", "心跳", "bpm", "手环") ->
                "运动心率区间：燃脂 60-70% 最大心率、心肺 70-85%。最大心率≈(220-年龄)，手环能实时监测帮你控强度。"

            has("深蹲", "下蹲", "蹲") ->
                "标准深蹲：双脚与肩同宽、脚尖微外展；下蹲像「往后坐椅子」，膝盖朝脚尖、背部中立；到大腿近水平再起身。先做 3 组×10。"

            has("跳", "跳绳", "开合") ->
                "跳绳/开合跳：前脚掌落地缓冲、落地轻，手腕摇绳而非甩大臂；保持节奏均匀。有膝伤就改低冲击版本。"

            has("纠正", "不标准", "姿势", "规范", "错误") ->
                "动作不标准别硬冲数量：用「AI 动作诊断」上传一段视频，系统会按主错误类型给针对性纠正，再进教学页重练，前后可对比进步。"

            has("入门", "新手", "第一次", "开始") ->
                "新手第 1 周以「低强度·短时长·勤热身」为主：每天 20-30 分钟、动作做标准优先于次数，别一上来就猛练。"

            has("八段", "太极", "功法", "养生") ->
                "八段锦/太极适合银龄与久坐人群：动作慢、重心稳、配合呼吸，每天 1 套（10-15 分钟）就能改善气血循环。"

            has("瑜伽", "拉伸", "柔韧") ->
                "瑜伽/拉伸改善柔韧：保持每个式子 20-30 秒、均匀呼吸；力量型人群建议先热身后拉，避免拉伤。"

            has("计划", "安排", "每周", "作息") ->
                "一周模板：周一/三/五 力量（深蹲·划船·开合跳），周二/四 有氧 30 分钟，周五肩颈拉伸放松，周末 1 次长活动（快走/太极）。"

            else -> null
        }
    }

    /** 根据年龄段推荐该练什么（用于"练什么"这类问题）。 */
    fun recommendByAge(age: AgeGroup): String = when (age) {
        AgeGroup.CHILD -> "儿童推荐：跳绳、开合跳、基础拉伸，游戏化更能坚持。"
        AgeGroup.YOUTH -> "青年推荐：深蹲、肩颈拉伸、瑜伽/力量训练，兼顾体态与减脂。"
        AgeGroup.MIDDLE -> "中年推荐：下班力量健身 + 肩颈缓解，改善久坐代谢。"
        AgeGroup.SENIOR -> "银龄推荐：八段锦、太极、舒缓拉伸，重在稳与柔、防跌倒。"
    }
}
