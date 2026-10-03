package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 深蹲分析器（青年）。
 *
 *  状态机：standing -> down -> bottom -> up(完成一次)。
 *  - 用膝关节角（有效平均）判断下蹲深度与阶段。
 *  - 用躯干倾角判断是否过前倾。
 *  - 用左右膝对称性判断重心稳定。
 *
 *  健壮性（对照 spec §12）：
 *  - [avgKnee] 只统计可见度足够的关键点，某侧缺失时**用单侧**而非 0 拉低均值。
 *  - 阶段切换用迟滞阈值（DOWN/BOTTOM/UP 各有进入/退出差），避免抖动重复计数。
 *  - 膝盖方向（内扣）检测：膝盖 x 是否明显偏离脚尖/脚踝方向。
 */
class SquatAnalyzer : ExerciseAnalyzer {

    companion object {
        const val SQUAT_KNEE_DEG = 110f    // 低于此值视为已下蹲
        const val BOTTOM_KNEE_DEG = 85f    // 低于此值视为到达最低点
        const val STAND_KNEE_DEG = 150f    // 高于此值视为已起身
        const val HYST = 8f                // 迟滞带：阶段切换需超过 ±HYST 才切换
        const val MIN_VIS = 0.4f          // 关键点可见度下限
    }

    override fun key() = "squat"

    private var state = "standing"
    private var minKnee = Float.MAX_VALUE  // 记录本组最低膝角，用于幅度评分

    override fun reset() {
        state = "standing"
        minKnee = Float.MAX_VALUE
    }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty()) {
            return AnalysisResult(
                phase = PhaseState("无人", false),
                errors = listOf(ErrorType.NO_PERSON),
                posture = 0f, symmetry = 0f, depth = 0f
            )
        }

        // 有效膝关节角：只统计可见度足够的腿，缺腿时单侧计算
        val knee = avgKnee(lm)
        if (knee == null) {
            return AnalysisResult(
                phase = PhaseState("姿态不完整", false),
                errors = listOf(ErrorType.POSTURE_DRIFT),
                posture = 0.5f, symmetry = 0.5f, depth = 0f
            )
        }

        minKnee = minOf(minKnee, knee)
        val tilt = torsoTilt(lm)
        val sym = symmetry(lm)

        // 状态机（带迟滞）
        val phase = when (state) {
            "standing" -> {
                if (knee < SQUAT_KNEE_DEG - HYST) { state = "down"; PhaseState("下蹲中", false) }
                else PhaseState("站立准备", false)
            }
            "down" -> {
                when {
                    knee < BOTTOM_KNEE_DEG + HYST -> { state = "bottom"; PhaseState("最低点", false) }
                    knee > STAND_KNEE_DEG + HYST -> { state = "standing"; PhaseState("起身完成", true) }
                    else -> PhaseState("下蹲中", false)
                }
            }
            "bottom" -> {
                if (knee > STAND_KNEE_DEG) { state = "standing"; PhaseState("起身完成", true) }
                else PhaseState("起身中", false)
            }
            else -> { state = "standing"; PhaseState("站立准备", false) }
        }

        // 评分信号
        val depth = ((160f - knee) / 70f).coerceIn(0f, 1f)  // 蹲得越深 depth 越高
        val posture = if (tilt < 15f) 1f else (1f - (tilt - 15f) / 35f).coerceIn(0f, 1f)
        val errors = buildList {
            if (tilt > 22f) add(ErrorType.BODY_TOO_FORWARD)
            if (depth < 0.4f && state != "standing") add(ErrorType.RANGE_TOO_SHALLOW)
            if (sym < 0.55f) add(ErrorType.UNSTABLE)
            if (kneeOutOfAlignment(lm)) add(ErrorType.KNEE_ANGLE_INVALID)
            if (!frame.detected) add(ErrorType.NO_PERSON)
        }

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("膝关节", knee),
                JointAngle("髋关节", avgHip(lm)),
                JointAngle("躯干倾角", tilt)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = errors,
            subLabel = phase.phase
        )
    }

    /** 有效膝关节角：只统计可见度足够的腿，缺腿时用单侧。 */
    private fun avgKnee(lm: Map<Int, Landmark>): Float? {
        val l = AngleUtils.angleOfSafe(
            if (visOk(lm, PoseIndex.L_HIP, PoseIndex.L_KNEE, PoseIndex.L_ANKLE)) lm[PoseIndex.L_HIP] else null,
            if (visOk(lm, PoseIndex.L_HIP, PoseIndex.L_KNEE, PoseIndex.L_ANKLE)) lm[PoseIndex.L_KNEE] else null,
            if (visOk(lm, PoseIndex.L_HIP, PoseIndex.L_KNEE, PoseIndex.L_ANKLE)) lm[PoseIndex.L_ANKLE] else null
        )
        val r = AngleUtils.angleOfSafe(
            if (visOk(lm, PoseIndex.R_HIP, PoseIndex.R_KNEE, PoseIndex.R_ANKLE)) lm[PoseIndex.R_HIP] else null,
            if (visOk(lm, PoseIndex.R_HIP, PoseIndex.R_KNEE, PoseIndex.R_ANKLE)) lm[PoseIndex.R_KNEE] else null,
            if (visOk(lm, PoseIndex.R_HIP, PoseIndex.R_KNEE, PoseIndex.R_ANKLE)) lm[PoseIndex.R_ANKLE] else null
        )
        // 可见度不足的一侧角度为 0，不参与平均
        val parts = listOfNotNull(if (l > 1f) l else null, if (r > 1f) r else null)
        return if (parts.isEmpty()) null else parts.sum() / parts.size
    }

    private fun visOk(lm: Map<Int, Landmark>, vararg idx: Int): Boolean =
        idx.all { (lm[it]?.visibility ?: 0f) >= MIN_VIS }

    private fun avgHip(lm: Map<Int, Landmark>): Float {
        val lh = AngleUtils.angleOfSafe(lm[PoseIndex.L_KNEE], lm[PoseIndex.L_HIP], lm[PoseIndex.L_SHOULDER])
        val rh = AngleUtils.angleOfSafe(lm[PoseIndex.R_KNEE], lm[PoseIndex.R_HIP], lm[PoseIndex.R_SHOULDER])
        return (lh + rh) / 2f
    }

    /** 躯干倾角（度）。 */
    private fun torsoTilt(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        val ls = lm[PoseIndex.L_SHOULDER]; val rs = lm[PoseIndex.R_SHOULDER]
        if (ls == null || rs == null) return 0f
        val shoulderMid = AngleUtils.mid(ls, rs)
        val above = Landmark(-1, hipMid.x, hipMid.y - abs(shoulderMid.y - hipMid.y).coerceAtLeast(0.1f))
        return AngleUtils.angleOf(above, hipMid, shoulderMid)
    }

    /** 左右肩对称度。 */
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

    /** 膝盖是否内扣（偏离脚尖方向）：简化用左右膝相对髋的水平偏移差。 */
    private fun kneeOutOfAlignment(lm: Map<Int, Landmark>): Boolean {
        val lk = lm[PoseIndex.L_KNEE] ?: return false
        val rk = lm[PoseIndex.R_KNEE] ?: return false
        val lh = lm[PoseIndex.L_HIP] ?: return false
        val rh = lm[PoseIndex.R_HIP] ?: return false
        // 膝盖 x 偏离髋 x 过大且方向异常
        val lOff = abs(lk.x - lh.x); val rOff = abs(rk.x - rh.x)
        return lOff > 0.18f || rOff > 0.18f
    }
}
