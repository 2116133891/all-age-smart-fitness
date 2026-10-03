package com.quannian.zhidong.coach

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.TrainingReport

/**
 * 本地教练话术生成器（默认 Provider，离线可用）。
 *
 *  用规则 + 模板生成动态自然语言，引用实时数据（次数/分数/具体错误），
 *  比"预设固定文案"更能体现"AI 教练"。话术风格按年龄段微调。
 */
object LocalCoachProvider : CoachAIProvider {

    private fun tone(age: String): String = when (age) {
        "child" -> "小运动家"
        "senior" -> "您"
        else -> "你"
    }

    override fun correctionFor(exercise: String, age: String, errors: Set<ErrorType>, score: Int): List<String> {
        if (errors.isEmpty()) {
            return listOf("${tone(age)}做得很好，继续保持这个节奏！")
        }
        // 按优先级取前 2 条，引用实时数据
        val top = errors.take(2)
        return top.map { err ->
            val who = tone(age)
            when (err) {
                ErrorType.BODY_TOO_FORWARD ->
                    "$who 这一${if (score >= 70) "些动作幅度很好，" else ""}起身时躯干有前倾，注意收腹、臀部向后坐，让背部保持挺直。"
                ErrorType.RANGE_TOO_SHALLOW ->
                    "$who 动作幅度可以再深一点，下蹲到膝盖接近 90° 再起身，让肌肉充分参与。"
                ErrorType.UNSTABLE ->
                    "$who 重心有点不稳，双脚与肩同宽、膝盖与脚尖同方向，动作放慢一些。"
                ErrorType.ARMS_TOO_LOW ->
                    "$who 手臂/抬举幅度偏低，试着把动作做到位，感受目标部位的拉伸。"
                ErrorType.KNEE_ANGLE_INVALID ->
                    "$who 膝盖有内扣倾向，让膝盖始终朝脚尖方向，起身时不要锁死。"
                ErrorType.SPEED_TOO_FAST ->
                    "$who 节奏偏快，动作均匀、配合呼吸，${if (age == "senior") "尤其要慢" else "会更稳"}。"
                ErrorType.POSTURE_DRIFT ->
                    "$who 身体重心左右有偏移，保持中立位，左右对称地完成动作。"
                ErrorType.NO_PERSON ->
                    "请${who}站到摄像头前，保持全身入镜，进来就开始计数。"
            }
        }
    }

    override fun generateSummary(report: TrainingReport): String {
        val who = tone(report.ageGroup)
        val rep = if (report.repCount >= report.targetReps) "完成了全部 ${report.targetReps} 次目标动作"
        else "完成了 ${report.repCount}/${report.targetReps} 次"
        val quality = when {
            report.overallScore >= 85 -> "动作非常规范，表现优秀"
            report.overallScore >= 70 -> "动作整体规范，表现良好"
            report.overallScore >= 50 -> "动作基本到位，还能进一步打磨"
            else -> "动作还有明显改进空间"
        }
        val weak = weakestDimension(report)
        return "本次 ${report.exerciseName} 训练${rep}，综合评分 ${report.overallScore} 分，$quality。主要短板在${weak}，下次重点加强这一项。"
    }

    override fun nextSuggestion(report: TrainingReport): String {
        val who = tone(report.ageGroup)
        return when {
            report.repCount >= report.targetReps && report.overallScore >= 85 ->
                "${who}已经掌握得很好了，下次可以尝试增加次数或提高难度，${who}进步很快！"
            report.overallScore >= 70 ->
                "下次保持节奏、重点${weakestDimension(report)}的改进，${who}的动作会更标准。"
            else ->
                "先放慢速度、把${weakestDimension(report)}做标准，${who}不要追求次数，质量优先。"
        }
    }

    private fun weakestDimension(r: TrainingReport): String {
        val dims = listOf(
            "动作幅度" to r.rangeScore,
            "动作姿态" to r.postureScore,
            "动作稳定" to r.stabilityScore,
            "动作完整度" to r.completenessScore
        )
        return dims.minByOrNull { it.second }?.first ?: "动作姿态"
    }
}
