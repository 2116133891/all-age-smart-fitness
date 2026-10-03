package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame

/**
 * 肩颈拉伸分析器（青年）。
 *
 *  识别：左侧拉伸 / 右侧拉伸 / 抬头 / 低头 / 肩部位置。
 *  用 头-肩 连线相对水平/竖直的偏角判断头颈方向，
 *  用 肩高差 判断肩部是否耸起。
 *  状态机：neutral -> left -> back -> right -> back，一次完整左右+抬头 = 1 次。
 */
class NeckStretchAnalyzer : ExerciseAnalyzer {

    companion object {
        const val HEAD_TILT_DEG = 12f   // 头相对肩线偏角超过此值视为"侧倾/低头"
        const val SORROW_DEG = 6f       // 肩高差超过此值视为耸肩
    }

    override fun key() = "shoulder"

    private var state = "neutral"
    private var sideDone = 0  // 本组完成的侧向次数

    override fun reset() { state = "neutral"; sideDone = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val head = headOf(lm) ?: return AnalysisResult(
            PhaseState("姿态不完整", false), emptyList(), 0f, 0.5f, 0.5f, listOf(ErrorType.POSTURE_DRIFT))

        val neckTilt = neckTilt(lm)
        val hunch = shoulderHunch(lm)

        val phase = when (state) {
            "neutral" -> when {
                neckTilt > HEAD_TILT_DEG -> { state = "tilted"; PhaseState("头颈侧倾中", false) }
                hunch > SORROW_DEG -> { state = "sorrow"; PhaseState("耸肩中", false) }
                else -> PhaseState("放松站立", false)
            }
            "tilted", "sorrow" -> {
                if (neckTilt < HEAD_TILT_DEG * 0.5f && hunch < SORROW_DEG * 0.5f) {
                    sideDone++
                    state = "neutral"
                    PhaseState("完成一侧", sideDone >= 2)  // 左右各一次 = 1 次
                } else PhaseState("保持拉伸", false)
            }
            else -> { state = "neutral"; PhaseState("放松站立", false) }
        }

        val depth = (neckTilt / 25f).coerceIn(0f, 1f)
        val posture = if (hunch < SORROW_DEG) 1f else (1f - (hunch - SORROW_DEG) / 30f).coerceIn(0f, 1f)
        val sym = symmetry(lm)
        val errors = buildList {
            if (hunch > SORROW_DEG * 1.5f) add(ErrorType.ARMS_TOO_LOW)
            if (sym < 0.5f) add(ErrorType.UNSTABLE)
            if (!frame.detected) add(ErrorType.NO_PERSON)
        }

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("头颈偏角", neckTilt),
                JointAngle("肩部放松度", (1 - hunch / 30f).coerceIn(0f, 1f) * 100f)
            ),
            depth = depth, posture = posture, symmetry = sym, errors = errors,
            subLabel = phase.phase
        )
    }

    private fun headOf(lm: Map<Int, Landmark>): Landmark? {
        // 用鼻-耳中点近似头部位置（MediaPipe 头部关键点 0..10）
        val nose = lm[PoseIndex.NOSE]
        return nose
    }

    /** 头颈相对躯干中线的偏角（度）。 */
    private fun neckTilt(lm: Map<Int, Landmark>): Float {
        val nose = lm[PoseIndex.NOSE] ?: return 0f
        val lmHip = AngleUtils.hipMid(lm) ?: return 0f
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        if (ls == null || rs == null) return 0f
        val shoulderMid = AngleUtils.mid(ls, rs)
        // 竖直参考线：从肩中点向上
        val above = Landmark(-1, shoulderMid.x, shoulderMid.y - 0.2f)
        val trunk = AngleUtils.angleOf(above, shoulderMid, lmHip)
        // 头颈：从肩中点到鼻子的偏角
        val neck = AngleUtils.angleOf(above, shoulderMid, nose)
        return Math.abs(neck - trunk)
    }

    /** 耸肩程度（肩高差 / 头肩比，越大越耸）。 */
    private fun shoulderHunch(lm: Map<Int, Landmark>): Float {
        val ls = lm[PoseIndex.L_SHOULDER] ?: return 0f
        val rs = lm[PoseIndex.R_SHOULDER] ?: return 0f
        val diff = kotlin.math.abs(ls.y - rs.y)
        val headDist = kotlin.math.abs(rs.x - ls.x).coerceAtLeast(0.05f)
        return (diff / headDist) * 0.3f  // 归一到 0-1
    }

    private fun symmetry(lm: Map<Int, Landmark>): Float {
        val ls = lm[PoseIndex.L_SHOULDER] ?: return 0.8f
        val rs = lm[PoseIndex.R_SHOULDER] ?: return 0.8f
        val lh = lm[PoseIndex.L_HIP]; val rh = lm[PoseIndex.R_HIP]
        if (lh == null || rh == null) return 0.7f
        val hipMid = (lh.x + rh.x) / 2f
        if (lm[PoseIndex.L_HIP] == null || lm[PoseIndex.R_HIP] == null) return 0.7f
        val dl = Math.abs(ls.x - hipMid); val dr = Math.abs(rs.x - hipMid)
        if (dl + dr < 1e-3f) return 0.5f
        return Math.min(dl, dr) / Math.max(dl, dr)
    }
}
