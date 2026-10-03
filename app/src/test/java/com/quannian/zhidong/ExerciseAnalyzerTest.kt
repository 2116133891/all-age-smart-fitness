package com.quannian.zhidong

import com.quannian.zhidong.analyzer.BaduanjinAnalyzer
import com.quannian.zhidong.analyzer.ExerciseAnalyzerFactory
import com.quannian.zhidong.analyzer.GentleStretchAnalyzer
import com.quannian.zhidong.analyzer.JumpingJackAnalyzer
import com.quannian.zhidong.analyzer.JumpRopeAnalyzer
import com.quannian.zhidong.analyzer.NeckStretchAnalyzer
import com.quannian.zhidong.analyzer.StretchAnalyzer
import com.quannian.zhidong.analyzer.SquatAnalyzer
import com.quannian.zhidong.analyzer.TaiChiAnalyzer
import com.quannian.zhidong.analyzer.YogaAnalyzer
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 各动作分析器的 JVM 单测（对照 spec §33）。
 *
 *  用合成的 [PoseFrame]（模拟 33 关键点）驱动每个分析器，验证：
 *  - 正常姿态 / 错误姿态
 *  - 状态切换（standing→down→bottom→up）
 *  - 重复计数
 *  - 缺失 landmark / 低置信度不崩
 *  - 边界角度
 *
 *  不依赖 Android，纯 JVM 可跑。
 */
class ExerciseAnalyzerTest {

    private val factory = ExerciseAnalyzerFactory

    /** 构造一个带"站立"姿态的 [PoseFrame]。 */
    private fun standingFrame(ts: Long = 0L): PoseFrame =
        PoseFrame(ts, standingLandmarks(), true)

    // ============ 通用健壮性 ============

    @Test
    fun `empty landmarks do not crash and report no person`() {
        val empty = PoseFrame(0, emptyList(), false)
        val keys = listOf("squat", "jumprope", "jack", "stretch", "shoulder", "yoga", "baduanjin", "taichi", "easy")
        for (key in keys) {
            val result = factory.forKey(key).analyze(empty, null)
            assertTrue("$key should flag NO_PERSON", result.errors.contains(ErrorType.NO_PERSON))
        }
    }

    @Test
    fun `single side missing does not crash`() {
        val lm = mutableListOf<Landmark>()
        lm.add(Landmark(0, 0.5f, 0.1f))
        lm.add(Landmark(11, 0.42f, 0.35f))
        lm.add(Landmark(12, 0.58f, 0.35f))
        lm.add(Landmark(23, 0.44f, 0.6f))
        val frame = PoseFrame(0, lm, true)
        val result = factory.forKey("squat").analyze(frame, null)
        assertNotNull(result)
    }

    @Test
    fun `all analyzers report a phase and do not throw`() {
        val keys = listOf("squat", "jumprope", "jack", "stretch", "shoulder", "yoga", "baduanjin", "taichi", "easy")
        for (key in keys) {
            val frame = standingFrame()
            val result = factory.forKey(key).analyze(frame, null)
            assertNotNull("phase for $key", result.phase)
            assertNotNull("jointAngles for $key", result.jointAngles)
        }
    }

    // ============ 深蹲 ============

    @Test
    fun `squat state machine progresses standing to down to bottom to up`() {
        val a = SquatAnalyzer()
        var prev: com.quannian.zhidong.model.PoseFrame? = null
        // 1) 站立
        val f0 = standingFrame(0)
        val r0 = a.analyze(f0, null); prev = f0
        assertEquals("站立准备", r0.phase.phase)
        // 2) 开始下蹲（让 avgKnee 足够小）
        val f1 = PoseFrame(1, squatLandmarks(95f), true)
        val r1 = a.analyze(f1, prev); prev = f1
        assertTrue("should be 下蹲中 (got ${r1.phase.phase})", r1.phase.phase.contains("下蹲"))
        // 3) 最低点（让 avgKnee 更小）
        val f2 = PoseFrame(2, squatLandmarks(70f), true)
        val r2 = a.analyze(f2, prev); prev = f2
        assertTrue("should be 最低点 (got ${r2.phase.phase})", r2.phase.phase.contains("最低点"))
        // 4) 起身完成（回到站立 → isRep）
        val f3 = PoseFrame(3, standingLandmarks(), true)
        val r3 = a.analyze(f3, prev); prev = f3
        assertTrue("rising back to standing should count a rep (got ${r3.phase.phase})", r3.phase.isRep)
    }

