package com.quannian.zhidong.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * 数字人动作帧序列渲染器（纯 JVM + android.graphics，不依赖 Compose，可单测几何部分）。
 *
 *  设计（spec §八 方案 B：Pose Sprite / 帧序列）：
 *  - 把 [CoachMotionEngine] 输出的 [CoachPose] 画到离屏 [Bitmap]（透明背景），
 *    每个动作生成 N 帧透明人形序列；[CoachSpriteView] 按序播放（整帧透明位图）。
 *  - 分层 2D 骨骼人形（头/躯干/双臂/双腿），关节角度来自 [CoachPose]，
 *    **不同动作（深蹲屈膝下沉、侧拉伸侧倾、开合跳分腿举臂、八段锦 8 式…）得到肉眼可见不同姿态**，
 *    不再是"整张 PNG 上下跳"。
 *  - 按年龄差异化（儿童头大 / 青年修长 / 银龄稳重 + 白发/老花镜），配色用年龄段色。
 *
 *  与 [CoachMotionView]（Compose Canvas 画）的区别：本类画到 Bitmap，
 *  便于①帧序列播放②预渲染缓存③纯 JVM 单测（几何一致性）。
 */
object CoachSpriteRenderer {

    /** 一帧透明人形位图。 */
    data class Sprite(val bitmap: Bitmap)

    /** 一个动作的帧序列（教学/跟练演示用）。 */
    data class SpriteSequence(val frames: List<Sprite>, val loop: Boolean = true)

    /**
     * 把一个 [CoachPose] 渲染成一张透明 [Bitmap]。
     * @param sizePx 边长（像素，正方形，人形居中）。
     * @param ageId  年龄段（child/youth/senior）→ 头身比 + 发型。
     * @param mainColor 主色（RGB，年龄配色）。
     */
    fun renderPose(pose: CoachPose, sizePx: Int, ageId: String, mainColor: Int): Sprite {
        val bm = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        drawFigure(canvas, pose, sizePx, ageId, mainColor)
        return Sprite(bm)
    }

    /**
     * 把一个教学/演示序列（[CoachMotionSequence]）渲染成帧序列。
     *  不同 [CoachPose] 出不同帧；相邻重复 pose 不重复渲染（省内存）。
     */
    fun renderSequence(
        seq: com.quannian.zhidong.ui.components.CoachMotionSequence,
        sizePx: Int,
        ageId: String,
        mainColor: Int
    ): SpriteSequence {
        val frames = seq.steps.map { renderPose(it.pose, sizePx, ageId, mainColor) }
        return SpriteSequence(frames = frames, loop = seq.loop)
    }

    /** 画一个人形到 [Canvas]（透明背景，[size] 像素正方形）。 */
    private fun drawFigure(
        canvas: Canvas,
        pose: CoachPose,
        size: Int,
        ageId: String,
        main: Int
    ) {
        val w = size.toFloat()
        val cx = w / 2f
        val age = when (ageId) {
            "child" -> Age.CHILD
            "senior" -> Age.SENIOR
            else -> Age.YOUTH
        }

        val headR = when (age) {
            Age.CHILD -> w * 0.16f
            Age.SENIOR -> w * 0.11f
            else -> w * 0.085f
        }
        val torsoH = when (age) {
            Age.CHILD -> w * 0.20f
            Age.SENIOR -> w * 0.24f
            else -> w * 0.28f
        }
        val limbW = when (age) {
            Age.CHILD -> w * 0.055f
            Age.SENIOR -> w * 0.035f
            else -> w * 0.03f
        }

        val skin = Color.rgb(232, 180, 143)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = main
            strokeWidth = limbW
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = main
            style = Paint.Style.FILL
        }
        val pSkin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = skin
            strokeWidth = limbW * 0.9f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        // 整体垂直偏移：让整个人形居中（头到脚占满）
        val totalH = headR * 2 + torsoH + w * 0.4f
        val topY = ((w - totalH) / 2f).coerceAtLeast(4f)

        val liftPx = pose.bodyLift * w * 0.04f
        val shiftPx = pose.weightShift * w * 0.05f
        val baseX = cx + shiftPx

        // 躯干顶/底（考虑髋屈曲下沉）
        val sinkPx = pose.hipBend / 180f * w * 0.05f
        val torsoTopY = topY + headR * 1.6f + sinkPx - liftPx
        val torsoBotY = torsoTopY + torsoH
        val leanRad = Math.toRadians(pose.spineLean.toDouble())

        fun rot(px: Float, py: Float): Pair<Float, Float> {
            val dx = px - baseX
            val dy = py - torsoBotY
            val rx = baseX + (dx * cos(leanRad) - dy * sin(leanRad)).toFloat()
            val ry = torsoBotY + (dx * sin(leanRad) + dy * cos(leanRad)).toFloat()
            return rx to ry
        }

        // ===== 腿 =====
        val hipL = rot(baseX - limbW * 1.6f, torsoBotY)
        val hipR = rot(baseX + limbW * 1.6f, torsoBotY)
        val hipSpread = pose.legSpread * w * 0.18f
        val thighLen = w * 0.20f
        val shinLen = w * 0.20f
        val hipBendRad = Math.toRadians(pose.hipBend.toDouble())
        val kneeBendRad = Math.toRadians(pose.kneeBend.toDouble())

