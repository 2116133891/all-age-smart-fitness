package com.quannian.zhidong.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.quannian.zhidong.analyzer.ExerciseAnalyzer
import com.quannian.zhidong.analyzer.ExerciseAnalyzerFactory
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import com.quannian.zhidong.model.TrainingReport
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.score.AgeProfile
import com.quannian.zhidong.score.CorrectionEngine
import com.quannian.zhidong.score.ScoreCalculator
import com.quannian.zhidong.ui.components.CoachState

/**
 * 跟练页 ViewModel：把"一帧关键点"变成可展示的实时状态。
 *
 *  职责（P0 核心闭环）：
 *   - 每帧 landmarks → [ExerciseAnalyzer]（各动作专属分析器）→ 评分 → 纠错
 *   - 维护实时 UI 状态（得分、状态、纠正建议、计数、子标签如"八段锦·第N式"）
 *   - 训练结束时汇总 [TrainingReport]
 *
 *  各动作的 depth / posture / symmetry / errors 直接来自 [ExerciseAnalyzer.analyze]
 *  返回的 [com.quannian.zhidong.analyzer.AnalysisResult]，不再在 ViewModel 里手写规则。
 *  评分按年龄段使用不同权重（见 [ScoreCalculator] + [AgeProfile]）。
 */
class FollowAlongViewModel(analysisKind: String?, targetReps: Int) : ViewModel() {

    private val exercise = resolveExercise(analysisKind)
    private val targetReps = targetReps
    /**
     * 每个训练 Session 用**全新**的分析器实例（P0：避免跨场次共享有状态分析器导致计数错位）。
     * 工厂改为 [freshFor] 每次 new 一个新实例；[startSession] 时再 reset 一次确保干净起点。
     */
    private val analyzer: ExerciseAnalyzer = ExerciseAnalyzerFactory.freshFor(analysisKind)
    private val ageProfile: AgeProfile = AgeProfile.forAge(exercise.ageGroup)

    // ---- 实时状态（Compose 直接收集） ----
    private val _landmarks = MutableStateFlow<List<Landmark>>(emptyList())
    val landmarks: StateFlow<List<Landmark>> = _landmarks

    private val _phase = MutableStateFlow(PhaseState("等待中", false))
    val phase: StateFlow<PhaseState> = _phase

    /** 子标签（如"八段锦 · 第 1 式 两手托天理三焦" / "已跳 N 次"）。 */
    private val _subLabel = MutableStateFlow("")
    val subLabel: StateFlow<String> = _subLabel

    /** 当前"式/小节"序号（八段锦 0..7、太极 0..4），供教练动作引擎选择对应姿态。 */
    private val _moveIndex = MutableStateFlow(0)
    val moveIndex: StateFlow<Int> = _moveIndex

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _stateText = MutableStateFlow("准备开始")
    val stateText: StateFlow<String> = _stateText

    private val _corrections = MutableStateFlow<List<String>>(emptyList())
    val corrections: StateFlow<List<String>> = _corrections

    /** 当前帧命中的错误类型集合（喂给 LocalCoachProvider 生成数字人话术）。 */
    private val _currentErrors = MutableStateFlow<Set<ErrorType>>(emptySet())
    val currentErrors: StateFlow<Set<ErrorType>> = _currentErrors

    /** AI 姿态识别是否就绪（MediaPipe 模型加载完成）。 */
    private val _poseReady = MutableStateFlow(false)
    val poseReady: StateFlow<Boolean> = _poseReady

    fun setPoseReady(ready: Boolean) { _poseReady.value = ready }

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount

    /** 数字人教练状态（由分析阶段 + 纠错驱动）。 */
    private val _coachState = MutableStateFlow(CoachState.IDLE)
    val coachState: StateFlow<CoachState> = _coachState

    // ---- 内部统计（用于结算报告） ----
    private var repCountInternal = 0
    private var seenErrors = mutableSetOf<ErrorType>()
    private var depthSum = 0f
    private var depthSamples = 0
    private var postureSteadySum = 0f
    private var symSteadySum = 0f
    private var rhythmSum = 0f
    private var startTime = 0L

    init {
        startTime = System.currentTimeMillis()
        // 进入跟练页 = 新 Session，保证分析器从干净状态开始（P0 reset）
        analyzer.reset()
    }

    /**
     * 结束一次训练 Session（页面退出 / 返回时调用）：
     *  释放分析器状态、停止 TTS，避免下一场被污染。
     */
    fun onSessionEnd() {
        analyzer.reset()
        repCountInternal = 0
        seenErrors = mutableSetOf()
        depthSum = 0f; depthSamples = 0
        postureSteadySum = 0f; symSteadySum = 0f; rhythmSum = 0f
        _repCount.value = 0
        _currentErrors.value = emptySet()
        _coachState.value = CoachState.IDLE
        startTime = System.currentTimeMillis()
    }