    @Test
    fun `shallow squat has low depth`() {
        val a = SquatAnalyzer()
        a.analyze(standingFrame(0), null)
        // 130° 浅蹲：offset=0.20, avgKnee≈85° → depth=(160-85)/70=1.07 → 不低
        // 改用 150° 浅蹲（接近直腿）：offset=0.05, avgKnee≈173° → depth=(160-173)/70<0 → 0
        val shallow = a.analyze(PoseFrame(1, squatLandmarks(150f), true), null)
        assertFalse("shallow shouldn't mark bottom yet", shallow.phase.phase.contains("最低点"))
        assertTrue("shallow depth should be low", shallow.depth < 0.5f)
    }

    @Test
    fun `squat forward lean flags BODY_TOO_FORWARD`() {
        val a = SquatAnalyzer()
        a.analyze(standingFrame(0), null)
        val leaning = PoseFrame(1, squatLandmarks(100f, 30f), true)
        val r = a.analyze(leaning, null)
        assertTrue("leaning should flag BODY_TOO_FORWARD", r.errors.contains(ErrorType.BODY_TOO_FORWARD))
    }

    // ============ 跳绳 ============

    @Test
    fun `jumprope counts jumps with de-bounce`() {
        val a = JumpRopeAnalyzer()
        val jump = a.analyze(PoseFrame(1000, jumpLandmarks(true), true), null)
        a.analyze(PoseFrame(1400, jumpLandmarks(false), true), null)
        assertTrue("airborne should report 腾空/起跳", jump.phase.phase.contains("腾空") || jump.phase.phase.contains("起跳"))
        assertTrue("jumps should be counted", a.currentJumps() >= 1)
    }

    @Test
    fun `jumprope de-bounce ignores sub-minimum interval jumps`() {
        val a = JumpRopeAnalyzer()
        a.analyze(PoseFrame(1000, jumpLandmarks(true), true), null)
        val first = a.currentJumps()
        a.analyze(PoseFrame(1010, jumpLandmarks(true), true), null)
        assertEquals("sub-interval jump should not count", first, a.currentJumps())
    }

    // ============ 开合跳 ============

    @Test
    fun `jumping jack closed to open to closed counts one rep`() {
        val a = JumpingJackAnalyzer()
        // 模拟一个完整的 closed → open → closed 循环
        // 关键：open 时腿和手臂都要超过阈值，closed 时都要低于阈值
        val closed = PoseFrame(0, jackLandmarks(false), true)
        a.analyze(closed, null)  // state: closed → closed（保持）

        val open = PoseFrame(1, jackLandmarks(true), true)
        val rOpen = a.analyze(open, closed)  // state: closed → open
        assertTrue("open phase should be 开跳 (got ${rOpen.phase.phase})", rOpen.phase.phase.contains("开跳"))

        val closedAgain = PoseFrame(2, jackLandmarks(false), true)
        val rClosed = a.analyze(closedAgain, open)  // state: open → closed（isRep）
        assertTrue("closed→open→closed should be a rep (got ${rClosed.phase.phase})", rClosed.phase.isRep)
    }

    // ============ 八段锦 ============

    @Test
    fun `baduanjin displays current move subLabel`() {
        val a = BaduanjinAnalyzer()
        val r = a.analyze(PoseFrame(0, baduanjinLandmarks(true), true), null)
        assertNotNull("subLabel should show move", r.subLabel)
        assertTrue("subLabel should contain 八段锦", r.subLabel!!.contains("八段锦"))
        assertTrue("subLabel should contain 第1式", r.subLabel!!.contains("第1式"))
    }

    @Test
    fun `baduanjin all 8 move names present`() {
        assertEquals(8, BaduanjinAnalyzer.MOVE_NAMES.size)
    }

    // ============ 太极 ============

    @Test
    fun `taichi arm raise moves to hold`() {
        val a = TaiChiAnalyzer()
        val r = a.analyze(PoseFrame(0, taiChiLandmarks(true), true), null)
        assertTrue("should be in 起势/推掌保持", r.phase.phase.contains("起势") || r.phase.phase.contains("推掌"))
    }

