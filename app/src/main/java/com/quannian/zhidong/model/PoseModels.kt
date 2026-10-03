package com.quannian.zhidong.model

/**
 * 年龄段定义。三个入口对应不同的数字人教练与运动内容。
 *
 *  - [title]：入口卡片主标题
 *  - [subtitle]：副标题（气质词，如"活泼 · 明亮 · 运动感"）
 *  - [tagline]：竞赛版首页展示的行为标签（如"轻运动 · 快乐成长"）
 */
enum class AgeGroup(
    val id: String,
    val title: String,
    val subtitle: String,
    val tagline: String
) {
    CHILD("child", "儿童运动教练", "活泼 · 明亮 · 运动感", "轻运动 · 快乐成长"),
    YOUTH("youth", "青年运动教练", "科技 · 简洁 · 现代", "科学运动 · 姿态纠正"),
    SENIOR("senior", "银龄运动教练", "稳重 · 温和 · 健康感", "舒缓运动 · 健康相伴")
}

/**
 * 单个运动项目。每个 AgeGroup 下有若干 Exercise，点击后进入数字人教学页。
 */
data class Exercise(
    val id: String,
    val ageGroup: AgeGroup,
    val name: String,
    val icon: String,            // 用于卡片视觉的 emoji / 简码
    val tagline: String,         // 一句话介绍
    val duration: String,        // 建议时长
    val level: String,           // 难度
    val steps: List<String>,     // 动作步骤
    val cautions: List<String>,  // 注意事项
    val coachIntro: String,      // 数字人开场白
    /** 该动作对应的姿态分析器 key（见 analyzer.AnalysisKind）。null 表示暂未接入实时检测。 */
    val analysisKind: String?,
    /** 实时检测目标次数 */
    val targetReps: Int
)

/**
 * 单个姿态关键点的归一化坐标与可见度。
 */
data class Landmark(
    val index: Int,
    val x: Float,               // 0..1 归一化
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f
)

/**
 * 一帧的姿态数据：若干关键点。
 */
data class PoseFrame(
    val timestampMs: Long,
    val landmarks: List<Landmark>,
    val detected: Boolean
)

/**
 * 把 MediaPipe 归一化关键点转成本项目的 [Landmark] 列表。
 *
 *  健壮性处理：
 *  - 校验数量：MediaPipe 单姿态固定输出 33 点；若数量异常，仅取前 33 并过滤，
 *    避免后续按索引访问越界。
 *  - 缺失/低置信度：保留原索引（index 不变），仅把低可见度点的 visibility 归一，
 *    让分析器可按 visibility 判断"该侧是否可信"，而不是让一个坏点把整帧拖垮。
 */
const val POSE_LANDMARK_COUNT = 33
const val MIN_LANDMARK_VISIBILITY = 0.3f

fun List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>.toLandmarks(): List<Landmark> =
    mapIndexed { i, lm ->
        Landmark(
            index = i,
            x = lm.x(),
            y = lm.y(),
            z = lm.z(),
            visibility = lm.visibility().orElse(1f).coerceIn(0f, 1f)
        )
    }.filter { it.index in 0 until POSE_LANDMARK_COUNT }

/**
 * 关节角度计算结果。角度单位：度。
 */
data class JointAngle(
    val label: String,
    val degrees: Float
)

/**
 * 一次动作阶段识别结果。
 */
data class PhaseState(
    val phase: String,          // 例如 "下蹲中" / "最低点" / "起身中" / "保持"
    val isRep: Boolean         // 本帧是否判定为一次完成
)
