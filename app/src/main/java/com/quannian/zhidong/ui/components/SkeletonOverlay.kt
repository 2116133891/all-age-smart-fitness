package com.quannian.zhidong.ui.components

import android.graphics.RectF
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
 *
 *  关键点由 MediaPipe 对**分析流原始帧**（通常 4:3）检测，归一化到该帧的
 *  0..1 坐标系；而 [PreviewView]（FILL_START + COMPATIBLE）把**预览流**拉伸填满
 *  并裁切，可见区域由 [crop]（`PreviewView.getPreviewCrop()`，源帧归一化矩形）描述。
 *  因此不能简单用 `lm.x * canvasW`，否则骨架与真人身体会错位。
 *
 *  映射规则（参考 vmalikov/pose-detection-android 的 crop 映射做法）：
 *   1. 源帧归一化点 (lx, ly) → 像素 = (crop.left + lx * crop.width) * canvasW
 *   2. 前置摄像头（[mirror]）：Canvas 与 PreviewView 一样都水平镜像，
 *      检测前已将分析帧镜像，故检测坐标即画面坐标，仅对 canvas 做同样的
 *      水平翻转，使骨架与预览镜像画面严格重合。
 *
 * @param landmarks 归一化（0..1，源帧坐标）33 关键点
 * @param crop      PreviewView 的可见裁切矩形（源帧归一化坐标），null 时按全帧处理
 * @param mirror    是否水平镜像（前置摄像头传 true）
 */
@Composable
fun SkeletonOverlay(
    landmarks: List<Landmark>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF22D3EE),
    jointColor: Color = Color(0xFFFFD54A),
    lineStrokeDp: Dp = 4.dp,
    crop: RectF? = null,
    mirror: Boolean = false
) {
    val byIndex = rememberByIndex(landmarks)
    val c = crop ?: RectF(0f, 0f, 1f, 1f)
    Canvas(modifier = modifier.fillMaxSize()) {
        if (byIndex.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        fun toPoint(lm: Landmark): Offset {
            // 1) 源帧归一化 → 画布像素（含裁切）
            var px = (c.left + lm.x * c.width()) * w
            val py = (c.top + lm.y * c.height()) * h
            // 2) 前置镜像
            if (mirror) px = w - px
            return Offset(px, py)
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
