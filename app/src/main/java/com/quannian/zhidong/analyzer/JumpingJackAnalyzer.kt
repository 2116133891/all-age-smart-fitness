package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 开合跳分析器（儿童）。
 *
 *  一个完整周期 = 双腿合拢 + 双手下垂 → 双腿分开 + 双手上举 → 恢复。
 *  用 脚踝间距（分/合）+ 手腕高度（举/垂）两个信号做状态机：
 *    closed -> open -> closed(完成一次)。
 *  识别 手臂高度 / 双脚距离 / 身体中心位移。
 */
class JumpingJackAnalyzer : ExerciseAnalyzer {

    companion object {
        const val LEG_OPEN_DEG = 40f      // 髋-踝 分腿角超过此值视为"开"
        const val LEG_CLOSED_DEG = 18f
        const val ARM_UP_DEG = 150f       // 手臂接近伸直上举
        const val ARM_DOWN_DEG = 80f
    }

    override fun key() = "jack"

    private var state = "closed"
    private var repCount = 0

    override fun reset() { state = "closed"; repCount = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val legOpen = legAngle(lm)
        val armUp = armAngle(lm)

        val isLegsOpen = legOpen > LEG_OPEN_DEG
        val isArmsUp = armUp > ARM_UP_DEG
        val isLegsClosed = legOpen < LEG_CLOSED_DEG
        val isArmsDown = armUp < ARM_DOWN_DEG

        val phase = when (state) {
            "closed" -> {
                if (isLegsOpen && isArmsUp) { state = "open"; PhaseState("开跳", false) }
                else PhaseState("并拢准备", false)
            }
            "open" -> {
                if (isLegsClosed && isArmsDown) {
                    repCount++
                    state = "closed"
                    PhaseState("合跳完成", true)
                } else PhaseState("保持开跳", false)
            }
            else -> { state = "closed"; PhaseState("并拢准备", false) }
        }

        val depth = ((armUp - ARM_DOWN_DEG) / (ARM_UP_DEG - ARM_DOWN_DEG)).coerceIn(0f, 1f)
        val posture = if (torsoLeaning(lm) < 25f) 1f else 0.6f
        val sym = symmetry(lm)

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("分腿角", legOpen),
                JointAngle("抬臂幅度", armUp)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = buildList {
                if (!frame.detected) add(ErrorType.NO_PERSON)
                if (sym < 0.5f) add(ErrorType.UNSTABLE)
                if (legOpen in (LEG_OPEN_DEG - 10f)..LEG_OPEN_DEG && !isArmsUp) add(ErrorType.ARMS_TOO_LOW)
            },
            subLabel = "已完成 ${repCount} 次"
        )
    }

    /** 髋-踝 分腿角（度，越大越开）。用髋中点为顶点、左右踝为两端的三点夹角（点积法）。 */
    private fun legAngle(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val la = lm[PoseIndex.L_ANKLE]; val ra = lm[PoseIndex.R_ANKLE]
        if (la == null || ra == null) return 0f
        return AngleUtils.angleOf(la, hipMid, ra)
    }

    /** 双臂上举幅度（肩-肘-腕 夹角平均，越大越举高）。 */
    private fun armAngle(lm: Map<Int, Landmark>): Float {
        val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
        val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
        return (l + r) / 2f
    }

    private fun torsoLeaning(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        if (ls == null || rs == null) return 0f
        val sMid = AngleUtils.mid(ls, rs)
        val above = Landmark(-1, hipMid.x, hipMid.y - 0.1f)
        return AngleUtils.angleOf(above, hipMid, sMid)
    }

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
