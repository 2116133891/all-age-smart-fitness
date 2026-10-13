package com.quannian.zhidong

import com.quannian.zhidong.analyzer.ExerciseAnalyzerFactory
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.video.VideoAnalysisEngine
import com.quannian.zhidong.video.VideoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-1 视频分析"诚实结果"回归单测（纯 JVM，不依赖 Android）。
 *
 *  锁定 UI 依赖的契约（VideoAnalysisScreen）：
 *   - 全程无人 → 0 次、frameCount = 实际喂入帧数（"覆盖率"分母诚实，不伪造）。
 *   - 空/无人帧不崩、不计次（UI 用 landmarks 是否空判断"识别人体帧"）。
 *   - 混合（空帧 + 有人帧）→ 有人帧驱动计次，空帧被容忍。
 *   - 评分 only 在真实计次后有意义；无人 overallScore 应为保守值（非满分伪造）。
 */
class VideoAnalysisHonestTest {

    /** 一帧含人体（站立深蹲关键点）vs 无人帧（空 landmarks）。 */
    private fun personFrame(index: Int, tsMs: Long, deep: Boolean): VideoFrame {
        val mk = fun(i: Int, x: Float, y: Float) = Landmark(i, x, y, 0f, 1f)
        val hipX = if (deep) 0.30f else 0.44f
        val hipY = if (deep) 0.82f else 0.40f
        val kneeX = 0.44f
        val kneeY = if (deep) 0.72f else 0.70f
        val ankleX = 0.44f
        val ankleY = 0.94f
        val lms = mutableListOf<Landmark>()
        for (i in 0 until 33) {
            lms.add(when (i) {
                23 -> mk(i, hipX, hipY)      // L hip
                24 -> mk(i, 1f - hipX, hipY) // R hip
                25 -> mk(i, kneeX, kneeY)    // L knee
                26 -> mk(i, 1f - kneeX, kneeY)
                27 -> mk(i, ankleX, ankleY)  // L ankle
                28 -> mk(i, 1f - ankleX, ankleY)
                11 -> mk(i, 0.44f, 0.30f)    // L shoulder
                12 -> mk(i, 0.56f, 0.30f)    // R shoulder
                else -> mk(i, 0.5f, 0.5f)
            })
        }
        return VideoFrame(index, tsMs, lms)
    }

    private fun emptyFrame(index: Int, tsMs: Long) = VideoFrame(index, tsMs, emptyList())

    @Test
    fun `no person across all frames gives zero reps but honest frameCount`() {
        // 全部无人帧（landmarks 空）：UI 会把 personFrames=0 → 走 NO_PERSON 分支，不跳伪造报告。
        val frames = (0 until 24).map { emptyFrame(it, (it * 80).toLong()) }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        assertEquals("全程无人应为 0 次", 0, r.totalReps)
        assertEquals("frameCount 应等于实际喂入帧数（覆盖率分母诚实）", 0, r.frameCount)
        assertTrue("无人不应有 rep 采样", r.repSamples.isEmpty())
    }

    @Test
    fun `person frames count reps and empty frames are tolerated`() {
        // 混合：每 10 帧里夹 4 帧空镜（无人），其余为站立→下蹲→站立循环，应仍能计次。
        val frames = ArrayList<VideoFrame>()
        var i = 0
        repeat(3) {
            repeat(8) {
                if (i % 10 == 9) frames += emptyFrame(i, (i * 80).toLong())
                else frames += personFrame(i, (i * 80).toLong(), deep = false)
                i++
            }
            repeat(8) {
                if (i % 10 == 9) frames += emptyFrame(i, (i * 80).toLong())
                else frames += personFrame(i, (i * 80).toLong(), deep = true)
                i++
            }
            repeat(8) {
                if (i % 10 == 9) frames += emptyFrame(i, (i * 80).toLong())
                else frames += personFrame(i, (i * 80).toLong(), deep = false)
                i++
            }
        }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        // 有人帧驱动计次；空帧不崩、不污染。
        assertTrue("混合帧应能计到次数", r.totalReps >= 1)
        assertEquals("frameCount 等于含人体帧数（空帧不计入）",
            frames.count { it.landmarks.isNotEmpty() }, r.frameCount)
        assertTrue("时间轴非空", r.errorTimeline.isNotEmpty())
    }

    @Test
    fun `no person result must not fabricate a high overallScore`() {
        // 诚实：全程无人时综合评分应是保守值（0 分区间），而不是伪造的高分。
        val frames = (0 until 30).map { emptyFrame(it, (it * 80).toLong()) }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        assertTrue("无人结果应低分（不伪造）", r.overallScore < 40)
    }
}
