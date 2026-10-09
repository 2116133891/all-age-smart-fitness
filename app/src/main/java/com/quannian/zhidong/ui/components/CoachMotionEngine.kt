package com.quannian.zhidong.ui.components

/** 教练状态。 */
enum class CoachState { IDLE, INTRO, PREPARE, DEMO, COUNTDOWN, GOOD, CORRECT, REST, FINISH }

/**
 * 数字人动作系统的纯数据模型 + 引擎（无 Compose 依赖，可单测）。
 *
 *  设计：[CoachMotionEngine]（纯 JVM）根据 (运动, 阶段, 教练状态, 年龄) 计算出一帧
 *  人形的关节参数 [CoachPose]，再由 [CoachMotionView]（Compose Canvas）把关节参数
 *  画成一个分层 2D 骨骼人形。这样**不同动作会有肉眼可见的差异**（深蹲屈膝、侧拉伸侧倾、
 *  开合跳分腿等），而不是整张 PNG 上下跳动。
 *
 *  角度约定（0 = 下垂/中立，正值朝某一侧）：
 *  - shoulderRaise: 上臂相对躯干上举角度（°），0=垂于体侧，180=高举过头
 *  - elbowBend: 前臂相对上臂弯曲（°）
 *  - hipBend: 髋屈曲（°），0=腿伸直，90=大腿与小腿近垂直
 *  - kneeBend: 膝屈曲（°），0=腿伸直
 *  - spineLean: 躯干相对竖直的前倾/侧倾（°），带符号（正=向左）
 *  - headTilt: 头相对肩的侧倾（°），带符号（正=向左）
 *  - legSpread: 双脚分开的归一化值（0=并拢，1=全开）
 *  - bodyLift: 身体离地量（0..1），跳绳/开合跳腾空用
 *  - weightShift: 重心左右偏移（-1..1）
 */

/** 一帧教练人形的关节配置（纯数据，引擎输出）。 */
data class CoachPose(
    /** 上臂上举角（°，0=体侧，180=过头顶），左右共用。 */
    val shoulderRaise: Float = 0f,
    /** 前臂弯曲（°，0=伸直）。 */
    val elbowBend: Float = 0f,
    /** 上臂向身体两侧外展（°，0=贴体，90=水平张开），开合跳"开"用。 */
    val armSpread: Float = 0f,
    /** 髋屈曲（°，0=伸直）。 */
    val hipBend: Float = 0f,
    /** 膝屈曲（°，0=伸直）。 */
    val kneeBend: Float = 0f,
    /** 躯干倾斜（°，0=竖直；带符号，正=向画面左）。 */
    val spineLean: Float = 0f,
    /** 头侧倾（°，0=正；带符号，正=向左），肩颈拉伸用。 */
    val headTilt: Float = 0f,
    /** 双脚分开程度（0=并拢，1=全开），开合跳用。 */
    val legSpread: Float = 0f,
    /** 身体离地高度（0..1），跳绳/开合跳腾空用。 */
    val bodyLift: Float = 0f,
    /** 重心左右偏移（-1..1）。 */
    val weightShift: Float = 0f,
    /** 表情：0=平静 1=微笑 2=鼓励大笑 3=专注。 */
    val face: Float = 0f,
    /** 整体动画进度 0..1（驱动同一 pose 内的平滑过渡）。 */
    val progress: Float = 0f
)

/** 一次完整教练动作的可播放序列（教学页用：动作 + 同步解说）。 */
data class CoachMotionStep(
    val pose: CoachPose,
    val durationMs: Long,
    val message: String? = null
)

data class CoachMotionSequence(
    val exerciseKey: String,
    val steps: List<CoachMotionStep>,
    val loop: Boolean = false
)

/** 播放控制器的动画状态（供事件驱动，避免每帧无谓重组）。 */
enum class CoachAnimationState { PLAYING, PAUSED, HOLD, FINISHED }

