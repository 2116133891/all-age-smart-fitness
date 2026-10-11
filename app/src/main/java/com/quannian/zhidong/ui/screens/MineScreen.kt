package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.theme.Palette

/**
 * "我的" tab（企业级）：头像画像 + 训练统计 + 能力入口（我的训练/手环/个性化）+ 设置/关于。
 *  数据全部本地（Room 统计 + SharedPreferences 画像），无网络。
 */
@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    onTraining: () -> Unit = {},
    onWearable: () -> Unit = {},
    onProfile: () -> Unit = {},
    onBodyTest: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    var summary by remember { mutableStateOf<TrainingRepository.Summary?>(null) }

    LaunchedEffect(Unit) {
        summary = runCatching { TrainingRepository(context).summary() }.getOrNull()
    }

    val sessions = summary?.sessionCount ?: 0
    val reps = summary?.totalRepetitions ?: 0
    val avg = summary?.avgScore ?: 0
    val totalMin = ((summary?.totalDurationSec ?: 0) / 60)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 头像 + 画像
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Palette.card, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoachAvatar(ageId = profile.ageId, size = 72.dp)
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("我的运动档案", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${ProfileStore.ageLabel(profile.ageId)} · ${ProfileStore.genderLabel(profile.gender)} · ${if (profile.nickname.isBlank()) "运动者" else profile.nickname}",
                        fontSize = 13.sp, color = Palette.inkSoft
                    )
                }
            }
        }

        // 训练统计
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.card, RoundedCornerShape(20.dp))
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock("${sessions}", "累计次数", Palette.accent)
                StatBlock("${reps}", "完成动作", Palette.good)
                StatBlock("${avg}", "平均得分", Palette.middle)
                StatBlock("${totalMin}", "运动分钟", Palette.warn)
            }
        }

        // 能力入口
        item {
            Text("我的功能", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
        }
        item { MenuRow("我的训练 · 成长曲线", Icons.Filled.History, Palette.accent, onTraining) }
        item { MenuRow("运动手环 · 配对同步", Icons.Filled.Sync, Palette.middle, onWearable) }
        item { MenuRow("体测自评", Icons.Filled.FitnessCenter, Palette.good, onBodyTest) }
        item { MenuRow("个性化 · 年龄性别", Icons.Filled.Person, Palette.senior, onProfile) }

        // 设置 + 关于
        item { Spacer(Modifier.height(6.dp)) }
        item { MenuRow("设置", Icons.Filled.Settings, Palette.inkSoft, onSettings) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("全龄智动 · 数字人智能运动指导 v1.0", fontSize = 12.sp, color = Palette.inkSoft,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("数据本地存储 · 摄像头姿态识别 · 不上云", fontSize = 11.sp, color = Palette.line,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 15.sp, color = Palette.ink, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = Palette.line, modifier = Modifier.size(22.dp))
    }
}
