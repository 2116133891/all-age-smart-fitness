package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame

/**
 * 统一动作分析接口。
 *
 *  每项运动实现一个 [ExerciseAnalyzer]，输出阶段、次数、关节角度、评分信号与纠错。
 *  流程：PoseFrame → [ExerciseAnalyzer.analyze] → [AnalysisResult] → Score / Correction → UI。
 *
 *  旧 [PoseAnalyzer] 接口保留以兼容 [AnalyzerFactory]，新分析器实现 [ExerciseAnalyzer]。
 *  [ExerciseAnalyzer] 比 [PoseAnalyzer] 多输出 [AnalysisResult.signals]（供评分/纠错使用）。
 */
interface ExerciseAnalyzer {
    /** 与 [com.quannian.zhidong.model.Exercise.analysisKind] 对应。 */
    fun key(): String

    /** 逐帧分析。返回阶段 + 是否完成一次 + 关节角度 + 评分信号。 */
    fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): AnalysisResult

    /** 复位内部状态（切换动作 / 重新开始时调用）。 */
    fun reset()
}

/**
 * 一次分析的输出：阶段、完成信号、关节角度、评分信号。
 *
 *  - [rep] 为 true 表示本帧判定"完成一次动作"。
 *  - [depth] 0-1：动作幅度达标比（越大越达标）。
 *  - [posture] 0-1：躯干稳定/姿态质量（1 = 完全稳定）。
 *  - [symmetry] 0-1：左右对称度（1 = 完全对称）。
 *  - [errors]：本帧命中的纠错类型。
 */
data class AnalysisResult(
    val phase: PhaseState = PhaseState("等待中", false),
    val jointAngles: List<JointAngle> = emptyList(),
    val depth: Float = 0f,
    val posture: Float = 1f,
    val symmetry: Float = 1f,
    val errors: List<ErrorType> = emptyList(),
    /** 额外 UI 展示项，例如"八段锦 · 第 N 式"。null 表示不显示。 */
    val subLabel: String? = null,
    /** 节奏稳定性 0-1（跳绳 / 开合跳用），默认 1。 */
    val rhythm: Float = 1f,
    /** 多式动作当前"式/小节"序号（八段锦 0..7、太极 0..4 等），默认 0。供教练动作引擎选择对应姿态。 */
    val moveIndex: Int = 0
)

/**
 * 分析器工厂：把运动 [key] 路由到对应 [ExerciseAnalyzer]。
 *
 *  每个动作都有独立分析器（不再全部复用 ArmReach）：
 *   儿童：jumprope / stretch / jack
 *   青年：squat / shoulder / yoga
 *   银龄：baduanjin / taichi / easy
 */
object ExerciseAnalyzerFactory {

    /**
     * 每个动作的"零状态"工厂：每次 [freshFor] 都 new 一个全新分析器（P0：避免跨 Session 共享有状态实例）。
     *  用**函数**（非 lambda / 匿名类）实现，保持纯 JVM 可直跑（java -cp 单测），不引入
     *  Compose 编译器生成的 LiveLiterals 合成类。
     */
    private fun createNew(key: String): ExerciseAnalyzer = when (key) {
        "squat" -> SquatAnalyzer()
        "shoulder" -> NeckStretchAnalyzer()
        "yoga" -> YogaAnalyzer()
        "jumprope" -> JumpRopeAnalyzer()
        "stretch" -> StretchAnalyzer()
        "jack" -> JumpingJackAnalyzer()
        "baduanjin" -> BaduanjinAnalyzer()
        "taichi" -> TaiChiAnalyzer()
        "easy" -> GentleStretchAnalyzer()
        else -> SquatAnalyzer()  // 未知 key 兜底深蹲（最稳通用动作）
    }

    private val knownKeys = setOf("squat", "shoulder", "yoga", "jumprope", "stretch", "jack", "baduanjin", "taichi", "easy")

    /** 为一次训练 Session 返回**全新**的专属分析器。未知 key 兜底为深蹲。 */
    fun freshFor(key: String?): ExerciseAnalyzer = if (key != null && key in knownKeys) createNew(key) else createNew("squat")

    /** 兼容旧调用：返回一个全新分析器（不再复用 9 个长期单例）。 */
    fun forKey(key: String?): ExerciseAnalyzer = freshFor(key)

    /** 是否某个 key 有专属分析器（用于 UI 展示"已支持实时检测"）。 */
    fun hasDedicated(key: String?): Boolean = key != null && key in knownKeys
}