/**
 * 数字人动作引擎（纯 JVM，无 Compose / Android 依赖，可单测）。
 *
 *  核心职责：把"某个运动在某个阶段 + 教练状态"翻译成一个具体的 [CoachPose]。
 *  不同动作返回**明显不同**的关节参数：
 *   - 深蹲：屈膝屈髋下沉 → 最低点 → 起身
 *   - 肩颈拉伸：头颈左右侧倾
 *   - 瑜伽：站立侧弯 + 单侧上举
 *   - 跳绳：连续小跳 + 手腕转动
 *   - 开合跳：分腿 + 举臂 开合循环
 *   - 八段锦：8 式不同姿态
 *   - 太极：起势/推掌/云手/重心转移
 *   - 舒缓拉伸：慢速抬臂 + 侧倾
 */
object CoachMotionEngine {

    /** 深蹲：按阶段（站立/下蹲中/最低点/起身）给出屈膝屈髋 + 躯干 + 手臂。 */
    fun squat(phase: String, state: CoachState, ageSpeed: Float): CoachPose {
        // 银龄动作更慢更浅（ageSpeed 越小幅度越小）
        val amp = 1f - (1f - ageSpeed) * 0.4f
        return when {
            // 纠错时暂停在正确姿态：展示"标准最低点"
            state == CoachState.CORRECT -> CoachPose(
                shoulderRaise = 15f * amp, hipBend = 85f * amp, kneeBend = 95f * amp,
                spineLean = -4f, face = 3f, progress = 1f
            )
            // 做得好：站直鼓励
            state == CoachState.GOOD -> CoachPose(
                shoulderRaise = 160f, elbowBend = 0f, hipBend = 0f, kneeBend = 0f,
                face = 2f, progress = 1f
            )
            else -> when (phase) {
                // 站立准备
                "站立准备", "等待中", "准备开始" -> CoachPose(
                    shoulderRaise = 8f, hipBend = 0f, kneeBend = 0f, face = 0f
                )
                // 下蹲中：逐渐屈膝
                "下蹲中" -> CoachPose(
                    shoulderRaise = 30f * amp, hipBend = 45f * amp, kneeBend = 55f * amp,
                    spineLean = -3f, face = 3f
                )
                // 最低点：深蹲到位，臀部后坐
                "最低点" -> CoachPose(
                    shoulderRaise = 10f, hipBend = 88f * amp, kneeBend = 100f * amp,
                    spineLean = -5f, face = 3f
                )
                // 起身中：逐渐伸直
                "起身中", "起身完成", "合跳完成" -> CoachPose(
                    shoulderRaise = 120f * amp, hipBend = 25f * amp, kneeBend = 30f * amp,
                    face = 1f
                )
                else -> CoachPose(
                    shoulderRaise = 20f * amp, hipBend = 40f * amp, kneeBend = 50f * amp,
                    face = 1f
                )
            }
        }
    }

    /** 肩颈拉伸：头颈左右侧倾 + 肩部放松（慢）。 */
    fun neckStretch(phase: String, state: CoachState): CoachPose {
        val tilt = when (phase) {
            "头颈侧倾中" -> 32f      // 侧倾
            "保持拉伸" -> 30f
            "完成一侧" -> 14f
            else -> 0f
        }
        // 左右交替：用 phase 文本里隐含的"侧"来定方向；这里按"侧倾中=左 / 完成=回中"简化
        val dir = when (phase) {
            "头颈侧倾中" -> 1f        // 向左
            "保持拉伸" -> 1f
            else -> 0f
        }
        return when (state) {
            CoachState.GOOD -> CoachPose(headTilt = 0f, face = 2f)
            CoachState.CORRECT -> CoachPose(headTilt = 24f, shoulderRaise = 4f, face = 3f)
            else -> CoachPose(
                headTilt = tilt * dir,
                shoulderRaise = 6f,
                spineLean = 0f,
                face = if (tilt > 0f) 3f else 0f
            )
        }
    }

