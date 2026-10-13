package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.components.TeachingVideoPlayer
import com.quannian.zhidong.ui.theme.Palette

/**
 * 数字人教学页（竞赛演示级）。
 *
 *  核心：大尺寸动作示范（占约 45% 页面高度）+ 动作/文字同步。
 *  P0-2 修复（对照"中年/银龄仍显示旧火柴人"）：
 *   - 教学舞台不再用 CoachSpriteView/CoachMotionView 的 2D 关节火柴人；
 *     改按 exerciseId 从 [TeachingVideoRepository.teachingVideoFor] 取**匹配动作**的
 *     真实 AIGC 示范视频（八段锦→八段锦、太极→太极、肩颈→肩颈…），用 [TeachingVideoPlayer]
 *     循环播放，动作与页面动作严格一致，不做泛化替代。
 *   - 该动作无专属示范视频时（如深蹲/引体等），走**诚实**卡片：
 *     显示该动作的要领步骤 + 安全提示，绝不塞一个不匹配的视频冒充。
 *   - 顶部保留小 [CoachAvatar] 做"数字教练"身份识别（静止形象），主体换成真视频。
 */
@Composable
fun CoachScreen(
    exerciseId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStartFollow: () -> Unit
) {
    val context = LocalContext.current
    val ex = ExerciseRepository.find(exerciseId) ?: return
    val age = ex.ageGroup
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)

    // 数字教练针对纠正模式（spec §十九）：从视频报告进入时，数字教练重新示范
    // 针对主错误的正确动作 + 纠正示范文案 + "开始重新练习"。
    val correction = remember(exerciseId) {
        com.quannian.zhidong.video.VideoAnalysisChannel.peekCorrection().let { (exId, err) ->
            val isThis = exId == exerciseId
            if (isThis) {
                com.quannian.zhidong.video.VideoAnalysisChannel.consumeCorrection()
                err
            } else null
        }
    }
    val correctionText = correction?.let { code ->
        val err = com.quannian.zhidong.model.ErrorType.values().firstOrNull { t -> t.code == code }
            ?: com.quannian.zhidong.model.ErrorType.POSTURE_DRIFT
        com.quannian.zhidong.video.CorrectionWording.of(err)
    }

    // P0-2：该动作匹配的"真实示范视频"（按 exerciseId 严格匹配，无匹配返回 null）。
    val teachVideo = remember(exerciseId) {
        TeachingVideoRepository.teachingVideoFor(exerciseId)
    }
    val videoUsable = teachVideo != null &&
        teachVideo.assetPath.isNotBlank() &&
        TeachingVideoRepository.assetExists(context, teachVideo.assetPath)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("数字人教学", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
                Pill(ex.level, color)
            }
        }

        // 数字教练针对纠正横幅（spec §十九）：数字教练重新示范 + 纠正文案
        if (correctionText != null) {
            item {
                Card(borderColor = color.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎯", fontSize = 18.sp)
                        Text("数字教练纠正示范", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        correctionText,
                        fontSize = 14.sp, color = Palette.ink, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("数字教练正在重新示范正确动作，看示范后点下方「开始重新练习」。",
                        fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp)
                }
            }
        }

        // 动作示范舞台（大尺寸，占约 45% 页面高度）：真视频优先；无匹配视频走诚实卡片。
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(soft, RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 小形象做"数字教练"身份识别（静止 PNG，非示范主体）
                        CoachAvatar(ageId = age.id, size = 40.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(ex.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Spacer(Modifier.weight(1f))
                        val tag = when {
                            correctionText != null -> "纠正示范"
                            videoUsable -> "示范视频"
                            else -> "动作要领"
                        }
                        Text(tag, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                    }

                    // 主体（P0-2）：匹配动作的真实示范视频（循环播放）；无匹配→诚实卡片。
                    val usableVideo = if (videoUsable) teachVideo else null
                    if (usableVideo != null) {
                        TeachingVideoPlayer(
                            assetPath = usableVideo.assetPath,
                            title = "",
                            subtitle = "${ex.name} · 标准动作示范",
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            loop = true
                        )
                    } else {
                        // 诚实分支：该动作无专属示范视频（深蹲/引体等），展示要领与提示，不塞泛化视频。
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF18243C), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (teachVideo != null) "该动作示范视频暂时不可用" else "该动作暂无专属示范视频",
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD9A8)
                            )
                            Text(
                                "下方为该动作的要领与安全提示，跟随文字与跟练页的实时反馈练习。",
                                fontSize = 12.sp, color = Color(0xFFB9C4DA), lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("动作要领", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            ex.steps.forEachIndexed { i, s ->
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${i + 1}", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(16.dp))
                                    Text(s, fontSize = 12.sp, color = Color(0xFFDCE4F2), modifier = Modifier.weight(1f),
                                        lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 今日计划
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今天我们一起进行", fontSize = 14.sp, color = Palette.inkSoft)
                    Text("${ex.duration}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                    Text("基础训练，共 ${ex.targetReps} 组动作。", fontSize = 14.sp, color = Palette.inkSoft)
                }
            }
        }

        // 动作要领速览
        item {
            Card() {
                Text("动作要领", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                ex.steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${i + 1}", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                        Text(s, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    if (i < ex.steps.lastIndex) Spacer(Modifier.height(6.dp))
                }
                // 安全提示
                if (ex.cautions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚠️", fontSize = 14.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("注意事项", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                            ex.cautions.forEach { c ->
                                Text("· $c", fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }
        }

        // 行动按钮
        item {
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("▶  开始跟练", color, Color.White, onStartFollow, Modifier.fillMaxWidth())
                ActionButton("返回首页", Palette.ink, Color.White, onBack, Modifier.fillMaxWidth())
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ActionButton(
    text: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
