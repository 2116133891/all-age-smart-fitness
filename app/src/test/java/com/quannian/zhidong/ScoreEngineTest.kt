package com.quannian.zhidong

import com.quannian.zhidong.score.CorrectionEngine
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.score.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 评分 / 纠错引擎的纯逻辑单测（不依赖 Android，可在 JVM 上运行）。
 */
class ScoreEngineTest {

    @Test
    fun `perfect performance yields high overall score`() {
        val b = ScoreCalculator.calculate(
            repCount = 10,
            targetReps = 10,
            avgDepth = 1f,
            postureSteady = 1f,
            symSteady = 1f,
            errorCount = 0
        )
        assertTrue("综合分应接近满分", b.overall >= 95)
        assertEquals(100.0f, b.rangeScore, 0.01f)
        assertEquals(100.0f, b.postureScore, 0.01f)
        assertEquals(100.0f, b.stabilityScore, 0.01f)
        assertEquals(100.0f, b.completenessScore, 0.01f)
    }

    @Test
    fun `low depth lowers range and overall`() {
        val b = ScoreCalculator.calculate(
            repCount = 3,
            targetReps = 10,
            avgDepth = 0.2f,
            postureSteady = 0.9f,
            symSteady = 0.9f,
            errorCount = 5
        )
        assertTrue("幅度不足时综合分应明显偏低", b.overall < 60)
    }

    @Test
    fun `stateOf buckets correctly`() {
        assertEquals("动作规范，表现优秀", ScoreCalculator.stateOf(90))
        assertEquals("基本规范，继续保持", ScoreCalculator.stateOf(75))
        assertEquals("需要调整，注意细节", ScoreCalculator.stateOf(55))
        assertEquals("动作偏差较大，重新练习", ScoreCalculator.stateOf(30))
    }

    @Test
    fun `correction engine flags shallow range`() {
        val errors = CorrectionEngine.evaluate(
            CorrectionEngine.FrameSignals(
                personPresent = true,
                depthRatio = 0.3f,
                torsoTilt = 5f,
                symmetryRatio = 0.9f,
                hasFullPose = true
            )
        )
        assertTrue(errors.contains(ErrorType.RANGE_TOO_SHALLOW))
    }

    @Test
    fun `correction engine flags forward lean`() {
        val errors = CorrectionEngine.evaluate(
            CorrectionEngine.FrameSignals(
                personPresent = true,
                depthRatio = 0.9f,
                torsoTilt = 30f,
                symmetryRatio = 0.9f,
                hasFullPose = true
            )
        )
        assertTrue(errors.contains(ErrorType.BODY_TOO_FORWARD))
    }

    @Test
    fun `no person triggers NO_PERSON`() {
        val errors = CorrectionEngine.evaluate(
            CorrectionEngine.FrameSignals(false, 0f, 0f, 0f, false)
        )
        assertEquals(listOf(ErrorType.NO_PERSON), errors)
    }

    @Test
    fun `top suggestion picks first error`() {
        val sug = CorrectionEngine.topSuggestion(
            setOf(ErrorType.ARMS_TOO_LOW, ErrorType.UNSTABLE)
        )
        assertEquals(ErrorType.ARMS_TOO_LOW.suggestion, sug)
    }
}