    /** 瑜伽：站立侧弯 + 同侧手臂上举（左右两侧）。 */
    fun yoga(phase: String, state: CoachState): CoachPose {
        val lean = when (phase) {
            "侧倾保持中" -> 26f
            "回正中" -> 12f
            "完成一侧保持" -> 4f
            else -> 0f
        }
        // 单侧上举：左倾时左臂高举
        val upArm = 150f
        return when (state) {
            CoachState.GOOD -> CoachPose(shoulderRaise = upArm, armSpread = 20f, face = 2f)
            CoachState.CORRECT -> CoachPose(spineLean = 20f, shoulderRaise = 120f, armSpread = 30f, face = 3f)
            else -> CoachPose(
                spineLean = lean,
                shoulderRaise = 90f + lean,
                armSpread = 10f + lean * 0.4f,
                face = 3f
            )
        }
    }

    /** 跳绳：连续小跳 + 手腕转动 + 轻微下沉（不大幅度跳）。 */
    fun jumpRope(phase: String, state: CoachState): CoachPose {
        // 小跳：身体小幅离地 + 屈膝缓冲；手腕高频（用 progress 表达转动方向）
        val jump = when (phase) {
            "腾空" -> 0.5f
            "起跳" -> 0.3f
            "落地恢复" -> 0.1f
            else -> 0.0f
        }
        return when (state) {
            CoachState.GOOD -> CoachPose(bodyLift = 0.1f, kneeBend = 8f, face = 2f)
            CoachState.CORRECT -> CoachPose(bodyLift = 0.35f, kneeBend = 20f, hipBend = 15f, face = 3f)
            else -> CoachPose(
                bodyLift = jump,
                kneeBend = 18f + jump * 10f,
                hipBend = 12f + jump * 8f,
                elbowBend = 40f,       // 手腕持绳
                shoulderRaise = 6f,
                face = 1f
            )
        }
    }

    /** 开合跳：合拢 ↔ 打开（分腿 + 举臂）循环。 */
    fun jumpingJack(phase: String, state: CoachState): CoachPose {
        val open = when (phase) {
            "开跳" -> 1f
            "保持开跳" -> 0.9f
            "合跳完成" -> 0.15f
            else -> 0f
        }
        return when (state) {
            CoachState.GOOD -> CoachPose(legSpread = 0.6f, shoulderRaise = 150f, face = 2f)
            CoachState.CORRECT -> CoachPose(legSpread = 0.8f, shoulderRaise = 155f, face = 3f)
            else -> CoachPose(
                legSpread = open,
                shoulderRaise = 10f + open * 150f,
                bodyLift = open * 0.35f,
                kneeBend = open * 10f,
                face = 1f
            )
        }
    }

    /** 儿童基础拉伸：双臂自然下垂 ↔ 上举保持 ↔ 放下。 */
    fun stretch(phase: String, state: CoachState): CoachPose {
        val up = when (phase) {
            "上举中", "保持上举" -> 1f
            else -> 0f
        }
        return when (state) {
            CoachState.GOOD -> CoachPose(shoulderRaise = 160f, armSpread = 14f, face = 2f)
            CoachState.CORRECT -> CoachPose(shoulderRaise = 130f, armSpread = 20f, face = 3f)
            else -> CoachPose(
                shoulderRaise = 10f + up * 150f,
                armSpread = up * 14f,
                face = 1f
            )
        }
    }

