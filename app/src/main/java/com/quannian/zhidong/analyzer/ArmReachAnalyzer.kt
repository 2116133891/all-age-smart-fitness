package com.quannian.zhidong.analyzer

import com.quannian.zhidong.model.JointAngle
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.PhaseState
import com.quannian.zhidong.model.PoseFrame

/**
 * 抬臂 / 肩颈拉伸 / 八段锦"两手托天" 通用分析器。
 *
 * 原理：
 *  - 用 肩肘腕 夹角判断抬臂幅度（手臂越接近伸直上举，角度越大）。
 *  - 状态机：down（手臂放下）-> up（手臂上举，完成一次）-> down。
 *  - 完成一次 = 从放下抬到目标高度再放下。
 */
class ArmReachAnalyzer : PoseAnalyzer {

    companion object {
        const val UP_DEG = 150f        // 手臂夹角接近伸直 + 上举
        const val DOWN_DEG = 90f
    }

    override fun key() = "reach"

    private var state = "down"

    override fun analyze(frame: PoseFrame, prevFrame: PoseFrame?): PhaseState {
        val lm = frame.landmarks.associateBy { it.index }
        if (frame.landmarks.isEmpty()) return PhaseState("无人", false)

        val lArm = armAngle(lm)
        val rArm = armAngle(r(lm))
        val avg = (lArm + rArm) / 2f

        return when (state) {
            "down" -> {
                if (avg >= UP_DEG) { state = "up"; PhaseState("上举中", false) }
                else PhaseState("放下", false)
            }
            "up" -> {
                if (avg < DOWN_DEG) { state = "down"; PhaseState("完成一次", true) }
                else PhaseState("保持上举", false)
            }
            else -> { state = "down"; PhaseState("放下", false) }
        }
    }

    private fun r(lm: Map<Int, Landmark>): Map<Int, Landmark> {
        // 右侧镜像：用右肩右肘右腕
        val remap = mutableMapOf<Int, Landmark>()
        lm.forEach { (k, v) ->
            val mk = when (k) {
                PoseIndex.R_SHOULDER -> PoseIndex.L_SHOULDER
                PoseIndex.R_ELBOW -> PoseIndex.L_ELBOW
                PoseIndex.R_WRIST -> PoseIndex.L_WRIST
                else -> k
            }
            remap[mk] = v
        }
        return remap
    }

    private fun armAngle(lm: Map<Int, Landmark>): Float {
        val s = lm[PoseIndex.L_SHOULDER]; val e = lm[PoseIndex.L_ELBOW]; val w = lm[PoseIndex.L_WRIST]
        if (s == null || e == null || w == null) return 0f
        return AngleUtils.angleOf(s, e, w)
    }

    override fun jointAngles(frame: PoseFrame): List<JointAngle> {
        val lm = frame.landmarks.associateBy { it.index }
        val la = AngleUtils.angleOfSafe(lm[PoseIndex.L_SHOULDER], lm[PoseIndex.L_ELBOW], lm[PoseIndex.L_WRIST])
        val ra = AngleUtils.angleOfSafe(lm[PoseIndex.R_SHOULDER], lm[PoseIndex.R_ELBOW], lm[PoseIndex.R_WRIST])
        return listOf(
            JointAngle("左臂夹角", la),
            JointAngle("右臂夹角", ra),
            JointAngle("平均抬臂幅度", (la + ra) / 2f)
        )
    }
}
