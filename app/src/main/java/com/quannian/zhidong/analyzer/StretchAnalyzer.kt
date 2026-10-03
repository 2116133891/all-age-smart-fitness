package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 基础拉伸分析器（儿童）。
 *
 *  识别：双臂上举 / 身体保持 / 手臂角度 / 持续时间。
 *  状态机：down -> up(保持计时) -> down，完成一次 = 抬起到目标高度并保持 N 帧再放下。
 */
class StretchAnalyzer : ExerciseAnalyzer {

    companion object {
        const val UP_DEG = 145f
        const val DOWN_DEG = 95f
        const val HOLD_FRAMES = 12     // 保持上举的最少帧数
    }

    override fun key() = "stretch"

    private var state = "down"
    private var holdFrames = 0

    override fun reset() { state = "down"; holdFrames = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val arm = armAngle(lm)
        var done = false
        when (state) {
            "down" -> {
                if (arm >= UP_DEG) { state = "up"; holdFrames = 0 }
            }
            "up" -> {
                if (arm >= UP_DEG - 10f) {
                    holdFrames++
                    if (holdFrames >= HOLD_FRAMES) { state = "held" }
                } else state = "up"
            }
            "held" -> {
                if (arm < DOWN_DEG) { done = true; state = "down" }
            }
        }

        val phase = when {
            done -> PhaseState("完成一次", true)
            state == "held" -> PhaseState("保持上举", false)
            state == "up" -> PhaseState("上举中", false)
            else -> PhaseState("放下", false)
        }

        val depth = (arm / 160f).coerceIn(0f, 1f)
        val posture = if (torsoLeaning(lm) < 20f) 1f else 0.7f
        val sym = symmetry(lm)

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("左臂夹角", AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])),
                JointAngle("右臂夹角", AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])),
                JointAngle("平均抬臂幅度", arm)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = buildList {
                if (!frame.detected) add(ErrorType.NO_PERSON)
                if (arm < UP_DEG * 0.7f && state == "up") add(ErrorType.ARMS_TOO_LOW)
                if (torsoLeaning(lm) > 25f) add(ErrorType.BODY_TOO_FORWARD)
            },
            subLabel = phase.phase
        )
    }

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