    /**
     * 八段锦：8 式，每式不同姿态（肉眼可区分）。
     * @param move 0..7
     */
    fun baduanjin(move: Int, state: CoachState, ageSpeed: Float): CoachPose {
        val amp = 1f - (1f - ageSpeed) * 0.35f  // 银龄幅度略小
        return when (move) {
            0 -> { // 两手托天：双臂上托
                CoachPose(shoulderRaise = 165f * amp, armSpread = 12f * amp, elbowBend = 8f, face = 3f)
            }
            1 -> { // 左右开弓似射雕：分腿 + 推掌
                CoachPose(legSpread = 0.7f * amp, shoulderRaise = 80f * amp, armSpread = 55f * amp,
                    elbowBend = 20f, spineLean = 3f, face = 3f)
            }
            2 -> { // 调理脾胃须单举：单臂上举另一臂下拉
                CoachPose(shoulderRaise = 150f * amp, armSpread = 70f * amp, weightShift = -0.2f, face = 3f)
            }
            3 -> { // 五劳七伤往后瞧：躯干轻转 + 侧倾
                CoachPose(spineLean = 14f * amp, headTilt = 18f * amp, shoulderRaise = 10f, face = 3f)
            }
            4 -> { // 摇头摆尾去心火：重心左右摆
                CoachPose(weightShift = 0.3f, spineLean = 6f * amp, shoulderRaise = 14f, legSpread = 0.3f, face = 3f)
            }
            5 -> { // 两手攀足固肾腰：前弯下探
                CoachPose(hipBend = 55f * amp, kneeBend = 12f, spineLean = 30f * amp, shoulderRaise = 40f, face = 3f)
            }
            6 -> { // 攒拳怒目增气力：握拳蓄力
                CoachPose(elbowBend = 55f * amp, shoulderRaise = 70f * amp, armSpread = 25f * amp, face = 3f)
            }
            7 -> { // 背后七颠百病消：脚跟提放
                CoachPose(bodyLift = 0.15f, kneeBend = 6f, face = 1f)
            }
            else -> CoachPose(face = 0f)
        }
    }

    /** 太极：起势 / 推掌 / 云手 / 重心转移 / 收势（慢）。 */
    fun taiChi(phase: String, state: CoachState, ageSpeed: Float): CoachPose {
        val amp = 1f - (1f - ageSpeed) * 0.4f
        return when (phase) {
            "起势" -> CoachPose(shoulderRaise = 40f * amp, armSpread = 30f * amp, elbowBend = 30f, face = 3f)
            "推掌保持", "推掌" -> CoachPose(shoulderRaise = 70f * amp, armSpread = 40f * amp, weightShift = 0.1f, face = 3f)
            "重心移动中", "云手" -> CoachPose(shoulderRaise = 90f * amp, armSpread = 60f * amp, weightShift = -0.3f, spineLean = 4f, face = 3f)
            "完成一次循环", "收势" -> CoachPose(shoulderRaise = 20f * amp, armSpread = 10f, face = 2f)
            else -> CoachPose(shoulderRaise = 50f * amp, armSpread = 35f * amp, face = 3f)
        }
    }

    /** 银龄舒缓拉伸：抬臂 + 侧倾 + 回中 + 放松（慢速，ageSpeed 小）。 */
    fun gentleStretch(phase: String, state: CoachState, ageSpeed: Float): CoachPose {
        val amp = 1f - (1f - ageSpeed) * 0.4f
        return when (phase) {
            "缓慢上举", "上举" -> CoachPose(shoulderRaise = 120f * amp, armSpread = 12f * amp, face = 3f)
            "缓慢保持", "保持" -> CoachPose(shoulderRaise = 130f * amp, spineLean = 8f * amp, face = 3f)
            "回中", "放松" -> CoachPose(shoulderRaise = 30f * amp, face = 1f)
            else -> CoachPose(shoulderRaise = 8f * amp, face = 0f)
        }
    }

    /** 通用 IDLE / INTRO / FINISH 姿态（不区分动作）。 */
    fun idle(state: CoachState): CoachPose = when (state) {
        CoachState.IDLE, CoachState.REST -> CoachPose(shoulderRaise = 4f, face = 0f)
        CoachState.INTRO, CoachState.FINISH -> CoachPose(shoulderRaise = 150f, armSpread = 10f, face = 2f)
        CoachState.COUNTDOWN -> CoachPose(shoulderRaise = 90f, armSpread = 30f, face = 3f)
        else -> CoachPose(face = 1f)
    }