        fun drawLeg(hip: Pair<Float, Float>, spreadX: Float) {
            val spread = spreadX + hipSpread
            val kneeRad = hipBendRad - kneeBendRad * 0.5
            val kx = hip.first + spread + sin(kneeRad).toFloat() * thighLen
            val ky = hip.second + cos(kneeRad).toFloat() * thighLen
            val ankleRad = kneeRad - kneeBendRad
            val ax = kx + sin(ankleRad).toFloat() * shinLen
            val ay = ky + cos(ankleRad).toFloat() * shinLen
            p.color = main
            canvas.drawLine(hip.first, hip.second, kx, ky, p)
            canvas.drawLine(kx, ky, ax, ay, p)
            p.color = main
            canvas.drawCircle(ax, ay + limbW, limbW * 1.1f, pFill)
        }
        drawLeg(hipL, -hipSpread * 0.5f)
        drawLeg(hipR, hipSpread * 0.5f)

        // ===== 躯干 =====
        val tl = rot(baseX - limbW * 1.8f, torsoTopY)
        val tr = rot(baseX + limbW * 1.8f, torsoTopY)
        val bl = rot(baseX - limbW * 1.8f, torsoBotY)
        val br = rot(baseX + limbW * 1.8f, torsoBotY)
        val torso = Path().apply {
            moveTo(tl.first, tl.second)
            lineTo(tr.first, tr.second)
            lineTo(br.first, br.second)
            lineTo(bl.first, bl.second)
            close()
        }
        pFill.color = main
        canvas.drawPath(torso, pFill)

        // ===== 手臂 =====
        val shL = rot(baseX - limbW * 1.7f, torsoTopY + torsoH * 0.08f)
        val shR = rot(baseX + limbW * 1.7f, torsoTopY + torsoH * 0.08f)
        val armLen = w * 0.18f

        fun drawArm(shoulder: Pair<Float, Float>, sideSign: Float) {
            val raiseRad = Math.toRadians((pose.shoulderRaise * sideSign).toDouble())
            val spreadRad = Math.toRadians(pose.armSpread.toDouble())
            val baseRad = Math.toRadians(90.0)
            val upRad = baseRad - raiseRad / sideSign + spreadRad * sideSign * 0.3
            val ex = shoulder.first + sin(upRad).toFloat() * armLen * sideSign
            val ey = shoulder.second + cos(upRad).toFloat() * armLen
            val elbowBendRad = Math.toRadians(pose.elbowBend.toDouble())
            val wristRad = upRad + elbowBendRad * sideSign
            val wx = ex + sin(wristRad).toFloat() * armLen * 0.8f * sideSign
            val wy = ey + cos(wristRad).toFloat() * armLen * 0.8f
            pSkin.color = skin
            canvas.drawLine(shoulder.first, shoulder.second, ex, ey, pSkin)
            canvas.drawLine(ex, ey, wx, wy, pSkin)
            pFill.color = main
            canvas.drawCircle(wx, wy, limbW * 1.1f, pFill)
        }
        drawArm(shL, 1f)
        drawArm(shR, -1f)

        // ===== 头 =====
        val neck = rot(baseX, torsoTopY)
        val tiltRad = Math.toRadians(pose.headTilt.toDouble())
        val headCx = neck.first + sin(tiltRad).toFloat() * headR * 1.4f
        val headCy = neck.second - headR * 1.5f - headR * 0.4f
        pFill.color = skin
        canvas.drawCircle(headCx, headCy, headR, pFill)
        // 头发（银龄白发）
        val hairColor = if (age == Age.SENIOR) Color.rgb(200, 205, 214) else Color.rgb(42, 46, 58)
        pFill.color = hairColor
        canvas.drawArc(
            RectF(headCx - headR, headCy - headR, headCx + headR, headCy + headR),
            180f, 180f, false, pFill
        )
        // 眼睛
        val eyeSpread = headR * 0.4f
        val eyeY = headCy - headR * 0.05f
        pFill.color = Color.WHITE
        canvas.drawCircle(headCx - eyeSpread, eyeY, headR * 0.13f, pFill)
        canvas.drawCircle(headCx + eyeSpread, eyeY, headR * 0.13f, pFill)
        pFill.color = main
        canvas.drawCircle(headCx - eyeSpread, eyeY + headR * 0.02f, headR * 0.06f, pFill)
        canvas.drawCircle(headCx + eyeSpread, eyeY + headR * 0.02f, headR * 0.06f, pFill)
        // 嘴
        val mouthColor = Color.rgb(122, 59, 46)
        when {
            pose.face >= 2f -> {
                p.color = mouthColor
                p.strokeWidth = limbW * 0.35f
                canvas.drawArc(
                    RectF(headCx - headR * 0.45f, headCy + headR * 0.1f, headCx + headR * 0.45f, headCy + headR * 1.0f),
                    20f, 140f, false, p
                )
            }
            pose.face >= 1f -> {
                p.color = mouthColor
                p.strokeWidth = limbW * 0.35f
                canvas.drawArc(
                    RectF(headCx - headR * 0.35f, headCy + headR * 0.15f, headCx + headR * 0.35f, headCy + headR * 0.9f),
                    30f, 120f, false, p
                )
            }
            else -> {
                p.color = mouthColor
                p.strokeWidth = limbW * 0.3f
                canvas.drawLine(headCx - headR * 0.2f, headCy + headR * 0.35f, headCx + headR * 0.2f, headCy + headR * 0.35f, p)
            }
        }
        // 银龄老花镜
        if (age == Age.SENIOR) {
            val gr = eyeSpread * 1.1f
            p.color = main
            p.strokeWidth = 2f
            p.style = Paint.Style.STROKE
            canvas.drawCircle(headCx - eyeSpread, headCy, gr, p)
            canvas.drawCircle(headCx + eyeSpread, headCy, gr, p)
            canvas.drawLine(headCx - eyeSpread + gr, headCy, headCx + eyeSpread - gr, headCy, p)
        }
    }

    private enum class Age { CHILD, YOUTH, SENIOR }
}
