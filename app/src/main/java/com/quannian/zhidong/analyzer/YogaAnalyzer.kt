package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 瑜伽分析器（青年）。
 *
 *  基础瑜伽 = 山式站立 + 侧伸展持姿。
 *  识别：双臂上举 / 躯干侧倾 / 下肢稳定 / 保持时间。
 *  用"持姿计时 + 姿态评分"，一次完成 = 侧倾达到目标角度并保持 N 帧。
 *
 *  安全说明：非医学级，只做基础站立侧伸展的规范度识别。
 */
class YogaAnalyzer : ExerciseAnalyzer {

    companion object {
        const val SIDE_BEND_DEG = 18f    // 躯干侧倾角度目标
        const val ARM_UP_DEG = 150f
        const val HOLD_FRAMES = 20       // 侧倾保持帧数
        const val MAX_TILT_DEG = 30f     // 超过视为过度
    }

    override fun key() = "yoga"

    private var state = "neutral"
    private var holdFrames = 0
    private var bentSide = 0  // 0=中立 1=左 2=右

    override fun reset() { state = "neutral"; holdFrames = 0; bentSide = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val tilt = torsoTilt(lm)
        val arm = armAngle(lm)

        var done = false
        when (state) {
            "neutral" -> {
                if (tilt >= SIDE_BEND_DEG) {
                    state = "held"; holdFrames = 0
                    val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
                    bentSide = if (ls != null && rs != null && ls.y < rs.y) 1 else 2
                }
            }
            "held" -> {
                holdFrames++
                if (tilt < SIDE_BEND_DEG * 0.6f) {
                    state = "releasing"
                } else if (holdFrames >= HOLD_FRAMES) {
                    done = true
                    state = "neutral"
                }
            }
            "releasing" -> {
                if (tilt < SIDE_BEND_DEG * 0.4f) state = "neutral"
            }
        }

        val phase = when {
            done -> PhaseState("完成一侧保持", true)
            state == "held" -> PhaseState("侧倾保持中", false)
            state == "releasing" -> PhaseState("回正中", false)
            else -> PhaseState("中立站立", false)
        }

        val depth = (tilt / SIDE_BEND_DEG).coerceIn(0f, 1f)
        val posture = if (arm >= ARM_UP_DEG * 0.8f && tilt < MAX_TILT_DEG) 1f else 0.7f
        val sym = stability(lm)

        val errors = buildList {
            if (!frame.detected) add(ErrorType.NO_PERSON)
            if (tilt > MAX_TILT_DEG) add(ErrorType.BODY_TOO_FORWARD)
            if (sym < 0.6f) add(ErrorType.UNSTABLE)
            if (arm < ARM_UP_DEG * 0.6f && state == "held") add(ErrorType.ARMS_TOO_LOW)
        }

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("躯干侧倾", tilt),
                JointAngle("抬臂幅度", arm),
                JointAngle("下肢稳定", sym * 100f)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = errors,
            subLabel = phase.phase
        )
    }

    private fun torsoTilt(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        if (ls == null || rs == null) return 0f
        val sMid = AngleUtils.mid(ls, rs)
        val above = Landmark(-1, hipMid.x, hipMid.y - 0.15f)
        return AngleUtils.angleOf(above, hipMid, sMid)
    }

    private fun armAngle(lm: Map<Int, Landmark>): Float {
        val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
        val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
        return (l + r) / 2f
    }

    /** 下肢稳定度：左右踝高度差小 = 稳。 */
    private fun stability(lm: Map<Int, Landmark>): Float {
        val la = lm[PoseIndex.L_ANKLE]; val ra = lm[PoseIndex.R_ANKLE]
        val lh = lm[PoseIndex.L_HIP]; val rh = lm[PoseIndex.R_HIP]
        if (la == null || ra == null || lh == null || rh == null) return 0.7f
        val hipMidY = (lh.y + rh.y) / 2f
        val lOff = abs(la.y - hipMidY); val rOff = abs(ra.y - hipMidY)
        val total = lOff + rOff
        if (total < 1e-3f) return 0.5f
        return minOf(lOff, rOff) / total
    }
}
