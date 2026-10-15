package com.quannian.zhidong.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quannian.zhidong.camera.CameraManager
import com.quannian.zhidong.coach.LocalCoachProvider
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.pose.PoseDetector
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.components.SkeletonOverlay
import com.quannian.zhidong.ui.components.TeachingVideoPlayer
import com.quannian.zhidong.ui.theme.Palette

/**
 * 跟练页 · V3.3 双栏重构（spec §二 P0）：
 *
 *   ┌──────────────────────┬──────────────────────┐
 *   │ 左侧：对应动作真实教学视频 │ 右侧：实时摄像头 + MediaPipe 骨架 │
 *   ├──────────────────────┼──────────────────────┤
 *   │ 示范控制 / 动作要点    │ 识别状态 / 次数 / 反馈 │
 *   └──────────────────────┴──────────────────────┘
 *
 *  关键改造（相对旧版）：
 *   - 左侧不再是 Canvas 火柴人（CoachSpriteView），而是 [TeachingVideoPlayer] 播放
 *     [TeachingVideoRepository.teachingVideoFor] 严格匹配到当前动作的真实视频（循环静音）。
 *   - 无匹配视频时走**诚实**"暂无该动作示范视频"卡片 + 动作要点，不悄悄回退火柴人。
 *   - 右侧保留 CameraX + MediaPipe 实时摄像头 + 骨架叠加；两个画面**同屏同时可见**，
 *     不通过 tab 切换冒充双栏。
 *   - 布局响应式：窄屏（竖屏）用上下/并排自适应，宽屏（横屏）用左右双栏；两者都在
 *     同一屏内可见，不锁全局方向、不影响导航栈与相机生命周期。
 */