    // ============ 肩颈拉伸 ============

    @Test
    fun `neck stretch reports a side phase`() {
        val a = NeckStretchAnalyzer()
        a.analyze(standingFrame(0), null)
        val tilted = a.analyze(PoseFrame(1, neckLandmarks(true), true), null)
        val ok = tilted.phase.phase.contains("侧倾") || tilted.phase.phase.contains("耸肩") || tilted.phase.phase.contains("保持")
        assertTrue("should report a side/hold phase", ok)
    }

    // ============ 瑜伽 ============

    @Test
    fun `yoga side bend enters held`() {
        val a = YogaAnalyzer()
        var prev: PoseFrame? = null
        val f0 = standingFrame(0); prev = f0
        a.analyze(f0, null)
        val bent = PoseFrame(1, yogaLandmarks(true), true); prev = bent
        val r = a.analyze(bent, prev)
        val ok = r.phase.phase.contains("侧倾") || r.phase.phase.contains("保持")
        assertTrue("should enter held/bent", ok)
    }

    // ============ 银龄舒缓拉伸 ============

    @Test
    fun `gentle stretch reports a hold phase`() {
        val a = GentleStretchAnalyzer()
        a.analyze(standingFrame(0), null)
        val up = a.analyze(PoseFrame(1, stretchLandmarks(true), true), null)
        val ok = up.phase.phase.contains("上举") || up.phase.phase.contains("缓慢") || up.phase.phase.contains("保持")
        assertTrue("should report a hold phase", ok)
    }

    // ============ 儿童拉伸 ============

    @Test
    fun `child stretch reaches up`() {
        val a = StretchAnalyzer()
        a.analyze(standingFrame(0), null)
        val up = a.analyze(PoseFrame(1, stretchLandmarks(true), true), null)
        assertTrue("should be 上举中", up.phase.phase.contains("上举"))
    }

    // ============================================================
    // 姿态构造辅助（合成 33 点）
    // ============================================================

    private fun standingLandmarks(): List<Landmark> {
        val lm = MutableList(33) { Landmark(it, 0.5f, it * 0.02f) }
        setLandmark(lm, 11, 0.4f, 0.3f)
        setLandmark(lm, 12, 0.6f, 0.3f)
        setLandmark(lm, 13, 0.36f, 0.4f)
        setLandmark(lm, 14, 0.64f, 0.4f)
        setLandmark(lm, 15, 0.34f, 0.5f)
        setLandmark(lm, 16, 0.66f, 0.5f)
        setLandmark(lm, 23, 0.42f, 0.55f)
        setLandmark(lm, 24, 0.58f, 0.55f)
        setLandmark(lm, 25, 0.43f, 0.72f)
        setLandmark(lm, 26, 0.57f, 0.72f)
        setLandmark(lm, 27, 0.44f, 0.9f)
        setLandmark(lm, 28, 0.56f, 0.9f)
        return lm
    }

    private fun setLandmark(lm: MutableList<Landmark>, idx: Int, x: Float, y: Float) {
        lm[idx] = Landmark(idx, x, y, 1f)
    }

    private fun squatLandmarks(kneeDeg: Float, torsoLeanDeg: Float = 0f): List<Landmark> {
        // 真实膝角（AngleUtils.angleOf 实测校准，两腿对称前移）：
        //   offset=0.00→~176°(直腿) 0.10→~117° 0.15→~97° 0.22→~71° 0.25→~66°
        // 映射：kneeDeg 越高（越直）offset 越小
        val offset = when {
            kneeDeg >= 150f -> 0.00f   // 直腿/浅蹲 ~176°
            kneeDeg >= 105f -> 0.10f    // ~117°
            kneeDeg >= 88f  -> 0.15f    // ~97°
            kneeDeg >= 75f  -> 0.22f    // ~71°
            else            -> 0.25f    // 最深 ~66°
        }
        val lm = standingLandmarks().toMutableList()
        setLandmark(lm, 23, 0.42f, 0.45f)            // L_HIP
        setLandmark(lm, 24, 0.58f, 0.45f)            // R_HIP
        setLandmark(lm, 25, 0.42f + offset, 0.55f)   // L_KNEE 前移
        setLandmark(lm, 26, 0.58f + offset, 0.55f)   // R_KNEE 前移
        setLandmark(lm, 27, 0.44f, 0.85f)            // L_ANKLE
        setLandmark(lm, 28, 0.56f, 0.85f)            // R_ANKLE
        if (torsoLeanDeg > 0f) {
            val shift = torsoLeanDeg / 30f * 0.15f
            setLandmark(lm, 11, 0.40f + shift, 0.30f)  // L_SHOULDER 前移
            setLandmark(lm, 12, 0.60f + shift, 0.30f)  // R_SHOULDER 前移
        }
        return lm
    }

