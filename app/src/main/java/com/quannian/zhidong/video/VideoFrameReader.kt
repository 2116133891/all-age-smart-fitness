package com.quannian.zhidong.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.toLandmarks
import com.quannian.zhidong.pose.PoseDetector
import java.io.File

/**
 * 离线视频抽帧器（Android，本机处理，不上传服务器）。
 *
 *  流程（spec §十二/§十四）：
 *   1. 用户通过系统选择器拿到本地视频 [Uri]（或 [File]）。
 *   2. [MediaMetadataRetriever] 读取 width/height/duration。
 *   3. 按 ~[fps] 抽帧（默认 12fps，避免整段每帧跑 MediaPipe 拖卡）：
 *      `getFrameAtTime(tMs, OPTION_CLOSEST)` → 该帧截图 `Bitmap`。
 *   4. 逐帧交 MediaPipe（[PoseDetector]，VIDEO 模式）出 33 点，打包 [VideoFrame]。
 *
 *  **默认本地处理**（spec §三十三）：全程不离开设备，UI 明示「视频仅在本机分析」。
 *
 *  说明：`MediaMetadataRetriever.getFrameAtTime` 抽的是**该时刻的画面帧**（用于
 *  姿态识别足够；若需精确解码帧可用 Media3/MediaCodec，本 MVP 用 MMR 足够演示且更稳）。
 */
class VideoFrameReader(private val context: Context) {

    companion object {
        const val TAG = "VideoFrameReader"
        /** 抽帧帧率（spec §三十六：10~15 FPS，取 12）。 */
        const val DEFAULT_FPS = 12
        /** 分析时长上限（秒）。超过则只分析前 N 秒，避免长视频拖卡。 */
        const val MAX_ANALYZE_SECONDS = 60
    }

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val frameCount: Int,
        val sampleIntervalMs: Long
    )

    /** 读视频元信息（宽高 / 时长 / 按 [fps] 抽帧将得到多少帧）。 */
    fun inspect(uri: Uri): VideoInfo {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val interval = 1000L / fpsCoerce(fps = DEFAULT_FPS)
            val frames = if (dur <= 0) 0 else ((dur / interval).toInt() + 1)
            return VideoInfo(w, h, dur, frames, interval)
        } finally {
            release(mmr)
        }
    }

    private fun fpsCoerce(fps: Int) = fps.coerceIn(1, 30)

    /**
     * 抽取并分析：按 [fps] 抽帧 → 每帧 MediaPipe 出 33 点 → [VideoFrame] 序列。
     *
     * @param detector 已 init 的 [PoseDetector]（VIDEO 模式，时间戳自动单调）。
     * @param isCancelled 取消判定（用户点"取消分析"后置 true）；命中即在下一帧前停止，
     *                    返回已处理到的帧供 UI 显示"已停/重选"，并让调用方 finally 释放检测器。
     * @param onProgress 进度回调（已处理帧数 / 总帧数 / 已识别人体帧数），供 UI 实时进度条与"覆盖率"。
     * @return 抽帧得到的关键点序列（时间升序），可直接喂 [VideoAnalysisEngine.analyze]。
     */
    fun extractFrames(
        uri: Uri,
        detector: PoseDetector,
        fps: Int = DEFAULT_FPS,
        isCancelled: () -> Boolean = { false },
        onProgress: (done: Int, total: Int, personFrames: Int) -> Unit = { _, _, _ -> }
    ): List<VideoFrame> {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (dur <= 0L) return emptyList()

            val effDurMs = minOf(dur, MAX_ANALYZE_SECONDS * 1000L)
            val interval = 1000L / fpsCoerce(fps)
            val total = (effDurMs / interval).toInt() + 1

            val frames = ArrayList<VideoFrame>(total)
            var personFrames = 0
            for (i in 0 until total) {
                // 用户取消：下一帧前停止，返回已处理帧，让调用方 finally 释放检测器。
                if (isCancelled()) {
                    onProgress(i + 1, total, personFrames)
                    break
                }
                val tMs = (i * interval).coerceAtMost(effDurMs)
                // MediaPipe VIDEO 要求严格单调递增时间戳：用递增帧序号（ms 语义），
                // 而非真实 tMs（抽帧间隔固定，帧序号即等距，严格单调且 > lastTs）。
                val lastTs = i * 1000L + 1
                val bitmap = grabFrame(mmr, tMs)
                if (bitmap != null) {
                    val landmarks = try {
                        detector.detect(bitmap, lastTs).toLandmarks()
                    } catch (e: Exception) {
                        Log.w(TAG, "第 ${i} 帧检测失败", e)
                        emptyList()
                    }
                    if (!bitmap.isRecycled) bitmap.recycle()
                    // 该帧识别人体才计入"覆盖率"（personFrames），无人帧 landmarks 为空。
                    if (landmarks.isNotEmpty()) personFrames++
                    frames += VideoFrame(i, tMs, landmarks)
                }
                onProgress(i + 1, total, personFrames)
            }
            return frames
        } catch (e: Exception) {
            Log.e(TAG, "视频抽帧失败", e)
            return emptyList()
        } finally {
            release(mmr)
        }
    }

    /** 抓某时刻画面帧（缩小到 720p 以下以省电，与摄像头同口径）。 */
    private fun grabFrame(mmr: MediaMetadataRetriever, tMs: Long): Bitmap? {
        return try {
            val src = mmr.getFrameAtTime(tMs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            scaleDown(src, 720)
        } catch (e: Exception) {
            Log.w(TAG, "getFrameAtTime 失败 t=$tMs", e)
            null
        }
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val ratio = (maxSide.toFloat() / maxOf(w, h)).coerceAtMost(1f)
        if (ratio >= 0.99f) return src
        return try {
            Bitmap.createScaledBitmap(src, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
                .also { src.recycle() }
        } catch (_: Exception) {
            src
        }
    }

    /** 抽一帧封面缩略图（视频预览用，不检测）。 */
    fun thumbnail(uri: Uri, maxSide: Int = 480): Bitmap? {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val b = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            return scaleDown(b, maxSide)
        } catch (e: Exception) {
            Log.w(TAG, "取缩略图失败", e)
            return null
        } finally {
            release(mmr)
        }
    }

    private fun release(mmr: MediaMetadataRetriever) {
        try {
            mmr.release()
        } catch (_: Exception) {
        }
    }
}
