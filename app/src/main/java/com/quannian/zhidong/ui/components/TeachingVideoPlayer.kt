package com.quannian.zhidong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.quannian.zhidong.ui.theme.Palette
import java.io.File
import android.net.Uri
import java.util.Locale

/**
 * V2 教学视频播放器（Media3 ExoPlayer，本地 assets 视频）。
 *
 *  满足任务 3.3 全部要求：
 *   - 播放/暂停（内置 PlayerControlView + 自定义 按钮）
 *   - 进度条 + 当前/总时长
 *   - 全屏（[onFullScreen] 回调，切 横屏 Activity）
 *   - 加载失败 明确 提示（onPlayerError 监听）
 *   - 离开 页面 释放（DisposableEffect → player.release()）
 *   - 本地 视频 打包 进 assets，网络 不可用 也能 播放（asset:// 协议 由 ExoPlayer 原生处理）
 *   - 重播 按钮
 *
 *  @param assetPath  本地 assets 路径（"videos/xxx.mp4"）。
 *  @param title / subtitle 教学 文字。
 *  @param onFullScreen 可选 全屏 回调。
 */
@Composable
fun TeachingVideoPlayer(
    assetPath: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onFullScreen: (() -> Unit)? = null,
    loop: Boolean = false,
    muted: Boolean = false,
    /** P0-B：true=容器强制 16:9（旧行为）；false=容器跟随父级高度，
     * 由 PlayerView resizeMode=FIT 对源视频真实比例做 letterbox，头脚完整入镜不拉伸。
     * 跟练双画面传 false。 */
    fixed16By9: Boolean = true
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    val player = remember { ExoPlayer.Builder(context).build() }

    // 本地 assets 视频 → 复制到应用缓存 后 用 file:// URI 播放（ExoPlayer 默认 数据源 可靠 支持，
    // 网络 不可用 也能 播放，满足 任务 3.3 "本地 视频 打包 进 assets"）。
    DisposableEffect(context, assetPath, player) {
        var listener: Player.Listener? = null
        try {
            val cacheDir = File(context.cacheDir, "videos").apply { mkdirs() }
            val fileName = assetPath.substringAfterLast('/')
            val file = File(cacheDir, fileName)
            if (!file.exists() || file.length() == 0L) {
                context.assets.open(assetPath).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(file).toString())
                .setMimeType("video/mp4")
                .build()
            val mediaSource =
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
                    .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    errorMsg = "视频 加载 失败（${error.errorCodeName}）"
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val d = player.duration
                        if (d in 1 until 1_000_000_000L) durationMs = d
                    }
                }
            }
            player.addListener(listener)
            if (loop) {
                player.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
            player.playWhenReady = true
            player.prepare()
        } catch (e: Exception) {
            errorMsg = "视频 资源 准备 失败：${e.message}"
        }
        onDispose {
            listener?.let { player.removeListener(it) }
            player.release()
        }
    }

    // 时间 轮询（composable 上下文；仅 更新 当前 时间/时长 显示）
    LaunchedEffect(player) {
        while (true) {
            currentMs = player.currentPosition
            kotlinx.coroutines.delay(250)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotBlank()) Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink,
            modifier = Modifier.padding(horizontal = 4.dp))
        if (subtitle.isNotBlank()) {
            Text(subtitle, fontSize = 12.sp, color = Palette.inkSoft,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }

        // 播放 区（16:9）
        // P0-B：容器策略按 fixed16By9 切换。true=固定 16:9；false=吃掉剩余高度、由 FIT 对源比例 letterbox。
        val videoBoxModifier: Modifier = if (fixed16By9) {
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .aspectRatio(16f / 9f)
                .background(Color(0xFF0E1420), RoundedCornerShape(16.dp))
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .background(Color(0xFF0E1420), RoundedCornerShape(16.dp))
        }
        Box(
            modifier = videoBoxModifier,
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    // V3.6.1 根因修复：PlayerView 默认使用 SurfaceView，其 Surface 在 window
                    // 层级渲染，会被 Compose Box 的 .background(0xFF0E1420) 遮挡 → 控制条正常
                    // 但视频帧不可见（FAIL_BLACK_PLAYER）。改用 ExoPlayer.setVideoTextureView() 绑定
                    // 裸 TextureView，参与正常 View 合成管线，与 Compose 正确叠加。
                    val tv = android.view.TextureView(ctx)
                    tv.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    tv.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(s: android.graphics.SurfaceTexture, w: Int, h: Int) {
                            player.setVideoTextureView(tv)
                        }
                        override fun onSurfaceTextureSizeChanged(s: android.graphics.SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(s: android.graphics.SurfaceTexture): Boolean {
                            player.setVideoTextureView(null)
                            return true
                        }
                        override fun onSurfaceTextureUpdated(s: android.graphics.SurfaceTexture) {}
                    }
                    tv
                }
            )
            if (errorMsg != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xCC0E1420)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("视频加载失败", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(errorMsg ?: "", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            if (onFullScreen != null) {
                IconButton(onClick = onFullScreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "全屏",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // 进度条 + 时间 + 播放/暂停 + 重播
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val progress = if (durationMs > 0) (currentMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Slider(
                value = progress,
                onValueChange = { f ->
                    val target = (f * durationMs).toLong()
                    player.seekTo(target)
                    currentMs = target
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(currentMs), fontSize = 12.sp, color = Palette.inkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "播放暂停", tint = Palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = {
                        player.seekTo(0)
                        currentMs = 0
                        player.play()
                    }) {
                        Icon(Icons.Filled.Replay, contentDescription = "从头", tint = Palette.accent,
                            modifier = Modifier.size(22.dp))
                    }
                }
                Text(formatTime(durationMs), fontSize = 12.sp, color = Palette.inkSoft)
            }
        }
    }
}

/** 时间 mm:ss。 */
internal fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
}
