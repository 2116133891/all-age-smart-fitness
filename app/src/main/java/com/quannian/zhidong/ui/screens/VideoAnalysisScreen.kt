package com.quannian.zhidong.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.score.AgeProfile
import com.quannian.zhidong.video.VideoAnalysisChannel
import com.quannian.zhidong.video.VideoAnalysisEngine
import com.quannian.zhidong.video.VideoFrameReader
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分析页阶段状态。
 *  - [IDLE]      等待开始（尚未点击"开始分析"）
 *  - [RUNNING]   正在提取帧 + 姿态识别 + 评分（实时进度/覆盖率/已处理帧）
 *  - [DONE]      分析完成（有真实人体帧，结果已写入通道，可进报告）
 *  - [NO_PERSON]  提取到视频但全程未识别人体 → 诚实指引，不跳伪造报告
 *  - [ERROR]     姿态模型初始化失败或视频不可读（可重试）
 *  - [CANCELED]  用户点了"取消分析"（保留缩略图，可重选）
 */
private enum class Stage { IDLE, RUNNING, DONE, NO_PERSON, ERROR, CANCELED }

/**
 * 离线视频动作分析页（spec §十五）。
 *
 *  选视频 → 预览 + "开始分析" → 后台抽帧(12fps) + MediaPipe 姿态识别 + 分析器评分
 *  → 实时进度条（已处理 N/M 帧 · 覆盖率 · 综合评分）→ 完成进报告页；无人/不可读走诚实分支。
 *  全程本机（spec §三十三），页面明示「视频仅在本机分析」。
 *
 *  P0-1 修复（对照"卡在'等待分析'/0 帧"）：
 *   - 进度条 + 已处理帧/总帧/识别人体帧（覆盖率）实时刷新，不再恒为 0。
 *   - 阶段文本随 EXTRACT→SCORING→DONE 真实切换（不再停在"等待分析"）。
 *   - 取消（AtomicBoolean 早退 + 协程 cancel）与页面离开时 finally 释放 PoseDetector，防资源泄漏。
 *   - 无人体 / 视频不可读 走独立诚实状态，不再伪造"分析完成"。
 */
