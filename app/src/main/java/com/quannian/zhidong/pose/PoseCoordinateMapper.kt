package com.quannian.zhidong.pose

import com.quannian.zhidong.model.Landmark

/**
 * 姿态坐标映射（纯逻辑，JVM 可测）—— 任务 17/10：旋转 / 镜像 / 坐标诊断。
 *
 *  把"源帧归一化关键点（0..1）"映射到"画布像素坐标"，显式处理两个常见错位来源：
 *   1) 可见裁切：分析流（源帧，通常 4:3）与预览流（PreviewView，FILL_START + COMPATIBLE）
 *      宽高比不同，预览是源帧的中央裁切。可见区用 [Crop]（源帧归一化 0..1 矩形）描述，
 *      关键点必须先落进该矩形再放大到画布，否则骨架与真人身体错位。
 *   2) 前置镜像：前置摄像头预览对用户呈"镜面"，画布需与预览做同样的水平翻转，
 *      与 [CameraManager] 对**分析帧**的镜像保持一致（分析帧镜像后检测到的坐标已处于
 *      "镜像画面"坐标系，画布再翻转一次即与预览镜像画面严格重合）。
 *
 *  设计成纯 Kotlin（无 android.* / Compose 依赖），以便纯 JVM 单测覆盖映射正确性；
 *  旋转（ImageProxy → upright）由 [CameraManager] 在出图阶段处理（本类只消费已 upright 的帧）。
 */

/** 源帧归一化（0..1）可见裁切矩形。对应 android.graphics.RectF，但此处用纯数据类以便 JVM 测试。 */
data class Crop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        /** 全帧（未裁切）。ViewPort 未就绪或 scaleType=FIT 时用。 */
        val FULL = Crop(0f, 0f, 1f, 1f)
    }

    /** 从 android.graphics.RectF 转换（UI 层桥接；JVM 测试不触达 android.*）。 */
    fun fromRectF(r: android.graphics.RectF): Crop =
        Crop(r.left, r.top, r.right, r.bottom)
}

/** 画布像素坐标。 */
data class CanvasPoint(val x: Float, val y: Float)

/**
 * 源帧归一化关键点 → 画布像素。
 *
 *  映射两步：
 *   1) 源帧归一化点 (lm.x, lm.y) → 可见裁切内像素：
 *        px0 = (crop.left + lm.x * crop.width)  * canvasW
 *        py  = (crop.top  + lm.y * crop.height) * canvasH
 *   2) 前置摄像头（[mirror]=true）画布水平翻转：px = canvasW - px0。
 *
 *  约定：[lm] 已基于"upright 且（前置时）已镜像"的分析帧检测得到，
 *  因此本函数不再做旋转、也不再做第二次镜像（避免双重翻转错位）。
 */
object PoseCoordinateMapper {

    fun toCanvasPoint(
        lm: Landmark,
        crop: Crop,
        canvasW: Float,
        canvasH: Float,
        mirror: Boolean
    ): CanvasPoint {
        val px0 = (crop.left + lm.x * crop.width) * canvasW
        val py = (crop.top + lm.y * crop.height) * canvasH
        val px = if (mirror) canvasW - px0 else px0
        return CanvasPoint(px, py)
    }

    /** 批量映射（骨架连线 / 关节点共用同一套规则）。 */
    fun toCanvasPoints(
        landmarks: List<Landmark>,
        crop: Crop,
        canvasW: Float,
        canvasH: Float,
        mirror: Boolean
    ): List<CanvasPoint> =
        landmarks.map { toCanvasPoint(it, crop, canvasW, canvasH, mirror) }

    /**
     * 诊断：给定一个关键点，若按"全帧"映射与"按 crop 映射"差异超过阈值，
     * 说明预览裁切显著，返回 true（提示 UI 用 crop 而非全帧，避免错位）。
     */
    fun cropMatters(lm: Landmark, crop: Crop, tolerance: Float = 0.02f): Boolean {
        val full = toCanvasPoint(lm, Crop.FULL, 1f, 1f, mirror = false)
        val cropped = toCanvasPoint(lm, crop, 1f, 1f, mirror = false)
        return kotlin.math.abs(cropped.x - full.x) > tolerance ||
            kotlin.math.abs(cropped.y - full.y) > tolerance
    }
}
