package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 八段锦分析器（银龄）。
 *
 *  分阶段动作状态机，8 个式别：
 *   1 两手托天理三焦  2 左右开弓似射雕  3 调理脾胃须单举
 *   4 五劳七伤往后瞧  5 摇头摆尾去心火  6 两手攀足固肾腰
 *   7 攒拳怒目增气力  8 背后七颠百病消
 *
 *  规则（基于人体关键点，非医学级）：每个式别识别 1 个可量化的关键姿态
 *  （双手位置 / 躯干旋转 / 下肢稳定 / 阶段时间），当前式别达到判定条件则自动
 *  推进到下一式。UI 显示"八段锦 · 第 N 式"。
 */
class BaduanjinAnalyzer : ExerciseAnalyzer {

    companion object {
        val MOVE_NAMES = listOf(
            "两手托天理三焦", "左右开弓似射雕", "调理脾胃须单举", "五劳七伤往后瞧",
            "摇头摆尾去心火", "两手攀足固肾腰", "攒拳怒目增气力", "背后七颠百病消"
        )
        const val ARM_UP_DEG = 150f
        const val SIDE_BEND_DEG = 15f
        const val HOLD_FRAMES = 15
    }

    override fun key() = "baduanjin"

    private var moveIndex = 0       // 当前式别 0..7
    private var holdFrames = 0

    override fun reset() { moveIndex = 0; holdFrames = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val current = moveIndex
        val signal = moveSignal(current, lm)
        val depth = (signal / 100f).coerceIn(0f, 1f)

        // 达到判定条件并持续 HOLD_FRAMES 帧 → 推进下一式（第8式完成后回第1式）
        if (signal >= 80f) {
            holdFrames++
            if (holdFrames >= HOLD_FRAMES) {
                moveIndex = (moveIndex + 1) % MOVE_NAMES.size
                holdFrames = 0
            }
        } else {
            holdFrames = 0
        }

        val posture = if (torsoLeaning(lm) < 25f) 1f else 0.7f
        val sym = symmetry(lm)
        val curMove = moveIndex
        return AnalysisResult(
            phase = PhaseState("第${curMove + 1}式 · 保持中", false),
            jointAngles = listOf(
                JointAngle("当前式别进度", signal),
                JointAngle("躯干稳定", posture * 100f)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = buildList {
                if (!frame.detected) add(ErrorType.NO_PERSON)
                if (sym < 0.5f) add(ErrorType.UNSTABLE)
            },
            subLabel = "八段锦 · 第${curMove + 1}式 ${MOVE_NAMES[curMove]}",
            moveIndex = curMove
        )
    }

    /** 当前式别的可量化信号（0-100）。每个式别用一个明确的姿态指标。 */
    private fun moveSignal(move: Int, lm: Map<Int, Landmark>): Float = when (move) {
        0 -> { // 两手托天：双臂上举
            (armAngle(lm) / 160f).coerceIn(0f, 1f) * 100f
        }
        1 -> { // 左右开弓：躯干侧倾 + 手臂前推
            val tilt = torsoTilt(lm)
            val arm = armAngle(lm)
            ((tilt / SIDE_BEND_DEG).coerceIn(0f, 1f) + (arm / ARM_UP_DEG).coerceIn(0f, 1f)) / 2f * 100f
        }
        2 -> { // 调理脾胃须单举：单臂上举，另一臂下拉
            val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
            val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
            (abs(l - r) / 90f).coerceIn(0f, 1f) * 100f  // 左右不对称 = 单举
        }
        3 -> { // 往后瞧：躯干旋转
            trunkRotation(lm) * 100f / 60f
        }
        4 -> { // 摇头摆尾：重心左右移
            weightShift(lm) * 100f
        }
        5 -> { // 攀足固肾腰：前弯（躯干前倾）
            (torsoLeaning(lm) / 35f).coerceIn(0f, 1f) * 100f
        }
        6 -> { // 攒拳怒目：手臂握拳前推（近似用抬臂+身体稳定）
            ((armAngle(lm) / ARM_UP_DEG).coerceIn(0f, 1f) + stability(lm)) / 2f * 100f
        }
        7 -> { // 七颠：脚跟提放（小腿 + 重心微动）
            heelLift(lm) * 100f
        }
        else -> 0f
    }

    private fun armAngle(lm: Map<Int, Landmark>): Float {
        val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
        val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
        return (l + r) / 2f
    }

    private fun torsoTilt(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        if (ls == null || rs == null) return 0f
        val sMid = AngleUtils.mid(ls, rs)
        val above = Landmark(-1, hipMid.x, hipMid.y - 0.15f)
        return AngleUtils.angleOf(above, hipMid, sMid)
    }

    private fun torsoLeaning(lm: Map<Int, Landmark>): Float = torsoTilt(lm)

    /** 躯干绕竖直轴旋转（肩-髋连线水平差）。0-1。 */
    private fun trunkRotation(lm: Map<Int, Landmark>): Float {
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        val lh = lm[PoseIndex.L_HIP]; val rh = lm[PoseIndex.R_HIP]
        if (ls == null || rs == null || lh == null || rh == null) return 0f
        val rot = abs((rs.y - rs.x) - (ls.y - ls.x))
        return (rot / 0.3f).coerceIn(0f, 1f)
    }

    /** 重心左右移（髋中点相对画面中心偏移）。0-1。 */
    private fun weightShift(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        return abs(hipMid.x - 0.5f) * 2f  // 0.5 = 正中，越偏越大
    }

    /** 脚跟提放（踝相对髋高度波动）。0-1。 */
    private fun heelLift(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val la = lm[PoseIndex.L_ANKLE]; val ra = lm[PoseIndex.R_ANKLE]
        val ankles = listOfNotNull(la, ra)
        if (ankles.isEmpty()) return 0f
        val avgY = ankles.map { it.y }.average().toFloat()
        return abs(avgY - hipMid.y).coerceIn(0f, 1f)
    }

    /** 下肢稳定度。 */
    private fun stability(lm: Map<Int, Landmark>): Float = symmetry(lm)

    private fun symmetry(lm: Map<Int, Landmark>): Float {
        val ls = lm[PoseIndex.L_SHOULDER] ?: return 0.8f
        val rs = lm[PoseIndex.R_SHOULDER] ?: return 0.8f
        val lh = lm[PoseIndex.L_HIP]; val rh = lm[PoseIndex.R_HIP]
        if (lh == null || rh == null) return 0.7f
        val hipMid = (lh.x + rh.x) / 2f
        if (lm[PoseIndex.L_HIP] == null || lm[PoseIndex.R_HIP] == null) return 0.7f
        val dl = abs(ls.x - hipMid); val dr = abs(rs.x - hipMid)
        if (dl + dr < 1e-3f) return 0.5f
        return minOf(dl, dr) / maxOf(dl, dr)
    }
}
