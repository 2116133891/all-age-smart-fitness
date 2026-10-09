package com.quannian.zhidong.video

import com.quannian.zhidong.analyzer.ExerciseAnalyzer
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame
import com.quannian.zhidong.score.AgeProfile
import com.quannian.zhidong.score.ScoreCalculator
import kotlin.math.roundToInt

/**
 * 离线视频动作分析引擎（纯 JVM，可单测，不依赖 Android/Compose）。
 *
 *  输入：抽帧后得到的 [VideoFrame] 列表（[timestampMs]+[landmarks]）+ 一个**全新**
 *  [ExerciseAnalyzer]（保证 FSM 状态连续、不与实时跟练串味）+ 年龄段权重。
 *  输出：[VideoAnalysisResult]（次数 / 四维评分 / 每 rep 质量 / 时间轴 / 主错误 / 做对需改）。
 *
 *  复用而非重造（spec §二十六/§二十八）：
 *   - 关键点平滑用 [OneEuroFilter]（抑制 MediaPipe 视频抖动）。
 *   - 阶段/计数/幅度/姿态/对称直接交给各动作专属 [ExerciseAnalyzer.analyze]。
 *   - 评分交给 [ScoreCalculator]（四维加权，年龄段权重）。
 *   - 纠错交给 [CorrectionEngine] 的文案（在 UI/报告层组合）。
 *
 *  每 rep 质量（借鉴 RepDetect per-rep 思路）：在该 rep 完成帧，用"该 rep 期间累计的
 *  幅度/姿态/对称均值"过一遍 [ScoreCalculator] 得 0-100 规范度。
 */
object VideoAnalysisEngine {

    /** 抽帧采样率（Hz）：视频按 ~12fps 抽帧，OneEuro 用同采样率。 */
    const val SAMPLE_RATE_HZ = 12.0

    /** 主错误判定：取出现次数最高的错误类型（时间轴/针对纠正用）。 */
    const val PRIMARY_ERROR_MIN_COUNT = 1

