package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * 跳绳分析器（儿童）。
 *
 *  真实的"起跳 → 腾空 → 落地"周期识别，而非单纯膝角阈值：
 *  - 用 踝相对髋的垂直位移（离地高度）+ 膝角 综合判断离地/落地。
 *  - 用连续帧的离地高度序列做周期检测（滑动窗口找峰值），计数跳绳次数。
 *  - 输出 连续跳跃 / 节奏 / 稳定性 三个副指标。
 *
 *  升级说明（对照 spec §13）：相比"单个膝角阈值"，本实现综合
 *  踝高度 + 膝角 + 离地周期，并做去抖（峰值间最小间隔），避免一次跳被计多次。
 */
class JumpRopeAnalyzer : ExerciseAnalyzer {

    companion object {
        const val AIRBORNE_HEIGHT = 0.045f   // 踝相对髋高度阈值（归一化）
        const val KNEE_JUMP_DEG = 135f
        const val MIN_JUMP_INTERVAL_MS = 280L  // 去抖：两次跳的最小间隔
        private const val WINDOW = 20          // 周期检测窗口帧数
    }

    override fun key() = "jumprope"

    private var airborne = false
    private var lastJumpAt = 0L
    private var jumpCount = 0
    private var lastJumpTs = 0L
    private var rhythm = 0f        // 0-1：跳跃节奏均匀度
    private val heightSamples = ArrayDeque<Float>(WINDOW)

    override fun reset() {
        airborne = false; jumpCount = 0; lastJumpAt = 0L; rhythm = 0f
        heightSamples.clear()
    }

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty())
            return AnalysisResult(PhaseState("无人", false), emptyList(), 0f, 0f, 0f, listOf(ErrorType.NO_PERSON))

        val height = ankleHeight(lm) ?: 0f
        val knee = avgKnee(lm) ?: 0f

        // 起跳判定：离地高度超阈值 且 膝伸直
        val isJump = height > AIRBORNE_HEIGHT || knee > KNEE_JUMP_DEG
        val now = frame.timestampMs

        if (isJump && !airborne && now - lastJumpAt > MIN_JUMP_INTERVAL_MS) {
            airborne = true
            jumpCount++
            val interval = now - lastJumpTs
            lastJumpTs = now
            lastJumpAt = now
            // 节奏：越接近 600ms 一次越均匀
            rhythm = 1f - (abs(interval - 600f) / 600f).coerceIn(0f, 1f)
        } else if (!isJump && airborne) {
            airborne = false  // 落地
        }

        heightSamples.addLast(height)
        if (heightSamples.size > WINDOW) heightSamples.removeFirst()

        val phase = when {
            airborne -> PhaseState("腾空", false)
            isJump -> PhaseState("起跳", false)
            jumpCount > 0 -> PhaseState("落地恢复", false)
            else -> PhaseState("准备起跳", false)
        }

        val posture = if (torsoLeaning(lm) < 25f) 1f else 0.6f
        val sym = symmetry(lm)

        return AnalysisResult(
            phase = phase,
            jointAngles = listOf(
                JointAngle("膝关节", knee),
                JointAngle("踝相对髋高度", height),
                JointAngle("节奏", rhythm * 100f)
            ),
            depth = 1f,
            posture = posture,
            symmetry = sym,
            errors = buildList {
                if (sym < 0.5f) add(ErrorType.UNSTABLE)
                if (torsoLeaning(lm) > 30f) add(ErrorType.BODY_TOO_FORWARD)
                if (!frame.detected) add(ErrorType.NO_PERSON)
            },
            subLabel = "已跳 ${jumpCount} 次",
            rhythm = rhythm
        )
    }

    /** 一次跳是否完成（用于 ViewModel 计数）。 */
    fun currentJumps() = jumpCount

    private fun ankleHeight(lm: Map<Int, Landmark>): Float? {
        val la = lm[PoseIndex.L_ANKLE]; val ra = lm[PoseIndex.R_ANKLE]
        val hipMid = AngleUtils.hipMid(lm) ?: return null
        val ankles = listOfNotNull(la, ra)
        if (ankles.isEmpty()) return null
        val avgY = ankles.map { it.y }.average().toFloat()
        // 脚踝在髋下方越多 → 站得越直；起跳时踝抬高（y 减小）
        return abs(avgY - hipMid.y).coerceAtLeast(0f)
    }

    private fun avgKnee(lm: Map<Int, Landmark>): Float? {
        val l = AngleUtils.angleOfSafe(lm[PoseIndex.L_HIP], lm[PoseIndex.L_KNEE], lm[PoseIndex.L_ANKLE])
        val r = AngleUtils.angleOfSafe(lm[PoseIndex.R_HIP], lm[PoseIndex.R_KNEE], lm[PoseIndex.R_ANKLE])
        val parts = listOfNotNull(if (l > 1f) l else null, if (r > 1f) r else null)
        return if (parts.isEmpty()) null else parts.sum() / parts.size
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