@Composable
fun FollowAlongScreen(
    exerciseId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onFinish: (com.quannian.zhidong.model.TrainingReport) -> Unit
) {
    val ex = ExerciseRepository.find(exerciseId) ?: return
    val exTarget = ex.targetReps
    val context = LocalContext.current
    val vm = viewModel<FollowAlongViewModel> {
        FollowAlongViewModel(ex.analysisKind, exTarget)
    }
    val landmarks by vm.landmarks.collectAsState()
    val phase by vm.phase.collectAsState()
    val score by vm.score.collectAsState()
    val stateText by vm.stateText.collectAsState()
    val corrections by vm.corrections.collectAsState()
    val repCount by vm.repCount.collectAsState()
    val poseReady by vm.poseReady.collectAsState()
    val subLabel by vm.subLabel.collectAsState()
    val coachState by vm.coachState.collectAsState()
    val currentErrors by vm.currentErrors.collectAsState()

    val ageId = ex.ageGroup.id
    val ageColor = Palette.ageColor(ageId)
    val ageSoft = Palette.ageSoft(ageId)

    // 教学视频（P0 统一严格映射；无匹配/空资源 → null，走诚实空态卡片）。
    val teachVideo = remember(exerciseId) { TeachingVideoRepository.teachingVideoFor(exerciseId) }
    val videoUsable = teachVideo != null && teachVideo.assetPath.isNotBlank()

    // 关键修复：cameraManager 在组合期创建单实例（幂等 start/release），避免 AndroidView factory 早于
    // LaunchedEffect 导致黑屏；黑屏/初始化失败走 [CameraManager.restart()] 重绑，不重建实例、不竞争 CameraX 用例。
    val cameraManager = remember {
        CameraManager(
            context = context,
            owner = context as androidx.lifecycle.LifecycleOwner,
            poseDetector = PoseDetector(context.applicationContext),
            onFrame = { lm -> vm.onFrame(lm) },
            onPoseReady = { ready -> vm.setPoseReady(ready) }
        )
    }
    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // 前后摄像头（默认后置）
    var facingBack by remember { mutableStateOf(true) }

    // TTS 语音教练开关（默认关闭）
    var voiceOn by remember { mutableStateOf(false) }
    LaunchedEffect(voiceOn) {
        com.quannian.zhidong.tts.TtsCoach.setVoiceEnabled(voiceOn)
        if (!voiceOn) com.quannian.zhidong.tts.TtsCoach.stop()
    }

    // 实时纠错 → 教练话术（本地，引用当前分数/次数 + 当前帧真实错误类型）
    val coachSpeech = remember(currentErrors, repCount, score) {
        LocalCoachProvider.correctionFor(ex.name, ageId, currentErrors, score).firstOrNull()
            ?: "保持节奏，${if (repCount >= exTarget) "做得很好！" else "继续加油！"}"
    }
    var lastSpokenSpeech by remember { mutableStateOf("") }
    LaunchedEffect(coachSpeech, voiceOn, repCount) {
        if (voiceOn && coachSpeech != lastSpokenSpeech) {
            com.quannian.zhidong.tts.TtsCoach.speak(coachSpeech)
            lastSpokenSpeech = coachSpeech
        }
    }
    // 页面退出时停止语音 + 释放分析器
    DisposableEffect(Unit) {
        onDispose {
            com.quannian.zhidong.tts.TtsCoach.stop()
            com.quannian.zhidong.tts.TtsCoach.setVoiceEnabled(false)
            vm.onSessionEnd()
        }
    }
    // P0-A 修复：任何退出路径（系统返回 / tab 切换 / 路由 pop）都释放相机与后台 executor。
    // 此前缺失会导致 CameraX 用例竞争，并让已 shutdown 的 executor 仍被 onGloballyPositioned
    // 触发 start() → RejectedExecutionException（主线程未捕获 → 闪退）。
    DisposableEffect(cameraManager) {
        onDispose { cameraManager.release() }
    }

    // 响应式：测量整页宽度。宽度大（横屏）→ 左右双栏；窄（竖屏）→ 上下双栏（两画面仍同屏可见）。
    var pageWidthDp by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg)
            .onGloballyPositioned { coords ->
                val wDp = coords.size.width / density.density
                if (wDp != 0f) pageWidthDp = wDp
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 头部：标题 + 年龄标签 + 语音开关 + 前后摄像头切换 + 返回
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { cameraManager.release(); onBack() }) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
            }
            Column(Modifier.weight(1f)) {
                Text("跟练 · ${ex.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Text(TeachingVideoRepository.followCapabilityLabel(exerciseId), fontSize = 11.sp, color = Palette.inkSoft)
            }
            Pill(ex.ageGroup.title, ageColor)
            Box(
                modifier = Modifier
                    .size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (voiceOn) ageSoft else Palette.cardSoft)
                    .clickable { voiceOn = !voiceOn },
                contentAlignment = Alignment.Center
            ) {
                Text(if (voiceOn) "🔊" else "🔇", fontSize = 16.sp, color = Palette.ink)
            }
            Box(
                modifier = Modifier
                    .size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Palette.cardSoft)
                    .clickable { cameraManager.switchCamera(); facingBack = !facingBack },
                contentAlignment = Alignment.Center
            ) {
                Text("↻", fontSize = 16.sp, color = Palette.ink, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── V3.5 P0-7：强制竖屏「上下堆叠」：上=教练示范视频，下=实时摄像头+骨架 ──
        // 不再区分横/竖屏并排，始终上视频下相机，两块同屏同时可见（非 tab 切换）。
        // 用 BoxWithConstraints 按可用高度自适应：空间充足时各占权重，空间紧张时压缩
        // 标题/次要指标/间距（绝不移除任一块），视频保持源宽高比、相机保持相机宽高比，
        // 不 fillBounds 拉伸、不切头脚；宽高比不一致时用中性底色填充。
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val avail = maxHeight
                // 空间紧张（< 520dp 可用高）→ 压缩两块权重，保证两块都可见
                val tight = avail < 520.dp
                val videoW = if (tight) 0.46f else 0.44f
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeachPane(
                        exId = exerciseId, exName = ex.name, exSteps = ex.steps,
                        teachVideo = teachVideo, videoUsable = videoUsable,
                        ageColor = ageColor,
                        modifier = Modifier.fillMaxWidth().weight(videoW)
                    )
                    CameraPane(
                        cameraManager = cameraManager, landmarks = landmarks,
                        facingBack = facingBack, poseReady = poseReady,
                        hasPermission = hasPermission,
                        onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                        onRetry = {
                            // 黑屏/初始化失败：重绑同一相机实例（不重建、不竞争 CameraX 用例）
                            cameraManager.restart()
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "📺 上方为示范视频，下方为你的实时画面与识别骨架，上下同屏对比跟练。",
            fontSize = 11.sp, color = Palette.inkSoft, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))

        // 数据看板：次数 + 得分
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreTile("完成次数", "${repCount}/${exTarget}", Modifier.weight(1f))
            ScoreTile("动作得分", "$score", Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        // 综合评分条 + 状态
        Card() {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("综合评分", fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                Text("$score", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (score / 100f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = ageColor,
                trackColor = Palette.line
            )
            Spacer(Modifier.height(6.dp))
            Text(stateText, fontSize = 12.sp, color = Palette.inkSoft)
            if (subLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Pill(subLabel, ageColor)
            }
        }

        Spacer(Modifier.height(8.dp))

        // 实时纠错 + 教练话术
        Card() {
            Text("实时纠错", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Spacer(Modifier.height(6.dp))
            if (corrections.isEmpty()) {
                Text("✓ 动作标准，继续保持！", fontSize = 13.sp, color = Palette.good)
            } else {
                corrections.take(2).forEach { c ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 13.sp)
                        Text(c, fontSize = 13.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ageSoft, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎤", fontSize = 14.sp)
                Text("教练：$coachSpeech", fontSize = 12.sp, color = Palette.ink, lineHeight = 16.sp, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // 结束按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ageColor, RoundedCornerShape(16.dp))
                .clickable { cameraManager.release(); onFinish(vm.finishReport(context)) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("结束训练并查看报告  (${repCount}/${exTarget})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 左栏：真实教学视频（循环静音）。无匹配视频 → 诚实"暂无示范视频"卡片 + 动作要点。
 *  绝不用火柴人 / 静态图冒充视频。
 */
@Composable
internal fun TeachPane(
    exId: String,
    exName: String,
    exSteps: List<String>,
    teachVideo: com.quannian.zhidong.model.BodyTestVideo?,
    videoUsable: Boolean,
    ageColor: Color,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ageColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        if (videoUsable) {
            Column(Modifier.fillMaxSize()) {
                Text("动作示范 · ${exName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ageColor)
                Spacer(Modifier.height(4.dp))
                TeachingVideoPlayer(
                    assetPath = teachVideo!!.assetPath,
                    title = "",
                    subtitle = "循环静音 · 边看边练",
                    loop = true,
                    muted = true,
                    fixed16By9 = false,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        } else {
            // 诚实空态：该动作没有专属示范视频
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("暂无「${exName}」示范视频", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(6.dp))
                Text("可对照下方动作要点与右侧实时摄像头进行跟练。", fontSize = 12.sp, color = Palette.inkSoft)
                Spacer(Modifier.height(8.dp))
                exSteps.take(3).forEach { s ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = ageColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(s, fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f), lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** 右栏：实时摄像头 + 人体骨架（复用 CameraManager + SkeletonOverlay）。 */
@Composable
internal fun CameraPane(
    cameraManager: CameraManager,
    landmarks: List<com.quannian.zhidong.model.Landmark>,
    facingBack: Boolean,
    poseReady: Boolean,
    hasPermission: Boolean,
    onGrant: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10182A), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (hasPermission) {
            Box(Modifier.fillMaxSize()) {
                var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
                var crop: RectF by remember { mutableStateOf(RectF(0f, 0f, 1f, 1f)) }
                // V3.5 黑屏根因修复：PreviewView 在 AndroidView factory 阶段往往尚未
                // 测量（0 尺寸），此刻直接 start() 会让 CameraX 把 Surface 绑到 0 尺寸上
                // → 画面黑。改为：factory 只创建并记录 PreviewView，真正 start() 延后到
                // 该 view 有非 0 测量尺寸时（onGloballyPositioned）再触发一次（幂等）。
                AndroidView(
                    factory = { ctx ->
                        val pv = PreviewView(ctx)
                        pv.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        previewViewRef = pv
                        pv
                    },
                    modifier = Modifier.fillMaxSize().onGloballyPositioned { coords ->
                        // 仅当首次真正拿到非 0 尺寸时启动相机（CameraManager.start 幂等）
                        val pv = previewViewRef ?: return@onGloballyPositioned
                        if (coords.size.width > 0 && coords.size.height > 0) {
                            cameraManager.start(pv)
                        }
                    },
                    onRelease = { cameraManager.release(); previewViewRef = null }
                )
                LaunchedEffect(previewViewRef, facingBack) {
                    val view = previewViewRef ?: return@LaunchedEffect
                    var tries = 0
                    while (true) {
                        val c = cameraManager.visibleCrop(view)
                        if (c != null) { crop = c; break }
                        if (++tries > 60) break
                        kotlinx.coroutines.delay(50)
                    }
                }
                if (landmarks.isNotEmpty()) {
                    SkeletonOverlay(
                        landmarks = landmarks,
                        modifier = Modifier.fillMaxSize(),
                        crop = crop,
                        mirror = !facingBack
                    )
                }
                when {
                    !poseReady -> Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                        Pill("正在启动智能识别…", Palette.warn)
                    }
                    landmarks.isEmpty() -> Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)) {
                        Pill("没有检测到完整人体，请站到摄像头前并后退一步", Palette.warn)
                    }
                    else -> Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                        Pill(if (facingBack) "后置 · 识别中" else "前置 · 识别中", Palette.good)
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("需要摄像头权限", fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.accent, RoundedCornerShape(12.dp))
                        .clickable { onGrant() }.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("授权摄像头", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

/** 数据小卡片。 */
@Composable
private fun ScoreTile(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
    }
}
