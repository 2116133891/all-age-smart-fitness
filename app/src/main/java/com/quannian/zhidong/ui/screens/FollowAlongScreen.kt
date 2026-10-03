package com.quannian.zhidong.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.quannian.zhidong.model.Exercise
import com.quannian.zhidong.pose.PoseDetector
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.CoachState
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.components.SkeletonOverlay
import com.quannian.zhidong.ui.theme.Palette

/**
 * 跟练页：竞赛演示级布局（对照 spec §10）。
 *
 *  布局：
 *   ┌──────────────────────────────────────┐
 *   │ 跟练 · 基础深蹲            [青年]  ↻ │  头部：标题 + 年龄 + 前后摄像头切换
 *   ├──────────────────────────────────────┤
 *   │ ┌────────┐   ┌──────────────────────┐│
 *   │ │ 数字人  │   │ 摄像头 + 人体骨架     ││  数字人（智能教练）+ 摄像头画面
 *   │ │ 教动作  │   │                        ││
 *   │ └────────┘   └──────────────────────┘│
 *   ├──────────────────────────────────────┤
 *   │ 次数  05    得分  92                  │  数据看板
 *   │ ████████████░░                        │
 *   │ ✓ 动作标准    💡 膝盖与脚尖同方向       │  纠错
 *   │        [结束训练]                      │
 *   └──────────────────────────────────────┘
 *
 *  数字人根据 动作阶段 + 实时纠错 驱动状态（DEMO / CORRECT / GOOD），
 *  体现"数字人是智能纠错的可视化载体"。
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

    val ageId = ex.ageGroup.id
    val ageColor = Palette.ageColor(ageId)
    val ageSoft = Palette.ageSoft(ageId)

    // 关键修复：cameraManager 在组合期创建，避免 AndroidView factory 早于 LaunchedEffect 导致黑屏。
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

    // TTS 语音教练开关（默认关闭；用户可打开）
    var voiceOn by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(voiceOn) {
        com.quannian.zhidong.tts.TtsCoach.setVoiceEnabled(voiceOn)
        if (!voiceOn) com.quannian.zhidong.tts.TtsCoach.stop()
    }

    // 实时纠错 → 数字人话术（本地教练，引用当前分数/次数）
    val coachSpeech = remember(corrections, repCount, score) {
        LocalCoachProvider.correctionFor(ex.name, ageId, emptySet(), score).firstOrNull()
            ?: "保持节奏，${if (repCount >= exTarget) "做得很好！" else "继续加油！"}"
    }
    // 语音教练：话术变化时朗读（仅在 voiceOn 时生效）
    androidx.compose.runtime.LaunchedEffect(coachSpeech, voiceOn, repCount) {
        if (voiceOn) com.quannian.zhidong.tts.TtsCoach.speak(coachSpeech)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 头部：标题 + 年龄标签 + 前后摄像头切换
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { cameraManager.release(); onBack() }) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Column(Modifier.weight(1f)) {
                    Text("跟练 · ${ex.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("当前动作：${ex.name}", fontSize = 11.sp, color = Palette.inkSoft)
                }
                Pill(ex.ageGroup.title, ageColor)
                // 语音教练开关（🔊 开 / 🔇 关）
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (voiceOn) ageSoft else Palette.cardSoft)
                        .clickable { voiceOn = !voiceOn },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (voiceOn) "🔊" else "🔇", fontSize = 18.sp, color = Palette.ink)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Palette.cardSoft)
                        .clickable { cameraManager.switchCamera(); facingBack = !facingBack },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↻", fontSize = 18.sp, color = Palette.ink, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 数字人 + 摄像头（并排，摄像头为主区域）
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 数字人教练面板
                Column(
                    modifier = Modifier
                        .weight(0.36f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ageSoft, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CoachAvatar(
                        ageId = ageId,
                        size = 96.dp,
                        coachState = coachState
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("智能教练", fontSize = 11.sp, color = Palette.inkSoft)
                    if (subLabel.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Pill(subLabel, ageColor)
                    }
                }
                // 摄像头 + 骨骼（主区域）
                CameraPane(
                    cameraManager = cameraManager,
                    landmarks = landmarks,
                    facingBack = facingBack,
                    poseReady = poseReady,
                    hasPermission = hasPermission,
                    onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(0.64f)
                )
            }
        }

        // 数据看板：次数 + 得分
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreTile("完成次数", "${repCount}/${exTarget}", Modifier.weight(1f))
                ScoreTile("动作得分", "$score", Modifier.weight(1f))
            }
        }

        // 综合评分条
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("综合评分", fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                    Text("$score", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (score / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = ageColor,
                    trackColor = Palette.line
                )
                Spacer(Modifier.height(6.dp))
                Text(stateText, fontSize = 12.sp, color = Palette.inkSoft)
            }
        }

        // 纠错 + 数字人话术
        item {
            Card() {
                Text("实时纠错", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                Spacer(Modifier.height(6.dp))
                if (corrections.isEmpty()) {
                    Text("✓ 动作标准，继续保持！", fontSize = 14.sp, color = Palette.good)
                } else {
                    corrections.forEach { c ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 13.sp)
                            Text(c, fontSize = 13.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                        }
                    }
                }
                // 数字人当前话术（教练说的）
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ageSoft, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎤", fontSize = 14.sp)
                    Text("教练：$coachSpeech", fontSize = 12.sp, color = Palette.ink, lineHeight = 16.sp)
                }
            }
        }

        // 结束按钮
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ageColor, RoundedCornerShape(16.dp))
                    .clickable { cameraManager.release(); onFinish(vm.finishReport()) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("结束训练并查看报告  (${repCount}/${exTarget})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** 摄像头 + 骨骼画面区块。weight 由调用方在 RowScope 里应用。 */
@Composable
private fun CameraPane(
    cameraManager: CameraManager,
    landmarks: List<com.quannian.zhidong.model.Landmark>,
    facingBack: Boolean,
    poseReady: Boolean,
    hasPermission: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF10182A), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (hasPermission) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val pv = PreviewView(ctx)
                        pv.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        cameraManager.start(pv)
                        pv
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { cameraManager.release() }
                )
                if (landmarks.isNotEmpty()) {
                    SkeletonOverlay(
                        landmarks = landmarks,
                        modifier = Modifier.fillMaxSize(),
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
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("需要摄像头权限", fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.accent, RoundedCornerShape(12.dp))
                        .clickable { onGrant() }.padding(horizontal = 20.dp, vertical = 10.dp)
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
