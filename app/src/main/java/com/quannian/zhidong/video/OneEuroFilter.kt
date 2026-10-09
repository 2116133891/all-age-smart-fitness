package com.quannian.zhidong.video

import kotlin.math.sqrt

/**
 * 一维 One Euro Filter（通用低通+高通联合滤波器，纯 JVM 可单测）。
 *
 *  参考 Casiez 等人的 One Euro Filter（开源、MIT 级通用算法；IO_motion 等项目广泛使用）。
 *  特点：静止时平滑（抑制 MediaPipe 关键点抖动），运动快时跟手（自适应）。
 *  参数：
 *   - [minCutoff]  截止频率下限（Hz），越小越平滑。一般 1.0。
 *   - [beta]      速度系数，越大越快跟手。一般 0.0~0.5。
 *   - [samplingRate] 采样帧率（Hz），= 1000 / frameIntervalMs。
 *
 *  用法：每个关键点索引一条 filter；对 33 点各起 x/y 两条，即 66 个 [OneEuroFilter]。
 *  本类**不依赖 Android**，可直接用纯 JVM 单测（正常/抖动/边界/低置信度）。
 */
class OneEuroFilter(
    private val samplingRate: Double,
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.0
) {
    private var xHat: Double = 0.0
    private var dxHat: Double = 0.0
    private var initialized = false

    /** 提交一个采样，返回滤波后的值。 */
    fun update(value: Double): Double {
        if (!initialized) {
            xHat = value
            dxHat = 0.0
            initialized = true
            return xHat
        }
        val alpha = alpha(1.0 / samplingRate.coerceAtLeast(1e-6))
        val xRaw = value
        val dx = (xRaw - xHat) / (1.0 / samplingRate.coerceAtLeast(1e-6))
        val aD = alpha(minCutoff + beta * Math.abs(dx))
        dxHat = (1 - aD) * dx + aD * dxHat
        val xPredict = xHat + dxHat
        xHat = (1 - alpha) * xRaw + alpha * xPredict
        return xHat
    }

    private fun alpha(cutoff: Double): Double {
        val tau = 1.0 / (2.0 * Math.PI * (cutoff.coerceAtLeast(0.01)))
        return 1.0 / (1.0 + tau * samplingRate)
    }

    fun reset() {
        xHat = 0.0
        dxHat = 0.0
        initialized = false
    }

    /** 二维版本：对同一关键点 (x, y) 分别滤波。 */
    class Pair(
        samplingRate: Double,
        minCutoff: Double = 1.0,
        beta: Double = 0.0
    ) {
        val fx = OneEuroFilter(samplingRate, minCutoff, beta)
        val fy = OneEuroFilter(samplingRate, minCutoff, beta)
        fun update(x: Double, y: Double): PairValue =
            PairValue(fx.update(x), fy.update(y))

        fun reset() {
            fx.reset()
            fy.reset()
        }
    }

    data class PairValue(val x: Double, val y: Double)
}
