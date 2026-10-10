package com.quannian.zhidong.repository

import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.model.Exercise

/**
 * 运动内容仓库。第一阶段为本地预设数据，后续可替换为网络 / 数据库。
 * 按年龄段组织，每个 Exercise 携带数字人教练的开场白、步骤、注意事项，
 * 以及对应的姿态分析器 key（analysisKind），用于实时检测路由。
 * 扩展新的运动（跳绳计数、瑜伽、八段锦、太极等）：只需在此新增 Exercise 条目
 * 并在 analyzer/ 目录实现对应规则，无需改动整体架构。
 */
object ExerciseRepository {

    fun all(): List<Exercise> = child() + youth() + middle() + senior()

    fun byAgeGroup(age: AgeGroup): List<Exercise> = when (age) {
        AgeGroup.CHILD -> child()
        AgeGroup.YOUTH -> youth()
        AgeGroup.MIDDLE -> middle()
        AgeGroup.SENIOR -> senior()
    }

    fun find(id: String): Exercise? = all().firstOrNull { it.id == id }

    private fun child(): List<Exercise> = listOf(
        Exercise(
            id = "child-jumprope",
            ageGroup = AgeGroup.CHILD,
            name = "跳绳",
            icon = "🪀",
            tagline = "轻盈起跳 · 协调节奏",
            duration = "5 分钟",
            level = "入门",
            steps = listOf(
                "双脚并拢站直，手腕放松，绳体自然垂于身后",
                "单手摇绳，绳从脚后绕过，同时双脚轻跳",
                "跳起高度以绳体能通过为准，落地轻缓",
                "保持节奏均匀，先求稳再提速"
            ),
            cautions = listOf(
                "选空旷场地，避开障碍物",
                "落地用前脚掌缓冲，避免崴脚",
                "感到不适立即停止休息"
            ),
            coachIntro = "嗨，小运动家！今天我们一起练习跳绳。站稳位置，手腕放松，我们开始啦！",
            analysisKind = "jumprope",
            targetReps = 10,
            scene = "日常"
        ),
        Exercise(
            id = "child-stretch",
            ageGroup = AgeGroup.CHILD,
            name = "基础拉伸",
            icon = "🧘",
            tagline = "舒展身体 · 热身放松",
            duration = "3 分钟",
            level = "入门",
            steps = listOf(
                "双臂向上伸展，抬头吸气",
                "缓慢侧弯，拉伸体侧",
                "弯腰触碰脚尖，保持膝盖微曲"
            ),
            cautions = listOf("动作放慢，不要用力过猛", "出现疼痛立即停止"),
            coachIntro = "准备好了吗？现在我们慢慢拉伸，像小树苗一样伸展身体。",
            analysisKind = "stretch",
            targetReps = 6,
            scene = "日常"
        ),
        Exercise(
            id = "child-jack",
            ageGroup = AgeGroup.CHILD,
            name = "开合跳",
            icon = "🐇",
            tagline = "全身协调 · 提升活力",
            duration = "5 分钟",
            level = "进阶",
            steps = listOf(
                "双脚并拢站立，双臂垂于体侧",
                "开跳时双脚分开，双臂上举",
                "合跳时回到并拢起始位置"
            ),
            cautions = listOf("落地轻柔", "保持核心收紧"),
            coachIntro = "开合跳来啦！像小兔子一样，跳得开心又不累。",
            analysisKind = "jack",
            targetReps = 10,
            scene = "日常"
        )
    )

    private fun youth(): List<Exercise> = listOf(
        Exercise(
            id = "youth-squat",
            ageGroup = AgeGroup.YOUTH,
            name = "基础深蹲",
            icon = "🏋️‍♂️",
            tagline = "下肢力量 · 核心稳定",
            duration = "5 分钟",
            level = "进阶",
            steps = listOf(
                "双脚与肩同宽，脚尖微外展，自然站立",
                "屈膝屈髋下蹲，膝盖朝脚尖方向",
                "蹲至大腿接近水平，保持背部挺直",
                "发力起身回到起始位置"
            ),
            cautions = listOf(
                "膝盖不要超过脚尖太多",
                "保持腰背中立，不要弓背",
                "动作节奏均匀，全程不憋气"
            ),
            coachIntro = "欢迎回到训练！今天练习标准深蹲。请站在摄像头前，保持全身在画面中央，我们开始第一组。",
            analysisKind = "squat",
            targetReps = 10,
            scene = "日常"
        ),
        Exercise(
            id = "youth-shoulder",
            ageGroup = AgeGroup.YOUTH,
            name = "肩颈拉伸",
            icon = "🙆",
            tagline = "缓解疲劳 · 提升体态",
            duration = "3 分钟",
            level = "入门",
            steps = listOf(
                "站直，手臂缓慢上举过头顶",
                "保持手臂伸直，感受肩颈拉伸",
                "缓慢放下，重复"
            ),
            cautions = listOf("动作缓慢", "不要耸肩"),
            coachIntro = "久坐之后肩颈容易僵，现在做一下抬臂拉伸，把状态打开。",
            analysisKind = "shoulder",
            targetReps = 8,
            scene = "上班"
        ),
        Exercise(
            id = "youth-yoga",
            ageGroup = AgeGroup.YOUTH,
            name = "瑜伽",
            icon = "🧎",
            tagline = "柔韧平衡 · 身心放松",
            duration = "10 分钟",
            level = "进阶",
            steps = listOf(
                "从山式站立开始",
                "过渡到 Chair Pose 坐姿",
                "保持呼吸均匀"
            ),
            cautions = listOf("量力而行", "保持呼吸"),
            coachIntro = "今天是一段舒缓的瑜伽练习，跟着节奏慢慢来。",
            analysisKind = "yoga",
            targetReps = 6,
            scene = "日常"
        )
    )

