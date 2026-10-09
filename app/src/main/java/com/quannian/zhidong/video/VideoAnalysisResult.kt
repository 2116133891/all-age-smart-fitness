package com.quannian.zhidong.video

import com.quannian.zhidong.model.ErrorType

/**
 * 一帧抽取出来的视频关键点数据（视频分析管线用）。
 *
 *  [timestampMs] 为视频内该帧的时间点（用于时间轴 / 问题片段回放定位）。
 *  [landmarks] 为 MediaPipe 33 点（归一化 0..1）；空 = 该帧无人。
 */
data class VideoFrame(
    val index: Int,
    val timestampMs: Long,
    val landmarks: List<com.quannian.zhidong.model.Landmark>
)

/**
 * 视频分析中"完成一次动作"的采样（per-rep quality，借鉴 RepDetect）。
 *  - [qualityScore]：该次动作的规范度（0-100，由该 rep 期间四维信号平均推算）。
 *  - [phaseAt]：完成时所处阶段文本。
 *  - [errors]：该次期间命中的错误类型。
 *  - [timestampMs]：完成时刻（供时间轴 / 回放定位）。
 */
data class RepSample(
    val repNumber: Int,
    val timestampMs: Long,
    val qualityScore: Int,
    val phaseAt: String,
    val errors: List<ErrorType>
)

/**
 * 时间轴上的一个事件（正常点 / 前倾点 / 浅蹲点 等）。
 *  [isError] 为 false = 正常/达标，true = 命中错误（[errorType] 非空）。
 */
data class TimelineEvent(
    val timestampMs: Long,
    val label: String,
    val errorType: ErrorType?,
    val isError: Boolean
)

/**
 * 一次离线视频动作分析的汇总结果（供报告页 / 时间轴 / 回放 / 前后对比使用）。
 *
 *  全部在**本机**完成，不上传服务器（spec §三十三）。
 */
data class VideoAnalysisResult(
    /** 抽取到的视频总时长（ms）。 */
    val durationMs: Long,
    /** 实际处理帧数。 */
    val frameCount: Int,
    /** 完成次数。 */
    val totalReps: Int,
    /** 综合评分 0-100（四维加权，复用 ScoreCalculator）。 */
    val overallScore: Int,
    /** 动作规范度 0-100（= 综合，展示用；非医学评估）。 */
    val formScore: Int,
    /** 各次动作质量采样（时间轴 + 每 rep 质量）。 */
    val repSamples: List<RepSample> = emptyList(),
    /** 时间轴事件（正常 + 错误定位）。 */
    val errorTimeline: List<TimelineEvent> = emptyList(),
    /** 命中过的错误类型（去重）。 */
    val errorTypesSeen: Set<ErrorType> = emptySet(),
    /** 做得好的地方（报告"✓"栏）。 */
    val highlights: List<String> = emptyList(),
    /** 需要改进（报告"⚠"栏）。 */
    val improvements: List<String> = emptyList(),
    /** 主错误（数字教练"针对纠正"用；取最高频）。 */
    val primaryError: ErrorType? = null
) {
    /** 是否存在值得纠正的主错误。 */
    val hasCorrection: Boolean get() = primaryError != null

    /** "改善前"分（前后对比的基准，取自本视频报告 overallScore）。 */
    val beforeScore: Int get() = overallScore
}