    /** 处理一帧（来自 CameraManager 的 onFrame）。 */
    fun onFrame(landmarks: List<Landmark>) {
        _landmarks.value = landmarks
        val frame = PoseFrame(System.currentTimeMillis(), landmarks, landmarks.isNotEmpty())

        if (frame.detected) {
            val result = analyzer.analyze(frame, lastFrame)
            lastFrame = frame

            _phase.value = result.phase
            if (result.subLabel != null) _subLabel.value = result.subLabel
            _moveIndex.value = result.moveIndex

            if (result.phase.isRep) {
                repCountInternal++
                _repCount.value = repCountInternal
            }

            seenErrors += result.errors
            // 当前帧真实错误类型（喂给数字人话术 / LocalCoachProvider）
            val frameErrors: Set<ErrorType> = result.errors.toSet().filter { it != ErrorType.NO_PERSON }.toSet()
            _currentErrors.value = frameErrors

            val breakdown = ScoreCalculator.calculate(
                repCount = repCountInternal,
                targetReps = targetReps,
                avgDepth = result.depth,
                postureSteady = result.posture,
                symSteady = result.symmetry,
                errorCount = result.errors.size,
                weights = ageProfile
            )
            _score.value = breakdown.overall
            _stateText.value = ScoreCalculator.stateOf(breakdown.overall, ageProfile)

            val current = CorrectionEngine.suggestions(frameErrors, 3)
            _corrections.value = if (current.isNotEmpty()) current
            else _corrections.value.takeLast(1)
            // 数字人教练状态驱动：有纠错 → CORRECT；完成一次 → GOOD；演示中 → DEMO
            _coachState.value = when {
                result.errors.isNotEmpty() -> CoachState.CORRECT
                result.phase.isRep -> CoachState.GOOD
                else -> CoachState.DEMO
            }

            depthSum += result.depth; depthSamples++
            postureSteadySum += result.posture
            symSteadySum += result.symmetry
            rhythmSum += result.rhythm
        } else {
            _stateText.value = "未检测到人体，请站到摄像头前"
            _currentErrors.value = emptySet()
        }
    }

    private var lastFrame: PoseFrame? = null

    /** 结算：生成训练报告，并**立即**持久化到 Room（不依赖报告页落库，P0 §9）。 */
    fun finishReport(context: android.content.Context): TrainingReport {
        val report = buildReport()
        // 结束训练瞬间落库：即使报告页出问题，数据已保存。
        viewModelScope.launch {
            try {
                TrainingRepository(context.applicationContext).save(
                    report,
                    exerciseId = exercise.id,
                    exerciseName = report.exerciseName
                )
            } catch (_: Exception) {
                // 持久化失败不影响报告展示
            }
        }
        return report
    }

    private fun buildReport(): TrainingReport {
        val repCount = repCountInternal
        val target = targetReps
        val avgDepth = if (depthSamples > 0) depthSum / depthSamples else 0f
        val posture = if (depthSamples > 0) postureSteadySum / depthSamples else 0f
        val sym = if (depthSamples > 0) symSteadySum / depthSamples else 0f
        val rhythm = if (depthSamples > 0) rhythmSum / depthSamples else 1f
        val durSec = ((System.currentTimeMillis() - startTime) / 1000).toInt()

        val breakdown = ScoreCalculator.calculate(
            repCount, target, avgDepth, posture, sym, seenErrors.size, ageProfile
        )
        val overall = breakdown.overall

        val highlights = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        if (repCount >= target) highlights += "完成了全部 ${target} 次目标动作"
        if (overall >= 70) highlights += "动作整体规范，表现良好"
        if (posture > 0.8f) highlights += "躯干保持得很稳"
        if (rhythm > 0.7f && repCount > 0) highlights += "节奏比较均匀"
        if (improvements.isEmpty() && errorsTop() == null) improvements += "动作完成度较高，继续保持"
        errorsTop()?.let { improvements += it }
        if (posture < 0.7f) improvements += "注意保持躯干稳定"
        if (avgDepth < 0.6f) improvements += "可适当增加动作幅度"
        if (sym < 0.7f) improvements += "注意左右对称、保持重心稳定"

        return TrainingReport(
            exerciseName = exercise.name,
            ageGroup = exercise.ageGroup.id,
            repCount = repCount,
            targetReps = target,
            formScorePercent = breakdown.formPercent,
            overallScore = overall,
            durationSec = durSec,
            highlights = highlights.ifEmpty { listOf("保持住了基本节奏") },
            improvements = improvements.ifEmpty { listOf("动作节奏可以更均匀") },
            errorTypesSeen = seenErrors.map { it.code }.toSet(),
            rangeScore = breakdown.rangeScore,
            postureScore = breakdown.postureScore,
            stabilityScore = breakdown.stabilityScore,
            completenessScore = breakdown.completenessScore
        )
    }

    private fun errorsTop(): String? = CorrectionEngine.suggestions(seenErrors.toSet(), 1).firstOrNull()

    private fun resolveExercise(kind: String?): com.quannian.zhidong.model.Exercise =
        ExerciseRepository.all().firstOrNull { it.analysisKind == kind }
            ?: ExerciseRepository.all().first()
}