    /** 按运动 key + 阶段 + 状态 路由到具体动作 pose（外部统一入口）。 */
    fun poseFor(
        exerciseKey: String?,
        phase: String,
        state: CoachState,
        ageId: String,
        moveIndex: Int = 0
    ): CoachPose {
        // 银龄动作整体偏慢（ageSpeed 0.55），儿童活泼（1.0），青年中速（0.85）
        val speed = when (ageId) { "child" -> 1.0f; "senior" -> 0.55f; else -> 0.85f }
        if (state == CoachState.IDLE || state == CoachState.REST ||
            state == CoachState.INTRO || state == CoachState.FINISH || state == CoachState.COUNTDOWN
        ) {
            // 这些通用状态直接给通用姿态，避免每个动作都覆盖
            if (state != CoachState.IDLE && state != CoachState.REST) return idle(state)
            // IDLE/REST：站立
        }
        return when (exerciseKey) {
            "squat" -> squat(phase, state, speed)
            "shoulder" -> neckStretch(phase, state)
            "yoga" -> yoga(phase, state)
            "jumprope" -> jumpRope(phase, state)
            "jack" -> jumpingJack(phase, state)
            "stretch" -> stretch(phase, state)
            "baduanjin" -> baduanjin(moveIndex, state, speed)
            "taichi" -> taiChi(phase, state, speed)
            "easy" -> gentleStretch(phase, state, speed)
            else -> when (state) {
                CoachState.GOOD -> CoachPose(shoulderRaise = 155f, face = 2f)
                CoachState.CORRECT -> CoachPose(shoulderRaise = 120f, armSpread = 20f, face = 3f)
                else -> idle(state)
            }
        }
    }

