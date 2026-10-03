package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 银龄舒缓拉伸分析器。
 *
 *  识别：举臂 / 侧倾 / 肩部 / 上肢角度。
 *  银龄安全原则（对照 spec §16）：
 *  - 动作速度降低时**不应**被当作识别失败（放宽速度阈值）
 *  - 更长的稳定时间（HOLD_FRAMES 更大）
 *  - 更大的安全容错（姿态分不轻易降 0）
 *
 *  状态机：down -> up(缓慢) -> hold(保持) -> down，一次完成 = 缓慢抬举并保持再放下。
 */
class GentleStretchAnalyzer : ExerciseAnalyzer {

    companion object {
        const val UP_DEG = 130f          // 银龄抬举目标（比青年低，更舒缓）
        const val DOWN_DEG = 85f
        const val HOLD_FRAMES = 40       // 银龄：更长的稳定保持时间
        const val TOLERANCE_TILT = 35f   // 更大的安全容错
    }

    override fun key() = "easy"

    private var state = "down"
    private var holdFrames = 0

    override fun reset() { state = "down"; holdFrames = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val arm = armAngle(lm)
        val tilt = torsoTilt(lm)
        val shoulderRelaxed = shoulderRelaxed(lm)

        var done = false
        when (state) {
            "down" -> if (arm >= UP_DEG) { state = "up"; holdFrames = 0 }
            "up" -> {
                if (arm >= UP_DEG - 15f) holdFrames++
                if (holdFrames >= HOLD_FRAMES) { state = "held" }
            }
            "held" -> {
                if (arm < DOWN_DEG) { done = true; state = "down" }
            }
        }

        val phase = when {
            done -> PhaseState("完成一次", true)
            state == "held" -> PhaseState("缓慢保持", false)
            state == "up" -> PhaseState("缓慢上举", false)
            else -> PhaseState("放松", false)
        }

        val depth = (arm / UP_DEG).coerceIn(0f, 1f)
        // 银龄：即使倾角偏大也不立刻降姿态分（安全容错）
        val posture = if (tilt < TOLERANCE_TILT) 1f else (1f - (tilt - TOLERANCE_TILT) / 40f).coerceIn(0.5f, 1f)
        val sym = symmetry(lm)

        val errors = buildList {
            if (!frame.detected) add(ErrorType.NO_PERSON)
            if (shoulderRelaxed < 0.4f) add(ErrorType.ARMS_TOO_LOW)  // 肩未放松
            if (sym < 0.4f) add(ErrorType.UNSTABLE)
        }

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("左臂夹角", AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])),
                JointAngle("右臂夹角", AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])),
                JointAngle("肩部放松度", shoulderRelaxed * 100f)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = errors,
            subLabel = phase.phase
        )
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

    /** 肩部放松度：肩-肘-腕 角度越舒展越放松（0-1）。 */
    private fun shoulderRelaxed(lm: Map<Int, Landmark>): Float {
        val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
        val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
        return ((l + r) / 2f / 160f).coerceIn(0f, 1f)
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
