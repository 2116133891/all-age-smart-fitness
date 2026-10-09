package com.quannian.zhidong.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * AI 动作诊断 · 入口（spec §十一/§三十一）。
 *
 *  首页「AI 动作诊断」卡片 → 本页：选运动 → 选本地视频 → 进入分析页。
 *  默认本地分析（spec §三十三）：选的是**设备上的视频**，绝不上传。
 */
@Composable
fun VideoPickScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStartAnalysis: (exerciseKind: String, exerciseName: String, ageId: String, videoUri: String) -> Unit
) {
    // 选中的运动（默认青年·深蹲——项目第一重点）
    val allExercises = ExerciseRepository.all()
    val defaultEx = allExercises.firstOrNull { it.id == "youth-squat" } ?: allExercises.first()
    var selectedKind by remember { androidx.compose.runtime.mutableStateOf(defaultEx.analysisKind ?: "squat") }
    var selectedName by remember { androidx.compose.runtime.mutableStateOf(defaultEx.name) }
    var selectedAgeId by remember { androidx.compose.runtime.mutableStateOf(defaultEx.ageGroup.id) }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // 把 Uri 转成字符串传下去（Nav 参数）；分析页再转回 Uri。
            onStartAnalysis(selectedKind, selectedName, selectedAgeId, uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("AI 动作诊断", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
                Pill("仅本机分析", Palette.accent)
            }
        }

        item {
            Card() {
                Text("第 1 步 · 选择要分析的动作", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(4.dp))
                Text("视频里的人正在做哪个动作？我们按这个动作做姿态识别与规范度评分。", fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp)
            }
        }

        items(AgeGroup.entries.toList()) { age ->
            val list = ExerciseRepository.byAgeGroup(age)
            if (list.isNotEmpty()) {
                Card(borderColor = Palette.ageColor(age.id).copy(alpha = 0.18f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(age.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ageColor(age.id))
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    list.forEach { ex ->
                        val isSelected = ex.analysisKind == selectedKind
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Palette.ageSoft(age.id) else Color.Transparent, RoundedCornerShape(14.dp))
                                .border(if (isSelected) 1.dp else 0.dp, Palette.ageColor(age.id).copy(alpha = if (isSelected) 0.5f else 0f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .clickable {
                                    selectedKind = ex.analysisKind ?: "squat"
                                    selectedName = ex.name
                                    selectedAgeId = age.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(ex.icon, fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                                Text(ex.tagline, fontSize = 11.sp, color = Palette.inkSoft)
                            }
                            if (isSelected) Text("✓", fontSize = 16.sp, color = Palette.ageColor(age.id), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("第 2 步 · 选择运动视频", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Text(
                    "从设备相册/文件里挑一段你做动作的视频（本地读取，不上传）。",
                    fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.accent, RoundedCornerShape(16.dp))
                        .clickable { videoLauncher.launch(arrayOf("video/*")) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📹  选择视频", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.ink, RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("返回", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
