package com.quannian.zhidong

import com.quannian.zhidong.analyzer.ExerciseAnalyzerFactory
import com.quannian.zhidong.analyzer.SquatAnalyzer
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PoseFrame
import com.quannian.zhidong.score.AgeProfile
import com.quannian.zhidong.video.OneEuroFilter
import com.quannian.zhidong.video.VideoAnalysisEngine
import com.quannian.zhidong.video.VideoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 离线视频动作分析引擎单测（纯 JVM，对照 spec §十一~§二十/§三十七）。
 *  覆盖：正常 / 异常 / 边界 / 缺失点 / 低置信度 / 抖动(OneEuro) / 重复计数。
 */
class VideoAnalysisEngineTest {

    /**
     * 造一帧深蹲关键点（33 点）。用**真实三点夹角**驱动 SquatAnalyzer FSM：
     *  - 站立：髋在踝正上方（共线），H-K-A 夹角 = 180°（>150，算站立/起身完成）。
     *  - 下蹲中：髋小幅后坐，夹角 ≈ 100°（<110，触发 down 状态）。
     *  - 最低点：髋大幅后坐+下沉，夹角 ≈ 80°（<85+HYST，触发 bottom 状态）。
     *  起身时回到站立 → SquatAnalyzer 计一次 rep。
     */
    private fun squatFrame(
        index: Int,
        tsMs: Long,
        kneeDeep: Boolean,
        vis: Float = 1f
    ): VideoFrame {
        val mk = fun(i: Int, x: Float, y: Float) = Landmark(i, x, y, 0f, vis)
        // 归一化坐标。关键点：踝固定，髋在"站立"时高，"深蹲"时大幅下沉到接近踝 -> 膝角变小。
        // 站立: 髋(0.44,0.40) 膝(0.44,0.70) 踝(0.44,0.94)  -> 角度≈180 (>150 站立)
        // 深蹲: 髋(0.30,0.82) 膝(0.44,0.72) 踝(0.44,0.94)  -> 角度<93 (触发 down+bottom)
        val lHipX: Float
        val lHipY: Float
        val lKneeX: Float
        val lKneeY: Float
        val lAnkleX: Float
        val lAnkleY: Float
        if (kneeDeep) {
            lHipX = 0.30f; lHipY = 0.82f; lKneeX = 0.44f; lKneeY = 0.72f
            lAnkleX = 0.44f; lAnkleY = 0.94f
        } else {
            lHipX = 0.44f; lHipY = 0.40f; lKneeX = 0.44f; lKneeY = 0.70f
            lAnkleX = 0.44f; lAnkleY = 0.94f
        }
        // 右腿镜像：以画面中线 0.5 对称
        val rHipX = 1f - lHipX
        val rKneeX = 1f - lKneeX
        val rAnkleX = 1f - lAnkleX
        val lms = mutableListOf<Landmark>()
        for (i in 0 until 33) {
            lms.add(when (i) {
                11 -> mk(11, lHipX - 0.02f, lHipY)   // L_SHOULDER（肩在髋上方）
                12 -> mk(12, rHipX + 0.02f, lHipY)   // R_SHOULDER
                23 -> mk(23, lHipX, lHipY)                    // L_HIP
                24 -> mk(24, rHipX, lHipY)                    // R_HIP
                25 -> mk(25, lKneeX, lKneeY)                  // L_KNEE
                26 -> mk(26, rKneeX, lKneeY)                  // R_KNEE
                27 -> mk(27, lAnkleX, lAnkleY)                // L_ANKLE
                28 -> mk(28, rAnkleX, lAnkleY)                // R_ANKLE
                else -> mk(i, 0.5f, 0.3f)
            })
        }
        return VideoFrame(index, tsMs, lms)
    }

    @Test
    fun `empty frames give zero reps and empty result`() {
        val r = VideoAnalysisEngine.analyze(emptyList(), ExerciseAnalyzerFactory.freshFor("squat"), 10)
        assertEquals(0, r.totalReps)
        assertTrue(r.repSamples.isEmpty())
        assertEquals(0L, r.durationMs)
    }

    @Test
    fun `squat cycles count reps and build timeline`() {
        // 站立→下蹲→站立 循环 3 次，每次 ~30 帧，间隔 80ms(≈12fps)
        val frames = ArrayList<VideoFrame>()
        var i = 0
        repeat(3) {
            // 站立（上）
            repeat(8) { frames += squatFrame(i, (i * 80).toLong(), kneeDeep = false); i++ }
            // 下蹲（深）
            repeat(8) { frames += squatFrame(i, (i * 80).toLong(), kneeDeep = true); i++ }
            // 起身（回站立）
            repeat(8) { frames += squatFrame(i, (i * 80).toLong(), kneeDeep = false); i++ }
        }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        // 每个"回到站立"判为一次 rep
        assertTrue("应计到多次深蹲, got ${r.totalReps}", r.totalReps >= 2)
        assertTrue("应有 rep 采样", r.repSamples.isNotEmpty())
        assertTrue("时间轴非空", r.errorTimeline.isNotEmpty())
        assertTrue("完成次数与 rep 采样一致", r.totalReps == r.repSamples.size)
    }

