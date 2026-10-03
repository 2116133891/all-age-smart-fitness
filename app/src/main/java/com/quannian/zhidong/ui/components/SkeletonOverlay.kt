package com.quannian.zhidong.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quannian.zhidong.analyzer.PoseIndex
import com.quannian.zhidong.model.Landmark

/** 33 点的人体骨架连线（左右各 5 条 + 躯干 2 条），用于在摄像头上画骨骼。 */
private val BONES: List<IntArray> = listOf(
    intArrayOf(PoseIndex.L_SHOULDER, PoseIndex.L_ELBOW),
    intArrayOf(PoseIndex.L_ELBOW, PoseIndex.L_WRIST),
    intArrayOf(PoseIndex.R_SHOULDER, PoseIndex.R_ELBOW),
    intArrayOf(PoseIndex.R_ELBOW, PoseIndex.R_WRIST),
    intArrayOf(PoseIndex.L_SHOULDER, PoseIndex.L_HIP),
    intArrayOf(PoseIndex.L_HIP, PoseIndex.L_KNEE),
    intArrayOf(PoseIndex.L_KNEE, PoseIndex.L_ANKLE),
    intArrayOf(PoseIndex.R_SHOULDER, PoseIndex.R_HIP),
    intArrayOf(PoseIndex.R_HIP, PoseIndex.R_KNEE),
    intArrayOf(PoseIndex.R_KNEE, PoseIndex.R_ANKLE),
    intArrayOf(PoseIndex.L_SHOULDER, PoseIndex.R_SHOULDER),
    intArrayOf(PoseIndex.L_HIP, PoseIndex.R_HIP),
    intArrayOf(PoseIndex.L_SHOULDER, PoseIndex.L_HIP),
    intArrayOf(PoseIndex.R_SHOULDER, PoseIndex.R_HIP)
)

/**
 * 在摄像头画面之上绘制人体骨骼。
 * [landmarks] 为归一化（0..1）33 关键点，[widthPx]/[heightPx] 为画布像素尺寸。
 */
@Composable
fun SkeletonOverlay(
    landmarks: List<Landmark>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF22D3EE),
    jointColor: Color = Color(0xFFFFD54A),
    lineStrokeDp: Dp = 4.dp,
    /** 前置摄像头为镜像画面，骨骼需水平翻转以与预览方向一致。 */
    mirror: Boolean = false
) {
    val byIndex = rememberByIndex(landmarks)
    Canvas(modifier = modifier.fillMaxSize()) {
        if (byIndex.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        fun toPoint(lm: Landmark): Offset {
            val x = if (mirror) (1f - lm.x) * w else lm.x * w
            return Offset(x, lm.y * h)
        }
        // 连线
        BONES.forEach { (a, b) ->
            val la = byIndex[a]; val lb = byIndex[b]
            if (la == null || lb == null) return@forEach
            if (la.visibility < 0.4f || lb.visibility < 0.4f) return@forEach
            drawLine(
                color = lineColor,
                start = toPoint(la),
                end = toPoint(lb),
                strokeWidth = lineStrokeDp.toPx()
            )
        }
        // 关节点
        byIndex.values.forEach { lm ->
            if (lm.visibility < 0.4f) return@forEach
            drawCircle(
                color = jointColor,
                radius = 3.dp.toPx(),
                center = toPoint(lm)
            )
        }
    }
}

private fun rememberByIndex(landmarks: List<Landmark>): Map<Int, Landmark> =
    landmarks.associateBy { it.index }
