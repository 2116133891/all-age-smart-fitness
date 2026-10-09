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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachMotionView
import com.quannian.zhidong.ui.components.CoachState
import com.quannian.zhidong.ui.components.CoachTeachingPlayer
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * 数字人教学页（竞赛演示级）。
 *
 *  核心：数字人大尺寸动作演示（35%~45% 页面高度，spec §十九）+ 动作/文字同步（§十七）。
 *  - 用 [CoachTeachingPlayer] 播放该动作的演示序列，数字人**真的做这个动作**
 *    （深蹲会屈膝下蹲、侧拉伸会侧倾、开合跳会分腿举臂…），不再是统一上下跳。
 *  - "开始跟练"进入跟练页。
 */
@Composable
fun CoachScreen(
    exerciseId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStartFollow: () -> Unit
) {
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

        // 数字人动作演示舞台（大尺寸，占约 40% 页面高度）
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
                        Text(ex.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Spacer(Modifier.weight(1f))
                        Text(if (correctionText != null) "纠正示范" else "数字人示范", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                    }
                    // 大尺寸动作演示：数字人帧序列（Pose Sprite）演示该动作，逐帧不同姿态
                    com.quannian.zhidong.ui.components.CoachSpriteView(
                        ageId = age.id,
                        exerciseKey = ex.analysisKind,
                        figureSizePx = 560,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
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
