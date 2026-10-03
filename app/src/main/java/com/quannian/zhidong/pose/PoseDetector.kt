package com.quannian.zhidong.pose

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * 姿态检测器：封装 MediaPipe Pose Landmarker（本地运行，不依赖后端）。
 *
 *  职责：
 *  1. 从 assets 加载 pose_landmarker.task（AndroidAssetUtil -> ByteBuffer -> createFromBuffer）。
 *  2. 对每帧 [Bitmap] 构造 RGB 的 [MPImage]，视频模式推理，输出归一化（0..1）33 个关键点。
 *
 *  本类只做"人体姿态检测"，不含任何动作判断（那是 analyzer 的职责）。
 */
class PoseDetector(private val context: Context) {

    private var landmarker: PoseLandmarker? = null
    private var initialized = false
    private val tag = "PoseDetector"

    /** 最近一次成功使用的帧时间戳。detectForVideo 要求单调递增，避免相同/回退时间戳报错。 */
    private var lastTimestampNs = 0L

    companion object {
        const val MODEL_ASSET = "pose_landmarker.task"
    }

    /** 初始化：从 assets 加载模型并创建 landmarker。@return 成功返回 true。 */
    @Synchronized
    fun init(): Boolean {
        if (initialized) return true
        return try {
            // 用标准 Android asset 读取模型字节，替代 AndroidAssetUtil。
            // AndroidAssetUtil（来自 tasks-core）的 native 方法没有对应 .so 被打进 APK，
            // 直接调用会 UnsatisfiedLinkError 崩溃；改用 InputStream 完全绕开它。
            val modelBytes: ByteArray = context.assets.open(MODEL_ASSET).use { input: InputStream ->
                val buf = ByteArray(input.available())
                input.read(buf)
                buf
            }
            // MediaPipe 要求 setModelAssetBuffer 必须是 direct（off-heap）或 Mapped ByteBuffer，
            // ByteBuffer.wrap(ByteArray) 是堆内存 buffer，会抛 IllegalArgumentException。
            val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply { put(modelBytes) }
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetBuffer(modelBuffer)
                        .build()
                )
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .build()
            landmarker = PoseLandmarker.createFromOptions(context, options)
            initialized = true
            Log.i(tag, "Pose landmarker 初始化成功")
            true
        } catch (e: Exception) {
            Log.e(tag, "Pose landmarker 初始化失败", e)
            false
        }
    }

    fun isReady() = initialized

    /** 释放资源。 */
    @Synchronized
    fun close() {
        try {
            landmarker?.close()
        } catch (_: Exception) {
        }
        landmarker = null
        initialized = false
    }

    /**
     * 对一帧推理。返回归一化 33 关键点（单人）。无人时返回空列表。
     * @param bitmap 已缩放的摄像头帧（建议 720p 以下以省电）。
     * @param timestampNs 本帧单调递增时间戳（纳秒）；若未传则内部自增。
     */
    fun detect(bitmap: Bitmap, timestampNs: Long? = null): List<NormalizedLandmark> {
        val lm = landmarker ?: return emptyList()
        // detectForVideo 要求严格单调递增的时间戳；相同或回退会抛异常。
        val ts = (timestampNs ?: System.nanoTime()).let { cand ->
            if (cand > lastTimestampNs) cand else lastTimestampNs + 1
        }
        lastTimestampNs = ts
        return try {
            val mpImage = bitmapToMPImage(bitmap)
            val result: PoseLandmarkerResult =
                lm.detectForVideo(mpImage, ts)
            mpImage.close()
            result.landmarks().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "detect 出错 ts=$ts", e)
            emptyList()
        }
    }

    /** 把 [Bitmap]（ARGB_8888）转成 [MPImage]（RGBA），MediaPipe 直接消费。 */
    private fun bitmapToMPImage(bitmap: Bitmap): MPImage =
        BitmapImageBuilder(bitmap).build()
}