    /** 中年（下班健身 / 肩颈减压 / 久坐恢复）。 */
    private fun middle(): List<Exercise> = listOf(
        Exercise(
            id = "middle-gym",
            ageGroup = AgeGroup.MIDDLE,
            name = "下班力量",
            icon = "🏋️",
            tagline = "燃脂塑力 · 高效 50 分钟",
            duration = "50 分钟",
            level = "进阶",
            steps = listOf(
                "5 分钟动态热身（开合跳/高抬腿）",
                "力量为主：深蹲 + 俯卧撑 + 划船 各 3 组",
                "10 分钟中等强度有氧收尾",
                "心率控制在燃脂区间（最大心率 60-70%）"
            ),
            cautions = listOf(
                "下班先热身再发力",
                "力量组间休息 30-60 秒",
                "有基础病先问医生"
            ),
            coachIntro = "下班黄金时段到！今天做一套高效力量+有氧，50 分钟搞定，不挤占休息。",
            analysisKind = "squat",
            targetReps = 12,
            scene = "下班"
        ),
        Exercise(
            id = "middle-neck",
            ageGroup = AgeGroup.MIDDLE,
            name = "肩颈缓解",
            icon = "💆",
            tagline = "久坐救星 · 工位即做",
            duration = "5 分钟",
            level = "入门",
            steps = listOf(
                "头部缓慢侧倾 + 保持 15 秒，换边",
                "耸肩放松 10 次",
                "后收下巴对位 10 次",
                "肩袖画圈放松"
            ),
            cautions = listOf(
                "动作轻缓，不要甩头",
                "落枕急性期避免大幅度",
                "每天 1 次比一次猛练更护颈"
            ),
            coachIntro = "久坐族肩颈放松！这几个动作在工位就能做，5 分钟把颈肩打开。",
            analysisKind = "shoulder",
            targetReps = 10,
            scene = "上班"
        ),
        Exercise(
            id = "middle-stretch",
            ageGroup = AgeGroup.MIDDLE,
            name = "久坐恢复",
            icon = "🧘",
            tagline = "腰背放松 · 助眠拉伸",
            duration = "8 分钟",
            level = "入门",
            steps = listOf(
                "站立体前屈，放松腰背",
                "坐姿扭转，舒展脊柱",
                "婴儿式下榻，深呼吸 30 秒",
                "睡前 5 分钟助眠拉伸"
            ),
            cautions = listOf(
                "均匀呼吸",
                "不要憋气",
                "有腰椎伤先咨询"
            ),
            coachIntro = "久坐一天后，我们用 8 分钟把腰背和睡眠都照顾到。",
            analysisKind = "easy",
            targetReps = 8,
            scene = "下班"
        )
    )

    private fun senior(): List<Exercise> = listOf(
        Exercise(
            id = "senior-baduanjin",
            ageGroup = AgeGroup.SENIOR,
            name = "八段锦",
            icon = "🍃",
            tagline = "传统功法 · 舒缓康养",
            duration = "8 分钟",
            level = "入门",
            steps = listOf(
                "两脚开立，与肩同宽，含胸拔背站定",
                "双手缓缓上托至头顶（第一式·两手托天理三焦）",
                "缓慢下落，回到起始位置",
                "全程配合呼吸，动作舒缓连贯"
            ),
            cautions = listOf(
                "动作舒缓，避免突然用力",
                "下蹲或上托时保持重心稳定",
                "有基础疾病者量力而行"
            ),
            coachIntro = "您好，今天我们一起练习八段锦第一式。动作不急，跟着我的节奏，舒展身心。",
            analysisKind = "baduanjin",
            targetReps = 8,
            scene = "康复"
        ),
        Exercise(
            id = "senior-taichi",
            ageGroup = AgeGroup.SENIOR,
            name = "太极",
            icon = "☯️",
            tagline = "刚柔并济 · 养心调息",
            duration = "10 分钟",
            level = "入门",
            steps = listOf(
                "松肩沉肘，缓慢起势",
                "重心平稳移动，保持中正",
                "动作连绵不断"
            ),
            cautions = listOf("速度均匀", "重心不要起伏过大"),
            coachIntro = "我们进入太极起势，慢下来，感受身体的呼吸。",
            analysisKind = "taichi",
            targetReps = 8,
            scene = "康复"
        ),
        Exercise(
            id = "senior-easy",
            ageGroup = AgeGroup.SENIOR,
            name = "舒缓拉伸",
            icon = "🌿",
            tagline = "活动关节 · 轻松放松",
            duration = "4 分钟",
            level = "入门",
            steps = listOf(
                "头部缓慢旋转放松",
                "肩部画圈活动",
                "上身缓慢侧弯"
            ),
            cautions = listOf("动作轻缓", "量力而行"),
            coachIntro = "现在做一组简单的拉伸，活动一下关节，舒缓身心。",
            analysisKind = "easy",
            targetReps = 6,
            scene = "日常"
        ),
        Exercise(
            id = "senior-balance",
            ageGroup = AgeGroup.SENIOR,
            name = "平衡防跌",
            icon = "🧘‍♀️",
            tagline = "单腿站 · 稳而不晃",
            duration = "5 分钟",
            level = "入门",
            steps = listOf(
                "扶物单腿站 10 秒，换腿",
                "脚跟脚尖串联走 10 步",
                "闭眼静站 5 秒（可扶物）"
            ),
            cautions = listOf(
                "全程扶物更安全",
                "头晕立即停",
                "每天 1 次预防跌倒"
            ),
            coachIntro = "今天我们练平衡，单腿站 + 串联走，预防跌倒是重中之重。",
            analysisKind = "easy",
            targetReps = 8,
            scene = "康复"
        )
    )
}
