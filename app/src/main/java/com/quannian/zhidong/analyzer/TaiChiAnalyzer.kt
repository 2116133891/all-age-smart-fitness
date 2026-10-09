package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import kotlin.math.abs

/**
 * 太极分析器（银龄）。
 *
 *  阶段识别 + 持续时间 + 姿态评分：
 *  - 起势（双臂缓抬）
 *  - 左右移动（重心转移）
 *  - 手臂推掌
 *  - 重心转移 / 躯干旋转
 *  - 缓慢动作保持
 *
 *  银龄安全原则：动作慢、重心稳；速度过快或重心大幅起伏判为不规范（而非失败）。
 */
class TaiChiAnalyzer : ExerciseAnalyzer {

    companion object {
        const val ARM_UP_DEG = 140f
        const val SLOW_HOLD_FRAMES = 30   // 银龄要求更长的稳定保持
        const val MAX_TILT_DEG = 30f
    }

    override fun key() = "taichi"

    private var state = "starting"
    private var holdFrames = 0

    override fun reset() { state = "starting"; holdFrames = 0 }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val arm = armAngle(lm)
        val tilt = torsoTilt(lm)
        val shift = weightShift(lm)

        var done = false
        when (state) {
            "starting" -> {
                if (arm >= ARM_UP_DEG * 0.6f) { state = "hold"; holdFrames = 0 }
            }
            "hold" -> {
                holdFrames++
                // 重心开始转移 → 进入移动
                if (holdFrames >= SLOW_HOLD_FRAMES && shift > 0.05f) { state = "moving" }
                else if (arm < ARM_UP_DEG * 0.4f) { state = "starting" }
            }
            "moving" -> {
                // 保持移动 + 稳定：重心持续转移 且 躯干不大幅起伏 = 完成一次循环
                if (shift > 0.08f && tilt < MAX_TILT_DEG) {
                    holdFrames++
                    if (holdFrames >= SLOW_HOLD_FRAMES) {
                        done = true
                        state = "starting"
                        holdFrames = 0
                    }
                } else state = "moving"
            }
        }

        val phase = when {
            done -> PhaseState("完成一次循环", true)
            state == "moving" -> PhaseState("重心移动中", false)
            state == "hold" -> PhaseState("推掌保持", false)
            else -> PhaseState("起势", false)
        }

        val depth = (arm / ARM_UP_DEG).coerceIn(0f, 1f)
        val posture = if (tilt < MAX_TILT_DEG) 1f else 0.6f
        val sym = symmetry(lm)

        // 银龄安全：重心起伏过大提示（不计失败）
        val errors = buildList {
            if (!frame.detected) add(ErrorType.NO_PERSON)
            if (tilt > MAX_TILT_DEG) add(ErrorType.POSTURE_DRIFT)
            if (sym < 0.45f) add(ErrorType.UNSTABLE)
        }

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("推掌幅度", arm),
                JointAngle("重心转移", shift * 100f),
                JointAngle("躯干稳定", (1 - tilt / MAX_TILT_DEG).coerceIn(0f, 1f) * 100f)
            ),
            depth = depth,
            posture = posture,
            symmetry = sym,
            errors = errors,
            subLabel = phase.phase,
            moveIndex = when {
                done -> 4  // 收势
                state == "moving" -> 3  // 重心转移 / 云手
                state == "hold" -> 2    // 推掌保持
                else -> 0               // 起势
            }
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

    private fun weightShift(lm: Map<Int, Landmark>): Float {
        val hipMid = AngleUtils.hipMid(lm) ?: return 0f
        return abs(hipMid.x - 0.5f) * 2f
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