    private fun jumpLandmarks(airborne: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        val lift = if (airborne) 0.12f else 0.02f
        setLandmark(lm, 27, 0.44f, 0.9f - lift)
        setLandmark(lm, 28, 0.56f, 0.9f - lift)
        return lm
    }

    private fun jackLandmarks(open: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (open) {
            // 双腿外展（踝远离中线）+ 双臂上举（腕到头顶）
            setLandmark(lm, 27, 0.32f, 0.85f)   // L_ANKLE
            setLandmark(lm, 28, 0.68f, 0.85f)   // R_ANKLE
            setLandmark(lm, 11, 0.38f, 0.30f)   // L_SHOULDER
            setLandmark(lm, 12, 0.62f, 0.30f)   // R_SHOULDER
            setLandmark(lm, 13, 0.34f, 0.18f)   // L_ELBOW 上提
            setLandmark(lm, 14, 0.66f, 0.18f)   // R_ELBOW 上提
            setLandmark(lm, 15, 0.30f, 0.06f)   // L_WRIST 头顶
            setLandmark(lm, 16, 0.70f, 0.06f)   // R_WRIST 头顶
        } else {
            // 双腿并拢（踝近中线）+ 双臂屈肘下垂（腕回到肩同高 = 肘角 <80°）
            setLandmark(lm, 27, 0.49f, 0.85f)   // L_ANKLE
            setLandmark(lm, 28, 0.51f, 0.85f)   // R_ANKLE
            setLandmark(lm, 13, 0.44f, 0.48f)   // L_ELBOW 外展
            setLandmark(lm, 14, 0.56f, 0.48f)   // R_ELBOW 内收
            setLandmark(lm, 15, 0.44f, 0.30f)   // L_WRIST 回到肩高（肘角 ~12°）
            setLandmark(lm, 16, 0.56f, 0.30f)   // R_WRIST 回到肩高
        }
        return lm
    }

    private fun baduanjinLandmarks(armUp: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (armUp) {
            setLandmark(lm, 15, 0.4f, 0.05f)
            setLandmark(lm, 16, 0.6f, 0.05f)
        }
        return lm
    }

    private fun taiChiLandmarks(armUp: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (armUp) {
            setLandmark(lm, 15, 0.42f, 0.2f)
            setLandmark(lm, 16, 0.58f, 0.2f)
        }
        return lm
    }

    private fun neckLandmarks(tilted: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (tilted) setLandmark(lm, 0, 0.42f, 0.12f)
        return lm
    }

    private fun yogaLandmarks(bent: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (bent) {
            // 左侧倾：左肩向左大幅偏移 + 髋中点右偏 → 躯干侧倾 ≥18°
            setLandmark(lm, 23, 0.42f, 0.55f)   // L_HIP
            setLandmark(lm, 24, 0.58f, 0.55f)   // R_HIP
            setLandmark(lm, 11, 0.15f, 0.10f)   // L_SHOULDER 大幅左移
            setLandmark(lm, 12, 0.58f, 0.32f)   // R_SHOULDER
            setLandmark(lm, 13, 0.18f, 0.05f)   // L_ELBOW
            setLandmark(lm, 14, 0.62f, 0.40f)   // R_ELBOW
            setLandmark(lm, 15, 0.15f, 0.02f)   // L_WRIST
            setLandmark(lm, 16, 0.64f, 0.45f)   // R_WRIST
        }
        return lm
    }

    private fun stretchLandmarks(up: Boolean): List<Landmark> {
        val lm = standingLandmarks().toMutableList()
        if (up) {
            setLandmark(lm, 15, 0.35f, 0.08f)
            setLandmark(lm, 16, 0.65f, 0.08f)
        }
        return lm
    }
}
