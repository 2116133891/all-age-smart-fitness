package com.quannian.zhidong

import com.quannian.zhidong.ui.components.CoachMotionEngine
import com.quannian.zhidong.ui.components.CoachPose
import com.quannian.zhidong.ui.components.CoachState
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 数字人动作引擎单测（纯 JVM，对照 spec §八~§十六）：
 *  - 不同动作产生**肉眼可见不同**的关节姿态（不是统一上下跳）
 *  - 八段锦 8 式姿态两两不同
 *  - 深蹲按阶段有屈膝/最低点/起身差异
 *  - 纠错/鼓励状态有专属姿态
 */
class CoachMotionEngineTest {

    private fun distinct(p1: CoachPose, p2: CoachPose): Boolean =
        p1.shoulderRaise != p2.shoulderRaise ||
            p1.hipBend != p2.hipBend ||
            p1.kneeBend != p2.kneeBend ||
            p1.spineLean != p2.spineLean ||
            p1.headTilt != p2.headTilt ||
            p1.legSpread != p2.legSpread ||
            p1.bodyLift != p2.bodyLift

    // ============ 不同动作明显不同（核心验收 §八~§十六）============

    @Test
    fun `squat and yoga and jumping jack differ in pose`() {
        val squat = CoachMotionEngine.poseFor("squat", "下蹲中", CoachState.DEMO, "youth")
        val yoga = CoachMotionEngine.poseFor("yoga", "侧倾保持中", CoachState.DEMO, "youth")
        val jack = CoachMotionEngine.poseFor("jack", "开跳", CoachState.DEMO, "child")
        assertTrue("squat vs yoga 应不同", distinct(squat, yoga))
        assertTrue("squat vs jack 应不同", distinct(squat, jack))
        assertTrue("yoga vs jack 应不同", distinct(yoga, jack))
    }

    @Test
    fun `squat shows knee bend in down phase but not standing`() {
        val standing = CoachMotionEngine.squat("站立准备", CoachState.DEMO, 0.85f)
        val bottom = CoachMotionEngine.squat("最低点", CoachState.DEMO, 0.85f)
        assertTrue("站立时膝盖应伸直", standing.kneeBend < 10f)
        assertTrue("最低点应明显屈膝", bottom.kneeBend > 60f)
        assertTrue("最低点屈髋", bottom.hipBend > 60f)
    }

    @Test
    fun `neck stretch shows head tilt, others don't`() {
        val tilt = CoachMotionEngine.neckStretch("头颈侧倾中", CoachState.DEMO)
        val squat = CoachMotionEngine.squat("下蹲中", CoachState.DEMO, 0.85f)
        assertTrue("肩颈拉伸头应侧倾", tilt.headTilt > 15f)
        assertTrue("深蹲头不应侧倾", squat.headTilt == 0f)
    }

    @Test
    fun `jumping jack open phase spreads legs and raises arms`() {
        val open = CoachMotionEngine.jumpingJack("开跳", CoachState.DEMO)
        val closed = CoachMotionEngine.jumpingJack("并拢准备", CoachState.DEMO)
        assertTrue("开跳分腿", open.legSpread > closed.legSpread)
        assertTrue("开跳举臂", open.shoulderRaise > closed.shoulderRaise)
        assertTrue("开跳有离地", open.bodyLift > 0f)
    }

    @Test
    fun `jump rope has small bounce not large jump`() {
        val airborne = CoachMotionEngine.jumpRope("腾空", CoachState.DEMO)
        val ready = CoachMotionEngine.jumpRope("准备起跳", CoachState.DEMO)
        assertTrue("跳绳腾空有小幅离地", airborne.bodyLift > ready.bodyLift)
        assertTrue("离地幅度应小（小跳）", airborne.bodyLift <= 0.6f)
    }

    @Test
    fun `baduanjin 8 moves are all visually distinct`() {
        val poses = (0 until 8).map { CoachMotionEngine.baduanjin(it, CoachState.DEMO, 0.55f) }
        // 任意两式都应不同
        for (i in 0 until 8) {
            for (j in (i + 1) until 8) {
                assertTrue("八段锦第${i + 1}式与第${j + 1}式应不同",
                    distinct(poses[i], poses[j]) || poses[i].armSpread != poses[j].armSpread || poses[i].face != poses[j].face)
            }
        }
    }

    @Test
    fun `tai chi has multi-phase poses`() {
        val start = CoachMotionEngine.taiChi("起势", CoachState.DEMO, 0.55f)
        val push = CoachMotionEngine.taiChi("推掌保持", CoachState.DEMO, 0.55f)
        val cloud = CoachMotionEngine.taiChi("重心移动中", CoachState.DEMO, 0.55f)
        assertTrue("起势 vs 推掌 不同", distinct(start, push))
        assertTrue("推掌 vs 云手 不同", distinct(push, cloud))
        assertTrue("云手重心偏移", cloud.weightShift != 0f)
    }

    @Test
    fun `correction state differs from good state`() {
        val correct = CoachMotionEngine.squat("下蹲中", CoachState.CORRECT, 0.85f)
        val good = CoachMotionEngine.squat("下蹲中", CoachState.GOOD, 0.85f)
        assertTrue("纠错与鼓励姿态应不同", distinct(correct, good))
        assertTrue("GOOD 应站直举臂鼓励", good.shoulderRaise > 100f)
    }

    @Test
    fun `teaching sequence returns multiple distinct steps`() {
        val sq = CoachMotionEngine.teachingSequence("squat", "youth", "squat")
        assertTrue("深蹲教学序列 ≥4 步", sq.steps.size >= 4)
        assertTrue("序列带同步解说文案", sq.steps.all { !it.message.isNullOrBlank() })
        val neck = CoachMotionEngine.teachingSequence("shoulder", "youth", "shoulder")
        assertTrue("肩颈教学序列有侧倾步", neck.steps.any { it.pose.headTilt != 0f })
    }
}
