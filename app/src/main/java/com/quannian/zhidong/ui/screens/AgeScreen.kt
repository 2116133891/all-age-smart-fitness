package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.ExerciseCard
import com.quannian.zhidong.ui.theme.Palette

/**
 * 年龄段页：数字人教练 + 该年龄的运动项目列表。
 */
@Composable
fun AgeScreen(
    id: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPickExercise: (String) -> Unit
) {
    val age = AgeGroup.entries.firstOrNull { it.id == id } ?: AgeGroup.YOUTH
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)
    val exercises = ExerciseRepository.byAgeGroup(age)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 头部：返回 + 数字人 + 标题
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(soft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CoachAvatar(ageId = age.id, size = 44.dp)
                }
                Column {
                    Text(age.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text(age.subtitle, fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("选择今天想练习的项目", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
        }

        // 运动卡片
        items(exercises) { ex ->
            ExerciseCard(
                icon = ex.icon,
                title = ex.name,
                tagline = ex.tagline,
                duration = ex.duration,
                level = ex.level,
                accent = color,
                accentSoft = soft,
                detected = ex.analysisKind != null,
                onClick = { onPickExercise(ex.id) }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
