package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame

/**
 * MediaPipe 全身姿态关键点索引（33 点）。
 * 参考 Pose Landmarks 标准编号。
 */
object PoseIndex {
    const val NOSE = 0
    const val L_SHOULDER = 11
    const val R_SHOULDER = 12
    const val L_ELBOW = 13
    const val R_ELBOW = 14
    const val L_WRIST = 15
    const val R_WRIST = 16
    const val L_HIP = 23
    const val R_HIP = 24
    const val L_KNEE = 25
    const val R_KNEE = 26
    const val L_ANKLE = 27
    const val R_ANKLE = 28
}

/**
 * 关节角度通用工具：基于三点计算夹角（度）。
 */
object AngleUtils {

    fun angleOf(a: Landmark, b: Landmark, c: Landmark): Float {
        // 以 b 为顶点的夹角
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v2x = c.x - b.x
        val v2y = c.y - b.y
        val dot = v1x * v2x + v1y * v2y
        val m1 = kotlin.math.hypot(v1x, v1y)
        val m2 = kotlin.math.hypot(v2x, v2y)
        if (m1 < 1e-6f || m2 < 1e-6f) return 0f
        var cos = dot / (m1 * m2)
        cos = cos.coerceIn(-1.0f, 1.0f)
        var acosV = kotlin.math.acos(cos.toDouble())
        // 弧度转角度
        acosV = Math.toDegrees(acosV)
        return acosV.toFloat()
    }

    /** 弧度转角度（工具，避免各处手写 toDegrees）。 */
    fun degOf(radians: Double): Float =
        Math.toDegrees(radians).toFloat()

    /** 从 0..1 余弦值得角度（度），输入先 clamp。 */
    fun degOfCos(cosValue: Float): Float = degOf(kotlin.math.acos(cosValue.coerceIn(-1f, 1f).toDouble()))

    fun dist(a: Landmark, b: Landmark): Float = kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    fun mid(a: Landmark, b: Landmark): Landmark =
        Landmark(-1, (a.x + b.x) / 2f, (a.y + b.y) / 2f)

    fun hipMid(lm: Map<Int, Landmark>): Landmark? {
        val lh = lm[PoseIndex.L_HIP]; val rh = lm[PoseIndex.R_HIP]
        if (lh == null || rh == null) return null
        return mid(lh, rh)
    }

    /** 三点夹角的 null 安全版本：任一点缺失返回 0f。用于从 Map<Int, Landmark> 直接取。 */
    fun angleOfSafe(
        a: Landmark?, b: Landmark?, c: Landmark?
    ): Float {
        if (a == null || b == null || c == null) return 0f
        return angleOf(a, b, c)
    }
}

/**
 * 姿态分析器接口。每个动作实现一个 analyzer，输出角度、阶段、是否完成一次（rep）。
 * 未来新增跳绳计数、八段锦等动作：新增实现即可，不影响架构。
 */
interface PoseAnalyzer {
    /** 本分析器对应的 key（与 Exercise.analysisKind 对应）。 */
    fun key(): String
    /** 当前帧的姿态阶段。isRep=true 表示本帧完成了一次动作。 */
    fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): PhaseState
    /** 计算关键关节角度，用于评分与纠错。 */
    fun jointAngles(frame: PoseFrame): List<JointAngle>
}

/**
 * 分析器工厂（旧接口 [PoseAnalyzer]）。
 *
 *  注意：自 spec §17 起，动作分析统一走 [ExerciseAnalyzerFactory]（实现
 *  [ExerciseAnalyzer] 接口的 9 个专属分析器）。本工厂保留以兼容
 *  [ArmReachAnalyzer] 等仍实现旧接口的代码，不再作为新动作的入口。
 */
object AnalyzerFactory {

    private val instances = mapOf(
        "reach" to ArmReachAnalyzer()
    )

    fun forKey(key: String?): PoseAnalyzer? = key?.let { instances[it] }

    /** 兜底：当没有专用分析器时，用 reach（抬臂）作为通用姿态检测。 */
    fun orDefault(key: String?): PoseAnalyzer = forKey(key) ?: ArmReachAnalyzer()
}