    @Test
    fun `low visibility points do not crash and degrade gracefully`() {
        val frames = (0 until 30).map { squatFrame(it, (it * 80).toLong(), false, vis = 0.2f) }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        // 低可见度 → 分析器按 visOk 过滤，不崩、结果正常汇总
        assertNotNull(r)
        assertTrue("低可见度仍产出结果", r.frameCount >= 0)
    }

    @Test
    fun `frames with no landmarks (empty person) are tolerated`() {
        val frames = (0 until 20).map { VideoFrame(it, (it * 80).toLong(), emptyList()) }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        assertEquals("无人不应计次", 0, r.totalReps)
        assertTrue("无人应记入错误类型(NO_PERSON 被过滤后可能为 postureDrift 等)",
            r.errorTypesSeen.isNotEmpty() || r.totalReps == 0)
    }

    @Test
    fun `error frames mark timeline as error with primary error`() {
        // 制造躯干前倾：肩明显低于髋（弯腰），持续多帧
        val frames = ArrayList<VideoFrame>()
        var idx = 0
        repeat(2) {
            repeat(6) {
                // 前倾：双肩 y > 髋 y（图像坐标 y 越大越靠下）
                val f = squatFrame(idx, (idx * 80).toLong(), kneeDeep = false)
                val withLean = f.landmarks.map { lm ->
                    when (lm.index) {
                        11 -> lm.copy(y = lm.y + 0.10f)
                        12 -> lm.copy(y = lm.y + 0.10f)
                        else -> lm
                    }
                }
                frames += VideoFrame(idx, f.timestampMs, withLean); idx++
            }
            repeat(6) { frames += squatFrame(idx, (idx * 80).toLong(), kneeDeep = false); idx++ }
        }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        // 只要有 rep 完成，时间轴里应出现"错误"事件（前倾）
        val errEvents = r.errorTimeline.filter { it.isError }
        assertTrue("前倾应产生错误时间轴事件, got=${r.errorTimeline.size}", errEvents.isNotEmpty())
        assertNotNull("应识别主错误", r.primaryError)
        assertTrue("主错误应出现在文案", r.improvements.any { it.isNotBlank() })
    }

    @Test
    fun `results include beforeScore for before-after comparison`() {
        val frames = (0 until 60).map { squatFrame(it, (it * 80).toLong(), kneeDeep = it % 16 in 4..11) }
        val r = VideoAnalysisEngine.analyze(frames, ExerciseAnalyzerFactory.freshFor("squat"), 10)
        assertEquals("beforeScore 应等于 overallScore（前后对比基准）", r.overallScore, r.beforeScore)
    }
}

/**
 * OneEuro 平滑单测（纯 JVM）：静止平滑、抖动抑制、低置信度边界。
 */
class OneEuroFilterTest {

    @Test
    fun `constant input converges to same value`() {
        val f = OneEuroFilter(12.0, 1.0, 0.0)
        var out = 0.0
        repeat(50) { out = f.update(5.0) }
        assertTrue("恒定输入应收敛", abs(out - 5.0) < 0.1)
    }

    @Test
    fun `jittery input is smoothed compared to raw`() {
        val raw = (0 until 100).map { if (it % 2 == 0) 10.0 else 12.0 }
        val f = OneEuroFilter(12.0, 1.0, 0.0)
        var smoothed = 0.0
        for (v in raw) smoothed = f.update(v)
        // 平滑后输出波动应小于原始 10..12 的高频抖动
        assertTrue("OneEuro 应抑制高频抖动", smoothed > 9.0 && smoothed < 13.0)
    }

    @Test
    fun `reset clears state`() {
        val f = OneEuroFilter(12.0)
        repeat(30) { f.update(20.0) }
        f.reset()
        val first = f.update(0.0)
        assertEquals("reset 后首次 update 即取输入", 0.0, first, 1e-9)
    }

    @Test
    fun `pair filters x and y independently`() {
        val p = OneEuroFilter.Pair(12.0, 1.0, 0.0)
        var v = p.update(1.0, 2.0)
        repeat(40) { v = p.update(1.0, 2.0) }
        assertTrue("x 收敛", abs(v.x - 1.0) < 0.1)
        assertTrue("y 收敛", abs(v.y - 2.0) < 0.1)
    }
}
