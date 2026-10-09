package com.quannian.zhidong.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.analyzer.ExerciseAnalyzerFactory
import com.quannian.zhidong.pose.PoseDetector
import com.quannian.zhidong.score.AgeProfile
import com.quannian.zhidong.video.VideoAnalysisChannel
import com.quannian.zhidong.video.VideoAnalysisEngine
import com.quannian.zhidong.video.VideoFrameReader
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 分析页阶段状态。 */
private enum class Stage { IDLE, RUNNING, DONE, ERROR }

/**
 * 离线视频动作分析页（spec §十五）。
 *
 *  选视频 → 预览 + "开始分析" → 后台抽帧 + MediaPipe + 分析器 → 进度条
 *  （当前动作 / 第 N 次 / 阶段 / 当前评分）→ 完成后进报告页。
 *  全程本机（spec §三十三），页面明示「视频仅在本机分析」。
 */
@Composable
fun VideoAnalysisScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val sel = remember { VideoAnalysisChannel.pendingSelection } ?: run {
        onBack()
        return
    }
    val ageColor = Palette.ageColor(sel.ageId)

    // 视频预览缩略图
    var thumb by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sel.videoUri) {
        thumb = withContext(Dispatchers.IO) {
            try {
                VideoFrameReader(context).thumbnail(Uri.parse(sel.videoUri))
            } catch (_: Exception) {
                null
            }
        }
    }

    // 分析状态
    var stage by remember { mutableStateOf(Stage.IDLE) }
    var progress by remember { mutableStateOf(0f) }
    var doneFrames by remember { mutableStateOf(0) }
    var totalFrames by remember { mutableStateOf(0) }
    var currentPhase by remember { mutableStateOf("等待分析") }
    var currentReps by remember { mutableStateOf(0) }
    // 触发标志：点击"开始分析"置 true，分析完成后不再变；用 Int 避免枚举重入
    var runTrigger by remember { mutableStateOf(0) }

    // 后台：init PoseDetector → 抽帧分析 → 写入结果通道（仅当 runTrigger 变化时执行）
    LaunchedEffect(runTrigger) {
        if (runTrigger == 0) return@LaunchedEffect
        stage = Stage.RUNNING
        withContext(Dispatchers.IO) {
            val detector = PoseDetector(context.applicationContext)
            val ok = try {
                detector.init()
            } catch (_: Exception) {
                false
            }
            if (!ok) {
                stage = Stage.ERROR
            } else {
                val reader = VideoFrameReader(context.applicationContext)
                val uri = Uri.parse(sel.videoUri)
                val frames = reader.extractFrames(uri, detector) { done, total ->
                    totalFrames = total
                    doneFrames = done
                    progress = if (total > 0) done.toFloat() / total else 0f
                }
                val analyzer = ExerciseAnalyzerFactory.freshFor(sel.exerciseKind)
                val target = com.quannian.zhidong.repository.ExerciseRepository
                    .all().firstOrNull { it.analysisKind == sel.exerciseKind }?.targetReps ?: 10
                val result = VideoAnalysisEngine.analyze(
                    frames = frames,
                    analyzer = analyzer,
                    targetReps = target,
                    weights = AgeProfile.forAge(sel.ageId)
                )
                VideoAnalysisChannel.put(
                    result = result,
                    uri = uri,
                    exerciseKind = sel.exerciseKind,
                    exerciseName = sel.exerciseName,
                    ageId = sel.ageId
                )
                // 清理缩略图
                thumb?.let { if (!it.isRecycled) it.recycle() }
                currentPhase = "分析完成"
                currentReps = result.totalReps
                stage = Stage.DONE
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (stage != Stage.RUNNING) onBack() }) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("视频动作分析", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
                Pill(sel.exerciseName, ageColor)
            }
        }

        // 视频预览
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10182A), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val t = thumb
                if (t != null) {
                    Image(
                        bitmap = t.asImageBitmap(),
                        contentDescription = "视频预览",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎬", fontSize = 40.sp)
                        Text(sel.exerciseName, fontSize = 14.sp, color = Color.White)
                    }
                }
                if (stage == Stage.RUNNING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("正在分析…", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 本地分析提示（隐私）
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔒", fontSize = 15.sp)
                    Text(
                        "视频仅在本机分析，绝不上传服务器。",
                        fontSize = 13.sp, color = Palette.good, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 进度 / 实时数据
        if (stage == Stage.RUNNING || stage == Stage.DONE) {
            item {
                Card() {
                    Text("分析进度", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = ageColor,
                        trackColor = Palette.line
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("已处理", fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                        Text("阶段：$currentPhase", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                        Text("次数：$currentReps", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 错误
        if (stage == Stage.ERROR) {
            item {
                Card() {
                    Text("⚠ 无法完成分析", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.bad)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "姿态模型初始化失败或视频无法读取。请确认视频格式（MP4/H264）并稍后重试。",
                        fontSize = 13.sp, color = Palette.inkSoft, lineHeight = 18.sp
                    )
                }
            }
        }

        // 行动按钮
        item {
            when (stage) {
                Stage.IDLE -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ageColor, RoundedCornerShape(16.dp))
                        .clickable { runTrigger += 1 }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶  开始分析（${VideoFrameReader.DEFAULT_FPS} fps 抽帧）", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Stage.RUNNING -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.line, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("分析中，请稍候…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.inkSoft)
                }
                Stage.DONE -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.accent, RoundedCornerShape(16.dp))
                        .clickable { onDone() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("查看 AI 动作体检报告", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Stage.ERROR -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.ink, RoundedCornerShape(16.dp))
                        .clickable { stage = Stage.IDLE }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("重试", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
