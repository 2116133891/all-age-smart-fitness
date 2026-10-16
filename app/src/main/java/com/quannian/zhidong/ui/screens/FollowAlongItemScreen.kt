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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.quannian.zhidong.camera.CameraManager
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.BodyTestResult
import com.quannian.zhidong.db.BodyTestStandardService
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.model.BodyTestVideo
import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.pose.PoseDetector
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * V3.3 青年体测「单项跟练/测评」双栏页（spec §3.2 / §2）：
 *
 *  从体测项目卡 / 教学视频页 / 备考计划任务 进入，带 itemKey。
 *  - 左侧：该项的真实示范视频（[TeachingVideoRepository.videoForItem] 严格映射；BMI/肺活量无视频走诚实空态）。
 *  - 右侧：实时摄像头 + MediaPipe 骨架（辅助识别，不承诺自动测距/计数）。
 *  - 底部：该项目成绩人工录入 + 保存（source="manual"），诚实分级；BMI 双值算 BMI。
 *
 *  双栏复用 [TeachPane] / [CameraPane]（与 [FollowAlongScreen] 同一套组件与严格视频映射），
 *  不另造冲突跟练页；相机用独立 [CameraManager] 实例，避免跨页共享状态。
 */
@Composable
fun FollowAlongItemScreen(
    itemKey: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val gender = profile.gender
    val item = remember(itemKey) { BodyTestCatalog.byKey(itemKey) }
    val repo = remember { BodyTestRepository(context) }
    val scope = rememberCoroutineScope()

    // V3.5 P0-5：示范视频/头像按 用户教练风格 + 年龄段 真正解析（年龄组由该体测项所属决定）。
    val coachStyle = remember { com.quannian.zhidong.model.CoachStyleStore.load(context) }
    val assetAge = remember(itemKey) { com.quannian.zhidong.repository.CoachAssetRepository.ownerAgeOf(itemKey) }
    val assetRes = remember(itemKey, gender, coachStyle) {
        com.quannian.zhidong.repository.CoachAssetRepository.resolveAsset(
            itemKey = itemKey, gender = gender, style = coachStyle,
            assetType = com.quannian.zhidong.repository.CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = assetAge
        )
    }
    // #5 统一权威解析（教学页与跟练页同一来源）：风格专属示范缺失时，回退到「同一动作」的
    // 权威标准示范视频（TeachingVideoRepository，与独立教学页同源），不做跨动作/跨风格/跨年龄段替换。
    // 独立教学页直接透传已知 assetPath 故能播放；跟练页重解析为空即导致"视频消失"。此回退消除该不一致。
    val canonical = remember(itemKey, gender) {
        com.quannian.zhidong.repository.TeachingVideoRepository.videoForItem(itemKey, gender)
            ?: com.quannian.zhidong.repository.TeachingVideoRepository.byId(itemKey)
    }
    val styleMissing = assetRes.resolved == null && !assetRes.methodOnly
    val teachVideo = if (styleMissing) canonical else assetRes.resolved
    val videoUsable = teachVideo != null && teachVideo.assetPath.isNotBlank()
    val styleNote = when {
        assetRes.methodOnly -> ""
        styleMissing && canonical != null -> "风格专属示范暂缺，先播放该动作标准示范"
        else -> assetRes.note
    }
    val displayName = item?.let { BodyTestCatalog.displayName(it, gender) } ?: itemKey

    // 相机（独立实例；onFrame 仅写回 landmarks 供骨架展示，不接分析器、不做自动计数）
    val landmarkBox = remember { java.util.concurrent.atomic.AtomicReference<List<Landmark>>(emptyList()) }
    val readyBox = remember { java.util.concurrent.atomic.AtomicReference<Boolean>(false) }
    val cameraManager = remember {
        CameraManager(
            context = context,
            owner = context as androidx.lifecycle.LifecycleOwner,
            poseDetector = PoseDetector(context.applicationContext),
            onFrame = { lm -> landmarkBox.set(lm) },
            onPoseReady = { ready -> readyBox.set(ready) }
        )
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }
    var facingBack by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { cameraManager.release() }
    }

    // 录入状态
    var input by remember { mutableStateOf("") }
    var input2 by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<BodyTestResult?>(null) }
    LaunchedEffect(itemKey) { latest = repo.latestResult(itemKey) }

    val isHeightWeight = item?.inputType == BodyTestCatalog.InputType.HEIGHT_WEIGHT
    val value = input.toDoubleOrNull()
    val value2 = input2.toDoubleOrNull()
    val canSave = if (isHeightWeight) value != null && value2 != null && (value ?: 0.0) > 0 else value != null

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { cameraManager.release(); onBack() }) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
            }
            Column(Modifier.weight(1f)) {
                Text("跟练 · $displayName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                val cap = TeachingVideoRepository.followCapabilityLabel(itemKey)
                Text(
                    if (styleNote.isNotBlank()) "$cap · $styleNote" else cap,
                    fontSize = 11.sp, color = Palette.inkSoft
                )
            }
            if (item?.genderSplit == true) Pill(displayName, Palette.accent)
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

        // V3.5 P0-7：强制竖屏「上下堆叠」：上=教练示范视频，下=实时摄像头+骨架（同 FollowAlongScreen 规则）。
        // 不区分横/竖屏并排，始终上视频下相机，两块同屏可见；BoxWithConstraints 按可用高度自适应，
        // 空间紧张时压缩权重但绝不移除任一块；视频/相机保持各自宽高比，不拉伸、不切头脚。
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val avail = maxHeight
            val tight = avail < 520.dp
            val videoW = if (tight) 0.46f else 0.42f
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TeachPane(
                    exId = itemKey, exName = displayName, exSteps = keyPointsOf(teachVideo),
                    teachVideo = teachVideo, videoUsable = videoUsable,
                    ageColor = Palette.accent, modifier = Modifier.fillMaxWidth().weight(videoW)
                )
                ItemCameraPane(
                    cameraManager = cameraManager,
                    facingBack = facingBack,
                    hasPermission = hasPermission,
                    onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    onRetry = { cameraManager.restart() },
                    landmarkBox = landmarkBox, readyBox = readyBox,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Card() {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isHeightWeight) "录入身高 / 体重（计算 BMI）" else "录入实测成绩（${item?.unit ?: ""}）",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink
                )
                Spacer(Modifier.weight(1f))
                Pill("人工录入", Palette.warn)
            }
            Spacer(Modifier.height(8.dp))
            if (isHeightWeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        label = { Text("身高 (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = input2, onValueChange = { input2 = it },
                        label = { Text("体重 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                val bmi = if (canSave) value2!! / ((value!! / 100.0) * (value!! / 100.0)) else null
                if (bmi != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("BMI = ${"%.1f".format(bmi)} kg/m²（= 体重kg ÷ 身高m²，非体脂率）",
                        fontSize = 13.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    label = { Text("实测值（${item?.unit ?: ""}）· 人工录入") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSave) Palette.accent else Palette.line)
                    .clickable(enabled = canSave && !saved) {
                        val raw = if (isHeightWeight) value2!! / ((value!! / 100.0) * (value!! / 100.0)) else value!!
                        scope.launch {
                            val applicable = BodyTestStandardService.applicable(gender)
                            val (score, scoreValid) =
                                if (applicable) BodyTestStandardService.scoreFor(itemKey, gender, raw) else 0 to false
                            repo.saveResult(BodyTestResult(
                                itemKey = itemKey, ageGroup = "youth", gender = gender,
                                testDate = System.currentTimeMillis(),
                                rawValue = raw,
                                rawValue2 = if (isHeightWeight) (value2 ?: 0.0) else 0.0,
                                heightCm = if (isHeightWeight) (value ?: 0.0) else 0.0,
                                weightKg = if (isHeightWeight) (value2 ?: 0.0) else 0.0,
                                unit = item?.unit ?: "",
                                source = "manual",
                                standardVersion = if (applicable) BodyTestStandardService.STANDARD_VERSION else "",
                                score = score, scoreValid = scoreValid,
                                note = if (isHeightWeight) "BMI 人工录入计算" else "人工录入实测值"
                            ))
                            latest = repo.latestResult(itemKey)
                            saved = true
                        }
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Check, null,
                    tint = if (canSave && !saved) Color.White else Palette.inkSoft, modifier = Modifier.size(18.dp))
                Text(
                    if (saved) "已保存本次成绩 ✓" else "保存成绩",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (canSave && !saved) Color.White else Palette.inkSoft
                )
            }
            if (latest != null && latest!!.scoreValid) {
                Spacer(Modifier.height(6.dp))
                Text("最近官方评分：${latest!!.score} 分（${BodyTestStandardService.gradeLabel(latest!!.score)}）",
                    fontSize = 12.sp, color = Palette.good, fontWeight = FontWeight.SemiBold)
            }
            if (TeachingVideoRepository.isMethodOnlyItem(itemKey)) {
                Spacer(Modifier.height(6.dp))
                Text("此项为独立测评录入项（无动作示范视频），不作为动态跟练项目；右侧摄像头仅作站位辅助。",
                    fontSize = 11.sp, color = Palette.inkSoft, lineHeight = 15.sp)
            }
        }
    }
}

/** 视频动作要点（无视频给安全要点）。 */
private fun keyPointsOf(v: BodyTestVideo?): List<String> {
    if (v == null) return listOf("站好位置，保持身体在画面中央", "完成后在下方录入实测值")
    return v.keyPoints.ifEmpty { listOf(v.summary) }
}

/**
 * 单项页专用相机区：与 [FollowAlongScreen.CameraPane] 布局一致，但额外把
 * landmarks/poseReady 写回调用方 State（用 AtomicReference 桥接 CameraManager 回调）。
 */
@Composable
private fun ItemCameraPane(
    cameraManager: CameraManager,
    facingBack: Boolean,
    hasPermission: Boolean,
    onGrant: () -> Unit,
    onRetry: () -> Unit = {},
    landmarkBox: java.util.concurrent.atomic.AtomicReference<List<Landmark>>,
    readyBox: java.util.concurrent.atomic.AtomicReference<Boolean>,
    modifier: Modifier
) {
    // 把 CameraManager 的回调结果（经 AtomicReference 桥接）反映到本地 Compose State：
    // 轻量轮询（120ms）更新骨架关键点与识别就绪标志，避免在 CameraManager 构造期捕获不可写局部 var。
    var lm by remember { mutableStateOf<List<Landmark>>(landmarkBox.get()) }
    var pr by remember { mutableStateOf<Boolean>(readyBox.get()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (lm !== landmarkBox.get()) lm = landmarkBox.get()
            if (pr != readyBox.get()) pr = readyBox.get()
            kotlinx.coroutines.delay(120)
        }
    }
    com.quannian.zhidong.ui.screens.CameraPane(
        cameraManager = cameraManager,
        landmarks = lm,
        facingBack = facingBack,
        poseReady = pr,
        hasPermission = hasPermission,
        onGrant = onGrant,
        onRetry = onRetry,
        modifier = modifier
    )
}