@Composable
fun VideoAnalysisScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // content:// URI：OpenDocument 结果，跨组件传递用字符串；分析时再 Uri.parse 回 Uri。
    val sel = remember { VideoAnalysisChannel.pendingSelection }
    if (sel == null) {
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
    var personFrames by remember { mutableStateOf(0) }
    var currentPhase by remember { mutableStateOf("等待开始") }
    var currentReps by remember { mutableStateOf(0) }
    var currentScore by remember { mutableStateOf<Int?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val cancelFlag = remember { AtomicBoolean(false) }

    // 页面离开（onDispose）：置取消 + 终止协程；IO 侧 extractFrames 在下一帧检查 cancelFlag 早退，
    // 并在 finally 释放 PoseDetector，避免后台卡死/内存泄漏。
    DisposableEffect(Unit) {
        onDispose {
            cancelFlag.set(true)
            job?.cancel()
        }
    }

    /** 综合评分只在 DONE 时非 null；覆盖率 = 识别人体帧 / 已处理帧。 */
    val coveragePct = if (doneFrames > 0) (personFrames * 100f / doneFrames).toInt() else 0

    fun startRun() {
        if (stage == Stage.RUNNING) return
        cancelFlag.set(false)
        stage = Stage.RUNNING
        progress = 0f
        doneFrames = 0
        totalFrames = 0
        personFrames = 0
        currentPhase = "提取视频帧…"
        currentReps = 0
        currentScore = null
        job = scope.launch(Dispatchers.IO) {
            val appCtx = context.applicationContext
            val detector = PoseDetector(appCtx)
            try {
                val initOk = try {
                    detector.init()
                } catch (_: Throwable) {
                    false
                }
                if (!initOk) {
                    stage = Stage.ERROR
                    currentPhase = "模型加载失败"
                    return@launch
                }
                currentPhase = "正在识别人体姿态"
                val reader = VideoFrameReader(appCtx)
                val uri = Uri.parse(sel.videoUri)
                val frames = reader.extractFrames(
                    uri = uri,
                    detector = detector,
                    isCancelled = { cancelFlag.get() },
                    onProgress = { done, total, person ->
                        totalFrames = total
                        doneFrames = done
                        personFrames = person
                        progress = if (total > 0) done.toFloat() / total else 0f
                    }
                )
                // 用户取消：不跳报告，回到可重选（保留缩略图）。
                if (cancelFlag.get()) {
                    stage = Stage.CANCELED
                    currentPhase = "已取消"
                    return@launch
                }
                // 视频不可读 / 无时长：诚实 ERROR，不伪造。
                if (frames.isEmpty()) {
                    stage = Stage.ERROR
                    currentPhase = "无法读取视频"
                    return@launch
                }
                val person = frames.count { it.landmarks.isNotEmpty() }
                personFrames = person
                // 全程无人体：诚实指引，不跳伪造报告。
                if (person == 0) {
                    stage = Stage.NO_PERSON
                    currentPhase = "未识别人体"
                    return@launch
                }
                currentPhase = "计算规范度评分"
                val analyzer = ExerciseAnalyzerFactory.freshFor(sel.exerciseKind)
                val target = ExerciseRepository.all()
                    .firstOrNull { it.analysisKind == sel.exerciseKind }?.targetReps ?: 10
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
                currentReps = result.totalReps
                currentScore = result.overallScore
                currentPhase = "分析完成"
                thumb?.let { if (!it.isRecycled) it.recycle() }
                thumb = null
                stage = Stage.DONE
            } finally {
                // 无论完成/取消/异常都释放检测器（资源释放）。
                detector.close()
            }
        }
    }

    fun cancelRun() {
        cancelFlag.set(true)
        // 协程在下一帧早退；这里 cancel 只是兜底终止引用。
        job?.let { j -> if (!j.isActive) return@let; j.cancel() }
        stage = Stage.CANCELED
        currentPhase = "已取消"
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

        // 视频预览（RUNNING 时叠加"正在分析"遮罩；缩略图为 content:// 视频首帧）
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("正在分析…", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("已处理 $doneFrames/$totalFrames 帧", fontSize = 12.sp, color = Color(0xFFE5EBFF))
                        }
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

        // 进度 / 实时数据（RUNNING / DONE / NO_PERSON / CANCELED 都展示真实数字）
        if (stage != Stage.IDLE) {
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
                        Text("已处理 $doneFrames/$totalFrames 帧", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                        Text("阶段：$currentPhase", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                        Text("人体帧 $personFrames", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("覆盖率：$coveragePct%", fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                        val score = currentScore
                        if (score != null) {
                            Text("综合评分：$score", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.accent, modifier = Modifier.weight(1f))
                        } else {
                            Text("综合评分：—", fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                        }
                        Text("次数：$currentReps", fontSize = 12.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 错误 / 无人 分支（诚实，不跳伪造报告）
        if (stage == Stage.ERROR || stage == Stage.NO_PERSON) {
            item {
                Card() {
                    Text(
                        if (stage == Stage.NO_PERSON) "⚠ 未识别到人体" else "⚠ 无法完成分析",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (stage == Stage.NO_PERSON) Palette.bad else Palette.bad
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (stage == Stage.NO_PERSON) {
                            "这段视频里没检测到做动作的人。请确保全程有完整的人体在画面内（站到镜头中央、全身入镜），重选一段视频再试。"
                        } else {
                            "姿态模型初始化失败或视频无法读取。请确认视频格式（MP4/H264）并稍后重试；若反复失败可重选视频。"
                        },
                        fontSize = 13.sp, color = Palette.inkSoft, lineHeight = 18.sp
                    )
                }
            }
        }

        // 行动按钮（按阶段）
        item {
            when (stage) {
                Stage.IDLE -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ageColor, RoundedCornerShape(16.dp))
                        .clickable { startRun() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶  开始分析（${VideoFrameReader.DEFAULT_FPS} fps 抽帧）", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Stage.RUNNING -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.line, RoundedCornerShape(16.dp))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("分析中，请稍候…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.inkSoft)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.ink, RoundedCornerShape(16.dp))
                            .clickable { cancelRun() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
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

                Stage.CANCELED -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.accent, RoundedCornerShape(16.dp))
                            .clickable { startRun() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("重新开始", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.ink, RoundedCornerShape(16.dp))
                            .clickable { onBack() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("重选视频", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.accent, RoundedCornerShape(16.dp))
                            .clickable { startRun() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("重试", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Palette.ink, RoundedCornerShape(16.dp))
                            .clickable { onBack() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("重选视频", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