    /**
     * 教学演示序列（教学页"开始教学"用）：每一步 = 一个关节姿态 + 同步解说文案。
     * 不同动作返回**明显不同**的多步序列，数字人动作与文字严格同步（spec §十七）。
     */
    fun teachingSequence(exerciseKey: String?, ageId: String, moveName: String): CoachMotionSequence {
        val step = 2200L
        return when (exerciseKey) {
            "squat" -> CoachMotionSequence(exerciseKey ?: "squat", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 8f, face = 0f), step, "Step 1 · 双脚与肩同宽，自然站立"),
                CoachMotionStep(CoachPose(shoulderRaise = 20f, hipBend = 45f, kneeBend = 55f, face = 3f), step, "Step 2 · 臀部向后坐，开始下蹲"),
                CoachMotionStep(CoachPose(shoulderRaise = 10f, hipBend = 88f, kneeBend = 100f, face = 3f), step, "Step 3 · 蹲到最低点，膝盖朝向脚尖"),
                CoachMotionStep(CoachPose(shoulderRaise = 120f, hipBend = 25f, kneeBend = 30f, face = 1f), step, "Step 4 · 缓慢起身，回到站立")
            ))
            "shoulder" -> CoachMotionSequence("shoulder", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(face = 0f), step, "Step 1 · 放松站立，肩颈自然"),
                CoachMotionStep(CoachPose(headTilt = 30f, shoulderRaise = 4f, face = 3f), step, "Step 2 · 头颈向左侧倾，拉直身体一侧"),
                CoachMotionStep(CoachPose(face = 1f), step, "Step 3 · 缓缓回正，感受拉伸"),
                CoachMotionStep(CoachPose(headTilt = -30f, shoulderRaise = 4f, face = 3f), step, "Step 4 · 头颈向右侧倾，重复换边")
            ))
            "yoga" -> CoachMotionSequence("yoga", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 8f, face = 0f), step, "Step 1 · 山式站立，双脚稳定"),
                CoachMotionStep(CoachPose(shoulderRaise = 150f, face = 3f), step, "Step 2 · 双臂沿耳侧上举过头"),
                CoachMotionStep(CoachPose(spineLean = 26f, shoulderRaise = 170f, armSpread = 12f, face = 3f), step, "Step 3 · 向一侧侧弯，手臂延伸"),
                CoachMotionStep(CoachPose(spineLean = 0f, shoulderRaise = 150f, face = 1f), step, "Step 4 · 回正，换边重复")
            ))
            "jumprope" -> CoachMotionSequence("jumprope", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(elbowBend = 40f, face = 0f), 900L, "Step 1 · 手腕放松持绳，准备起跳"),
                CoachMotionStep(CoachPose(bodyLift = 0.4f, kneeBend = 20f, hipBend = 15f, face = 3f), 900L, "Step 2 · 轻脚掌离地，身体小幅下沉"),
                CoachMotionStep(CoachPose(bodyLift = 0f, kneeBend = 8f, face = 1f), 900L, "Step 3 · 柔和落地，保持节奏循环")
            ))
            "jack" -> CoachMotionSequence("jack", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 10f, legSpread = 0f, face = 0f), 1200L, "Step 1 · 并脚站立，双手垂于体侧"),
                CoachMotionStep(CoachPose(shoulderRaise = 160f, legSpread = 1f, bodyLift = 0.3f, face = 3f), 1200L, "Step 2 · 跳开双腿，双手举过头顶"),
                CoachMotionStep(CoachPose(shoulderRaise = 10f, legSpread = 0f, face = 1f), 1200L, "Step 3 · 并拢收回，循环往复")
            ))
            "stretch" -> CoachMotionSequence("stretch", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 8f, face = 0f), step, "Step 1 · 双臂自然下垂，放松"),
                CoachMotionStep(CoachPose(shoulderRaise = 155f, armSpread = 14f, face = 3f), step, "Step 2 · 双臂向上伸展，指尖朝上"),
                CoachMotionStep(CoachPose(shoulderRaise = 150f, face = 1f), step, "Step 3 · 保持伸展，深呼吸"),
                CoachMotionStep(CoachPose(shoulderRaise = 20f, face = 1f), step, "Step 4 · 缓慢放下，回到放松")
            ))
            "baduanjin" -> baduanjinSequence(ageId)
            "taichi" -> CoachMotionSequence("taichi", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 40f, armSpread = 30f, elbowBend = 30f, face = 3f), 3000L, "起势 · 双臂缓缓上抬"),
                CoachMotionStep(CoachPose(shoulderRaise = 70f, armSpread = 40f, weightShift = 0.1f, face = 3f), 3000L, "推掌 · 含胸亮掌向前"),
                CoachMotionStep(CoachPose(shoulderRaise = 90f, armSpread = 60f, weightShift = -0.3f, spineLean = 4f, face = 3f), 3000L, "云手 · 重心左右转移"),
                CoachMotionStep(CoachPose(shoulderRaise = 20f, face = 1f), 3000L, "收势 · 缓缓收回，气息归元")
            ))
            "easy" -> CoachMotionSequence("easy", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 8f, face = 0f), 2600L, "Step 1 · 放松站立，肩颈下沉"),
                CoachMotionStep(CoachPose(shoulderRaise = 120f, face = 3f), 2600L, "Step 2 · 双臂缓慢上举"),
                CoachMotionStep(CoachPose(spineLean = 10f, shoulderRaise = 125f, face = 3f), 2600L, "Step 3 · 向一侧缓慢侧倾"),
                CoachMotionStep(CoachPose(shoulderRaise = 30f, face = 1f), 2600L, "Step 4 · 缓缓回正放松")
            ))
            else -> CoachMotionSequence(exerciseKey ?: "", loop = true, steps = listOf(
                CoachMotionStep(CoachPose(shoulderRaise = 10f, face = 0f), step, "放松准备"),
                CoachMotionStep(CoachPose(shoulderRaise = 150f, face = 3f), step, "开始动作"),
                CoachMotionStep(CoachPose(shoulderRaise = 20f, face = 1f), step, "完成放松")
            ))
        }
    }

    /** 八段锦 8 式演示序列（银龄，慢速）。 */
    private fun baduanjinSequence(ageId: String): CoachMotionSequence {
        val names = listOf(
            "两手托天", "左右开弓", "单举理脾", "往后瞧", "摇头摆尾", "两手攀足", "攒拳怒目", "提踵颠足"
        )
        val dur = 2400L
        val steps = names.mapIndexed { i, n ->
            CoachMotionStep(baduanjin(i, CoachState.DEMO, 0.55f), dur, "第${i + 1}式 · $n")
        }
        return CoachMotionSequence("baduanjin", loop = true, steps = steps)
    }
}
