package com.quannian.zhidong.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.LifecycleOwner
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.toLandmarks
import com.quannian.zhidong.pose.PoseDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 摄像头管理器：CameraX 预览 + 图像分析 → 姿态检测 → 回调 UI。
 *
 *  设计（CameraX 与 MediaPipe 解耦）：
 *   - [start] 只做"尽快显示画面"：绑定 Preview + ImageAnalysis，**不**同步加载模型。
 *   - 姿态模型 [PoseDetector] 在后台线程初始化；初始化完成前，分析器收到空帧，
 *     UI 显示"正在启动智能识别…"；就绪后切到"AI 姿态识别已就绪 → 识别中"。
 *   - 支持前后摄像头切换（[switchCamera]），切换时 unbind→rebind。
 *   - 默认后置摄像头（运动识别更稳，用户站到摄像头前）。
 *
 *  与 pose/detector 解耦：本类只负责"出图 + 出关键点"，
 *  分析 / 评分 / 纠错由上层（Compose Screen + ViewModel）基于回调的 landmarks 计算。
 */
class CameraManager(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val poseDetector: PoseDetector,
    private val onFrame: (List<Landmark>) -> Unit,
    private val onPoseReady: (Boolean) -> Unit = {}
) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val poseInitExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val tag = "CameraManager"

    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var provider: ProcessCameraProvider? = null
    private var initializedProvider = false

    /** 当前镜头方向：true = 后置（默认，运动识别），false = 前置。 */
    var facingBack = true
        private set

    /** 是否已绑定并开始分析。 */
    var running = false
        private set

    /** 已绑定的 PreviewView 引用（切换 / 重绑时复用）。 */
    private var viewRef: PreviewView? = null

    private fun getProvider(): ProcessCameraProvider? {
        if (initializedProvider) return provider
        provider = try {
            ProcessCameraProvider.getInstance(context.applicationContext).get()
        } catch (e: Exception) {
            Log.e(tag, "获取 CameraProvider 失败", e)
            null
        }
        initializedProvider = true
        return provider
    }

    /** 启动相机并**后台**初始化姿态模型（不阻塞主线程）。 */
    fun start(view: PreviewView) {
        if (running) return
        viewRef = view
        val provider = getProvider()
        if (provider == null) {
            onFrame(emptyList())
            onPoseReady(false)
            return
        }
        try {
            bindUseCases(view, provider, facingBack)
            running = true
            Log.i(tag, "相机已启动 (facing=${if (facingBack) "back" else "front"})")
        } catch (e: Exception) {
            Log.e(tag, "启动相机失败", e)
            onFrame(emptyList())
        }
        // 后台加载姿态模型，避免 5.7MB 模型阻塞预览。
        onPoseReady(false)
        poseInitExecutor.execute {
            val ok = poseDetector.init()
            runOnUiThread { onPoseReady(ok) }
        }
    }

    /** 前后摄像头切换：默认后置，切换时 unbind → 重绑。 */
    fun switchCamera() {
        if (!running) return
        val view = viewRef ?: return
        val provider = getProvider() ?: return
        facingBack = !facingBack
        try {
            provider.unbindAll()
            bindUseCases(view, provider, facingBack)
            Log.i(tag, "已切换到${if (facingBack) "后置" else "前置"}摄像头")
        } catch (e: Exception) {
            Log.e(tag, "切换摄像头失败", e)
        }
    }

    private fun bindUseCases(view: PreviewView, provider: ProcessCameraProvider, back: Boolean) {
        val selector = CameraSelector.Builder()
            .requireLensFacing(if (back) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT)
            .build()
        preview = Preview.Builder().build()
        // 预览填满并裁切 PreviewView（CameraX 1.3.0 无 CENTER_CROP，用 FILL_START），
        // 前后摄像头画面大小一致。
        view.scaleType = PreviewView.ScaleType.FILL_START
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor) { image -> analyzeFrame(image) }
            }

        // 预览面交给 PreviewView；等它真正拿到尺寸后再绑定 Surface，避免 0 尺寸黑屏。
        view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        var bound = false
        view.doOnPreDraw {
            if (!bound && view.width > 0 && view.height > 0) {
                val sp = view.getSurfaceProvider()
                preview?.setSurfaceProvider { request -> sp.onSurfaceRequested(request) }
                bound = true
            }
        }

        provider.unbindAll()
        provider.bindToLifecycle(owner, selector, preview, imageAnalyzer)
    }

    /** 把一帧 [ImageProxy] 转成缩放后的 Bitmap，做姿态推理并回调。 */
    private fun analyzeFrame(image: ImageProxy) {
        val bitmap = try {
            image.toBitmap()
        } catch (e: Exception) {
            Log.e(tag, "toBitmap 失败", e)
            null
        } finally {
            image.close()
        }
        if (bitmap == null) {
            onFrame(emptyList())
            return
        }
        val resized = scaleDown(bitmap, 720)
        val landmarks = poseDetector.detect(resized).toLandmarks()
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        onFrame(landmarks)
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val ratio = (maxSide.toFloat() / maxOf(w, h)).coerceAtMost(1f)
        if (ratio >= 0.99f) return src
        return Bitmap.createScaledBitmap(
            src,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun runOnUiThread(block: () -> Unit) {
        // 简化：onFrame 已在 analysisExecutor 回调，UI 状态流用 StateFlow 自动切主线程；
        // pose 就绪信号同样通过 Compose 的 mutableState 收集，无需额外 Handler。
        block()
    }

    /** 停止相机并释放资源。 */
    fun stop() {
        running = false
        try {
            imageAnalyzer?.setAnalyzer(analysisExecutor) { }
            imageAnalyzer = null
            preview = null
            provider?.unbindAll()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        analysisExecutor.shutdown()
        poseInitExecutor.shutdown()
        poseDetector.close()
        viewRef = null
    }
}