    /**
     * 分析一段视频。
     * @param frames 抽帧后的关键点序列（按时间升序）。
     * @param analyzer **全新**的分析器实例（调用方 [ExerciseAnalyzerFactory.freshFor]）。
     * @param targetReps 目标次数（用于完整度维度）。
     * @param weights 年龄段权重。
     * @param smooth 是否对关键点做 OneEuro 平滑（默认开，抑制抖动）。
     */
    fun analyze(
        frames: List<VideoFrame>,
        analyzer: ExerciseAnalyzer,
        targetReps: Int,
        weights: AgeProfile = AgeProfile.DEFAULT,
        smooth: Boolean = true
    ): VideoAnalysisResult {
        // 每个关键点一对 (x,y) OneEuro 滤波器（33 点 × 2）。仅 smooth=true 时启用。
        val filters = if (smooth) Array(33) { OneEuroFilter.Pair(SAMPLE_RATE_HZ, 1.0, 0.05) } else null

        val durationMs = if (frames.isEmpty()) 0L else frames.last().timestampMs
        val repSamples = mutableListOf<RepSample>()
        val timeline = mutableListOf<TimelineEvent>()
        val errorFreq = mutableMapOf<ErrorType, Int>()
        val seenErrors = mutableSetOf<ErrorType>()

        var repCount = 0
        var depthSum = 0f
        var postureSum = 0f
        var symSum = 0f
        var errorInFrameCount = 0
        var validFrameCount = 0
        var sampleFrames = 0

        // 每个 rep 的累计（rep 内 depth/posture/sym 求和 + 期间错误）
        var repDepthAcc = 0f
        var repPostureAcc = 0f
        var repSymAcc = 0f
        var repSamplesInWindow = 0
        val repErrors = mutableListOf<ErrorType>()

        var prevFrame: PoseFrame? = null

        for (frame in frames) {
            val rawLandmarks = frame.landmarks
            if (rawLandmarks.isEmpty()) {
                // 该帧无人：不累加质量，但要记到时间轴（避免"空镜"污染）
                val emptyFrame = PoseFrame(frame.timestampMs, emptyList(), false)
                val r = analyzer.analyze(emptyFrame, prevFrame)
                prevFrame = emptyFrame
                seenErrors += r.errors
                repErrors += r.errors
                continue
            }

            // OneEuro 平滑（对 33 点 x/y）
            val smoothedLandmarks: List<Landmark>
            if (filters != null) {
                smoothedLandmarks = rawLandmarks.map { lm ->
                    val f = filters[lm.index.coerceIn(0, 32)]
                    val v = f.update(lm.x.toDouble(), lm.y.toDouble())
                    Landmark(lm.index, v.x.toFloat(), v.y.toFloat(), lm.z, lm.visibility)
                }
            } else {
                smoothedLandmarks = rawLandmarks
            }

            val poseFrame = PoseFrame(frame.timestampMs, smoothedLandmarks, true)
            val r = analyzer.analyze(poseFrame, prevFrame)
            prevFrame = poseFrame

            validFrameCount++
            sampleFrames++
            depthSum += r.depth
            postureSum += r.posture
            symSum += r.symmetry

            // 当前帧错误
            val frameErrors = r.errors.filter { it != ErrorType.NO_PERSON }
            if (frameErrors.isNotEmpty()) {
                errorInFrameCount++
                frameErrors.forEach {
                    errorFreq[it] = (errorFreq[it] ?: 0) + 1
                    seenErrors.add(it)
                }
                // 即便该帧未完成一次动作，也把"当前帧错误"记到时间轴（供"查看问题动作"定位）
                val primaryNow = frameErrors.firstOrNull()
                timeline += TimelineEvent(
                    frame.timestampMs,
                    primaryNow?.suggestion ?: "有偏差",
                    primaryNow,
                    true
                )
            }
            repErrors += frameErrors

            // rep 内累计（供 per-rep 质量）
            repDepthAcc += r.depth
            repPostureAcc += r.posture
            repSymAcc += r.symmetry
            repSamplesInWindow++

            // 完成一次：结算本 rep 质量 + 记时间轴事件
            if (r.phase.isRep) {
                repCount++
                val avgDepth = if (repSamplesInWindow > 0) repDepthAcc / repSamplesInWindow else 0f
                val avgPosture = if (repSamplesInWindow > 0) repPostureAcc / repSamplesInWindow else 1f
                val avgSym = if (repSamplesInWindow > 0) repSymAcc / repSamplesInWindow else 1f
                val repBreakdown = ScoreCalculator.calculate(
                    repCount = 1,
                    targetReps = 1,
                    avgDepth = avgDepth,
                    postureSteady = avgPosture,
                    symSteady = avgSym,
                    errorCount = repErrors.size,
                    weights = weights
                )
                val repErrs = repErrors.distinct().filter { it != ErrorType.NO_PERSON }
                repSamples += RepSample(
                    repNumber = repCount,
                    timestampMs = frame.timestampMs,
                    qualityScore = repBreakdown.overall,
                    phaseAt = r.phase.phase,
                    errors = repErrs
                )
                // 时间轴事件：该 rep 正常（无错）或命中主错误
                val primaryInRep = repErrs.maxByOrNull { errorFreq[it] ?: 0 }
                if (repErrs.isEmpty()) {
                    timeline += TimelineEvent(frame.timestampMs, "正常", null, false)
                } else {
                    timeline += TimelineEvent(
                        frame.timestampMs,
                        primaryInRep?.suggestion ?: "有偏差",
                        primaryInRep,
                        true
                    )
                }
                // 重置 rep 窗口
                repDepthAcc = 0f; repPostureAcc = 0f; repSymAcc = 0f
                repSamplesInWindow = 0
                repErrors.clear()
            }
        }

        // 综合评分（整段视频）
        val avgDepth = if (validFrameCount > 0) depthSum / validFrameCount else 0f
        val avgPosture = if (validFrameCount > 0) postureSum / validFrameCount else 1f
        val avgSym = if (validFrameCount > 0) symSum / validFrameCount else 1f
        val overall = ScoreCalculator.calculate(
            repCount = repCount,
            targetReps = targetReps,
            avgDepth = avgDepth,
            postureSteady = avgPosture,
            symSteady = avgSym,
            errorCount = errorInFrameCount,
            weights = weights
        ).overall

        // 主错误（最高频）
        val primary = errorFreq.entries.maxByOrNull { it.value }?.takeIf { it.value >= PRIMARY_ERROR_MIN_COUNT }?.key

        // 做对 / 需改（复用 ScoreCalculator 文案口径 + 主错误）
        val highlights = buildList {
            if (repCount > 0) add("完成 ${repCount} 次动作")
            if (overall >= 70) add("动作整体规范，表现良好")
            if (avgPosture > 0.8f) add("躯干保持得很稳")
            if (avgSym > 0.7f) add("左右较对称，重心稳定")
            if (this.isEmpty()) add("保持住了基本节奏")
        }
        val improvements = buildList {
            primary?.let { add(CorrectionWording.of(it)) }
            if (avgDepth < 0.6f) add("动作幅度可再加大，下蹲/下探更深一些")
            if (avgPosture < 0.7f) add("注意保持躯干稳定")
            if (avgSym < 0.7f) add("注意左右对称、重心稳定")
            if (this.isEmpty()) add("动作节奏可以更均匀")
        }

        return VideoAnalysisResult(
            durationMs = durationMs,
            frameCount = sampleFrames,
            totalReps = repCount,
            overallScore = overall,
            formScore = overall,
            repSamples = repSamples,
            errorTimeline = timeline,
            errorTypesSeen = seenErrors,
            highlights = highlights,
            improvements = improvements,
            primaryError = primary
        )
    }
}

/** 错误类型 → 数字教练"针对纠正"文案（视频报告/教练纠正共用，纯 JVM 可单测）。 */
object CorrectionWording {
    fun of(e: ErrorType): String = when (e) {
        ErrorType.ARMS_TOO_LOW -> "手臂抬高一些，尽量达到目标幅度"
        ErrorType.BODY_TOO_FORWARD -> "上身保持中立，不要过度前倾"
        ErrorType.KNEE_ANGLE_INVALID -> "下蹲时膝盖对准脚尖方向，不要内扣"
        ErrorType.UNSTABLE -> "放慢节奏，先站稳再做"
        ErrorType.RANGE_TOO_SHALLOW -> "幅度略浅，臀部向后坐、继续下降"
        ErrorType.SPEED_TOO_FAST -> "起身/动作速度放慢，节奏更均匀"
        ErrorType.NO_PERSON -> "站到画面中央，保证全身入镜"
        ErrorType.POSTURE_DRIFT -> "重心回中，保持左右对称"
    }
}
